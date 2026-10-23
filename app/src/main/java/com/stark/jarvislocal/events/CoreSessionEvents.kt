package com.stark.jarvislocal.events

import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Business correlation IDs identify a CORE operation, not an inferred backend request ID or lease. */
internal class ConversationTrace internal constructor(val sessionId: String, val requestId: String,
    internal val generation: Long) {
    internal var finished = false
    internal var stopRequested = false
}
/** Owned by the existing synchronized SessionCoordinator; no callbacks or additional scheduler. */
internal class ConversationEventPublisher(private val bus: CoreEventBus) {
    fun accepted(sessionId: String): ConversationTrace? {
        val generation = bus.generationIfEnabled() ?: return null
        val trace = ConversationTrace(sessionId, UUID.randomUUID().toString(), generation)
        return trace
    }
    fun started(trace: ConversationTrace?) {
        if (trace == null || trace.finished) return
        emit(trace, CoreEvent.UserInput)
        if (!trace.stopRequested) emit(trace, CoreEvent.Thinking)
    }
    fun completed(trace: ConversationTrace?) {
        if (trace == null || trace.finished || trace.stopRequested) return
        trace.finished = true
        emit(trace, CoreEvent.ResponseCompleted)
    }
    fun cancelRequested(trace: ConversationTrace?) {
        if (trace != null && !trace.finished && !trace.stopRequested) {
            trace.stopRequested = true
            emit(trace, CoreEvent.CancelRequested)
        }
    }
    fun finished(trace: ConversationTrace?, cancelled: Boolean, failed: Boolean, quarantined: Boolean = false) {
        if (trace == null || (trace.finished && !quarantined)) return
        trace.finished = true
        emit(trace, when {
            quarantined -> CoreEvent.Error(CoreEvent.Code.RESOURCE_UNCONFIRMED)
            cancelled || trace.stopRequested -> CoreEvent.Cancelled
            failed -> CoreEvent.Error(CoreEvent.Code.CORE_FAILURE)
            else -> CoreEvent.Error(CoreEvent.Code.MISSING_RESULT)
        })
    }
    private fun emit(trace: ConversationTrace, event: CoreEvent) {
        bus.publish(EventEnvelope(trace.sessionId, trace.requestId, event), trace.generation)
    }
}

data class EventUiState(val enabled: Boolean, val sessionId: String? = null, val requestId: String? = null,
    val busy: Boolean = false, val phase: String = "DISABLED", val replayGap: Boolean = false)
/** Observation only: subscription/resubscription never starts a request or calls an engine. */
class UiStateProjection(bus: CoreEventBus) {
    val state: Flow<EventUiState> = bus.state.map(::project).distinctUntilChanged()
    companion object {
        fun project(snapshot: EventSnapshot): EventUiState {
            val last = snapshot.events.lastOrNull()?.envelope
            return EventUiState(snapshot.enabled, last?.sessionId, last?.requestId,
                last?.event is CoreEvent.UserInput || last?.event is CoreEvent.Thinking ||
                    last?.event is CoreEvent.ResponseChunk || last?.event is CoreEvent.CancelRequested ||
                    last?.event is CoreEvent.ToolPending,
                if (!snapshot.enabled) "DISABLED" else last?.event?.kind ?: "IDLE", snapshot.dropped > 0)
        }
    }
}

/** Technical-only projection, suitable for a future EVOLUTION sink; no native release claim. */
data class DiagnosticEvent(val sequence: Long, val kind: String, val elapsedMs: Long, val durationMs: Long?) {
    init { require(kind in setOf("USER_INPUT", "THINKING", "RESPONSE_CHUNK", "RESPONSE_COMPLETED", "TOOL_PENDING", "SPEECH_REQUESTED", "CANCEL_REQUESTED", "CANCELLED", "ERROR")) }
}
fun interface DiagnosticsEventSink { fun record(event: DiagnosticEvent) }
class BoundedDiagnosticsJournal(private val capacity: Int = 128) : DiagnosticsEventSink {
    private val events = java.util.ArrayDeque<DiagnosticEvent>()
    init { require(capacity in 1..4096) }
    @Synchronized override fun record(event: DiagnosticEvent) {
        require(event.sequence >= 1 && event.elapsedMs >= 0 && (event.durationMs == null || event.durationMs >= 0))
        events.addLast(event)
        while (events.size > capacity) events.removeFirst()
    }
    @Synchronized fun snapshot(): List<DiagnosticEvent> = events.toList()
}
/** Caller explicitly feeds snapshots; replay is deduplicated and duration absent after an incomplete replay. */
class DiagnosticsProjection(private val sink: DiagnosticsEventSink) {
    private var lastSequence = 0L
    private val starts = linkedMapOf<Pair<String, String>, Long>()
    @Synchronized fun accept(snapshot: EventSnapshot) {
        for (observed in snapshot.events) {
            if (observed.sequence <= lastSequence) continue
            lastSequence = observed.sequence
            val envelope = observed.envelope
            val key = envelope.sessionId to envelope.requestId
            if (envelope.event is CoreEvent.UserInput) {
                starts[key] = observed.elapsedMs
                while (starts.size > 64) starts.remove(starts.keys.first())
            }
            val terminal = envelope.event is CoreEvent.ResponseCompleted || envelope.event is CoreEvent.Cancelled || envelope.event is CoreEvent.Error
            val started = if (terminal) starts.remove(key) else null
            sink.record(DiagnosticEvent(observed.sequence, envelope.event.kind, observed.elapsedMs,
                started?.let { (observed.elapsedMs - it).coerceAtLeast(0) }))
        }
    }
}
