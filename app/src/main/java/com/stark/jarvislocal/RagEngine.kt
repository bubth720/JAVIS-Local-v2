package com.stark.jarvislocal

import java.text.Normalizer
import kotlin.math.ln

class RagEngine(private val db: LocalDatabase) {
    private val stopWords = setOf(
        "les","des","une","un","dans","pour","avec","sur","que","qui","est","sont","aux","du","de","la","le","et","ou","en","au",
        "ce","ces","cette","mon","ma","mes","ton","ta","tes","son","sa","ses","je","tu","il","elle","nous","vous","ils","elles",
        "quel","quelle","quels","quelles","effectue","effectué","par"
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
                val rareBoost = if (term.length >= 5) 1.8 else 1.0
                score += (1.0 + ln(1.0 + tf)) * idf * rareBoost
            }
            if (normalize(chunk.text).contains(normalize(query)) && query.length > 8) score += 4.0
            RetrievalHit(chunk, score)
        }.filter { it.score > 0.0 }
            .sortedByDescending { it.score }
            .take(limit)
    }

    /**
     * Recherche directe dans de petites fenêtres de lignes.
     * Plus adaptée aux tableaux/listes PDF que les gros chunks.
     */
    fun bestEvidence(query: String): DocumentEvidence? {
        val queryTokens = tokens(query)
        if (queryTokens.isEmpty()) return null

        var best: DocumentEvidence? = null

        for (chunk in db.allChunks()) {
            val lines = chunk.text
                .split(Regex("\\r?\\n"))
                .map { it.trim() }
                .filter { it.isNotBlank() }

            if (lines.isEmpty()) continue

            for (i in lines.indices) {
                val from = maxOf(0, i - 2)
                val to = minOf(lines.size, i + 3)
                val window = lines.subList(from, to).joinToString(" | ")
                val normalized = normalize(window)

                var score = 0.0
                for (term in queryTokens) {
                    if (normalized.contains(term)) {
                        score += when {
                            term.length >= 7 -> 3.0
                            term.length >= 5 -> 2.2
                            else -> 1.2
                        }
                    }
                }

                if (queryTokens.count { normalized.contains(it) } >= 2) score += 2.0

                if (best == null || score > best!!.score) {
                    best = DocumentEvidence(
                        documentName = chunk.documentName,
                        text = window.take(900),
                        score = score
                    )
                }
            }
        }

        return best?.takeIf { it.score >= 4.0 }
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
