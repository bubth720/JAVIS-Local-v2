package com.stark.jarvislocal

import java.text.Normalizer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class JarvisCore(
    private val db: LocalDatabase,
    private val rag: RagEngine,
    private val llm: LlamaLocalEngine,
    private val settingsStore: SettingsStore
) {
    suspend fun chat(userText: String): ChatMessage {
        val user = userText.trim()
        require(user.isNotEmpty())

        fastLocalReply(user)?.let { answer ->
            db.addMessage(ChatMessage(role = "user", text = user))
            return ChatMessage(role = "assistant", text = answer).also(db::addMessage)
        }

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
        val history = db.recentMessages(12).dropLast(1)
        val prompt = PromptBuilder.build(user, history, hits, memories)

        val raw = llm.generate(prompt, settings.systemPrompt, settings)
            .ifBlank { "Le modèle n'a produit aucune réponse." }

        return ChatMessage(
            role = "assistant",
            text = raw,
            source = hits.firstOrNull()?.chunk?.documentName
        ).also(db::addMessage)
    }

    private fun fastLocalReply(input: String): String? {
        val s = normalize(input)
        return when {
            s in setOf("bonjour", "salut", "hello", "bonsoir", "coucou") ->
                "Bonjour. JARVIS est opérationnel et fonctionne entièrement en local."

            s in setOf("merci", "merci jarvis") ->
                "Avec plaisir."

            s == "qui es tu" || s == "qui es-tu" ->
                "Je suis JARVIS, ton assistant local. Mon modèle, ma mémoire et tes documents restent sur cet appareil."

            s.contains("quelle heure") || s == "heure" ->
                "Il est " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + "."

            else -> null
        }
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9 -]+"), "")
            .trim()
}
