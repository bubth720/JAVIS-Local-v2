package com.stark.jarvislocal.fixture

import org.junit.Assert.*
import org.junit.Test

class PilotArbiterTest {
    private val participants = setOf("llm","historical")
    private fun arbiter(journal: MemoryJournal = MemoryJournal(), clock: () -> Long = { 10 }) = PilotArbiter(participants,journal,clock)

    @Test fun coldStartRequiresAllParticipantsAndFreshChallenge() {
        val a = arbiter(); val r = request()
        assertThrows(PilotBlocked::class.java) { a.reserve(r,a.authorization(r)) }
        assertThrows(PilotBlocked::class.java) { a.reconcile(emptyList()) }
        val proof = RecoveryEvidence("llm",null,"stale",ExecutionProof.NOT_STARTED,CleanupProof.RELEASED)
        assertThrows(PilotBlocked::class.java) { a.reconcile(listOf(proof,proof.copy(owner="historical"))) }
        a.recover(); assertEquals(PilotPhase.IDLE,a.snapshot().phase)
    }

    @Test fun singleReservationRetainedUntilExactNativeAndCleanupEvidence() {
        val a = arbiter(); a.recover(); val r = request(); val key = a.reserve(r,a.authorization(r)); a.running(key)
        val other = request()
        assertThrows(PilotBlocked::class.java) { a.reserve(other,a.authorization(other,"historical")) }
        assertThrows(PilotBlocked::class.java) { a.complete(released(key.copy(generation=key.generation+1))) }
        assertTrue(a.complete(released(key)))
        assertTrue(a.wasReleased(key)); assertEquals(PilotPhase.IDLE,a.snapshot().phase)
        val next = a.reserve(other,a.authorization(other,"historical"))
        assertEquals(key.generation+1,next.generation)
        assertThrows(PilotBlocked::class.java) { a.complete(released(key)) }
        assertEquals(next,a.snapshot().key)
    }

    @Test fun stopDoesNotReleaseAndCancelledResponseIsNotConsumable() {
        val a=arbiter(); a.recover(); val r=request(); val key=a.reserve(r,a.authorization(r))
        a.running(key); a.requestStop(key)
        assertEquals(key,a.snapshot().key)
        assertFalse(a.complete(released(key)))
        assertTrue(a.wasReleased(key))
    }

    @Test fun clientOrServiceDeathAndLostAckRequireReconciliationNotRetry() {
        for (failure in listOf("client death","service death","ACK lost")) {
            val a=arbiter(); a.recover(); val r=request(); val key=a.reserve(r,a.authorization(r)); a.running(key)
            a.connectionLost()
            assertThrows(failure,PilotBlocked::class.java) { a.complete(released(key)) }
            val next=request()
            assertThrows(failure,PilotBlocked::class.java) { a.reserve(next,a.authorization(next)) }
            a.recover(key)
            assertTrue(a.wasReleased(key)); assertEquals(PilotPhase.IDLE,a.snapshot().phase)
        }
    }

    @Test fun timeoutNeverReleasesByExpiryAlone() {
        var now=10L; val a=arbiter(clock={now}); a.recover(); val r=request(); val key=a.reserve(r,a.authorization(r,deadline=20))
        now=21; a.expire()
        assertEquals(key,a.snapshot().key); assertEquals(PilotPhase.RECOVERY_REQUIRED,a.snapshot().phase)
        assertThrows(PilotBlocked::class.java) { a.complete(released(key)) }
    }

    @Test fun unknownNativeStateAndFailedUnloadKeepReservation() {
        for (cleanup in CleanupProof.values()) {
            val a=arbiter(); a.recover(); val r=request(); val key=a.reserve(r,a.authorization(r))
            val evidence=released(key).copy(execution=ExecutionProof.UNKNOWN,cleanup=cleanup)
            assertFalse(a.complete(evidence)); assertEquals(key,a.snapshot().key)
            assertEquals(PilotPhase.RECOVERY_REQUIRED,a.snapshot().phase)
        }
    }

    @Test fun loadNeverStartedMayReleaseButCannotProvideANativeResponse() {
        val a=arbiter(); a.recover(); val r=request(); val key=a.reserve(r,a.authorization(r))
        assertFalse(a.complete(released(key).copy(execution=ExecutionProof.NOT_STARTED)))
        assertEquals(PilotPhase.IDLE,a.snapshot().phase)
    }

    @Test fun brokerRestartPreservesOwnerAndRejectsOldRecoveryChallenge() {
        val journal=MemoryJournal(); val old=arbiter(journal); old.recover(); val r=request(); val key=old.reserve(r,old.authorization(r))
        val restarted=arbiter(journal)
        assertEquals(key,restarted.snapshot().key); assertNotEquals(old.epoch,restarted.epoch)
        val proofs=listOf(RecoveryEvidence("llm",key,old.recoveryChallenge,ExecutionProof.NATIVE_RETURNED,CleanupProof.RELEASED),
            RecoveryEvidence("historical",null,old.recoveryChallenge,ExecutionProof.NOT_STARTED,CleanupProof.RELEASED))
        assertThrows(PilotBlocked::class.java) { restarted.reconcile(proofs) }
        restarted.recover(key)
        assertEquals(PilotPhase.IDLE,restarted.snapshot().phase)
        assertThrows(PilotBlocked::class.java) { restarted.reserve(r,old.authorization(r)) }
    }

    @Test fun journalWriteFailureDoesNotGrantOrPretendToRelease() {
        val journal=MemoryJournal(); val a=arbiter(journal); a.recover(); val r=request()
        journal.failWrite=true
        assertThrows(PilotBlocked::class.java) { a.reserve(r,a.authorization(r)) }
        journal.failWrite=false
        assertThrows(PilotBlocked::class.java) { a.reserve(r,a.authorization(r)) }
    }

    @Test fun wrongOwnerExpiredMandateAndOldOperationAreRefused() {
        val a=arbiter(); a.recover(); val r=request()
        assertThrows(PilotBlocked::class.java) { a.reserve(r,a.authorization(r,"stranger")) }
        assertThrows(PilotBlocked::class.java) { a.reserve(r,a.authorization(r,deadline=9)) }
        val key=a.reserve(r,a.authorization(r))
        assertThrows(PilotBlocked::class.java) { a.complete(released(key,"historical")) }
        a.complete(released(key))
        assertThrows(PilotBlocked::class.java) { a.reserve(r,a.authorization(r)) }
    }
}
