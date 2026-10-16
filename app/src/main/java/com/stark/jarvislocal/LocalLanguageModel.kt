package com.stark.jarvislocal

/** Test seam for the existing local engine; no remote implementation. */
interface LocalLanguageModel {
    suspend fun warmUp(settings: JarvisSettings)
    suspend fun generate(prompt: String, systemPrompt: String, settings: JarvisSettings): String
    fun isLoaded(): Boolean

    val capabilities: InferenceCapabilities get() = InferenceCapabilities()

    suspend fun prepare(settings: JarvisSettings): EngineCompletion {
        warmUp(settings)
        // isLoaded=false is not evidence that allocated resources have been released.
        return EngineCompletion(nativeDone = true, modelUnloaded = false)
    }

    /** Compatibility bridge: system remains a distinct argument to the historical engine. */
    suspend fun infer(request: InferenceRequest): InferenceResponse {
        validateInference(request, capabilities)
        return InferenceResponse(request.sessionId, request.requestId,
            generate(request.renderPrompt(), request.systemPrompt, request.settings()))
    }

    fun requestStop(): StopResult = StopResult.UNSUPPORTED

    // Existing implementations without a release contract must never authorize switching.
    suspend fun unload(): UnloadResult = UnloadResult.UNKNOWN
    fun openSession(sessionId: String) { }
}
