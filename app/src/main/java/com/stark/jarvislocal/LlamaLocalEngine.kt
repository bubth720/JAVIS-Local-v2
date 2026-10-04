package com.stark.jarvislocal

import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

class LlamaLocalEngine(private val modelManager: ModelManager) {
    private val lock = Mutex()

    suspend fun generate(prompt: String, systemPrompt: String, settings: JarvisSettings): String =
        lock.withLock {
            require(modelManager.hasLlm()) { "Le modèle jarvis.gguf n'est pas prêt." }

            val model = Llama.loadModel(
                modelPath = modelManager.llmFile.absolutePath,
                config = LlamaConfig(
                    contextSize = settings.contextSize,
                    threads = recommendedThreads()
                )
            )
            try {
                Llama.complete(
                    model = model,
                    prompt = prompt,
                    systemPrompt = systemPrompt,
                    maxTokens = settings.maxTokens
                ).text.trim()
            } finally {
                Llama.releaseModel(model)
            }
        }

    private fun recommendedThreads(): Int =
        max(2, Runtime.getRuntime().availableProcessors().coerceAtMost(6))
}
