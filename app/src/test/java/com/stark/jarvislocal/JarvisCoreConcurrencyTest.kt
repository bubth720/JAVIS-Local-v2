package com.stark.jarvislocal

import android.app.Application
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 35], application = Application::class, manifest = Config.NONE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class JarvisCoreConcurrencyTest {
    private lateinit var db: LocalDatabase
    private lateinit var engine: FakeLanguageModel
    private lateinit var core: JarvisCore

    @Before fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = LocalDatabase(context)
        engine = FakeLanguageModel()
        core = JarvisCore(db, RagEngine(db), engine, SettingsStore(context))
    }
    @After fun tearDown() { db.close() }

    @Test fun wholeConversationIsExclusiveAndHistoryIsNeverMixed() = runTest {
        engine.pending = CompletableDeferred()
        val first = async { core.chat("alpha") }
        runCurrent()
        expectBusy { core.chat("beta") }
        assertEquals(listOf("alpha"), db.recentMessages().map { it.text })
        assertEquals(1, engine.prompts.size)
        engine.pending!!.complete("réponse alpha"); runCurrent()
        assertEquals("réponse alpha", first.await().text)
        engine.pending = null
        core.chat("beta")
        assertEquals(listOf("user", "assistant", "user", "assistant"), db.recentMessages().map { it.role })
        assertEquals(listOf("alpha", "réponse alpha", "beta", "réponse beta"), db.recentMessages().map { it.text })
        assertFalse(engine.prompts[0].contains("beta"))
        assertTrue(engine.prompts[1].contains("USER: alpha"))
        assertTrue(engine.prompts[1].contains("ASSISTANT: réponse alpha"))
        assertEquals(1, Regex("UTILISATEUR: beta").findAll(engine.prompts[1]).count())
    }

    @Test fun generationFailureReleasesTheConversationLock() = runTest {
        engine.fail = true
        try { core.chat("alpha"); fail("Generation should fail") } catch (e: IllegalStateException) { }
        val answer = core.chat("merci")
        assertEquals("Avec plaisir.", answer.text)
        assertEquals(listOf("alpha", "merci", "Avec plaisir."), db.recentMessages().map { it.text })
    }

    @Test fun cancellationKeepsLockUntilNativeReturnAndAddsNoErrorOrDuplicateAnswer() = runTest {
        engine.pending = CompletableDeferred()
        val first = launch { core.chat("alpha") }
        runCurrent(); first.cancel(); runCurrent()
        assertFalse(first.isCompleted)
        expectBusy { core.chat("merci") }
        assertEquals(1, db.recentMessages().size)
        engine.pending!!.complete("réponse annulée"); runCurrent()
        assertTrue(first.isCompleted)
        assertEquals(listOf("alpha"), db.recentMessages().map { it.text })
        core.chat("merci")
        assertEquals(listOf("alpha", "merci", "Avec plaisir."), db.recentMessages().map { it.text })
    }

    private suspend fun expectBusy(block: suspend () -> Unit) {
        try { block(); fail("Concurrent request must be rejected") }
        catch (e: IllegalStateException) { assertTrue(e.message.orEmpty().contains("occupé")) }
    }

    private class FakeLanguageModel : LocalLanguageModel {
        var pending: CompletableDeferred<String>? = null
        var fail = false
        val prompts = mutableListOf<String>()
        override suspend fun warmUp(settings: JarvisSettings) = Unit
        override fun isLoaded() = true
        override suspend fun generate(prompt: String, systemPrompt: String, settings: JarvisSettings): String = withContext(NonCancellable) {
            prompts += prompt
            if (fail) error("génération échouée")
            pending?.await() ?: "réponse beta"
        }
    }
}
