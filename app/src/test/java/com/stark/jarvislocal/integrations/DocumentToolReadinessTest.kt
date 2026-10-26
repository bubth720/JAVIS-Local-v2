package com.stark.jarvislocal.integrations

import android.app.Application
import com.stark.jarvislocal.AppGraph
import com.stark.jarvislocal.ChatMessage
import com.stark.jarvislocal.SessionPhase
import com.stark.jarvislocal.TtsStatus
import com.stark.jarvislocal.events.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Host Android simulation only; neither a real producer nor FTS5/SAF/Android instrumentation proof. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 25, 26, 35], application = Application::class)
class DocumentToolReadinessTest {
    @Test fun defaultGraphDependenciesAreBlockedWithoutOpeningAnyPort() {
        val registry = ConnectorRegistry.defaults(); val port = UnavailableDocumentToolReadiness(registry)
        assertFalse(port.enabled)
        for (dep in DocumentToolDependency.entries) {
            val block = port.inspect(dep)
            assertEquals(IntegrationBlockState.BLOCKED, block.state)
            assertEquals(IntegrationBlockReason.FEATURE_DISABLED, block.reason)
            assertFalse(block.canExecute)
        }
        assertTrue(registry.health().values.all { it == ModuleHealth.DISABLED })
    }
    @Test fun repeatedOptInIsRefusedAndNeverChangesTheFeatureFlagOrConnectorState() {
        val registry = ConnectorRegistry.defaults(); val before = registry.health()
        val port = UnavailableDocumentToolReadiness(registry)
        repeat(3) { for (dep in DocumentToolDependency.entries) assertFalse(port.requestLabOptIn(dep).block.canExecute) }
        assertFalse(port.enabled); assertEquals(before, registry.health())
    }
    @Test fun enabledDiscoveryMetadataCannotAdmitAnUnqualifiedProducer() {
        val registry = ConnectorRegistry.defaults(); val port = UnavailableDocumentToolReadiness(registry)
        for (dep in DocumentToolDependency.entries) {
            val connector = registry.find(dep.moduleId) as UnavailableModuleConnector<*>
            connector.setEnabled(true)
            assertEquals(IntegrationBlockState.UNAVAILABLE, port.requestLabOptIn(dep).block.state)
            assertEquals(IntegrationBlockReason.PRODUCER_ANDROID_UNQUALIFIED, port.inspect(dep).reason)
            assertFalse(port.enabled)
            connector.disconnect()
            assertEquals(IntegrationBlockState.BLOCKED, port.inspect(dep).state)
        }
    }
    @Test fun connectedOrDegradedHealthCannotManufactureAndroidQualificationOrRelease() {
        val c = MetadataConnector("nexus")
        val port = UnavailableDocumentToolReadiness(ConnectorRegistry(listOf(c)))
        c.value = ModuleHealth.CONNECTED
        assertEquals(IntegrationBlockReason.HEALTH_NOT_QUALIFICATION, port.inspect(DocumentToolDependency.NEXUS).reason)
        c.value = ModuleHealth.DEGRADED
        assertEquals(IntegrationBlockReason.RECOVERY_REQUIRED, port.inspect(DocumentToolDependency.NEXUS).reason)
        assertEquals(IntegrationBlockState.UNKNOWN, port.requestLabOptIn(DocumentToolDependency.NEXUS).block.state)
        assertEquals(0, c.operations); assertFalse(port.enabled)
    }
    @Test fun missingOrFailedHealthIsRefusedAndDoesNotExposePrivateFailureText() {
        val c = MetadataConnector("nexus"); c.fail = true
        val port = UnavailableDocumentToolReadiness(ConnectorRegistry(listOf(c)))
        assertEquals(IntegrationBlockReason.MODULE_ABSENT, port.inspect(DocumentToolDependency.SERVICES).reason)
        val result = port.requestLabOptIn(DocumentToolDependency.NEXUS)
        assertEquals(IntegrationBlockReason.HEALTH_UNAVAILABLE, result.block.reason)
        assertFalse(result.toString().contains("synthetic-secret")); assertEquals(0, c.operations)
    }
    @Test fun concurrentOptInsDoNotAcquireAProducerOrBecomeReady() {
        val c = MetadataConnector("services"); c.value = ModuleHealth.CONNECTED
        val port = UnavailableDocumentToolReadiness(ConnectorRegistry(listOf(c)))
        val workers = Executors.newFixedThreadPool(3)
        try {
            val results = (1..24).map { workers.submit<LabAdmissionRefused> { port.requestLabOptIn(DocumentToolDependency.SERVICES) } }
            assertTrue(results.all { !it.get(5, TimeUnit.SECONDS).block.canExecute })
            assertFalse(port.enabled); assertEquals(0, c.operations)
        } finally { workers.shutdownNow() }
    }
    @Test fun appGraphUsesItsExistingRegistryWithoutPreparingModelsOrChangingSession() {
        val graph = AppGraph(RuntimeEnvironment.getApplication())
        try {
            val before = graph.session.state.value
            (graph.connectors.find("nexus") as UnavailableModuleConnector<*>).setEnabled(true)
            assertEquals(IntegrationBlockState.UNAVAILABLE, graph.documentToolsReadiness.inspect(DocumentToolDependency.NEXUS).state)
            assertFalse(graph.documentToolsReadiness.requestLabOptIn(DocumentToolDependency.NEXUS).block.canExecute)
            assertEquals(before, graph.session.state.value); assertEquals(SessionPhase.PREPARING, before.phase)
            assertFalse(before.busy); assertFalse(before.prepared)
            assertFalse(graph.llm.isLoaded()); assertEquals(TtsStatus.STOPPED, graph.tts.status.value)
        } finally { graph.db.close(); graph.tts.shutdown() }
    }
    @Test fun refusalAndRecreatedGraphPreserveSyntheticHistoryMemoriesDocumentsAndSettings() {
        val app = RuntimeEnvironment.getApplication(); val first = AppGraph(app)
        first.db.addMessage(ChatMessage(role = "user", text = "synthetic retained history"))
        first.db.addMemory("synthetic explicitly retained memory")
        first.db.addDocument("synthetic.txt", "text/plain", listOf("synthetic retained passage"))
        first.settings.save(first.settings.load().copy(systemPrompt = "synthetic historical SYSTEM", contextSize = 1024, maxTokens = 128, topKDocuments = 2))
        val messages = first.db.recentMessages(); val memories = first.db.allMemories()
        val chunks = first.db.allChunks(); val settings = first.settings.load()
        first.documentToolsReadiness.requestLabOptIn(DocumentToolDependency.SERVICES)
        first.db.close(); first.tts.shutdown()
        val second = AppGraph(app)
        try {
            assertFalse(second.documentToolsReadiness.enabled)
            assertEquals(messages, second.db.recentMessages()); assertEquals(memories, second.db.allMemories())
            assertEquals(chunks, second.db.allChunks()); assertEquals(listOf("synthetic.txt"), second.db.documentNames())
            assertEquals(settings, second.settings.load()); assertFalse(second.llm.isLoaded())
            assertFalse(second.session.state.value.prepared)
        } finally { second.db.close(); second.tts.shutdown() }
    }
    private class MetadataConnector(override val moduleId: String) : ModuleConnector<Any> {
        @Volatile var value = ModuleHealth.DISABLED
        var operations = 0
        var fail = false
        override fun health(): ModuleHealth {
            if (fail) throw IllegalStateException("synthetic-secret-source-or-path")
            return value
        }
        override suspend fun discover(): ModuleHealth { operations++; error("Unexpected discovery") }
        override suspend fun connect(): ConnectionResult<Any> { operations++; error("Unexpected producer connection") }
        override fun disconnect() { operations++; error("Unexpected producer lifecycle change") }
    }
}
