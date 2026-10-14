package com.stark.jarvislocal

import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionCoordinatorTest {
    @Test fun cancellationBeforeFirstInstructionDoesNotLeaveGateLocked() = runTest {
        val actions = FakeSessionActions()
        val session = readySession(actions)
        session.send("jamais commencée")
        session.cancelCurrent()
        runCurrent()
        assertFalse(session.state.value.busy)
        assertTrue(actions.questions.isEmpty())
        assertTrue(actions.errors.isEmpty())
        assertTrue(session.send("reprise")); runCurrent()
        assertEquals(listOf("reprise"), actions.questions)
    }

    @Test fun rapidDoubleSendIsRejectedWithoutDuplicatingWork() = runTest {
        val actions = FakeSessionActions()
        val result = CompletableDeferred<ChatMessage>()
        actions.chatBlock = { result.await() }
        val session = readySession(actions)
        assertTrue(session.send("première"))
        assertFalse(session.send("deuxième"))
        runCurrent()
        assertEquals(listOf("première"), actions.questions)
        assertTrue(session.state.value.busy)
        result.complete(ChatMessage(role = "assistant", text = "réponse"))
        runCurrent()
        assertFalse(session.state.value.busy)
        assertEquals(1, actions.answers.size)
        assertTrue(session.send("deuxième"))
        runCurrent()
        assertEquals(listOf("première", "deuxième"), actions.questions)
    }

    @Test fun preparationIsSingleFlightAndNotRepeatedAfterRotation() = runTest {
        val actions = FakeSessionActions()
        val loaded = CompletableDeferred<Unit>()
        actions.prepareBlock = { loaded.await() }
        val session = newSession(actions)
        assertTrue(session.prepare())
        assertFalse(session.prepare())
        runCurrent()
        session.onBackground()
        session.onScreenDestroyed(changingConfigurations = true)
        session.onForeground()
        assertFalse(session.prepare())
        assertEquals(1, actions.preparations)
        loaded.complete(Unit)
        runCurrent()
        assertTrue(session.state.value.prepared)
        assertTrue(session.prepare())
        runCurrent()
        assertEquals(1, actions.preparations)
    }

    @Test fun failedPreparationDoesNotPretendToBeReadyAndCanBeRetried() = runTest {
        val actions = FakeSessionActions()
        actions.prepareBlock = { error("modèle absent") }
        val session = newSession(actions)
        session.prepare(); runCurrent()
        assertEquals(SessionPhase.ERROR, session.state.value.phase)
        assertFalse(session.state.value.prepared)
        actions.prepareBlock = { }
        session.prepare(); runCurrent()
        assertEquals(SessionPhase.READY, session.state.value.phase)
    }

    @Test fun conversationErrorReleasesGateAndReportsExactlyOneError() = runTest {
        val actions = FakeSessionActions()
        actions.chatBlock = { error("échec") }
        val session = readySession(actions)
        session.send("question"); runCurrent()
        assertFalse(session.state.value.busy)
        assertEquals(listOf("échec"), actions.errors)
        actions.chatBlock = { ChatMessage(role = "assistant", text = "OK") }
        session.send("reprise"); runCurrent()
        assertEquals(listOf("question", "reprise"), actions.questions)
        assertEquals(SessionPhase.READY, session.state.value.phase)
    }

    @Test fun timeoutKeepsGateUntilActualNativeCompletion() = runTest {
        val actions = FakeSessionActions()
        val nativeResult = CompletableDeferred<ChatMessage>()
        actions.chatBlock = { nativeResult.await() }
        val session = readySession(actions)
        session.send("lente"); runCurrent()
        advanceTimeBy(1_001); runCurrent()
        assertEquals(SessionPhase.TIMEOUT, session.state.value.phase)
        assertTrue(session.state.value.busy)
        assertFalse(session.send("dangereuse"))
        assertFalse(session.startRecording())
        nativeResult.complete(ChatMessage(role = "assistant", text = "tardive"))
        runCurrent()
        assertFalse(session.state.value.busy)
        assertEquals(listOf("lente"), actions.questions)
        assertEquals(listOf("tardive"), actions.answers)
    }

    @Test fun cancellationDrainsNativeWorkWithoutLoggingAnApplicationError() = runTest {
        val actions = FakeSessionActions()
        val nativeResult = CompletableDeferred<ChatMessage>()
        actions.chatBlock = { nativeResult.await() }
        val session = readySession(actions)
        session.send("active"); runCurrent()
        session.cancelCurrent(); runCurrent()
        assertTrue(session.state.value.busy)
        assertFalse(session.send("seconde"))
        assertTrue(actions.errors.isEmpty())
        nativeResult.complete(ChatMessage(role = "assistant", text = "fin réelle"))
        runCurrent()
        assertFalse(session.state.value.busy)
        assertTrue(actions.errors.isEmpty())
        assertTrue(session.send("reprise")); runCurrent()
    }

    @Test fun operationCancellationIsNotRecordedAsFailure() = runTest {
        val actions = FakeSessionActions()
        actions.chatBlock = { throw CancellationException("annulation normale") }
        val session = readySession(actions)
        session.send("annulée"); runCurrent()
        assertTrue(actions.errors.isEmpty())
        assertFalse(session.state.value.busy)
        assertEquals(SessionPhase.READY, session.state.value.phase)
    }

    @Test fun rotationRetainsConversationAndSharedSpeechEngine() = runTest {
        val actions = FakeSessionActions()
        val answer = CompletableDeferred<ChatMessage>()
        actions.chatBlock = { answer.await() }
        val session = readySession(actions)
        session.send("en cours"); runCurrent()
        session.onBackground()
        session.onScreenDestroyed(changingConfigurations = true)
        session.onForeground()
        assertTrue(session.state.value.busy)
        assertFalse(session.send("double"))
        assertEquals(0, actions.speechReleases)
        answer.complete(ChatMessage(role = "assistant", text = "réponse unique")); runCurrent()
        assertEquals(1, actions.questions.size)
        assertEquals(1, actions.spoken.size)
    }

    @Test fun backgroundDoesNotSpeakAndPermanentDestroyReleasesSpeech() = runTest {
        val actions = FakeSessionActions()
        val result = CompletableDeferred<ChatMessage>()
        actions.chatBlock = { result.await() }
        val session = readySession(actions)
        session.send("question"); runCurrent()
        session.onBackground()
        result.complete(ChatMessage(role = "assistant", text = "réponse")); runCurrent()
        assertTrue(actions.spoken.isEmpty())
        session.onScreenDestroyed(changingConfigurations = false)
        assertEquals(1, actions.speechReleases)
    }

    @Test fun disabledSpeechDoesNotAffectTextualConversation() = runTest {
        val actions = FakeSessionActions().apply { speechEnabled = false }
        val session = readySession(actions)
        session.send("question"); runCurrent()
        assertEquals(listOf("OK"), actions.answers)
        assertTrue(actions.spoken.isEmpty())
        assertEquals(SessionPhase.READY, session.state.value.phase)
    }

    @Test fun recordingIsExclusiveAndTranscribesOnlyAfterConfirmedStop() = runTest {
        val actions = FakeSessionActions()
        val finishedStop = CompletableDeferred<File?>()
        actions.stopBlock = { discard -> if (discard) null else finishedStop.await() }
        val session = readySession(actions)
        session.startRecording(); runCurrent()
        assertEquals(SessionPhase.LISTENING, session.state.value.phase)
        assertEquals(1, actions.speechStops)
        assertFalse(session.startRecording())
        assertFalse(session.send("concurrente"))
        assertTrue(session.stopRecording())
        assertFalse(session.stopRecording())
        runCurrent()
        assertEquals(0, actions.transcriptions)
        advanceTimeBy(1_001); runCurrent()
        assertEquals(SessionPhase.TIMEOUT, session.state.value.phase)
        assertTrue(session.state.value.busy)
        finishedStop.complete(actions.file); runCurrent()
        assertEquals(1, actions.transcriptions)
        assertEquals(listOf("texte reconnu"), actions.questions)
        assertTrue(actions.deleted.contains(actions.file))
    }

    @Test fun backgroundDiscardsMicrophoneInsteadOfTranscribingIt() = runTest {
        val actions = FakeSessionActions()
        val session = readySession(actions)
        session.startRecording(); runCurrent()
        session.onBackground(); runCurrent()
        assertEquals(0, actions.transcriptions)
        assertTrue(actions.discards > 0)
        assertFalse(session.state.value.busy)
    }

    @Test fun microphoneFailureDoesNotLeaveSessionStuck() = runTest {
        val actions = FakeSessionActions()
        val session = readySession(actions)
        session.startRecording(); runCurrent()
        actions.recording.completeExceptionally(IllegalStateException("lecture échouée"))
        runCurrent()
        assertEquals(SessionPhase.ERROR, session.state.value.phase)
        assertFalse(session.state.value.busy)
        assertEquals(0, actions.transcriptions)
        assertEquals(1, actions.errors.size)
        assertTrue(session.send("reprise")); runCurrent()
    }

    @Test fun transcriptionTimeoutBlocksNewWorkUntilNativeReturnAndDeletesAudio() = runTest {
        val actions = FakeSessionActions()
        val text = CompletableDeferred<String>()
        actions.transcribeBlock = { text.await() }
        val session = readySession(actions)
        session.startRecording(); runCurrent()
        session.stopRecording(); runCurrent()
        advanceTimeBy(1_001); runCurrent()
        assertEquals(SessionPhase.TIMEOUT, session.state.value.phase)
        assertFalse(session.send("double"))
        session.onScreenDestroyed(changingConfigurations = true)
        assertTrue(session.state.value.busy)
        text.complete("reconnue"); runCurrent()
        assertEquals(listOf("reconnue"), actions.questions)
        assertTrue(actions.deleted.contains(actions.file))
        assertFalse(session.state.value.busy)
    }

    @Test fun emptyTranscriptionIsNotSentAsConversation() = runTest {
        val actions = FakeSessionActions().apply { transcribeBlock = { "  " } }
        val session = readySession(actions)
        session.startRecording(); runCurrent()
        session.stopRecording(); runCurrent()
        assertTrue(actions.questions.isEmpty())
        assertTrue(actions.deleted.contains(actions.file))
        assertFalse(session.state.value.busy)
    }

    @Test fun pdfCannotInterleaveWithConversationHistory() = runTest {
        val actions = FakeSessionActions()
        val result = CompletableDeferred<ChatMessage>()
        actions.chatBlock = { result.await() }
        val session = readySession(actions)
        session.send("question"); runCurrent()
        assertFalse(session.importPdf("content://pdf"))
        assertEquals(0, actions.imports)
        result.complete(ChatMessage(role = "assistant", text = "OK")); runCurrent()
        assertTrue(session.importPdf("content://pdf")); runCurrent()
        assertEquals(1, actions.imports)
    }

    private fun TestScope.newSession(actions: FakeSessionActions): SessionCoordinator =
        SessionCoordinator(actions, backgroundScope, StandardTestDispatcher(testScheduler), 1_000, 1_000, 1_000)

    private fun TestScope.readySession(actions: FakeSessionActions): SessionCoordinator =
        newSession(actions).also { it.prepare(); runCurrent(); it.onForeground() }
}

internal class FakeSessionActions : SessionActions {
    var prepareBlock: suspend () -> Unit = { }
    var chatBlock: suspend (String) -> ChatMessage = { ChatMessage(role = "assistant", text = "OK") }
    var stopBlock: (suspend (Boolean) -> File?)? = null
    var transcribeBlock: suspend () -> String = { "texte reconnu" }
    var speechEnabled = true
    var preparations = 0
    var imports = 0
    var transcriptions = 0
    var discards = 0
    var speechReleases = 0
    var speechStops = 0
    val file = File("synthetic-recording.wav")
    var recording = CompletableDeferred<File>()
    val questions = mutableListOf<String>()
    val answers = mutableListOf<String>()
    val spoken = mutableListOf<String>()
    val errors = mutableListOf<String>()
    val deleted = mutableListOf<File>()
    override suspend fun prepare(progress: (String) -> Unit) { preparations++; prepareBlock() }
    override suspend fun chat(text: String): ChatMessage {
        questions += text
        return chatBlock(text).also { answers += it.text }
    }
    override suspend fun importPdf(uri: String) { imports++ }
    override suspend fun startRecording() { recording = CompletableDeferred() }
    override suspend fun awaitRecording(): File = recording.await()
    override suspend fun stopRecording(discard: Boolean): File? {
        if (discard) discards++
        stopBlock?.let { return it(discard) }
        recording.complete(file)
        return if (discard) null else file
    }
    override suspend fun transcribe(file: File): String { transcriptions++; return transcribeBlock() }
    override fun deleteRecording(file: File) { deleted += file }
    override suspend fun reportError(detail: String) { errors += detail }
    override fun speakIfEnabled(text: String) { if (speechEnabled) spoken += text }
    override fun stopSpeech() { speechStops++ }
    override fun releaseSpeech() { speechReleases++ }
    override fun initializeSpeechIfEnabled() = Unit
}
