package com.stark.jarvislocal

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProcessInferenceAdmissionTest {
    private val settings = JarvisSettings()
    private fun controller(backend: LocalLanguageModel, admission: ProcessInferenceAdmission) =
        InferenceController(InferenceEngineFactory({ backend }, admission = admission)).also { it.openSession("s") }
    private fun request() = InferenceRequest("s", "r", "system", "question", "")
    private suspend fun held(block: suspend () -> Unit) {
        try { block(); fail("Second backend must be refused") }
        catch (failure: InferenceRejected) { assertEquals(InferenceRefusal.PROCESS_ENGINE_HELD, failure.reason) }
    }

    @Test fun independentControllersCannotWarmTwoBackendsUntilConfirmedRelease() = runTest {
        val admission = ProcessInferenceAdmission()
        val first = SimulatedEngine(); val second = SimulatedEngine()
        val a = controller(first, admission); val b = controller(second, admission)
        a.warmUp(settings)
        held { b.warmUp(settings) }; assertEquals(0, second.preparations)
        a.infer(request())
        held { b.warmUp(settings) }
        assertEquals(UnloadResult.CONFIRMED, a.unload())
        b.warmUp(settings); assertEquals(1, second.preparations)
        held { a.warmUp(settings) }
    }

    @Test fun concurrentPreparationReservesBeforeBackendStarts() = runTest {
        val admission = ProcessInferenceAdmission()
        val first = SimulatedEngine().apply { preparation = CompletableDeferred() }
        val second = SimulatedEngine()
        val a = controller(first, admission); val b = controller(second, admission)
        val preparing = launch { a.warmUp(settings) }; runCurrent()
        held { b.warmUp(settings) }
        assertEquals(0, second.preparations)
        first.preparation!!.complete(Unit); preparing.join()
        held { b.warmUp(settings) }
    }

    @Test fun timeoutAndCancellationDoNotFreeAdmissionAtNativeReturn() = runTest {
        val admission = ProcessInferenceAdmission()
        val first = SimulatedEngine().apply { answer = CompletableDeferred() }
        val a = controller(first, admission); val b = controller(SimulatedEngine(), admission)
        val job = async { runCatching { withTimeout(100) { a.infer(request()) } } }
        runCurrent(); advanceTimeBy(101); runCurrent()
        assertFalse(job.isCompleted); assertTrue(a.state.value.stopRequested)
        held { b.warmUp(settings) }
        first.answer!!.complete("late"); runCurrent()
        assertTrue(job.await().isFailure); assertTrue(a.state.value.nativeDone)
        held { b.warmUp(settings) }
        assertEquals(UnloadResult.CONFIRMED, a.unload())
        b.warmUp(settings)
    }

    @Test fun failedOrUnknownReleaseBlocksAReplacementControllerAndSameBackend() = runTest {
        for (result in listOf(UnloadResult.FAILED, UnloadResult.UNKNOWN)) {
            val admission = ProcessInferenceAdmission()
            val first = SimulatedEngine().apply { unloadResult = result }
            val a = controller(first, admission); a.warmUp(settings)
            assertEquals(result, a.unload())
            held { controller(SimulatedEngine(), admission).warmUp(settings) }
            val replacement = controller(first, admission)
            val failure = runCatching { replacement.warmUp(settings) }.exceptionOrNull() as InferenceRejected
            assertEquals(InferenceRefusal.RELEASE_UNCONFIRMED, failure.reason)
            assertTrue(replacement.state.value.quarantined)
            assertFalse(replacement.state.value.modelUnloaded)
            assertEquals(1, first.preparations)
        }
    }

    @Test fun failedLoadWithUnknownResourcesCannotFallbackToHistorical() = runTest {
        val admission = ProcessInferenceAdmission()
        val failed = SimulatedEngine().apply { failure = InferenceFailure("failed load", true, releaseUnconfirmed = true) }
        val a = controller(failed, admission)
        assertTrue(runCatching { a.infer(request()) }.isFailure)
        val historical = SimulatedEngine()
        held { controller(historical, admission).warmUp(settings) }
        assertEquals(0, historical.preparations)
    }

    @Test fun controlledFallbackAfterLoadFailureRequiresReleaseAcknowledgement() = runTest {
        val admission = ProcessInferenceAdmission()
        val delegate = SimulatedEngine().apply {
            preparation = CompletableDeferred<Unit>().also {
                it.completeExceptionally(InferenceFailure("load rejected before a usable model", nativeDone = true))
            }
        }
        val historical = SimulatedEngine()
        val manager = InferenceController(InferenceEngineFactory({ historical }, { delegate }, admission))
        manager.openSession("s"); manager.select(InferenceMode.DELEGATED_SIMULATOR)
        assertTrue(runCatching { manager.warmUp(settings) }.isFailure)
        assertTrue(runCatching { manager.select(InferenceMode.HISTORICAL) }.isFailure)
        assertEquals(0, historical.preparations)
        assertEquals(UnloadResult.CONFIRMED, manager.unload())
        manager.select(InferenceMode.HISTORICAL); manager.warmUp(settings)
        assertEquals(1, historical.preparations)
    }

    @Test fun contextResizeDoesNotOpenAnAdmissionGapBetweenReleaseAndReload() = runTest {
        val admission = ProcessInferenceAdmission()
        var loads = 0; var releases = 0
        val backend = object : LlamaBackend {
            override suspend fun load(path: String, settings: JarvisSettings): Any { loads++; return Any() }
            override suspend fun complete(model: Any, prompt: String, systemPrompt: String, settings: JarvisSettings) = "answer"
            override fun release(model: Any) {
                releases++
                val failure = assertThrows(InferenceRejected::class.java) { admission.acquire(Any()) }
                assertEquals(InferenceRefusal.PROCESS_ENGINE_HELD, failure.reason)
            }
        }
        val engine = LlamaLocalEngine({ true }, "synthetic.gguf", backend, admission)
        engine.warmUp(settings); engine.warmUp(settings.copy(contextSize = 1024))
        assertEquals(2, loads); assertEquals(1, releases)
        held { controller(SimulatedEngine(), admission).warmUp(settings) }
    }

    @Test fun directHistoricalInstancesUseSameAdmissionAndCannotBypassController() = runTest {
        val admission = ProcessInferenceAdmission()
        val firstBackend = FakeLlamaBackend(); val secondBackend = FakeLlamaBackend()
        val first = LlamaLocalEngine({ true }, "first.gguf", firstBackend, admission)
        val second = LlamaLocalEngine({ true }, "second.gguf", secondBackend, admission)
        val manager = controller(first, admission)
        manager.warmUp(settings)
        held { second.warmUp(settings) }
        assertEquals(0, secondBackend.loads)
        assertEquals(UnloadResult.CONFIRMED, manager.unload())
        second.warmUp(settings); assertEquals(1, secondBackend.loads)
        held { manager.warmUp(settings) }
    }

    @Test fun nativeLoadFailureAndReleaseFailureKeepProcessReserved() = runTest {
        for (failLoad in listOf(true, false)) {
            val admission = ProcessInferenceAdmission()
            val backend = FakeLlamaBackend().apply { this.failLoad = failLoad }
            val first = LlamaLocalEngine({ true }, "first.gguf", backend, admission)
            if (failLoad) assertTrue(runCatching { first.warmUp(settings) }.isFailure)
            else {
                first.warmUp(settings); backend.failRelease = true
                assertEquals(UnloadResult.FAILED, first.unload())
            }
            val other = FakeLlamaBackend()
            held { LlamaLocalEngine({ true }, "second.gguf", other, admission).warmUp(settings) }
            assertEquals(0, other.loads)
        }
    }

    @Test fun staleReleaseCannotReleaseTheNextOwner() {
        val admission = ProcessInferenceAdmission()
        val first = Any(); val second = Any(); val third = Any()
        admission.acquire(first); admission.confirmRelease(first); admission.acquire(second)
        admission.confirmRelease(first)
        val failure = assertThrows(InferenceRejected::class.java) { admission.acquire(third) }
        assertEquals(InferenceRefusal.PROCESS_ENGINE_HELD, failure.reason)
    }

    @Test fun factoriesShareProductionAdmissionByDefault() {
        val first = InferenceEngineFactory({ SimulatedEngine() })
        val second = InferenceEngineFactory({ SimulatedEngine() })
        assertSame(ProcessInferenceAdmission.process, first.admission)
        assertSame(first.admission, second.admission)
    }
}
