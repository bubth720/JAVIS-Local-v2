package com.stark.jarvislocal

import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Le modèle reste chargé en mémoire entre les messages.
 * La V2 rechargeait ~1,2 Go à chaque requête, ce qui provoquait le blocage apparent.
 */
class LlamaLocalEngine(private val modelManager: ModelManager) : LocalLanguageModel {
    private val lock = Mutex()
    @Volatile private var model: LlamaModel? = null
    private var loadedContextSize: Int = 0

    override suspend fun warmUp(settings: JarvisSettings) {
        withContext(NonCancellable) { lock.withLock { ensureLoaded(settings) } }
    }

    override suspend fun generate(
        prompt: String,
        systemPrompt: String,
        settings: JarvisSettings
    ): String = withContext(NonCancellable) { lock.withLock {
        val activeModel = ensureLoaded(settings)
        try {
            Llama.complete(
                model = activeModel,
                prompt = prompt,
                systemPrompt = systemPrompt,
                maxTokens = settings.maxTokens
            ).text.trim()
        } catch (e: Throwable) {
            if (e is OutOfMemoryError) {
                releaseInternal()
                throw IllegalStateException(
                    "Mémoire insuffisante pour le modèle actuel. Ferme les applications en arrière-plan puis relance JARVIS."
                )
            }
            throw e
        }
    } }

    override fun isLoaded(): Boolean = model != null

    private suspend fun ensureLoaded(settings: JarvisSettings): LlamaModel {
        require(modelManager.hasLlm()) { "Le modèle jarvis.gguf n'est pas prêt." }

        val existing = model
        if (existing != null && loadedContextSize == settings.contextSize) return existing

        releaseInternal()
        val loaded = Llama.loadModel(
            modelPath = modelManager.llmFile.absolutePath,
            config = LlamaConfig(
                contextSize = settings.contextSize,
                threads = recommendedThreads()
            )
        )
        model = loaded
        loadedContextSize = settings.contextSize
        return loaded
    }

    private fun releaseInternal() {
        model?.let { runCatching { Llama.releaseModel(it) } }
        model = null
        loadedContextSize = 0
    }

    private fun recommendedThreads(): Int =
        max(2, Runtime.getRuntime().availableProcessors().coerceAtMost(6))
}
