package com.stark.jarvislocal.fixture

import android.app.Application
import android.content.ComponentName
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class)
class AndroidFixtureTest {
    @Test fun journalReopensAndRestartRemainsBlockedWithoutNewEvidence() {
        val file=File(RuntimeEnvironment.getApplication().filesDir,"pilot-test.journal")
        val a=PilotArbiter(setOf("llm","historical"),AtomicPilotJournal(file)){10}
        a.recover(); val r=request(); val key=a.reserve(r,a.authorization(r)); a.running(key)
        val b=PilotArbiter(setOf("llm","historical"),AtomicPilotJournal(file)){10}
        assertEquals(key,b.snapshot().key); assertEquals(PilotPhase.RECOVERY_REQUIRED,b.snapshot().phase)
        assertFalse(file.readText(Charsets.ISO_8859_1).contains(r.question))
        assertFalse(file.readText(Charsets.ISO_8859_1).contains(r.systemPrompt))
        b.recover(key); assertEquals(PilotPhase.IDLE,b.snapshot().phase)
    }
    @Test fun corruptJournalIsNotReset() {
        val file=File(RuntimeEnvironment.getApplication().filesDir,"corrupt-test.journal")
        val bytes=byteArrayOf(1,2,3); file.writeBytes(bytes)
        assertThrows(PilotBlocked::class.java) { AtomicPilotJournal(file).read() }
        assertArrayEquals(bytes,file.readBytes())
    }
    @Test fun callerUidIsCapturedInsideLocalBinderTransactionBeforeDispatch() {
        val expectedUid=Binder.getCallingUid(); var captured=-1
        val digest="a".repeat(64)
        val guard=BinderCallerGuard { uid -> captured=uid; PeerFacts(uid,setOf("test.llm"),setOf(digest)) }
        val binder=object : Binder() {
            override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
                guard.capture(PeerPin(expectedUid,"test.llm",digest)); return true
            }
        }
        val data=Parcel.obtain(); val reply=Parcel.obtain()
        try { assertTrue(binder.transact(IBinder.FIRST_CALL_TRANSACTION,data,reply,0)); assertEquals(expectedUid,captured) }
        finally { data.recycle(); reply.recycle() }
    }
    @Test fun mismatchedUidPackageCertificateAndSharedUidAreRefused() {
        val pin=PeerPin(42,"test.llm","a".repeat(64))
        requirePeer(PeerFacts(42,setOf("test.llm"),setOf(pin.certificateSha256)),pin)
        for (facts in listOf(PeerFacts(43,setOf("test.llm"),setOf(pin.certificateSha256)),
            PeerFacts(42,setOf("other"),setOf(pin.certificateSha256)),
            PeerFacts(42,setOf("test.llm","other"),setOf(pin.certificateSha256)),
            PeerFacts(42,setOf("test.llm"),setOf("b".repeat(64)))))
            assertThrows(PilotBlocked::class.java) { requirePeer(facts,pin) }
    }
    @Test fun actualArbiterStubRejectsUnenrolledCallerBeforeMalformedPayload() {
        val service=Robolectric.buildService(fr.jarvis.fixture.core.P2ArbiterService::class.java).create()
        val binder=service.get().onBind(android.content.Intent())
        val data=Parcel.obtain()
        try {
            data.writeByteArray(byteArrayOf(0xff.toByte()))
            assertThrows(SecurityException::class.java) { (binder as fr.jarvis.fixture.p2.IP2Arbiter.Stub).onTransact(IBinder.FIRST_CALL_TRANSACTION,data,null,IBinder.FLAG_ONEWAY) }
        } finally { data.recycle(); service.destroy() }
    }
    @Test fun unpublishedEndpointNeverBecomesReady() {
        val context=RuntimeEnvironment.getApplication()
        assertEquals(EndpointReadiness.PUBLICATION_REQUIRED,EndpointInspector(context).inspect(null))
        val activity=Robolectric.buildActivity(FixtureActivity::class.java).setup()
        activity.pause().stop().destroy()
    }
}
