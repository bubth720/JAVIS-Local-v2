package com.stark.jarvislocal.fixture

import com.stark.jarvislocal.InferenceRequest
import java.util.UUID

// Local admission state; transport mapping is in P2Mapping.
internal enum class PilotPhase { IDLE, RESERVED, RUNNING, STOP_REQUESTED, RECOVERY_REQUIRED }
internal enum class ExecutionProof { NOT_STARTED, NATIVE_RETURNED, UNKNOWN }
internal enum class CleanupProof { RELEASED, UNKNOWN, FAILED }
internal data class PilotKey(val sessionId: String, val operationId: String, val correlationId: String,
    val epoch: String, val cancellationId: String, val authorizationRef: String, val leaseId: String, val generation: Long)
internal data class PilotRecord(val owner: String?, val key: PilotKey?, val generation: Long,
    val phase: PilotPhase, val deadline: Long, val cancelled: Boolean, val lastReleased: PilotKey?)
internal interface PilotJournal { fun read(): PilotRecord?; fun write(record: PilotRecord) }
internal data class SyntheticAuthorization(val owner: String, val sessionId: String, val operationId: String,
    val ref: String, val brokerEpoch: String, val deadline: Long)
internal data class ReleaseEvidence(val owner: String, val key: PilotKey, val execution: ExecutionProof, val cleanup: CleanupProof)
internal data class RecoveryEvidence(val owner: String, val key: PilotKey?, val challenge: String,
    val execution: ExecutionProof, val cleanup: CleanupProof)

/** Persistent fail-closed state machine. Evidence is trusted-local input, NOT authenticated IPC. */
internal class PilotArbiter(private val participants: Set<String>, private val journal: PilotJournal, private val clock: () -> Long) {
    val epoch: String = UUID.randomUUID().toString()
    val recoveryChallenge: String = UUID.randomUUID().toString()
    private var healthy = true
    private val usedOperations = mutableSetOf<String>()
    private var record: PilotRecord

    init {
        require(participants.all { it.matches(Regex("[A-Za-z0-9._-]{1,80}")) })
        val old = journal.read() // Corrupt/unknown journal must fail construction; never reset it.
        record = (old ?: PilotRecord(null, null, 0, PilotPhase.IDLE, 0, false, null))
            .copy(phase = PilotPhase.RECOVERY_REQUIRED, cancelled = true)
        record.key?.let { usedOperations += it.operationId }
        record.lastReleased?.let { usedOperations += it.operationId }
        persist(record)
    }

    @Synchronized fun snapshot(): PilotRecord = record

    @Synchronized fun reconcile(evidence: List<RecoveryEvidence>) {
        requirePilot(healthy && participants.isNotEmpty(), "RECOVERY_UNAVAILABLE")
        requirePilot(record.phase == PilotPhase.RECOVERY_REQUIRED, "NOT_RECOVERING")
        requirePilot(evidence.size == participants.size && evidence.map { it.owner }.toSet() == participants, "ALL_PARTICIPANTS_REQUIRED")
        requirePilot(evidence.all {
            it.challenge == recoveryChallenge && it.execution != ExecutionProof.UNKNOWN && it.cleanup == CleanupProof.RELEASED &&
                it.key == if (it.owner == record.owner) record.key else null
        }, "RECOVERY_NOT_ATTESTED")
        persist(PilotRecord(null, null, record.generation, PilotPhase.IDLE, 0, false, record.key ?: record.lastReleased))
    }

    @Synchronized fun reserve(request: InferenceRequest, authorization: SyntheticAuthorization, serverEpoch: String = epoch): PilotKey {
        requirePilot(healthy && record.phase == PilotPhase.IDLE, "ADMISSION_BLOCKED")
        PilotRequestGate.validate(request)
        requirePilot(authorization.owner in participants && authorization.sessionId == request.sessionId &&
            authorization.operationId == request.requestId && authorization.brokerEpoch == epoch && canonicalUuid(authorization.ref), "UNAUTHORIZED_SESSION")
        requirePilot(authorization.deadline > clock() && authorization.deadline - clock() <= 90_000, "EXPIRED_OR_INVALID_DEADLINE")
        requirePilot(usedOperations.size < 256 && request.requestId !in usedOperations, "REPLAY_OR_QUOTA")
        requirePilot(record.generation < 9007199254740991L && canonicalUuid(serverEpoch), "GENERATION_EXHAUSTED")
        val key = PilotKey(request.sessionId, request.requestId, UUID.randomUUID().toString(), serverEpoch,
            UUID.randomUUID().toString(), authorization.ref, UUID.randomUUID().toString(), record.generation + 1)
        // Write before returning any grant. Failure means no participant may start work.
        persist(PilotRecord(authorization.owner, key, key.generation, PilotPhase.RESERVED, authorization.deadline, false, record.lastReleased))
        usedOperations += request.requestId
        return key
    }

    @Synchronized fun running(key: PilotKey) {
        active(key)
        requirePilot(record.phase == PilotPhase.RESERVED, "INVALID_PHASE")
        persist(record.copy(phase = PilotPhase.RUNNING))
    }

    @Synchronized fun requestStop(key: PilotKey) {
        active(key)
        persist(record.copy(phase = PilotPhase.STOP_REQUESTED, cancelled = true))
    }

    @Synchronized fun connectionLost() {
        persist(record.copy(phase = PilotPhase.RECOVERY_REQUIRED, cancelled = true))
    }

    @Synchronized fun expire() {
        if (record.key != null && clock() >= record.deadline) connectionLost()
    }

    /** Return permission to consume a result only AFTER an exact, explicit cleanup receipt. */
    @Synchronized fun complete(evidence: ReleaseEvidence): Boolean {
        active(evidence.key)
        requirePilot(evidence.owner == record.owner, "WRONG_OWNER")
        if (evidence.execution == ExecutionProof.UNKNOWN || evidence.cleanup != CleanupProof.RELEASED) {
            connectionLost()
            return false
        }
        val consumable = !record.cancelled && evidence.execution == ExecutionProof.NATIVE_RETURNED && clock() < record.deadline
        persist(PilotRecord(null, null, record.generation, PilotPhase.IDLE, 0, false, evidence.key))
        return consumable
    }

    @Synchronized fun wasReleased(key: PilotKey): Boolean = record.lastReleased == key

    private fun active(key: PilotKey) {
        requirePilot(healthy && record.phase != PilotPhase.RECOVERY_REQUIRED, "RECONCILIATION_REQUIRED")
        requirePilot(record.key == key, "STALE_OPERATION")
    }

    private fun persist(next: PilotRecord) {
        try { journal.write(next) }
        catch (error: Exception) { healthy = false; throw PilotBlocked("JOURNAL_IO_BLOCKED") }
        record = next
    }
}
