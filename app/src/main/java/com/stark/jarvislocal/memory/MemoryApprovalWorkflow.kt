package com.stark.jarvislocal.memory

import com.stark.jarvislocal.identity.IdentityCodec
import java.security.MessageDigest
import java.util.UUID

enum class MemoryScope { GENERAL, PREFERENCE, SENSITIVE }
enum class MemoryDecision { ACCEPT, REJECT, REPLACE }
data class MemoryReference(val id: String, val revision: Long, val contentDigest: String)
data class ConsentReceipt(val id: String, val nonce: String, val sessionId: String, val contentDigest: String, val confirmedAtMs: Long)
data class MemoryRecord(val reference: MemoryReference, val content: String, val scope: MemoryScope,
    val preferenceKey: String?, val createdAtMs: Long, val expiresAtMs: Long?, val suspended: Boolean,
    val consent: ConsentReceipt, val source: String = "USER_EXPLICIT")
class PendingMemoryCandidate internal constructor(val id: String, val sessionId: String,
    val content: String, val scope: MemoryScope, val preferenceKey: String?,
    val replaces: MemoryReference?, val createdElapsedMs: Long, val expiresElapsedMs: Long, val expiresAtMs: Long?,
    internal val sessionGeneration: Long, internal val sessionToken: java.util.concurrent.atomic.AtomicLong)
sealed class MemoryApprovalResult {
    data class Confirmed(val record: MemoryRecord) : MemoryApprovalResult()
    data class Conflict(val current: MemoryReference) : MemoryApprovalResult()
    data class Refused(val code: String) : MemoryApprovalResult()
    data object Rejected : MemoryApprovalResult()
}
class MemoryRejected(val reason: String) : IllegalStateException(reason)
internal fun contentDigest(text: String): String = MessageDigest.getInstance("SHA-256")
    .digest(IdentityCodec.utf8(text)).joinToString("") { "%02x".format(it.toInt() and 255) }
internal fun validateContent(content: String) {
    require(content.isNotBlank() && content.length <= 1500 && IdentityCodec.utf8(content).size <= 6000)
    require(content.none { it.code == 127 || it.code < 32 && it !in "\n\r\t" })
}

/** Trusted in-process UI workflow. A model/document cannot create an approval by declaring a flag. */
class MemoryApprovalWorkflow(private val store: ConfirmedMemoryStore,
    private val elapsedMs: () -> Long = android.os.SystemClock::elapsedRealtime,
    private val wallMs: () -> Long = System::currentTimeMillis) {
    private val pending = java.util.concurrent.ConcurrentHashMap<String, PendingMemoryCandidate>()
    private val sessions = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicLong>()
    @Synchronized fun propose(sessionId: String, exactContent: String, scope: MemoryScope = MemoryScope.GENERAL,
        preferenceKey: String? = null, edit: MemoryReference? = null, expiresAtMs: Long? = null): PendingMemoryCandidate {
        require(UUID.fromString(sessionId).toString() == sessionId); validateContent(exactContent)
        require(preferenceKey == null || preferenceKey.matches(Regex("[a-z][a-z0-9_.-]{0,63}")))
        require(expiresAtMs == null || expiresAtMs > wallMs())
        pruneExpired()
        require(pending.size < 32) { "PENDING_QUOTA" }
        require(sessions.containsKey(sessionId) || sessions.size < 64) { "SESSION_QUOTA" }
        val old = if (edit != null) store.readOwned(edit.id).also {
            if (it.reference != edit) throw MemoryRejected("REVISION_CONFLICT")
            require(it.preferenceKey == preferenceKey) { "PREFERENCE_KEY_CHANGE_NOT_SUPPORTED" }
        } else preferenceKey?.let(store::findPreference)
        val epoch = sessions.computeIfAbsent(sessionId) { java.util.concurrent.atomic.AtomicLong() }
        val generation = epoch.get()
        val now = elapsedMs(); require(now >= 0 && now <= Long.MAX_VALUE - 90_000)
        val candidate = PendingMemoryCandidate(UUID.randomUUID().toString(), sessionId, exactContent, scope,
            preferenceKey, old?.reference, now, now + 90_000, expiresAtMs,
            generation, epoch)
        pending[candidate.id] = candidate
        return candidate
    }
    @Synchronized fun decide(candidateId: String, sessionId: String, exactDisplayedContent: String,
        decision: MemoryDecision): MemoryApprovalResult {
        val candidate = pending[candidateId] ?: return MemoryApprovalResult.Refused("CANDIDATE_ABSENT_OR_CONSUMED")
        if (sessionId != candidate.sessionId) return MemoryApprovalResult.Refused("SESSION_MISMATCH")
        val now = elapsedMs()
        if (now < candidate.createdElapsedMs || now >= candidate.expiresElapsedMs) {
            pending.remove(candidateId); return MemoryApprovalResult.Refused("CONSENT_EXPIRED")
        }
        if (exactDisplayedContent != candidate.content) return MemoryApprovalResult.Refused("PREVIEW_MISMATCH")
        if (decision == MemoryDecision.REJECT) {
            pending.remove(candidateId)
            return MemoryApprovalResult.Rejected // no personal content or rejected candidate is persisted
        }
        if (candidate.replaces != null && decision != MemoryDecision.REPLACE) return MemoryApprovalResult.Conflict(candidate.replaces)
        if (candidate.replaces == null && decision == MemoryDecision.REPLACE) return MemoryApprovalResult.Refused("NO_REPLACEMENT_TARGET")
        return try {
            val record = store.confirm(candidate, wallMs()) {
                if (sessions[candidate.sessionId] !== candidate.sessionToken || candidate.sessionToken.get() != candidate.sessionGeneration || pending[candidate.id] !== candidate)
                    throw MemoryRejected("SESSION_REVOKED")
                val instant = elapsedMs()
                if (instant < candidate.createdElapsedMs || instant >= candidate.expiresElapsedMs) throw MemoryRejected("CONSENT_EXPIRED")
            }
            pending.remove(candidateId)
            MemoryApprovalResult.Confirmed(record)
        } catch (failure: MemoryRejected) { MemoryApprovalResult.Refused(failure.reason) }
    }
    fun revokeSession(sessionId: String) {
        val generation = sessions[sessionId] ?: return
        generation.incrementAndGet()
        pending.entries.removeAll { it.value.sessionId == sessionId }
        sessions.remove(sessionId, generation)
    }
    @Synchronized private fun pruneExpired() {
        val now = elapsedMs()
        pending.entries.removeAll { now < it.value.createdElapsedMs || now >= it.value.expiresElapsedMs }
    }
}

data class MemoryView(val reference: MemoryReference, val content: String, val scope: MemoryScope)
class ProjectionPolicy(allowed: Set<MemoryScope> = emptySet()) {
    internal val allowed = allowed.toSet()
}
fun interface MemoryLookupPort { fun recent(policy: ProjectionPolicy, limit: Int): List<MemoryView> }
class ConfirmedMemoryLookup(private val store: ConfirmedMemoryStore,
    private val wallMs: () -> Long = System::currentTimeMillis) : MemoryLookupPort {
    override fun recent(policy: ProjectionPolicy, limit: Int): List<MemoryView> {
        require(limit in 1..10)
        if (policy.allowed.isEmpty()) return emptyList()
        return store.listOwned().filter {
            it.scope != MemoryScope.SENSITIVE && it.scope in policy.allowed && !it.suspended && wallMs() >= it.createdAtMs && (it.expiresAtMs == null || wallMs() < it.expiresAtMs)
        }.take(limit).map { MemoryView(it.reference, it.content, it.scope) }
    }
}
