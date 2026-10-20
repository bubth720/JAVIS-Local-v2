package com.stark.jarvislocal

import java.io.File
import kotlinx.coroutines.CoroutineScope

class Mvp2Application : MvpApplication() {
    internal lateinit var graph: Mvp2Graph
    override fun createRuntime(store: MvpModelStore, controller: InferenceController, scope: CoroutineScope): MvpRuntime {
        graph = Mvp2Graph(this, controller)
        return MvpRuntime(store, controller, scope, { bytes -> MvpSafety.check(bytes, MvpSafety.read(this)) },
            conversation = graph.conversation)
    }
}

internal class Mvp2Graph(app: Mvp2Application, controller: InferenceController) {
    val db = LocalDatabase(app)
    val settings = SettingsStore(app)
    val core = JarvisCore(db, RagEngine(db), controller, settings)
    val conversation = CoreConversationAdapter(db, settings, core)
    val speech = Mvp2Speech(LocalTts(app)) {
        android.provider.Settings.Global.getInt(app.contentResolver, android.provider.Settings.Global.AIRPLANE_MODE_ON, 0) == 1
    }
    val voiceModels = MvpModelStore(File(app.noBackupFilesDir, "mvp-whisper"),
        MvpImportPolicy(50L * 1024 * 1024, 160L * 1024 * 1024), format = MvpModelFormat.WHISPER_GGML)
    // Hash/format validation is not native qualification. No recorder, Whisper allocation or Binder is activated.
    val microphoneEnabled = false
    val microphoneReason = "Whisper non qualifié : microphone désactivé, saisie textuelle disponible. Import ≠ transcription validée."
}
