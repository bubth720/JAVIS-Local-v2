package com.stark.jarvislocal

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Preview-only launcher. No preparation, import, download, session or native call on opening. */
class PreviewActivity : AppCompatActivity() {
    private val graph by lazy { (application as JarvisApplication).graph }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_preview)
        val root = findViewById<android.view.View>(R.id.previewRoot)
        val padding = (16 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(padding + bars.left, padding + bars.top, padding + bars.right, padding + bars.bottom)
            insets
        }
        findViewById<Button>(R.id.previewStart).setOnClickListener {
            AlertDialog.Builder(this).setTitle(R.string.preview_confirm_title)
                .setMessage(R.string.preview_confirm_body)
                .setPositiveButton(R.string.preview_open_lab) { _, _ ->
                    startActivity(Intent(this, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_TEXT_LAB, true))
                }.setNegativeButton(android.R.string.cancel, null).show()
        }
        findViewById<Button>(R.id.previewModelHelp).setOnClickListener {
            AlertDialog.Builder(this).setTitle(R.string.preview_model_title)
                .setMessage(R.string.preview_model_help)
                .setPositiveButton(android.R.string.ok, null).show()
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val status = withContext(Dispatchers.IO) {
                when {
                    graph.models.hasLlm() -> R.string.preview_model_present
                    graph.models.llmFile.exists() -> R.string.preview_model_invalid
                    else -> R.string.preview_model_absent
                }
            }
            findViewById<TextView>(R.id.previewModelStatus).setText(status)
        }
    }
}
