package com.stark.jarvislocal

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

class LocalTts(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    @Volatile private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) tts.language = Locale.FRENCH
    }

    private fun localVoices(): List<Voice> =
        if (!ready) emptyList()
        else tts.voices.orEmpty().filter { !it.isNetworkConnectionRequired }

    fun speak(text: String) {
        if (!ready) return
        localVoices().firstOrNull { it.locale.language == "fr" }?.let { tts.voice = it }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
    }

    fun shutdown() = tts.shutdown()
}
