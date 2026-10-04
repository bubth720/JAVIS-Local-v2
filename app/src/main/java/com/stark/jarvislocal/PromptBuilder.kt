package com.stark.jarvislocal

object PromptBuilder {
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
