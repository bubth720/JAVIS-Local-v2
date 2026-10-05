package com.stark.jarvislocal

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

class MainActivity : AppCompatActivity() {
    private val graph by lazy { (application as JarvisApplication).graph }

    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var modelText: TextView
    private lateinit var messages: RecyclerView
    private val adapter = MessageAdapter()

    private val pdfPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importPdf(uri)
    }

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) toggleMic() else toast("Permission micro refusée.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        applySystemInsets()

        input = findViewById(R.id.input)
        status = findViewById(R.id.statusText)
        modelText = findViewById(R.id.modelText)
        messages = findViewById(R.id.messages)

        messages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        messages.adapter = adapter

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

        refreshMessages()
        prepareModels()
    }

    private fun applySystemInsets() {
        val root = findViewById<android.view.View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                bars.top,
                view.paddingRight,
                bars.bottom + dp(8)
            )
            insets
        }
    }

    private fun prepareModels() {
        setStatus("PRÉPARATION", false)

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    graph.models.ensureBundledModels { label ->
                        runOnUiThread {
                            setStatus("PRÉPARATION", false)
                            modelText.text = label
                        }
                    }
                }

                modelText.text = modelSummary("Chargement du cerveau local…")

                withContext(Dispatchers.Default) {
                    graph.llm.warmUp(graph.settings.load())
                }

                refreshModelStatus()
                setStatus("PRÊT", true)

                if (graph.db.recentMessages(1).isEmpty()) {
                    graph.db.addMessage(
                        ChatMessage(
                            role = "assistant",
                            text = "Noyau local initialisé. Le modèle est chargé et prêt à répondre."
                        )
                    )
                    refreshMessages()
                }
            } catch (e: Exception) {
                setStatus("ERREUR", false)
                modelText.text = "Initialisation incomplète : " + (e.message ?: "erreur inconnue")
            }
        }
    }

    private fun send(text: String? = null) {
        val q = (text ?: input.text.toString()).trim()
        if (q.isEmpty()) return

        input.setText("")
        setStatus("RÉFLEXION", false)

        lifecycleScope.launch {
            try {
                val answer = withTimeout(90_000L) {
                    withContext(Dispatchers.Default) {
                        graph.core.chat(q)
                    }
                }

                refreshMessages()

                if (graph.settings.load().speakReplies) {
                    graph.tts.speak(answer.text)
                }

                setStatus("PRÊT", true)
            } catch (_: TimeoutCancellationException) {
                graph.db.addMessage(
                    ChatMessage(
                        role = "assistant",
                        text = "La génération a dépassé 90 secondes. J'ai arrêté l'attente pour éviter un blocage."
                    )
                )
                refreshMessages()
                setStatus("TIMEOUT", false)
            } catch (e: Exception) {
                graph.db.addMessage(
                    ChatMessage(
                        role = "assistant",
                        text = "Erreur locale : " + (e.message ?: "erreur inconnue")
                    )
                )
                refreshMessages()
                setStatus("ERREUR", false)
            }
        }
    }

    private fun importPdf(uri: Uri) {
        setStatus("PDF", false)

        lifecycleScope.launch {
            try {
                val info = fileInfo(uri)
                val count = withContext(Dispatchers.IO) {
                    graph.pdf.import(uri, info.first, info.second)
                }

                graph.db.addMessage(
                    ChatMessage(
                        role = "assistant",
                        text = "PDF « " + info.first + " » indexé localement : " + count + " passages. Tu peux maintenant me poser une question factuelle sur son contenu.",
                        source = info.first
                    )
                )

                refreshMessages()
                setStatus("PRÊT", true)
            } catch (e: Exception) {
                toast("PDF : " + (e.message ?: "erreur"))
                setStatus("ERREUR", false)
            }
        }
    }

    private fun toggleMic() {
        val audio = File(cacheDir, "jarvis_input.wav")
        val button = findViewById<Button>(R.id.mic)

        if (!graph.recorder.isRecording()) {
            graph.recorder.start(audio)
            button.text = "■ Stop"
            setStatus("ÉCOUTE", false)
        } else {
            graph.recorder.stop()
            button.text = "🎙  Voix"
            setStatus("TRANSCRIPTION", false)

            lifecycleScope.launch {
                try {
                    val text = withTimeout(60_000L) {
                        withContext(Dispatchers.Default) {
                            graph.whisper.transcribe(audio.absolutePath)
                        }
                    }

                    if (text.isNotBlank()) {
                        input.setText(text)
                        send(text)
                    } else {
                        toast("Aucune parole reconnue.")
                        setStatus("PRÊT", true)
                    }
                } catch (e: Exception) {
                    toast("Voix : " + (e.message ?: "erreur"))
                    setStatus("ERREUR", false)
                }
            }
        }
    }

    private fun showMemoryDialog() {
        val edit = EditText(this).apply {
            hint = "Ex. Mon projet principal s'appelle Orion."
        }

        val current = graph.db.allMemories()
            .take(12)
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
            minLines = 7
            maxLines = 12
        }

        AlertDialog.Builder(this)
            .setTitle("Personnalité de JARVIS")
            .setView(edit)
            .setPositiveButton("Enregistrer") { _, _ ->
                graph.settings.save(
                    settings.copy(systemPrompt = edit.text.toString())
                )
                toast("Personnalité enregistrée.")
            }
            .setNeutralButton(
                if (settings.speakReplies) "Couper voix" else "Activer voix"
            ) { _, _ ->
                graph.settings.save(
                    settings.copy(speakReplies = !settings.speakReplies)
                )
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun refreshMessages() {
        val list = graph.db.recentMessages(100)
        adapter.submit(list)
        if (list.isNotEmpty()) {
            messages.post {
                messages.scrollToPosition(list.size - 1)
            }
        }
    }

    private fun refreshModelStatus() {
        modelText.text = modelSummary(
            if (graph.llm.isLoaded()) "LLM chargé" else "LLM prêt"
        )
    }

    private fun modelSummary(prefix: String): String {
        val mb = graph.models.llmFile.length() / 1024 / 1024
        val whisper = if (graph.models.hasWhisper()) "Whisper ✓" else "Whisper absent"
        return "$prefix · $mb Mo · $whisper · hors réseau"
    }

    private fun setStatus(label: String, ready: Boolean) {
        status.text = label
        status.setTextColor(
            ContextCompat.getColor(
                this,
                if (ready) R.color.jarvis_green else R.color.jarvis_cyan
            )
        )
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
            if (cursor.moveToFirst()) {
                name = cursor.getString(0) ?: name
            }
        }

        return name to contentResolver.getType(uri)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onDestroy() {
        if (graph.recorder.isRecording()) {
            graph.recorder.stop()
        }
        graph.tts.shutdown()
        super.onDestroy()
    }
}
