package com.stark.jarvislocal.coreui.lab

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/**
 * LOT4 test-only authority: no production port, real UID, token, SAF or IPC.
 * A positive decision here MUST NOT be interpreted as a runtime grant.
 */
class CoreLot4SyntheticAuthorityTest {
    private enum class Refusal {
        NONE, DISABLED, EPOCH, GENERATION, IDENTITY, EXPIRED, REVOKED,
        REPLAY, CANCELLED, UNKNOWN
    }
    private data class Key(
        val epoch: String, val session: String, val request: String,
        val generation: Long, val deadlineNanos: Long
    )
    private class Authority(private val clock: () -> Long, val epoch: String = UUID.randomUUID().toString()) {
        var enabled: Boolean = false
        var realIdentityAttested: Boolean = false
            private set
        var canExecute: Boolean = false
            private set
        private var generation: Long = 1
        private var cancelled = false
        private var uncertain = false
        private val consumed = HashSet<Pair<String, String>>()
        fun revoke() { generation++ }
        fun cancel() { cancelled = true }
        fun unknown() { uncertain = true }
        fun review(key: Key, syntheticIdentity: Boolean): Refusal = when {
            !enabled -> Refusal.DISABLED
            uncertain -> Refusal.UNKNOWN
            cancelled -> Refusal.CANCELLED
            key.epoch != epoch -> Refusal.EPOCH
            key.generation != generation || key.generation < 1 || key.generation > 9007199254740991L -> Refusal.GENERATION
            !syntheticIdentity -> Refusal.IDENTITY
            key.deadlineNanos <= clock() -> Refusal.EXPIRED
            !valid(key.session) || !valid(key.request) -> Refusal.IDENTITY
            consumed.contains(key.session to key.request) -> Refusal.REPLAY
            else -> Refusal.NONE
        }
        fun consumeSyntheticFixture(key: Key, syntheticIdentity: Boolean): Refusal {
            val result = review(key, syntheticIdentity)
            if (result == Refusal.NONE) consumed.add(key.session to key.request)
            return result
        }
        private fun valid(s: String): Boolean = try { UUID.fromString(s).toString() == s } catch (_: IllegalArgumentException) { false }
    }
    private val id1 = "00000000-0000-4000-8000-000000000001"
    private val id2 = "00000000-0000-4000-8000-000000000002"
    private fun key(a: Authority, generation: Long = 1, deadline: Long = 1000L) =
        Key(a.epoch, id1, id2, generation, deadline)

    @Test fun defaultAlwaysDisabledAndCannotExecute() {
        val a = Authority({100L})
        assertEquals(Refusal.DISABLED, a.review(key(a), true))
        assertFalse(a.enabled); assertFalse(a.canExecute); assertFalse(a.realIdentityAttested)
    }
    @Test fun explicitFixtureIsStillNotAndroidAuthentication() {
        val a = Authority({100L}); a.enabled=true
        assertEquals(Refusal.NONE, a.consumeSyntheticFixture(key(a),true))
        assertFalse(a.realIdentityAttested);assertFalse(a.canExecute)
    }
    @Test fun noPayloadIdentity() {
        val a=Authority({100L}); a.enabled=true
        assertEquals(Refusal.IDENTITY,a.consumeSyntheticFixture(key(a),false))
    }
    @Test fun generationZeroDenied() {
        val a=Authority({100L}); a.enabled=true
        assertEquals(Refusal.GENERATION,a.consumeSyntheticFixture(key(a,0),true))
    }
    @Test fun generationOverWireLimitDenied() {
        val a=Authority({100L}); a.enabled=true
        assertEquals(Refusal.GENERATION,a.consumeSyntheticFixture(key(a,9007199254740992L),true))
    }
    @Test fun epochChangeDenied() {
        val a=Authority({100L}); a.enabled=true
        assertEquals(Refusal.EPOCH,a.consumeSyntheticFixture(key(a).copy(epoch=UUID.randomUUID().toString()),true))
    }
    @Test fun replayDenied() {
        val a=Authority({100L}); a.enabled=true
        assertEquals(Refusal.NONE,a.consumeSyntheticFixture(key(a),true))
        assertEquals(Refusal.REPLAY,a.consumeSyntheticFixture(key(a),true))
    }
    @Test fun expiredDenied() {
        val a=Authority({1000L}); a.enabled=true
        assertEquals(Refusal.EXPIRED,a.consumeSyntheticFixture(key(a),true))
    }
    @Test fun revokeInvalidatesGeneration() {
        val a=Authority({100L}); a.enabled=true
        val old=key(a)
        a.revoke()
        assertEquals(Refusal.GENERATION,a.consumeSyntheticFixture(old,true))
    }
    @Test fun cancelRefusesOldAndNew() {
        val a=Authority({100L}); a.enabled=true
        a.cancel()
        assertEquals(Refusal.CANCELLED,a.consumeSyntheticFixture(key(a),true))
    }
    @Test fun unknownStaysBlocked() {
        val a=Authority({100L}); a.enabled=true
        a.unknown()
        assertEquals(Refusal.UNKNOWN,a.consumeSyntheticFixture(key(a),true))
        assertFalse(a.canExecute)
    }
    @Test fun malformedUuidDenied() {
        val a=Authority({100L}); a.enabled=true
        assertEquals(Refusal.IDENTITY,a.consumeSyntheticFixture(key(a).copy(request="forged"),true))
    }
}
