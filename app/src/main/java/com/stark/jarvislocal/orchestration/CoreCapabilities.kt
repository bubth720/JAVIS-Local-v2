package com.stark.jarvislocal.orchestration

import android.os.SystemClock
import com.stark.jarvislocal.ChatMessage
import com.stark.jarvislocal.LocalDatabase
import com.stark.jarvislocal.RagEngine
import com.stark.jarvislocal.localTimeReply

/** Real existing CORE operations; no new engine, database, command parser or inference admission. */
class CoreCapabilities(private val database: LocalDatabase, private val rag: RagEngine,
    private val conversation: suspend (String) -> ChatMessage,
    private val conversationQualified: Boolean = false) {
    fun registry() = StrategyRegistry(listOf(
        adapter("core.history", IntentCategory.READ) { request ->
            database.recentMessages(request.limit).map { IntentItem(it.id.toString(), it.text.take(1500), it.role) }
        },
        adapter("core.documents", IntentCategory.READ) { request ->
            database.documentNames().take(request.limit).mapIndexed { index, name -> IntentItem(index.toString(), name.take(1500)) }
        },
        adapter("core.search", IntentCategory.SEARCH) { request ->
            require(request.input.isNotBlank())
            rag.retrieve(request.input, 10).filter {
                request.context.documentIds.isEmpty() || it.chunk.documentId in request.context.documentIds
            }.take(request.limit).map {
                IntentItem(it.chunk.id.toString(), it.chunk.text.take(3000), it.chunk.documentName.take(512))
            }
        },
        adapter("core.time", IntentCategory.DIAGNOSTIC) { listOf(IntentItem("local-clock", localTimeReply())) },
        adapter("core.conversation", IntentCategory.CONVERSATION, conversationQualified) { request ->
            require(request.input.isNotBlank())
            listOf(IntentItem(request.requestId, conversation(request.input).text))
        }
    ))
    private fun adapter(id: String, category: IntentCategory, available: Boolean = true,
        operation: suspend (IntentEnvelope) -> List<IntentItem>) = object : LocalCapabilityAdapter {
        override val descriptor = CapabilityDescriptor(id, category, available)
        override suspend fun execute(intent: IntentEnvelope) = operation(intent)
    }
}

/** Only constructed explicitly by a host; default disabled, no automatic personal data reads. */
class MvpIntentFacade(registry: StrategyRegistry, permissions: IntentPermissions,
    private val elapsedMs: () -> Long = SystemClock::elapsedRealtime) {
    @Volatile private var enabled = false
    val isEnabled: Boolean get() = enabled
    val tasks = TaskCoordinator(IntentRouter(registry, permissions), elapsedMs, enabled = { enabled })
    fun setEnabled(value: Boolean): Boolean = synchronized(tasks) {
        if (tasks.isBusy()) false else { enabled = value; true }
    }
    suspend fun execute(intent: IntentEnvelope): IntentResult {
        return tasks.execute(intent)
    }
    fun request(sessionId: String, capability: String, category: IntentCategory, input: String = "") =
        IntentEnvelope(sessionId, capabilityId = capability, category = category, input = input,
            deadlineElapsedMs = elapsedMs() + 30_000)
}
