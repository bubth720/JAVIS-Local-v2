package com.stark.jarvislocal.identity

import java.util.Collections
import java.util.UUID

internal fun Map<String, Any?>.exact(names: Set<String>) = requireIdentity(keys == names, "UNKNOWN_OR_MISSING_FIELD")
internal fun Map<String, Any?>.s(key: String): String = this[key] as? String ?: throw IdentityRejected("INVALID_TYPE")
internal fun Map<String, Any?>.n(key: String, min: Long = 0): Long = (this[key] as? Long)?.also {
    requireIdentity(it in min..IdentityCodec.MAX_INTEGER, "INVALID_NUMBER")
} ?: throw IdentityRejected("INVALID_TYPE")
internal fun uuid(s: String): String = s.also { requireIdentity(runCatching { UUID.fromString(it).toString() == it }.getOrDefault(false), "INVALID_UUID") }
internal fun hash(s: String): String = s.also { requireIdentity(it.matches(Regex("[a-f0-9]{64}")), "INVALID_DIGEST") }
internal fun Map<String, Any?>.map(key: String): Map<String, Any?> {
    @Suppress("UNCHECKED_CAST")
    return this[key] as? Map<String, Any?> ?: throw IdentityRejected("INVALID_TYPE")
}
private val common = setOf("version", "schema_revision", "kind", "id", "revision")
internal fun header(v: Map<String, Any?>, kind: String) {
    requireIdentity(v["version"] == IdentityCodec.VERSION && v["schema_revision"] == IdentityCodec.REVISION && v["kind"] == kind, "INCOMPATIBLE")
    uuid(v.s("id")); v.n("revision", 1)
}

data class IdentityReference(val id: String, val revision: Long, val sha256: String) {
    init { uuid(id); requireIdentity(revision in 1..IdentityCodec.MAX_INTEGER, "INVALID_NUMBER"); hash(sha256) }
    fun json(): Map<String, Any?> = mapOf("id" to id, "revision" to revision, "sha256" to sha256)
    companion object {
        internal fun parse(v: Map<String, Any?>): IdentityReference {
            v.exact(setOf("id", "revision", "sha256")); return IdentityReference(v.s("id"), v.n("revision", 1), v.s("sha256"))
        }
    }
}

class IdentityStyles private constructor(private val styles: Map<String, String>) {
    fun values(): Map<String, String> = styles
    override fun equals(other: Any?): Boolean = other is IdentityStyles && styles == other.styles
    override fun hashCode(): Int = styles.hashCode()
    companion object {
        private val allowed = mapOf("assertiveness" to setOf("ASSERTIVE", "CALM"), "warmth" to setOf("WARM", "NEUTRAL"),
            "verbosity" to setOf("CONCISE", "BALANCED", "DETAILED"), "humour" to setOf("OFF", "LIGHT"),
            "critical_thinking" to setOf("EVIDENCE_FIRST"), "addressing" to setOf("VOUS", "TU"))
        internal fun parse(v: Map<String, Any?>): IdentityStyles {
            v.exact(allowed.keys)
            val result = v.mapValues { (k, value) -> requireIdentity(value in allowed.getValue(k), "STYLE_VALUE_MISMATCH"); value as String }
            return IdentityStyles(Collections.unmodifiableMap(LinkedHashMap(result)))
        }
    }
}

class IdentityManifest private constructor(val reference: IdentityReference, val constitutionSha256: String,
    val composerRevision: Long, val styles: IdentityStyles) {
    companion object {
        fun decode(bytes: ByteArray, expectedDigest: String): IdentityManifest {
            val v = IdentityCodec.decode(bytes)
            v.exact(common + setOf("name", "profile", "locale", "constitution_sha256", "composer_revision", "styles"))
            header(v, "manifest")
            requireIdentity(v["name"] == "JARVIS" && v["profile"] == "B" && v["locale"] == "fr-FR", "MANIFEST_PROFILE")
            requireIdentity(IdentityCodec.canonical(v).size <= 4096, "MANIFEST_LIMIT")
            requireIdentity(IdentityCodec.digest(v) == hash(expectedDigest), "MANIFEST_DIGEST_MISMATCH")
            return IdentityManifest(IdentityReference(v.s("id"), v.n("revision", 1), expectedDigest),
                hash(v.s("constitution_sha256")), v.n("composer_revision", 1), IdentityStyles.parse(v.map("styles")))
        }
    }
}

class IdentitySnapshot private constructor(val reference: IdentityReference, val sessionId: String,
    val manifestRef: IdentityReference, val policyRevision: Long, val composerRevision: Long,
    val styles: IdentityStyles, val createdMs: Long, val expiresMs: Long) {
    companion object {
        fun decode(bytes: ByteArray): IdentitySnapshot {
            val v = IdentityCodec.decode(bytes)
            v.exact(common + setOf("session_id", "manifest_ref", "policy_revision", "consent_revision", "composer_revision",
                "jarvis_preferences", "personal_preferences", "consents", "styles", "created_ms", "expires_ms"))
            header(v, "snapshot")
            for (field in listOf("jarvis_preferences", "personal_preferences", "consents"))
                requireIdentity(v[field] is List<*> && (v[field] as List<*>).isEmpty(), "ID002_READ_ONLY_SCOPE")
            requireIdentity(v.n("consent_revision") == 0L, "ID002_READ_ONLY_SCOPE")
            val created = v.n("created_ms"); val expires = v.n("expires_ms", 1)
            requireIdentity(expires > created, "INVALID_LIFETIME")
            return IdentitySnapshot(IdentityReference(v.s("id"), v.n("revision", 1), IdentityCodec.digest(v)),
                uuid(v.s("session_id")), IdentityReference.parse(v.map("manifest_ref")), v.n("policy_revision", 1),
                v.n("composer_revision", 1), IdentityStyles.parse(v.map("styles")), created, expires)
        }
    }
}
