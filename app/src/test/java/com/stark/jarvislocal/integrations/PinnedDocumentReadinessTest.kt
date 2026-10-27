package com.stark.jarvislocal.integrations

import com.stark.jarvislocal.events.ConnectorRegistry
import org.junit.Assert.*
import org.junit.Test

class PinnedDocumentReadinessTest {
    @Test fun pinnedReferencesNeverTurnMetadataIntoExecution() {
        val registry=ConnectorRegistry.defaults()
        val reader=PinnedDocumentReadiness(UnavailableDocumentToolReadiness(registry))
        for(p in PinnedDocumentProducer.entries) {
            assertTrue(p.sourceCommit.matches(Regex("[0-9a-f]{40}")))
            val result=reader.inspect(p)
            assertFalse(result.canExecute)
            assertEquals(IntegrationBlockState.BLOCKED,result.state)
            assertEquals(IntegrationBlockReason.FEATURE_DISABLED,result.reason)
        }
    }
    @Test fun producerInspectionFailureStaysUnknownWithoutLeakingPrivateText() {
        val port=object: DocumentToolReadinessPort {
            override val enabled=false
            override fun inspect(dependency:DocumentToolDependency):IntegrationBlock = error("synthetic-private-path")
            override fun requestLabOptIn(dependency:DocumentToolDependency):LabAdmissionRefused = error("must not request consent")
        }
        val result=PinnedDocumentReadiness(port).inspect(PinnedDocumentProducer.SERVICES)
        assertEquals(IntegrationBlockState.UNKNOWN,result.state)
        assertFalse(result.canExecute);assertFalse(result.toString().contains("private"))
    }
    @Test fun repeatedInspectionDoesNotRequestAdmissionOrChangeRegistry() {
        val registry=ConnectorRegistry.defaults();val before=registry.health()
        val reader=PinnedDocumentReadiness(UnavailableDocumentToolReadiness(registry))
        repeat(10) { assertFalse(reader.inspect(PinnedDocumentProducer.NEXUS).canExecute) }
        assertEquals(before,registry.health())
    }
}
