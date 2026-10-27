package com.stark.jarvislocal.integrations

/** Diagnostic references only. These SHAs do not replace the consumed JVM dependencies. */
enum class PinnedDocumentProducer(val dependency: DocumentToolDependency, val sourceCommit: String) {
    NEXUS(DocumentToolDependency.NEXUS, "201f0d2b03209de104157595a557143fe87eb94e"),
    SERVICES(DocumentToolDependency.SERVICES, "ea541d20b08f7ad0949354829a977f6752cbffa3")
}
data class PinnedDocumentStatus(val producer: PinnedDocumentProducer, val state: IntegrationBlockState,
    val reason: IntegrationBlockReason) {
    val canExecute: Boolean get() = false
}
/** Reuses the existing diagnostic port. No factory, discovery, URI, consent or effect callback. */
class PinnedDocumentReadiness(private val readiness: DocumentToolReadinessPort) {
    fun inspect(producer: PinnedDocumentProducer): PinnedDocumentStatus = try {
        val block = readiness.inspect(producer.dependency)
        PinnedDocumentStatus(producer, block.state, block.reason)
    } catch (_: Exception) {
        PinnedDocumentStatus(producer, IntegrationBlockState.UNKNOWN, IntegrationBlockReason.HEALTH_UNAVAILABLE)
    }
}
