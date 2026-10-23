package com.stark.jarvislocal.identity

import com.stark.jarvislocal.identity.profiles.*
import java.util.UUID

enum class IdentityChangeState { PENDING, APPROVED, REJECTED }
data class IdentityChangeMetadata(val proposalId: String, val state: IdentityChangeState, val resulting: ProfileReference?)
class IdentityChangeProposal internal constructor(val id: String, val expected: ProfileReference,
    val draft: ProfileDraft, val preview: List<LanguagePreview>, val differences: List<ProfileDifference>,
    val approvalDigest: String, internal val expiresElapsedMs: Long)

/** Trusted owner editor; no model-supplied approvals. Rejected/pending payloads stay in bounded RAM. */
class IdentityChangeJournal(private val repository: ProfileRepository,
    private val elapsedMs: () -> Long = android.os.SystemClock::elapsedRealtime) {
    private val pending = linkedMapOf<String, IdentityChangeProposal>()
    private val decisions = ArrayDeque<IdentityChangeMetadata>()
    @Synchronized fun propose(expected: ProfileReference, replacement: ProfileDraft): IdentityChangeProposal {
        require(pending.size < 16) { "PENDING_CHANGE_QUOTA" }
        val before = repository.read(expected.id)
        if (before.reference != expected) throw ProfileRejected("REVISION_CONFLICT")
        require(expected.revision < 64) { "REVISION_QUOTA" }
        val encoded = ProfileCodec.encode(expected.id, expected.revision + 1, replacement)
        val nonce = UUID.randomUUID().toString()
        val digest = IdentityCodec.digest(mapOf("proposal" to nonce, "base" to expected.sha256,
            "next" to ProfileCodec.digest(encoded)))
        val now = elapsedMs(); require(now >= 0 && now <= Long.MAX_VALUE - 90_000)
        val proposal = IdentityChangeProposal(nonce, expected, replacement,
            PersonaContext.entries.map { TemplateRealizer().preview(replacement, it) },
            DiffService.compare(before.draft, replacement), digest, now + 90_000)
        pending[nonce] = proposal
        record(IdentityChangeMetadata(nonce, IdentityChangeState.PENDING, null))
        return proposal
    }
    @Synchronized fun approve(id: String, exactPreviewDigest: String): IdentityProfile {
        val proposal = pending[id] ?: throw ProfileRejected("PROPOSAL_ABSENT_OR_CONSUMED")
        if (exactPreviewDigest != proposal.approvalDigest) throw ProfileRejected("PREVIEW_MISMATCH")
        val now = elapsedMs()
        if (now < proposal.expiresElapsedMs - 90_000 || now >= proposal.expiresElapsedMs) {
            pending.remove(id); throw ProfileRejected("PROPOSAL_EXPIRED")
        }
        val committed = repository.revise(proposal.expected, proposal.draft)
        pending.remove(id)
        record(IdentityChangeMetadata(id, IdentityChangeState.APPROVED, committed.reference))
        return committed
    }
    @Synchronized fun reject(id: String) {
        if (pending.remove(id) == null) throw ProfileRejected("PROPOSAL_ABSENT_OR_CONSUMED")
        record(IdentityChangeMetadata(id, IdentityChangeState.REJECTED, null))
    }
    @Synchronized fun revokePending() { pending.keys.toList().forEach(::reject) }
    @Synchronized fun journal(): List<IdentityChangeMetadata> = decisions.toList()
    fun revisions(profileId: String): List<ProfileRevisionInfo> = repository.history(profileId)
    private fun record(entry: IdentityChangeMetadata) {
        decisions.addLast(entry); while (decisions.size > 64) decisions.removeFirst()
    }
}
