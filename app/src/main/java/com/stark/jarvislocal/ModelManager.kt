package com.stark.jarvislocal

import android.content.Context
import java.io.File

class ModelManager(private val context: Context) {
    private val modelDir = File(context.filesDir, "models").apply { mkdirs() }

    val llmFile: File get() = File(modelDir, "jarvis.gguf")
    val whisperFile: File get() = File(modelDir, "whisper.bin")

    fun ensureBundledModels(onProgress: ((String) -> Unit)? = null) {
        copyAssetIfNeeded("models/jarvis.gguf", llmFile, onProgress)
        copyAssetIfNeeded("models/whisper.bin", whisperFile, onProgress)
    }

    fun hasLlm(): Boolean = llmFile.exists() && llmFile.length() > 100L * 1024 * 1024
    fun hasWhisper(): Boolean = whisperFile.exists() && whisperFile.length() > 50L * 1024 * 1024

    private fun copyAssetIfNeeded(assetPath: String, dst: File, onProgress: ((String) -> Unit)?) {
        if (dst.exists() && dst.length() > 1024 * 1024) return
        val tmp = File(dst.parentFile, dst.name + ".part")
        onProgress?.invoke("Préparation de " + dst.name + "…")
        context.assets.open(assetPath).use { input ->
            tmp.outputStream().buffered(1024 * 1024).use { output ->
                input.copyTo(output, 1024 * 1024)
            }
        }
        if (dst.exists()) dst.delete()
        check(tmp.renameTo(dst)) { "Impossible d'installer " + dst.name }
    }
}
