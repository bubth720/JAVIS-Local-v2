package com.stark.jarvislocal

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class SpeechOutputPortTest {
    private fun request(text: String)=SpeechOutputRequest(SpeechOutputKey(UUID.randomUUID().toString(),UUID.randomUUID().toString(),UUID.randomUUID().toString()),text)
    @Test fun historicalPortForwardsExactTextIncludingLongResponses() {
        val sent=mutableListOf<String>(); val port=HistoricalSpeechOutput { sent+=it }
        for(text in listOf("Bonjour.","réponse PDF ".repeat(200),"")) {
            var received: SpeechOutputEvent?=null;val r=request(text)
            assertEquals(SpeechOutputStatus.LOCAL_TTS_REQUESTED,port.request(r){received=it})
            assertEquals(text,sent.last());assertEquals(r.key,received!!.key)
        }
    }
    @Test fun historicalPortDoesNotInventPlaybackCompletionOrCancellation() {
        val r=request("Bonjour.");val events=mutableListOf<SpeechOutputEvent>();val port=HistoricalSpeechOutput{}
        port.request(r){events+=it};assertEquals(listOf(SpeechOutputEvent(r.key,SpeechOutputStatus.LOCAL_TTS_REQUESTED)),events)
        assertEquals(SpeechOutputStatus.CANCEL_UNSUPPORTED,port.cancel(r.key))
    }
    @Test fun pilotDisabledAndUnavailableOnApi24And25() {
        for(api in listOf(24,25,26,35)) {
            val expected=if(api<26) SpeechOutputStatus.API_UNSUPPORTED else SpeechOutputStatus.PILOT_DISABLED
            assertEquals(expected,DisabledVoicePilot(api).request(request("Bonjour.")){})
        }
    }
    @Test fun pilotRejectsLongMalformedAndReservedTextWithoutTruncation() {
        for(text in listOf("x".repeat(301),"", " ","\uD800","x\u0000", "<s>Bonjour")) {
            assertFalse(validPilotSpeechText(text))
            assertEquals(SpeechOutputStatus.INVALID_INPUT,DisabledVoicePilot(35).request(request(text)){})
        }
        assertTrue(validPilotSpeechText("😀".repeat(150)));assertFalse(validPilotSpeechText("😀".repeat(151)))
    }
    @Test fun oldCorrelationAndOldSessionCallbacksAreIgnored() {
        val pending=mutableListOf<Pair<SpeechOutputRequest,(SpeechOutputEvent)->Unit>>()
        val port=object: SpeechOutputPort {
            override fun request(request: SpeechOutputRequest,event: (SpeechOutputEvent)->Unit): SpeechOutputStatus {
                pending+=request to event; return SpeechOutputStatus.LOCAL_TTS_REQUESTED
            }
            override fun cancel(key: SpeechOutputKey)=SpeechOutputStatus.CANCEL_UNSUPPORTED
        }
        val session=SpeechOutputSession(port);val events=mutableListOf<SpeechOutputEvent>()
        session.submit("Un."){events+=it};session.submit("Deux."){events+=it}
        pending[0].second(SpeechOutputEvent(pending[0].first.key,SpeechOutputStatus.LOCAL_TTS_REQUESTED));assertTrue(events.isEmpty())
        pending[1].second(SpeechOutputEvent(pending[1].first.key.copy(session=UUID.randomUUID().toString()),SpeechOutputStatus.LOCAL_TTS_REQUESTED));assertTrue(events.isEmpty())
        pending[1].second(SpeechOutputEvent(pending[1].first.key,SpeechOutputStatus.LOCAL_TTS_REQUESTED));assertEquals(1,events.size)
        session.invalidate();pending[1].second(events.single());assertEquals(1,events.size)
    }
    @Test fun invalidatedCancellationDoesNotMeanNativeFinished() {
        var callback: ((SpeechOutputEvent)->Unit)?=null;var key: SpeechOutputKey?=null
        val port=object: SpeechOutputPort {
            override fun request(request: SpeechOutputRequest,event: (SpeechOutputEvent)->Unit): SpeechOutputStatus { key=request.key;callback=event;return SpeechOutputStatus.LOCAL_TTS_REQUESTED }
            override fun cancel(key: SpeechOutputKey)=SpeechOutputStatus.CANCEL_UNSUPPORTED
        }
        val session=SpeechOutputSession(port);var count=0;session.submit("Bonjour."){count++}
        assertEquals(SpeechOutputStatus.CANCEL_UNSUPPORTED,session.cancel());callback!!(SpeechOutputEvent(key!!,SpeechOutputStatus.LOCAL_TTS_REQUESTED));assertEquals(0,count)
    }
    @Test fun diagnosticsNeverContainSpeechContent() { assertFalse(request("synthetic secret marker").toString().contains("synthetic secret marker")) }
}
