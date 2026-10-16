package com.stark.jarvislocal

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
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
class InferenceControllerTest {
    private val settings = JarvisSettings()
    private fun request(id: String = "r1", session: String = "s1") = InferenceRequest(session, id, "system only", "question", "context\n")
    private fun controller(engine: SimulatedEngine = SimulatedEngine()): InferenceController =
        InferenceController(InferenceEngineFactory({ engine })).also { it.openSession("s1") }
    private fun refused(reason: InferenceRefusal, block: () -> Unit) {
        val error = assertThrows(InferenceRejected::class.java, block)
        assertEquals(reason, error.reason)
    }
    private suspend fun refusedSuspend(reason: InferenceRefusal, block: suspend () -> Unit) {
        try { block(); fail("Expected $reason") }
        catch (error: InferenceRejected) { assertEquals(reason, error.reason) }
    }

    @Test fun selectionHappensBeforeAnyConstructionOrWarmup() = runTest {
        var historicalCreated = 0; var delegatedCreated = 0
        val historical = SimulatedEngine(); val delegated = SimulatedEngine()
        val manager = InferenceController(InferenceEngineFactory({ historicalCreated++; historical }, { delegatedCreated++; delegated }))
        manager.select(InferenceMode.DELEGATED_SIMULATOR)
        assertEquals(0, historicalCreated); assertEquals(0, delegatedCreated)
        manager.warmUp(settings)
        assertEquals(0, historicalCreated); assertEquals(1, delegatedCreated)
        assertEquals(0, historical.preparations); assertEquals(1, delegated.preparations)
    }

    @Test fun historicalIsDefaultAndUnconfiguredDelegateIsRefused() = runTest {
        val engine = SimulatedEngine(); val manager = controller(engine)
        refused(InferenceRefusal.DELEGATE_DISABLED) { manager.select(InferenceMode.DELEGATED_SIMULATOR) }
        manager.warmUp(settings)
        assertEquals(InferenceMode.HISTORICAL, manager.state.value.mode)
        assertEquals(1, engine.preparations)
    }

    @Test fun simultaneousPreparationAndSelectionCannotDoubleLoad() = runTest {
        val engine = SimulatedEngine().apply { preparation = CompletableDeferred() }
        var delegates = 0
        val manager = InferenceController(InferenceEngineFactory({ engine }, { delegates++; SimulatedEngine() }))
        manager.openSession("s1")
        val preparing = launch { manager.warmUp(settings) }; runCurrent()
        refused(InferenceRefusal.BUSY) { manager.select(InferenceMode.DELEGATED_SIMULATOR) }
        refusedSuspend(InferenceRefusal.BUSY) { manager.warmUp(settings) }
        refusedSuspend(InferenceRefusal.BUSY) { manager.infer(request()) }
        assertEquals(1, engine.preparations); assertEquals(0, delegates)
        engine.preparation!!.complete(Unit); preparing.join()
        refused(InferenceRefusal.RELEASE_UNCONFIRMED) { manager.select(InferenceMode.DELEGATED_SIMULATOR) }
    }

    @Test fun cancellationBeforeDispatchDoesNotConstructOrLoadAnything() = runTest {
        var creations = 0
        val manager = InferenceController(InferenceEngineFactory({ creations++; SimulatedEngine() }))
        manager.openSession("s1")
        val job = launch { manager.infer(request()) }
        job.cancel(); runCurrent()
        assertEquals(0, creations); assertFalse(manager.state.value.busy)
        assertTrue(manager.state.value.modelUnloaded)
    }

    @Test fun cancellationDuringGenerationRequestsStopAndDrainsBeforeUnlock() = runTest {
        val engine = SimulatedEngine().apply { answer = CompletableDeferred() }
        val manager = controller(engine)
        val job = launch { manager.infer(request()) }; runCurrent()
        job.cancel(); runCurrent()
        assertTrue(manager.state.value.stopRequested); assertFalse(manager.state.value.nativeDone)
        assertTrue(manager.state.value.busy); assertFalse(job.isCompleted)
        refusedSuspend(InferenceRefusal.BUSY) { manager.infer(request("r2")) }
        engine.answer!!.complete("late"); runCurrent()
        assertTrue(job.isCompleted); assertTrue(manager.state.value.nativeDone)
        assertFalse(manager.state.value.modelUnloaded); assertFalse(manager.state.value.busy)
        assertEquals(1, engine.stops)
    }

    @Test fun cancellationAfterReservationButBeforeBackendDispatchDoesNotGenerate() = runTest {
        val engine = SimulatedEngine(); val manager = controller(engine)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { manager.infer(request()) }
        job.cancel(); runCurrent()
        assertTrue(job.isCompleted); assertEquals(0, engine.generations)
        assertTrue(manager.state.value.nativeDone); assertTrue(manager.state.value.modelUnloaded)
        assertFalse(manager.state.value.quarantined)
    }

    @Test fun negativeIsLoadedIsNotAnUnloadAcknowledgement() = runTest {
        val backend = object : LocalLanguageModel {
            override suspend fun warmUp(settings: JarvisSettings) = Unit
            override suspend fun generate(prompt: String, systemPrompt: String, settings: JarvisSettings) = "answer"
            override fun isLoaded() = false
        }
        val manager = InferenceController(InferenceEngineFactory({ backend }))
        manager.warmUp(settings)
        assertFalse(manager.state.value.modelUnloaded)
        refused(InferenceRefusal.RELEASE_UNCONFIRMED) { manager.select(InferenceMode.HISTORICAL) }
        assertEquals(UnloadResult.UNKNOWN, manager.unload())
        assertTrue(manager.state.value.quarantined)
    }

    @Test fun cancellationCannotEraseAFailedReleaseReportedDuringDrain() = runTest {
        val completion = CompletableDeferred<Unit>()
        val backend = object : LocalLanguageModel {
            override suspend fun warmUp(settings: JarvisSettings) = Unit
            override fun isLoaded() = true
            override suspend fun generate(prompt: String, systemPrompt: String, settings: JarvisSettings) = "unused"
            override suspend fun infer(request: InferenceRequest): InferenceResponse {
                completion.await()
                throw InferenceFailure("release uncertain", nativeDone = true, releaseUnconfirmed = true)
            }
        }
        val manager = InferenceController(InferenceEngineFactory({ backend })).also { it.openSession("s1") }
        val job = launch { manager.infer(request()) }; runCurrent()
        job.cancel(); runCurrent(); completion.complete(Unit); runCurrent()
        assertTrue(job.isCompleted); assertTrue(manager.state.value.nativeDone)
        assertTrue(manager.state.value.quarantined); assertFalse(manager.state.value.modelUnloaded)
        refused(InferenceRefusal.RELEASE_UNCONFIRMED) { manager.select(InferenceMode.HISTORICAL) }
    }

    @Test fun cancellationAfterCompletedGenerationDoesNotInventAnotherStop() = runTest {
        val engine = SimulatedEngine(); val manager = controller(engine)
        val job = launch { assertEquals("answer", manager.infer(request()).text) }
        runCurrent(); assertTrue(job.isCompleted)
        job.cancel(); runCurrent()
        assertEquals(0, engine.stops); assertFalse(manager.state.value.stopRequested)
        assertTrue(manager.state.value.nativeDone); assertFalse(manager.state.value.modelUnloaded)
    }

    @Test fun timeoutDoesNotConfirmNativeDoneOrUnload() = runTest {
        val engine = SimulatedEngine().apply { answer = CompletableDeferred() }
        val manager = controller(engine)
        val result = async { runCatching { withTimeout(100) { manager.infer(request()) } } }
        runCurrent(); advanceTimeBy(101); runCurrent()
        assertFalse(result.isCompleted); assertTrue(manager.state.value.busy)
        assertTrue(manager.state.value.stopRequested); assertFalse(manager.state.value.nativeDone)
        refused(InferenceRefusal.BUSY) { manager.select(InferenceMode.HISTORICAL) }
        assertEquals(0, engine.unloads)
        engine.answer!!.complete("late"); runCurrent()
        assertTrue(result.await().isFailure); assertTrue(manager.state.value.nativeDone)
        assertFalse(manager.state.value.modelUnloaded)
    }

    @Test fun explicitStopIsDistinctFromDoneAndDiscardsTheEventualResponse() = runTest {
        val engine = SimulatedEngine().apply { answer = CompletableDeferred() }
        val manager = controller(engine)
        val result = async { runCatching { manager.infer(request()) } }; runCurrent()
        assertEquals(StopResult.UNSUPPORTED, manager.requestStop())
        assertTrue(manager.state.value.stopRequested); assertFalse(manager.state.value.nativeDone)
        engine.answer!!.complete("late"); runCurrent()
        assertEquals(InferenceRefusal.STOP_REQUESTED, (result.await().exceptionOrNull() as InferenceRejected).reason)
        assertTrue(manager.state.value.nativeDone); assertFalse(manager.state.value.modelUnloaded)
    }

    @Test fun previousSessionAndWrongResponseIdsAreRejected() = runTest {
        val engine = SimulatedEngine().apply { answer = CompletableDeferred() }
        val manager = controller(engine)
        val result = async { runCatching { manager.infer(request()) } }; runCurrent()
        manager.openSession("s2")
        engine.answer!!.complete("old session"); runCurrent()
        assertEquals(InferenceRefusal.STALE_RESPONSE, (result.await().exceptionOrNull() as InferenceRejected).reason)
        engine.answer = null; engine.wrongRequestId = true
        refusedSuspend(InferenceRefusal.STALE_RESPONSE) { manager.infer(request("r2", "s2")) }
        engine.wrongRequestId = false
        assertEquals("answer", manager.infer(request("r3", "s2")).text)
    }

    @Test fun failedUnloadQuarantinesAndBlocksAllNextEngineOperations() = runTest {
        for (release in listOf(UnloadResult.FAILED, UnloadResult.UNKNOWN)) {
            val engine = SimulatedEngine().apply { unloadResult = release }
            val manager = controller(engine); manager.warmUp(settings)
            assertEquals(release, manager.unload())
            assertTrue(manager.state.value.quarantined); assertFalse(manager.state.value.modelUnloaded)
            refused(InferenceRefusal.RELEASE_UNCONFIRMED) { manager.select(InferenceMode.HISTORICAL) }
            refusedSuspend(InferenceRefusal.RELEASE_UNCONFIRMED) { manager.warmUp(settings) }
            refusedSuspend(InferenceRefusal.RELEASE_UNCONFIRMED) { manager.infer(request()) }
            refusedSuspend(InferenceRefusal.RELEASE_UNCONFIRMED) { manager.unload() }
        }
    }

    @Test fun unconfirmedNativeCompletionCannotAuthorizeAnyNewEngine() = runTest {
        val engine = SimulatedEngine().apply { nativeDone = false }
        val manager = controller(engine)
        refusedSuspend(InferenceRefusal.RELEASE_UNCONFIRMED) { manager.infer(request()) }
        assertTrue(manager.state.value.quarantined); assertFalse(manager.state.value.nativeDone)
        refusedSuspend(InferenceRefusal.RELEASE_UNCONFIRMED) { manager.warmUp(settings) }
        refused(InferenceRefusal.RELEASE_UNCONFIRMED) { manager.select(InferenceMode.HISTORICAL) }
    }

    @Test fun explicitHistoricalReturnRequiresConfirmedUnload() = runTest {
        val historical = SimulatedEngine(); val delegated = SimulatedEngine()
        val manager = InferenceController(InferenceEngineFactory({ historical }, { delegated }))
        manager.select(InferenceMode.DELEGATED_SIMULATOR); manager.warmUp(settings)
        refused(InferenceRefusal.RELEASE_UNCONFIRMED) { manager.select(InferenceMode.HISTORICAL) }
        assertEquals(UnloadResult.CONFIRMED, manager.unload())
        assertTrue(manager.state.value.nativeDone); assertTrue(manager.state.value.modelUnloaded)
        manager.select(InferenceMode.HISTORICAL)
        assertEquals(0, historical.preparations)
        manager.warmUp(settings)
        assertEquals(1, historical.preparations); assertEquals(1, delegated.unloads)
    }

    @Test fun structuredSystemContextSourcesAndLimitsReachBackendUnchanged() = runTest {
        val engine = SimulatedEngine(); val manager = controller(engine)
        val request = request().copy(sources = listOf(InferenceSource(3, 4, "atlas.pdf", 2)), limits = GenerationLimits(1024, 128))
        manager.infer(request)
        assertEquals(request, engine.requests.single())
        assertFalse(request.renderPrompt().contains(request.systemPrompt))
    }

    @Test fun incompatibleContractSystemCapabilityAndBudgetNeverInvokeBackend() = runTest {
        val engine = SimulatedEngine(); val manager = controller(engine)
        refusedSuspend(InferenceRefusal.CONTRACT) { manager.infer(request().copy(contractVersion = 2)) }
        engine.capabilities = InferenceCapabilities(contractVersion = 2)
        refusedSuspend(InferenceRefusal.CONTRACT) { manager.infer(request().copy(contractVersion = 2)) }
        engine.capabilities = InferenceCapabilities(separateSystemPrompt = false)
        refusedSuspend(InferenceRefusal.SYSTEM_PROMPT) { manager.infer(request()) }
        engine.capabilities = InferenceCapabilities()
        refusedSuspend(InferenceRefusal.BUDGET) { manager.infer(request().copy(question = "é".repeat(3000))) }
        refusedSuspend(InferenceRefusal.BUDGET) { manager.infer(request().copy(limits = GenerationLimits(2048, 0))) }
        assertEquals(0, engine.generations); assertTrue(manager.state.value.modelUnloaded)
        assertFalse(manager.state.value.quarantined)
    }

    @Test fun legacyGeneratePreservesExactPromptAndSeparateSystem() = runTest {
        val engine = SimulatedEngine(); val manager = controller(engine)
        assertEquals("legacy", manager.generate("already formatted prompt", "system only", settings))
        assertEquals(listOf("already formatted prompt", "system only"), engine.legacyArguments)
    }

    @Test fun exceptionWithoutCompletionEvidenceQuarantinesInsteadOfGuessing() = runTest {
        val engine = SimulatedEngine().apply { failure = IllegalStateException("unknown execution") }
        val manager = controller(engine)
        assertTrue(runCatching { manager.infer(request()) }.isFailure)
        assertTrue(manager.state.value.quarantined)
        assertFalse(manager.state.value.nativeDone)
    }

    @Test fun attestedGenerationErrorCanRetrySameEngineButCannotSwitchBeforeUnload() = runTest {
        val engine = SimulatedEngine().apply { failure = InferenceFailure("returned failure", nativeDone = true) }
        val manager = controller(engine)
        assertTrue(runCatching { manager.infer(request()) }.isFailure)
        assertTrue(manager.state.value.nativeDone); assertFalse(manager.state.value.quarantined)
        refused(InferenceRefusal.RELEASE_UNCONFIRMED) { manager.select(InferenceMode.HISTORICAL) }
        engine.failure = null
        assertEquals("answer", manager.infer(request("r2")).text)
    }
}

internal class SimulatedEngine : LocalLanguageModel {
    var preparation: CompletableDeferred<Unit>? = null
    var answer: CompletableDeferred<String>? = null
    var preparations = 0; var generations = 0; var stops = 0; var unloads = 0
    var loaded = false
    var nativeDone = true
    var wrongRequestId = false
    var unloadResult = UnloadResult.CONFIRMED
    var failure: Exception? = null
    val requests = mutableListOf<InferenceRequest>()
    var legacyArguments = emptyList<String>()
    override var capabilities = InferenceCapabilities()
    override suspend fun warmUp(settings: JarvisSettings) { preparations++; preparation?.await(); loaded = true }
    override suspend fun infer(request: InferenceRequest): InferenceResponse {
        generations++; loaded = true; requests += request
        failure?.let { throw it }
        return InferenceResponse(request.sessionId, if (wrongRequestId) "stale" else request.requestId, answer?.await() ?: "answer", nativeDone)
    }
    override suspend fun generate(prompt: String, systemPrompt: String, settings: JarvisSettings): String {
        generations++; loaded = true; legacyArguments = listOf(prompt, systemPrompt); return "legacy"
    }
    override fun requestStop(): StopResult { stops++; return StopResult.UNSUPPORTED }
    override suspend fun unload(): UnloadResult { unloads++; if (unloadResult == UnloadResult.CONFIRMED) loaded = false; return unloadResult }
    override fun isLoaded() = loaded
}
