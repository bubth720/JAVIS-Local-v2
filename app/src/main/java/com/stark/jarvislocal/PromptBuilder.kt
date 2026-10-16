package com.stark.jarvislocal

object PromptBuilder {
    data class Prepared(val text: String, val documentNames: List<String>, val context: String = "",
        val sources: List<InferenceSource> = emptyList())

    /** Conservative UTF-8 byte budget, not a model tokenizer. Never truncate the question. */
    fun prepare(
        user: String,
        history: List<ChatMessage>,
        hits: List<RetrievalHit>,
        memories: List<MemoryItem>,
        settings: JarvisSettings
    ): Prepared {
        val available = settings.contextSize - settings.maxTokens - 128 - settings.systemPrompt.toByteArray(Charsets.UTF_8).size
        val question = "UTILISATEUR: $user\nASSISTANT:"
        require(available > 0 && question.toByteArray(Charsets.UTF_8).size <= available) {
            "Question ou prompt système trop long pour le contexte local. Réduis sa longueur."
        }
        var remaining = available - question.toByteArray(Charsets.UTF_8).size
        fun fits(block: String): Boolean {
            val size = block.toByteArray(Charsets.UTF_8).size
            if (size > remaining) return false
            remaining -= size
            return true
        }
        val documents = StringBuilder()
        val names = mutableListOf<String>()
        val sources = mutableListOf<InferenceSource>()
        hits.forEach { hit ->
            val block = "[SOURCE ${names.size + 1}: ${hit.chunk.documentName}]\n${hit.chunk.text}\n\n"
            val header = if (documents.isEmpty()) "DOCUMENTS LOCAUX PERTINENTS:\n" else ""
            if (fits(header + block)) {
                documents.append(header).append(block)
                names += hit.chunk.documentName
                sources += InferenceSource(hit.chunk.documentId, hit.chunk.id, hit.chunk.documentName, hit.chunk.chunkIndex)
            }
        }
        val memory = StringBuilder()
        memories.forEach {
            val block = (if (memory.isEmpty()) "MÉMOIRE LOCALE PERTINENTE:\n" else "") + "- ${it.text}\n"
            if (fits(block)) memory.append(block)
        }
        // Select newest entries first, render them in chronological order.
        val selected = mutableListOf<String>()
        history.takeLast(12).asReversed().forEach {
            val block = "${it.role.uppercase()}: ${it.text.take(1400)}\n"
            val header = if (selected.isEmpty()) "HISTORIQUE RÉCENT:\n" else ""
            if (fits(header + block)) selected += block
        }
        val recent = if (selected.isEmpty()) "" else "HISTORIQUE RÉCENT:\n" + selected.asReversed().joinToString("")
        val context = memory.toString() + documents + recent
        return Prepared(context + question, names.distinct(), context, sources.toList())
    }

    fun build(
        user: String,
        history: List<ChatMessage>,
        hits: List<RetrievalHit>,
        memories: List<MemoryItem>
    ): String {
        val sb = StringBuilder()

        if (memories.isNotEmpty()) {
            sb.append("MÉMOIRE LOCALE PERTINENTE:\n")
            memories.forEach { sb.append("- ").append(it.text).append('\n') }
            sb.append('\n')
        }

        if (hits.isNotEmpty()) {
            sb.append("DOCUMENTS LOCAUX PERTINENTS:\n")
            hits.forEachIndexed { index, hit ->
                sb.append("[SOURCE ").append(index + 1).append(": ")
                    .append(hit.chunk.documentName).append("]\n")
                sb.append(hit.chunk.text).append("\n\n")
            }
        }

        if (history.isNotEmpty()) {
            sb.append("HISTORIQUE RÉCENT:\n")
            history.takeLast(12).forEach {
                sb.append(it.role.uppercase()).append(": ")
                    .append(it.text.take(1400)).append('\n')
            }
            sb.append('\n')
        }

        sb.append("UTILISATEUR: ").append(user).append("\nASSISTANT:")
        return sb.toString()
    }
}
