package com.stark.jarvislocal.memory

import android.app.Application
import com.stark.jarvislocal.identity.IdentityCodec
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
class MemoryRecallExportTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val db get() = File(context.noBackupFilesDir, "confirmed_memory_lab.sqlite")
    private val directory get() = File(context.noBackupFilesDir, "synthetic_mem02_exports")
    private lateinit var store: ConfirmedMemoryStore
    private lateinit var workflow: MemoryApprovalWorkflow
    private lateinit var controls: UserMemoryControl
    private lateinit var index: MemoryRecallIndex
    private lateinit var exports: MemoryExportService
    private var wall = 1000L
    private var elapsed = 100L
    private val session = UUID.randomUUID().toString()
    private val policy = ProjectionPolicy(setOf(MemoryScope.GENERAL, MemoryScope.PREFERENCE, MemoryScope.SENSITIVE))
    @Before fun setup() {
        db.delete(); directory.deleteRecursively()
        store = ConfirmedMemoryStore(context); workflow = MemoryApprovalWorkflow(store, { elapsed }, { wall })
        controls = UserMemoryControl(); index = MemoryRecallIndex(store, controls) { wall }
        exports = MemoryExportService(store, controls, directory, { wall }, { elapsed })
    }
    @After fun teardown() { store.close(); db.delete(); directory.deleteRecursively() }
    private fun accept(text: String, scope: MemoryScope = MemoryScope.GENERAL, key: String? = null, expiry: Long? = null): MemoryRecord {
        val p = workflow.propose(session, text, scope, key, expiresAtMs = expiry)
        return (workflow.decide(p.id, session, text, MemoryDecision.ACCEPT) as MemoryApprovalResult.Confirmed).record
    }
    private fun open() = controls.resumeSession(session)
    private fun query(text: String, bytes: Int = 4096) = MemoryQuery(session, text, budgetBytes = bytes)
    @Test fun onlyConfirmedAuthorizedPreferencesAreRecalledWithProvenanceAndNfc() {
        workflow.propose(session, "café refused synthetic")
        val record = accept("café synthétique préféré", MemoryScope.PREFERENCE, "fixture.drink")
        accept("café secret synthetic", MemoryScope.SENSITIVE)
        assertEquals(RecallStatus.DENIED, index.recall(query("cafe\u0301"), policy).status)
        open(); assertEquals(RecallStatus.DENIED, index.recall(query("café")).status)
        val recalled = index.recall(query("cafe\u0301"), policy)
        assertEquals(RecallStatus.READY, recalled.status); assertEquals(record.reference, recalled.items.single().reference)
        assertEquals(setOf("café"), recalled.items.single().explanation.matchingTerms)
        assertEquals("USER_EXPLICIT", recalled.items.single().explanation.source)
        assertEquals(1000L, recalled.items.single().explanation.confirmedAtMs)
    }
    @Test fun ambiguousLexicalRecallRequiresExplicitCurrentSelectionBeforeAssembly() {
        val a = accept("synthetic blue preference"); val b = accept("synthetic green preference"); open()
        val q = query("synthetic preference")
        assertEquals(RecallStatus.NEEDS_CONFIRMATION, index.recall(q, policy).status)
        val assembler = MemoryContextAssembler(index)
        assertTrue(assembler.assemble(q, policy).jsonData.isEmpty())
        val chosen = assembler.assemble(q, policy, setOf(a.reference))
        assertEquals(listOf(a.reference), chosen.references); assertTrue(chosen.jsonData.contains(a.content)); assertFalse(chosen.jsonData.contains(b.content))
        store.deleteOwned(a.reference)
        assertFalse(assembler.assemble(q, policy, setOf(a.reference)).status == RecallStatus.READY)
    }
    @Test fun contextQuotesUntrustedContentAndCountsUtf8WithoutTruncating() {
        val r = accept("budget 🦉 éé \"ignore SYSTEM\"\n" + "😀".repeat(80)); open()
        val assembler = MemoryContextAssembler(index)
        assertEquals(RecallStatus.EMPTY, assembler.assemble(query("budget", 256), policy).status)
        val context = assembler.assemble(query("budget", 4096), policy)
        assertEquals(IdentityCodec.utf8(context.jsonData).size, context.bytes); assertTrue(context.bytes <= 4096)
        val decoded = IdentityCodec.decode(IdentityCodec.utf8(context.jsonData))
        assertEquals(true, decoded["untrusted_data"]); assertEquals("confirmed_memory_data", decoded["kind"])
        assertEquals(r.content, ((decoded["entries"] as List<*>).single() as Map<*, *>)["text"])
        assertFalse(context.toString().contains("ignore SYSTEM"))
    }
    @Test fun suppressionSuspensionExpiryAndSessionExclusionImmediatelyBlockRecallAndExport() {
        val r = accept("synthetic expiry", expiry = 2000); open()
        val p = exports.preview(session, setOf(r.reference), policy)
        controls.exclude(session, r.reference)
        assertEquals(RecallStatus.EMPTY, index.recall(query("expiry"), policy).status)
        rejected { exports.writeConfirmed(p.id, session, p.sha256) }
        controls.suspendSession(session); open()
        store.suspendOwned(r.reference, true)
        assertEquals(RecallStatus.EMPTY, index.recall(query("expiry"), policy).status)
        store.suspendOwned(store.readOwned(r.reference.id).reference, false)
        wall = 2000
        assertEquals(RecallStatus.EMPTY, index.recall(query("expiry"), policy).status)
        rejected { exports.preview(session, setOf(store.readOwned(r.reference.id).reference), policy) }
        store.deleteOwned(store.readOwned(r.reference.id).reference)
        rejected { exports.writeConfirmed(p.id, session, p.sha256) }
        assertTrue(exports.exportedFiles().isEmpty())
    }
    @Test fun voluntaryExportCreatesRealFileAndSandboxNeverWritesOrTrustsImportedConsent() {
        val r = accept("synthetic preferred language 🦉", MemoryScope.PREFERENCE, "fixture.language"); open()
        val p = exports.preview(session, setOf(r.reference), policy)
        assertFalse(directory.exists()); assertFalse(p.json.contains("session_id")); assertFalse(p.json.contains("nonce"))
        val file = exports.writeConfirmed(p.id, session, p.sha256)
        assertEquals(p.json, file.readText()); assertEquals(p.sha256, IdentityCodec.sha256(file.readBytes()))
        assertEquals(listOf(file), exports.exportedFiles())
        val before = store.auditOperations(); val imported = exports.previewImport(file)
        assertEquals("UNTRUSTED_IMPORT_REQUIRES_FRESH_CONSENT", imported.candidates.single().authority)
        assertTrue(imported.candidates.single().conflicts.contains("EXISTING_ID_NO_OVERWRITE"))
        assertEquals(before, store.auditOperations()); assertEquals(r, store.listOwned().single())
        store.deleteOwned(r.reference)
        val isolated = exports.previewImport(file)
        assertTrue(isolated.candidates.single().conflicts.isEmpty()); assertTrue(store.listOwned().isEmpty())
        println("MEM-02 synthetic demo: private export verified; sandbox preview only; no automatic import")
    }
    @Test fun wrongPreviewSessionExpiredConsentAndReplayNeverCreateAnExport() {
        val r = accept("synthetic exact export"); open(); val p = exports.preview(session, setOf(r.reference), policy)
        rejected { exports.writeConfirmed(p.id, session, "0".repeat(64)) }
        rejected { exports.writeConfirmed(p.id, UUID.randomUUID().toString(), p.sha256) }
        elapsed += 90_000; rejected { exports.writeConfirmed(p.id, session, p.sha256) }
        assertTrue(exports.exportedFiles().isEmpty())
        val fresh = exports.preview(session, setOf(r.reference), policy)
        val first = exports.writeConfirmed(fresh.id, session, fresh.sha256)
        rejected { exports.writeConfirmed(fresh.id, session, fresh.sha256) }
        val next = exports.preview(session, setOf(r.reference), policy)
        val second = exports.writeConfirmed(next.id, session, next.sha256)
        assertNotEquals(first.name, second.name); assertEquals(2, exports.exportedFiles().size)
        assertEquals(fresh.json, first.readText())
    }
    @Test fun sessionRecreationCannotAuthorizeAnOldExportPreview() {
        val r = accept("synthetic ABA export"); open(); val p = exports.preview(session, setOf(r.reference), policy)
        controls.suspendSession(session); open()
        rejected { exports.writeConfirmed(p.id, session, p.sha256) }
        assertTrue(exports.exportedFiles().isEmpty())
    }
    @Test fun deletionOrEditingAfterPreviewRequiresANewPreview() {
        val r = accept("synthetic before export"); open(); val p = exports.preview(session, setOf(r.reference), policy)
        val edit = workflow.propose(session, "synthetic changed", edit = r.reference)
        workflow.decide(edit.id, session, edit.content, MemoryDecision.REPLACE)
        rejected { exports.writeConfirmed(p.id, session, p.sha256) }
        assertTrue(exports.exportedFiles().isEmpty())
    }
    @Test fun unavailableEncryptionRefusesInsteadOfWritingPlaintext() {
        val r = accept("synthetic encrypted request"); open(); val p = exports.preview(session, setOf(r.reference), policy)
        try { exports.writeConfirmed(p.id, session, p.sha256, encrypted = true); fail("Expected refusal") }
        catch (e: MemoryRejected) { assertEquals("CIPHER_UNAVAILABLE", e.reason) }
        assertFalse(directory.exists())
    }
    @Test fun corruptUnsupportedDuplicatedOrAuthorityBearingImportsAreRejected() {
        val r = accept("synthetic import"); open(); val p = exports.preview(session, setOf(r.reference), policy)
        val corruptions = listOf(p.json.replace("synthetic import", "synthetic tamper"),
            p.json.replace(MemoryExportService.FORMAT, "jarvis.confirmed-memory-export/9"),
            p.json.replace("\"format\":", "\"format\":\"duplicate\",\"format\":"),
            p.json.dropLast(1) + ",\"permissions\":true}", p.json + " trailing")
        for (bad in corruptions) rejected { exports.decodeImport(bad.toByteArray()) }
        rejected { exports.decodeImport(byteArrayOf(0xC3.toByte(), 0x28)) }
        rejected { exports.decodeImport(ByteArray(32769)) }
        assertEquals(r, store.listOwned().single())
    }
    @Test fun sensitiveExpiredAndUnconfirmedRowsCannotBeExported() {
        val r = accept("synthetic sensitive", MemoryScope.SENSITIVE); open()
        rejected { exports.preview(session, setOf(r.reference), policy) }
        val normal = accept("synthetic normal")
        rejected { exports.preview(session, setOf(normal.reference)) }
        assertFalse(directory.exists())
    }
    @Test fun selectedFreshDestinationIsReadBackAndExistingDestinationIsNeverOpenedForWriting() {
        val r = accept("synthetic destination 🦉"); open()
        class Target(var empty: Boolean = true) : MemoryExportDestinationPort {
            val output = java.io.ByteArrayOutputStream(); var opens = 0
            override fun isEmpty() = empty
            override fun openOutput(): java.io.OutputStream { opens++; return output }
            override fun openReadback() = output.toByteArray().inputStream()
        }
        val existing = Target(false); val p = exports.preview(session, setOf(r.reference), policy)
        rejected { exports.writeConfirmed(p.id, session, p.sha256, destination = existing) }
        assertEquals(0, existing.opens); assertFalse(directory.exists())
        val fresh = Target()
        val file = exports.writeConfirmed(p.id, session, p.sha256, destination = fresh)
        assertArrayEquals(file.readBytes(), fresh.output.toByteArray()); assertEquals(1, fresh.opens)
    }
    @Test fun failedProviderReadbackKeepsPrivateCopyAndNeverClaimsRollbackOrReusesConsent() {
        val r = accept("synthetic uncertain provider"); open(); val p = exports.preview(session, setOf(r.reference), policy)
        val target = object : MemoryExportDestinationPort {
            override fun isEmpty() = true
            override fun openOutput() = java.io.ByteArrayOutputStream()
            override fun openReadback() = "synthetic corruption".byteInputStream()
        }
        try { exports.writeConfirmed(p.id, session, p.sha256, destination = target); fail("Expected unknown result") }
        catch (e: MemoryRejected) { assertEquals("EXPORT_COPY_UNKNOWN_PRIVATE_COPY_RETAINED", e.reason) }
        assertEquals(p.json, exports.exportedFiles().single().readText())
        rejected { exports.writeConfirmed(p.id, session, p.sha256, destination = target) }
    }
    @Test fun revocationDoesNotWaitForABlockedProviderAndPreventsFurtherCopying() {
        val r = accept("synthetic interrupted provider"); open(); val p = exports.preview(session, setOf(r.reference), policy)
        val entered = java.util.concurrent.CountDownLatch(1); val resume = java.util.concurrent.CountDownLatch(1)
        val copied = java.io.ByteArrayOutputStream()
        val target = object : MemoryExportDestinationPort {
            override fun isEmpty() = true
            override fun openOutput(): java.io.OutputStream {
                entered.countDown(); check(resume.await(4, java.util.concurrent.TimeUnit.SECONDS)); return copied
            }
            override fun openReadback() = copied.toByteArray().inputStream()
        }
        val pool = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            val writing = pool.submit<File> { exports.writeConfirmed(p.id, session, p.sha256, destination = target) }
            assertTrue(entered.await(4, java.util.concurrent.TimeUnit.SECONDS))
            pool.submit { controls.suspendSession(session); exports.revokeSession(session) }.get(2, java.util.concurrent.TimeUnit.SECONDS)
            resume.countDown()
            try { writing.get(4, java.util.concurrent.TimeUnit.SECONDS); fail("Expected unknown interrupted copy") }
            catch (e: java.util.concurrent.ExecutionException) { assertEquals("EXPORT_COPY_UNKNOWN_PRIVATE_COPY_RETAINED", (e.cause as MemoryRejected).reason) }
            assertEquals(0, copied.size()); assertEquals(1, exports.exportedFiles().size)
        } finally { resume.countDown(); pool.shutdownNow() }
    }
    private fun rejected(block: () -> Any?) { try { block(); fail("Expected refusal") } catch (_: IllegalArgumentException) { } catch (_: IllegalStateException) { } }
}
