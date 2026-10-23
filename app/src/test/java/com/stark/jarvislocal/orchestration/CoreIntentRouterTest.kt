package com.stark.jarvislocal.orchestration

import android.app.Application
import com.stark.jarvislocal.ChatMessage
import com.stark.jarvislocal.LocalDatabase
import com.stark.jarvislocal.RagEngine
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 25, 35], application = Application::class)
class CoreIntentRouterTest {
    @Test fun fourRealCommandsPreserveSQLiteAndNeverInvokeModel() = runTest {
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("jarvis_local_v2.db")
        val db = LocalDatabase(context)
        try {
            db.addMessage(ChatMessage(role = "user", text = "fixture history"))
            db.addDocument("fixture.txt", "text/plain", listOf("Mars possède deux satellites : Phobos et Deimos."))
            val registry = CoreCapabilities(db, RagEngine(db), { error("No native model in this test") }).registry()
            val facade = MvpIntentFacade(registry, IntentPermissions(registry.descriptors().map { it.id }.toSet())) { 100 }
            val session = UUID.randomUUID().toString()
            assertEquals(IntentResult.Refused(IntentResult.Reason.DISABLED),
                facade.execute(facade.request(session, "core.history", IntentCategory.READ)))
            assertTrue(facade.setEnabled(true))
            val history = facade.execute(facade.request(session, "core.history", IntentCategory.READ)) as IntentResult.Completed
            val docs = facade.execute(facade.request(session, "core.documents", IntentCategory.READ)) as IntentResult.Completed
            val hits = facade.execute(facade.request(session, "core.search", IntentCategory.SEARCH, "Phobos")) as IntentResult.Completed
            val clock = facade.execute(facade.request(session, "core.time", IntentCategory.DIAGNOSTIC)) as IntentResult.Completed
            assertEquals("fixture history", history.items.single().text)
            assertEquals("fixture.txt", docs.items.single().text)
            assertEquals("fixture.txt", hits.items.single().source)
            assertTrue(hits.items.single().text.contains("Phobos"))
            val scoped = facade.request(session, "core.search", IntentCategory.SEARCH, "Phobos")
                .copy(context = IntentContext(listOf(999999L)))
            assertTrue((facade.execute(scoped) as IntentResult.Completed).items.isEmpty())
            assertTrue(clock.items.single().text.matches(Regex("Il est [0-9]{2}:[0-9]{2}\\.")))
            assertEquals(4, setOf(history.requestId, docs.requestId, hits.requestId, clock.requestId).size)
            assertTrue(history.sessionId == session && hits.sessionId == session)
            assertTrue(db.allMemories().isEmpty())
            assertEquals(1, db.recentMessages().size)
            assertEquals(IntentResult.CapabilityUnavailable("core.conversation"),
                facade.execute(facade.request(session, "core.conversation", IntentCategory.CONVERSATION, "test")))
            println("CORE-01 JVM SQLITE DEMO: history=${history.items.size}, documents=${docs.items.size}, search=${hits.items.size}, time=${clock.items.size}; no native inference")
        } finally { db.close(); context.deleteDatabase("jarvis_local_v2.db") }
    }
    @Test fun unknownCategoryAndEmptySearchRefuseWithoutWrites() = runTest {
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("jarvis_local_v2.db")
        val db = LocalDatabase(context)
        try {
            val registry = CoreCapabilities(db, RagEngine(db), { error("No engine") }).registry()
            val facade = MvpIntentFacade(registry, IntentPermissions(setOf("core.history", "core.search"))) { 100 }
            facade.setEnabled(true)
            val session = UUID.randomUUID().toString()
            assertEquals(IntentResult.CapabilityUnavailable("absent.tool"),
                facade.execute(facade.request(session, "absent.tool", IntentCategory.TOOL)))
            assertEquals(IntentResult.Refused(IntentResult.Reason.INVALID_CATEGORY),
                facade.execute(facade.request(session, "core.history", IntentCategory.TOOL)))
            assertEquals(IntentResult.Refused(IntentResult.Reason.INVALID_INPUT),
                facade.execute(facade.request(session, "core.search", IntentCategory.SEARCH)))
            assertTrue(db.recentMessages().isEmpty())
        } finally { db.close(); context.deleteDatabase("jarvis_local_v2.db") }
    }
}
