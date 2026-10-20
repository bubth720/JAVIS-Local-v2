package com.stark.jarvislocal

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

/** Explicit actions only; no import/download/load on opening or rotation. */
open class MvpActivity : AppCompatActivity() {
    internal val runtime get() = (application as MvpApplication).runtime
    protected val fields = mutableMapOf<String, EditText>()
    private val controls = mutableListOf<Button>()
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private var pendingManifest: MvpModelManifest? = null
    private val picker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val manifest = pendingManifest
        pendingManifest = null
        val uri = result.data?.data
        if (result.resultCode == RESULT_OK && manifest != null && uri != null) {
            if (uri.scheme != "content") toast("Sélection content:// locale requise")
            else if (!runtime.importModel(manifest) { requireNotNull(contentResolver.openInputStream(uri)) { "Fichier inaccessible" } })
                toast("Opération déjà active : import refusé")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24,48,24,32) }
        val scroll = ScrollView(this).apply { addView(layout) }
        setContentView(scroll)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val padding = (16 * resources.displayMetrics.density).toInt()
            view.setPadding(padding + bars.left, padding + bars.top, padding + bars.right, padding + bars.bottom)
            insets
        }
        decorate(layout)
        layout.addView(TextView(this).apply { text = introText(); textSize = 18f })
        status = TextView(this); layout.addView(status)
        val modelForm = if (compactUi) LinearLayout(this).also { form ->
            form.orientation = LinearLayout.VERTICAL; form.visibility = android.view.View.GONE
            layout.addView(Button(this).apply {
                text = "Modèles et paramètres"
                setOnClickListener { form.visibility = if (form.visibility == android.view.View.GONE)
                    android.view.View.VISIBLE else android.view.View.GONE }
            })
            layout.addView(form)
        } else layout
        fun field(key: String, hint: Int, initial: String = "", parent: LinearLayout = modelForm): EditText = EditText(this).also {
            it.hint = getString(hint); it.setText(savedInstanceState?.getString(key) ?: initial)
            fields[key] = it; parent.addView(it)
        }
        field("provenance", R.string.mvp_provenance)
        field("license", R.string.mvp_license)
        field("sha", R.string.mvp_sha)
        field("bytes", R.string.mvp_bytes).inputType = android.text.InputType.TYPE_CLASS_NUMBER
        val prefs = getSharedPreferences("mvp_options", MODE_PRIVATE)
        field("system", R.string.mvp_system, prefs.getString("system", "Tu es un assistant local. Réponds en français de façon concise.")!!)
        fun settings(): JarvisSettings {
            val system = fields.getValue("system").text.toString()
            require(system.toByteArray(Charsets.UTF_8).size <= 1024) { "Prompt système limité à 1024 octets" }
            check(prefs.edit().putString("system", system).commit()) { "Paramètres non enregistrés" }
            return JarvisSettings(systemPrompt = system, maxTokens = 128)
        }
        fun button(label: Int, action: () -> Unit) = Button(this).also { b ->
            b.setText(label); b.setOnClickListener { try { action() } catch (failure: Exception) { toast(failure.message ?: "Action refusée") } }
            controls += b; layout.addView(b)
        }
        button(R.string.mvp_import) {
            val manifest = MvpModelManifest(fields.getValue("provenance").text.toString().trim(),
                fields.getValue("license").text.toString().trim(), fields.getValue("sha").text.toString().trim(),
                fields.getValue("bytes").text.toString().toLong())
            manifest.validate(MvpImportPolicy())
            pendingManifest = manifest
            picker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"; putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        }
        button(R.string.mvp_load) { if (!runtime.load(settings())) toast("Opération déjà active") }
        button(R.string.mvp_unload) { if (!runtime.unload()) toast("Opération déjà active") }
        val question = field("question", R.string.mvp_question, parent = layout)
        button(R.string.mvp_send) {
            val text = question.text.toString()
            confirmSend(text) {
                try {
                    if (runtime.send(text, settings())) question.text.clear() else toast("Envoi refusé ou opération déjà active")
                } catch (failure: Exception) { toast(failure.message ?: "Envoi refusé") }
            }
        }
        Button(this).also { b ->
            b.setText(R.string.mvp_cancel); b.setOnClickListener { runtime.cancel() }; layout.addView(b)
        }
        if (!compactUi) addInteractionControls(layout)
        transcript = TextView(this).apply { setTextIsSelectable(true) }; layout.addView(transcript)
        if (compactUi) addInteractionControls(layout)
        if (savedInstanceState?.getBoolean("pending") == true) {
            try { pendingManifest = MvpModelManifest(fields.getValue("provenance").text.toString(), fields.getValue("license").text.toString(),
                fields.getValue("sha").text.toString(), fields.getValue("bytes").text.toString().toLong()) }
            catch (_: Exception) { pendingManifest = null }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                runtime.state.collect { state ->
                    status.text = state.detail + (state.model?.let { "\n${it.provenance}\nLicence : ${it.license}\nSHA-256 : ${it.sha256}\n${it.bytes} octets" } ?: "")
                    controls.forEach { it.isEnabled = !state.busy }
                    renderInteraction(state)
                    transcript.text = state.messages.joinToString("\n\n") { "${it.role} : ${it.text}" }
                }
            }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        fields.forEach { (key, value) -> outState.putString(key, value.text.toString()) }
        outState.putBoolean("pending", pendingManifest != null)
        super.onSaveInstanceState(outState)
    }
    override fun onStop() {
        if (!isChangingConfigurations) runtime.cancel("Application en arrière-plan : arrêt demandé, attente de fin native.")
        super.onStop()
    }
    protected open val compactUi: Boolean = false
    protected open fun introText(): String = getString(R.string.mvp_intro)
    protected open fun decorate(layout: LinearLayout) { }
    protected open fun addInteractionControls(layout: LinearLayout) { }
    internal open fun renderInteraction(state: MvpState) { }
    protected open fun confirmSend(text: String, send: () -> Unit) { send() }
    protected fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
}
