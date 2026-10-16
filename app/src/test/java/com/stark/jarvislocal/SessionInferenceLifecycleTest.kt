package com.stark.jarvislocal

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionInferenceLifecycleTest {
    @Test fun timeoutRequestsStopButKeepsSessionBusyAndRejectsTheLateAnswer() = runTest {
        val engine = SimulatedEngine().apply { answer = CompletableDeferred() }
        val manager = InferenceController(InferenceEngineFactory({ engine })).also { it.openSession("session") }
        val fake = FakeSessionActions().apply {
            chatBlock = { question -> ChatMessage(role = "assistant", text = manager.infer(
                InferenceRequest("session", "request", "system", question, "")).text) }
        }
        val actions = object : SessionActions by fake {
            override fun requestInferenceStop() { manager.requestStop() }
            override fun inferenceReleaseUnconfirmed() = manager.state.value.quarantined
        }
        val session = SessionCoordinator(actions, backgroundScope, StandardTestDispatcher(testScheduler), chatTimeoutMillis = 100)
        session.prepare(); runCurrent(); session.onForeground()
        assertTrue(session.send("question")); runCurrent()
        advanceTimeBy(101); runCurrent()
        assertEquals(SessionPhase.TIMEOUT, session.state.value.phase)
        assertTrue(session.state.value.busy); assertTrue(manager.state.value.stopRequested)
        assertFalse(manager.state.value.nativeDone); assertFalse(session.send("too early"))
        engine.answer!!.complete("late"); runCurrent()
        assertFalse(session.state.value.busy); assertTrue(manager.state.value.nativeDone)
        assertTrue(fake.answers.isEmpty()); assertTrue(fake.spoken.isEmpty())
        assertEquals(SessionPhase.ERROR, session.state.value.phase)
        engine.answer = null
        assertTrue(session.send("next")); runCurrent()
        assertEquals(listOf("answer"), fake.answers)
    }

    @Test fun closureDuringUnknownCompletionCannotDisplayReadyOrUnloaded() = runTest {
        val engine = SimulatedEngine().apply { answer = CompletableDeferred(); nativeDone = false }
        val manager = InferenceController(InferenceEngineFactory({ engine })).also { it.openSession("s") }
        val fake = FakeSessionActions().apply {
            chatBlock = { ChatMessage(role = "assistant", text = manager.infer(InferenceRequest("s", "r", "system", it, "")).text) }
        }
        val actions = object : SessionActions by fake {
            override fun requestInferenceStop() { manager.requestStop() }
            override fun inferenceReleaseUnconfirmed() = manager.state.value.quarantined
        }
        val session = SessionCoordinator(actions, backgroundScope, StandardTestDispatcher(testScheduler))
        session.prepare(); runCurrent(); session.send("question"); runCurrent()
        session.onScreenDestroyed(false); runCurrent()
        assertTrue(session.state.value.busy)
        engine.answer!!.complete("unattested"); runCurrent()
        assertEquals(SessionPhase.ERROR, session.state.value.phase)
        assertTrue(manager.state.value.quarantined); assertFalse(manager.state.value.nativeDone)
        assertFalse(manager.state.value.modelUnloaded); assertTrue(fake.answers.isEmpty())
    }
}
