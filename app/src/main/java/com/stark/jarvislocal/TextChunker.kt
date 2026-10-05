package com.stark.jarvislocal

object TextChunker {
    fun chunk(text: String, targetChars: Int = 1000, overlapChars: Int = 180): List<String> {
        val clean = text
            .replace("\u0000", " ")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

        if (clean.isBlank()) return emptyList()
        if (clean.length <= targetChars) return listOf(clean)

        val parts = mutableListOf<String>()
        var start = 0

        while (start < clean.length) {
            var end = minOf(clean.length, start + targetChars)

            if (end < clean.length) {
                val floor = start + (targetChars * 0.60).toInt()
                val candidates = listOf(
                    clean.lastIndexOf("\n", end),
                    clean.lastIndexOf(". ", end),
                    clean.lastIndexOf("; ", end)
                )
                val natural = candidates.filter { it >= floor }.maxOrNull()
                if (natural != null) end = natural + 1
            }

            val piece = clean.substring(start, end).trim()
            if (piece.length > 30) parts += piece

            if (end >= clean.length) break
            start = maxOf(0, end - overlapChars)
        }

        return parts
    }
}
