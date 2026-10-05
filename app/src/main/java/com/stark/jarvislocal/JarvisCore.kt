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

        // Pour une recherche factuelle dans un PDF, on montre d'abord la preuve exacte
        // plutôt que d'attendre une inférence longue.
        val evidence = rag.bestEvidence(user)
        if (evidence != null && looksLikeDocumentLookup(user)) {
            val answer = buildString {
                append("J'ai retrouvé ce passage dans « ")
                append(evidence.documentName)
                append(" » :\n\n")
                append(evidence.text)
                append("\n\nSi tu veux, je peux ensuite l'interpréter avec le modèle local.")
            }
            return ChatMessage(
                role = "assistant",
                text = answer,
                source = evidence.documentName
            ).also(db::addMessage)
        }

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

    private fun looksLikeDocumentLookup(input: String): Boolean {
        val s = normalize(input)
        return s.contains("quel") ||
            s.contains("quelle") ||
            s.contains("combien") ||
            s.contains("chrono") ||
            s.contains("score") ||
            s.contains("resultat") ||
            s.contains("résultat") ||
            s.contains("document") ||
            s.contains("pdf")
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
