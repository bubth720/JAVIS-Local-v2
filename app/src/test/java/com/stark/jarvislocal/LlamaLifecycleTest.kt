package com.stark.jarvislocal

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LlamaLifecycleTest {
    private fun engine(backend: FakeLlamaBackend) = LlamaLocalEngine({ true }, "synthetic.gguf", backend, ProcessInferenceAdmission())

    @Test fun warmupAndGenerationReuseTheHistoricalModelAndKeepSystemSeparate() = runTest {
        val backend = FakeLlamaBackend(); val engine = engine(backend)
        engine.warmUp(JarvisSettings())
        assertEquals("answer", engine.generate("context and question", "system only", JarvisSettings()))
        engine.warmUp(JarvisSettings())
        assertEquals(1, backend.loads); assertEquals(0, backend.releases)
        assertEquals(listOf("context and question", "system only"), backend.arguments)
        assertEquals(StopResult.UNSUPPORTED, engine.requestStop())
        assertEquals(UnloadResult.CONFIRMED, engine.unload()); assertFalse(engine.isLoaded())
        assertEquals(UnloadResult.CONFIRMED, engine.unload()); assertEquals(1, backend.releases)
    }

    @Test fun failedUnloadRetainsHandleAndNeverLoadsAnotherModel() = runTest {
        val backend = FakeLlamaBackend(); val engine = engine(backend)
        engine.warmUp(JarvisSettings()); backend.failRelease = true
        assertEquals(UnloadResult.FAILED, engine.unload()); assertFalse(engine.isLoaded())
        assertEquals(UnloadResult.UNKNOWN, engine.unload())
        val failure = runCatching { engine.warmUp(JarvisSettings(contextSize = 1024)) }.exceptionOrNull() as InferenceFailure
        assertTrue(failure.releaseUnconfirmed); assertTrue(failure.nativeDone)
        assertEquals(1, backend.loads); assertEquals(1, backend.releases)
    }

    @Test fun failedContextResizeReleaseCannotLoadReplacement() = runTest {
        val backend = FakeLlamaBackend(); val engine = engine(backend)
        engine.warmUp(JarvisSettings()); backend.failRelease = true
        assertTrue(runCatching { engine.warmUp(JarvisSettings(contextSize = 1024)) }.isFailure)
        assertEquals(1, backend.loads)
        assertEquals(UnloadResult.UNKNOWN, engine.unload())
    }

    @Test fun failedLoadWithoutHandleIsQuarantinedRatherThanRetriedBlindly() = runTest {
        val backend = FakeLlamaBackend().apply { failLoad = true }; val engine = engine(backend)
        val failure = runCatching { engine.warmUp(JarvisSettings()) }.exceptionOrNull() as InferenceFailure
        assertTrue(failure.releaseUnconfirmed)
        assertEquals(UnloadResult.UNKNOWN, engine.unload())
        assertTrue(runCatching { engine.warmUp(JarvisSettings()) }.isFailure)
        assertEquals(1, backend.loads)
    }

    @Test fun callerCancellationDoesNotReleaseModelBeforeCompletionReturns() = runTest {
        val backend = FakeLlamaBackend().apply { answer = CompletableDeferred() }; val engine = engine(backend)
        val job = launch { engine.generate("prompt", "system", JarvisSettings()) }; runCurrent()
        job.cancel(); runCurrent()
        assertFalse(job.isCompleted); assertEquals(0, backend.releases)
        backend.answer!!.complete("answer"); runCurrent()
        assertTrue(job.isCompleted); assertEquals(0, backend.releases)
        assertEquals(UnloadResult.CONFIRMED, engine.unload()); assertEquals(1, backend.releases)
    }

    @Test fun memoryFailureWithFailedReleaseKeepsControllerQuarantined() = runTest {
        val backend = FakeLlamaBackend().apply { failRelease = true; completionError = OutOfMemoryError("synthetic") }
        val manager = InferenceController(isolatedInferenceFactory({ engine(backend) }))
        manager.openSession("s")
        assertTrue(runCatching { manager.infer(InferenceRequest("s", "r", "system", "question", "")) }.isFailure)
        assertTrue(manager.state.value.nativeDone); assertTrue(manager.state.value.quarantined)
        assertFalse(manager.state.value.modelUnloaded); assertEquals(1, backend.loads)
        assertTrue(runCatching { manager.warmUp(JarvisSettings()) }.isFailure)
        assertEquals(1, backend.loads)
    }
}

internal class FakeLlamaBackend : LlamaBackend {
    var loads = 0; var releases = 0
    var failRelease = false; var failLoad = false
    var completionError: Throwable? = null
    var answer: CompletableDeferred<String>? = null
    var arguments = emptyList<String>()
    private val handle = Any()
    override suspend fun load(path: String, settings: JarvisSettings): Any {
        loads++; if (failLoad) error("synthetic load failure"); return handle
    }
    override suspend fun complete(model: Any, prompt: String, systemPrompt: String, settings: JarvisSettings): String {
        check(model === handle); arguments = listOf(prompt, systemPrompt)
        completionError?.let { throw it }
        return answer?.await() ?: "answer"
    }
    override fun release(model: Any) {
        check(model === handle); releases++
        if (failRelease) error("synthetic release failure")
    }
}
