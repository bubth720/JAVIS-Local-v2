package com.stark.jarvislocal.identity

import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.util.UUID

/** Host authority; never deserialized from a manifest, document or model response. Disabled by default. */
class IdentityPolicy(val enabled: Boolean = false, val revision: Long = 1,
    private val inferenceSafe: () -> Boolean = { false }) {
    @Volatile private var invalidated = false
    init { requireIdentity(revision in 1..IdentityCodec.MAX_INTEGER, "INVALID_POLICY") }
    fun invalidate() { invalidated = true }
    internal fun requireEnabled() { requireIdentity(enabled && !invalidated, "IDENTITY_DISABLED_OR_REVOKED") }
    internal fun validate(manifest: IdentityManifest, snapshot: IdentitySnapshot, session: String, now: Long) {
        requireEnabled()
        requireIdentity(snapshot.sessionId == session, "STALE_SESSION")
        requireIdentity(snapshot.policyRevision == revision && snapshot.manifestRef == manifest.reference &&
            snapshot.composerRevision == manifest.composerRevision && snapshot.styles == manifest.styles, "SNAPSHOT_MISMATCH")
        requireIdentity(now >= snapshot.createdMs && now < snapshot.expiresMs, "EXPIRED")
    }
    internal fun requireSafeInference() { requireIdentity(inferenceSafe(), "CLEANUP_UNCONFIRMED") }
}

/** Only the two technical read operations of approved001B. No personal preference or writer. */
interface IdentityPort {
    fun readTechnicalManifest(): IdentityManifest
    fun readTechnicalSnapshot(sessionId: String, policy: IdentityPolicy, nowMs: Long, lifetimeMs: Long = 90_000): IdentitySnapshot
}

/** Immutable packaged technical data. Supply assets.open("identity/manifest-b.json"); no SharedPreferences/DB. */
class LocalIdentityPort(private val openManifest: () -> InputStream) : IdentityPort {
    companion object {
        const val MANIFEST_DIGEST = "13f60b25f0a06a58f18a5570448b1373402aa514efa58927960498edcc3a4dd1"
    }
    override fun readTechnicalManifest(): IdentityManifest {
        val bytes = openManifest().use { stream ->
            val out = ByteArrayOutputStream(); val buffer = ByteArray(2048)
            while (true) {
                val size = stream.read(buffer); if (size < 0) break
                requireIdentity(out.size() + size <= 32768, "WIRE_LIMIT"); out.write(buffer, 0, size)
            }
            out.toByteArray()
        }
        return IdentityManifest.decode(bytes, MANIFEST_DIGEST)
    }
    override fun readTechnicalSnapshot(sessionId: String, policy: IdentityPolicy, nowMs: Long, lifetimeMs: Long): IdentitySnapshot {
        policy.requireEnabled(); uuid(sessionId)
        requireIdentity(nowMs in 0..IdentityCodec.MAX_INTEGER && lifetimeMs in 1..90_000 &&
            nowMs <= IdentityCodec.MAX_INTEGER - lifetimeMs, "INVALID_LIFETIME")
        val manifest = readTechnicalManifest()
        val fields = mapOf("version" to IdentityCodec.VERSION, "schema_revision" to IdentityCodec.REVISION,
            "kind" to "snapshot", "id" to UUID.randomUUID().toString(), "revision" to 1L, "session_id" to sessionId,
            "manifest_ref" to manifest.reference.json(), "policy_revision" to policy.revision, "consent_revision" to 0L,
            "composer_revision" to manifest.composerRevision, "jarvis_preferences" to emptyList<Any>(),
            "personal_preferences" to emptyList<Any>(), "consents" to emptyList<Any>(), "styles" to manifest.styles.values(),
            "created_ms" to nowMs, "expires_ms" to nowMs + lifetimeMs)
        return IdentitySnapshot.decode(IdentityCodec.canonical(fields))
    }
}
