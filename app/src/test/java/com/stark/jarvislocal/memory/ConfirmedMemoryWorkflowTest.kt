package com.stark.jarvislocal.memory

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import com.stark.jarvislocal.ChatMessage
import com.stark.jarvislocal.LocalDatabase
import java.io.File
import java.util.UUID
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 25, 35], application = Application::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class ConfirmedMemoryWorkflowTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val path get() = File(context.noBackupFilesDir, "confirmed_memory_lab.sqlite")
    private lateinit var store: ConfirmedMemoryStore
    private lateinit var workflow: MemoryApprovalWorkflow
    private var elapsed = 100L
    private var wall = 1000L
    private val session = UUID.randomUUID().toString()
    @Before fun setup() { path.delete(); store = ConfirmedMemoryStore(context); workflow = MemoryApprovalWorkflow(store, { elapsed }, { wall }) }
    @After fun teardown() { store.close(); path.delete() }
    private fun accept(text: String, scope: MemoryScope = MemoryScope.GENERAL, key: String? = null, expires: Long? = null): MemoryRecord {
        val candidate = workflow.propose(session, text, scope, key, expiresAtMs = expires)
        return (workflow.decide(candidate.id, session, candidate.content, MemoryDecision.ACCEPT) as MemoryApprovalResult.Confirmed).record
    }
    private fun sql(action: (SQLiteDatabase) -> Unit) = SQLiteDatabase.openDatabase(path.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use(action)
    @Test fun pendingAndRejectedContentNeverBecomeDurableMemory() {
        val candidate = workflow.propose(session, "synthetic declined preference")
        assertTrue(store.listOwned().isEmpty())
        assertEquals(MemoryApprovalResult.Rejected, workflow.decide(candidate.id, session, candidate.content, MemoryDecision.REJECT))
        assertTrue(store.listOwned().isEmpty()); assertTrue(store.auditOperations().isEmpty())
    }
    @Test fun exactContentPositiveApprovalIsRequiredAndCannotReplay() {
        val content = "  Préférence synthétique 🦉\nDeuxième ligne.  "
        val candidate = workflow.propose(session, content)
        assertEquals(MemoryApprovalResult.Refused("PREVIEW_MISMATCH"), workflow.decide(candidate.id, session, content.trim(), MemoryDecision.ACCEPT))
        val record = (workflow.decide(candidate.id, session, content, MemoryDecision.ACCEPT) as MemoryApprovalResult.Confirmed).record
        assertEquals(content, record.content); assertEquals(contentDigest(content), record.consent.contentDigest)
        assertEquals(MemoryApprovalResult.Refused("CANDIDATE_ABSENT_OR_CONSUMED"), workflow.decide(candidate.id, session, content, MemoryDecision.ACCEPT))
        store.close(); store = ConfirmedMemoryStore(context)
        assertEquals(record, store.readOwned(record.reference.id)); assertEquals(1, store.listOwned().size)
    }
    @Test fun wrongSessionExpiryAndRevocationPreventNewMemory() {
        val first = workflow.propose(session, "synthetic")
        assertEquals(MemoryApprovalResult.Refused("SESSION_MISMATCH"), workflow.decide(first.id, UUID.randomUUID().toString(), first.content, MemoryDecision.ACCEPT))
        elapsed = first.expiresElapsedMs
        assertEquals(MemoryApprovalResult.Refused("CONSENT_EXPIRED"), workflow.decide(first.id, session, first.content, MemoryDecision.ACCEPT))
        val second = workflow.propose(session, "another synthetic")
        workflow.revokeSession(session)
        assertEquals(MemoryApprovalResult.Refused("CANDIDATE_ABSENT_OR_CONSUMED"), workflow.decide(second.id, session, second.content, MemoryDecision.ACCEPT))
        assertTrue(store.listOwned().isEmpty())
    }
    @Test fun revocationDoesNotWaitForAnInFlightDecisionMonitor() {
        val candidate = workflow.propose(session, "synthetic pending revocation")
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        try {
            synchronized(workflow) { executor.submit { workflow.revokeSession(session) }.get(2, java.util.concurrent.TimeUnit.SECONDS) }
            assertEquals(MemoryApprovalResult.Refused("CANDIDATE_ABSENT_OR_CONSUMED"), workflow.decide(candidate.id, session, candidate.content, MemoryDecision.ACCEPT))
            assertTrue(store.listOwned().isEmpty())
        } finally { executor.shutdownNow() }
    }
    @Test fun delayedProposalCannotReuseARecreatedSessionEpoch() {
        val captured = java.util.concurrent.CountDownLatch(1)
        val resume = java.util.concurrent.CountDownLatch(1)
        val calls = java.util.concurrent.atomic.AtomicInteger()
        workflow = MemoryApprovalWorkflow(store, {
            if (calls.incrementAndGet() == 2) { captured.countDown(); check(resume.await(3, java.util.concurrent.TimeUnit.SECONDS)) }
            elapsed
        }, { wall })
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        try {
            val delayed = executor.submit<PendingMemoryCandidate> { workflow.propose(session, "synthetic delayed proposal") }
            assertTrue(captured.await(3, java.util.concurrent.TimeUnit.SECONDS))
            workflow.revokeSession(session)
            resume.countDown()
            val stale = delayed.get(3, java.util.concurrent.TimeUnit.SECONDS)
            val fresh = workflow.propose(session, "synthetic new incarnation")
            assertEquals(MemoryApprovalResult.Refused("SESSION_REVOKED"), workflow.decide(stale.id, session, stale.content, MemoryDecision.ACCEPT))
            assertTrue(store.listOwned().isEmpty())
            assertTrue(workflow.decide(fresh.id, session, fresh.content, MemoryDecision.ACCEPT) is MemoryApprovalResult.Confirmed)
        } finally { resume.countDown(); executor.shutdownNow() }
    }
    @Test fun lostAcknowledgementDoesNotImplyRollbackOrPermitNonceReplay() {
        val candidate = workflow.propose(session, "synthetic committed before acknowledgement")
        workflow.decide(candidate.id, session, candidate.content, MemoryDecision.ACCEPT) // UI acknowledgement is intentionally lost
        workflow.revokeSession(session)
        store.close(); store = ConfirmedMemoryStore(context)
        assertEquals(candidate.content, store.listOwned().single().content)
        assertEquals(MemoryApprovalResult.Refused("CANDIDATE_ABSENT_OR_CONSUMED"), workflow.decide(candidate.id, session, candidate.content, MemoryDecision.ACCEPT))
        assertEquals(1, store.listOwned().size)
    }
    @Test fun contradictoryPreferenceNeedsExplicitReplacementOfTheBoundRevision() {
        val first = accept("synthetic preference A", MemoryScope.PREFERENCE, "fixture.preference")
        val candidate = workflow.propose(session, "synthetic preference B", MemoryScope.PREFERENCE, "fixture.preference")
        assertEquals(MemoryApprovalResult.Conflict(first.reference), workflow.decide(candidate.id, session, candidate.content, MemoryDecision.ACCEPT))
        assertEquals(first, store.readOwned(first.reference.id))
        val changed = (workflow.decide(candidate.id, session, candidate.content, MemoryDecision.REPLACE) as MemoryApprovalResult.Confirmed).record
        assertEquals(2L, changed.reference.revision); assertEquals(candidate.content, changed.content)
        assertEquals(listOf("CREATED", "EDITED"), store.auditOperations())
    }
    @Test fun staleEditingCannotOverrideARevisedMemory() {
        val first = accept("initial synthetic")
        val a = workflow.propose(session, "synthetic A", edit = first.reference)
        val b = workflow.propose(session, "synthetic B", edit = first.reference)
        workflow.decide(b.id, session, b.content, MemoryDecision.REPLACE)
        assertEquals(MemoryApprovalResult.Refused("REVISION_CONFLICT"), workflow.decide(a.id, session, a.content, MemoryDecision.REPLACE))
        assertEquals("synthetic B", store.listOwned().single().content)
    }
    @Test fun readPortDeniesByDefaultAndRevocationExpiryAreImmediate() {
        val general = accept("general synthetic", expires = 2000)
        accept("sensitive synthetic", MemoryScope.SENSITIVE)
        val port = ConfirmedMemoryLookup(store) { wall }
        assertTrue(port.recent(ProjectionPolicy(), 10).isEmpty())
        assertEquals(listOf(general.reference), port.recent(ProjectionPolicy(setOf(MemoryScope.GENERAL)), 10).map { it.reference })
        store.suspendOwned(general.reference, true)
        assertTrue(port.recent(ProjectionPolicy(setOf(MemoryScope.GENERAL)), 10).isEmpty())
        store.suspendOwned(store.readOwned(general.reference.id).reference, false)
        assertEquals(1, port.recent(ProjectionPolicy(setOf(MemoryScope.GENERAL)), 10).size)
        wall = 2000
        assertTrue(port.recent(ProjectionPolicy(setOf(MemoryScope.GENERAL)), 10).isEmpty())
        assertTrue(port.recent(ProjectionPolicy(setOf(MemoryScope.SENSITIVE)), 10).isEmpty())
    }
    @Test fun deletionSurvivesReopeningWithoutChangingChatHistory() {
        val legacy = LocalDatabase(context)
        try {
            legacy.addMessage(ChatMessage(role = "user", text = "historical synthetic chat"))
            val messages = legacy.recentMessages()
            val record = accept("synthetic to delete")
            store.deleteOwned(record.reference)
            store.close(); store = ConfirmedMemoryStore(context)
            assertTrue(store.listOwned().isEmpty()); assertEquals(messages, legacy.recentMessages())
            sql { db -> db.rawQuery("SELECT COUNT(*) FROM consent_receipts", null).use { it.moveToFirst(); assertEquals(0, it.getInt(0)) } }
            assertEquals(listOf("CREATED", "DELETED"), store.auditOperations())
        } finally { legacy.close(); context.deleteDatabase("jarvis_local_v2.db") }
    }
    @Test fun failedReceiptInsertionRollsBackMemoryAndJournalThenAllowsExplicitRetry() {
        val candidate = workflow.propose(session, "synthetic rollback")
        store.listOwned()
        sql { it.execSQL("CREATE TRIGGER synthetic_failure BEFORE INSERT ON consent_receipts BEGIN SELECT RAISE(ABORT,'synthetic'); END") }
        try { workflow.decide(candidate.id, session, candidate.content, MemoryDecision.ACCEPT); fail("Expected SQLite failure") }
        catch (_: android.database.sqlite.SQLiteException) { }
        assertTrue(store.listOwned().isEmpty()); assertTrue(store.auditOperations().isEmpty())
        sql { it.execSQL("DROP TRIGGER synthetic_failure") }
        assertTrue(workflow.decide(candidate.id, session, candidate.content, MemoryDecision.ACCEPT) is MemoryApprovalResult.Confirmed)
        assertEquals(1, store.listOwned().size)
    }
    @Test fun tamperedContentWithoutMatchingReceiptIsNeverReturned() {
        val record = accept("synthetic original")
        sql { it.execSQL("UPDATE confirmed_memory SET content='synthetic tampered'") }
        try { store.readOwned(record.reference.id); fail("Expected integrity refusal") }
        catch (e: MemoryRejected) { assertEquals("CORRUPT_MEMORY", e.reason) }
        assertTrue(path.exists())
    }
    @Test fun corruptFileIsPreservedAndIsolatedManualFixtureRestorationWorks() {
        val record = accept("synthetic recovery")
        store.close(); val backup = path.readBytes()
        val corrupt = "synthetic broken sqlite".repeat(200).toByteArray(); path.writeBytes(corrupt)
        store = ConfirmedMemoryStore(context)
        try { store.listOwned(); fail("Expected corruption") } catch (_: android.database.sqlite.SQLiteException) { }
        assertArrayEquals(corrupt, path.readBytes()); store.close()
        path.writeBytes(backup); store = ConfirmedMemoryStore(context)
        assertEquals(record, store.readOwned(record.reference.id))
    }
    @Test fun concurrentPendingPreferenceRequiresNewReviewInsteadOfOverwriting() {
        val first = workflow.propose(session, "synthetic one", MemoryScope.PREFERENCE, "fixture.key")
        val second = workflow.propose(session, "synthetic two", MemoryScope.PREFERENCE, "fixture.key")
        workflow.decide(first.id, session, first.content, MemoryDecision.ACCEPT)
        assertEquals(MemoryApprovalResult.Refused("PREFERENCE_CONFLICT_REVIEW_REQUIRED"), workflow.decide(second.id, session, second.content, MemoryDecision.ACCEPT))
        assertEquals("synthetic one", store.listOwned().single().content)
    }
}
