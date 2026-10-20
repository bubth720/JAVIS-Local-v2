package com.stark.jarvislocal

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TtsStatus { STOPPED, INITIALIZING, READY, UNAVAILABLE }

internal interface SpeechEngine {
    fun selectLocalFrenchVoice(): Boolean
    fun speak(text: String): Boolean
    fun stop()
    fun shutdown()
}

class LocalTts internal constructor(private val factory: ((Boolean) -> Unit) -> SpeechEngine) {
    constructor(context: Context) : this(speechEngineFactory(context.applicationContext))

    private val mutableStatus = MutableStateFlow(TtsStatus.STOPPED)
    val status: StateFlow<TtsStatus> = mutableStatus.asStateFlow()
    private var engine: SpeechEngine? = null
    private var generation = 0L
    private var earlyResult: Boolean? = null

    @Synchronized
    fun initialize() {
        if (engine != null && mutableStatus.value != TtsStatus.UNAVAILABLE) return
        if (engine != null) shutdown()
        val token = ++generation
        mutableStatus.value = TtsStatus.INITIALIZING
        earlyResult = null
        try {
            engine = factory { initialized -> onInitialized(token, initialized) }
            earlyResult?.let { onInitialized(token, it) }
        } catch (e: Exception) {
            engine = null
            mutableStatus.value = TtsStatus.UNAVAILABLE
        }
    }

    @Synchronized
    private fun onInitialized(token: Long, success: Boolean) {
        if (token != generation) return
        val current = engine
        if (current == null) {
            earlyResult = success
            return
        }
        earlyResult = null
        val usable = try { success && current.selectLocalFrenchVoice() }
        catch (e: Exception) { false }
        mutableStatus.value = if (usable) TtsStatus.READY else TtsStatus.UNAVAILABLE
    }

    @Synchronized
    fun speak(text: String): Boolean {
        if (engine == null) initialize()
        if (mutableStatus.value != TtsStatus.READY) return false
        return try {
            requireNotNull(engine).speak(text).also { if (!it) mutableStatus.value = TtsStatus.UNAVAILABLE }
        } catch (e: Exception) {
            mutableStatus.value = TtsStatus.UNAVAILABLE
            false
        }
    }

    @Synchronized
    fun stop() {
        try { engine?.stop() } catch (e: Exception) { mutableStatus.value = TtsStatus.UNAVAILABLE }
    }

    @Synchronized
    fun shutdown() {
        generation++
        val previous = engine
        engine = null
        earlyResult = null
        mutableStatus.value = TtsStatus.STOPPED
        try { previous?.shutdown() } catch (e: Exception) { mutableStatus.value = TtsStatus.UNAVAILABLE }
    }
}

internal fun offlineFrenchVoice(voices: Set<Voice>): Voice? =
    voices.firstOrNull { it.locale.language == "fr" && !it.isNetworkConnectionRequired &&
        TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty() }

private class AndroidSpeechEngine(context: Context, onInit: (Boolean) -> Unit) : SpeechEngine {
    private val tts = TextToSpeech(context) { status -> onInit(status == TextToSpeech.SUCCESS) }

    override fun selectLocalFrenchVoice(): Boolean {
        val voice = offlineFrenchVoice(tts.voices.orEmpty()) ?: return false
        return tts.setVoice(voice) == TextToSpeech.SUCCESS
    }

    override fun speak(text: String): Boolean =
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply") == TextToSpeech.SUCCESS

    override fun stop() { tts.stop() }
    override fun shutdown() { tts.shutdown() }
}

private fun speechEngineFactory(applicationContext: Context): ((Boolean) -> Unit) -> SpeechEngine =
    { callback -> AndroidSpeechEngine(applicationContext, callback) }
