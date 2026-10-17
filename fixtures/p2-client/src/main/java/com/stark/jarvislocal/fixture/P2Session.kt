package com.stark.jarvislocal.fixture

import com.stark.jarvislocal.InferenceRequest
import fr.jarvis.fixture.p2.IP2Endpoint

/** Blocking IPC calls must run off the UI thread. No retries of submit, no fallback. */
internal class P2Session(private val remote: IP2Endpoint, private val arbiter: PilotArbiter,
    private val grants: GrantRegistry, private val clock: () -> Long, private val clientUid: Int, private val serverUid: Int) {
    private var active: Pair<PilotKey,Map<String,String>>? = null
    private var stopped=false
    @Synchronized fun submit(request: InferenceRequest, authorization: SyntheticAuthorization, serverEpoch: String,
        sha: String, model: String = "SMALL", profile: String = "ECO") {
        requirePilot(active == null,"BUSY_SESSION")
        // Validate before making a durable reservation, then map the assigned full key.
        val placeholder=PilotKey(request.sessionId,request.requestId,serverEpoch,serverEpoch,serverEpoch,authorization.ref,serverEpoch,1)
        P2Mapping.request(request,placeholder,model,sha,profile,authorization.deadline-clock())
        val key=arbiter.reserve(request,authorization,serverEpoch)
        val fields=P2Mapping.request(request,key,model,sha,profile,authorization.deadline-clock())
        grants.register(P2Mapping.query(fields,clientUid,serverUid))
        active=key to fields; stopped=false
        try { P2Mapping.correlate(fields,Wire.decode(remote.submit(Wire.encode(fields)),P2Mapping.receiptNames)) }
        catch (e: Exception) { arbiter.connectionLost(); throw e }
    }
    @Synchronized fun cancel() {
        val (key,_)=active ?: throw PilotBlocked("NO_OPERATION")
        stopped=true; arbiter.requestStop(key)
        try { remote.cancel(Wire.encode(P2Mapping.key(key))) }
        catch (e: Exception) { arbiter.connectionLost(); throw e }
    }
    @Synchronized fun poll(): String? {
        val (key,fields)=active ?: throw PilotBlocked("NO_OPERATION")
        try {
            arbiter.expire()
            val receipt=Wire.decode(remote.status(Wire.encode(P2Mapping.key(key))),P2Mapping.receiptNames)
            P2Mapping.correlate(fields,receipt)
            if (receipt["phase"] in setOf("UNKNOWN","CLEANUP_FAILED","REJECTED")) {
                arbiter.connectionLost(); throw PilotBlocked("RECONCILIATION_REQUIRED")
            }
            if (!P2Mapping.released(receipt)) return null
            // Recovery after loss/timeout requires a separate fresh reconciliation, never poll-based reset.
            val permitted=arbiter.complete(ReleaseEvidence("llm",key,ExecutionProof.NATIVE_RETURNED,CleanupProof.RELEASED))
            active=null
            return if (permitted && !stopped && receipt["code"] == "NONE") P2Mapping.finalText(fields,receipt) else null
        } catch (e: Exception) { arbiter.connectionLost(); throw e }
    }
    fun disconnected() { arbiter.connectionLost() }
}
