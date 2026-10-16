package com.stark.jarvislocal

/** Test seam for the existing local engine; no remote implementation. */
interface LocalLanguageModel {
    suspend fun warmUp(settings: JarvisSettings)
    suspend fun generate(prompt: String, systemPrompt: String, settings: JarvisSettings): String
    fun isLoaded(): Boolean
}
