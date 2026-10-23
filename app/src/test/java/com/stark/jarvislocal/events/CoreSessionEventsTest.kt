package com.stark.jarvislocal.events

import android.app.Application
import com.stark.jarvislocal.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 25, 35], application = Application::class)
class CoreSessionEventsTest {
    private open class Actions : SessionActions {
        override suspend fun prepare(progress: (String) -> Unit) { }
        override suspend fun chat(text: String) = ChatMessage(role = "assistant", text = text)
        override suspend fun importPdf(uri: String) { }
        override suspend fun startRecording() { error("Unused") }
        override suspend fun awaitRecording(): File = error("Unused")
        override suspend fun stopRecording(discard: Boolean): File? = null
        override suspend fun transcribe(file: File) = error("Unused")
        override fun deleteRecording(file: File) { }
        override suspend fun reportError(detail: String) { }
        override fun speakIfEnabled(text: String) { error("No voice requested") }
        override fun stopSpeech() { }
        override fun releaseSpeech() { }
        override fun initializeSpeechIfEnabled() { }
    }
    @Test fun realCoreDocumentCommandEmitsCorrelatedMetadataWithoutAnyNativeCall() = runTest {
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("jarvis_local_v2.db")
        val db = LocalDatabase(context)
        try {
            db.addDocument("fixture.txt", "text/plain", listOf("synthetic content"))
            val model = object : LocalLanguageModel {
                override suspend fun warmUp(settings: JarvisSettings) { error("No model") }
                override suspend fun generate(prompt: String, systemPrompt: String, settings: JarvisSettings): String = error("No model")
                override fun isLoaded() = false
            }
            val core = JarvisCore(db, RagEngine(db), model, SettingsStore(context))
            val actions = object : Actions() { override suspend fun chat(text: String) = core.chat(text) }
            val bus = CoreEventBus({ 100 }); bus.setEnabled(true)
            val coordinator = SessionCoordinator(actions, this, StandardTestDispatcher(testScheduler),
                conversationEvents = ConversationEventPublisher(bus))
            assertTrue(coordinator.send("/docs")); runCurrent()
            assertFalse(coordinator.state.value.busy)
            assertTrue(db.recentMessages().last().text.contains("fixture.txt"))
            assertEquals(listOf("USER_INPUT", "THINKING", "RESPONSE_COMPLETED"), bus.state.value.events.map { it.envelope.event.kind })
            assertEquals(1, bus.state.value.events.map { it.envelope.requestId }.distinct().size)
            assertEquals(listOf(1L, 2L, 3L), bus.state.value.events.map { it.sequence })
            assertFalse(bus.state.value.events.toString().contains("fixture.txt"))
        } finally { db.close(); context.deleteDatabase("jarvis_local_v2.db") }
    }
    @Test fun cancelRetainsExistingSessionGateUntilWorkerReallyReturns() = runTest {
        val pending = CompletableDeferred<Unit>()
        val actions = object : Actions() {
            override suspend fun chat(text: String): ChatMessage { pending.await(); return ChatMessage(role = "assistant", text = "late") }
        }
        val bus = CoreEventBus({ 100 }); bus.setEnabled(true)
        val coordinator = SessionCoordinator(actions, this, StandardTestDispatcher(testScheduler),
            conversationEvents = ConversationEventPublisher(bus))
        coordinator.send("synthetic"); runCurrent(); coordinator.cancelCurrent(); runCurrent()
        assertTrue(coordinator.state.value.busy)
        assertFalse(coordinator.send("another"))
        assertEquals("CANCEL_REQUESTED", bus.state.value.events.last().envelope.event.kind)
        pending.complete(Unit); runCurrent()
        assertFalse(coordinator.state.value.busy)
        assertEquals("CANCELLED", bus.state.value.events.last().envelope.event.kind)
        assertFalse(bus.state.value.events.any { it.envelope.event is CoreEvent.ResponseCompleted })
    }
    @Test fun disabledObserverLeavesHistoricalChatPathAvailable() = runTest {
        val bus = CoreEventBus({ 100 })
        val coordinator = SessionCoordinator(Actions(), this, StandardTestDispatcher(testScheduler),
            conversationEvents = ConversationEventPublisher(bus))
        assertTrue(coordinator.send("synthetic")); runCurrent()
        assertTrue(bus.state.value.events.isEmpty()); assertFalse(coordinator.state.value.busy)
    }
}
