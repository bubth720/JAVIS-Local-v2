package com.stark.jarvislocal

/** READY is voice metadata, not acoustic/offline proof. Human check is session-scoped and explicit. */
internal class Mvp2Speech(private val local: LocalTts, private val airplaneMode: () -> Boolean = { false }) {
    private fun offline(): Boolean {
        val allowed = try { airplaneMode() } catch (_: Exception) { false }
        // An observed loss of the prerequisite permanently invalidates this probe/session.
        // A later airplane-mode flag alone must never resurrect the previous human confirmation.
        if (!allowed) { verified = false; probeAccepted = false }
        return allowed
    }
    private var probeAccepted = false
    private var verified = false
    val status get() = local.status
    val verifiedByUser get() = offline() && verified && local.status.value == TtsStatus.READY
    fun initialize(): Boolean {
        verified = false; probeAccepted = false
        if (!offline()) { local.shutdown(); return false }
        local.initialize(); return true
    }
    fun testPhrase(): Boolean {
        verified = false
        if (!offline()) { probeAccepted = false; local.shutdown(); return false }
        probeAccepted = local.status.value == TtsStatus.READY && local.speak("Test de la voix française hors ligne.")
        return probeAccepted
    }
    fun confirmOfflineHeard() {
        check(offline() && probeAccepted && local.status.value == TtsStatus.READY) { "Tester une voix locale prête avant confirmation" }
        verified = true
    }
    fun readReply(text: String): Boolean {
        if (!offline()) { disable(); return false }
        if (!verifiedByUser || local.status.value != TtsStatus.READY || text.isBlank() || text.length > 300) return false
        if (!local.speak(text)) { verified = false; return false }
        return true // queued by Android only, not an audibility or playback-completion receipt
    }
    fun stop() { local.stop() }
    fun disable() { verified = false; probeAccepted = false; local.shutdown() }
}
