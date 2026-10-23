package com.stark.jarvislocal.orchestration

import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskCoordinatorTest {
    private val session = UUID.randomUUID().toString()
    private fun request(id: String = "core.read", deadline: Long = 1000, confirmation: Confirmation = Confirmation.MISSING) =
        IntentEnvelope(session, capabilityId = id, category = IntentCategory.READ, deadlineElapsedMs = deadline, confirmation = confirmation)
    private fun adapter(requiresConfirmation: Boolean = false, operation: suspend (IntentEnvelope) -> List<IntentItem> = { emptyList() }) =
        object : LocalCapabilityAdapter {
            override val descriptor = CapabilityDescriptor("core.read", IntentCategory.READ, requiresConfirmation = requiresConfirmation)
            override suspend fun execute(intent: IntentEnvelope) = operation(intent)
        }
    private fun tasks(adapter: LocalCapabilityAdapter, clock: () -> Long = { 100 }, max: Int = 256) =
        TaskCoordinator(IntentRouter(StrategyRegistry(listOf(adapter)), IntentPermissions(setOf("core.read"))), clock, max)

    @Test fun duplicateRequestAndQuotaAreExplicit() = runTest {
        val tasks = tasks(adapter(), max = 1)
        val req = request()
        assertTrue(tasks.execute(req) is IntentResult.Completed)
        assertEquals(IntentResult.Refused(IntentResult.Reason.REPLAY), tasks.execute(req))
        assertEquals(IntentResult.Refused(IntentResult.Reason.QUOTA), tasks.execute(request()))
    }
    @Test fun deadlineBeforeDispatchDoesNotCallAdapter() = runTest {
        var calls = 0
        val tasks = tasks(adapter { calls++; emptyList() })
        assertEquals(IntentResult.Refused(IntentResult.Reason.EXPIRED), tasks.execute(request(deadline = 100)))
        assertEquals(0, calls)
    }
    @Test fun deadlineAfterReturnRejectsLateResult() = runTest {
        var now = 100L
        val tasks = tasks(adapter { now = 1000; listOf(IntentItem("x", "late")) }, { now })
        assertEquals(IntentResult.Refused(IntentResult.Reason.EXPIRED), tasks.execute(request()))
        assertFalse(tasks.isBusy())
    }
    @Test fun logicalCancellationRetainsGateUntilAdapterReturns() = runTest {
        val done = CompletableDeferred<Unit>()
        val tasks = tasks(adapter { withContext(NonCancellable) { done.await() }; listOf(IntentItem("x", "late")) })
        val req = request()
        val first = async { tasks.execute(req) }; runCurrent()
        assertTrue(tasks.cancel(session, req.requestId))
        assertEquals(TaskPhase.CANCELLING, tasks.state.value.last().phase)
        assertEquals(IntentResult.Refused(IntentResult.Reason.BUSY), tasks.execute(request()))
        assertTrue(tasks.isBusy())
        done.complete(Unit)
        assertEquals(IntentResult.Refused(IntentResult.Reason.CANCELLED), first.await())
        assertFalse(tasks.isBusy())
        assertTrue(tasks.execute(request()) is IntentResult.Completed)
    }
    @Test fun coroutineCancellationDoesNotProveUnderlyingCompletion() = runTest {
        val done = CompletableDeferred<Unit>()
        val tasks = tasks(adapter { withContext(NonCancellable) { done.await() }; emptyList() })
        val operation = launch { tasks.execute(request()) }; runCurrent()
        operation.cancel(); runCurrent()
        assertTrue(tasks.isBusy())
        assertEquals(IntentResult.Refused(IntentResult.Reason.BUSY), tasks.execute(request()))
        done.complete(Unit); operation.join()
        assertFalse(tasks.isBusy())
        assertEquals(TaskPhase.CANCELLED, tasks.state.value.last().phase)
    }
    @Test fun confirmationCannotGrantHostPermission() = runTest {
        var calls = 0
        val registry = StrategyRegistry(listOf(adapter(true) { calls++; emptyList() }))
        val denied = IntentRouter(registry, IntentPermissions(emptySet()))
        assertEquals(IntentResult.Refused(IntentResult.Reason.UNAUTHORIZED), denied.route(request(confirmation = Confirmation.EXPLICIT)))
        val allowed = IntentRouter(registry, IntentPermissions(setOf("core.read")))
        assertEquals(IntentResult.UserConfirmationRequired("core.read"), allowed.route(request()))
        assertEquals(IntentResult.Refused(IntentResult.Reason.DECLINED), allowed.route(request(confirmation = Confirmation.DECLINED)))
        assertEquals(0, calls)
        assertTrue(allowed.route(request(confirmation = Confirmation.EXPLICIT)) is IntentResult.Completed)
    }
    @Test fun errorsDoNotExposeExceptionDetailsOrKeepGateClosed() = runTest {
        val tasks = tasks(adapter { error("sensitive test-only detail") })
        assertEquals(IntentResult.Refused(IntentResult.Reason.FAILED), tasks.execute(request()))
        assertFalse(tasks.isBusy())
        assertEquals(TaskPhase.FAILED, tasks.state.value.last().phase)
    }
    @Test fun boundedOutputRejectsOversizeWithoutReturningPayload() = runTest {
        val tasks = tasks(adapter { listOf(IntentItem("x", "a".repeat(65537))) })
        assertEquals(IntentResult.Refused(IntentResult.Reason.INVALID_INPUT), tasks.execute(request()))
    }
    @Test fun callerCannotMutateDocumentScopeAfterConstruction() {
        val selected = mutableListOf(1L)
        val context = IntentContext(selected)
        selected.add(2L)
        assertEquals(listOf(1L), context.documentIds)
    }
    @Test(expected = IllegalArgumentException::class) fun duplicateCapabilitiesCannotOverrideOwner() {
        StrategyRegistry(listOf(adapter(), adapter()))
    }
    @Test(expected = IllegalArgumentException::class) fun unsupportedVersionIsRejected() { request().copy(version = 2) }
    @Test(expected = IllegalArgumentException::class) fun unboundedRequestIsRejected() { request().copy(input = "x".repeat(2049)) }
    @Test fun disableIsRefusedWhileTaskActive() = runTest {
        val done = CompletableDeferred<Unit>()
        val facade = MvpIntentFacade(StrategyRegistry(listOf(adapter { done.await(); emptyList() })), IntentPermissions(setOf("core.read"))) { 100 }
        facade.setEnabled(true)
        val first = async { facade.execute(request()) }; runCurrent()
        assertFalse(facade.setEnabled(false))
        done.complete(Unit); first.await()
        assertTrue(facade.setEnabled(false))
        assertEquals(IntentResult.Refused(IntentResult.Reason.DISABLED), facade.execute(request()))
    }
}
