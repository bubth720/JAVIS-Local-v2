package com.stark.jarvislocal

import android.content.Context
import dev.ffmpegkit.whisper.Whisper
import dev.ffmpegkit.whisper.WhisperConfig

class WhisperLocalEngine(
    private val context: Context,
    private val modelManager: ModelManager
) {
    suspend fun transcribe(audioPath: String): String {
        require(modelManager.hasWhisper()) { "Le modèle whisper.bin n'est pas prêt." }
        val model = Whisper.loadModel(context, modelManager.whisperFile.absolutePath)
        return try {
            Whisper.transcribe(
                model,
                audioPath,
                WhisperConfig(language = "fr")
            ).text.trim()
        } finally {
            Whisper.releaseModel(model)
        }
    }
}
