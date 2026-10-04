package com.stark.jarvislocal

import android.content.Context

class SettingsStore(context: Context) {
    private val p = context.getSharedPreferences("jarvis_settings_v2", Context.MODE_PRIVATE)

    fun load() = JarvisSettings(
        systemPrompt = p.getString("prompt", JarvisSettings.DEFAULT_SYSTEM_PROMPT) ?: JarvisSettings.DEFAULT_SYSTEM_PROMPT,
        contextSize = p.getInt("ctx", 4096),
        maxTokens = p.getInt("max", 512),
        topKDocuments = p.getInt("topk", 5),
        speakReplies = p.getBoolean("speak", false)
    )

    fun save(s: JarvisSettings) {
        p.edit()
            .putString("prompt", s.systemPrompt)
            .putInt("ctx", s.contextSize)
            .putInt("max", s.maxTokens)
            .putInt("topk", s.topKDocuments)
            .putBoolean("speak", s.speakReplies)
            .apply()
    }
}
