package com.stark.jarvislocal

import android.content.Context
import java.io.File
import java.io.InputStream

class ModelManager internal constructor(private val modelDir: File, private val openAsset: (String) -> InputStream) {
    constructor(context: Context) : this(File(context.applicationContext.filesDir, "models"), assetOpener(context.applicationContext))
    init { modelDir.mkdirs() }

    val llmFile: File get() = File(modelDir, "jarvis.gguf")
    val whisperFile: File get() = File(modelDir, "whisper.bin")

    @Synchronized
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
        openAsset(assetPath).use { input ->
            tmp.outputStream().buffered(1024 * 1024).use { output ->
                input.copyTo(output, 1024 * 1024)
            }
        }
        if (dst.exists()) dst.delete()
        check(tmp.renameTo(dst)) { "Impossible d'installer " + dst.name }
    }
}

private fun assetOpener(applicationContext: Context): (String) -> InputStream =
    { path -> applicationContext.assets.open(path) }
