package com.stark.jarvislocal

import java.io.File
import java.io.RandomAccessFile

/** Recorder format: mono, 16 kHz, signed 16 bit, 44-byte header. */
internal object WavFiles {
    fun clearAbandonedRecordings(cacheDirectory: File) {
        cacheDirectory.listFiles()?.filter {
            it.isFile && (it.name == "jarvis_input.wav" || (it.name.startsWith("jarvis_input_") && it.name.endsWith(".wav")))
        }?.forEach(::delete)
    }

    fun delete(file: File) {
        check(!file.exists() || file.delete()) { "Impossible de supprimer l’audio temporaire" }
    }

    fun writeHeader(file: RandomAccessFile, dataBytes: Long) {
        require(dataBytes in 0..(0xffffffffL - 36) && dataBytes % 2 == 0L) { "Taille audio invalide" }
        file.seek(0)
        fun int(value: Long) = file.write(byteArrayOf(value.toByte(), (value shr 8).toByte(), (value shr 16).toByte(), (value shr 24).toByte()))
        fun short(value: Int) = file.write(byteArrayOf(value.toByte(), (value shr 8).toByte()))
        file.writeBytes("RIFF"); int(36 + dataBytes); file.writeBytes("WAVEfmt "); int(16)
        short(1); short(1); int(16000); int(32000); short(2); short(16)
        file.writeBytes("data"); int(dataBytes)
    }

    fun validate(file: File) {
        require(file.isFile && file.length() >= 46) { "Enregistrement WAV absent ou vide" }
        RandomAccessFile(file, "r").use { input ->
            fun tag(): String = ByteArray(4).also(input::readFully).toString(Charsets.US_ASCII)
            fun int(): Long = (0..3).fold(0L) { value, index -> value or (input.readUnsignedByte().toLong() shl (8 * index)) }
            fun short(): Int = input.readUnsignedByte() or (input.readUnsignedByte() shl 8)
            require(tag() == "RIFF" && int() == file.length() - 8 && tag() == "WAVE" && tag() == "fmt ") { "En-tête WAV invalide" }
            require(int() == 16L && short() == 1 && short() == 1 && int() == 16000L && int() == 32000L && short() == 2 && short() == 16) { "Format WAV incompatible" }
            require(tag() == "data") { "Données WAV absentes" }
            val size = int()
            require(size == file.length() - 44 && size > 0 && size % 2 == 0L) { "Enregistrement WAV incomplet" }
        }
    }
}
