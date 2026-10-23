package com.stark.jarvislocal.memory

import com.stark.jarvislocal.identity.IdentityCodec

data class MemoryContext(val status: RecallStatus, val jsonData: String, val references: List<MemoryReference>, val bytes: Int) {
    override fun toString() = "MemoryContext(status=$status, bytes=$bytes, data=REDACTED)"
}

/** A separate, quoted data block. Byte limit is NOT a tokenizer or a generation admission. */
class MemoryContextAssembler(private val index: MemoryRecallIndex) {
    fun assemble(query: MemoryQuery, policy: ProjectionPolicy = ProjectionPolicy(), selected: Set<MemoryReference>? = null): MemoryContext {
        val recall = index.recall(query, policy, selected) // fresh read: no cached deleted/suspended memories
        if (recall.status != RecallStatus.READY) return MemoryContext(recall.status, "", emptyList(), 0)
        val entries = mutableListOf<Map<String, Any?>>()
        val refs = mutableListOf<MemoryReference>()
        var bytes = ByteArray(0)
        for (item in recall.items) {
            val entry = mapOf("id" to item.reference.id, "revision" to item.reference.revision,
                "content_sha256" to item.reference.contentDigest, "text" to item.content,
                "source" to item.explanation.source, "scope" to item.scope.name,
                "confirmed_at_ms" to item.explanation.confirmedAtMs)
            val encoded = IdentityCodec.canonical(mapOf("kind" to "confirmed_memory_data", "untrusted_data" to true,
                "entries" to (entries + listOf(entry))))
            if (encoded.size > query.budgetBytes) continue // omit a whole entry; never truncate consented content
            entries += entry; refs += item.reference; bytes = encoded
        }
        if (refs.isEmpty()) return MemoryContext(RecallStatus.EMPTY, "", emptyList(), 0)
        return MemoryContext(RecallStatus.READY, bytes.toString(Charsets.UTF_8), refs.toList(), bytes.size)
    }
}
