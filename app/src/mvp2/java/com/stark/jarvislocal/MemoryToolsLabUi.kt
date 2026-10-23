package com.stark.jarvislocal

import android.app.AlertDialog
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import com.stark.jarvislocal.memory.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Opt-in owner tools. No automatic prompt injection, external provider, or historical database access. */
internal class MemoryToolsLabUi(private val activity: Mvp2Activity, private val runtime: MvpRuntime, private val graph: Mvp2Graph) {
    private val policy = ProjectionPolicy(setOf(MemoryScope.GENERAL, MemoryScope.PREFERENCE))
    private val session get() = graph.intentSessionId
    fun show() {
        if (!visible()) return
        graph.memoryControls.resumeSession(session)
        AlertDialog.Builder(activity).setTitle("Rappel et export — laboratoire")
            .setItems(arrayOf("Rechercher un souvenir confirmé", "Exporter une sélection (JSON privé en clair)", "Examiner un export privé sans l'importer", "Suspendre le rappel de cette session")) { _, which ->
                if (!visible()) return@setItems
                when (which) { 0 -> ask(); 1 -> chooseExport(); 2 -> chooseImport(); 3 -> { graph.revokePendingMemory(); toast("Rappel et propositions suspendus.") } }
            }.setNegativeButton(android.R.string.cancel, null).show()
    }
    private fun ask() {
        val input = EditText(activity).apply { hint = "Question de recherche (512 octets maximum)"; isSaveEnabled = false }
        AlertDialog.Builder(activity).setTitle("Rappel lexical, sans LLM").setView(input)
            .setPositiveButton("Rechercher") { _, _ ->
                if (visible()) run { val query = MemoryQuery(session, input.text.toString()); val result = graph.memoryRecall.recall(query, policy)
                    onUi { if (result.status == RecallStatus.NEEDS_CONFIRMATION) {
                        AlertDialog.Builder(activity).setTitle("Rappel ambigu — choisir le contenu exact")
                            .setItems(result.items.map { it.content }.toTypedArray()) { _, i ->
                                if (visible()) run { val context = graph.memoryContext.assemble(query, policy, setOf(result.items[i].reference)); onUi { showText("Contexte mémoire, donnée non fiable", context.jsonData.ifBlank { "Rappel indisponible : ${context.status}" }) } }
                            }.setNegativeButton(android.R.string.cancel, null).show()
                    } else {
                        val explanation = result.items.joinToString("\n\n") { "${it.content}\nSource : ${it.reference.id} / r${it.reference.revision} ; mots : ${it.explanation.matchingTerms.joinToString()} ; ${it.explanation.source}" }
                        showText("Pourquoi ce rappel ?", explanation.ifBlank { "État : ${result.status}" })
                    } }
                }
            }.setNegativeButton(android.R.string.cancel, null).show()
    }
    private fun chooseExport() = run {
        val rows = graph.confirmedMemory.listOwned().filter { !it.suspended && it.scope != MemoryScope.SENSITIVE && (it.expiresAtMs == null || System.currentTimeMillis() < it.expiresAtMs) }
        onUi {
            if (rows.isEmpty()) { toast("Aucun souvenir autorisé à exporter."); return@onUi }
            val selected = mutableSetOf<MemoryReference>()
            AlertDialog.Builder(activity).setTitle("Sélection volontaire — 10 souvenirs maximum")
                .setMultiChoiceItems(rows.map { it.content }.toTypedArray(), null) { _, i, checked -> if (checked) selected.add(rows[i].reference) else selected.remove(rows[i].reference) }
                .setPositiveButton("Voir le JSON exact") { _, _ -> if (visible()) run {
                    val preview = graph.memoryExports.preview(session, selected.toSet(), policy)
                    onUi { AlertDialog.Builder(activity).setTitle("Confirmer l'export privé en clair")
                        .setMessage("${preview.json}\n\nSHA-256 : ${preview.sha256}\nFichier privé de cette application, non partagé. Aucun chiffrement applicatif. Annuler après écriture ne supprime pas un export déjà créé.")
                        .setPositiveButton("Créer ce fichier privé") { _, _ -> if (visible()) run {
                            val file = graph.memoryExports.writeConfirmed(preview.id, session, preview.sha256)
                            onUi { showText("Export privé créé", "${file.name}\nPas de partage automatique. L'aperçu d'import est disponible dans ce laboratoire.") }
                        } }.setNeutralButton("Choisir un fichier local") { _, _ -> if (visible()) activity.chooseMemoryExport(preview.references) }
                        .setNegativeButton(android.R.string.cancel, null).show() }
                } }.setNegativeButton(android.R.string.cancel, null).show()
        }
    }
    /** Returning from the picker grants no consent: re-read rows and show a fresh exact preview. */
    fun confirmLocalDestination(uri: android.net.Uri, selected: Set<MemoryReference>) {
        if (!visible() || uri.scheme != "content") return
        graph.memoryControls.resumeSession(session)
        run {
            val preview = graph.memoryExports.preview(session, selected, policy)
            onUi { AlertDialog.Builder(activity).setTitle("Confirmer l'export vers le fichier choisi")
                .setMessage("${preview.json}\n\nJSON en clair. SHA-256 : ${preview.sha256}\nUne erreur peut laisser un fichier incomplet ; aucun document du fournisseur ne sera supprimé automatiquement.")
                .setPositiveButton("Exporter ce contenu exact") { _, _ -> if (visible()) {
                    if (!runtime.inspect { check ->
                        check()
                        val destination = object : MemoryExportDestinationPort {
                            override fun isEmpty(): Boolean = activity.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { c ->
                                val column = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                                c.moveToFirst() && column >= 0 && !c.isNull(column) && c.getLong(column) == 0L
                            } ?: false
                            override fun openOutput() = requireNotNull(activity.contentResolver.openOutputStream(uri, "w"))
                            override fun openReadback() = requireNotNull(activity.contentResolver.openInputStream(uri))
                        }
                        val file = graph.memoryExports.writeConfirmed(preview.id, session, preview.sha256, destination = destination, checkActive = check)
                        onUi { showText("Export relu et vérifié", "Copie locale vérifiée par SHA-256. Copie privée conservée : ${file.name}") }
                    }) toast("Opération active : aucun export commencé.")
                } }.setNegativeButton(android.R.string.cancel, null).show() }
        }
    }
    private fun chooseImport() = run {
        val files = graph.memoryExports.exportedFiles()
        onUi {
            if (files.isEmpty()) { toast("Aucun export privé disponible."); return@onUi }
            AlertDialog.Builder(activity).setTitle("Aperçu isolé — aucune restauration automatique")
                .setItems(files.map { it.name }.toTypedArray()) { _, i -> if (visible()) run {
                    val preview = graph.memoryExports.previewImport(files[i], files[i].extension == "bin")
                    onUi { showText("Contenu importé non approuvé", preview.candidates.joinToString("\n\n") { "${it.content}\n${it.authority}\n${it.conflicts.joinToString()}" } + "\n\nAucune écriture mémoire. Chaque contenu exige une nouvelle confirmation propriétaire avant toute réintroduction.") }
                } }.setNegativeButton(android.R.string.cancel, null).show()
        }
    }
    private fun run(block: suspend () -> Unit) {
        if (!visible() || !runtime.inspect { check -> check(); block(); check() }) toast("Opération indisponible ou déjà active.")
    }
    private suspend fun onUi(block: () -> Unit) = withContext(Dispatchers.Main) { if (visible()) block() }
    private fun visible() = activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && graph.intentFacade.isEnabled
    private fun showText(title: String, text: String) { AlertDialog.Builder(activity).setTitle(title).setMessage(text).setPositiveButton(android.R.string.ok, null).show() }
    private fun toast(text: String) = Toast.makeText(activity, text, Toast.LENGTH_LONG).show()
}
