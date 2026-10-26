package com.stark.jarvislocal.integrations

import com.stark.jarvislocal.events.ConnectorRegistry
import com.stark.jarvislocal.events.ModuleHealth

/** Local diagnostic projection, not an approved UNIFIED wire contract or an executable tool catalogue. */
enum class DocumentToolDependency(val moduleId: String) { NEXUS("nexus"), SERVICES("services") }
enum class IntegrationBlockState { BLOCKED, UNAVAILABLE, UNKNOWN }
enum class IntegrationBlockReason {
    FEATURE_DISABLED, PRODUCER_ANDROID_UNQUALIFIED, HEALTH_NOT_QUALIFICATION,
    RECOVERY_REQUIRED, MODULE_ABSENT, HEALTH_UNAVAILABLE
}
class IntegrationBlock internal constructor(val dependency: DocumentToolDependency,
    val state: IntegrationBlockState, val reason: IntegrationBlockReason) {
    val canExecute: Boolean get() = false
    override fun toString() = "IntegrationBlock($dependency, $state, $reason)"
}
class LabAdmissionRefused internal constructor(val block: IntegrationBlock) {
    override fun toString() = "LabAdmissionRefused($block)"
}

/** No URI/text/approval is accepted while the producer gates are red. No success/port can be returned. */
interface DocumentToolReadinessPort {
    val enabled: Boolean
    fun inspect(dependency: DocumentToolDependency): IntegrationBlock
    fun requestLabOptIn(dependency: DocumentToolDependency): LabAdmissionRefused
}

/** Reuses AppGraph's existing connector registry; never discovers, connects or initializes a producer.
 * Health is diagnostic metadata, never evidence of API24/25 qualification or host authorization.
 * There is deliberately no activation setter/factory until NEXUS and SERVICES gates are reviewed.
 */
class UnavailableDocumentToolReadiness(private val connectors: ConnectorRegistry) : DocumentToolReadinessPort {
    override val enabled: Boolean get() = false
    override fun inspect(dependency: DocumentToolDependency): IntegrationBlock {
        val connector = connectors.find(dependency.moduleId)
            ?: return IntegrationBlock(dependency, IntegrationBlockState.UNAVAILABLE, IntegrationBlockReason.MODULE_ABSENT)
        val health = try { connector.health() } catch (_: Exception) {
            return IntegrationBlock(dependency, IntegrationBlockState.UNKNOWN, IntegrationBlockReason.HEALTH_UNAVAILABLE)
        }
        return when (health) {
            ModuleHealth.DISABLED -> IntegrationBlock(dependency, IntegrationBlockState.BLOCKED, IntegrationBlockReason.FEATURE_DISABLED)
            ModuleHealth.UNAVAILABLE -> IntegrationBlock(dependency, IntegrationBlockState.UNAVAILABLE, IntegrationBlockReason.PRODUCER_ANDROID_UNQUALIFIED)
            ModuleHealth.CONNECTED -> IntegrationBlock(dependency, IntegrationBlockState.UNKNOWN, IntegrationBlockReason.HEALTH_NOT_QUALIFICATION)
            ModuleHealth.DEGRADED -> IntegrationBlock(dependency, IntegrationBlockState.UNKNOWN, IntegrationBlockReason.RECOVERY_REQUIRED)
        }
    }
    override fun requestLabOptIn(dependency: DocumentToolDependency) = LabAdmissionRefused(inspect(dependency))
}
