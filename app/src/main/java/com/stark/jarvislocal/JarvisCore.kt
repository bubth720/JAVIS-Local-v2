package com.stark.jarvislocal

class JarvisCore(
    private val db: LocalDatabase,
    private val rag: RagEngine,
    private val llm: LlamaLocalEngine,
    private val settingsStore: SettingsStore
) {
    suspend fun chat(userText: String): ChatMessage {
        val user = userText.trim()
        require(user.isNotEmpty())

        if (user.startsWith("/remember ", ignoreCase = true)) {
            val memory = user.substringAfter(' ').trim()
            db.addMemory(memory)
            db.addMessage(ChatMessage(role = "user", text = user))
            return ChatMessage(role = "assistant", text = "Mémoire enregistrée localement.")
                .also(db::addMessage)
        }

        if (user.equals("/docs", ignoreCase = true)) {
            db.addMessage(ChatMessage(role = "user", text = user))
            val docs = db.documentNames()
            val answer = if (docs.isEmpty()) "Aucun document local."
            else "Documents locaux :\n- " + docs.joinToString("\n- ")
            return ChatMessage(role = "assistant", text = answer).also(db::addMessage)
        }

        db.addMessage(ChatMessage(role = "user", text = user))

        val settings = settingsStore.load()
        val hits = rag.retrieve(user, settings.topKDocuments)
        val memories = rag.relevantMemories(user)
        val history = db.recentMessages(16).dropLast(1)
        val prompt = PromptBuilder.build(user, history, hits, memories)

        val raw = llm.generate(prompt, settings.systemPrompt, settings)
            .ifBlank { "Le modèle n'a produit aucune réponse." }

        return ChatMessage(
            role = "assistant",
            text = raw,
            source = hits.firstOrNull()?.chunk?.documentName
        ).also(db::addMessage)
    }
}
