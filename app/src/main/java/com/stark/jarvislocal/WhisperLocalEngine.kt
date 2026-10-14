package com.stark.jarvislocal

import android.content.Context
import dev.ffmpegkit.whisper.Whisper
import dev.ffmpegkit.whisper.WhisperConfig
import dev.ffmpegkit.whisper.WhisperModel
import java.io.File
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

internal interface WhisperBackend {
    suspend fun load(): Any
    suspend fun transcribe(model: Any, audioPath: String): String
    fun release(model: Any)
}

class WhisperLocalEngine internal constructor(
    private val modelAvailable: () -> Boolean,
    private val backend: WhisperBackend
) {
    constructor(context: Context, modelManager: ModelManager) : this(
        { modelManager.hasWhisper() },
        object : WhisperBackend {
            override suspend fun load(): Any = Whisper.loadModel(context.applicationContext, modelManager.whisperFile.absolutePath)
            override suspend fun transcribe(model: Any, audioPath: String): String =
                Whisper.transcribe(model as WhisperModel, audioPath, WhisperConfig(language = "fr")).text
            override fun release(model: Any) = Whisper.releaseModel(model as WhisperModel)
        }
    )

    private val lock = Mutex()

    suspend fun transcribe(audioPath: String): String {
        currentCoroutineContext().ensureActive()
        check(lock.tryLock()) { "Une transcription est déjà active" }
        return try {
            require(modelAvailable()) { "Le modèle whisper.bin n'est pas prêt." }
            WavFiles.validate(File(audioPath))
            withContext(NonCancellable) {
                val model = backend.load()
                try { backend.transcribe(model, audioPath).trim() }
                finally { backend.release(model) }
            }
        } finally { lock.unlock() }
    }
}
