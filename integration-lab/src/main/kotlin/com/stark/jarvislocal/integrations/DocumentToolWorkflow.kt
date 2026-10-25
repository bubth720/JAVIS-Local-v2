package com.stark.jarvislocal.integrations

import fr.jarvis.nexus.rag.SearchScope
import fr.jarvis.services.modules.data.Data
import fr.jarvis.services.tools.ToolDispatcher
import java.util.UUID

/** Bounded, synchronous JVM owner workflow; no Android/IPC composition, model or second registry.
 * Approval is a separate trusted-host call. Never parse document/CSV text as an instruction.
 */
class DocumentToolWorkflow(private val knowledge: KnowledgeReadPort, private val tools: ServicesToolPort,
    private val receipts: WorkflowReceipts, private val access: HostAccessPort,
    private val elapsedMs: () -> Long = { System.nanoTime() / 1_000_000 }) {
    enum class Format { CSV, DOCX }
    class Preview internal constructor(val correlation: Correlation, val context: String, val normalizedCsv: String,
        val statistics: Map<String, String>, val review: ToolReview, internal val deadline: Long) {
        override fun toString() = "WorkflowPreview(content=REDACTED)"
    }
    private val pending = java.util.concurrent.ConcurrentHashMap<String, Preview>()
    private val cancelled = java.util.concurrent.ConcurrentHashMap.newKeySet<Correlation>()
    private val finishing = java.util.concurrent.atomic.AtomicBoolean()
    private val resolution = Any() // serialize final acceptance with logical cancellation, never worker await
    private data class Active(val correlation: Correlation, val operation: ToolOperation, val deadline: Long)
    @Volatile private var active: Active? = null

    @Synchronized fun prepare(c: Correlation, question: String, scope: SearchScope, csv: String,
        numericColumn: String, outputName: String, format: Format, deadlineMs: Long): Preview {
        if (active != null) throw IntegrationRefused("WORKFLOW_BUSY_OR_UNKNOWN")
        checkHost(c, deadlineMs)
        require(deadlineMs - elapsedMs() in 1..30_000)
        require(outputName.matches(Regex("[a-zA-Z0-9_-]{1,40}\\.${format.name.lowercase()}")))
        require(strictBytes(csv).size <= 4096 && strictBytes(numericColumn).size <= 128)
        receipts.begin(c)
        try {
            val binding = knowledge.search(SourceSearchRequest(c, question, scope))
            checkHost(c, deadlineMs)
            val context = ContextAssembler(access).assemble(binding, c, 12000)
            val step = tools.prepare(child(c, "normalize"), "data.csv.normalize", "1.0.0", mapOf("csv" to csv))
            val normalized = tools.executeReadOnly(step.id, step.digest).await(remaining(deadlineMs))
            if (normalized.status != ToolDispatcher.Status.SUCCESS) throw IntegrationRefused("CSV_NOT_COMPLETED")
            checkHost(c, deadlineMs)
            val text = normalized.value as String
            val stats = Data().csv(text).stats(numericColumn).toMap()
            val exportText = if (format == Format.CSV) text else "Synthetic analysis\n$text\n$stats\n$context"
            val review = tools.prepare(child(c, "export"), "documents.${format.name.lowercase()}.export", "1.0.0",
                mapOf("path" to outputName, "text" to exportText))
            checkHost(c, deadlineMs)
            return Preview(c, context, text, java.util.Collections.unmodifiableMap(stats), review, deadlineMs)
                .also { pending[c.requestId] = it }
        } catch (e: Exception) {
            // No export dispatched. Retrieval/normalization may be incomplete; never retry this ID.
            receipts.transition(c, WorkflowReceipts.State.UNKNOWN)
            throw e
        }
    }
    @Synchronized fun approve(c: Correlation, exactDigest: String): ToolOperation {
        val p = pending[c.requestId] ?: throw IntegrationRefused("PREVIEW_ABSENT_OR_CONSUMED")
        if (p.correlation != c) throw IntegrationRefused("RESPONSE_CORRELATION_MISMATCH")
        checkHost(c, p.deadline)
        if (p.review.digest != exactDigest) throw IntegrationRefused("PREVIEW_MISMATCH")
        if (active != null) throw IntegrationRefused("WORKFLOW_BUSY_OR_UNKNOWN")
        pending.remove(c.requestId)
        receipts.transition(c, WorkflowReceipts.State.DISPATCHED) // durable before any mutation
        return try { tools.confirmAndExecute(p.review.id, exactDigest).also { active = Active(c, it, p.deadline) } }
        catch (e: Exception) { receipts.transition(c, WorkflowReceipts.State.UNKNOWN); throw e }
    }
    fun finish(c: Correlation): ToolDispatcher.Result {
        if (!finishing.compareAndSet(false, true)) throw IntegrationRefused("WORKFLOW_BUSY_OR_UNKNOWN")
        try {
            val op = active ?: throw IntegrationRefused("OPERATION_ABSENT")
            if (op.correlation != c) throw IntegrationRefused("RESPONSE_CORRELATION_MISMATCH")
            if (receipts.state(c.requestId) != WorkflowReceipts.State.DISPATCHED) throw IntegrationRefused("TOOL_RESULT_UNKNOWN")
            try {
                val result = op.operation.await(remaining(op.deadline))
                return synchronized(resolution) {
                    checkHost(c, op.deadline)
                    if (result.status != ToolDispatcher.Status.SUCCESS) throw IntegrationRefused("EXPORT_NOT_COMPLETED")
                    receipts.transition(c, WorkflowReceipts.State.COMPLETED)
                    active = null
                    result
                }
            } catch (e: Exception) {
                receipts.transition(c, WorkflowReceipts.State.UNKNOWN)
                // Keep blocked: UNKNOWN is never proof of worker/native completion.
                throw e
            }
        } finally { finishing.set(false) }
    }
    fun cancel(c: Correlation) = synchronized(resolution) {
        if (cancelled.size >= 256 && c !in cancelled) throw IntegrationRefused("CANCELLATION_QUOTA")
        cancelled.add(c) // observable even while prepare/await is working; no waiting for the worker
        pending[c.requestId]?.takeIf { it.correlation == c }?.let {
            if (pending.remove(c.requestId, it)) receipts.transition(c, WorkflowReceipts.State.REFUSED)
        }
        active?.takeIf { it.correlation == c }?.operation?.cancel()
    }
    private fun checkHost(c: Correlation, deadline: Long) {
        if (c in cancelled) throw IntegrationRefused("WORKFLOW_CANCELLED")
        if (!access.allows(c, "nexus.read") || !access.allows(c, "data.read") || !access.allows(c, "documents.export"))
            throw IntegrationRefused("CORE_DENIED_OR_STALE_SESSION")
        remaining(deadline)
    }
    private fun remaining(deadline: Long): Long {
        val now = elapsedMs()
        if (now < 0 || deadline <= now) throw IntegrationRefused("WORKFLOW_DEADLINE")
        return (deadline - now).coerceAtMost(5000)
    }
    private fun child(c: Correlation, step: String) = c.copy(requestId = UUID.nameUUIDFromBytes(
        strictBytes("core.workflow/1:${c.requestId}:$step")).toString())
}
