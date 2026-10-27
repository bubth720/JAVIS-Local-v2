package com.stark.jarvislocal.integrations

/** Read-only pre-fusion status for all producers. Never grants a capability. */
data class PassiveProducerGate(val moduleId: String, val state: IntegrationBlockState,
    val reason: IntegrationBlockReason, val canExecute: Boolean = false)

/** No new connector, permission, session, transport or SDK producer is constructed. */
object DocumentToolGateSnapshot {
    fun capture(readiness: DocumentToolReadinessPort): List<PassiveProducerGate> =
        DocumentToolDependency.entries.map { dependency ->
            val block = try { readiness.inspect(dependency) } catch (_: Exception) {
                IntegrationBlock(dependency, IntegrationBlockState.UNKNOWN, IntegrationBlockReason.HEALTH_UNAVAILABLE)
            }
            PassiveProducerGate(dependency.moduleId, block.state, block.reason)
        }
}
