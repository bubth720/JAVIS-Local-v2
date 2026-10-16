package com.stark.jarvislocal

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(graph.session.state, graph.tts.status) { session, tts -> session to tts }
                    .collect { (session, tts) ->
                        renderSession(session, tts)
                        refreshMessages()
                    }
            }
        }
        status.setOnClickListener {
            if (!graph.session.state.value.prepared && !graph.session.state.value.busy) prepareModels()
        }
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
        graph.session.prepare()
    }

    private fun send(text: String? = null) {
        val question = (text ?: input.text.toString()).trim()
        if (question.isEmpty()) return
        if (graph.session.send(question)) input.setText("")
        else toast("JARVIS est occupé. Ta saisie est conservée.")
    }

    private fun importPdf(uri: Uri) {
        if (!graph.session.importPdf(uri.toString())) {
            toast("JARVIS est occupé. Réessaie l’import après l’opération en cours.")
        }
    }

    private fun toggleMic() {
        val phase = graph.session.state.value.phase
        if (phase == SessionPhase.LISTENING || phase == SessionPhase.STARTING_AUDIO) {
            graph.session.stopRecording()
        } else if (!graph.session.startRecording()) {
            toast("JARVIS est occupé. Réessaie après l’opération en cours.")
        }
    }

    private fun renderSession(session: SessionState, tts: TtsStatus) {
        val label = when (session.phase) {
            SessionPhase.PREPARING -> "PRÉPARATION"
            SessionPhase.READY -> "PRÊT"
            SessionPhase.THINKING -> "RÉFLEXION"
            SessionPhase.STARTING_AUDIO, SessionPhase.LISTENING, SessionPhase.STOPPING_AUDIO -> "ÉCOUTE"
            SessionPhase.TRANSCRIBING -> "TRANSCRIPTION"
            SessionPhase.IMPORTING -> "PDF"
            SessionPhase.ERROR -> "ERREUR"
            SessionPhase.TIMEOUT -> "TIMEOUT"
        }
        setStatus(label, session.phase == SessionPhase.READY && session.prepared)
        val voiceUnavailable = graph.settings.load().speakReplies && tts == TtsStatus.UNAVAILABLE
        modelText.text = modelSummary(session.detail + if (voiceUnavailable) " · Voix française locale indisponible" else "")
        val microphoneActive = session.phase == SessionPhase.LISTENING
        findViewById<Button>(R.id.mic).apply {
            text = if (microphoneActive) "■ Stop" else "🎙  Voix"
            isEnabled = !session.busy || microphoneActive
        }
        findViewById<Button>(R.id.send).isEnabled = !session.busy
        findViewById<Button>(R.id.importPdf).isEnabled = !session.busy
    }

    private fun showMemoryDialog() = lifecycleScope.launch {
        val edit = EditText(this@MainActivity).apply {
            hint = "Ex. Mon projet principal s'appelle Orion."
        }

        val current = try {
            withContext(Dispatchers.IO) {
                graph.db.allMemories().take(12).joinToString("\n") { "• " + it.text }.ifBlank { "Aucune mémoire." }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { toast("Lecture de la mémoire impossible."); return@launch }
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@launch

        AlertDialog.Builder(this@MainActivity)
            .setTitle("Mémoire locale")
            .setMessage(current)
            .setView(edit)
            .setPositiveButton("Ajouter") { _, _ ->
                val value = edit.text.toString().trim()
                if (value.isNotEmpty()) {
                    lifecycleScope.launch {
                        try {
                            withContext(Dispatchers.IO) { graph.db.addMemory(value) }
                            toast("Mémoire enregistrée.")
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (error: Exception) { toast("Mémoire non confirmée. Vérifie la liste avant de réessayer.") }
                    }
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
                if (graph.settings.load().speakReplies) graph.tts.initialize() else graph.tts.shutdown()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private suspend fun refreshMessages() {
        val list = withContext(Dispatchers.IO) { graph.db.recentMessages(100) }
        adapter.submit(list)
        if (list.isNotEmpty()) {
            messages.post {
                messages.scrollToPosition(list.size - 1)
            }
        }
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

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onStart() {
        super.onStart()
        graph.session.onForeground()
    }

    override fun onStop() {
        graph.session.onBackground()
        super.onStop()
    }

    override fun onDestroy() {
        graph.session.onScreenDestroyed(isChangingConfigurations)
        super.onDestroy()
    }
}
