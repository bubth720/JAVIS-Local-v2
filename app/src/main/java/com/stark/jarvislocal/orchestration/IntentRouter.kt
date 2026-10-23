package com.stark.jarvislocal.orchestration

import java.util.Collections
import java.util.UUID

/** Trusted in-process intent, never deserialized from model output or a document. */
data class IntentEnvelope(
    val sessionId: String,
    val requestId: String = UUID.randomUUID().toString(),
    val capabilityId: String,
    val category: IntentCategory,
    val input: String = "",
    val context: IntentContext = IntentContext(),
    val limit: Int = 10,
    val origin: IntentOrigin = IntentOrigin.LOCAL_UI,
    val deadlineElapsedMs: Long,
    val confirmation: Confirmation = Confirmation.MISSING,
    val version: Int = 1
) {
    init {
        require(version == 1) { "Unsupported intent version" }
        require(UUID.fromString(sessionId).toString() == sessionId)
        require(UUID.fromString(requestId).toString() == requestId)
        require(capabilityId.matches(Regex("[a-z][a-z0-9_.-]{0,79}")))
        require(input.toByteArray(Charsets.UTF_8).size <= 2048)
        require(limit in 1..40 && deadlineElapsedMs >= 0)
    }
}
/** Only caller-selected document IDs; never a system prompt or inferred authorization. */
class IntentContext(documentIds: Collection<Long> = emptyList()) {
    val documentIds: List<Long> = Collections.unmodifiableList(ArrayList(documentIds))
    init { require(this.documentIds.size <= 32 && this.documentIds.all { it > 0 } && this.documentIds.distinct().size == this.documentIds.size) }
}
enum class IntentCategory { CONVERSATION, READ, SEARCH, DIAGNOSTIC, TOOL }
enum class IntentOrigin { LOCAL_UI, JVM_DEMONSTRATION }
enum class Confirmation { MISSING, DECLINED, EXPLICIT }

data class CapabilityDescriptor(val id: String, val category: IntentCategory,
    val available: Boolean = true, val requiresConfirmation: Boolean = false)

/** Host-owned allowlist is independent of the payload's confirmation or origin label. */
class IntentPermissions(allowedCapabilities: Set<String>) {
    private val allowed = allowedCapabilities.toSet()
    fun permits(id: String): Boolean = id in allowed
}

data class IntentItem(val id: String, val text: String, val source: String? = null)
sealed class IntentResult {
    data class Completed(val sessionId: String, val requestId: String,
        val capabilityId: String, val items: List<IntentItem>) : IntentResult()
    data class Refused(val reason: Reason) : IntentResult()
    data class CapabilityUnavailable(val capabilityId: String) : IntentResult()
    data class UserConfirmationRequired(val capabilityId: String) : IntentResult()
    enum class Reason { DISABLED, UNAUTHORIZED, DECLINED, BUSY, EXPIRED, CANCELLED, REPLAY,
        QUOTA, INVALID_CATEGORY, INVALID_INPUT, FAILED }
}

interface LocalCapabilityAdapter {
    val descriptor: CapabilityDescriptor
    suspend fun execute(intent: IntentEnvelope): List<IntentItem>
}

/** No dynamic discovery or replacement of an existing capability. */
class StrategyRegistry(adapters: List<LocalCapabilityAdapter>) {
    private val strategies: Map<String, LocalCapabilityAdapter>
    init {
        require(adapters.size <= 32 && adapters.map { it.descriptor.id }.distinct().size == adapters.size)
        strategies = adapters.associateBy { it.descriptor.id }
    }
    fun descriptors(): List<CapabilityDescriptor> = strategies.values.map { it.descriptor }
    internal fun find(id: String): LocalCapabilityAdapter? = strategies[id]
}

class IntentRouter(private val registry: StrategyRegistry, private val permissions: IntentPermissions) {
    suspend fun route(intent: IntentEnvelope): IntentResult {
        val adapter = registry.find(intent.capabilityId)
            ?: return IntentResult.CapabilityUnavailable(intent.capabilityId)
        if (!permissions.permits(intent.capabilityId)) return IntentResult.Refused(IntentResult.Reason.UNAUTHORIZED)
        if (adapter.descriptor.category != intent.category) return IntentResult.Refused(IntentResult.Reason.INVALID_CATEGORY)
        if (intent.context.documentIds.isNotEmpty() && intent.category != IntentCategory.SEARCH)
            return IntentResult.Refused(IntentResult.Reason.INVALID_INPUT)
        if (!adapter.descriptor.available) return IntentResult.CapabilityUnavailable(intent.capabilityId)
        if (adapter.descriptor.requiresConfirmation) {
            if (intent.confirmation == Confirmation.DECLINED) return IntentResult.Refused(IntentResult.Reason.DECLINED)
            if (intent.confirmation != Confirmation.EXPLICIT) return IntentResult.UserConfirmationRequired(intent.capabilityId)
        }
        val items = adapter.execute(intent)
        require(items.size <= 40 && items.sumOf { it.text.toByteArray(Charsets.UTF_8).size.toLong() } <= 65536)
        return IntentResult.Completed(intent.sessionId, intent.requestId, intent.capabilityId,
            Collections.unmodifiableList(ArrayList(items)))
    }
}
