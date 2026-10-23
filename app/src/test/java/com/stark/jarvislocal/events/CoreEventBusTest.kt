package com.stark.jarvislocal.events

import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CoreEventBusTest {
    private val session = UUID.randomUUID().toString()
    private fun event(kind: CoreEvent = CoreEvent.Thinking) = EventEnvelope(session, UUID.randomUUID().toString(), kind)
    @Test fun disabledDefaultAndOldGenerationRemainRefused() {
        val bus = CoreEventBus({ 10 })
        assertNull(bus.generationIfEnabled())
        assertFalse(bus.publish(event(), 0))
        bus.setEnabled(true)
        val generation = bus.generationIfEnabled()!!
        assertTrue(bus.publish(event(), generation))
        bus.setEnabled(false); bus.setEnabled(true)
        assertTrue(bus.state.value.events.isEmpty())
        assertFalse(bus.publish(event(), generation))
    }
    @Test fun orderDeduplicationOverflowAndProjectionAreObservable() {
        val bus = CoreEventBus({ 10 }, capacity = 2)
        bus.setEnabled(true); val generation = bus.generationIfEnabled()!!
        val first = event(CoreEvent.UserInput)
        assertTrue(bus.publish(first, generation)); assertFalse(bus.publish(first, generation))
        bus.publish(event(CoreEvent.Thinking), generation)
        bus.publish(event(CoreEvent.ResponseCompleted), generation)
        assertEquals(listOf(2L, 3L), bus.state.value.events.map { it.sequence })
        assertEquals(1L, bus.state.value.dropped)
        val ui = UiStateProjection.project(bus.state.value)
        assertEquals("RESPONSE_COMPLETED", ui.phase)
        assertFalse(ui.busy); assertTrue(ui.replayGap)
    }
    @Test fun resubscribingUiDoesNotPublishOrStartAnyNewOperation() = runTest {
        val bus = CoreEventBus({ 100 }); bus.setEnabled(true)
        bus.publish(event(), bus.generationIfEnabled()!!)
        val projection = UiStateProjection(bus)
        assertEquals(projection.state.first(), projection.state.first())
        assertEquals(1, bus.state.value.events.size)
    }
    @Test fun cancellationIsBusyUntilFinishedAndNeverAssertsRelease() {
        val bus = CoreEventBus({ 100 }); bus.setEnabled(true)
        val events = ConversationEventPublisher(bus)
        val trace = events.accepted(session)
        events.started(trace); events.cancelRequested(trace)
        assertTrue(UiStateProjection.project(bus.state.value).busy)
        events.finished(trace, cancelled = true, failed = false)
        assertEquals("CANCELLED", UiStateProjection.project(bus.state.value).phase)
        assertFalse(UiStateProjection.project(bus.state.value).busy)
        assertEquals(4, bus.state.value.events.size)
        assertEquals(1, bus.state.value.events.map { it.envelope.requestId }.distinct().size)
    }
    @Test fun stopRequestRejectsLateCompletionAndDoesNotUnlockBeforeFinish() {
        val bus = CoreEventBus({ 100 }); bus.setEnabled(true)
        val events = ConversationEventPublisher(bus); val trace = events.accepted(session)
        events.started(trace); events.cancelRequested(trace); events.completed(trace)
        assertTrue(UiStateProjection.project(bus.state.value).busy)
        events.finished(trace, false, false)
        assertEquals("CANCELLED", bus.state.value.events.last().envelope.event.kind)
    }
    @Test fun completedTraceIsNotReplayedAsCancellation() {
        val bus = CoreEventBus({ 100 }); bus.setEnabled(true)
        val events = ConversationEventPublisher(bus)
        val trace = events.accepted(session)
        events.started(trace); events.completed(trace); events.finished(trace, true, false)
        assertEquals(listOf("USER_INPUT", "THINKING", "RESPONSE_COMPLETED"), bus.state.value.events.map { it.envelope.event.kind })
    }
    @Test fun disablingDuringRequestPreventsOldTerminalAfterReenable() {
        val bus = CoreEventBus({ 100 }); bus.setEnabled(true)
        val publisher = ConversationEventPublisher(bus); val trace = publisher.accepted(session)
        publisher.started(trace); bus.setEnabled(false); bus.setEnabled(true); publisher.completed(trace)
        assertTrue(bus.state.value.events.isEmpty())
    }
    @Test fun diagnosticJournalIsBoundedDeduplicatedAndContainsOnlyTechnicalFields() {
        var now = 100L
        val bus = CoreEventBus({ now }); bus.setEnabled(true)
        val publisher = ConversationEventPublisher(bus); val trace = publisher.accepted(session)
        publisher.started(trace); now = 250; publisher.completed(trace)
        val journal = BoundedDiagnosticsJournal(2); val projection = DiagnosticsProjection(journal)
        projection.accept(bus.state.value); projection.accept(bus.state.value)
        assertEquals(2, journal.snapshot().size)
        assertEquals(150L, journal.snapshot().last().durationMs)
        assertEquals("RESPONSE_COMPLETED", journal.snapshot().last().kind)
    }
    @Test fun concurrentPublishersKeepOneSequenceAndBoundedReplay() {
        val bus = CoreEventBus({ 100 }); bus.setEnabled(true)
        val generation = bus.generationIfEnabled()!!
        val pool = java.util.concurrent.Executors.newFixedThreadPool(4)
        try {
            val jobs = (1..128).map { pool.submit<Boolean> { bus.publish(event(), generation) } }
            assertTrue(jobs.all { it.get(10, java.util.concurrent.TimeUnit.SECONDS) })
            assertEquals((65L..128L).toList(), bus.state.value.events.map { it.sequence })
            assertEquals(64L, bus.state.value.dropped)
        } finally { pool.shutdownNow() }
    }
    @Test fun lateThinkingCannotOverrideCancellation() {
        val bus = CoreEventBus({ 100 }); bus.setEnabled(true)
        val request = event(CoreEvent.CancelRequested)
        val generation = bus.generationIfEnabled()!!
        assertTrue(bus.publish(request, generation))
        assertFalse(bus.publish(request.copy(event = CoreEvent.Thinking, eventId = UUID.randomUUID().toString()), generation))
        assertEquals("CANCEL_REQUESTED", UiStateProjection.project(bus.state.value).phase)
    }
    @Test fun absentModulesNeverConnectAndDistinguishDisabledFromUnavailable() = runTest {
        val registry = ConnectorRegistry.defaults()
        assertTrue(registry.health().values.all { it == ModuleHealth.DISABLED })
        val voice = registry.find("voice") as UnavailableModuleConnector<*>
        assertEquals(ConnectionResult.Refused(ModuleHealth.DISABLED), voice.connect())
        voice.setEnabled(true)
        assertEquals(ModuleHealth.UNAVAILABLE, voice.discover())
        assertEquals(ConnectionResult.Refused(ModuleHealth.UNAVAILABLE), voice.connect())
        voice.disconnect(); assertEquals(ModuleHealth.DISABLED, voice.health())
    }
    @Test(expected = IllegalArgumentException::class) fun invalidVersionIsRefused() { event().copy(version = 2) }
    @Test(expected = IllegalArgumentException::class) fun invalidCorrelationIsRefused() { event().copy(requestId = "bad") }
}
