package com.stark.jarvislocal.integrations

import fr.jarvis.services.modules.guard.Guard
import fr.jarvis.services.tools.*
import java.util.UUID
import java.util.concurrent.TimeUnit

class ToolReview internal constructor(val id: String, val correlation: Correlation, val invocation: ToolInvocation,
    val descriptor: ToolDescriptor, internal val createdMs: Long, internal val deadlineMs: Long) {
    val digest: String get() = invocation.contentDigest()
    override fun toString() = "ToolReview(tool=${descriptor.id}, content=REDACTED)"
}
class ToolOperation internal constructor(val correlation: Correlation, private val handle: ToolDispatcher.Handle,
    private val permission: String, private val access: HostAccessPort) {
    fun cancel() { handle.cancel() } // logical cancellation, never a native release proof
    fun await(timeoutMs: Long = 5000): ToolDispatcher.Result {
        require(timeoutMs in 1..90000)
        val result = try { handle.result.get(timeoutMs, TimeUnit.MILLISECONDS) }
        catch (_: InterruptedException) { Thread.currentThread().interrupt(); throw IntegrationRefused("TOOL_RESULT_UNKNOWN") }
        catch (_: Exception) { throw IntegrationRefused("TOOL_RESULT_UNKNOWN") }
        if (result.requestId != correlation.requestId || !access.allows(correlation, permission))
            throw IntegrationRefused("RESPONSE_CORRELATION_MISMATCH_OR_REVOKED")
        return result
    }
}
interface ToolExecutionPort {
    fun prepare(correlation: Correlation, id: String, version: String, arguments: Map<String, String>): ToolReview
    fun executeReadOnly(reviewId: String, exactDigest: String): ToolOperation
}

/** Reuses SERVICES Guard/ApprovalGate; creates no second durable authority or executable tool catalogue. */
class ServicesToolAdapter(private val registry: ToolRegistry, private val dispatcher: ToolDispatcher,
    private val approvals: ApprovalGate, private val host: Guard.Context,
    private val access: HostAccessPort = HostAccessPort { _, _ -> false },
    private val elapsedMs: () -> Long = { System.nanoTime() / 1_000_000 }) : ToolExecutionPort {
    private val pending = linkedMapOf<String, ToolReview>()
    private val seenRequests = mutableSetOf<String>()
    @Synchronized override fun prepare(correlation: Correlation, id: String, version: String, arguments: Map<String, String>): ToolReview {
        val d = registry.descriptor(id, version)
        if (!access.allows(correlation, d.permission)) throw IntegrationRefused("CORE_DENIED_OR_STALE_SESSION")
        require(arguments.size <= 16 && arguments.values.sumOf { strictBytes(it).size.toLong() } <= 8192)
        val invocation = ToolInvocation(correlation.requestId, id, version, arguments, 5000, null)
        val now = elapsedMs(); require(now >= 0 && now <= Long.MAX_VALUE - 30_000)
        pending.entries.removeAll { now < it.value.createdMs || now >= it.value.deadlineMs }
        if (pending.size >= 32 || seenRequests.size >= 128) throw IntegrationRefused("REVIEW_QUOTA")
        if (!seenRequests.add(correlation.requestId)) throw IntegrationRefused("REQUEST_REPLAY")
        val review = ToolReview(UUID.randomUUID().toString(), correlation, invocation, d, now, now + 30_000)
        pending[review.id] = review; return review
    }
    override fun executeReadOnly(reviewId: String, exactDigest: String): ToolOperation = launch(reviewId, exactDigest, false)
    /** Only trusted owner UI may invoke this after displaying and approving the exact operation and digest. */
    fun confirmAndExecute(reviewId: String, exactDisplayedDigest: String): ToolOperation = launch(reviewId, exactDisplayedDigest, true)
    @Synchronized private fun launch(id: String, digest: String, confirmed: Boolean): ToolOperation {
        val p = pending[id] ?: throw IntegrationRefused("REVIEW_ABSENT_OR_CONSUMED")
        if (digest != p.digest) throw IntegrationRefused("PREVIEW_MISMATCH")
        if (!access.allows(p.correlation, p.descriptor.permission)) throw IntegrationRefused("CORE_DENIED_OR_STALE_SESSION")
        val now = elapsedMs()
        if (now < p.createdMs || now >= p.deadlineMs) { pending.remove(id); throw IntegrationRefused("REVIEW_EXPIRED") }
        if (p.descriptor.mutating && !confirmed) throw IntegrationRefused("CONFIRMATION_REQUIRED")
        pending.remove(id) // no replay or automatic retry, even if dispatch/ACK fails
        val raw = p.invocation
        val approved = if (p.descriptor.mutating) {
            val token = approvals.confirm(host, p.descriptor, raw)
            ToolInvocation(raw.requestId, raw.toolId, raw.version, raw.arguments, raw.timeoutMs, token)
        } else raw
        if (!access.allows(p.correlation, p.descriptor.permission)) throw IntegrationRefused("CORE_DENIED_OR_STALE_SESSION")
        return ToolOperation(p.correlation, dispatcher.dispatch(host, approved), p.descriptor.permission, access)
    }
}
