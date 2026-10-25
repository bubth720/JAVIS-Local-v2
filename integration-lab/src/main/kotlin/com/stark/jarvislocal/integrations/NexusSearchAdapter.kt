package com.stark.jarvislocal.integrations

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import fr.jarvis.nexus.api.*
import fr.jarvis.nexus.rag.*
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.util.UUID

class IntegrationRefused(val code: String) : IllegalStateException(code)
data class Correlation(val sessionId: String, val requestId: String, val generation: Long = 0) {
    init { listOf(sessionId, requestId).forEach { require(UUID.fromString(it).toString() == it) }; require(generation >= 0) }
}
/** Supplied by a trusted CORE host. Never reconstructed from documents/tool arguments. */
fun interface HostAccessPort { fun allows(correlation: Correlation, capability: String): Boolean }
internal fun strictBytes(text: String): ByteArray {
    val b = Charsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT).encode(CharBuffer.wrap(text))
    return ByteArray(b.remaining()).also { b.get(it) }
}
data class SourceSearchRequest(val correlation: Correlation, val question: String, val scope: SearchScope,
    val budgetBytes: Int = 8192) {
    init { require(question.isNotBlank() && strictBytes(question).size <= 1024); require(budgetBytes in 256..16000) }
}
data class CitationBinding(val correlation: Correlation, val bundle: ContextBundle)
interface KnowledgeReadPort { fun search(request: SourceSearchRequest): CitationBinding }
interface SourceSearchPort : KnowledgeReadPort

/** JVM adapter over the actual delivered NEXUS port; nothing is linked into an Android APK. */
class NexusSearchAdapter(private val nexus: RagContextPort,
    private val access: HostAccessPort = HostAccessPort { _, _ -> false }) : SourceSearchPort {
    override fun search(request: SourceSearchRequest): CitationBinding {
        fun authorized() { if (!access.allows(request.correlation, "nexus.read")) throw IntegrationRefused("CORE_DENIED_OR_STALE_SESSION") }
        authorized()
        val scope = request.scope.copy(collectionIds = request.scope.collectionIds.toSet(), sourceIds = request.scope.sourceIds?.toSet())
        val bundle = requireOk(nexus.retrieve(KnowledgeQuery(request.question), scope, ContextLimits(request.budgetBytes, 5, 5)))
        for (item in bundle.items) {
            authorized()
            if (item.citations.isEmpty()) throw IntegrationRefused("CITATION_REQUIRED")
            val resolved = item.citations.map { requireOk(nexus.resolve(it, scope)) }
            val valid = when (item.grouping) {
                EvidenceGrouping.SINGLE -> resolved.size == 1 && resolved.single() == item.text
                EvidenceGrouping.IDENTICAL_TEXT -> resolved.all { it == item.text }
                EvidenceGrouping.CONTIGUOUS_FRAGMENTS -> resolved.joinToString("") == item.text
            }
            if (!valid) throw IntegrationRefused("CITATION_TEXT_MISMATCH")
        }
        if (strictBytes(bundle.asUntrustedJson()).size > request.budgetBytes) throw IntegrationRefused("CONTEXT_BYTE_BUDGET")
        authorized()
        return CitationBinding(request.correlation, bundle)
    }
    private fun <T> requireOk(result: CatalogResult<T>): T = when (result) {
        is CatalogResult.Ok -> result.value
        is CatalogResult.Rejected -> throw IntegrationRefused("NEXUS_${result.error}")
    }
}

/** Data-only envelope; no SYSTEM prompt, tokenizer, native admission, memory lookup, or generation API. */
class ContextAssembler(private val access: HostAccessPort = HostAccessPort { _, _ -> false }) {
    fun assemble(binding: CitationBinding, expected: Correlation, budgetBytes: Int): String {
        require(budgetBytes in 256..20000)
        if (!access.allows(expected, "nexus.read")) throw IntegrationRefused("CORE_DENIED_OR_STALE_SESSION")
        require(binding.bundle.items.size in 1..5 && binding.bundle.items.all { it.text.length <= 20000 && it.citations.size in 1..20 })
        if (binding.correlation != expected) throw IntegrationRefused("RESPONSE_CORRELATION_MISMATCH")
        val root = JsonObject().apply {
            addProperty("format", "jarvis.core.context-lab/1")
            addProperty("untrusted_data", true)
            addProperty("session_id", expected.sessionId); addProperty("request_id", expected.requestId)
            addProperty("generation", expected.generation)
            add("documentary", JsonParser.parseString(binding.bundle.asUntrustedJson()))
            add("confirmed_memory", com.google.gson.JsonNull.INSTANCE)
            addProperty("generation_available", false)
        }.toString()
        if (strictBytes(root).size > budgetBytes) throw IntegrationRefused("CONTEXT_BYTE_BUDGET")
        if (!access.allows(expected, "nexus.read")) throw IntegrationRefused("CORE_DENIED_OR_STALE_SESSION")
        return root
    }
}
