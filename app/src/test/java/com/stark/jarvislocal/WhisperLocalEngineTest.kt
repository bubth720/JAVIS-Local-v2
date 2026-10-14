package com.stark.jarvislocal

import java.io.File
import java.io.RandomAccessFile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class WhisperLocalEngineTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun missingModelDoesNotLoadNativeResources() = runTest {
        val backend = FakeWhisper()
        val engine = WhisperLocalEngine({ false }, backend)
        expectFailure { engine.transcribe(wav().path) }
        assertEquals(0, backend.loads)
    }

    @Test fun absentOrTruncatedWavIsRejectedBeforeNativeLoad() = runTest {
        val backend = FakeWhisper()
        val engine = WhisperLocalEngine({ true }, backend)
        expectFailure { engine.transcribe(File(temporary.root, "missing.wav").path) }
        expectFailure { engine.transcribe(temporary.newFile().path) }
        val truncated = wav()
        RandomAccessFile(truncated, "rw").use { it.setLength(46) }
        expectFailure { engine.transcribe(truncated.path) }
        assertEquals(0, backend.loads)
    }

    @Test fun incompleteHeaderAndWrongPcmFormatAreRejected() = runTest {
        val backend = FakeWhisper()
        val engine = WhisperLocalEngine({ true }, backend)
        val incomplete = temporary.newFile()
        RandomAccessFile(incomplete, "rw").use { WavFiles.writeHeader(it, 0); it.seek(44); it.write(byteArrayOf(1, 2)) }
        expectFailure { engine.transcribe(incomplete.path) }
        val stereo = wav()
        RandomAccessFile(stereo, "rw").use { it.seek(22); it.write(byteArrayOf(2, 0)) }
        expectFailure { engine.transcribe(stereo.path) }
        assertEquals(0, backend.loads)
    }

    @Test fun successfulTranscriptionReleasesTheModelAndTrimsText() = runTest {
        val backend = FakeWhisper().apply { text = "  bonjour  " }
        assertEquals("bonjour", WhisperLocalEngine({ true }, backend).transcribe(wav().path))
        assertEquals(1, backend.loads)
        assertEquals(1, backend.releases)
    }

    @Test fun emptyTranscriptionStillReleasesTheModel() = runTest {
        val backend = FakeWhisper().apply { text = "  " }
        assertEquals("", WhisperLocalEngine({ true }, backend).transcribe(wav().path))
        assertEquals(1, backend.releases)
    }

    @Test fun loadingFailureAllowsRetryWithoutReleasingUnknownHandle() = runTest {
        val backend = FakeWhisper().apply { loadFailure = true }
        val engine = WhisperLocalEngine({ true }, backend)
        val audio = wav()
        expectFailure { engine.transcribe(audio.path) }
        assertEquals(0, backend.releases)
        backend.loadFailure = false
        assertEquals("bonjour", engine.transcribe(audio.path))
        assertEquals(1, backend.releases)
    }

    @Test fun transcriptionFailureReleasesTheModelAndAllowsRetry() = runTest {
        val backend = FakeWhisper().apply { transcriptionFailure = true }
        val engine = WhisperLocalEngine({ true }, backend)
        val audio = wav()
        expectFailure { engine.transcribe(audio.path) }
        assertEquals(1, backend.releases)
        backend.transcriptionFailure = false
        assertEquals("bonjour", engine.transcribe(audio.path))
        assertEquals(2, backend.releases)
    }

    @Test fun concurrentAttemptIsRejectedUntilFirstNativeCallEnds() = runTest {
        val backend = FakeWhisper().apply { result = CompletableDeferred() }
        val engine = WhisperLocalEngine({ true }, backend)
        val audio = wav()
        val first = async { engine.transcribe(audio.path) }
        runCurrent()
        expectFailure { engine.transcribe(audio.path) }
        assertEquals(1, backend.loads)
        backend.result!!.complete("réponse"); runCurrent()
        assertEquals("réponse", first.await())
        assertEquals(1, backend.releases)
    }

    @Test fun cancellationDoesNotReleaseNativeResourcesBeforeActualReturn() = runTest {
        val backend = FakeWhisper().apply { result = CompletableDeferred() }
        val engine = WhisperLocalEngine({ true }, backend)
        val audio = wav()
        val first = launch { engine.transcribe(audio.path) }
        runCurrent(); first.cancel(); runCurrent()
        assertFalse(first.isCompleted)
        assertEquals(0, backend.releases)
        expectFailure { engine.transcribe(audio.path) }
        backend.result!!.complete("fin"); runCurrent()
        assertTrue(first.isCompleted)
        assertEquals(1, backend.releases)
        backend.result = null
        assertEquals("bonjour", engine.transcribe(audio.path))
    }

    private fun wav(): File = temporary.newFile().also { file ->
        RandomAccessFile(file, "rw").use { WavFiles.writeHeader(it, 4); it.seek(44); it.write(byteArrayOf(1, 2, 3, 4)) }
    }

    private suspend fun expectFailure(block: suspend () -> Unit) {
        try { block(); fail("Failure expected") } catch (e: Exception) { /* expected */ }
    }

    private class FakeWhisper : WhisperBackend {
        var loads = 0
        var releases = 0
        var text = "bonjour"
        var loadFailure = false
        var transcriptionFailure = false
        var result: CompletableDeferred<String>? = null
        override suspend fun load(): Any { loads++; if (loadFailure) error("load failed"); return this }
        override suspend fun transcribe(model: Any, audioPath: String): String {
            if (transcriptionFailure) error("transcription failed")
            return result?.await() ?: text
        }
        override fun release(model: Any) { assertSame(this, model); releases++ }
    }
}
