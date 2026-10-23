package com.stark.jarvislocal.events

enum class ModuleHealth { DISABLED, UNAVAILABLE, CONNECTED, DEGRADED }
sealed class ConnectionResult<out T> {
    data class Connected<T>(val port: T) : ConnectionResult<T>()
    data class Refused(val health: ModuleHealth) : ConnectionResult<Nothing>()
}
/** Capability ports only. This API does not define a transport, signature permission or service. */
interface ModuleConnector<T> {
    val moduleId: String
    suspend fun discover(): ModuleHealth
    suspend fun connect(): ConnectionResult<T>
    fun disconnect()
    fun health(): ModuleHealth
}
/** Production default for not-yet-authorized external modules: never fabricates a connected port. */
class UnavailableModuleConnector<T>(override val moduleId: String) : ModuleConnector<T> {
    @Volatile private var enabled = false
    init { require(moduleId in setOf("voice", "interface", "nexus", "services")) }
    fun setEnabled(value: Boolean) { enabled = value }
    override suspend fun discover() = health()
    override suspend fun connect(): ConnectionResult<T> = ConnectionResult.Refused(health())
    override fun disconnect() { enabled = false }
    override fun health() = if (enabled) ModuleHealth.UNAVAILABLE else ModuleHealth.DISABLED
}
class ConnectorRegistry(connectors: List<ModuleConnector<*>>) {
    private val entries = connectors.associateBy { it.moduleId }
    init { require(connectors.size <= 8 && entries.size == connectors.size) }
    fun health(): Map<String, ModuleHealth> = entries.mapValues { it.value.health() }
    fun find(moduleId: String): ModuleConnector<*>? = entries[moduleId]
    companion object {
        fun defaults() = ConnectorRegistry(listOf("voice", "interface", "nexus", "services").map { UnavailableModuleConnector<Any>(it) })
    }
}
