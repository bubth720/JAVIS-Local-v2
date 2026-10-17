package com.stark.jarvislocal.fixture

import android.content.Context
import android.os.SystemClock
import java.io.File

/** No first-run assumption that another process has no native model. No reset API. */
internal class PilotRuntime(context: Context) {
    val arbiter = PilotArbiter(setOf("llm"),AtomicPilotJournal(File(context.filesDir,"pilot.journal")),SystemClock::elapsedRealtime)
    val grants = GrantRegistry(arbiter,SystemClock::elapsedRealtime)
    companion object {
        @Volatile private var instance: PilotRuntime? = null
        @Synchronized fun get(context: Context): PilotRuntime = instance ?: PilotRuntime(context.applicationContext).also { instance=it }
    }
}

/** Stores only opaque protocol fields and hashes; never a question, SYSTEM, URI or answer. */
internal class GrantRegistry(private val arbiter: PilotArbiter, private val clock: () -> Long) {
    private var registered: Map<String,String>? = null
    @Synchronized fun register(query: Map<String,String>) {
        val state=arbiter.snapshot()
        requirePilot(state.phase == PilotPhase.RESERVED && state.owner == "llm" && state.key != null, "NO_DURABLE_RESERVATION")
        requirePilot(query.keys == Wire.keyNames + setOf("request_digest","model","model_sha256","profile","context_tokens","output_tokens","remaining_ms","client_uid","server_uid") &&
            P2Mapping.key(state.key ?: throw PilotBlocked("NO_DURABLE_RESERVATION")).all { query[it.key] == it.value }, "UNREGISTERED_REQUEST")
        requirePilot(registered == null, "GRANT_ALREADY_REGISTERED")
        registered=query.toMap()
    }
    @Synchronized fun approve(query: Map<String,String>, callerUid: Int, ownUid: Int): Map<String,String> {
        arbiter.expire()
        val state=arbiter.snapshot()
        requirePilot(query == registered && query["server_uid"] == callerUid.toString() && query["client_uid"] == ownUid.toString(), "UNREGISTERED_AUTHORITY_QUERY")
        requirePilot(state.phase == PilotPhase.RESERVED && state.key != null && state.deadline > clock(), "ADMISSION_BLOCKED")
        // A duplicate authority request cannot obtain a second grant.
        arbiter.running(state.key ?: throw PilotBlocked("NO_DURABLE_RESERVATION"))
        registered=null
        return query + mapOf("approved" to "true","expires_elapsed_ms" to state.deadline.toString())
    }
}
