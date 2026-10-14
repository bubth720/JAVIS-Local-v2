package com.stark.jarvislocal

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WavRecorderTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun releaseFailureQuarantinesAudioAndNeverReturnsAValidRecording() = runBlocking {
        val input = FakeAudio().apply { releaseFailure = true }
        val recorder = WavRecorder({ true }, { input })
        val file = temporary.newFile()
        recorder.start(file); input.awaitBlockedRead()
        expectFailure { recorder.stop() }
        expectFailure { recorder.stop(discard = true) }
        expectFailure { recorder.start(temporary.newFile()) }
        assertEquals(RecorderPhase.ERROR, recorder.state.value)
        assertFalse(file.exists())
        assertEquals(1, input.starts)
    }

    @Test fun startupCleanupRemovesOnlyJarvisTemporaryAudio() {
        val legacy = temporary.newFile("jarvis_input.wav")
        val current = temporary.newFile("jarvis_input_123.wav")
        val unrelated = temporary.newFile("user.wav")
        val unrelatedPrefix = temporary.newFile("jarvis_input_notes.txt")
        val directory = temporary.newFolder("jarvis_input_folder.wav")
        WavFiles.clearAbandonedRecordings(temporary.root)
        assertFalse(legacy.exists())
        assertFalse(current.exists())
        assertTrue(unrelated.exists())
        assertTrue(unrelatedPrefix.exists())
        assertTrue(directory.exists())
    }

    @Test fun permissionRevokedDuringDeviceCreationDoesNotLeaveCaptureActive() = runBlocking {
        val recorder = WavRecorder({ true }, { throw SecurityException("permission revoked") })
        val file = temporary.newFile()
        expectFailure { recorder.start(file) }
        recorder.stop(discard = true)
        assertFalse(recorder.isRecording())
        assertFalse(file.exists())
    }

    @Test fun permissionRefusalDoesNotCreateAnAudioDevice() = runBlocking {
        var created = false
        val recorder = WavRecorder({ false }, { created = true; FakeAudio() })
        expectFailure { recorder.start(temporary.newFile()) }
        assertFalse(created)
        assertFalse(recorder.isRecording())
        assertEquals(RecorderPhase.IDLE, recorder.state.value)
    }

    @Test fun unavailableDeviceIsReleasedAndNextAttemptCanSucceed() = runBlocking {
        val unavailable = FakeAudio().apply { initialized = false }
        val next = FakeAudio()
        var attempt = 0
        val recorder = WavRecorder({ true }, { if (attempt++ == 0) unavailable else next })
        val bad = temporary.newFile()
        expectFailure { recorder.start(bad) }
        recorder.stop(discard = true)
        assertEquals(1, unavailable.releases)
        assertFalse(bad.exists())
        val good = temporary.newFile()
        recorder.start(good); next.awaitBlockedRead()
        assertEquals(good, recorder.stop())
        WavFiles.validate(good)
    }

    @Test fun deviceFactoryFailureDoesNotLeaveRecordingFlagSet() = runBlocking {
        val recorder = WavRecorder({ true }, { error("device unavailable") })
        val file = temporary.newFile()
        expectFailure { recorder.start(file) }
        recorder.stop(discard = true)
        assertFalse(recorder.isRecording())
        assertFalse(file.exists())
    }

    @Test fun startFailureReleasesDeviceAndDeletesFile() = runBlocking {
        val input = FakeAudio().apply { startFailure = true }
        val recorder = WavRecorder({ true }, { input })
        val file = temporary.newFile()
        expectFailure { recorder.start(file) }
        recorder.stop(discard = true)
        assertEquals(1, input.releases)
        assertFalse(file.exists())
        assertFalse(recorder.isRecording())
    }

    @Test fun readErrorIsReportedAndDoesNotProduceValidAudio() = runBlocking {
        val input = FakeAudio().apply { firstReadResult = -6 }
        val recorder = WavRecorder({ true }, { input })
        val file = temporary.newFile()
        recorder.start(file)
        expectFailure { recorder.awaitCompletion() }
        recorder.stop(discard = true)
        assertEquals(1, input.releases)
        assertFalse(recorder.isRecording())
        assertFalse(file.exists())
    }

    @Test fun normalStopFinalizesWavAndRepeatedStopIsSafe() = runBlocking {
        val input = FakeAudio()
        val recorder = WavRecorder({ true }, { input })
        val file = temporary.newFile()
        recorder.start(file); input.awaitBlockedRead()
        assertTrue(recorder.isRecording())
        assertEquals(file, recorder.stop())
        assertEquals(file, recorder.stop())
        WavFiles.validate(file)
        assertEquals(48L, file.length())
        assertEquals(1, input.releases)
        assertEquals(RecorderPhase.IDLE, recorder.state.value)
    }

    @Test fun stopWaitsForReleaseAndWriterTerminationBeforeReturningFile() = runBlocking {
        val input = FakeAudio().apply { releaseBarrier = CountDownLatch(1) }
        val recorder = WavRecorder({ true }, { input })
        val file = temporary.newFile()
        recorder.start(file); input.awaitBlockedRead()
        val stopped = async(Dispatchers.IO) { recorder.stop() }
        assertTrue(input.releaseEntered.await(2, TimeUnit.SECONDS))
        assertFalse(stopped.isCompleted)
        expectFailure { recorder.start(temporary.newFile()) }
        input.releaseBarrier!!.countDown()
        assertEquals(file, withTimeout(5_000) { stopped.await() })
        WavFiles.validate(file)
        assertFalse(recorder.isRecording())
    }

    @Test fun aSecondCaptureCannotStartWhileFirstIsActive() = runBlocking {
        val input = FakeAudio()
        val recorder = WavRecorder({ true }, { input })
        recorder.start(temporary.newFile()); input.awaitBlockedRead()
        expectFailure { recorder.start(temporary.newFile()) }
        recorder.stop(discard = true)
        assertEquals(1, input.starts)
    }

    @Test fun discardedCaptureIsDeletedAndNeverReturnedAsValid() = runBlocking {
        val input = FakeAudio()
        val recorder = WavRecorder({ true }, { input })
        val file = temporary.newFile()
        recorder.start(file); input.awaitBlockedRead()
        assertNull(recorder.stop(discard = true))
        assertNull(recorder.stop())
        assertFalse(file.exists())
        assertFalse(recorder.isRecording())
    }

    @Test fun openFileFailureReleasesAudioDevice() = runBlocking {
        val input = FakeAudio()
        val recorder = WavRecorder({ true }, { input }, { throw IOException("storage unavailable") })
        val file = temporary.newFile()
        expectFailure { recorder.start(file) }
        recorder.stop(discard = true)
        assertEquals(1, input.releases)
        assertFalse(file.exists())
    }

    @Test fun writeFailureDeletesIncompleteWavAndReleasesDevice() = runBlocking {
        val input = FakeAudio()
        val recorder = WavRecorder({ true }, { input }, { file ->
            object : RandomAccessFile(file, "rw") {
                override fun write(buffer: ByteArray, offset: Int, count: Int) { throw IOException("write failed") }
            }
        })
        val file = temporary.newFile()
        try { recorder.start(file) } catch (e: IOException) { /* header may fail first */ }
        expectFailure { recorder.awaitCompletion() }
        recorder.stop(discard = true)
        assertFalse(file.exists())
        assertEquals(1, input.releases)
    }

    @Test fun finalizationFailureDoesNotReturnAudioForTranscription() = runBlocking {
        val input = FakeAudio()
        val recorder = WavRecorder({ true }, { input }, { file ->
            object : RandomAccessFile(file, "rw") {
                override fun seek(position: Long) {
                    if (position == 0L && filePointer > 44) throw IOException("header finalization failed")
                    super.seek(position)
                }
            }
        })
        val file = temporary.newFile()
        recorder.start(file); input.awaitBlockedRead()
        expectFailure { recorder.stop() }
        recorder.stop(discard = true)
        assertFalse(file.exists())
        assertEquals(1, input.releases)
    }

    @Test fun emptyCaptureIsNotPresentedAsValidWav() = runBlocking {
        val input = FakeAudio().apply { firstReadResult = 0 }
        val recorder = WavRecorder({ true }, { input })
        val file = temporary.newFile()
        recorder.start(file); input.awaitBlockedRead()
        expectFailure { recorder.stop() }
        recorder.stop(discard = true)
        assertFalse(file.exists())
    }

    private suspend fun expectFailure(block: suspend () -> Unit) {
        try { withTimeout(5_000) { block() }; fail("Failure expected") }
        catch (e: Exception) { assertFalse(e is kotlinx.coroutines.TimeoutCancellationException) }
    }

    private class FakeAudio : AudioInput {
        override var initialized = true
        override val bufferSize = 16
        var startFailure = false
        var releaseFailure = false
        var firstReadResult = 4
        var starts = 0
        var releases = 0
        private val reads = AtomicInteger()
        private val blocked = CountDownLatch(1)
        private val stopRead = CountDownLatch(1)
        val releaseEntered = CountDownLatch(1)
        var releaseBarrier: CountDownLatch? = null
        override fun start() { if (startFailure) error("start failed"); starts++ }
        override fun read(buffer: ByteArray): Int {
            if (reads.incrementAndGet() == 1) {
                buffer.fill(1)
                return firstReadResult
            }
            blocked.countDown()
            check(stopRead.await(5, TimeUnit.SECONDS)) { "synthetic read not stopped" }
            return -3
        }
        fun awaitBlockedRead() { assertTrue(blocked.await(2, TimeUnit.SECONDS)) }
        override fun stop() { stopRead.countDown() }
        override fun release() {
            releaseEntered.countDown()
            check(releaseBarrier?.await(5, TimeUnit.SECONDS) != false)
            releases++
            if (releaseFailure) error("device release failed")
        }
    }
}
