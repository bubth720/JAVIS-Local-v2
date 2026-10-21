package com.stark.jarvislocal

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One Views UI; no Compose application, Binder pilot, avatar clock or speech auto-play. */
class Mvp2Activity : MvpActivity() {
    private val graph get() = (application as Mvp2Application).graph
    private lateinit var voiceStatus: TextView
    private lateinit var extensions: TextView
    private var pendingVoice: MvpModelManifest? = null
    private val voicePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val manifest = pendingVoice; pendingVoice = null
        val uri = result.data?.data
        if (result.resultCode == RESULT_OK && manifest != null && uri?.scheme == "content") {
            if (!runtime.importAuxiliary("Import Whisper privé (sans activation micro)…") { check ->
                graph.voiceModels.install(manifest, { requireNotNull(contentResolver.openInputStream(uri)) }, check)
            }) toast("Import refusé : autre opération active")
        }
    }
    override val compactUi = true
    override fun introText() = getString(R.string.mvp2_intro, applicationInfo.loadLabel(packageManager))
    override fun decorate(layout: LinearLayout) {
        layout.addView(ImageView(this).apply {
            setImageResource(R.drawable.avatar_static_v1)
            contentDescription = "Avatar V1 statique — illustration de démonstration, pas de personnalité activée"
            layoutParams = LinearLayout.LayoutParams((96 * resources.displayMetrics.density).toInt(), (128 * resources.displayMetrics.density).toInt())
        })
    }
    override fun addInteractionControls(layout: LinearLayout) {
        voiceStatus = TextView(this); layout.addView(voiceStatus)
        val extra = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = android.view.View.GONE }
        layout.addView(Button(this).apply { text = "Voix, souvenirs et capacités"; setOnClickListener {
            extra.visibility = if (extra.visibility == android.view.View.GONE) android.view.View.VISIBLE else android.view.View.GONE
        } })
        layout.addView(extra)
        fun button(label: String, action: () -> Unit) = Button(this).also { b ->
            b.text = label; b.setOnClickListener { try { action() } catch (failure: Exception) { toast(failure.message ?: "Action refusée") } }
            extra.addView(b)
        }
        button("1. Rechercher une voix locale (mode avion requis)") {
            if (!graph.speech.initialize()) toast("Active le mode avion, puis désactive le Wi-Fi avant le test de voix")
            renderSpeech()
        }
        button("2. Tester la voix hors ligne") {
            AlertDialog.Builder(this).setTitle("Vérification locale manuelle")
                .setMessage("Active le mode avion, désactive Wi-Fi et données. Aucune installation de voix automatique. Le test utilise une voix française déclarée locale ; cela ne prouve pas seul son fonctionnement hors ligne.")
                .setPositiveButton("Lire la phrase test") { _, _ ->
                    if (!graph.speech.testPhrase()) toast("Voix locale prête indisponible : lecture refusée")
                    renderSpeech()
                }.setNegativeButton(android.R.string.cancel, null).show()
        }
        button("3. Confirmer le test entendu sans réseau") {
            AlertDialog.Builder(this).setTitle("Confirmation utilisateur")
                .setMessage("Confirmes-tu avoir entendu la phrase avec Wi-Fi et données désactivés ? Cette attestation ne vaut que pour cette session de voix.")
                .setPositiveButton("Confirmer") { _, _ ->
                    try { graph.speech.confirmOfflineHeard() } catch (failure: Exception) { toast(failure.message ?: "Test requis") }
                    renderSpeech()
                }.setNegativeButton(android.R.string.cancel, null).show()
        }
        button("Lire la dernière réponse (300 caractères maximum)") {
            val answer = runtime.state.value.messages.lastOrNull { it.role == "assistant" }
            if (answer == null || !graph.speech.readReply(answer.text)) toast("Lecture refusée : test hors ligne requis, voix indisponible ou texte trop long")
            renderSpeech()
        }
        button("Arrêter et désactiver la lecture") { graph.speech.disable(); renderSpeech() }
        button("Ajouter un souvenir confirmé") {
            val input = EditText(this).apply { hint = "Souvenir explicite (maximum 1500 caractères)" }
            AlertDialog.Builder(this).setTitle("Confirmer l'enregistrement local").setView(input)
                .setMessage("Rien n'est mémorisé automatiquement. La mémoire SQLite et le message de confirmation sont des écritures séparées.")
                .setPositiveButton("Enregistrer") { _, _ ->
                    val memory = input.text.toString().trim()
                    if (memory.isBlank() || memory.length > 1500) toast("Souvenir vide ou trop long")
                    else if (!runtime.send("/remember $memory", graph.settings.load().copy(speakReplies = false))) toast("Opération déjà active")
                }.setNegativeButton(android.R.string.cancel, null).show()
        }
        button("Afficher les souvenirs locaux") {
            lifecycleScope.launch {
                try {
                    val text = withContext(Dispatchers.IO) { graph.db.allMemories().take(20).joinToString("\n\n") { it.text } }
                    AlertDialog.Builder(this@Mvp2Activity).setTitle("Souvenirs confirmés").setMessage(text.ifBlank { "Aucun souvenir." })
                        .setPositiveButton(android.R.string.ok, null).show()
                } catch (_: Exception) { toast("Lecture SQLite refusée") }
            }
        }
        button("Importer un modèle Whisper (sans activation micro)") { voiceImportDialog() }
        button(graph.microphoneReason) { toast(graph.microphoneReason) }.isEnabled = graph.microphoneEnabled
        extensions = TextView(this).apply { text = disabledExtensions.joinToString(" · ") { "${it.capability} : désactivé" } }
        extra.addView(extensions)
        lifecycleScope.launch { graph.speech.status.collect { renderSpeech() } }
        renderSpeech()
    }
    override fun confirmSend(text: String, send: () -> Unit) {
        if (text.trim().startsWith("/remember ", true)) {
            AlertDialog.Builder(this).setTitle("Confirmer ce souvenir")
                .setMessage("Enregistrer explicitement ce texte dans la mémoire locale ?")
                .setPositiveButton("Enregistrer") { _, _ -> send() }.setNegativeButton(android.R.string.cancel, null).show()
        } else send()
    }
    override fun renderInteraction(state: MvpState) { if (::voiceStatus.isInitialized) renderSpeech() }
    private fun renderSpeech() {
        if (!::voiceStatus.isInitialized) return
        voiceStatus.text = "Voix Android : ${graph.speech.status.value}. " +
            if (graph.speech.verifiedByUser) "Test hors ligne attesté par l'utilisateur ; lecture manuelle autorisée." else "Lecture des réponses désactivée : vérification manuelle requise."
    }
    private fun voiceImportDialog() {
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val values = listOf("Provenance publique", "Licence", "SHA-256 attendu", "Taille exacte en octets").map { hint ->
            EditText(this).also { it.hint = hint; container.addView(it) }
        }
        AlertDialog.Builder(this).setTitle("Whisper GGML local >50 Mio, ≤160 Mio").setView(container)
            .setMessage("Import vérifié ≠ modèle qualifié. Le microphone reste désactivé. Aucun modèle téléchargé.")
            .setPositiveButton("Choisir") { _, _ ->
                try {
                    val m = MvpModelManifest(values[0].text.toString().trim(), values[1].text.toString().trim(),
                        values[2].text.toString().trim(), values[3].text.toString().toLong())
                    m.validate(MvpImportPolicy(50L * 1024 * 1024, 160L * 1024 * 1024))
                    pendingVoice = m
                    voicePicker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"; putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    })
                } catch (failure: Exception) { toast(failure.message ?: "Import refusé") }
            }.setNegativeButton(android.R.string.cancel, null).show()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        pendingVoice?.let { outState.putStringArray("voiceManifest", arrayOf(it.provenance, it.license, it.sha256, it.bytes.toString())) }
        super.onSaveInstanceState(outState)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        savedInstanceState?.getStringArray("voiceManifest")?.let { row ->
            if (row.size == 4) pendingVoice = row[3].toLongOrNull()?.let { MvpModelManifest(row[0], row[1], row[2], it) }
        }
        super.onCreate(savedInstanceState)
    }
    override fun onStop() {
        graph.speech.stop()
        if (!isChangingConfigurations) graph.speech.disable()
        super.onStop()
    }
}
