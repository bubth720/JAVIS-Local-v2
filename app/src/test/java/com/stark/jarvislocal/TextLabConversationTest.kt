package com.stark.jarvislocal

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/** Synthetic responses for state tests only; not an LLM qualification. */
@OptIn(ExperimentalCoroutinesApi::class)
class TextLabConversationTest {
    @Test fun requiresExplicitOptInAndErasesHistoryAtExit() = runTest {
        var calls = 0
        val lab = TextLabConversation { _, _ -> calls++; ChatMessage(role="assistant",text="synthetic") }
        assertFalse(lab.active)
        assertTrue(runCatching { lab.chat("public fixture") }.isFailure)
        assertEquals(0,calls)
        lab.setActive(true);lab.chat("public fixture");assertEquals(2,lab.state.value.messages.size)
        lab.setActive(false);assertTrue(lab.state.value.messages.isEmpty())
        lab.setActive(true);assertTrue(lab.state.value.messages.isEmpty())
    }
    @Test fun boundedHistoryContainsOnlyCompletedPairs() = runTest {
        var seen = emptyList<ChatMessage>()
        val lab = TextLabConversation { _, history -> seen=history;ChatMessage(role="assistant",text="synthetic") }
        lab.setActive(true)
        repeat(30) { lab.chat("public fixture $it") }
        assertEquals(24,seen.size);assertEquals(40,lab.state.value.messages.size)
        assertEquals("user",seen.first().role);assertEquals("assistant",seen.last().role)
    }
    @Test fun cancellationRejectsLateAnswerAndKeepsReservationUntilReturn() = runTest {
        val entered=CompletableDeferred<Unit>();val returned=CompletableDeferred<Unit>()
        var seen=emptyList<ChatMessage>();var first=true
        val lab=TextLabConversation { _, h -> seen=h;if(first) { first=false;entered.complete(Unit);returned.await() };ChatMessage(role="assistant",text="synthetic") }
        lab.setActive(true)
        var rejected=false
        val job=launch { try { lab.chat("cancelled fixture") } catch(_: CancellationException) { rejected=true } }
        runCurrent();entered.await();lab.invalidatePending()
        assertFalse(job.isCompleted)
        assertTrue(runCatching { lab.setActive(false) }.isFailure)
        assertTrue(runCatching { lab.chat("concurrent") }.isFailure)
        returned.complete(Unit);runCurrent();assertTrue(rejected)
        assertEquals(1,lab.state.value.messages.size)
        lab.armTurn();lab.chat("next fixture");assertTrue(seen.isEmpty())
    }
    @Test fun failedOrEmptyAnswerNeverEntersFollowupContext() = runTest {
        var fail=true;var seen=emptyList<ChatMessage>()
        val lab=TextLabConversation { _, h ->seen=h;ChatMessage(role="assistant",text=if(fail) "" else "synthetic") }
        lab.setActive(true);assertTrue(runCatching { lab.chat("failed fixture") }.isFailure)
        fail=false;lab.chat("next fixture");assertTrue(seen.isEmpty())
    }
    @Test fun modeReadinessCannotChangeWhileCoordinatorIsBusy() = runTest {
        val done=CompletableDeferred<ChatMessage>();val actions=FakeSessionActions().apply { chatBlock={done.await()} }
        val session=SessionCoordinator(actions,this,coroutineContext[kotlin.coroutines.ContinuationInterceptor] as kotlinx.coroutines.CoroutineDispatcher)
        session.prepare();runCurrent();assertTrue(session.state.value.prepared)
        session.send("fixture");runCurrent();var changed=false
        assertFalse(session.reprepareWhenIdle {changed=true;true});assertFalse(changed)
        done.complete(ChatMessage(role="assistant",text="synthetic"));runCurrent()
        assertTrue(session.reprepareWhenIdle {changed=true;true});assertTrue(changed)
        assertFalse(session.state.value.prepared);assertFalse(session.state.value.busy)
    }
    @Test fun cancellationBeforeWorkerEntryNeverCallsInference() = runTest {
        var calls=0
        val lab=TextLabConversation { _, _ -> calls++;ChatMessage(role="assistant",text="synthetic") }
        lab.setActive(true)
        val actions=object: SessionActions by FakeSessionActions() {
            override fun beforeTextChat() {lab.armTurn()}
            override fun requestInferenceStop() {lab.invalidatePending()}
            override suspend fun chat(text: String)=lab.chat(text)
        }
        val workerScheduler=kotlinx.coroutines.test.TestCoroutineScheduler()
        val worker=kotlinx.coroutines.test.StandardTestDispatcher(workerScheduler)
        val session=SessionCoordinator(actions,this,worker)
        session.send("queued public fixture");runCurrent()
        session.cancelCurrent();runCurrent()
        workerScheduler.runCurrent();runCurrent()
        assertEquals(0,calls);assertTrue(lab.state.value.messages.isEmpty());assertFalse(session.state.value.busy)
    }
    @Test fun oversizedInputCannotRemainInVolatileHistory() = runTest {
        var calls=0
        val lab=TextLabConversation { _, _ -> calls++;ChatMessage(role="assistant",text="synthetic") }
        lab.setActive(true)
        assertTrue(runCatching {lab.chat("x".repeat(2049))}.isFailure)
        assertTrue(runCatching {lab.chat("é".repeat(1025))}.isFailure)
        assertEquals(0,calls);assertTrue(lab.state.value.messages.isEmpty())
    }

}
