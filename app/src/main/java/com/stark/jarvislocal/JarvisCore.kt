package com.stark.jarvislocal

import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex

class JarvisCore(
    private val db: LocalDatabase,
    private val rag: RagEngine,
    private val llm: LocalLanguageModel,
    private val settingsStore: SettingsStore
) {
    private val conversationLock = Mutex()
    private val inferenceSessionId = UUID.randomUUID().toString()
    init { llm.openSession(inferenceSessionId) }

    suspend fun chat(userText: String): ChatMessage {
        check(conversationLock.tryLock()) { "JARVIS est occupé. Réessaie après la réponse en cours." }
        return try { chatExclusive(userText) }
        finally { conversationLock.unlock() }
    }

    /** Explicit text lab: same controller/session/lock, without commands, RAG, SQLite or persona. */
    internal suspend fun chatVolatile(userText: String, history: List<ChatMessage>): ChatMessage {
        check(conversationLock.tryLock()) { "JARVIS est occupé. Réessaie après la réponse en cours." }
        return try {
            currentCoroutineContext().ensureActive()
            val user = userText.trim()
            require(user.isNotEmpty())
            val settings = settingsStore.load()
            val prompt = PromptBuilder.prepare(user, history, emptyList(), emptyList(), settings)
            inferPrepared(user, prompt, settings, requireText = true)
        } finally { conversationLock.unlock() }
    }

    private suspend fun chatExclusive(userText: String): ChatMessage {
        currentCoroutineContext().ensureActive()
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
        val prompt = PromptBuilder.prepare(user, history, hits, memories, settings)

        return inferPrepared(user, prompt, settings).also(db::addMessage)
    }

    private suspend fun inferPrepared(user: String, prompt: PromptBuilder.Prepared,
        settings: JarvisSettings, requireText: Boolean = false): ChatMessage {
        val request = InferenceRequest(inferenceSessionId, UUID.randomUUID().toString(), settings.systemPrompt,
            user, prompt.context, prompt.sources, GenerationLimits(settings.contextSize, settings.maxTokens))
        val response = llm.infer(request)
        if (!response.nativeDone) throw InferenceRejected(InferenceRefusal.RELEASE_UNCONFIRMED)
        if (response.sessionId != request.sessionId || response.requestId != request.requestId)
            throw InferenceRejected(InferenceRefusal.STALE_RESPONSE)
        if (requireText) check(response.text.isNotBlank()) { "Le LLM local n’a produit aucun texte." }
        val raw = response.text.ifBlank { "Le modèle n'a produit aucune réponse." }

        currentCoroutineContext().ensureActive()

        return ChatMessage(
            role = "assistant",
            text = raw,
            source = prompt.documentNames.takeIf { it.isNotEmpty() }?.joinToString(", ")
        )
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
                localTimeReply()

            else -> null
        }
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9 -]+"), "")
            .trim()
}

internal fun localTimeReply(
    now: Date = Date(),
    timeZone: TimeZone = TimeZone.getDefault()
): String {
    // java.time requires API 26; use a fresh formatter for API 24/25 and thread safety.
    val formatter = SimpleDateFormat("HH:mm", Locale.ROOT).apply {
        this.timeZone = timeZone
    }
    return "Il est " + formatter.format(now) + "."
}
