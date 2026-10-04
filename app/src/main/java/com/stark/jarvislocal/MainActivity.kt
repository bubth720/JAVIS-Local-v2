package com.stark.jarvislocal

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {
    private val graph by lazy { (application as JarvisApplication).graph }

    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var modelText: TextView
    private lateinit var history: TextView

    private val pdfPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importPdf(uri)
    }

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) toggleMic() else toast("Permission micro refusée.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        input = findViewById(R.id.input)
        status = findViewById(R.id.statusText)
        modelText = findViewById(R.id.modelText)
        history = findViewById(R.id.history)
        history.movementMethod = ScrollingMovementMethod()

        findViewById<Button>(R.id.send).setOnClickListener { send() }
        findViewById<Button>(R.id.importPdf).setOnClickListener {
            pdfPicker.launch(arrayOf("application/pdf"))
        }
        findViewById<Button>(R.id.mic).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                micPermission.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                toggleMic()
            }
        }
        findViewById<Button>(R.id.memory).setOnClickListener { showMemoryDialog() }
        findViewById<Button>(R.id.settings).setOnClickListener { showSettingsDialog() }

        refreshHistory()
        prepareModels()
    }

    private fun prepareModels() {
        status.text = "PRÉPARATION…"
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    graph.models.ensureBundledModels { label ->
                        runOnUiThread {
                            status.text = label
                            modelText.text = label
                        }
                    }
                }
                refreshModelStatus()
                status.text = "CORE READY"
                if (graph.db.recentMessages(1).isEmpty()) {
                    graph.db.addMessage(
                        ChatMessage(
                            role = "assistant",
                            text = "Noyau local initialisé. LLM, mémoire, PDF/RAG et voix hors ligne sont prêts."
                        )
                    )
                    refreshHistory()
                }
            } catch (e: Exception) {
                status.text = "ERREUR MODÈLE"
                modelText.text = "Modèles indisponibles : " + (e.message ?: "erreur inconnue")
            }
        }
    }

    private fun send(text: String? = null) {
        val q = (text ?: input.text.toString()).trim()
        if (q.isEmpty()) return
        input.setText("")
        status.text = "RÉFLEXION…"

        lifecycleScope.launch {
            try {
                val answer = withContext(Dispatchers.IO) { graph.core.chat(q) }
                refreshHistory()
                if (graph.settings.load().speakReplies) {
                    graph.tts.speak(answer.text)
                }
            } catch (e: Exception) {
                graph.db.addMessage(
                    ChatMessage(
                        role = "assistant",
                        text = "Erreur locale : " + (e.message ?: "erreur inconnue")
                    )
                )
                refreshHistory()
            } finally {
                status.text = "CORE READY"
            }
        }
    }

    private fun importPdf(uri: Uri) {
        status.text = "INDEXATION PDF…"
        lifecycleScope.launch {
            try {
                val info = fileInfo(uri)
                val count = withContext(Dispatchers.IO) {
                    graph.pdf.import(uri, info.first, info.second)
                }
                graph.db.addMessage(
                    ChatMessage(
                        role = "assistant",
                        text = "PDF « " + info.first + " » indexé localement : " + count + " passages."
                    )
                )
                refreshHistory()
            } catch (e: Exception) {
                toast("PDF : " + (e.message ?: "erreur"))
            } finally {
                status.text = "CORE READY"
            }
        }
    }

    private fun toggleMic() {
        val audio = File(cacheDir, "jarvis_input.wav")
        val button = findViewById<Button>(R.id.mic)

        if (!graph.recorder.isRecording()) {
            graph.recorder.start(audio)
            button.text = "■ Stop"
            status.text = "ÉCOUTE…"
        } else {
            graph.recorder.stop()
            button.text = "🎙 Voix"
            status.text = "TRANSCRIPTION…"

            lifecycleScope.launch {
                try {
                    val text = withContext(Dispatchers.IO) {
                        graph.whisper.transcribe(audio.absolutePath)
                    }
                    if (text.isNotBlank()) {
                        input.setText(text)
                        send(text)
                    } else {
                        toast("Aucune parole reconnue.")
                    }
                } catch (e: Exception) {
                    toast("Voix : " + (e.message ?: "erreur"))
                } finally {
                    status.text = "CORE READY"
                }
            }
        }
    }

    private fun showMemoryDialog() {
        val edit = EditText(this).apply {
            hint = "Ex. Mon projet principal s'appelle Orion."
        }
        val current = graph.db.allMemories().take(12)
            .joinToString("\n") { "• " + it.text }
            .ifBlank { "Aucune mémoire." }

        AlertDialog.Builder(this)
            .setTitle("Mémoire locale")
            .setMessage(current)
            .setView(edit)
            .setPositiveButton("Ajouter") { _, _ ->
                val value = edit.text.toString().trim()
                if (value.isNotEmpty()) {
                    graph.db.addMemory(value)
                    toast("Mémoire enregistrée.")
                }
            }
            .setNegativeButton("Fermer", null)
            .show()
    }

    private fun showSettingsDialog() {
        val settings = graph.settings.load()
        val edit = EditText(this).apply {
            setText(settings.systemPrompt)
            minLines = 8
            maxLines = 14
        }

        AlertDialog.Builder(this)
            .setTitle("Personnalité / instruction système")
            .setView(edit)
            .setPositiveButton("Enregistrer") { _, _ ->
                graph.settings.save(settings.copy(systemPrompt = edit.text.toString()))
            }
            .setNeutralButton(if (settings.speakReplies) "Couper voix" else "Activer voix") { _, _ ->
                graph.settings.save(settings.copy(speakReplies = !settings.speakReplies))
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun refreshHistory() {
        val text = graph.db.recentMessages(80).joinToString("\n\n") { message ->
            val who = if (message.role == "user") "VOUS" else "JARVIS"
            who + "\n" + message.text +
                (message.source?.let { "\nSource locale : " + it } ?: "")
        }
        history.text = text
        history.post { history.scrollTo(0, history.layout?.height ?: 0) }
    }

    private fun refreshModelStatus() {
        val llm = if (graph.models.hasLlm()) {
            "LLM ✓ " + (graph.models.llmFile.length() / 1024 / 1024) + " Mo"
        } else "LLM absent"

        val whisper = if (graph.models.hasWhisper()) "Whisper ✓" else "Whisper absent"
        modelText.text = llm + " · " + whisper + " · noyau hors réseau"
    }

    private fun fileInfo(uri: Uri): Pair<String, String?> {
        var name = "document.pdf"
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) name = cursor.getString(0) ?: name
        }
        return name to contentResolver.getType(uri)
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onDestroy() {
        if (graph.recorder.isRecording()) graph.recorder.stop()
        graph.tts.shutdown()
        super.onDestroy()
    }
}
