package com.stark.jarvislocal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

enum class RecorderPhase { IDLE, STARTING, RECORDING, STOPPING, ERROR }

internal interface AudioInput {
    val initialized: Boolean
    val bufferSize: Int
    fun start()
    fun read(buffer: ByteArray): Int
    fun stop()
    fun release()
}

class WavRecorder internal constructor(
    private val permissionGranted: () -> Boolean,
    private val inputFactory: () -> AudioInput,
    private val openFile: (File) -> RandomAccessFile = { RandomAccessFile(it, "rw") }
) {
    constructor(context: Context) : this(
        microphonePermission(context.applicationContext),
        { AndroidAudioInput() }
    )

    private class Capture(val file: File) {
        val stop = AtomicBoolean(false)
        val discard = AtomicBoolean(false)
        val started = CompletableDeferred<Unit>()
        val completed = CompletableDeferred<File>()
        @Volatile var input: AudioInput? = null
        @Volatile var stopFailure: Throwable? = null
        lateinit var worker: Thread
    }

    private val mutableState = MutableStateFlow(RecorderPhase.IDLE)
    val state: StateFlow<RecorderPhase> = mutableState.asStateFlow()
    @Volatile private var capture: Capture? = null
    @Volatile private var resourcesUncertain = false
    private var lastCapture: Capture? = null

    fun isRecording(): Boolean = mutableState.value == RecorderPhase.RECORDING

    suspend fun start(file: File) {
        val current = synchronized(this) {
            check(!resourcesUncertain) { "Libération du microphone non confirmée. Redémarre JARVIS." }
            check(capture == null) { "Une capture audio est déjà active" }
            check(permissionGranted()) { "Permission micro absente" }
            Capture(file).also {
                capture = it
                lastCapture = it
                mutableState.value = RecorderPhase.STARTING
                try {
                    it.worker = thread(start = false, name = "JarvisRecorder") { record(it) }
                    it.worker.start()
                } catch (e: Throwable) {
                    capture = null
                    lastCapture = null
                    mutableState.value = RecorderPhase.ERROR
                    throw e
                }
            }
        }
        try { current.started.await() }
        catch (e: CancellationException) {
            withContext(NonCancellable) { stop(discard = true) }
            throw e
        }
    }

    suspend fun awaitCompletion(): File {
        val current = synchronized(this) { lastCapture } ?: error("Aucune capture audio")
        val file = current.completed.await()
        withContext(Dispatchers.IO) { current.worker.join() }
        check(!current.worker.isAlive) { "Capture audio encore active" }
        return file
    }

    suspend fun stop(discard: Boolean = false): File? {
        val current = synchronized(this) { capture ?: lastCapture } ?: return null
        if (discard) current.discard.set(true)
        current.stop.set(true)
        if (!current.completed.isCompleted) {
            mutableState.value = RecorderPhase.STOPPING
            // AudioRecord.stop unblocks READ_BLOCKING; never join on the UI thread.
            withContext(Dispatchers.IO) {
                try { current.input?.stop() } catch (e: Throwable) { current.stopFailure = e }
            }
        }
        return try {
            val file = current.completed.await()
            withContext(Dispatchers.IO) { current.worker.join() }
            check(!current.worker.isAlive) { "Capture audio encore active" }
            if (current.discard.get()) { WavFiles.delete(file); null } else file
        } catch (e: Exception) {
            if (current.discard.get()) {
                withContext(NonCancellable + Dispatchers.IO) { current.worker.join() }
                WavFiles.delete(current.file)
                check(!resourcesUncertain) { "Libération du microphone non confirmée. Redémarre JARVIS." }
                null
            } else throw e
        }
    }

    private fun record(current: Capture) {
        var input: AudioInput? = null
        var file: RandomAccessFile? = null
        var failure: Throwable? = null
        var bytes = 0L
        try {
            input = inputFactory()
            current.input = input
            check(input.initialized && input.bufferSize > 0) { "Microphone indisponible" }
            file = openFile(current.file)
            file.setLength(0)
            WavFiles.writeHeader(file, 0)
            if (!current.stop.get()) input.start()
            if (!current.stop.get()) mutableState.value = RecorderPhase.RECORDING
            current.started.complete(Unit)
            val buffer = ByteArray(input.bufferSize)
            while (!current.stop.get()) {
                val count = input.read(buffer)
                if (current.stop.get()) break
                check(count >= 0 && count <= buffer.size) { "Lecture microphone échouée : $count" }
                if (count == 0) { Thread.sleep(10); continue }
                check(count % 2 == 0) { "Échantillon audio incomplet" }
                file.write(buffer, 0, count)
                bytes += count
                check(bytes <= 0xffffffffL - 36) { "Enregistrement trop volumineux" }
            }
        } catch (e: Throwable) {
            failure = e
            current.started.completeExceptionally(e)
        } finally {
            try { input?.stop() } catch (e: Throwable) { if (failure == null) failure = e }
            try { input?.release() } catch (e: Throwable) {
                resourcesUncertain = true
                if (failure == null) failure = e
            }
            if (failure == null) failure = current.stopFailure
            try { file?.let { WavFiles.writeHeader(it, bytes) } } catch (e: Throwable) { if (failure == null) failure = e }
            try { file?.close() } catch (e: Throwable) { if (failure == null) failure = e }
            if (failure == null && !current.discard.get()) {
                try { WavFiles.validate(current.file) } catch (e: Throwable) { failure = e }
            }
            if (failure != null || current.discard.get()) {
                try { WavFiles.delete(current.file) } catch (e: Throwable) { if (failure == null) failure = e }
            }
            synchronized(this) {
                if (capture === current) capture = null
                mutableState.value = if (resourcesUncertain || (failure != null && !current.discard.get())) RecorderPhase.ERROR else RecorderPhase.IDLE
            }
            if (failure != null) current.completed.completeExceptionally(requireNotNull(failure))
            else if (current.discard.get()) current.completed.completeExceptionally(CancellationException("Capture abandonnée"))
            else current.completed.complete(current.file)
        }
    }
}

private class AndroidAudioInput : AudioInput {
    override val bufferSize: Int = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        .also { check(it > 0) { "Format microphone indisponible : $it" } }.coerceAtLeast(4096)
    private val recorder = try {
        AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize * 2)
    } catch (e: SecurityException) {
        throw IllegalStateException("Permission micro refusée ou révoquée", e)
    }
    override val initialized: Boolean get() = recorder.state == AudioRecord.STATE_INITIALIZED
    @Synchronized override fun start() {
        recorder.startRecording()
        check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Microphone non démarré" }
    }
    override fun read(buffer: ByteArray): Int = recorder.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
    @Synchronized override fun stop() { if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop() }
    @Synchronized override fun release() { recorder.release() }
}

private fun microphonePermission(applicationContext: Context): () -> Boolean =
    { ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED }
