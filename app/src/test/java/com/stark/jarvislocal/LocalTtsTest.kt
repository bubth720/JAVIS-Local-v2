package com.stark.jarvislocal

import org.junit.Assert.*
import org.junit.Test

class LocalTtsTest {
    @Test fun asynchronousInitializationDoesNotSpeakUntilReady() {
        val fake = FakeSpeech()
        val tts = LocalTts { callback -> fake.callback = callback; fake }
        tts.initialize(); tts.initialize()
        assertEquals(TtsStatus.INITIALIZING, tts.status.value)
        assertFalse(tts.speak("avant"))
        assertTrue(fake.spoken.isEmpty())
        fake.callback(true)
        assertTrue(tts.speak("après"))
        assertEquals(listOf("après"), fake.spoken)
    }

    @Test fun synchronousInitializationCallbackIsHandled() {
        val fake = FakeSpeech()
        val tts = LocalTts { callback -> callback(true); fake }
        tts.initialize()
        assertEquals(TtsStatus.READY, tts.status.value)
        assertTrue(tts.speak("OK"))
    }

    @Test fun initializationFailureCanBeRetriedWithOneNewEngine() {
        val engines = mutableListOf<FakeSpeech>()
        val tts = LocalTts { callback -> FakeSpeech().also { it.callback = callback; engines += it } }
        tts.initialize(); engines[0].callback(false)
        assertEquals(TtsStatus.UNAVAILABLE, tts.status.value)
        assertFalse(tts.speak("impossible"))
        tts.initialize(); engines[1].callback(true)
        assertEquals(1, engines[0].shutdowns)
        assertTrue(tts.speak("reprise"))
    }

    @Test fun missingOfflineFrenchVoiceDoesNotFallBackToNetwork() {
        val fake = FakeSpeech().apply { localVoice = false }
        val tts = LocalTts { callback -> fake.callback = callback; fake }
        tts.initialize(); fake.callback(true)
        assertEquals(TtsStatus.UNAVAILABLE, tts.status.value)
        assertFalse(tts.speak("local uniquement"))
        assertTrue(fake.spoken.isEmpty())
    }

    @Test fun failedSpeakIsReportedWithoutThrowingIntoConversation() {
        val fake = FakeSpeech().apply { failSpeak = true }
        val tts = LocalTts { callback -> fake.callback = callback; fake }
        tts.initialize(); fake.callback(true)
        assertFalse(tts.speak("texte conservé"))
        assertEquals(TtsStatus.UNAVAILABLE, tts.status.value)
    }

    @Test fun lateCallbackAfterShutdownCannotReviveReleasedEngine() {
        val fake = FakeSpeech()
        val tts = LocalTts { callback -> fake.callback = callback; fake }
        tts.initialize(); tts.shutdown(); fake.callback(true)
        assertEquals(TtsStatus.STOPPED, tts.status.value)
        assertEquals(1, fake.shutdowns)
        assertTrue(fake.spoken.isEmpty())
    }

    @Test fun releasedEngineIsNotUsedAndCanBeRecreatedForNextScreen() {
        val engines = mutableListOf<FakeSpeech>()
        val tts = LocalTts { callback -> FakeSpeech().also { it.callback = callback; engines += it } }
        tts.initialize(); engines[0].callback(true)
        tts.shutdown(); tts.shutdown()
        assertFalse(tts.speak("initialisation"))
        engines[1].callback(true)
        assertTrue(tts.speak("nouvel écran"))
        assertEquals(1, engines[0].shutdowns)
        assertTrue(engines[0].spoken.isEmpty())
    }

    @Test fun stopPreservesReadyEngineForRotation() {
        val fake = FakeSpeech()
        var creations = 0
        val tts = LocalTts { callback -> creations++; fake.callback = callback; fake }
        tts.initialize(); fake.callback(true)
        tts.stop(); tts.initialize()
        assertTrue(tts.speak("après rotation"))
        assertEquals(1, creations)
        assertEquals(0, fake.shutdowns)
    }

    @Test fun factoryFailureLeavesClearUnavailableState() {
        val tts = LocalTts { throw IllegalStateException("moteur absent") }
        tts.initialize()
        assertEquals(TtsStatus.UNAVAILABLE, tts.status.value)
        assertFalse(tts.speak("texte"))
    }

    private class FakeSpeech : SpeechEngine {
        lateinit var callback: (Boolean) -> Unit
        var localVoice = true
        var failSpeak = false
        var shutdowns = 0
        val spoken = mutableListOf<String>()
        override fun selectLocalFrenchVoice() = localVoice
        override fun speak(text: String): Boolean {
            check(shutdowns == 0) { "released engine used" }
            if (failSpeak) error("synthèse échouée")
            spoken += text
            return true
        }
        override fun stop() = Unit
        override fun shutdown() { shutdowns++ }
    }
}
