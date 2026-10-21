package com.stark.jarvislocal

import org.junit.Assert.*
import org.junit.Test

class Mvp2SpeechTest {
    private class Engine : SpeechEngine {
        var local = true; var accepts = true; val spoken = mutableListOf<String>(); var shutdowns=0
        override fun selectLocalFrenchVoice() = local
        override fun speak(text:String):Boolean { if(accepts)spoken+=text;return accepts }
        override fun stop() { }
        override fun shutdown() { shutdowns++ }
    }
    private fun speech(e:Engine)=Mvp2Speech(LocalTts { done -> done(true); e }) { true }
    @Test fun metadataReadyDoesNotAuthorizeReplyWithoutHumanCheck() {
        val e=Engine();val s=speech(e);s.initialize()
        assertEquals(TtsStatus.READY,s.status.value);assertFalse(s.readReply("reply"));assertTrue(e.spoken.isEmpty())
        try {s.confirmOfflineHeard();fail()} catch (_:IllegalStateException) { }
        assertFalse(s.verifiedByUser)
    }
    @Test fun explicitProbeAndConfirmationAuthorizeOnlyBoundedUnmodifiedReply() {
        val e=Engine();val s=speech(e);s.initialize();assertTrue(s.testPhrase());s.confirmOfflineHeard()
        assertTrue(s.readReply("Texte exact 😀"));assertEquals("Texte exact 😀",e.spoken.last())
        assertFalse(s.readReply("a".repeat(301)));assertFalse(s.readReply(" "))
    }
    @Test fun missingLocalVoiceCannotBeConfirmedOrUsed() {
        val e=Engine().apply{local=false};val s=speech(e);s.initialize()
        assertFalse(s.testPhrase());assertFalse(s.readReply("reply"));assertEquals(TtsStatus.UNAVAILABLE,s.status.value)
        try{s.confirmOfflineHeard();fail()}catch(_:IllegalStateException){}
    }
    @Test fun shutdownRevokesProofAndRecreationDoesNotReplay() {
        val e=Engine();val s=speech(e);s.initialize();s.testPhrase();s.confirmOfflineHeard();s.disable()
        assertFalse(s.verifiedByUser);assertEquals(1,e.shutdowns);s.initialize();assertFalse(s.readReply("old history"))
    }
    @Test fun playbackFailureRevokesProofRatherThanFallback() {
        val e=Engine();val s=speech(e);s.initialize();s.testPhrase();s.confirmOfflineHeard();e.accepts=false
        assertFalse(s.readReply("reply"));assertFalse(s.verifiedByUser);assertEquals(TtsStatus.UNAVAILABLE,s.status.value)
    }
    @Test fun futureCapabilitiesAreDisabledAndCannotReturnFakeData() {
        assertEquals(FutureCapability.entries.toSet(),disabledExtensions.map{it.capability}.toSet())
        assertTrue(disabledExtensions.none{it.available})
    }
    @Test fun airplaneOffPreventsEngineInitializationAndRevokesPreviousProof() {
        val e=Engine();var offline=false;var created=0
        val s=Mvp2Speech(LocalTts{done->created++;done(true);e}){offline}
        assertFalse(s.initialize());assertEquals(0,created);assertFalse(s.testPhrase())
        offline=true;assertTrue(s.initialize());s.testPhrase();s.confirmOfflineHeard();assertTrue(s.verifiedByUser)
        offline=false;assertFalse(s.readReply("private reply"));assertFalse(s.verifiedByUser)
        offline=true;assertFalse(s.readReply("must recheck"))
        val unavailable=Mvp2Speech(LocalTts{error("engine must not be constructed")}){throw SecurityException("synthetic")}
        assertFalse(unavailable.initialize());assertFalse(unavailable.readReply("refuse"))
    }

    @Test fun observedAirplaneOffRevokesProbeAndProofPermanently() {
        val e=Engine();var airplane=true
        val s=Mvp2Speech(LocalTts{done->done(true);e}){airplane}
        s.initialize();s.testPhrase();s.confirmOfflineHeard();assertTrue(s.verifiedByUser)
        airplane=false;assertFalse(s.verifiedByUser)
        airplane=true;assertFalse(s.verifiedByUser);assertFalse(s.readReply("must not revive old proof"))
        try{s.confirmOfflineHeard();fail("old probe must also be revoked")}catch(_:IllegalStateException){}
        assertTrue(s.testPhrase());s.confirmOfflineHeard();assertTrue(s.readReply("new confirmation"))
    }

}
