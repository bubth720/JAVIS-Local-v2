package com.stark.jarvislocal.coreui.lab

import com.stark.jarvislocal.events.CoreEvent
import com.stark.jarvislocal.events.EventSnapshot
import com.stark.jarvislocal.integrations.PinnedDocumentProducer
import com.stark.jarvislocal.integrations.PinnedDocumentReadiness
import com.stark.jarvislocal.integrations.PinnedDocumentStatus
import java.util.Collections
import java.util.UUID

const val CORE_UI_READONLY_LAB_VERSION = "jarvis.core-ui.readonly-lab/1-draft"
enum class LabPortState { OFF, UNTRUSTED, REFUSED, UNKNOWN }
enum class LabRefusal {
    DISABLED, NONE, UNSUPPORTED_VERSION, PAYLOAD_IDENTITY, WRONG_ORIGIN, UID_MISMATCH,
    PACKAGE_MISMATCH, CERTIFICATE_MISMATCH, CERTIFICATE_EXPIRED, STALE_EPOCH,
    WRONG_SESSION, WRONG_GENERATION, ACK_REQUIRED, CANCELLED, EXPIRED, CLOCK_ROLLBACK,
    PROCESS_DEATH, REPLAY, SEQUENCE_GAP, RETROACTIVE_EVENT, SOURCE_UNAVAILABLE, REPLAY_WINDOW_LOST
}
enum class AssertionOrigin { PAYLOAD, SYNTHETIC_FIXTURE }
enum class LabProjectionOrigin { NONE, LABORATORY }

/** Test assertions only; these values are never system UID/package/certificate evidence. */
data class SyntheticPeerAssertion(val origin: AssertionOrigin, val module: String, val uid: Int,
    val packageName: String, val certificateSha256: String, val expiresAtMs: Long)
class SyntheticPeerPolicy(val uid: Int, val packageName: String, val certificateSha256: String) {
    init { require(uid >= 0 && packageName.length in 1..120 && certificateSha256.matches(Regex("[0-9a-f]{64}"))) }
    internal fun inspect(peer: SyntheticPeerAssertion, now: Long): LabRefusal = when {
        peer.origin != AssertionOrigin.SYNTHETIC_FIXTURE -> LabRefusal.PAYLOAD_IDENTITY
        peer.module != "interface" -> LabRefusal.WRONG_ORIGIN
        peer.uid != uid -> LabRefusal.UID_MISMATCH
        peer.packageName != packageName -> LabRefusal.PACKAGE_MISMATCH
        peer.certificateSha256 != certificateSha256 -> LabRefusal.CERTIFICATE_MISMATCH
        peer.expiresAtMs <= now -> LabRefusal.CERTIFICATE_EXPIRED
        else -> LabRefusal.NONE
    }
}
data class LabBinding(val epoch: String, val sessionId: String, val requestId: String,
    val generation: Long, val version: String = CORE_UI_READONLY_LAB_VERSION) {
    init {
        listOf(epoch, sessionId, requestId).forEach { require(UUID.fromString(it).toString() == it) }
        require(generation >= 1)
    }
}
/** Metadata only: completion is not a response, citation, playback or native-release proof. */
data class LabMetadata(val epoch: String, val sessionId: String, val requestId: String,
    val generation: Long, val eventId: String, val sequence: Long, val kind: String)
class LabProjection internal constructor(val state: LabPortState, val reason: LabRefusal,
    events: List<LabMetadata> = emptyList(), pinned: List<PinnedDocumentStatus> = emptyList()) {
    val events: List<LabMetadata> = Collections.unmodifiableList(ArrayList(events))
    val pinned: List<PinnedDocumentStatus> = Collections.unmodifiableList(ArrayList(pinned))
    val origin: LabProjectionOrigin get() = if (state == LabPortState.UNTRUSTED) LabProjectionOrigin.LABORATORY else LabProjectionOrigin.NONE
    val realCoreAuthenticated: Boolean get() = false
    val canExecute: Boolean get() = false
}

/**
 * OFF by default, uncomposed in AppGraph. Read-only pull of existing CORE metadata.
 * An explicit synthetic fixture may inspect metadata, always UNTRUSTED. No production
 * activation, callback, command, grant, extra coordinator, persistence or native admission.
 */
class ReadOnlyCoreUiLabPort(
    private val snapshot: () -> EventSnapshot,
    private val pinned: PinnedDocumentReadiness,
    private val policy: SyntheticPeerPolicy,
    private val clockMs: () -> Long,
    private val syntheticOptIn: Boolean = false,
    val epoch: String = UUID.randomUUID().toString(),
    private val lifetimeMs: Long = 30_000,
    private val maxEvents: Int = 64,
    private val maxRemembered: Int = 256
) {
    init {
        require(UUID.fromString(epoch).toString() == epoch)
        require(lifetimeMs in 1..30_000 && maxEvents in 1..64 && maxRemembered in maxEvents..256)
    }
    private var binding: LabBinding? = null
    private var peer: SyntheticPeerAssertion? = null
    private var openedAt = 0L
    private var lastClock = 0L
    private var acknowledged = false
    private var terminal: LabRefusal? = null
    private var lastSequence = 0L
    private val seen = LinkedHashMap<String, LabMetadata>()

    @Synchronized fun open(candidate: LabBinding, assertion: SyntheticPeerAssertion): LabProjection {
        if (!syntheticOptIn) return result(LabPortState.OFF, LabRefusal.DISABLED)
        terminal?.let { return result(LabPortState.UNKNOWN, it) }
        if (binding != null) return invalidate(LabRefusal.REPLAY)
        if (candidate.version != CORE_UI_READONLY_LAB_VERSION) return result(LabPortState.REFUSED, LabRefusal.UNSUPPORTED_VERSION)
        if (candidate.epoch != epoch) return result(LabPortState.REFUSED, LabRefusal.STALE_EPOCH)
        val now = clockMs()
        if (now < 0) return invalidate(LabRefusal.CLOCK_ROLLBACK)
        val identity = policy.inspect(assertion, now)
        if (identity != LabRefusal.NONE) return result(LabPortState.REFUSED, identity)
        val current = try { snapshot() } catch (_: Exception) { return invalidate(LabRefusal.SOURCE_UNAVAILABLE) }
        if (!current.enabled || current.generation != candidate.generation) return result(LabPortState.REFUSED, LabRefusal.WRONG_GENERATION)
        binding = candidate; peer = assertion; openedAt = now; lastClock = now
        return result(LabPortState.UNTRUSTED, LabRefusal.NONE)
    }

    private fun active(): LabProjection? {
        if (!syntheticOptIn) return result(LabPortState.OFF, LabRefusal.DISABLED)
        terminal?.let { return result(LabPortState.UNKNOWN, it) }
        if (binding == null) return result(LabPortState.REFUSED, LabRefusal.ACK_REQUIRED)
        val now = clockMs()
        if (now < lastClock) return invalidate(LabRefusal.CLOCK_ROLLBACK)
        lastClock = now
        if (now - openedAt >= lifetimeMs) return invalidate(LabRefusal.EXPIRED)
        val identity = policy.inspect(requireNotNull(peer), now)
        if (identity != LabRefusal.NONE) return invalidate(identity)
        return null
    }
    @Synchronized fun acknowledge(): LabProjection {
        active()?.let { return it }; acknowledged = true
        return result(LabPortState.UNTRUSTED, LabRefusal.NONE)
    }
    /** afterSequence is an exact cursor, not a request to replay or mutate the source bus. */
    @Synchronized fun poll(afterSequence: Long, payloadIdentityClaims: Map<String, String> = emptyMap()): LabProjection {
        active()?.let { return it }
        if (payloadIdentityClaims.isNotEmpty()) return invalidate(LabRefusal.PAYLOAD_IDENTITY)
        if (!acknowledged) return result(LabPortState.REFUSED, LabRefusal.ACK_REQUIRED)
        if (afterSequence != lastSequence) return invalidate(LabRefusal.REPLAY)
        val expected = requireNotNull(binding)
        val current = try { snapshot() } catch (_: Exception) { return invalidate(LabRefusal.SOURCE_UNAVAILABLE) }
        if (!current.enabled || current.generation != expected.generation) return invalidate(LabRefusal.WRONG_GENERATION)
        if (current.dropped > 0 || current.events.size > maxEvents) return invalidate(LabRefusal.REPLAY_WINDOW_LOST)
        val fresh = ArrayList<LabMetadata>()
        for (observed in current.events) {
            val e = observed.envelope
            if (e.version != 1) return invalidate(LabRefusal.UNSUPPORTED_VERSION)
            if (e.sessionId != expected.sessionId || e.requestId != expected.requestId) return invalidate(LabRefusal.WRONG_SESSION)
            val value = LabMetadata(epoch, e.sessionId, e.requestId, expected.generation,
                e.eventId, observed.sequence, e.event.kind)
            val old = seen[e.eventId]
            if (observed.sequence <= lastSequence) {
                if (old != value) return invalidate(LabRefusal.RETROACTIVE_EVENT)
                continue // same immutable source snapshot; never emit it again
            }
            if (old != null) return invalidate(LabRefusal.REPLAY)
            if (observed.sequence != lastSequence + 1) return invalidate(LabRefusal.SEQUENCE_GAP)
            if (seen.size >= maxRemembered) return invalidate(LabRefusal.REPLAY_WINDOW_LOST)
            seen[e.eventId] = value; fresh.add(value); lastSequence = observed.sequence
            if (e.event is CoreEvent.CancelRequested || e.event is CoreEvent.Cancelled) return invalidate(LabRefusal.CANCELLED)
        }
        val statuses = PinnedDocumentProducer.entries.map(pinned::inspect)
        return LabProjection(LabPortState.UNTRUSTED, LabRefusal.NONE, fresh, statuses)
    }
    @Synchronized fun cancel(): LabProjection = if (!syntheticOptIn) result(LabPortState.OFF, LabRefusal.DISABLED) else invalidate(LabRefusal.CANCELLED)
    /** Observation of death, not unload or permission to retry/reset the bus or a native engine. */
    @Synchronized fun processDied(): LabProjection = if (!syntheticOptIn) result(LabPortState.OFF, LabRefusal.DISABLED) else invalidate(LabRefusal.PROCESS_DEATH)
    private fun invalidate(reason: LabRefusal): LabProjection {
        if (terminal == null) terminal = reason
        return result(LabPortState.UNKNOWN, requireNotNull(terminal))
    }
    private fun result(state: LabPortState, reason: LabRefusal) = LabProjection(state, reason)
}
