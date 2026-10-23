package com.stark.jarvislocal

import android.app.AlertDialog
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import com.stark.jarvislocal.memory.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Explicit owner UI only, independent from historical memories. Nothing is inferred or imported. */
internal class MemoryLabUi(private val activity: Mvp2Activity, private val runtime: MvpRuntime,
    private val store: ConfirmedMemoryStore, private val workflow: MemoryApprovalWorkflow,
    private val sessionId: String) {
    fun show() = editor(null)
    private fun editor(record: MemoryRecord?) {
        val field = EditText(activity).apply { hint = "Contenu exact (1500 caractères maximum)"; isSaveEnabled = false; setText(record?.content.orEmpty()) }
        val dialog = AlertDialog.Builder(activity).setTitle("Mémoire de laboratoire isolée")
            .setMessage("Aucun souvenir historique copié. Seul le contenu affiché puis confirmé sera enregistré dans ce laboratoire. Après une interruption, résultat potentiellement inconnu : consulte la liste avant de réessayer.")
            .setView(field)
            .setPositiveButton("Prévisualiser") { _, _ -> prepare(field.text.toString(), record) }
            .setNegativeButton(android.R.string.cancel, null)
        if (record == null) dialog.setNeutralButton("Consulter / éditer") { _, _ -> list() }
        else dialog.setNeutralButton("Effacer") { _, _ -> confirmDelete(record) }
        dialog.show()
    }
    private fun prepare(text: String, old: MemoryRecord?) {
        if (!runtime.inspect { check ->
            check()
            val candidate = workflow.propose(sessionId, text, old?.scope ?: MemoryScope.GENERAL,
                old?.preferenceKey, old?.reference, old?.expiresAtMs)
            check()
            withContext(Dispatchers.Main) {
                check()
                if (!visible()) return@withContext
                AlertDialog.Builder(activity).setTitle("Confirmer ce contenu exact")
                    .setMessage("${candidate.content}\n\nPortée : ${candidate.scope}. Aucune autorisation d’outil ni mémorisation automatique. En cas d’interruption après confirmation, consulte la liste : une annulation ne prouve pas un retour arrière.")
                    .setPositiveButton("Confirmer l’enregistrement") { _, _ ->
                        if (!visible()) return@setPositiveButton
                        if (!runtime.confirmedMemoryAction { confirm ->
                            confirm()
                            val result = workflow.decide(candidate.id, sessionId, candidate.content,
                                if (candidate.replaces == null) MemoryDecision.ACCEPT else MemoryDecision.REPLACE)
                            confirm(); withContext(Dispatchers.Main) { confirm(); if (visible()) toast(when (result) {
                                is MemoryApprovalResult.Confirmed -> "Souvenir de laboratoire confirmé."
                                is MemoryApprovalResult.Refused -> "Refus : ${result.code}"
                                is MemoryApprovalResult.Conflict -> "Conflit : nouvel aperçu nécessaire."
                                MemoryApprovalResult.Rejected -> "Souvenir refusé."
                            }) }
                        }) toast("Opération active : aucun accord appliqué")
                    }.setNegativeButton("Refuser") { _, _ -> workflow.decide(candidate.id, sessionId, candidate.content, MemoryDecision.REJECT) }
                    .show()
            }
        }) toast("Opération active : aperçu refusé")
    }
    private fun list() {
        if (!runtime.inspect { check ->
            check(); val records = store.listOwned().take(20); check()
            withContext(Dispatchers.Main) {
                check(); if (!visible()) return@withContext
                if (records.isEmpty()) toast("Aucun souvenir de laboratoire confirmé.")
                else AlertDialog.Builder(activity).setTitle("Souvenirs de laboratoire")
                    .setItems(records.map { it.content }.toTypedArray()) { _, index -> editor(records[index]) }
                    .setNegativeButton(android.R.string.cancel, null).show()
            }
        }) toast("Opération active : lecture refusée")
    }
    private fun confirmDelete(record: MemoryRecord) {
        AlertDialog.Builder(activity).setTitle("Effacer ce souvenir de laboratoire ?").setMessage(record.content)
            .setPositiveButton("Effacer") { _, _ ->
                if (!visible()) return@setPositiveButton
                if (!runtime.confirmedMemoryAction { check ->
                    check(); store.deleteOwned(record.reference); check()
                    withContext(Dispatchers.Main) { check(); if (visible()) toast("Souvenir de laboratoire effacé. Historique préservé.") }
                }) toast("Opération active : aucun effacement appliqué")
            }.setNegativeButton(android.R.string.cancel, null).show()
    }
    private fun visible() = activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    private fun toast(text: String) = Toast.makeText(activity, text, Toast.LENGTH_LONG).show()
}
