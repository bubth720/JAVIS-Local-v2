package com.stark.jarvislocal.memory

import com.stark.jarvislocal.identity.IdentityCodec
import java.text.Normalizer
import java.util.Locale
import java.util.UUID

enum class MemoryUseContext { DAILY, WORK, URGENT }
data class MemoryQuery(val sessionId: String, val question: String, val context: MemoryUseContext = MemoryUseContext.DAILY,
    val limit: Int = 5, val budgetBytes: Int = 4096) {
    init {
        require(UUID.fromString(sessionId).toString() == sessionId)
        require(question.isNotBlank() && IdentityCodec.utf8(question).size <= 512)
        require(limit in 1..10 && budgetBytes in 256..8192)
    }
    override fun toString() = "MemoryQuery(context=$context, text=REDACTED)"
}

/** Volatile host controls, never decoded from a document or model answer. Closed by default. */
class UserMemoryControl {
    private val sessions = mutableMapOf<String, MutableSet<String>>()
    @Synchronized fun resumeSession(id: String) {
        require(UUID.fromString(id).toString() == id)
        require(id in sessions || sessions.size < 64)
        sessions.getOrPut(id) { mutableSetOf() }
    }
    @Synchronized fun suspendSession(id: String) { sessions.remove(id) }
    @Synchronized fun exclude(id: String, reference: MemoryReference) {
        sessions[id]?.let { require(reference.id in it || it.size < 200); it.add(reference.id) }
    }
    @Synchronized internal fun token(id: String): Any? = sessions[id]
    @Synchronized internal fun allows(id: String, memoryId: String? = null): Boolean =
        sessions[id]?.let { memoryId == null || memoryId !in it } ?: false
}

enum class RecallStatus { READY, EMPTY, NEEDS_CONFIRMATION, DENIED }
data class MemoryExplanation(val reference: MemoryReference, val matchingTerms: Set<String>, val context: MemoryUseContext,
    val confirmedAtMs: Long, val source: String = "USER_EXPLICIT")
data class RecalledMemory(val reference: MemoryReference, val content: String, val scope: MemoryScope,
    val explanation: MemoryExplanation) {
    override fun toString() = "RecalledMemory(content=REDACTED, scope=$scope)"
}
data class MemoryRecall(val status: RecallStatus, val items: List<RecalledMemory>) {
    override fun toString() = "MemoryRecall(status=$status, count=${items.size})"
}

/** Scan of at most 200 confirmed lab rows; no new store, embeddings, or hidden chat collection. */
class MemoryRecallIndex(private val store: ConfirmedMemoryStore, private val controls: UserMemoryControl,
    private val wallMs: () -> Long = System::currentTimeMillis) {
    private fun terms(text: String): Set<String> = Normalizer.normalize(text, Normalizer.Form.NFC)
        .lowercase(Locale.ROOT).split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.length >= 2 }.take(64).toSet()

    fun recall(query: MemoryQuery, policy: ProjectionPolicy = ProjectionPolicy(),
        selected: Set<MemoryReference>? = null): MemoryRecall = synchronized(store) {
        if (!controls.allows(query.sessionId) || policy.allowed.isEmpty()) return@synchronized MemoryRecall(RecallStatus.DENIED, emptyList())
        val sessionToken = controls.token(query.sessionId)
        val requested = terms(query.question)
        val now = wallMs()
        val ranked = store.listOwned().filter {
            it.scope != MemoryScope.SENSITIVE && it.scope in policy.allowed && !it.suspended &&
                controls.allows(query.sessionId, it.reference.id) && now >= it.createdAtMs &&
                (it.expiresAtMs == null || now < it.expiresAtMs)
        }.map { it to (terms(it.content + " " + it.preferenceKey.orEmpty()) intersect requested) }
            .filter { it.second.isNotEmpty() }
            .sortedWith(compareByDescending<Pair<MemoryRecord, Set<String>>> { it.second.size }
                .thenByDescending { it.first.createdAtMs }.thenBy { it.first.reference.id })
        if (controls.token(query.sessionId) !== sessionToken || !controls.allows(query.sessionId)) return@synchronized MemoryRecall(RecallStatus.DENIED, emptyList())
        if (ranked.isEmpty()) return@synchronized MemoryRecall(RecallStatus.EMPTY, emptyList())
        val chosen = if (selected == null) ranked.take(query.limit) else {
            if (selected.isEmpty() || selected.size > query.limit || !ranked.map { it.first.reference }.containsAll(selected))
                return@synchronized MemoryRecall(RecallStatus.NEEDS_CONFIRMATION, emptyList())
            ranked.filter { it.first.reference in selected }
        }
        val items = chosen.map { (r, matching) -> RecalledMemory(r.reference, r.content, r.scope,
            MemoryExplanation(r.reference, matching.toSet(), query.context, r.consent.confirmedAtMs)) }
        if (items.any { !controls.allows(query.sessionId, it.reference.id) }) return@synchronized MemoryRecall(RecallStatus.DENIED, emptyList())
        // Lexical ties are ambiguous, not proof that the texts semantically contradict.
        val ambiguous = selected == null && ranked.size > 1 && ranked[0].second.size == ranked[1].second.size
        MemoryRecall(if (ambiguous) RecallStatus.NEEDS_CONFIRMATION else RecallStatus.READY, items)
    }
}
