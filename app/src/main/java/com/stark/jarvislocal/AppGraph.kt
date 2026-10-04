package com.stark.jarvislocal

import android.content.Context

class AppGraph(context: Context) {
    val db = LocalDatabase(context)
    val settings = SettingsStore(context)
    val models = ModelManager(context)
    val rag = RagEngine(db)
    val pdf = PdfImporter(context, db)
    val llm = LlamaLocalEngine(models)
    val core = JarvisCore(db, rag, llm, settings)
    val whisper = WhisperLocalEngine(context, models)
    val tts = LocalTts(context)
    val recorder = WavRecorder(context)
}
