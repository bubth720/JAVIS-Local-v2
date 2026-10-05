package com.stark.jarvislocal

import android.content.Context

class SettingsStore(context: Context) {
    private val p = context.getSharedPreferences("jarvis_settings_v2", Context.MODE_PRIVATE)

    fun load() = JarvisSettings(
        systemPrompt = p.getString("prompt", JarvisSettings.DEFAULT_SYSTEM_PROMPT)
            ?: JarvisSettings.DEFAULT_SYSTEM_PROMPT,
        // On plafonne les anciens réglages V2 pour éviter 4096/512 sur mobile.
        contextSize = p.getInt("ctx", 2048).coerceAtMost(2048),
        maxTokens = p.getInt("max", 256).coerceAtMost(256),
        topKDocuments = p.getInt("topk", 4).coerceAtMost(4),
        speakReplies = p.getBoolean("speak", false)
    )

    fun save(s: JarvisSettings) {
        p.edit()
            .putString("prompt", s.systemPrompt)
            .putInt("ctx", s.contextSize.coerceAtMost(2048))
            .putInt("max", s.maxTokens.coerceAtMost(256))
            .putInt("topk", s.topKDocuments.coerceAtMost(4))
            .putBoolean("speak", s.speakReplies)
            .apply()
    }
}
