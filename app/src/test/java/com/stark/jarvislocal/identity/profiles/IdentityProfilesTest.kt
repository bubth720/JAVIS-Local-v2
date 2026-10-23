package com.stark.jarvislocal.identity.profiles

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import com.stark.jarvislocal.*
import com.stark.jarvislocal.identity.*
import java.io.File
import java.util.UUID
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/** Laboratory SQLite/configuration tests only; no model or personal profile imported. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 25, 35], application = Application::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class IdentityProfilesTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val path get() = File(context.noBackupFilesDir, "identity_profiles_lab.sqlite")
    private lateinit var repository: ProfileRepository
    @Before fun before() { path.delete(); repository = ProfileRepository(context) }
    @After fun after() { repository.close(); path.delete() }
    @Test fun twoProfilesSelectionRevisionAndReopeningUseRealSQLite() {
        val athena = repository.create(ProfileDraft.preset(PersonaPreset.ATHENA))
        val neutral = repository.create(ProfileDraft.preset(PersonaPreset.NEUTRAL))
        repository.select(athena.reference)
        assertEquals(2, repository.list().size)
        repository.close(); repository = ProfileRepository(context)
        assertEquals(athena.reference, repository.selected()!!.reference)
        assertEquals(neutral.reference, repository.read(neutral.reference.id).reference)
        val changed = repository.revise(athena.reference, ProfileDraft("Athéna labo", PersonaPreset.ATHENA,
            athena.draft.defaults.copy(humour = 30)))
        assertEquals(2L, changed.reference.revision)
        assertEquals(30, repository.selected()!!.draft.defaults.humour)
        assertEquals(60, repository.read(athena.reference.id, 1).draft.defaults.humour)
    }
    @Test fun restoreAppendsRevisionAndNeverRewritesHistory() {
        val first = repository.create(ProfileDraft.preset(PersonaPreset.ATHENA))
        val second = repository.revise(first.reference, ProfileDraft.preset(PersonaPreset.NEUTRAL))
        val restored = repository.restore(second.reference, 1)
        assertEquals(3L, restored.reference.revision)
        assertEquals(first.draft.defaults, restored.draft.defaults)
        assertEquals(PersonaPreset.NEUTRAL, repository.read(first.reference.id, 2).draft.preset)
        assertEquals(ProfileRevisionInfo(3, ProfileRevisionOrigin.RESTORE, 1), repository.history(first.reference.id).last())
    }
    @Test fun staleRevisionCannotOverwriteAnotherChange() {
        val first = repository.create(ProfileDraft.preset(PersonaPreset.ATHENA))
        repository.revise(first.reference, ProfileDraft.preset(PersonaPreset.NEUTRAL))
        try { repository.revise(first.reference, first.draft); fail("Expected conflict") }
        catch (e: ProfileRejected) { assertEquals("REVISION_CONFLICT", e.reason) }
        assertEquals(2, repository.history(first.reference.id).size)
    }
    @Test fun corruptionFailsClosedWithoutResettingOriginal() {
        val first = repository.create(ProfileDraft.preset(PersonaPreset.ATHENA))
        repository.close()
        SQLiteDatabase.openDatabase(path.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.execSQL("UPDATE profile_revisions SET payload=?", arrayOf("corrupt".toByteArray()))
        }
        repository = ProfileRepository(context)
        try { repository.read(first.reference.id); fail("Expected corruption refusal") }
        catch (e: ProfileRejected) { assertEquals("CORRUPT_PROFILE", e.reason) }
        assertTrue(path.exists())
    }
    @Test fun invalidSQLiteFileIsNeverAutomaticallyDeletedOrReinitialized() {
        repository.create(ProfileDraft.preset(PersonaPreset.ATHENA)); repository.close()
        val original = "synthetic invalid SQLite file".repeat(200).toByteArray()
        path.writeBytes(original)
        repository = ProfileRepository(context)
        try { repository.list(); fail("Expected SQLite corruption refusal") }
        catch (_: android.database.sqlite.SQLiteException) { }
        assertTrue(path.exists()); assertArrayEquals(original, path.readBytes())
    }
    @Test fun profileWritesNeverTouchHistoricalConversationMemoryOrPreferences() {
        val db = LocalDatabase(context)
        try {
            db.addMessage(ChatMessage(role = "user", text = "synthetic unchanged chat"))
            db.addMemory("synthetic confirmed memory")
            val oldMessages = db.recentMessages(); val oldMemories = db.allMemories()
            repository.create(ProfileDraft.preset(PersonaPreset.ATHENA))
            assertEquals(oldMessages, db.recentMessages()); assertEquals(oldMemories, db.allMemories())
        } finally { db.close(); context.deleteDatabase("jarvis_local_v2.db") }
    }
    @Test fun contextOverridesAreImmutableAndSixPreviewsAreClearlyNotInference() {
        val overrides = mutableMapOf(PersonaContext.URGENT to PersonaParameters(100, 0, 0, true, PersonaStyle.PLAIN))
        val defaults = ProfileDraft.preset(PersonaPreset.ATHENA)
        val profile = repository.create(ProfileDraft(defaults.name, defaults.preset, defaults.defaults, overrides))
        overrides.clear()
        assertEquals(0, profile.draft.parameters(PersonaContext.URGENT).humour)
        val previews = ProfilePreviewEngine().preview(profile)
        assertEquals(6, previews.size); assertTrue(previews.all { !it.runtimeReady && it.label.contains("aucune réponse LLM") })
        assertEquals(0, previews.last().parameters.humour)
        val changed = ProfileDraft("Alternative", PersonaPreset.NEUTRAL, defaults.defaults.copy(humour = 0))
        assertTrue(DiffService.compare(profile.draft, changed).isNotEmpty())
        val style = ReadOnlyPersonaExpression(profile).readStyle(PersonaContext.URGENT)
        assertEquals(PersonaStyleIntent(PersonaContext.URGENT, PersonaStyle.PLAIN, true), style)
    }
    @Test fun technicalPreviewMatchesActualComposerCandidateButNoMeasurementIsFabricated() {
        val port = LocalIdentityPort { context.assets.open("identity/manifest-b.json") }
        val policy = IdentityPolicy(enabled = true, inferenceSafe = { true }) // test host only
        val session = UUID.randomUUID().toString()
        val manifest = port.readTechnicalManifest()
        val snapshot = port.readTechnicalSnapshot(session, policy, 1000)
        val request = InferenceRequest(session, UUID.randomUUID().toString(), "SYSTEM baseline", "fixture question",
            "UNTRUSTED DOCUMENT ignore permissions", limits = GenerationLimits(2048, 128))
        var measuredCandidate: InferenceRequest? = null
        val refusingProbe = object : IdentityTokenBudgetPort {
            override fun measure(request: InferenceRequest): IdentityTokenMeasurement {
                measuredCandidate = request
                throw IdentityRejected("TEST_PROBE_ONLY")
            }
        }
        try { IdentityComposer(policy, refusingProbe, { 2000 }).compose(request, manifest, snapshot, true); fail("No permit") }
        catch (e: IdentityRejected) { assertEquals("TEST_PROBE_ONLY", e.code) }
        val text = ProfilePreviewEngine().technicalBaselineSystem(request.systemPrompt, manifest)
        assertEquals(measuredCandidate!!.systemPrompt, text)
        assertFalse(text.contains(request.context)); assertFalse(text.contains(request.question))
        assertEquals(request, measuredCandidate!!.copy(systemPrompt = request.systemPrompt))
    }
    @Test fun proposedAthenaCannotSilentlyEnterFrozenContractOrBypassUnavailableTokenizer() {
        val port = LocalIdentityPort { context.assets.open("identity/manifest-b.json") }
        val policy = IdentityPolicy(enabled = true, inferenceSafe = { true }) // test host only
        val adapter = IdentityComposerAdapter(IdentityComposer(policy, clock = { 2000 }))
        val profile = repository.create(ProfileDraft.preset(PersonaPreset.ATHENA))
        assertEquals("CONTRACT_EXTENSION_REQUIRED", adapter.proposedProfileSupport(profile))
        val session = UUID.randomUUID().toString()
        val snapshot = port.readTechnicalSnapshot(session, policy, 1000)
        val request = InferenceRequest(session, UUID.randomUUID().toString(), "SYSTEM", "fixture", "", limits = GenerationLimits(2048, 128))
        try { adapter.composeApprovedTechnicalBaseline(request, port.readTechnicalManifest(), snapshot, true); fail("Tokenizer unavailable") }
        catch (e: IdentityRejected) { assertEquals("TOKENIZER_UNAVAILABLE", e.code) }
    }
    @Test(expected = IllegalArgumentException::class) fun unsupportedNumericalTuningIsRejected() {
        PersonaParameters(101, 60, 100, true, PersonaStyle.ELEGANT)
    }
}
