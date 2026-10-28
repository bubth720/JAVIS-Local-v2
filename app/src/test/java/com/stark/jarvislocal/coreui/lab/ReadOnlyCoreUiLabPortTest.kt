package com.stark.jarvislocal.coreui.lab

import com.stark.jarvislocal.events.*
import com.stark.jarvislocal.integrations.*
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class ReadOnlyCoreUiLabPortTest {
    private val epoch = UUID.randomUUID().toString()
    private val session = UUID.randomUUID().toString()
    private val request = UUID.randomUUID().toString()
    private var now = 0L
    private val cert = "a".repeat(64)
    private val peer = SyntheticPeerAssertion(AssertionOrigin.SYNTHETIC_FIXTURE,"interface",1000,"fixture.interface",cert,60_000)
    private val policy = SyntheticPeerPolicy(1000,"fixture.interface",cert)
    private var source = EventSnapshot(enabled=true,generation=1)
    private val pinned = PinnedDocumentReadiness(UnavailableDocumentToolReadiness(ConnectorRegistry.defaults()))
    private fun port(enabled: Boolean = true, capacity: Int = 64) = ReadOnlyCoreUiLabPort({source},pinned,policy,{now},enabled,epoch,maxEvents=capacity)
    private fun binding() = LabBinding(epoch,session,request,1)
    private fun frame(kind: CoreEvent = CoreEvent.Thinking, seq: Long = 1, id: String = UUID.randomUUID().toString()) =
        ObservedEvent(EventEnvelope(session,request,kind,id),seq,0)
    private fun connected(): ReadOnlyCoreUiLabPort = port().also { assertEquals(LabPortState.UNTRUSTED,it.open(binding(),peer).state);it.acknowledge() }

    @Test fun defaultOffNeverReadsSourceAndNeverAuthenticates() {
        val p = ReadOnlyCoreUiLabPort({ error("must not read") },pinned,policy,{now})
        assertEquals(LabPortState.OFF,p.open(binding(),peer).state)
        assertEquals(LabPortState.OFF,p.poll(0).state)
        assertEquals(LabPortState.OFF,p.cancel().state)
        assertEquals(LabPortState.OFF,p.processDied().state)
    }
    @Test fun matchingSyntheticIdentityStillUntrustedNeverReady() {
        source=source.copy(events=listOf(frame()))
        val result=connected().poll(0)
        assertEquals(LabPortState.UNTRUSTED,result.state)
        assertEquals(LabProjectionOrigin.LABORATORY,result.origin)
        assertFalse(result.realCoreAuthenticated);assertFalse(result.canExecute)
        assertTrue(result.pinned.all { !it.canExecute })
    }
    @Test fun fakeOriginRefused() { assertEquals(LabRefusal.WRONG_ORIGIN,port().open(binding(),peer.copy(module="core")).reason) }
    @Test fun payloadClaimedUidAndPackageNeverAuthenticates() {
        assertEquals(LabRefusal.PAYLOAD_IDENTITY,port().open(binding(),peer.copy(origin=AssertionOrigin.PAYLOAD)).reason)
        assertEquals(LabRefusal.PAYLOAD_IDENTITY,connected().poll(0,mapOf("uid" to "1000","package" to "fixture.interface")).reason)
    }
    @Test fun differentUidRefused() { assertEquals(LabRefusal.UID_MISMATCH,port().open(binding(),peer.copy(uid=1001)).reason) }
    @Test fun differentPackageRefused() { assertEquals(LabRefusal.PACKAGE_MISMATCH,port().open(binding(),peer.copy(packageName="other")).reason) }
    @Test fun differentCertificateRefused() { assertEquals(LabRefusal.CERTIFICATE_MISMATCH,port().open(binding(),peer.copy(certificateSha256="b".repeat(64))).reason) }
    @Test fun expiredCertificateAssertionRefused() { assertEquals(LabRefusal.CERTIFICATE_EXPIRED,port().open(binding(),peer.copy(expiresAtMs=0)).reason) }
    @Test fun expiredAssertionAfterOpenBecomesUnknown() {
        val p=port();p.open(binding(),peer.copy(expiresAtMs=1));p.acknowledge();now=1
        assertEquals(LabPortState.UNKNOWN,p.poll(0).state)
    }
    @Test fun unknownProtocolVersionRefused() { assertEquals(LabRefusal.UNSUPPORTED_VERSION,port().open(binding().copy(version="unknown"),peer).reason) }
    @Test fun staleEpochRefused() { assertEquals(LabRefusal.STALE_EPOCH,port().open(binding().copy(epoch=UUID.randomUUID().toString()),peer).reason) }
    @Test fun changedBusGenerationBecomesUnknown() {
        val p=connected();source=source.copy(generation=2)
        assertEquals(LabRefusal.WRONG_GENERATION,p.poll(0).reason)
    }
    @Test fun repeatSourceSnapshotIsNotEmittedTwice() {
        source=source.copy(events=listOf(frame()));val p=connected()
        assertEquals(1,p.poll(0).events.size);assertTrue(p.poll(1).events.isEmpty())
    }
    @Test fun replayEventIdAtNewSequenceBecomesUnknown() {
        val one=frame();val p=connected();source=source.copy(events=listOf(one));p.poll(0)
        source=source.copy(events=listOf(one,frame(seq=2,id=one.envelope.eventId)))
        assertEquals(LabRefusal.REPLAY,p.poll(1).reason)
    }
    @Test fun oldClientCursorIsRejectedInsteadOfRetriggering() {
        source=source.copy(events=listOf(frame()));val p=connected();p.poll(0)
        assertEquals(LabRefusal.REPLAY,p.poll(0).reason)
    }
    @Test fun sequenceGapUnknownAndNoPartialBatchReturned() {
        source=source.copy(events=listOf(frame(),frame(seq=3)));val result=connected().poll(0)
        assertEquals(LabRefusal.SEQUENCE_GAP,result.reason);assertTrue(result.events.isEmpty())
    }
    @Test fun cancellationBeforeAckIsSticky() {
        val p=port();p.open(binding(),peer);p.cancel()
        assertEquals(LabRefusal.CANCELLED,p.acknowledge().reason)
        assertEquals(LabRefusal.CANCELLED,p.open(binding(),peer).reason)
    }
    @Test fun nativeEndIsNeverInferredFromLogicalCancellation() {
        val p=connected();source=source.copy(events=listOf(frame(CoreEvent.CancelRequested)))
        assertEquals(LabRefusal.CANCELLED,p.poll(0).reason)
        source=source.copy(events=listOf(frame(CoreEvent.ResponseCompleted)))
        assertEquals(LabRefusal.CANCELLED,p.poll(0).reason)
    }
    @Test fun processDeathNeverAllowsSameInstanceRetry() {
        val p=connected();p.processDied()
        assertEquals(LabRefusal.PROCESS_DEATH,p.open(binding(),peer).reason)
        assertFalse(p.poll(0).realCoreAuthenticated)
    }
    @Test fun retroactiveEventModificationRefused() {
        val event=frame();source=source.copy(events=listOf(event));val p=connected();p.poll(0)
        source=source.copy(events=listOf(event.copy(envelope=event.envelope.copy(event=CoreEvent.ResponseCompleted))))
        assertEquals(LabRefusal.RETROACTIVE_EVENT,p.poll(1).reason)
    }
    @Test fun retroactiveCapabilitiesClaimRefused() { assertEquals(LabRefusal.PAYLOAD_IDENTITY,connected().poll(0,mapOf("canExecute" to "true","state" to "READY")).reason) }
    @Test fun eventOutsideSessionRefusedWithoutMetadataLeak() {
        source=source.copy(events=listOf(frame().let { it.copy(envelope=it.envelope.copy(sessionId=UUID.randomUUID().toString())) }))
        val result=connected().poll(0);assertEquals(LabRefusal.WRONG_SESSION,result.reason);assertTrue(result.events.isEmpty())
    }
    @Test fun sourceFailureIsRedacted() {
        val p=ReadOnlyCoreUiLabPort({error("private-data-not-returned")},pinned,policy,{now},true,epoch)
        assertEquals(LabRefusal.SOURCE_UNAVAILABLE,p.open(binding(),peer).reason)
    }
    @Test fun missingAckDoesNotConsumeEvents() {
        val p=port();source=source.copy(events=listOf(frame()));p.open(binding(),peer)
        assertEquals(LabRefusal.ACK_REQUIRED,p.poll(0).reason);p.acknowledge();assertEquals(1,p.poll(0).events.size)
    }
    @Test fun timeoutAndClockRollbackFailClosed() {
        val p=connected();now=30_000;assertEquals(LabRefusal.EXPIRED,p.poll(0).reason)
        now=100;val q=connected();now=99;assertEquals(LabRefusal.CLOCK_ROLLBACK,q.poll(0).reason)
    }
    @Test fun droppedWindowIsNotSilentlyAccepted() {
        source=source.copy(dropped=1);assertEquals(LabRefusal.REPLAY_WINDOW_LOST,connected().poll(0).reason)
    }
    @Test fun realExistingBusIsReadOnlyAndHasNoTextOrCitations() {
        val bus=CoreEventBus({0});bus.setEnabled(true)
        assertTrue(bus.publish(EventEnvelope(session,request,CoreEvent.UserInput),1))
        val before=bus.state.value
        val p=ReadOnlyCoreUiLabPort({bus.state.value},pinned,policy,{0},true,epoch)
        p.open(binding(),peer);p.acknowledge();val result=p.poll(0)
        assertEquals("USER_INPUT",result.events.single().kind);assertEquals(before,bus.state.value)
        assertFalse(result.realCoreAuthenticated);assertTrue(result.pinned.all { !it.canExecute })
    }
}
