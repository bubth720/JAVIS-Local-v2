package com.stark.jarvislocal.fixture

import com.stark.jarvislocal.InferenceRequest
import java.util.UUID

internal fun id() = UUID.randomUUID().toString()
internal fun request() = InferenceRequest(id(),id(),"SYSTEM synthétique distinct", "Combien font 7 + 5 ?", "")
internal class MemoryJournal : PilotJournal {
    var record: PilotRecord? = null
    var failWrite = false
    override fun read() = record
    override fun write(record: PilotRecord) { if (failWrite) error("synthetic IO"); this.record = record }
}
internal fun PilotArbiter.recover(ownerKey: PilotKey? = null, owner: String = "llm") {
    reconcile(listOf(RecoveryEvidence("llm", if (owner == "llm") ownerKey else null, recoveryChallenge, ExecutionProof.NATIVE_RETURNED, CleanupProof.RELEASED),
        RecoveryEvidence("historical", if (owner == "historical") ownerKey else null, recoveryChallenge, ExecutionProof.NOT_STARTED, CleanupProof.RELEASED)))
}
internal fun PilotArbiter.authorization(request: InferenceRequest, owner: String = "llm", deadline: Long = 1000) =
    SyntheticAuthorization(owner,request.sessionId,request.requestId,id(),epoch,deadline)
internal fun released(key: PilotKey, owner: String = "llm") = ReleaseEvidence(owner,key,ExecutionProof.NATIVE_RETURNED,CleanupProof.RELEASED)
