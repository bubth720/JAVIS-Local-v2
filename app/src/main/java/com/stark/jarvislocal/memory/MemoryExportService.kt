package com.stark.jarvislocal.memory

import com.stark.jarvislocal.identity.IdentityCodec
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Future qualified host encryption adapter. No key, Keystore claim, or fallback is supplied here. */
interface MemoryExportCipherPort {
    fun encrypt(plainJson: ByteArray): ByteArray
    fun decrypt(ciphertext: ByteArray): ByteArray
}
/** Host-chosen, newly created destination only; false/unknown emptiness must refuse an overwrite. */
interface MemoryExportDestinationPort {
    fun isEmpty(): Boolean
    fun openOutput(): java.io.OutputStream
    fun openReadback(): java.io.InputStream
}
class MemoryExportPreview internal constructor(val id: String, val sessionId: String, val json: String,
    val sha256: String, internal val references: Set<MemoryReference>, internal val expiresElapsedMs: Long,
    internal val createdElapsedMs: Long, internal val sessionToken: Any) {
    override fun toString() = "MemoryExportPreview(content=REDACTED)"
}
data class ImportedMemoryCandidate(val sourceReference: MemoryReference, val content: String, val scope: MemoryScope,
    val preferenceKey: String?, val claimedConfirmedAtMs: Long, val expiresAtMs: Long?, val conflicts: List<String>) {
    val authority: String get() = "UNTRUSTED_IMPORT_REQUIRES_FRESH_CONSENT"
    override fun toString() = "ImportedMemoryCandidate(content=REDACTED, conflicts=${conflicts.size})"
}
data class ImportPreview(val candidates: List<ImportedMemoryCandidate>) {
    override fun toString() = "ImportPreview(count=${candidates.size}, writable=false)"
}

/** Explicit private-file export only. Preview/import are RAM; there is deliberately no import-apply API. */
class MemoryExportService(private val store: ConfirmedMemoryStore, private val controls: UserMemoryControl,
    private val privateDirectory: File, private val wallMs: () -> Long = System::currentTimeMillis,
    private val elapsedMs: () -> Long = android.os.SystemClock::elapsedRealtime,
    private val cipher: MemoryExportCipherPort? = null) {
    private val pending = java.util.concurrent.ConcurrentHashMap<String, MemoryExportPreview>()
    private val policies = java.util.concurrent.ConcurrentHashMap<String, ProjectionPolicy>()
    @Synchronized fun preview(sessionId: String, selected: Set<MemoryReference>, policy: ProjectionPolicy = ProjectionPolicy()): MemoryExportPreview = synchronized(store) {
        require(selected.size in 1..10)
        val sessionToken = controls.token(sessionId) ?: throw MemoryRejected("MEMORY_SESSION_SUSPENDED")
        val rows = selected.sortedBy { it.id }.map { active(sessionId, it, policy) }
        val bytes = IdentityCodec.canonical(mapOf("format" to FORMAT, "exported_at_ms" to wallMs(),
            "entries" to rows.map { r -> mapOf("source_id" to r.reference.id, "source_revision" to r.reference.revision,
                "content" to r.content, "content_sha256" to r.reference.contentDigest, "scope" to r.scope.name,
                "preference_key" to r.preferenceKey, "confirmed_at_ms" to r.consent.confirmedAtMs,
                "expires_at_ms" to r.expiresAtMs, "source" to "USER_EXPLICIT") }))
        prune(); if (pending.size >= 16) throw MemoryRejected("EXPORT_PREVIEW_QUOTA")
        val now = elapsedMs(); require(now >= 0 && now <= Long.MAX_VALUE - 90_000)
        if (controls.token(sessionId) !== sessionToken) throw MemoryRejected("MEMORY_SESSION_SUSPENDED")
        val p = MemoryExportPreview(UUID.randomUUID().toString(), sessionId, bytes.toString(Charsets.UTF_8),
            IdentityCodec.sha256(bytes), selected.toSet(), now + 90_000, now, sessionToken)
        pending[p.id] = p; policies[p.id] = ProjectionPolicy(policy.allowed); p
    }
    fun revokeSession(sessionId: String) {
        pending.values.filter { it.sessionId == sessionId }.map { it.id }.forEach { pending.remove(it); policies.remove(it) }
    }
    @Synchronized fun writeConfirmed(id: String, sessionId: String, displayedSha256: String, encrypted: Boolean = false,
        destination: MemoryExportDestinationPort? = null, checkActive: () -> Unit = {}): File = synchronized(store) {
        val p = pending[id] ?: throw MemoryRejected("EXPORT_PREVIEW_ABSENT")
        val policy = policies[id] ?: throw MemoryRejected("EXPORT_PREVIEW_ABSENT")
        if (p.sessionId != sessionId || p.sha256 != displayedSha256) throw MemoryRejected("EXPORT_PREVIEW_MISMATCH")
        if (controls.token(sessionId) !== p.sessionToken) throw MemoryRejected("MEMORY_SESSION_SUSPENDED")
        if (elapsedMs() < p.createdElapsedMs || elapsedMs() >= p.expiresElapsedMs) throw MemoryRejected("EXPORT_PREVIEW_EXPIRED")
        p.references.forEach { active(sessionId, it, policy) }
        checkActive()
        if (destination != null && !destination.isEmpty()) throw MemoryRejected("DESTINATION_NOT_EMPTY_OR_UNKNOWN")
        val plain = IdentityCodec.utf8(p.json)
        val bytes = if (encrypted) (cipher ?: throw MemoryRejected("CIPHER_UNAVAILABLE")).encrypt(plain) else plain
        require(bytes.isNotEmpty() && bytes.size <= 65536)
        if (controls.token(sessionId) !== p.sessionToken) throw MemoryRejected("MEMORY_SESSION_SUSPENDED")
        p.references.forEach { active(sessionId, it, policy) }
        if (pending[id] !== p) throw MemoryRejected("EXPORT_PREVIEW_ABSENT")
        val directory = privateDirectory.canonicalFile
        require((directory.isDirectory || directory.mkdirs()) && directory.listFiles().orEmpty().size < 20) { "EXPORT_DIRECTORY_QUOTA" }
        val target = File.createTempFile("memory-", if (encrypted) ".bin" else ".json", directory)
        var privateComplete = false
        try {
            FileOutputStream(target).use { out -> out.write(bytes); out.fd.sync() }
            privateComplete = true
            pending.remove(id); policies.remove(id)
            if (destination != null) {
                // A failed copy/ACK can leave a partial destination; never delete/overwrite a provider document.
                try {
                    checkActive()
                    if (!destination.isEmpty()) throw MemoryRejected("DESTINATION_NOT_EMPTY_OR_UNKNOWN")
                    destination.openOutput().use { out ->
                        var offset = 0
                        while (offset < bytes.size) {
                            checkActive()
                            if (controls.token(sessionId) !== p.sessionToken || !controls.allows(sessionId)) throw MemoryRejected("MEMORY_SESSION_SUSPENDED")
                            val n = minOf(4096, bytes.size - offset); out.write(bytes, offset, n); offset += n
                        }
                        out.flush()
                    }
                    checkActive()
                    val digest = java.security.MessageDigest.getInstance("SHA-256")
                    var count = 0
                    destination.openReadback().use { input ->
                        val buffer = ByteArray(4096)
                        while (true) { checkActive(); val n = input.read(buffer); if (n < 0) break; count += n
                            if (count > bytes.size) throw MemoryRejected("DESTINATION_INTEGRITY_FAILED")
                            digest.update(buffer, 0, n)
                        }
                    }
                    if (count != bytes.size || !java.security.MessageDigest.isEqual(digest.digest(), java.security.MessageDigest.getInstance("SHA-256").digest(bytes)))
                        throw MemoryRejected("DESTINATION_INTEGRITY_FAILED")
                } catch (_: Exception) { throw MemoryRejected("EXPORT_COPY_UNKNOWN_PRIVATE_COPY_RETAINED") }
            }
            target
        } catch (failure: Exception) {
            // This directory is private, single-writer host storage, never a shared/hostile provider.
            if (!privateComplete && target.canonicalFile.parentFile == directory) target.delete()
            throw failure
        }
    }
    @Synchronized fun exportedFiles(): List<File> = privateDirectory.canonicalFile.listFiles().orEmpty()
        .filter { it.isFile && it.name.matches(Regex("memory-[0-9]+\\.(json|bin)")) }.sortedBy { it.name }.take(20)
    @Synchronized fun previewImport(file: File, encrypted: Boolean = false): ImportPreview {
        require(file.canonicalFile.parentFile == privateDirectory.canonicalFile && file in exportedFiles())
        val raw = file.inputStream().use { input ->
            val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(4096)
            while (true) { val n = input.read(buffer); if (n < 0) break; require(out.size() + n <= 65536); out.write(buffer, 0, n) }
            out.toByteArray()
        }
        val plain = if (encrypted) (cipher ?: throw MemoryRejected("CIPHER_UNAVAILABLE")).decrypt(raw) else raw
        return decodeImport(plain)
    }
    fun decodeImport(bytes: ByteArray): ImportPreview = synchronized(store) {
        val root = IdentityCodec.decode(bytes)
        require(root.keys == setOf("format", "exported_at_ms", "entries") && root["format"] == FORMAT)
        val exportedAt = root["exported_at_ms"] as? Long ?: error("INVALID_EXPORT_TIME")
        require(exportedAt >= 0)
        val entries = root["entries"] as? List<*> ?: error("INVALID_ENTRIES")
        require(entries.size in 1..10)
        val ids = mutableSetOf<String>(); val keys = mutableSetOf<String>()
        val candidates = entries.map { raw ->
            val row = raw as? Map<*, *> ?: error("INVALID_ENTRY")
            require(row.keys == setOf("source_id", "source_revision", "content", "content_sha256", "scope", "preference_key", "confirmed_at_ms", "expires_at_ms", "source"))
            val id = row["source_id"] as? String ?: error("INVALID_ID")
            require(UUID.fromString(id).toString() == id && ids.add(id))
            val revision = row["source_revision"] as? Long ?: error("INVALID_REVISION"); require(revision > 0)
            val text = row["content"] as? String ?: error("INVALID_CONTENT"); validateContent(text)
            val digest = row["content_sha256"] as? String ?: error("INVALID_DIGEST"); require(digest == contentDigest(text))
            val scope = MemoryScope.valueOf(row["scope"] as? String ?: error("INVALID_SCOPE")); require(scope != MemoryScope.SENSITIVE)
            val key = row["preference_key"]?.let { it as? String ?: error("INVALID_KEY") }
            require(key == null || key.matches(Regex("[a-z][a-z0-9_.-]{0,63}")) && keys.add(key))
            val confirmed = row["confirmed_at_ms"] as? Long ?: error("INVALID_CONFIRMATION")
            val expires = row["expires_at_ms"]?.let { it as? Long ?: error("INVALID_EXPIRY") }
            require(confirmed >= 0 && confirmed <= exportedAt && (expires == null || expires > confirmed) && row["source"] == "USER_EXPLICIT")
            val conflicts = mutableListOf<String>()
            try { store.readOwned(id); conflicts += "EXISTING_ID_NO_OVERWRITE" } catch (e: MemoryRejected) { if (e.reason != "MEMORY_ABSENT_OR_UNCONFIRMED") throw e }
            if (key != null && store.findPreference(key) != null) conflicts += "EXISTING_PREFERENCE_NO_OVERWRITE"
            if (expires != null && wallMs() >= expires) conflicts += "EXPIRED_NO_ACTIVATION"
            ImportedMemoryCandidate(MemoryReference(id, revision, digest), text, scope, key, confirmed, expires, conflicts.toList())
        }
        ImportPreview(candidates.toList()) // no consent receipt is trusted or created from this file
    }
    private fun active(sessionId: String, expected: MemoryReference, policy: ProjectionPolicy): MemoryRecord {
        if (!controls.allows(sessionId, expected.id)) throw MemoryRejected("MEMORY_SESSION_SUSPENDED")
        val r = store.readOwned(expected.id); val now = wallMs()
        if (r.reference != expected || r.suspended || r.scope == MemoryScope.SENSITIVE || r.scope !in policy.allowed ||
            now < r.createdAtMs || r.expiresAtMs != null && now >= r.expiresAtMs) throw MemoryRejected("MEMORY_NO_LONGER_AUTHORIZED")
        return r
    }
    private fun prune() {
        val now = elapsedMs()
        pending.values.filter { now < it.createdElapsedMs || now >= it.expiresElapsedMs }.map { it.id }.forEach { pending.remove(it); policies.remove(it) }
    }
    companion object { const val FORMAT = "jarvis.confirmed-memory-export/1" }
}
