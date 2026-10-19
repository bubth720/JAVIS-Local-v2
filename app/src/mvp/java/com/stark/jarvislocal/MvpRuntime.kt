package com.stark.jarvislocal

import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class MvpState(val busy: Boolean = false, val detail: String = "Importer un modèle local ; aucun chargement automatique.",
    val model: MvpModelManifest? = null, val loaded: Boolean = false, val messages: List<ChatMessage> = emptyList())

/** Application-owned MVP gate. Cancellation never opens it before the controller has drained JNI. */
internal class MvpRuntime(private val store: MvpModelStore, private val engine: InferenceController,
    private val scope: CoroutineScope, private val safety: (Long) -> Unit,
    private val worker: CoroutineDispatcher = Dispatchers.IO, private val timeoutMillis: Long = 90_000) {
    private val mutableState = MutableStateFlow(MvpState())
    val state = mutableState.asStateFlow()
    private var job: Job? = null
    private var revision = 0L
    private var cancellationDetail: String? = null
    private var history = emptyList<ChatMessage>()
    private val session = UUID.randomUUID().toString()
    init { engine.openSession(session) }

    fun restore() = start("Lecture du manifeste privé…") { check ->
        check(); val selected = store.selected(); update { copy(model = selected) }
    }
    fun importModel(manifest: MvpModelManifest, open: () -> InputStream) = start("Copie et vérification SHA-256…") { check ->
        require(engine.state.value.modelUnloaded && !engine.state.value.quarantined) { "Décharger explicitement le moteur avant tout import" }
        store.install(manifest, open, check)
        // Safe only when the controller reports a confirmed unloaded state, never a reset of quarantine.
        engine.select(InferenceMode.HISTORICAL)
        history = emptyList()
        update { copy(model = manifest, messages = emptyList()) }
    }
    fun load(settings: JarvisSettings) = start("Vérification puis chargement natif…", monitor = true) { check ->
        val manifest = requireNotNull(store.selected()) { "Modèle absent" }
        safety(manifest.bytes); store.verifySelected(check); check()
        withTimeout(timeoutMillis) { engine.warmUp(settings) }
        check(engine.isLoaded()) { "Chargement non attesté" }
    }
    fun unload() = start("Déchargement : attente du retour natif…") {
        require(engine.unload() == UnloadResult.CONFIRMED) { "Libération non confirmée ; aucune nouvelle admission" }
    }
    @Synchronized fun cancel(reason: String = "Annulation demandée ; attente de la fin effective du traitement.") {
        if (!state.value.busy) return
        revision++
        cancellationDetail = reason
        update { copy(detail = reason) }
        engine.requestStop()
        job?.cancel()
    }
    fun send(text: String, settings: JarvisSettings): Boolean {
        val question = text.trim()
        if (question.isEmpty()) return false
        return start("Génération native locale…", monitor = true) { check ->
            require(engine.isLoaded()) { "Charger explicitement le modèle avant l'envoi" }
            safety(requireNotNull(store.selected()).bytes)
            require(question.toByteArray(Charsets.UTF_8).size <= 2048) { "Question trop longue" }
            val prompt = PromptBuilder.prepare(question, history, emptyList(), emptyList(), settings)
            check()
            val user = ChatMessage(role = "user", text = question)
            update { copy(messages = (messages + user).takeLast(40)) }
            val response = withTimeout(timeoutMillis) {
                engine.infer(InferenceRequest(session, UUID.randomUUID().toString(), settings.systemPrompt,
                    question, prompt.context, limits = GenerationLimits(settings.contextSize, settings.maxTokens)))
            }
            require(response.text.isNotBlank()) { "Réponse native vide" }
            val answer = ChatMessage(role = "assistant", text = response.text)
            synchronized(this@MvpRuntime) {
                check()
                history = (history + user + answer).takeLast(24)
                update { copy(messages = (messages + answer).takeLast(40)) }
            }
        }
    }
    @Synchronized private fun update(change: MvpState.() -> MvpState) { mutableState.value = state.value.change() }
    @Synchronized private fun start(detail: String, monitor: Boolean = false,
        block: suspend (() -> Unit) -> Unit): Boolean {
        if (state.value.busy) return false
        val token = ++revision
        cancellationDetail = null
        update { copy(busy = true, detail = detail) }
        val operation = scope.launch(start = CoroutineStart.LAZY) {
            val caller = currentCoroutineContext()
            var watcher: Job? = null
            try {
                if (monitor) watcher = launch {
                    while (isActive) {
                        delay(1000)
                        try { withContext(worker) { store.selected()?.let { safety(it.bytes) } } }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (failure: Exception) { this@MvpRuntime.cancel("Protection ressources : ${failure.message}. Attente de fin native."); break }
                    }
                }
                withContext(worker) { block {
                    caller.ensureActive()
                    synchronized(this@MvpRuntime) { if (token != revision) throw CancellationException("Tour invalidé") }
                } }
                update { copy(detail = "Opération terminée ; modèle ${if (engine.isLoaded()) "résident" else "déchargé"}.") }
            } catch (timeout: TimeoutCancellationException) {
                update { copy(detail = "Délai dépassé ; retour moteur observé, réponse écartée. Le modèle peut rester résident.") }
            } catch (cancelled: CancellationException) {
                update { copy(detail = (cancellationDetail ?: "Demande annulée.") + " Travail drainé ; modèle non nécessairement déchargé.") }
            } catch (failure: Exception) {
                update { copy(detail = "Refus/erreur : ${failure.message}") }
            } finally {
                withContext(NonCancellable) { watcher?.cancelAndJoin() }
                update { copy(busy = false, loaded = engine.isLoaded(), detail = if (engine.state.value.quarantined)
                    "Moteur en quarantaine : libération inconnue, redémarrage du processus nécessaire." else this.detail) }
            }
        }
        job = operation
        operation.invokeOnCompletion {
            synchronized(this) {
                if (job === operation) {
                    job = null
                    if (state.value.busy) update { copy(busy = false, loaded = engine.isLoaded(), detail = "Annulé avant démarrage.") }
                }
            }
        }
        operation.start()
        return true
    }
}
