package com.stark.jarvislocal

data class ChatMessage(
    val id: Long = 0,
    val role: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val source: String? = null
)

data class MemoryItem(
    val id: Long = 0,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class DocumentChunk(
    val id: Long = 0,
    val documentId: Long,
    val documentName: String,
    val chunkIndex: Int,
    val text: String
)

data class RetrievalHit(val chunk: DocumentChunk, val score: Double)

data class JarvisSettings(
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val contextSize: Int = 4096,
    val maxTokens: Int = 512,
    val topKDocuments: Int = 5,
    val speakReplies: Boolean = false
) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT =
            "Tu es JARVIS, un assistant personnel local. Réponds en français sauf demande contraire. Sois précis, concis et utile. Utilise les DOCUMENTS LOCAUX et la MÉMOIRE quand ils sont pertinents. N'invente pas leur contenu. /no_think"
    }
}
