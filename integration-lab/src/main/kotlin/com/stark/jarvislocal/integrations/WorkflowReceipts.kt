package com.stark.jarvislocal.integrations

import fr.jarvis.services.shared.storage.Store

/** Technical receipts in the existing SERVICES store. Never permissions, memory, prompts or retry grants.
 * One owner per synthetic host; call recoverInterrupted only after the previous owner has stopped.
 * No cross-process fencing or native completion proof is provided by this JVM laboratory.
 */
class WorkflowReceipts(private val store: Store) {
    enum class State { PREPARED, DISPATCHED, COMPLETED, REFUSED, UNKNOWN }
    @Synchronized fun begin(c: Correlation) = store.transaction {
        if (store.get(NS, c.requestId) != null) throw IntegrationRefused("WORKFLOW_REPLAY_OR_UNKNOWN")
        if (store.count(NS) >= 256) throw IntegrationRefused("RECEIPT_QUOTA")
        write(c, State.PREPARED)
    }
    @Synchronized fun transition(c: Correlation, next: State) = store.transaction {
        val old = read(c.requestId) ?: throw IntegrationRefused("RECEIPT_ABSENT")
        if (old.first != c) throw IntegrationRefused("RECEIPT_CORRELATION")
        val allowed = when (old.second) {
            State.PREPARED -> setOf(State.DISPATCHED, State.REFUSED, State.UNKNOWN)
            State.DISPATCHED -> setOf(State.COMPLETED, State.UNKNOWN)
            else -> emptySet()
        }
        if (next !in allowed) throw IntegrationRefused("RECEIPT_TERMINAL_OR_INVALID")
        write(c, next)
    }
    @Synchronized fun state(id: String): State? = read(id)?.second
    @Synchronized fun recoverInterrupted() = store.transaction {
        // Validate the whole namespace before any update; a corrupt receipt never resets the journal.
        if (store.count(NS) > 256) throw IntegrationRefused("RECEIPT_QUOTA")
        val records = store.all(NS).map(::decode)
        for ((c, state) in records) if (read(c.requestId) != (c to state)) throw IntegrationRefused("RECEIPT_CORRUPT")
        for ((c, state) in records) if (state in setOf(State.PREPARED, State.DISPATCHED)) write(c, State.UNKNOWN)
    }
    private fun read(id: String): Pair<Correlation, State>? = try {
        store.get(NS, id)?.let { decode(it).also { row -> if (row.first.requestId != id) throw IntegrationRefused("RECEIPT_CORRUPT") } }
    } catch (_: Exception) { throw IntegrationRefused("RECEIPT_CORRUPT") }
    private fun decode(row: Map<String, Any>): Pair<Correlation, State> = try {
        require(row.keys == setOf("schema", "session", "request", "generation", "state"))
        require(row["schema"] == "core.workflow-receipt/1")
        val c = Correlation(row["session"] as String, row["request"] as String, (row["generation"] as String).toLong())
        c to State.valueOf(row["state"] as String)
    } catch (_: Exception) { throw IntegrationRefused("RECEIPT_CORRUPT") }
    private fun write(c: Correlation, state: State) {
        store.put(NS, c.requestId, mapOf("schema" to "core.workflow-receipt/1", "session" to c.sessionId,
            "request" to c.requestId, "generation" to c.generation.toString(), "state" to state.name))
        if (read(c.requestId) != (c to state)) throw IntegrationRefused("RECEIPT_UNCONFIRMED")
    }
    companion object { internal const val NS = "core_workflow_receipts" }
}
