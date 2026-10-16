package com.stark.jarvislocal

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppGraph(context: Context, sessionOverride: SessionCoordinator? = null) {
    private val appContext = context.applicationContext
    val db = LocalDatabase(appContext)
    val settings = SettingsStore(appContext)
    val models = ModelManager(appContext)
    val rag = RagEngine(db)
    val pdf = PdfImporter(appContext, db)
    val llm = LlamaLocalEngine(models)
    val core = JarvisCore(db, rag, llm, settings)
    val whisper = WhisperLocalEngine(appContext, models)
    val tts = LocalTts(appContext)
    val recorder = WavRecorder(appContext)
    private val operationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val session = sessionOverride ?: SessionCoordinator(object : SessionActions {
        private val applicationContext = appContext
        private var recordingFile: File? = null

        override suspend fun prepare(progress: (String) -> Unit) {
            WavFiles.clearAbandonedRecordings(applicationContext.cacheDir)
            models.ensureBundledModels(progress)
            progress("Chargement du cerveau local…")
            llm.warmUp(settings.load())
            check(llm.isLoaded() && models.hasWhisper()) { "Préparation des modèles incomplète" }
            if (db.recentMessages(1).isEmpty()) {
                db.addMessage(ChatMessage(role = "assistant", text = "Noyau local initialisé. Le modèle est chargé et prêt à répondre."))
            }
        }

        override suspend fun chat(text: String): ChatMessage = core.chat(text)

        override suspend fun importPdf(uri: String) {
            val document = Uri.parse(uri)
            var name = "document.pdf"
            applicationContext.contentResolver.query(document, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) name = it.getString(0) ?: name
            }
            val count = pdf.import(document, name, applicationContext.contentResolver.getType(document))
            db.addMessage(ChatMessage(role = "assistant", text = "PDF « $name » indexé localement : $count passages. Tu peux maintenant me poser une question factuelle sur son contenu.", source = name))
        }

        override suspend fun startRecording() {
            val file = File.createTempFile("jarvis_input_", ".wav", applicationContext.cacheDir)
            recordingFile = file
            recorder.start(file)
        }

        override suspend fun awaitRecording(): File = recorder.awaitCompletion()

        override suspend fun stopRecording(discard: Boolean): File? {
            return try { recorder.stop(discard) }
            finally {
                if (discard) {
                    recordingFile?.let(WavFiles::delete)
                    recordingFile = null
                }
            }
        }

        override suspend fun transcribe(file: File): String = whisper.transcribe(file.absolutePath)
        override fun deleteRecording(file: File) { WavFiles.delete(file) }
        override suspend fun reportError(detail: String) {
            db.addMessage(ChatMessage(role = "assistant", text = "Erreur locale : $detail"))
        }
        override fun speakIfEnabled(text: String) { if (settings.load().speakReplies) tts.speak(text) }
        override fun stopSpeech() { tts.stop() }
        override fun releaseSpeech() { tts.shutdown() }
        override fun initializeSpeechIfEnabled() { if (settings.load().speakReplies) tts.initialize() }
    }, operationScope)
}
