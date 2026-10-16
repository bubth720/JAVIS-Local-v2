package com.stark.jarvislocal

import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

enum class InferenceMode { HISTORICAL, DELEGATED_SIMULATOR }
data class InferenceLifecycle(
    val mode: InferenceMode = InferenceMode.HISTORICAL,
    val busy: Boolean = false,
    val stopRequested: Boolean = false,
    val nativeDone: Boolean = true,
    val modelUnloaded: Boolean = true,
    val quarantined: Boolean = false,
    val error: String? = null
)

/** No discovery, IPC or production delegate. Suppliers construct without warming up. */
class InferenceEngineFactory internal constructor(
    private val historical: () -> LocalLanguageModel,
    private val delegatedSimulator: (() -> LocalLanguageModel)? = null,
    internal val admission: ProcessInferenceAdmission = ProcessInferenceAdmission.process
) {
    internal fun supports(mode: InferenceMode) = mode == InferenceMode.HISTORICAL || delegatedSimulator != null
    internal fun create(mode: InferenceMode): LocalLanguageModel = when (mode) {
        InferenceMode.HISTORICAL -> historical()
        InferenceMode.DELEGATED_SIMULATOR -> delegatedSimulator?.invoke()
            ?: throw InferenceRejected(InferenceRefusal.DELEGATE_DISABLED)
    }
}

/** Local operation gate plus shared process admission; never coordinates another APK. */
class InferenceController(private val factory: InferenceEngineFactory) : LocalLanguageModel {
    private val gate = Mutex()
    private val mutableState = MutableStateFlow(InferenceLifecycle())
    val state = mutableState.asStateFlow()
    private var engine: LocalLanguageModel? = null
    private var sessionId: String? = null
    private var epoch = 0L
    private var admissionToken: Long? = null

    @Synchronized fun select(mode: InferenceMode) {
        val current = mutableState.value
        if (current.busy) throw InferenceRejected(InferenceRefusal.BUSY)
        if (current.quarantined || !current.nativeDone || !current.modelUnloaded)
            throw InferenceRejected(InferenceRefusal.RELEASE_UNCONFIRMED)
        if (!factory.supports(mode)) throw InferenceRejected(InferenceRefusal.DELEGATE_DISABLED)
        engine = null
        epoch++
        mutableState.value = InferenceLifecycle(mode = mode)
    }

    @Synchronized override fun openSession(sessionId: String) {
        require(sessionId.isNotBlank())
        if (this.sessionId != sessionId) { this.sessionId = sessionId; epoch++ }
    }

    @Synchronized override fun requestStop(): StopResult {
        val current = mutableState.value
        if (!current.busy) return StopResult.NOT_RUNNING
        epoch++
        mutableState.value = current.copy(stopRequested = true)
        return try { engine?.requestStop() ?: StopResult.UNSUPPORTED }
        catch (e: Exception) {
            mutableState.value = mutableState.value.copy(error = "Demande d'arrêt non confirmée")
            StopResult.UNSUPPORTED
        }
    }

    override fun isLoaded(): Boolean = !state.value.modelUnloaded && !state.value.quarantined && engine?.isLoaded() == true

    override suspend fun warmUp(settings: JarvisSettings) {
        operate {
            validateInference(InferenceRequest("prepare", "prepare", settings.systemPrompt, "", "",
                limits = GenerationLimits(settings.contextSize, settings.maxTokens)), it.capabilities)
            val completion = drain { beginNative(mayAllocate = true); it.prepare(settings).also { result ->
                mark { copy(nativeDone = result.nativeDone, modelUnloaded = result.modelUnloaded,
                    quarantined = !result.nativeDone) }
                if (result.nativeDone && result.modelUnloaded) {
                    admissionToken?.let { token -> factory.admission.confirmRelease(it, token) }
                    admissionToken = null
                }
            } }
            if (!completion.nativeDone) throw InferenceRejected(InferenceRefusal.RELEASE_UNCONFIRMED)
        }
    }

    override suspend fun infer(request: InferenceRequest): InferenceResponse = execute(request) { it.infer(request) }

    // Preserve legacy callers' exact rendered prompt and separate system argument.
    override suspend fun generate(prompt: String, systemPrompt: String, settings: JarvisSettings): String {
        val session = synchronized(this) { sessionId ?: "legacy".also { sessionId = it; epoch++ } }
        val request = InferenceRequest(session, UUID.randomUUID().toString(), systemPrompt, "", prompt,
            limits = GenerationLimits(settings.contextSize, settings.maxTokens))
        return execute(request, prompt) { InferenceResponse(session, request.requestId, it.generate(prompt, systemPrompt, settings)) }.text
    }

    private suspend fun execute(request: InferenceRequest, prompt: String = request.renderPrompt(), call: suspend (LocalLanguageModel) -> InferenceResponse): InferenceResponse = operate { backend ->
        validateInference(request, backend.capabilities, prompt)
        val token = synchronized(this) {
            if (sessionId != request.sessionId) throw InferenceRejected(InferenceRefusal.STALE_RESPONSE)
            epoch
        }
        val response = drain { beginNative(mayAllocate = true); call(backend).also { result ->
            mark { copy(nativeDone = result.nativeDone, quarantined = !result.nativeDone) }
        } }
        if (!response.nativeDone) throw InferenceRejected(InferenceRefusal.RELEASE_UNCONFIRMED)
        currentCoroutineContext().ensureActive()
        synchronized(this) {
            if (mutableState.value.stopRequested) throw InferenceRejected(InferenceRefusal.STOP_REQUESTED)
            if (token != epoch || sessionId != request.sessionId || response.sessionId != request.sessionId || response.requestId != request.requestId)
                throw InferenceRejected(InferenceRefusal.STALE_RESPONSE)
        }
        response
    }

    override suspend fun unload(): UnloadResult {
        return operate { backend ->
            // A returned inference/load is distinct from a confirmed release.
            val result = drain { beginNative(mayAllocate = false); backend.unload().also { release ->
                if (release == UnloadResult.CONFIRMED) {
                    admissionToken?.let { token -> factory.admission.confirmRelease(backend, token) }
                    admissionToken = null
                }
                mark { copy(nativeDone = true, modelUnloaded = release == UnloadResult.CONFIRMED,
                    quarantined = release != UnloadResult.CONFIRMED,
                    error = if (release == UnloadResult.CONFIRMED) null else "Libération du moteur non attestée") }
            } }
            result
        }
    }

    private suspend fun <T> operate(block: suspend (LocalLanguageModel) -> T): T {
        currentCoroutineContext().ensureActive()
        if (!gate.tryLock()) throw InferenceRejected(InferenceRefusal.BUSY)
        try {
            val backend = synchronized(this) {
                val current = mutableState.value
                if (current.busy) throw InferenceRejected(InferenceRefusal.BUSY)
                if (current.quarantined || !current.nativeDone) throw InferenceRejected(InferenceRefusal.RELEASE_UNCONFIRMED)
                mutableState.value = current.copy(busy = true, stopRequested = false, error = null)
                engine ?: factory.create(current.mode).also { engine = it }
            }
            return block(backend)
        } catch (cancel: CancellationException) { throw cancel }
        catch (failure: Throwable) {
            val unconfirmed = failure is InferenceRejected && failure.reason == InferenceRefusal.RELEASE_UNCONFIRMED
            mark { copy(nativeDone = if (failure is InferenceFailure) failure.nativeDone else nativeDone,
                modelUnloaded = if (unconfirmed) false else modelUnloaded,
                quarantined = quarantined || unconfirmed || (failure is InferenceFailure && (!failure.nativeDone || failure.releaseUnconfirmed)) ||
                    (failure !is InferenceFailure && !nativeDone), error = failure.message) }
            throw failure
        } finally {
            mark { copy(busy = false) }
            gate.unlock()
        }
    }

    /** Cancellation requests stop but drains the real operation before releasing the gate. */
    private suspend fun <T> drain(block: suspend () -> T): T = supervisorScope {
        val caller = currentCoroutineContext()[Job]
        val work = async(NonCancellable) {
            try {
                if (caller?.isActive == false) {
                    requestStop()
                    throw CancellationException("Annulation avant l'appel moteur")
                }
                block()
            }
            catch (failure: InferenceFailure) {
                mark { copy(nativeDone = failure.nativeDone,
                    quarantined = !failure.nativeDone || failure.releaseUnconfirmed, error = failure.message) }
                throw failure
            }
        }
        try { work.await() }
        catch (cancel: CancellationException) {
            requestStop()
            withContext(NonCancellable) {
                try { work.await() } catch (failure: Throwable) {
                    mark { copy(quarantined = quarantined || !nativeDone, error = failure.message) }
                }
            }
            throw cancel
        } finally { withContext(NonCancellable) { work.join() } }
    }

    @Synchronized private fun beginNative(mayAllocate: Boolean) {
        if (mutableState.value.stopRequested) throw InferenceRejected(InferenceRefusal.STOP_REQUESTED)
        if (mayAllocate) admissionToken = factory.admission.acquire(requireNotNull(engine))
        mutableState.value = mutableState.value.copy(nativeDone = false,
            modelUnloaded = if (mayAllocate) false else mutableState.value.modelUnloaded)
    }

    @Synchronized private fun mark(change: InferenceLifecycle.() -> InferenceLifecycle) {
        val next = mutableState.value.change()
        if (next.quarantined) engine?.let { backend ->
            admissionToken?.let { token -> factory.admission.quarantine(backend, token) }
        }
        mutableState.value = next
    }
}
