package com.stark.jarvislocal

import java.util.UUID

/** Submission is not proof of playback, audible speech or native cleanup. */
internal enum class SpeechOutputStatus { LOCAL_TTS_REQUESTED, PILOT_DISABLED, API_UNSUPPORTED, INVALID_INPUT, CANCEL_UNSUPPORTED }
internal data class SpeechOutputKey(val session: String, val correlation: String, val cancellation: String) {
    init { require(listOf(session,correlation,cancellation).all { runCatching { UUID.fromString(it).toString() == it }.getOrDefault(false) }) }
}
internal class SpeechOutputRequest(val key: SpeechOutputKey,val text: String) {
    override fun toString()="SpeechOutputRequest[redacted]"
}
internal data class SpeechOutputEvent(val key: SpeechOutputKey,val status: SpeechOutputStatus)
internal interface SpeechOutputPort {
    fun request(request: SpeechOutputRequest, event: (SpeechOutputEvent) -> Unit): SpeechOutputStatus
    fun cancel(key: SpeechOutputKey): SpeechOutputStatus
}
/** Keeps the exact historical speak call and its existing length/language policy. */
internal class HistoricalSpeechOutput(private val speak: (String) -> Unit): SpeechOutputPort {
    override fun request(request: SpeechOutputRequest,event: (SpeechOutputEvent) -> Unit): SpeechOutputStatus {
        speak(request.text)
        event(SpeechOutputEvent(request.key,SpeechOutputStatus.LOCAL_TTS_REQUESTED))
        return SpeechOutputStatus.LOCAL_TTS_REQUESTED
    }
    override fun cancel(key: SpeechOutputKey)=SpeechOutputStatus.CANCEL_UNSUPPORTED
}
/** No enable switch: production admission is unavailable until C2 is qualified. */
internal class DisabledVoicePilot(private val api: Int): SpeechOutputPort {
    override fun request(request: SpeechOutputRequest,event: (SpeechOutputEvent) -> Unit): SpeechOutputStatus {
        val outcome=when {
            api < 26 -> SpeechOutputStatus.API_UNSUPPORTED
            !validPilotSpeechText(request.text) -> SpeechOutputStatus.INVALID_INPUT
            else -> SpeechOutputStatus.PILOT_DISABLED
        }
        event(SpeechOutputEvent(request.key,outcome)); return outcome
    }
    override fun cancel(key: SpeechOutputKey)=SpeechOutputStatus.PILOT_DISABLED
}
internal fun validPilotSpeechText(text: String): Boolean {
    if(text.length !in 1..300 || text.isBlank() || text.toByteArray(Charsets.UTF_8).size > 1200 ||
        text.any { it.isISOControl() && it != '\n' } || Regex("<(?:unk|s|/s|pad)>").containsMatchIn(text)) return false
    var i=0
    while(i<text.length) { val c=text[i++]; if(c.isHighSurrogate()) { if(i>=text.length || !text[i++].isLowSurrogate()) return false } else if(c.isLowSurrogate()) return false }
    return true
}
/** Session/correlation filtering for future callbacks, without altering chat or persistence. */
class SpeechOutputSession internal constructor(private val port: SpeechOutputPort,
    private val session: String = UUID.randomUUID().toString()) {
    private var active: SpeechOutputKey?=null
    fun speak(text: String) { submit(text) }
    @Synchronized internal fun submit(text: String,event: (SpeechOutputEvent) -> Unit = {}): SpeechOutputStatus {
        val key=SpeechOutputKey(session,UUID.randomUUID().toString(),UUID.randomUUID().toString())
        active=key
        return port.request(SpeechOutputRequest(key,text)) { received ->
            synchronized(this) { if(active == received.key) event(received) }
        }
    }
    @Synchronized internal fun invalidate() { active=null }
    @Synchronized internal fun cancel(): SpeechOutputStatus? {
        val key=active ?: return null
        active=null // Invalidate late results; this does NOT attest native or audio termination.
        return port.cancel(key)
    }
}
