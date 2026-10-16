package com.stark.jarvislocal

import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.math.max

internal interface LlamaBackend {
    suspend fun load(path: String, settings: JarvisSettings): Any
    suspend fun complete(model: Any, prompt: String, systemPrompt: String, settings: JarvisSettings): String
    fun release(model: Any)
}

/** The historical model remains loaded between messages. No IPC or native stop API. */
class LlamaLocalEngine internal constructor(
    private val modelReady: () -> Boolean,
    private val modelPath: String,
    private val backend: LlamaBackend
) : LocalLanguageModel {
    constructor(modelManager: ModelManager) : this({ modelManager.hasLlm() }, modelManager.llmFile.absolutePath, AndroidLlamaBackend())

    private val lock = Mutex()
    @Volatile private var model: Any? = null
    @Volatile private var releaseUncertain = false
    private var loadedContextSize = 0

    override suspend fun warmUp(settings: JarvisSettings) {
        withContext(NonCancellable) { lock.withLock { ensureLoaded(settings) } }
    }

    override suspend fun generate(prompt: String, systemPrompt: String, settings: JarvisSettings): String =
        withContext(NonCancellable) { lock.withLock {
            val active = ensureLoaded(settings)
            try { backend.complete(active, prompt, systemPrompt, settings).trim() }
            catch (error: Throwable) {
                if (error is OutOfMemoryError) {
                    try { releaseInternal() } catch (release: InferenceFailure) { throw release }
                    throw InferenceFailure(
                        "Mémoire insuffisante pour le modèle actuel. Ferme les applications en arrière-plan puis relance JARVIS.",
                        nativeDone = true, cause = error)
                }
                throw InferenceFailure("Échec de génération locale", nativeDone = true, cause = error)
            }
        } }

    override fun isLoaded(): Boolean = model != null && !releaseUncertain
    override fun requestStop(): StopResult = StopResult.UNSUPPORTED

    override suspend fun unload(): UnloadResult = withContext(NonCancellable) { lock.withLock {
        if (releaseUncertain) return@withLock UnloadResult.UNKNOWN
        try { releaseInternal(); UnloadResult.CONFIRMED }
        catch (error: InferenceFailure) { UnloadResult.FAILED }
    } }

    private suspend fun ensureLoaded(settings: JarvisSettings): Any {
        if (releaseUncertain) throw InferenceFailure("Libération du modèle non attestée ; redémarrage du processus nécessaire",
            nativeDone = true, releaseUnconfirmed = true)
        if (!modelReady()) throw InferenceFailure("Le modèle jarvis.gguf n'est pas prêt.", nativeDone = true)
        val existing = model
        if (existing != null && loadedContextSize == settings.contextSize) return existing
        releaseInternal()
        val loaded = try { backend.load(modelPath, settings) }
        catch (error: Throwable) {
            // A failed load provides no handle with which to attest resource release.
            releaseUncertain = true
            throw InferenceFailure("Chargement du modèle échoué ; ressources non attestées", nativeDone = true,
                cause = error, releaseUnconfirmed = true)
        }
        model = loaded
        loadedContextSize = settings.contextSize
        return loaded
    }

    private fun releaseInternal() {
        model?.let {
            try { backend.release(it) }
            catch (error: Throwable) {
                releaseUncertain = true
                // Keep the handle: an exception is not proof of release.
                throw InferenceFailure("Déchargement du modèle échoué ; changement de moteur interdit", nativeDone = true,
                    cause = error, releaseUnconfirmed = true)
            }
        }
        model = null
        loadedContextSize = 0
    }
}

private class AndroidLlamaBackend : LlamaBackend {
    override suspend fun load(path: String, settings: JarvisSettings): Any = Llama.loadModel(modelPath = path,
        config = LlamaConfig(contextSize = settings.contextSize, threads = max(2, Runtime.getRuntime().availableProcessors().coerceAtMost(6))))
    override suspend fun complete(model: Any, prompt: String, systemPrompt: String, settings: JarvisSettings): String =
        Llama.complete(model = model as LlamaModel, prompt = prompt, systemPrompt = systemPrompt, maxTokens = settings.maxTokens).text
    override fun release(model: Any) = Llama.releaseModel(model as LlamaModel)
}
