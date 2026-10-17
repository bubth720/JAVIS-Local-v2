package com.stark.jarvislocal.fixture

import com.stark.jarvislocal.InferenceRequest
import fr.jarvis.fixture.p2.IP2Endpoint
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.app.Application

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class)
class P2ContractTest {
    private fun synthetic()=InferenceRequest(id(),id(),"Réponds brièvement en français. /no_think","Combien font deux plus deux ?","")
    private fun key(r: InferenceRequest)=PilotKey(r.sessionId,r.requestId,id(),id(),id(),id(),id(),1)
    private fun fields(): Map<String,String> { val r=synthetic(); return P2Mapping.request(r,key(r),"SMALL","a".repeat(64),"ECO",1000) }
    private fun receipt(f: Map<String,String>)=f.filterKeys { it in Wire.keyNames } + mapOf(
        "request_digest" to Wire.digest(f),"phase" to "UNLOADED","code" to "NONE","native_ever_started" to "true",
        "effective_end" to "true","model_unloaded" to "true","release_confirmed" to "true","model_sha256" to f.getValue("model_sha256"),
        "template_sha256" to "b".repeat(64),"rendered_sha256" to "c".repeat(64),"input_tokens" to "20",
        "context_tokens" to f.getValue("context_tokens"),"loaded_context_tokens" to f.getValue("context_tokens"),"threads" to "2",
        "output_tokens" to f.getValue("output_tokens"),"text" to "Réponse SIMULÉE de test, aucune preuve JNI.")
    @Test fun publishedCrossRepositoryDigestVectorMatches() {
        val json=javaClass.getResourceAsStream("/p2-request.json")!!.readBytes()
        val expected=javaClass.getResourceAsStream("/p2-request.sha256")!!.bufferedReader().readText().trim().split(" ")[0]
        val f=Wire.decode(json,Wire.requestNames)
        assertEquals(expected,Wire.digest(f)); assertEquals(Wire.contract,f["contract"])
    }
    @Test fun exactSystemQuestionAndServerEpochArePreserved() {
        val r=synthetic(); val k=key(r); val f=P2Mapping.request(r,k,"SMALL","a".repeat(64),"ECO",1000)
        assertEquals(Wire.requestNames,f.keys); assertEquals(r.systemPrompt,f["system"]); assertEquals(r.question,f["question"])
        assertEquals(k.epoch,f["epoch"]); assertEquals(P2Mapping.key(k),f.filterKeys { it in Wire.keyNames })
        assertEquals(f,Wire.decode(Wire.encode(f),Wire.requestNames))
    }
    @Test fun privateContextAndNonPilotTextNeverReachTransport() {
        for(r in listOf(synthetic().copy(context=" "),synthetic().copy(question="private question"),synthetic().copy(systemPrompt="personal prompt")))
            assertThrows(PilotBlocked::class.java) { P2Mapping.request(r,key(r),"SMALL","a".repeat(64),"ECO",1000) }
    }
    @Test fun incompatibleModelProfileDurationAndMainBudgetAreRefused() {
        val r=synthetic()
        for((model,profile,time) in listOf(Triple("OTHER","ECO",1000L),Triple("SMALL","FULL_POWER",1000L),Triple("SMALL","ECO",90001L)))
            assertThrows(PilotBlocked::class.java) { P2Mapping.request(r,key(r),model,"a".repeat(64),profile,time) }
        val small=r.copy(limits=r.limits.copy(contextSize=512))
        assertThrows(PilotBlocked::class.java) { P2Mapping.request(small,key(small),"MAIN","a".repeat(64),"ECO",1000) }
    }
    @Test fun strictJsonRejectsDuplicateUnknownNumberMalformedUtf8AndOversize() {
        for(bytes in listOf("{\"x\":\"a\",\"x\":\"b\"}".toByteArray(),"{\"x\":1}".toByteArray(),
            "{\"x\":\"a\",\"y\":\"b\"}".toByteArray(),byteArrayOf(0xc3.toByte()),ByteArray(24577)))
            assertThrows(Refusal::class.java) { Wire.decode(bytes,setOf("x")) }
    }
    @Test fun everyCorrelationIdentifierAndDigestMustMatch() {
        val f=fields(); val response=receipt(f)
        for(field in Wire.keyNames + "request_digest")
            assertThrows(PilotBlocked::class.java) { P2Mapping.correlate(f,response + (field to "stale")) }
    }
    @Test fun cleanupAndNativeEvidenceRequiredBeforeText() {
        val f=fields(); val response=receipt(f)
        assertTrue(P2Mapping.finalText(f,response).contains("SIMULÉE"))
        for(field in listOf("effective_end","model_unloaded","release_confirmed","native_ever_started"))
            assertThrows(PilotBlocked::class.java) { P2Mapping.finalText(f,response + (field to "false")) }
        for(field in listOf("model_sha256","template_sha256","rendered_sha256","output_tokens","context_tokens"))
            assertThrows(PilotBlocked::class.java) { P2Mapping.finalText(f,response + (field to "wrong")) }
    }
    @Test fun effectiveNativeLimitsMayBeLowerButNeverExceedRequestedBudget() {
        val f=fields(); val smaller=receipt(f)+mapOf("context_tokens" to "512","loaded_context_tokens" to "768","output_tokens" to "64")
        assertTrue(P2Mapping.finalText(f,smaller).contains("SIMULÉE"))
        for(change in listOf(mapOf("output_tokens" to "257"),mapOf("context_tokens" to "2304"),mapOf("input_tokens" to "2000")))
            assertThrows(PilotBlocked::class.java) { P2Mapping.finalText(f,smaller+change) }
    }
    @Test fun authorityCannotGrantUnknownOrReplayedQuery() {
        val arbiter=PilotArbiter(setOf("llm","historical"),MemoryJournal()){10}; arbiter.recover()
        val grants=GrantRegistry(arbiter){10}; val r=synthetic(); val k=arbiter.reserve(r,arbiter.authorization(r))
        val f=P2Mapping.request(r,k,"SMALL","a".repeat(64),"ECO",990); val q=P2Mapping.query(f,20,30)
        assertThrows(PilotBlocked::class.java) { grants.approve(q,30,20) }
        grants.register(q)
        assertThrows(PilotBlocked::class.java) { grants.approve(q,31,20) }
        assertEquals("true",grants.approve(q,30,20)["approved"])
        assertThrows(PilotBlocked::class.java) { grants.approve(q,30,20) }
    }
    @Test fun authorityExpiresWithoutFreeingReservation() {
        var now=10L; val a=PilotArbiter(setOf("llm","historical"),MemoryJournal()){now}; a.recover()
        val g=GrantRegistry(a){now}; val r=synthetic(); val k=a.reserve(r,a.authorization(r))
        val q=P2Mapping.query(P2Mapping.request(r,k,"SMALL","a".repeat(64),"ECO",990),20,30); g.register(q)
        now=1001; assertThrows(PilotBlocked::class.java) { g.approve(q,30,20) }
        assertEquals(k,a.snapshot().key); assertEquals(PilotPhase.RECOVERY_REQUIRED,a.snapshot().phase)
    }
    private inner class Harness {
        var now=10L
        val a=PilotArbiter(setOf("llm","historical"),MemoryJournal()){now}.also { it.recover() }
        val g=GrantRegistry(a){now}
        lateinit var sent: Map<String,String>
        var answer: Map<String,String>?=null
        var failSubmit=false
        val endpoint=object : IP2Endpoint.Stub() {
            override fun hello(payload: ByteArray,clientLiveness: android.os.IBinder)=error("not used")
            override fun submit(payload: ByteArray): ByteArray {
                sent=Wire.decode(payload,Wire.requestNames)
                if(failSubmit) error("simulated ACK lost")
                g.approve(P2Mapping.query(sent,20,30),30,20)
                return Wire.encode(receipt(sent) + mapOf("phase" to "RUNNING","effective_end" to "false","model_unloaded" to "false","release_confirmed" to "false","text" to ""))
            }
            override fun status(key: ByteArray)=Wire.encode(answer ?: receipt(sent))
            override fun cancel(key: ByteArray)=status(key)
        }
        val s=P2Session(endpoint,a,g,{now},20,30)
        fun submit() { val r=synthetic(); s.submit(r,a.authorization(r),id(),"a".repeat(64)) }
    }
    @Test fun simulatedGenerationCorrelatesAndReleases() {
        val h=Harness(); h.submit(); assertTrue(h.s.poll()!!.contains("SIMULÉE")); assertEquals(PilotPhase.IDLE,h.a.snapshot().phase)
    }
    @Test fun cancellationNeverPersistsLateAssistantText() {
        val h=Harness(); h.submit(); h.s.cancel(); assertNull(h.s.poll()); assertEquals(PilotPhase.IDLE,h.a.snapshot().phase)
    }
    @Test fun lostSubmitAckHoldsLeaseWithoutReplay() {
        val h=Harness(); h.failSubmit=true; assertThrows(IllegalStateException::class.java) { h.submit() }
        assertEquals(PilotPhase.RECOVERY_REQUIRED,h.a.snapshot().phase); assertNotNull(h.a.snapshot().key)
    }
    @Test fun timeoutAndDisconnectRequireSeparateReconciliation() {
        for(timeout in listOf(true,false)) {
            val h=Harness(); h.submit(); if(timeout) h.now=1001 else h.s.disconnected()
            assertThrows(PilotBlocked::class.java) { h.s.poll() }; assertNotNull(h.a.snapshot().key)
        }
    }
    @Test fun obsoleteReceiptIsRejectedWithoutReleasingOwner() {
        val h=Harness(); h.submit(); h.answer=receipt(h.sent) + ("epoch" to id())
        assertThrows(PilotBlocked::class.java) { h.s.poll() }; assertNotNull(h.a.snapshot().key)
    }
    @Test fun failedUnloadNeverAdmitsNextOperation() {
        val h=Harness(); h.submit(); h.answer=receipt(h.sent)+mapOf("phase" to "CLEANUP_FAILED","model_unloaded" to "false","release_confirmed" to "false","text" to "")
        assertThrows(PilotBlocked::class.java) { h.s.poll() }; assertNotNull(h.a.snapshot().key)
        assertThrows(PilotBlocked::class.java) { h.submit() }
    }
    @Test fun productionStartupHasNoSyntheticRecoveryOrAdmission() {
        val a=PilotArbiter(setOf("llm"),MemoryJournal()){10}
        val r=synthetic(); assertThrows(PilotBlocked::class.java) { a.reserve(r,a.authorization(r)) }
        assertEquals(PilotPhase.RECOVERY_REQUIRED,a.snapshot().phase)
    }
}
