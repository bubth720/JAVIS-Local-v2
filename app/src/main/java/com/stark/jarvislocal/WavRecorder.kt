package com.stark.jarvislocal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class WavRecorder(private val context: Context) {
    private val recording = AtomicBoolean(false)
    private var worker: Thread? = null

    fun isRecording(): Boolean = recording.get()

    fun start(file: File) {
        require(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        ) { "Permission micro absente" }

        if (recording.getAndSet(true)) return
        worker = thread(name = "JarvisRecorder") {
            val sampleRate = 16000
            val minBuffer = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuffer * 2
            )

            val raf = RandomAccessFile(file, "rw")
            raf.setLength(0)
            writeHeader(raf, sampleRate, 0)
            var bytes = 0L
            val buffer = ByteArray(minBuffer)

            try {
                recorder.startRecording()
                while (recording.get()) {
                    val n = recorder.read(buffer, 0, buffer.size)
                    if (n > 0) {
                        raf.write(buffer, 0, n)
                        bytes += n
                    }
                }
            } finally {
                runCatching { recorder.stop() }
                recorder.release()
                raf.seek(0)
                writeHeader(raf, sampleRate, bytes)
                raf.close()
            }
        }
    }

    fun stop() {
        recording.set(false)
        worker?.join(2000)
        worker = null
    }

    private fun writeHeader(raf: RandomAccessFile, sampleRate: Int, dataBytes: Long) {
        val byteRate = sampleRate * 2
        fun leInt(v: Long) = raf.write(
            byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte())
        )
        fun leShort(v: Int) = raf.write(byteArrayOf(v.toByte(), (v shr 8).toByte()))

        raf.writeBytes("RIFF")
        leInt(36 + dataBytes)
        raf.writeBytes("WAVEfmt ")
        leInt(16)
        leShort(1)
        leShort(1)
        leInt(sampleRate.toLong())
        leInt(byteRate.toLong())
        leShort(2)
        leShort(16)
        raf.writeBytes("data")
        leInt(dataBytes)
    }
}
