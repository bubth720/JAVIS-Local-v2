package com.stark.jarvislocal.fixture

import com.stark.jarvislocal.InferenceRequest
import java.util.UUID

class PilotBlocked(val code: String) : IllegalStateException(code)
internal fun requirePilot(condition: Boolean, code: String) { if (!condition) throw PilotBlocked(code) }
internal fun canonicalUuid(value: String): Boolean = try { UUID.fromString(value).toString() == value } catch (e: IllegalArgumentException) { false }

/** CORE admissibility before the exact 003E wire mapping. */
object PilotRequestGate {
    fun validate(request: InferenceRequest) {
        requirePilot(request.contractVersion == 1, "CORE_CONTRACT_UNSUPPORTED")
        requirePilot(canonicalUuid(request.sessionId) && canonicalUuid(request.requestId), "INVALID_CORE_IDS")
        requirePilot(request.context.isEmpty() && request.sources.isEmpty(), "REAL_CONTEXT_UNSUPPORTED")
        requirePilot(request.question.isNotEmpty(), "EMPTY_QUESTION")
        for (text in listOf(request.question, request.systemPrompt)) {
            requirePilot(Charsets.UTF_8.newEncoder().canEncode(text), "INVALID_UTF8")
            requirePilot(text.toByteArray(Charsets.UTF_8).size <= 8192, "INPUT_TOO_LARGE")
        }
        requirePilot(request.limits.contextSize in 512..2048 && request.limits.contextSize % 256 == 0 &&
            request.limits.maxTokens in 1..256, "UNSUPPORTED_LOCAL_LIMITS")
    }
}
