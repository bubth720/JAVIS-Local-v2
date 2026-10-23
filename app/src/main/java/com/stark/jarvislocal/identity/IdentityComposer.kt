package com.stark.jarvislocal.identity

import com.stark.jarvislocal.InferenceRequest
import java.util.Collections

/** Must be supplied by the real engine on its own admitted worker/lease, with the same renderer as generate. */
interface IdentityTokenBudgetPort {
    fun measure(request: InferenceRequest): IdentityTokenMeasurement
}

data class IdentityTokenMeasurement(val requestDigest: String, val modelSha256: String, val templateSha256: String,
    val renderedSha256: String, val inputTokens: Long, val loadedContextTokens: Long, val admittedContextTokens: Long,
    val guardTokens: Long, val nativeVerified: Boolean)

object UnavailableIdentityTokenizer : IdentityTokenBudgetPort {
    override fun measure(request: InferenceRequest): IdentityTokenMeasurement = throw IdentityRejected("TOKENIZER_UNAVAILABLE")
}

/** A checked composition, NOT a generation permit, native completion or resource release receipt. */
data class IdentityComposition(val request: InferenceRequest, val snapshotRef: IdentityReference,
    val measurement: IdentityTokenMeasurement)

class IdentityComposer(private val policy: IdentityPolicy,
    private val tokenizer: IdentityTokenBudgetPort = UnavailableIdentityTokenizer,
    private val clock: () -> Long = System::currentTimeMillis) {
    companion object {
        /** Bind every request field, including source provenance, to the measurement (no user payload in logs). */
        fun requestDigest(r: InferenceRequest): String = IdentityCodec.digest(mapOf(
            "session_id" to r.sessionId, "request_id" to r.requestId, "system" to r.systemPrompt,
            "question" to r.question, "context" to r.context, "version" to r.contractVersion,
            "context_tokens" to r.limits.contextSize, "output_tokens" to r.limits.maxTokens,
            "sources" to r.sources.map { mapOf("document_id" to it.documentId, "chunk_id" to it.chunkId,
                "document_name" to it.documentName, "chunk_index" to it.chunkIndex) }))
    }
    fun compose(request: InferenceRequest, manifest: IdentityManifest, snapshot: IdentitySnapshot,
        separateSystemPrompt: Boolean): IdentityComposition {
        policy.validate(manifest, snapshot, request.sessionId, clock()); policy.requireSafeInference()
        requireIdentity(separateSystemPrompt, "CAPABILITY_UNSUPPORTED")
        requireIdentity(request.contractVersion == 1 && request.requestId.isNotBlank() &&
            request.question.isNotBlank() && request.limits.contextSize in 1..2048 && request.limits.maxTokens in 1..256, "INVALID_REQUEST")
        val fragment = IdentityStyleFormatter.render(snapshot.styles)
        requireIdentity(IdentityCodec.utf8(fragment).size <= 2048, "PERSONA_LIMIT")
        val candidate = request.copy(systemPrompt = request.systemPrompt + "\n\n" + fragment,
            sources = Collections.unmodifiableList(request.sources.toList()))
        requireIdentity(IdentityCodec.utf8(candidate.systemPrompt).size <= 8192, "SYSTEM_LIMIT")
        val expectedDigest = requestDigest(candidate)
        val measured = tokenizer.measure(candidate)
        requireIdentity(measured.nativeVerified, "TOKENIZER_UNAVAILABLE")
        requireIdentity(measured.requestDigest == expectedDigest, "MEASUREMENT_MISMATCH")
        hash(measured.modelSha256); hash(measured.templateSha256); hash(measured.renderedSha256)
        requireIdentity(listOf(measured.inputTokens, measured.loadedContextTokens, measured.admittedContextTokens,
            measured.guardTokens).all { it in 0..IdentityCodec.MAX_INTEGER }, "INVALID_MEASUREMENT")
        val effective = minOf(request.limits.contextSize.toLong(), measured.loadedContextTokens, measured.admittedContextTokens)
        requireIdentity(measured.inputTokens + request.limits.maxTokens.toLong() + measured.guardTokens <= effective, "CONTEXT_EXCEEDED")
        // Revocation/cancel during measurement cannot release an old snapshot into the next conversation.
        policy.validate(manifest, snapshot, request.sessionId, clock()); policy.requireSafeInference()
        return IdentityComposition(candidate, snapshot.reference, measured)
    }
}
