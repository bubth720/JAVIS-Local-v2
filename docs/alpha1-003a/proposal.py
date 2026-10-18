#!/usr/bin/env python3
"""Documentary library proposal. Returns strings only; never applies/compiles a candidate."""

MODEL = r'''package dev.ffmpegkit.llama

/** Handle visibility is informational, never an admission or interprocess release proof. */
class LlamaModel internal constructor(
    @Volatile internal var handle: Long,
    val config: LlamaConfig,
    internal val frozenFile: java.io.File?,
    internal val frozenSha256: String?,
) {
    internal constructor(handle: Long, config: LlamaConfig) : this(handle, config, null, null)
    val isLoaded: Boolean get() = handle != 0L
    internal fun requireHandle(): Long {
        LlamaOwner.requireOwned(this)
        if (handle == 0L) throw LlamaException.ModelReleased()
        return handle
    }
}
'''

MEASURED = r'''package dev.ffmpegkit.llama

import android.content.Context
import android.os.Looper
import android.os.Process
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** Existing native resources are retained here even when no caller receives a model. */
internal class OwnerSlot {
    var handle = 0L
    var model: LlamaModel? = null
    var snapshot: PrivateSnapshot? = null
    var nativeUncertain = false
    var abandoned = false
}

internal object LlamaOwner {
    @Volatile private var worker: Thread? = null
    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "jarvis-llama-owner").also { it.isDaemon = true; worker = it }
    }
    private val dispatcher = executor.asCoroutineDispatcher()
    private val apiBusy = AtomicBoolean(false)
    private val inAdmissionCallback = AtomicBoolean(false)
    private var slot: OwnerSlot? = null // only the owner worker reads/writes this reference

    private fun enter(sync: Boolean) {
        check(!inAdmissionCallback.get()) { "CALLBACK_REENTRANCY" }
        check(Thread.currentThread() !== worker) { "OWNER_REENTRANCY" }
        if (sync) check(Looper.myLooper() != Looper.getMainLooper()) { "SYNC_MAIN_THREAD_FORBIDDEN" }
        check(apiBusy.compareAndSet(false, true)) { "OWNER_BUSY" } // refuse, never queue behind a heavy call
    }
    private fun checkWorker() = check(Thread.currentThread() === worker) { "WRONG_WORKER" }
    private fun healthy() { checkWorker(); check(slot?.nativeUncertain != true) { "NATIVE_QUARANTINED" } }

    suspend fun <T> operation(block: (checkCancelled: () -> Unit) -> T): T {
        val caller = currentCoroutineContext()
        caller.ensureActive()
        enter(sync = false)
        try {
            val result = withContext(NonCancellable) { withContext(dispatcher) {
                healthy()
                caller.ensureActive()
                block { caller.ensureActive() }
            } }
            caller.ensureActive() // return of JNI, not unload; discard revoked output
            return result
        } finally { apiBusy.set(false) }
    }

    fun <T> blocking(block: () -> T): T {
        enter(sync = true)
        val future = try { executor.submit(Callable {
            try { healthy(); block() } finally { apiBusy.set(false) }
        }) } catch (failure: Throwable) { apiBusy.set(false); throw failure }
        try { return future.get() }
        catch (failure: ExecutionException) { throw (failure.cause ?: failure) }
        catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            // Never cancel the future, clear the owner, or infer native completion here.
            throw MeasuredTransactionFailure(false, interrupted)
        }
    }

    /** Cancellable delivery has an abandonment hook; the worker owns cleanup to its real end. */
    suspend fun load(factory: (OwnerSlot, () -> Unit) -> LlamaModel): LlamaModel {
        currentCoroutineContext().ensureActive()
        enter(sync = false)
        return suspendCancellableCoroutine { continuation ->
            val cancelled = AtomicBoolean(false)
            continuation.invokeOnCancellation { cancelled.set(true) } // intent only, no release
            try { executor.execute {
                var owned: OwnerSlot? = null
                var gateHeld = true
                try {
                    healthy()
                    if (!continuation.isActive || cancelled.get()) return@execute
                    check(slot == null) { "MODEL_OR_CLEANUP_ALREADY_OWNED" }
                    val current = OwnerSlot()
                    slot = current // reserve before file/native allocation
                    owned = current
                    val model = factory(current) {
                        if (cancelled.get() || !continuation.isActive)
                            throw java.util.concurrent.CancellationException("LOAD_CANCELLED")
                    }
                    check(current.model === model && current.handle != 0L)
                    apiBusy.set(false)
                    gateHeld = false // subsequent caller owns its own gate; never clear it in our finally
                    continuation.resume(model) { _, _, _ ->
                        // Runs on any caller thread; only enqueue, no native work or positive receipt.
                        try { executor.execute { abandon(current) } }
                        catch (_: Throwable) {
                            // Retained slot is NOT cleared. No later load can pass slot == null.
                        }
                    }
                } catch (failure: Throwable) {
                    var reported = failure
                    owned?.let { current ->
                        current.abandoned = true
                        try { cleanup(current) }
                        catch (cleanup: Throwable) { reported = MeasuredTransactionFailure(false, cleanup) }
                    }
                    if (gateHeld) { apiBusy.set(false); gateHeld = false }
                    continuation.resumeWithException(reported)
                } finally { if (gateHeld) apiBusy.set(false) }
            } } catch (failure: Throwable) {
                apiBusy.set(false) // enqueue failed, factory never ran
                continuation.resumeWithException(failure)
            }
        }
    }

    fun loadInto(current: OwnerSlot, path: String, config: LlamaConfig, digest: String?): LlamaModel {
        checkWorker(); check(slot === current && current.handle == 0L)
        current.nativeUncertain = true // includes throw/zero result with no recoverable handle
        val handle = LlamaJNI.nativeLoadModel(path, config.contextSize, config.threads, config.gpuLayers)
        check(handle != 0L) { "MODEL_LOAD_UNCONFIRMED" }
        current.handle = handle // retained BEFORE constructing or delivering a wrapper
        current.nativeUncertain = false
        val model = LlamaModel(handle, config, current.snapshot?.file, digest)
        current.model = model
        return model
    }

    fun requireOwned(model: LlamaModel) {
        checkWorker()
        check(slot?.model === model) { "STALE_MODEL_OWNER" }
        check(slot?.nativeUncertain != true) { "NATIVE_QUARANTINED" }
    }
    fun quarantine(model: LlamaModel) {
        checkWorker(); check(slot?.model === model)
        slot!!.nativeUncertain = true // no reset API
    }
    fun release(model: LlamaModel) {
        checkWorker()
        if (model.handle == 0L && slot?.model !== model) return // legacy double release, never touch newer owner
        requireOwned(model)
        cleanup(slot!!)
    }
    private fun abandon(current: OwnerSlot) {
        checkWorker()
        if (slot !== current) return // never release a newer owner
        current.abandoned = true
        try { cleanup(current) } catch (_: Throwable) {
            // Slot, handle or cleanup-pending file remain retained. No reset or logger.
        }
    }
    private fun cleanup(current: OwnerSlot) {
        checkWorker(); check(slot === current)
        check(!current.nativeUncertain) { "NATIVE_RELEASE_UNKNOWN" }
        if (current.handle != 0L) {
            try { LlamaJNI.nativeReleaseModel(current.handle) }
            catch (failure: Throwable) {
                current.nativeUncertain = true
                throw MeasuredTransactionFailure(false, failure)
            }
            current.handle = 0L // only AFTER actual native return
            current.model?.handle = 0L
        }
        // Reached on retry too, including handle == 0. File failure cannot turn unload into a load grant.
        current.snapshot?.deleteOwned()
        slot = null // only after native release AND exact-file cleanup+directory fsync
    }
    fun retryAbandonedFileCleanup() {
        checkWorker()
        val current = slot ?: return
        check(current.abandoned && current.handle == 0L && !current.nativeUncertain) { "NOT_FILE_ONLY_CLEANUP" }
        cleanup(current) // cannot call nativeReleaseModel because handle is already zero
    }
    fun finalAdmission(block: () -> Unit) {
        checkWorker()
        check(inAdmissionCallback.compareAndSet(false, true)) { "CALLBACK_REENTRANCY" }
        try { block() } finally { inAdmissionCallback.set(false) }
    }
}

/** Private filesystem ownership is separate from the model and all CORE/Binder admissions. */
internal class PrivateSnapshot(private val root: File, val file: File) {
    private var created = false
    private var inode = 0L
    private var device = 0L

    companion object {
        fun privateRoot(context: Context): File {
            val app = context.applicationContext
            val data = File(app.applicationInfo.dataDir).canonicalFile
            val platformRoot = app.noBackupFilesDir
            check(OsConstants.S_ISDIR(Os.lstat(platformRoot.path).st_mode)) { "PRIVATE_ROOT_LINK" }
            val root = platformRoot.canonicalFile
            check(root.parentFile == data && root.name == "no_backup") { "PRIVATE_ROOT_REQUIRED" }
            for (directory in listOf(data, root)) {
                val stat = Os.lstat(directory.path)
                check(OsConstants.S_ISDIR(stat.st_mode) && stat.st_uid == Process.myUid() &&
                    (stat.st_mode and 0x3f) == 0) { "PRIVATE_DIRECTORY_REQUIRED" }
            }
            return root
        }
        fun createDescriptor(root: File): PrivateSnapshot {
            // Allocate the descriptor BEFORE opening: an O_EXCL collision never owns/deletes another file.
            val nonce = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val name = nonce.joinToString("") { "%02x".format(it.toInt() and 255) }
            nonce.fill(0)
            return PrivateSnapshot(root, File(root, "jarvis-measured-$name.gguf"))
        }
    }

    fun copyBounded(sourcePath: String, expectedBytes: Long, expectedSha: String, checkCancelled: () -> Unit) {
        require(expectedBytes > 0L && expectedSha.matches(Regex("[a-f0-9]{64}"))) { "ARTIFACT_MANIFEST_REQUIRED" }
        val free = root.usableSpace
        check(free >= expectedBytes && free - expectedBytes >= 16L * 1024 * 1024) { "SNAPSHOT_DISK_UNAVAILABLE" }
        checkCancelled()
        val sourceFd = Os.open(sourcePath, OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW or OsConstants.O_CLOEXEC, 0)
        FileInputStream(sourceFd).use { input ->
            val original = Os.fstat(input.fd)
            check(OsConstants.S_ISREG(original.st_mode) && original.st_size == expectedBytes) { "SOURCE_SIZE_OR_TYPE" }
            val outFd = Os.open(file.path, OsConstants.O_WRONLY or OsConstants.O_CREAT or
                OsConstants.O_EXCL or OsConstants.O_NOFOLLOW or OsConstants.O_CLOEXEC, 0x180) // 0600
            created = true // retained by OwnerSlot before the file was created
            FileOutputStream(outFd).use { output ->
                val own = Os.fstat(output.fd)
                inode = own.st_ino; device = own.st_dev
                check(OsConstants.S_ISREG(own.st_mode) && own.st_uid == Process.myUid() &&
                    (own.st_mode and 0x3f) == 0) { "SNAPSHOT_OWNERSHIP" }
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(512 * 1024)
                var remaining = expectedBytes
                try {
                    while (remaining > 0L) {
                        checkCancelled()
                        val wanted = minOf(buffer.size.toLong(), remaining).toInt()
                        val count = input.read(buffer, 0, wanted)
                        check(count > 0) { "SOURCE_SHORT_OR_STALLED" }
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        remaining -= count.toLong()
                    }
                    checkCancelled()
                    check(input.read() == -1) { "SOURCE_GREW" } // at most expectedBytes+1 read, never +1 written
                    check(Os.fstat(input.fd).st_size == expectedBytes) { "SOURCE_SIZE_CHANGED" }
                    val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
                    check(actual == expectedSha) { "MODEL_HASH_MISMATCH" }
                    Os.fchmod(output.fd, 0x100) // 0400; same-UID/root compromise remains outside this guarantee
                    output.fd.sync()
                } finally { buffer.fill(0) }
            }
        }
        syncDirectory()
        checkCancelled() // before any native load; cancelled copy only needs file cleanup
    }
    private fun syncDirectory() {
        val fd = Os.open(root.path, OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW, 0)
        try {
            val directory = Os.fstat(fd)
            check(OsConstants.S_ISDIR(directory.st_mode) && directory.st_uid == Process.myUid() &&
                (directory.st_mode and 0x3f) == 0) { "PRIVATE_DIRECTORY_REQUIRED" }
            Os.fsync(fd)
        } finally { Os.close(fd) }
    }
    fun deleteOwned() {
        if (!created) return
        val stat = try { Os.lstat(file.path) } catch (failure: ErrnoException) {
            if (failure.errno != OsConstants.ENOENT) throw failure
            null
        }
        if (stat != null) {
            check(OsConstants.S_ISREG(stat.st_mode) && stat.st_uid == Process.myUid() &&
                stat.st_ino == inode && stat.st_dev == device && inode != 0L) { "SNAPSHOT_IDENTITY_CHANGED" }
            Os.remove(file.path) // regular owned file only, never source/glob/directory
        }
        syncDirectory() // retry after unlink/fsync failure also comes here
        created = false
    }
}

class MeasuredReceipt internal constructor(
    val revision: String, val modelSha256: String, val loadGeneration: Long,
    val templateSha256: String, val renderedSha256: String, val tokenIdsSha256: String,
    val bindingSha256: String, val kvEpoch: Long, val inputTokens: Long, val loadedContext: Long,
    val admittedContext: Long, val outputReserve: Int, val marginReserve: Int,
)
class MeasuredTransactionFailure(val nativeStateKnown: Boolean, cause: Throwable) :
    IllegalStateException("Measured transaction refused; inspect owner state", cause)

object LlamaMeasured {
    const val REVISION = "core.identity.measure/1-proposed"
    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun utf8(value: String): ByteArray {
        require('\u0000' !in value) { "NUL_UNSUPPORTED" }
        val encoded = Charsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(value))
        return ByteArray(encoded.remaining()).also { encoded.get(it) }
    }
    private fun decode(value: ByteArray) = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(value)).toString()

    /** New API only: host-approved SHA AND size, destination from private Android Context. */
    suspend fun loadFrozen(context: Context, modelPath: String, config: LlamaConfig,
        expectedSha256: String, expectedBytes: Long): LlamaModel = LlamaOwner.load { current, checkCancelled ->
        require(config.contextSize in 1..2048) { "INVALID_CONTEXT" }
        val root = PrivateSnapshot.privateRoot(context)
        val snapshot = PrivateSnapshot.createDescriptor(root)
        current.snapshot = snapshot // retain identity before opening or copying
        snapshot.copyBounded(modelPath, expectedBytes, expectedSha256, checkCancelled)
        checkCancelled()
        LlamaOwner.loadInto(current, snapshot.file.absolutePath, config, expectedSha256)
    }

    /** Explicit retry for an abandoned, already-native-free slot. Does NOT reset CORE admission. */
    fun retryAbandonedFileCleanup() = LlamaOwner.blocking { LlamaOwner.retryAbandonedFileCleanup() }

    suspend fun complete(model: LlamaModel, system: String, prompt: String, bindingSha256: String,
        requestedContext: Int, admittedContext: Int, outputReserve: Int,
        finalAdmission: (MeasuredReceipt) -> Unit): String = LlamaOwner.operation { checkCancelled ->
        check(model.frozenSha256 != null && model.frozenFile?.isFile == true) { "FROZEN_MODEL_REQUIRED" }
        require(bindingSha256.matches(Regex("[a-f0-9]{64}"))) { "INVALID_BINDING" }
        val handle = model.requireHandle()
        val random = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val ticket = random.joinToString("") { "%02x".format(it.toInt() and 255) }.toByteArray(Charsets.US_ASCII)
        random.fill(0)
        val binding = bindingSha256.toByteArray(Charsets.US_ASCII)
        var systemBytes: ByteArray? = null
        var promptBytes: ByteArray? = null
        var parts: Array<ByteArray>? = null
        var consumeEntered = false
        try {
            systemBytes = utf8(system); promptBytes = utf8(prompt)
            checkCancelled()
            parts = LlamaJNI.nativePrepareMeasured(handle, systemBytes, promptBytes, binding, ticket,
                requestedContext, admittedContext, outputReserve, 128, 5000)
            check(parts.size == 4) { "INVALID_RECEIPT" }
            val metadata = decode(parts[0]).split(',').map(String::toLong)
            check(metadata.size == 6 && metadata[0] == 1L && metadata[1] > 0 && metadata[2] > 0 && metadata[5] >= 0 &&
                parts[3].size.toLong() == metadata[2] * 4L) { "INVALID_RECEIPT" }
            val receipt = MeasuredReceipt(REVISION, requireNotNull(model.frozenSha256), metadata[1],
                sha(parts[1]), sha(parts[2]), sha(parts[3]), bindingSha256, metadata[5],
                metadata[2], metadata[3], metadata[4], outputReserve, 128)
            check(receipt.admittedContext == minOf(requestedContext.toLong(), admittedContext.toLong(), receipt.loadedContext))
            check(receipt.inputTokens + outputReserve.toLong() + 128L <= receipt.admittedContext) { "CONTEXT_EXCEEDED" }
            checkCancelled()
            LlamaOwner.finalAdmission { finalAdmission(receipt) }
            checkCancelled()
            consumeEntered = true
            val c = model.config
            val answer = LlamaJNI.nativeConsumeMeasured(handle, receipt.loadGeneration, ticket, binding,
                c.temperature, c.topP, c.topK, c.seed)
            try { decode(answer) } finally { answer.fill(0) }
        } catch (failure: Throwable) {
            if (consumeEntered) LlamaOwner.quarantine(model)
            throw MeasuredTransactionFailure(!consumeEntered, failure)
        } finally {
            try { LlamaJNI.nativeDiscardMeasured(handle, ticket) }
            catch (cleanup: Throwable) {
                LlamaOwner.quarantine(model)
                throw MeasuredTransactionFailure(false, cleanup)
            } finally {
                ticket.fill(0); binding.fill(0); systemBytes?.fill(0); promptBytes?.fill(0)
                parts?.forEach { it.fill(0) }
            }
        }
    }
}
'''


def revise(candidate):
    """Input/output maps contain only seven library-relative source strings."""
    candidate = dict(candidate)
    folder = 'library/src/main/kotlin/dev/ffmpegkit/llama/'
    candidate[folder + 'LlamaModel.kt'] = MODEL
    candidate[folder + 'LlamaMeasured.kt'] = MEASURED
    legacy = candidate[folder + 'Llama.kt']
    start = legacy.index('    ): LlamaModel = withContext(LlamaOwner.dispatcher) {')
    end = legacy.index('\n    /** Chat completion:', start)
    legacy = legacy[:start] + '''    ): LlamaModel = LlamaOwner.load { current, checkCancelled ->
        checkCancelled()
        if (!File(modelPath).exists()) throw LlamaException.ModelLoadFailed(modelPath)
        LlamaOwner.loadInto(current, modelPath, config, null)
    }
''' + legacy[end:]
    legacy = legacy.replace('import kotlinx.coroutines.withContext\n', '')
    legacy = legacy.replace('withContext(LlamaOwner.dispatcher) {', 'LlamaOwner.operation { _ ->')
    start = legacy.index('    fun releaseModel(model: LlamaModel)')
    end = legacy.index('\n    private fun parseResult', start)
    legacy = legacy[:start] + '''    // Existing signatures retained. Busy/reentrant/main-thread sync calls explicitly refuse.
    fun releaseModel(model: LlamaModel) = LlamaOwner.blocking { LlamaOwner.release(model) }

    fun getSystemInfo(): String = LlamaOwner.blocking { LlamaJNI.nativeGetSystemInfo() }

    // Retain the public synthetic bridge emitted by the old suspend implementation.
    @JvmStatic
    @JvmSynthetic
    @JvmName("access\\$parseResult")
    internal fun parseResultCompatibility(owner: Llama, json: String): LlamaResult = owner.parseResult(json)
''' + legacy[end:]
    candidate[folder + 'Llama.kt'] = legacy
    candidate['library/build.gradle.kts'] = candidate['library/build.gradle.kts'].replace(
        '0.1.1-jarvis-alpha1.002-review.1', '0.1.1-jarvis-alpha1.003a-review.1')
    return candidate
