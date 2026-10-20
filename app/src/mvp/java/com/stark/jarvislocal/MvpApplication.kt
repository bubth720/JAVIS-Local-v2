package com.stark.jarvislocal

import android.app.Application
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** No legacy graph, voice, SQLite, identity, asset preparation or auto warmup in this variant. */
open class MvpApplication : Application() {
    internal lateinit var runtime: MvpRuntime
    override fun onCreate() {
        super.onCreate()
        val store = MvpModelStore(File(noBackupFilesDir, "mvp-models"))
        val controller = InferenceController(InferenceEngineFactory(historical = {
            LlamaLocalEngine(store.selectedFile().absolutePath, store::ready)
        }))
        runtime = createRuntime(store, controller, CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))
        runtime.restore()
    }
    internal open fun createRuntime(store: MvpModelStore, controller: InferenceController, scope: CoroutineScope): MvpRuntime =
        MvpRuntime(store, controller, scope,
            { bytes -> MvpSafety.check(bytes, MvpSafety.read(this)) })
}
