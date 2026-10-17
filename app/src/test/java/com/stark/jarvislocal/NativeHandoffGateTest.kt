package com.stark.jarvislocal

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class NativeHandoffGateTest {
    private fun id()=UUID.randomUUID().toString()
    private class Fixture {
        var now=10L;var proof: NativeReleaseSnapshot?=null
        val gate=NativeHandoffGate(NativeReleasePort { proof }){now}
        fun prove(generation: Long=0) { proof=NativeReleaseSnapshot(gate.epoch,generation,NativeReleaseState.DONE_UNLOADED,NativeReleaseState.DONE_UNLOADED) }
    }
    @Test fun productionHasNoNativeProofAndNeverGrants() {
        val gate=NativeHandoffGate(clock={10});assertThrows(IllegalStateException::class.java){gate.reserve(id(),id(),100)}
        assertEquals(HandoffState.BLOCKED,gate.state)
    }
    @Test fun bothParticipantsMustHaveEffectiveDoneAndUnload() {
        for(state in NativeReleaseState.values()) for(llm in listOf(true,false)) {
            if(state == NativeReleaseState.DONE_UNLOADED) continue
            val f=Fixture();f.prove();f.proof=if(llm) f.proof!!.copy(llm=state) else f.proof!!.copy(whisper=state)
            assertThrows(IllegalStateException::class.java){f.gate.reserve(id(),id(),100)}
        }
    }
    @Test fun requestForStopOrNativeDoneWithResidencyCannotAdmit() {
        val f=Fixture();f.prove();f.proof=f.proof!!.copy(llm=NativeReleaseState.STOP_REQUESTED)
        assertThrows(IllegalStateException::class.java){f.gate.reserve(id(),id(),100)}
        f.proof=f.proof!!.copy(llm=NativeReleaseState.DONE_RESIDENT)
        assertThrows(IllegalStateException::class.java){f.gate.reserve(id(),id(),100)}
    }
    @Test fun staleGenerationOrPreviousProcessEpochCannotProveRelease() {
        val f=Fixture();f.prove();f.proof=f.proof!!.copy(epoch=id())
        assertThrows(IllegalStateException::class.java){f.gate.reserve(id(),id(),100)}
        f.prove(1);assertThrows(IllegalStateException::class.java){f.gate.reserve(id(),id(),100)}
    }
    @Test fun exclusiveReservationAndReleaseExactlyOnce() {
        val f=Fixture();f.prove();val t=f.gate.reserve(id(),id(),100)
        assertThrows(IllegalStateException::class.java){f.gate.reserve(id(),id(),100)}
        f.gate.confirmReleased(t,true,true,true)
        assertThrows(IllegalStateException::class.java){f.gate.confirmReleased(t,true,true,true)}
        f.prove(1);val next=f.gate.reserve(id(),id(),100);assertEquals(2,next.generation)
        assertThrows(IllegalStateException::class.java){f.gate.confirmReleased(t,true,true,true)}
        assertTrue(f.gate.accepts(next))
    }
    @Test fun cancellationInvalidatesResultsButRetainsHeavySlot() {
        val f=Fixture();f.prove();val t=f.gate.reserve(id(),id(),100);f.gate.stop(t);f.gate.stop(t)
        assertFalse(f.gate.accepts(t));assertThrows(IllegalStateException::class.java){f.gate.reserve(id(),id(),100)}
        f.gate.confirmReleased(t,true,true,true);assertEquals(HandoffState.RELEASED,f.gate.state)
    }
    @Test fun timeoutDeathAndCleanupFailureRequireReconciliation() {
        for(failure in listOf("timeout","death","cleanup")) {
            val f=Fixture();f.prove();val t=f.gate.reserve(id(),id(),100)
            when(failure) {
                "timeout"->{f.now=111;f.gate.expire()}
                "death"->f.gate.lostOrExpired()
                else->assertThrows(IllegalStateException::class.java){f.gate.confirmReleased(t,true,true,false)}
            }
            assertEquals(HandoffState.UNKNOWN,f.gate.state);assertFalse(f.gate.accepts(t))
            assertThrows(IllegalStateException::class.java){f.gate.confirmReleased(t,true,true,true)}
            assertThrows(IllegalStateException::class.java){f.gate.reserve(id(),id(),100)}
        }
    }
    @Test fun nativeAudioAndModelCleanupAreIndependent() {
        for(flags in listOf(Triple(false,true,true),Triple(true,false,true),Triple(true,true,false))) {
            val f=Fixture();f.prove();val t=f.gate.reserve(id(),id(),100)
            assertThrows(IllegalStateException::class.java){f.gate.confirmReleased(t,flags.first,flags.second,flags.third)}
            assertEquals(HandoffState.UNKNOWN,f.gate.state)
        }
    }
    @Test fun restartedGateDefaultsToUnknownAndCannotConsumeOldTicket() {
        val f=Fixture();f.prove();val t=f.gate.reserve(id(),id(),100)
        val restarted=NativeHandoffGate(clock={10})
        assertThrows(IllegalStateException::class.java){restarted.reserve(id(),id(),100)}
        assertThrows(IllegalStateException::class.java){restarted.confirmReleased(t,true,true,true)}
    }
    @Test fun replayIsRefusedAfterConfirmedRelease() {
        val f=Fixture();f.prove();val session=id();val correlation=id();val t=f.gate.reserve(session,correlation,100)
        f.gate.confirmReleased(t,true,true,true);f.prove(1)
        assertThrows(IllegalStateException::class.java){f.gate.reserve(session,correlation,100)}
    }
}
