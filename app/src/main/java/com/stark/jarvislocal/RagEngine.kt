package com.stark.jarvislocal

import java.text.Normalizer
import kotlin.math.ln

class RagEngine(private val db: LocalDatabase) {
    private val stopWords = setOf(
        "les","des","une","un","dans","pour","avec","sur","que","qui","est","sont","aux","du","de","la","le","et","ou","en","au",
        "ce","ces","cette","mon","ma","mes","ton","ta","tes","son","sa","ses","je","tu","il","elle","nous","vous","ils","elles"
    )

    fun retrieve(query: String, limit: Int = 5): List<RetrievalHit> {
        val q = tokens(query)
        if (q.isEmpty()) return emptyList()
        val chunks = db.allChunks()
        if (chunks.isEmpty()) return emptyList()

        val docFreq = q.associateWith { term -> chunks.count { tokens(it.text).contains(term) } }
        val n = chunks.size.toDouble()

        return chunks.map { chunk ->
            val words = tokensWithDuplicates(chunk.text)
            val counts = words.groupingBy { it }.eachCount()
            var score = 0.0
            for (term in q) {
                val tf = counts[term] ?: 0
                if (tf == 0) continue
                val df = docFreq[term] ?: 0
                val idf = ln(1.0 + (n - df + 0.5) / (df + 0.5))
                score += (1.0 + ln(1.0 + tf)) * idf
            }
            if (normalize(chunk.text).contains(normalize(query)) && query.length > 8) score += 4.0
            RetrievalHit(chunk, score)
        }.filter { it.score > 0.0 }
            .sortedByDescending { it.score }
            .take(limit)
    }

    fun relevantMemories(query: String, limit: Int = 4): List<MemoryItem> {
        val q = tokens(query)
        return db.allMemories()
            .map { m -> m to q.count { normalize(m.text).contains(it) } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    private fun tokens(s: String): Set<String> =
        tokensWithDuplicates(s).filter { it.length > 2 && it !in stopWords }.toSet()

    private fun tokensWithDuplicates(s: String): List<String> =
        normalize(s).split(Regex("\\s+")).filter { it.isNotBlank() }

    private fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
}
