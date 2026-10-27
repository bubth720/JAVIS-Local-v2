package com.stark.jarvislocal.integrations

import com.stark.jarvislocal.events.ConnectorRegistry
import com.stark.jarvislocal.events.UnavailableModuleConnector
import org.junit.Assert.*
import org.junit.Test

class DocumentToolGateSnapshotTest {
    @Test fun allProducerGatesStayClosedByDefault() {
        val registry = ConnectorRegistry.defaults()
        val before = registry.health()
        val gates = DocumentToolGateSnapshot.capture(UnavailableDocumentToolReadiness(registry))
        assertEquals(listOf("nexus", "services"), gates.map { it.moduleId })
        assertTrue(gates.all { !it.canExecute && it.state == IntegrationBlockState.BLOCKED })
        assertEquals(before, registry.health())
    }

    @Test fun enabledConnectorDoesNotBecomeApprovedProducer() {
        val registry = ConnectorRegistry.defaults()
        val connector = registry.find("nexus") as UnavailableModuleConnector<*>
        connector.setEnabled(true)
        val gates = DocumentToolGateSnapshot.capture(UnavailableDocumentToolReadiness(registry))
        assertEquals(IntegrationBlockState.UNAVAILABLE, gates.first().state)
        assertEquals(IntegrationBlockReason.PRODUCER_ANDROID_UNQUALIFIED, gates.first().reason)
        assertTrue(gates.none { it.canExecute })
    }

    @Test fun repeatedSnapshotsNeverMutateRegistry() {
        val registry = ConnectorRegistry.defaults()
        val before = registry.health()
        repeat(10) {
            assertEquals(2, DocumentToolGateSnapshot.capture(UnavailableDocumentToolReadiness(registry)).size)
        }
        assertEquals(before, registry.health())
    }
}
