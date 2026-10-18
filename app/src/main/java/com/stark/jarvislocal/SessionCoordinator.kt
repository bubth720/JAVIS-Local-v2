package com.stark.jarvislocal

import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

enum class SessionPhase { PREPARING, READY, THINKING, STARTING_AUDIO, LISTENING, STOPPING_AUDIO, TRANSCRIBING, IMPORTING, ERROR, TIMEOUT }

data class SessionState(
    val phase: SessionPhase = SessionPhase.PREPARING,
    val busy: Boolean = false,
    val prepared: Boolean = false,
    val detail: String = "Initialisation locale…",
    val revision: Long = 0
)

internal interface SessionActions {
    fun beforeTextChat() { }
    fun requestInferenceStop() { }
    fun inferenceReleaseUnconfirmed(): Boolean = false
    suspend fun prepare(progress: (String) -> Unit)
    suspend fun chat(text: String): ChatMessage
    suspend fun importPdf(uri: String)
    suspend fun startRecording()
    suspend fun awaitRecording(): File
    suspend fun stopRecording(discard: Boolean): File?
    suspend fun transcribe(file: File): String
    fun deleteRecording(file: File)
    suspend fun reportError(detail: String)
    fun speakIfEnabled(text: String)
    fun stopSpeech()
    fun releaseSpeech()
    fun initializeSpeechIfEnabled()
}

/** Application-owned operations. No Activity, View or UI callback is retained. */
class SessionCoordinator internal constructor(
    private val actions: SessionActions,
    private val scope: CoroutineScope,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val chatTimeoutMillis: Long = 90_000,
    private val transcriptionTimeoutMillis: Long = 60_000,
    private val audioTimeoutMillis: Long = 5_000
) {
    private val mutableState = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = mutableState.asStateFlow()
    private var currentJob: Job? = null
    private var stopSignal: CompletableDeferred<Unit>? = null
    @Volatile private var discardAudio = false
    @Volatile private var foreground = false

    /** UI mode readiness only. Does not reset the engine, quarantine, native gate or lease. */
    @Synchronized internal fun reprepareWhenIdle(change: () -> Boolean): Boolean {
        if (mutableState.value.busy || !change()) return false
        mutableState.value = mutableState.value.copy(phase = SessionPhase.PREPARING, prepared = false,
            detail = "Préparation du mode sélectionné…", revision = mutableState.value.revision + 1)
        return true
    }

    fun prepare(): Boolean = begin(SessionPhase.PREPARING, "Préparation du noyau local…", reportFailure = false) {
        if (mutableState.value.prepared) return@begin
        withContext(workerDispatcher) {
            actions.prepare { update(SessionPhase.PREPARING, it) }
        }
        synchronized(this) { mutableState.value = mutableState.value.copy(prepared = true) }
        if (foreground) actions.initializeSpeechIfEnabled()
    }

    fun send(text: String): Boolean {
        val question = text.trim()
        if (question.isEmpty()) return false
        return begin(SessionPhase.THINKING, "Réflexion locale…", onAccepted = actions::beforeTextChat) { conversation(question) }
    }

    fun importPdf(uri: String): Boolean = begin(SessionPhase.IMPORTING, "Import du PDF…") {
        withContext(workerDispatcher) { actions.importPdf(uri) }
    }

    @Synchronized
    fun startRecording(): Boolean {
        if (mutableState.value.busy) return false
        stopSignal = CompletableDeferred()
        discardAudio = false
        return begin(SessionPhase.STARTING_AUDIO, "Démarrage du microphone…") {
            var file: File? = null
            try {
                actions.stopSpeech()
                waitForWork(SessionPhase.STARTING_AUDIO, audioTimeoutMillis) { actions.startRecording() }
                update(SessionPhase.LISTENING, "Microphone actif. Appuie sur Stop.")
                supervisorScope {
                    val completion = async(workerDispatcher) { actions.awaitRecording() }
                    try {
                        select<Unit> {
                            requireNotNull(stopSignal).onAwait { }
                            completion.onAwait { file = it }
                        }
                    } finally {
                        // This watcher does not own the recorder or its worker thread.
                        completion.cancel()
                    }
                }
                update(SessionPhase.STOPPING_AUDIO, "Arrêt et finalisation du WAV…")
                file = waitForWork(SessionPhase.STOPPING_AUDIO, audioTimeoutMillis) {
                    actions.stopRecording(discardAudio)
                }
                if (!discardAudio && file != null) {
                    val recording = requireNotNull(file)
                    val text = waitForWork(SessionPhase.TRANSCRIBING, transcriptionTimeoutMillis) {
                        actions.transcribe(recording)
                    }.trim()
                    if (text.isNotEmpty()) conversation(text)
                    else update(SessionPhase.READY, "Aucune parole reconnue.")
                }
            } finally {
                withContext(NonCancellable + workerDispatcher) {
                    // Always drain the real recorder before releasing the session gate.
                    try { actions.stopRecording(discard = true) }
                    finally { file?.let(actions::deleteRecording) }
                }
                synchronized(this) { stopSignal = null }
            }
        }
    }

    @Synchronized
    fun stopRecording(discard: Boolean = false): Boolean {
        val signal = stopSignal ?: return false
        if (discard) discardAudio = true
        return signal.complete(Unit)
    }

    fun onForeground() {
        foreground = true
        if (mutableState.value.prepared) actions.initializeSpeechIfEnabled()
    }

    fun onBackground() {
        foreground = false
        actions.stopSpeech()
        if (mutableState.value.phase in setOf(SessionPhase.STARTING_AUDIO, SessionPhase.LISTENING, SessionPhase.STOPPING_AUDIO)) {
            stopRecording(discard = true)
        }
    }

    fun onScreenDestroyed(changingConfigurations: Boolean) {
        if (!changingConfigurations) {
            cancelCurrent()
            actions.releaseSpeech()
        }
    }

    @Synchronized
    fun cancelCurrent() {
        stopRecording(discard = true)
        actions.requestInferenceStop()
        currentJob?.cancel()
    }

    private suspend fun conversation(text: String) {
        val answer = waitForWork(SessionPhase.THINKING, chatTimeoutMillis) { actions.chat(text) }
        if (foreground) actions.speakIfEnabled(answer.text)
    }

    /** Timeout stops waiting, not the native operation. Keep the gate until it really returns. */
    private suspend fun <T> waitForWork(phase: SessionPhase, timeoutMillis: Long, block: suspend () -> T): T = supervisorScope {
        update(phase, when (phase) {
            SessionPhase.TRANSCRIBING -> "Transcription locale…"
            SessionPhase.THINKING -> "Réflexion locale…"
            SessionPhase.STOPPING_AUDIO -> "Arrêt et finalisation du WAV…"
            else -> "Démarrage du microphone…"
        })
        val work = async(workerDispatcher) { withContext(NonCancellable) { block() } }
        try {
            try { withTimeout(timeoutMillis) { work.await() } }
            catch (e: TimeoutCancellationException) {
                if (phase == SessionPhase.THINKING) actions.requestInferenceStop()
                update(SessionPhase.TIMEOUT, "Délai dépassé. Opération encore active ; nouvelle demande bloquée jusqu’à sa fin.")
                work.await()
            }
        } finally {
            if (!work.isCompleted) {
                if (phase == SessionPhase.THINKING) actions.requestInferenceStop()
                update(SessionPhase.TIMEOUT, "Arrêt demandé. Attente de la fin effective de l’opération…")
                withContext(NonCancellable) { work.join() }
            }
        }
    }

    @Synchronized
    private fun begin(phase: SessionPhase, detail: String, reportFailure: Boolean = true,
        onAccepted: (() -> Unit)? = null, block: suspend () -> Unit): Boolean {
        if (mutableState.value.busy) return false
        if (phase == SessionPhase.PREPARING && mutableState.value.prepared) return true
        onAccepted?.invoke()
        mutableState.value = mutableState.value.copy(phase = phase, busy = true, detail = detail, revision = mutableState.value.revision + 1)
        val job = scope.launch(start = CoroutineStart.LAZY) {
            var failure: String? = null
            var cancelled = false
            try { block() }
            catch (e: CancellationException) { cancelled = true }
            catch (e: Exception) {
                failure = e.message ?: "Erreur locale inconnue"
                if (reportFailure) {
                    try { withContext(workerDispatcher) { actions.reportError(requireNotNull(failure)) } }
                    catch (cancel: CancellationException) { throw cancel }
                    catch (writeError: Exception) { failure += " ; journal non enregistré : ${writeError.message}" }
                }
            } catch (fatal: Error) {
                failure = "Erreur fatale du noyau local"
                throw fatal
            } finally {
                synchronized(this@SessionCoordinator) {
                    if (actions.inferenceReleaseUnconfirmed()) failure = failure ?: "État natif ou libération du moteur non attesté ; moteur bloqué."
                    val previous = mutableState.value
                    mutableState.value = previous.copy(
                        phase = if (failure != null || !previous.prepared) SessionPhase.ERROR else SessionPhase.READY,
                        busy = false,
                        detail = failure ?: if (cancelled) "Demande annulée ; fin du traitement observée, modèle non nécessairement déchargé." else if (previous.phase == SessionPhase.READY && previous.prepared) previous.detail else if (previous.prepared) "Noyau local prêt." else "Modèles indisponibles ; préparation à réessayer.",
                        revision = previous.revision + 1
                    )
                    currentJob = null
                }
            }
        }
        currentJob = job
        job.invokeOnCompletion {
            synchronized(this@SessionCoordinator) {
                // A job cancelled before its first instruction never runs its finally block.
                if (currentJob === job) {
                    currentJob = null
                    stopSignal = null
                    val previous = mutableState.value
                    mutableState.value = previous.copy(
                        phase = if (previous.prepared) SessionPhase.READY else SessionPhase.ERROR,
                        busy = false,
                        detail = "Demande annulée avant son démarrage.",
                        revision = previous.revision + 1
                    )
                }
            }
        }
        job.start()
        return true
    }

    @Synchronized
    private fun update(phase: SessionPhase, detail: String) {
        mutableState.value = mutableState.value.copy(phase = phase, detail = detail)
    }
}
