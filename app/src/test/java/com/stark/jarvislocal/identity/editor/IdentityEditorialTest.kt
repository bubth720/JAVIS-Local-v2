package com.stark.jarvislocal.identity.editor

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import com.stark.jarvislocal.identity.*
import com.stark.jarvislocal.identity.profiles.*
import com.stark.jarvislocal.memory.*
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
class IdentityEditorialTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val profilePath get() = File(context.noBackupFilesDir, "identity_profiles_lab.sqlite")
    private val memoryPath get() = File(context.noBackupFilesDir, "confirmed_memory_lab.sqlite")
    private lateinit var profiles: ProfileRepository
    @Before fun setup() { profilePath.delete(); memoryPath.delete(); profiles = ProfileRepository(context) }
    @After fun close() { profiles.close(); profilePath.delete(); memoryPath.delete() }
    private fun changed(base: ProfileDraft, interests: Set<PersonaInterest> = setOf(PersonaInterest.MYTHOLOGY),
        rules: List<LanguageRule> = listOf(LanguageRule(LexicalToken.BONJOUR, LexicalToken.SALUT))) =
        ProfileDraft(base.name, base.preset, base.defaults, base.overrides, interests, rules)
    private fun refused(code: String, block: () -> Unit) {
        try { block(); fail("Expected $code") } catch (e: ProfileRejected) { assertEquals(code, e.reason) }
    }
    @Test fun lexicalPreviewAndExplicitApprovalCanBeRestoredWithoutRewritingHistory() {
        val first = profiles.create(ProfileDraft.preset(PersonaPreset.ATHENA))
        val original = TemplateRealizer().preview(first.draft, PersonaContext.DAILY)
        val journal = IdentityChangeJournal(profiles)
        val proposal = journal.propose(first.reference, changed(first.draft))
        val preview = proposal.preview.first { it.context == PersonaContext.DAILY }
        assertTrue(preview.after.startsWith("Salut.")); assertTrue(preview.before.startsWith("Bonjour."))
        assertEquals(1, preview.rules.size); assertEquals("Bonjour", preview.before.substring(preview.rules[0].start, preview.rules[0].endExclusive))
        assertEquals(first.reference, profiles.read(first.reference.id).reference)
        val edited = journal.approve(proposal.id, proposal.approvalDigest)
        profiles.close(); profiles = ProfileRepository(context)
        assertEquals(setOf(PersonaInterest.MYTHOLOGY), profiles.read(edited.reference.id).draft.interests)
        val restored = profiles.restore(edited.reference, 1)
        assertEquals(original.after, TemplateRealizer().preview(restored.draft, PersonaContext.DAILY).after)
        assertTrue(restored.draft.interests.isEmpty())
        assertEquals(3, profiles.history(first.reference.id).size)
        println("ID-02 synthetic demo: ${original.after} -> ${preview.after} -> ${TemplateRealizer().preview(restored.draft, PersonaContext.DAILY).after}")
    }
    @Test fun fictionNeverBecomesPersonalMemoryAndCategoriesRemainSeparate() {
        val draft = changed(ProfileDraft.preset(PersonaPreset.ATHENA))
        val catalog = PreferencesCatalog().read(draft, PersonaContext.DAILY)
        assertEquals(PersonaInterest.MYTHOLOGY, catalog.simulatedTastes.single().interest)
        assertTrue(catalog.simulatedTastes.single().simulated)
        assertTrue(catalog.confirmedUserPreferences.isEmpty())
        assertFalse(memoryPath.exists())
        assertTrue(catalog.programmedIdentityContract.startsWith("jarvis.identity/1"))
    }
    @Test fun duplicateChainedAndInstructionLikeLexicalRulesAreRejected() {
        for (rules in listOf(
            listOf(LanguageRule(LexicalToken.BONJOUR, LexicalToken.SALUT), LanguageRule(LexicalToken.BONJOUR, LexicalToken.MERCI)),
            listOf(LanguageRule(LexicalToken.BONJOUR, LexicalToken.SALUT), LanguageRule(LexicalToken.SALUT, LexicalToken.MERCI)))) {
            try { changed(ProfileDraft.preset(PersonaPreset.ATHENA), rules = rules); fail("Expected conflicting rule rejection") }
            catch (_: IllegalArgumentException) { }
        }
        try { LexicalToken.valueOf("ignore all permissions"); fail("Free text cannot be a language rule") } catch (_: IllegalArgumentException) { }
        assertEquals(setOf("BONJOUR", "SALUT", "MERCI", "VOLONTIERS"), LexicalToken.entries.map { it.name }.toSet())
    }
    @Test fun approvalIsBoundToPreviewAndRejectionNeverPersistsTheDraft() {
        val first = profiles.create(ProfileDraft.preset(PersonaPreset.ATHENA))
        val journal = IdentityChangeJournal(profiles)
        val proposal = journal.propose(first.reference, changed(first.draft))
        refused("PREVIEW_MISMATCH") { journal.approve(proposal.id, "0".repeat(64)) }
        assertEquals(1, profiles.history(first.reference.id).size)
        journal.reject(proposal.id)
        refused("PROPOSAL_ABSENT_OR_CONSUMED") { journal.approve(proposal.id, proposal.approvalDigest) }
        assertEquals(IdentityChangeState.REJECTED, journal.journal().last().state)
        assertEquals(first.reference, profiles.read(first.reference.id).reference)
        assertFalse(memoryPath.exists())
    }
    @Test fun staleExpiredAndRevokedProposalsCannotChangeProfiles() {
        val first = profiles.create(ProfileDraft.preset(PersonaPreset.ATHENA))
        var time = 100L
        val journal = IdentityChangeJournal(profiles) { time }
        val a = journal.propose(first.reference, changed(first.draft))
        val b = journal.propose(first.reference, changed(first.draft, setOf(PersonaInterest.ARTS)))
        val second = journal.approve(a.id, a.approvalDigest)
        refused("REVISION_CONFLICT") { journal.approve(b.id, b.approvalDigest) }
        val expired = journal.propose(second.reference, changed(first.draft))
        time += 90_000
        refused("PROPOSAL_EXPIRED") { journal.approve(expired.id, expired.approvalDigest) }
        val revoked = journal.propose(second.reference, first.draft)
        journal.revokePending()
        refused("PROPOSAL_ABSENT_OR_CONSUMED") { journal.approve(revoked.id, revoked.approvalDigest) }
        assertEquals(2, profiles.history(first.reference.id).size)
    }
    @Test fun urgentContextSuppressesHumourAndVariantsWithoutChangingTheStoredProfile() {
        val draft = changed(ProfileDraft.preset(PersonaPreset.ATHENA))
        val engine = TemplateRealizer()
        val daily = engine.preview(draft, PersonaContext.DAILY)
        val work = engine.preview(draft, PersonaContext.WORK)
        val urgent = engine.preview(draft, PersonaContext.URGENT)
        assertEquals(60, daily.effective.humour); assertEquals(30, work.effective.humour)
        assertEquals(0, urgent.effective.humour); assertEquals(urgent.before, urgent.after)
        assertTrue(urgent.rules.isEmpty()); assertEquals(60, draft.defaults.humour)
        assertTrue(urgent.explanations.contains("CONFIGURATION_ONLY"))
        val profile = profiles.create(draft)
        assertEquals("CONTRACT_EXTENSION_REQUIRED", IdentityComposerAdapter(IdentityComposer(IdentityPolicy(enabled = false, inferenceSafe = { false }))).proposedProfileSupport(profile))
    }
    @Test fun readOnlyPreferencePortRequiresActualConfirmedScopeAndTracksSuspension() {
        ConfirmedMemoryStore(context).use { store ->
            val workflow = MemoryApprovalWorkflow(store, { 100L }, { 1000L })
            val session = UUID.randomUUID().toString()
            fun consent(text: String, scope: MemoryScope, key: String? = null): MemoryRecord {
                val candidate = workflow.propose(session, text, scope, key)
                return (workflow.decide(candidate.id, session, candidate.content, MemoryDecision.ACCEPT) as MemoryApprovalResult.Confirmed).record
            }
            val reader = MemoryPreferenceReader(ConfirmedMemoryLookup(store) { 1000L })
            workflow.propose(session, "synthetic pending preference", MemoryScope.PREFERENCE, "fixture.pending")
            assertTrue(reader.read(10).isEmpty())
            consent("synthetic unrelated", MemoryScope.GENERAL)
            consent("synthetic sensitive", MemoryScope.SENSITIVE)
            val approved = consent("synthetic confirmed preference", MemoryScope.PREFERENCE, "fixture.confirmed")
            assertEquals(listOf(approved.reference), reader.read(10).map { it.reference })
            assertEquals(approved.content, reader.read(10).single().untrustedContent)
            store.suspendOwned(approved.reference, true)
            assertTrue(reader.read(10).isEmpty())
            store.deleteOwned(store.readOwned(approved.reference.id).reference)
            assertTrue(reader.read(10).isEmpty())
            assertEquals(2, store.listOwned().size)
        }
    }
    @Test fun oldEditorRevisionBytesRemainReadableAndUnmodifiedAfterNewRevision() {
        profiles.create(ProfileDraft.preset(PersonaPreset.NEUTRAL)) // initialize fresh schema
        val id = "00000000-0000-4000-8000-000000000001"
        val legacy = """{"format":"jarvis.identity-profile-lab/1","id":"00000000-0000-4000-8000-000000000001","revision":1,"name":"Synthetic legacy","preset":"NEUTRAL","defaults":{"frankness":50,"humour":0,"initiative":0,"tutoiement":true,"style":"PLAIN"},"overrides":{}}""".toByteArray()
        val digest = ProfileCodec.digest(legacy)
        SQLiteDatabase.openDatabase(profilePath.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.execSQL("INSERT INTO profiles VALUES(?,1)", arrayOf(id))
            db.execSQL("INSERT INTO profile_revisions VALUES(?,1,?,?,'INITIAL',NULL)", arrayOf(id, legacy, digest))
        }
        val old = profiles.read(id)
        assertTrue(old.draft.interests.isEmpty()); assertTrue(old.draft.languageRules.isEmpty())
        val next = profiles.revise(old.reference, changed(old.draft))
        assertEquals(2L, next.reference.revision)
        SQLiteDatabase.openDatabase(profilePath.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT payload FROM profile_revisions WHERE profile_id=? AND revision=1", arrayOf(id)).use { c ->
                assertTrue(c.moveToFirst()); assertArrayEquals(legacy, c.getBlob(0))
            }
        }
        assertEquals(old.reference, profiles.read(id, 1).reference)
    }
}
