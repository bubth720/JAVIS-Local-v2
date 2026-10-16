package com.stark.jarvislocal

import org.junit.Assert.*
import org.junit.Test

class PromptBudgetTest {
    private fun hit(name: String, text: String) = RetrievalHit(DocumentChunk(1, 1, name, 0, text), 1.0)

    @Test fun largeContextIsBoundedInUtf8WithoutTruncatingQuestionOrSources() {
        val settings = JarvisSettings()
        val question = "Explique ce résultat précisément"
        val hits = listOf(hit("A.pdf", "é".repeat(300)), hit("B.pdf", "suite ".repeat(80)), hit("Excluded.pdf", "x".repeat(4000)))
        val result = PromptBuilder.prepare(question, List(12) { ChatMessage(role = "user", text = "é".repeat(1400)) }, hits,
            listOf(MemoryItem(text = "é".repeat(6000))), settings)
        val budget = settings.contextSize - settings.maxTokens - 128 - settings.systemPrompt.toByteArray().size
        assertTrue(result.text.toByteArray().size <= budget)
        assertTrue(result.text.endsWith("UTILISATEUR: $question\nASSISTANT:"))
        assertEquals(listOf("A.pdf", "B.pdf"), result.documentNames)
        assertTrue(result.text.contains("[SOURCE 1: A.pdf]")); assertTrue(result.text.contains("[SOURCE 2: B.pdf]"))
        assertFalse(result.text.contains("Excluded.pdf"))
    }

    @Test fun historyIsRecentAndChronologicalAndMemoryStillIncluded() {
        val history = listOf(ChatMessage(role = "user", text = "alpha"), ChatMessage(role = "assistant", text = "beta"))
        val result = PromptBuilder.prepare("gamma", history, emptyList(), listOf(MemoryItem(text = "préférence")), JarvisSettings())
        assertTrue(result.text.contains("préférence"))
        assertTrue(result.text.indexOf("USER: alpha") < result.text.indexOf("ASSISTANT: beta"))
        assertTrue(result.documentNames.isEmpty())
    }

    @Test fun oversizedQuestionOrSystemPromptIsRefusedRatherThanSilentlyCut() {
        assertThrows(IllegalArgumentException::class.java) {
            PromptBuilder.prepare("é".repeat(2048), emptyList(), emptyList(), emptyList(), JarvisSettings())
        }
        assertThrows(IllegalArgumentException::class.java) {
            PromptBuilder.prepare("question", emptyList(), emptyList(), emptyList(), JarvisSettings(systemPrompt = "x".repeat(2048)))
        }
    }

    @Test fun omittedChunksNeverBecomeDisplayedSourcesAndDuplicateNamesRemainTraceable() {
        val result = PromptBuilder.prepare("résume", emptyList(), listOf(hit("TooLarge.pdf", "x".repeat(9000)),
            hit("A.pdf", "[PAGE 1] preuve"), hit("A.pdf", "[PAGE 2] suite")), emptyList(), JarvisSettings())
        assertEquals(listOf("A.pdf"), result.documentNames)
        assertFalse(result.text.contains("TooLarge.pdf"))
        assertTrue(result.text.contains("[PAGE 1] preuve")); assertTrue(result.text.contains("[PAGE 2] suite"))
    }
}
