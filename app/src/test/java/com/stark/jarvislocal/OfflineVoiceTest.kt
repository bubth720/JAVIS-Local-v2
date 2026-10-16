package com.stark.jarvislocal

import android.app.Application
import android.speech.tts.Voice
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24], application = Application::class, manifest = Config.NONE)
class OfflineVoiceTest {
    @Test fun selectsFrenchOfflineVoiceEvenWhenNetworkVoiceComesFirst() {
        val local = voice("local", Locale.FRENCH, false)
        assertEquals(local, offlineFrenchVoice(linkedSetOf(voice("network", Locale.FRENCH, true), local)))
    }
    @Test fun networkOnlyFrenchVoiceIsUnavailable() {
        assertNull(offlineFrenchVoice(setOf(voice("network", Locale.FRENCH, true))))
    }
    @Test fun nonFrenchOfflineVoiceDoesNotSilentlyReplaceFrench() {
        assertNull(offlineFrenchVoice(setOf(voice("english", Locale.ENGLISH, false))))
        assertNull(offlineFrenchVoice(emptySet()))
    }
    private fun voice(name: String, locale: Locale, network: Boolean) =
        Voice(name, locale, Voice.QUALITY_NORMAL, Voice.LATENCY_NORMAL, network, emptySet())
}
