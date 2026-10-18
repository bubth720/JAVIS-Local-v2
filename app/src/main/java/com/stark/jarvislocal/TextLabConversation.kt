package com.stark.jarvislocal

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Volatile UI/history only. No engine, database, preference writer, tool, voice or identity composer. */
data class TextLabState(val active: Boolean = false, val messages: List<ChatMessage> = emptyList())

class TextLabConversation internal constructor(
    private val infer: suspend (String, List<ChatMessage>) -> ChatMessage
) {
    private val mutableState = MutableStateFlow(TextLabState())
    val state = mutableState.asStateFlow()
    val active: Boolean get() = state.value.active
    private var revision = 0L
    private var pending = false
    private var revoked = false
    private var history = emptyList<ChatMessage>()

    /** Called by the existing session gate only when all its work has drained. */
    @Synchronized internal fun setActive(enabled: Boolean) {
        check(!pending) { "Attendre la fin du traitement avant de changer de mode" }
        revision++
        revoked = false
        history = emptyList()
        mutableState.value = TextLabState(enabled)
    }

    /** Invalidate output immediately; this does NOT stop JNI or release any native reservation. */
    @Synchronized internal fun invalidatePending() { revision++; revoked = true }

    /** Arm at session acceptance, BEFORE dispatch, so a queued cancelled turn stays rejected. */
    @Synchronized internal fun armTurn() {
        check(active && !pending)
        revision++
        revoked = false
    }

    internal suspend fun chat(text: String): ChatMessage {
        val token: Long
        val previous: List<ChatMessage>
        val user = ChatMessage(role = "user", text = text.trim())
        synchronized(this) {
            check(active && !pending) { "Laboratoire inactif ou occupé" }
            if (revoked) throw CancellationException("Tour du laboratoire annulé avant son démarrage")
            require(user.text.isNotBlank())
            require(user.text.length <= 2048 && user.text.toByteArray(Charsets.UTF_8).size <= 2048) {
                "Saisie trop longue pour le laboratoire textuel"
            }
            token = revision
            previous = history.toList()
            pending = true
            mutableState.value = state.value.copy(messages = (state.value.messages + user).takeLast(40))
        }
        try {
            val answer = infer(user.text, previous)
            synchronized(this) {
                if (!active || revision != token) throw CancellationException("Réponse du laboratoire invalidée")
                check(answer.role == "assistant" && answer.text.isNotBlank()) { "Réponse locale vide ou invalide" }
                history = (history + user + answer).takeLast(24)
                mutableState.value = state.value.copy(messages = (state.value.messages + answer).takeLast(40))
            }
            return answer
        } finally { synchronized(this) { pending = false } }
    }
}
