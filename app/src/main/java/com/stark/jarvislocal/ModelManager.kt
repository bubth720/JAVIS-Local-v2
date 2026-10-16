package com.stark.jarvislocal

import android.content.Context
import java.io.File
import java.io.InputStream
import java.io.FileOutputStream
import java.io.IOException

class ModelManager internal constructor(
    private val modelDir: File,
    private val assetLength: (String) -> Long,
    private val minimumBytes: (String) -> Long = { if (it.endsWith(".gguf")) 100L * 1024 * 1024 else 50L * 1024 * 1024 },
    private val replace: (File, File) -> Boolean = { from, to -> from.renameTo(to) },
    private val openAsset: (String) -> InputStream
) {
    constructor(context: Context) : this(
        File(context.applicationContext.filesDir, "models"),
        { path -> context.applicationContext.assets.openFd(path).use { it.length } },
        openAsset = assetOpener(context.applicationContext)
    )
    init { check(modelDir.isDirectory || modelDir.mkdirs()) { "Stockage des modèles inaccessible" } }

    val llmFile: File get() = File(modelDir, "jarvis.gguf")
    val whisperFile: File get() = File(modelDir, "whisper.bin")

    @Synchronized
    fun ensureBundledModels(onProgress: ((String) -> Unit)? = null) {
        copyAssetIfNeeded("models/jarvis.gguf", llmFile, onProgress)
        copyAssetIfNeeded("models/whisper.bin", whisperFile, onProgress)
    }

    fun hasLlm(): Boolean = isComplete("models/jarvis.gguf", llmFile)
    fun hasWhisper(): Boolean = isComplete("models/whisper.bin", whisperFile)

    private fun isComplete(path: String, file: File): Boolean = try {
        validate(path, file, assetLength(path))
        true
    } catch (e: IOException) { false } catch (e: IllegalArgumentException) { false }

    private fun validate(path: String, file: File, expected: Long) {
        require(expected > minimumBytes(path) && file.isFile && file.length() == expected) { "Modèle incomplet : ${file.name}" }
        val magic = if (path.endsWith(".gguf")) byteArrayOf(0x47, 0x47, 0x55, 0x46) else byteArrayOf(0x6c, 0x6d, 0x67, 0x67)
        file.inputStream().use { input ->
            require(magic.all { input.read() == (it.toInt() and 255) }) { "Format de modèle invalide : ${file.name}" }
        }
    }

    private fun copyAssetIfNeeded(assetPath: String, dst: File, onProgress: ((String) -> Unit)?) {
        val expected = assetLength(assetPath)
        if (isComplete(assetPath, dst)) return
        require(expected > minimumBytes(assetPath)) { "Asset de modèle incomplet : ${dst.name}" }
        val tmp = File(dst.parentFile, dst.name + ".part")
        onProgress?.invoke("Préparation de " + dst.name + "…")
        try {
            openAsset(assetPath).use { input ->
                FileOutputStream(tmp).use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    var copied = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) throw IOException("Lecture du modèle interrompue")
                        copied += count
                        require(copied <= expected) { "Taille de modèle inattendue" }
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                }
            }
            validate(assetPath, tmp, expected)
            // Same-directory rename replaces atomically on Android/Linux. Never delete dst first.
            check(replace(tmp, dst)) { "Impossible d'installer ${dst.name}" }
        } finally {
            // A process death may leave .part; the next attempt starts again, never appends.
            check(!tmp.exists() || tmp.delete()) { "Nettoyage du modèle temporaire impossible" }
        }
    }
}

private fun assetOpener(applicationContext: Context): (String) -> InputStream =
    { path -> applicationContext.assets.open(path) }
