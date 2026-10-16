package com.stark.jarvislocal

data class GenerationLimits(val contextSize: Int = 2048, val maxTokens: Int = 256)
data class InferenceSource(val documentId: Long, val chunkId: Long, val documentName: String, val chunkIndex: Int)
data class InferenceRequest(
    val sessionId: String,
    val requestId: String,
    val systemPrompt: String,
    val question: String,
    val context: String,
    val sources: List<InferenceSource> = emptyList(),
    val limits: GenerationLimits = GenerationLimits(),
    val contractVersion: Int = 1
) {
    fun renderPrompt(): String = context + "UTILISATEUR: $question\nASSISTANT:"
    fun settings(): JarvisSettings = JarvisSettings(systemPrompt = systemPrompt,
        contextSize = limits.contextSize, maxTokens = limits.maxTokens)
}

data class InferenceCapabilities(
    val contractVersion: Int = 1,
    val separateSystemPrompt: Boolean = true,
    val maxContextSize: Int = 2048,
    val maxGenerationTokens: Int = 256
)
data class EngineCompletion(val nativeDone: Boolean, val modelUnloaded: Boolean)
data class InferenceResponse(val sessionId: String, val requestId: String, val text: String, val nativeDone: Boolean = true)
enum class StopResult { REQUESTED, UNSUPPORTED, NOT_RUNNING }
enum class UnloadResult { CONFIRMED, UNKNOWN, FAILED }
enum class InferenceRefusal { BUSY, CONTRACT, SYSTEM_PROMPT, BUDGET, STALE_RESPONSE, STOP_REQUESTED, RELEASE_UNCONFIRMED, DELEGATE_DISABLED }
class InferenceRejected(val reason: InferenceRefusal) : IllegalStateException("Inférence refusée : $reason")

/** An exception alone does not prove that a backend has stopped. */
class InferenceFailure(message: String, val nativeDone: Boolean, cause: Throwable? = null,
    val releaseUnconfirmed: Boolean = false) : IllegalStateException(message, cause)

internal fun validateInference(request: InferenceRequest, capabilities: InferenceCapabilities, prompt: String = request.renderPrompt()) {
    if (request.contractVersion != 1 || request.contractVersion != capabilities.contractVersion || request.sessionId.isBlank() || request.requestId.isBlank())
        throw InferenceRejected(InferenceRefusal.CONTRACT)
    if (!capabilities.separateSystemPrompt) throw InferenceRejected(InferenceRefusal.SYSTEM_PROMPT)
    val limits = request.limits
    val bytes = prompt.toByteArray(Charsets.UTF_8).size.toLong() + request.systemPrompt.toByteArray(Charsets.UTF_8).size
    if (limits.contextSize !in 1..capabilities.maxContextSize || limits.maxTokens !in 1..capabilities.maxGenerationTokens ||
        bytes + limits.maxTokens + 128L > limits.contextSize)
        throw InferenceRejected(InferenceRefusal.BUDGET)
}
