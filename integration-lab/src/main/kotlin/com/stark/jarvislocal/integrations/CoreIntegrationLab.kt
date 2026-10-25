package com.stark.jarvislocal.integrations

import fr.jarvis.nexus.api.*
import fr.jarvis.nexus.importing.*
import fr.jarvis.nexus.rag.*
import fr.jarvis.nexus.store.NexusDatabase
import fr.jarvis.services.modules.data.Data
import fr.jarvis.services.modules.documents.Documents
import fr.jarvis.services.modules.guard.Guard
import fr.jarvis.services.platform.JdbcDatabase
import fr.jarvis.services.shared.storage.Store
import fr.jarvis.services.tools.*
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.file.Files
import java.util.UUID

/** Explicit synthetic host. Not composed into AppGraph/MVP or accessible from model/tool text. */
internal class SyntheticLab : AutoCloseable {
    val root = Files.createTempDirectory("jarvis-core03-synthetic-")
    val nexusDb = NexusDatabase.open(root.resolve("knowledge.sqlite"))
    val catalog = KnowledgeCatalogService(nexusDb, AccessPolicy { true })
    val collection = ok(catalog.createCollection("synthetic-core03"))
    val rag = LocalRagService(nexusDb, catalog) { SearchScope(setOf(collection.id)) }
    val scope = SearchScope(setOf(collection.id))
    val session = UUID.randomUUID().toString()
    @Volatile var authorized = true
    @Volatile var generation = 0L
    val access = HostAccessPort { c, capability -> authorized && c.sessionId == session && c.generation == generation && capability in setOf("nexus.read", "data.read", "documents.read", "documents.export", "confirm") }
    private val servicesDb = JdbcDatabase(root.resolve("services.sqlite"))
    val store = Store(servicesDb)
    private val guard = Guard(root.resolve("documents"), store)
    val host = Guard.Context("synthetic-core03", setOf("data.read", "documents.read", "documents.export", "confirm"))
    val tools = ToolRegistry(store)
    val approvals = ApprovalGate(store, guard)
    val dispatcher: ToolDispatcher
    val services: ServicesToolAdapter
    init {
        val documents = Documents(guard, store, null)
        ServicesTools.register(tools, documents, Data(), guard)
        // One additional binding in the existing SERVICES registry, using its real CSV writer.
        tools.register(ServicesTools.descriptor("documents.csv.export", "documents", "documents.export", true,
            "file_digest", "path", "text")) { ctx, args, task ->
            documents.export(ctx, args.getValue("path"), "csv", args.getValue("text"), task)
        }
        dispatcher = ToolDispatcher(tools, guard, approvals)
        services = ServicesToolAdapter(tools, dispatcher, approvals, host, access)
    }
    fun correlation() = Correlation(session, UUID.randomUUID().toString(), generation)
    fun addText(text: String): java.nio.file.Path {
        val bytes = strictBytes(text)
        val source = Files.createDirectories(root.resolve("sources")).resolve("${UUID.randomUUID()}.txt")
        Files.write(source, bytes)
        // Real bounded UTF-8 text decoding through NEXUS's delivered parser port; no fake search engine.
        val parser = ParserPort { _, data, stop ->
            stop.check()
            val decoded = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(data)).toString()
            listOf(ExtractedPage(1, decoded))
        }
        val importer = ImportCoordinator(nexusDb, catalog, ParserRegistry(mapOf("txt" to parser)))
        val job = ok(importer.enqueue(ImportSource(collection.id, source.toUri().toString(), "source.txt", "Synthetic source"), bytes.inputStream()))
        Files.newInputStream(source).use { check(ok(importer.process(job.id, it)).state == ImportState.COMPLETED) }
        return source
    }
    override fun close() {
        dispatcher.close()
        // Demo/tests await operations before disposal; this is not a native engine release attestation.
        servicesDb.close(); nexusDb.close()
        Files.walk(root).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
    }
    companion object {
        fun <T> ok(result: CatalogResult<T>): T = (result as CatalogResult.Ok<T>).value
    }
}
fun main() {
    SyntheticLab().use { lab ->
        lab.addText("Science knowledge from a synthetic local source.")
        val request = SourceSearchRequest(lab.correlation(), "science", lab.scope)
        val evidence = NexusSearchAdapter(lab.rag, lab.access).search(request)
        val context = ContextAssembler(lab.access).assemble(evidence, request.correlation, 12000)
        check(context.contains("sourceSha256"))
        val csv = lab.services.prepare(lab.correlation(), "data.csv.normalize", "1.0.0", mapOf("csv" to "label,value\n alpha ,1\n alpha ,1\n"))
        val normalized = lab.services.executeReadOnly(csv.id, csv.digest).await()
        check(normalized.status == ToolDispatcher.Status.SUCCESS && Data().csv(normalized.value as String).rows == listOf(listOf("alpha", "1")))
        val action = lab.services.prepare(lab.correlation(), "documents.docx.export", "1.0.0", mapOf("path" to "synthetic.docx", "text" to "Synthetic explicit fixture approval"))
        try { lab.services.executeReadOnly(action.id, action.digest); error("Unexpected mutation") }
        catch (e: IntegrationRefused) { check(e.code == "CONFIRMATION_REQUIRED") }
        // This is an explicitly synthetic fixture decision, never an approval inferred from model text.
        val output = lab.services.confirmAndExecute(action.id, action.digest).await()
        check(output.status == ToolDispatcher.Status.SUCCESS)
        check(Documents.readDocx(Files.readAllBytes(lab.root.resolve("documents/synthetic.docx"))).trim() == "Synthetic explicit fixture approval")
        check(lab.nexusDb.integrityCheck())
        println("CORE03 JVM: real_nexus_citations=${evidence.bundle.items.flatMap { it.citations }.size} csv_tool=SUCCESS mutation_without_approval=REFUSED docx_readback=VERIFIED android_connected=false native_inference=false")
        val workflow = DocumentToolWorkflow(NexusSearchAdapter(lab.rag, lab.access), lab.services,
            WorkflowReceipts(lab.store), lab.access)
        val preview = workflow.prepare(lab.correlation(), "science", lab.scope,
            "name,value\na,0.10\nb,0.20\n", "value", "fusion.csv", DocumentToolWorkflow.Format.CSV,
            System.nanoTime() / 1_000_000 + 20_000)
        check(preview.statistics["sum"] == "0.30")
        check(!Files.exists(lab.root.resolve("documents/fusion.csv")))
        workflow.approve(preview.correlation, preview.review.digest) // SYNTHETIC owner consent only
        check(workflow.finish(preview.correlation).status == ToolDispatcher.Status.SUCCESS)
        check(Files.readString(lab.root.resolve("documents/fusion.csv")) == preview.normalizedCsv)
        println("FUSION01 JVM: source_resolved=true csv_exact_sum=0.30 export_readback=VERIFIED consent=FIXTURE android_connected=false native_inference=false")
    }
}
