package com.stark.jarvislocal.orchestration

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Logical task states. COMPLETED never asserts native unload or releases an inference lease. */
enum class TaskPhase { RUNNING, CANCELLING, COMPLETED, REFUSED, FAILED, CANCELLED }
data class TaskState(val sessionId: String, val requestId: String, val capabilityId: String, val phase: TaskPhase)

/** Bounded in-memory IDs only. No input text, history or automatic retry is persisted here. */
class TaskCoordinator(private val router: IntentRouter, private val elapsedMs: () -> Long,
    private val maxRequests: Int = 256, private val enabled: () -> Boolean = { true }) {
    private data class Active(val intent: IntentEnvelope, var cancelled: Boolean = false)
    private val active = mutableMapOf<String, Active>()
    private val accepted = mutableSetOf<Pair<String, String>>()
    private val mutableState = MutableStateFlow<List<TaskState>>(emptyList())
    val state = mutableState.asStateFlow()
    init { require(maxRequests in 1..4096) }

    /** Logical invalidation only: caller still waits for execute/underlying JNI to return. */
    @Synchronized fun cancel(sessionId: String, requestId: String): Boolean {
        val operation = active[sessionId]?.takeIf { it.intent.requestId == requestId } ?: return false
        operation.cancelled = true
        publish(operation.intent, TaskPhase.CANCELLING)
        return true
    }
    @Synchronized fun isBusy(): Boolean = active.isNotEmpty()
    @Synchronized private fun begin(intent: IntentEnvelope): IntentResult? {
        if (!enabled()) return IntentResult.Refused(IntentResult.Reason.DISABLED)
        if (intent.sessionId in active) return IntentResult.Refused(IntentResult.Reason.BUSY)
        if (elapsedMs() >= intent.deadlineElapsedMs) return IntentResult.Refused(IntentResult.Reason.EXPIRED)
        val key = intent.sessionId to intent.requestId
        if (key in accepted) return IntentResult.Refused(IntentResult.Reason.REPLAY)
        if (accepted.size >= maxRequests) return IntentResult.Refused(IntentResult.Reason.QUOTA)
        accepted += key
        active[intent.sessionId] = Active(intent)
        publish(intent, TaskPhase.RUNNING)
        return null
    }
    @Synchronized private fun invalidated(intent: IntentEnvelope): IntentResult? = when {
        active[intent.sessionId]?.cancelled == true -> IntentResult.Refused(IntentResult.Reason.CANCELLED)
        elapsedMs() >= intent.deadlineElapsedMs -> IntentResult.Refused(IntentResult.Reason.EXPIRED)
        else -> null
    }
    @Synchronized private fun publish(intent: IntentEnvelope, phase: TaskPhase) {
        mutableState.value = (mutableState.value.filterNot {
            it.sessionId == intent.sessionId && it.requestId == intent.requestId
        } + TaskState(intent.sessionId, intent.requestId, intent.capabilityId, phase)).takeLast(64)
    }
    @Synchronized private fun finish(intent: IntentEnvelope, result: IntentResult) {
        // A cancellation racing the adapter return may not make a stale success observable.
        active.remove(intent.sessionId)
        publish(intent, when {
            result is IntentResult.Completed -> TaskPhase.COMPLETED
            result == IntentResult.Refused(IntentResult.Reason.CANCELLED) -> TaskPhase.CANCELLED
            result == IntentResult.Refused(IntentResult.Reason.FAILED) -> TaskPhase.FAILED
            else -> TaskPhase.REFUSED
        })
    }
    suspend fun execute(intent: IntentEnvelope): IntentResult {
        begin(intent)?.let { return it }
        var result: IntentResult = IntentResult.Refused(IntentResult.Reason.CANCELLED)
        try {
            currentCoroutineContext().ensureActive()
            val routed = invalidated(intent) ?: router.route(intent)
            currentCoroutineContext().ensureActive()
            // Check cancellation + publish completion atomically relative to cancel().
            return synchronized(this) {
                result = invalidated(intent) ?: routed
                finish(intent, result)
                result
            }
        } catch (cancelled: CancellationException) {
            result = IntentResult.Refused(IntentResult.Reason.CANCELLED)
            throw cancelled
        } catch (_: IllegalArgumentException) {
            result = IntentResult.Refused(IntentResult.Reason.INVALID_INPUT)
            return result
        } catch (_: Exception) {
            result = IntentResult.Refused(IntentResult.Reason.FAILED)
            return result
        } finally {
            synchronized(this) { if (active[intent.sessionId]?.intent?.requestId == intent.requestId) finish(intent, result) }
        }
    }
}
