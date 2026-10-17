package com.stark.jarvislocal

import java.util.UUID

internal enum class NativeReleaseState { UNKNOWN, RUNNING, STOP_REQUESTED, DONE_RESIDENT, DONE_UNLOADED, FAILED }
internal data class NativeReleaseSnapshot(val epoch: String,val generation: Long,val llm: NativeReleaseState,val whisper: NativeReleaseState)
/** Trusted in-process boundary. No implementation in this branch attests the historical JNI engines. */
internal fun interface NativeReleasePort { fun snapshot(): NativeReleaseSnapshot? }
internal data class HandoffTicket(val epoch: String,val generation: Long,val session: String,val correlation: String,val lease: String,val deadline: Long)
internal enum class HandoffState { BLOCKED, RESERVED, STOP_REQUESTED, UNKNOWN, RELEASED }

/** Gate model for isolated tests, NOT an interprocess arbiter or Android memory reservation.
 * Production defaults to unavailable; no Binder lease issuer or activation path is wired. */
internal class NativeHandoffGate(private val native: NativeReleasePort = NativeReleasePort { null },private val clock: () -> Long) {
    val epoch: String = UUID.randomUUID().toString()
    var state=HandoffState.BLOCKED;private set
    private var active: HandoffTicket?=null
    private var generation=0L
    private val seen=mutableSetOf<Pair<String,String>>()
    @Synchronized fun reserve(session: String,correlation: String,ttlMs: Long): HandoffTicket {
        check(active == null) { "HANDOFF_HELD" }
        require(listOf(session,correlation).all { runCatching { UUID.fromString(it).toString() == it }.getOrDefault(false) })
        require(ttlMs in 1..90000)
        val proof=native.snapshot()
        check(proof != null && proof.epoch == epoch && proof.generation == generation &&
            proof.llm == NativeReleaseState.DONE_UNLOADED && proof.whisper == NativeReleaseState.DONE_UNLOADED) { "NATIVE_RELEASE_UNATTESTED" }
        check(seen.size < 128 && (session to correlation) !in seen) { "REPLAY_OR_CAPACITY" }
        val ticket=HandoffTicket(epoch,++generation,session,correlation,UUID.randomUUID().toString(),clock()+ttlMs)
        seen+=session to correlation;active=ticket;state=HandoffState.RESERVED;return ticket
    }
    @Synchronized fun stop(ticket: HandoffTicket) { check(active == ticket);state=HandoffState.STOP_REQUESTED }
    @Synchronized fun lostOrExpired() { if(active != null) state=HandoffState.UNKNOWN }
    @Synchronized fun expire() { if(active?.let { clock() >= it.deadline } == true) lostOrExpired() }
    /** Internal evidence only; a future IPC caller must never supply these booleans as authority. */
    @Synchronized fun confirmReleased(ticket: HandoffTicket,nativeDone: Boolean,audioStopped: Boolean,modelUnloaded: Boolean) {
        check(active == ticket && state != HandoffState.UNKNOWN) { "RECONCILIATION_REQUIRED" }
        if(!nativeDone || !audioStopped || !modelUnloaded) { state=HandoffState.UNKNOWN;error("RELEASE_UNATTESTED") }
        active=null;state=HandoffState.RELEASED
    }
    @Synchronized fun accepts(ticket: HandoffTicket)=active == ticket && state == HandoffState.RESERVED && clock() < ticket.deadline
}
