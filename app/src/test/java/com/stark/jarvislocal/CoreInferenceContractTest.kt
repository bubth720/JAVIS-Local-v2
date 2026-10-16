package com.stark.jarvislocal

import android.app.Application
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 25, 35], application = Application::class, manifest = Config.NONE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class CoreInferenceContractTest {
    @Test fun corePassesSystemQuestionContextSourceIdsAndLimitsSeparatelyWithoutChangingData() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val settings = SettingsStore(context)
        settings.save(JarvisSettings(systemPrompt = "SYSTEME PERSONNEL DISTINCT", maxTokens = 128))
        val db = LocalDatabase(context)
        try {
            db.addMemory("atlas souvenir pertinent")
            db.addDocument("Atlas.pdf", "application/pdf", listOf("[PAGE 2] atlas preuve synthétique"))
            db.addMessage(ChatMessage(role = "user", text = "historique synthétique"))
            val beforeMemories = db.allMemories(); val beforeChunks = db.allChunks()
            var captured: InferenceRequest? = null
            val backend = object : LocalLanguageModel {
                override suspend fun warmUp(settings: JarvisSettings) = Unit
                override fun isLoaded() = true
                override suspend fun generate(prompt: String, systemPrompt: String, settings: JarvisSettings): String = error("Structured path required")
                override suspend fun infer(request: InferenceRequest): InferenceResponse {
                    captured = request
                    return InferenceResponse(request.sessionId, request.requestId, "réponse structurée")
                }
            }
            val core = JarvisCore(db, RagEngine(db), backend, settings)
            val answer = core.chat("résume atlas")
            val request = requireNotNull(captured)
            assertEquals("SYSTEME PERSONNEL DISTINCT", request.systemPrompt)
            assertEquals("résume atlas", request.question)
            assertFalse(request.context.contains(request.systemPrompt)); assertFalse(request.context.contains("UTILISATEUR: résume atlas"))
            assertTrue(request.context.contains("historique synthétique")); assertTrue(request.context.contains("atlas souvenir pertinent"))
            assertEquals(listOf(InferenceSource(beforeChunks.single().documentId, beforeChunks.single().id, "Atlas.pdf", 0)), request.sources)
            assertEquals(GenerationLimits(2048, 128), request.limits)
            assertTrue(request.sessionId.isNotBlank()); assertTrue(request.requestId.isNotBlank())
            assertEquals("Atlas.pdf", answer.source)
            assertEquals(beforeMemories, db.allMemories()); assertEquals(beforeChunks, db.allChunks())
            assertEquals("réponse structurée", db.recentMessages().last().text)
        } finally { db.close() }
    }

    @Test fun mismatchedOrUnattestedResponseNeverEntersHistory() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val db = LocalDatabase(context)
        try {
            var responseType = 0
            val backend = object : LocalLanguageModel {
                override suspend fun warmUp(settings: JarvisSettings) = Unit
                override fun isLoaded() = true
                override suspend fun generate(prompt: String, systemPrompt: String, settings: JarvisSettings) = "unused"
                override suspend fun infer(request: InferenceRequest) = when (responseType) {
                    0 -> InferenceResponse("old-session", request.requestId, "stale")
                    1 -> InferenceResponse(request.sessionId, "old-request", "stale")
                    else -> InferenceResponse(request.sessionId, request.requestId, "unknown", nativeDone = false)
                }
            }
            val core = JarvisCore(db, RagEngine(db), backend, SettingsStore(context))
            repeat(3) {
                responseType = it
                assertTrue(runCatching { core.chat("résume atlas") }.exceptionOrNull() is InferenceRejected)
            }
            assertEquals(listOf("user", "user", "user"), db.recentMessages().map { it.role })
            assertEquals("Avec plaisir.", core.chat("merci").text)
        } finally { db.close() }
    }
}
