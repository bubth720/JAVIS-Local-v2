package com.stark.jarvislocal

import android.app.Application
import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 25, 35], application = Application::class, manifest = Config.NONE)
class SettingsStoreTest {
    @Test fun preferencesSurviveStoreRecreationWithoutResettingPromptOrVoice() {
        val context = RuntimeEnvironment.getApplication() as Context
        val expected = JarvisSettings(systemPrompt = "Prompt privé synthétique é🙂", contextSize = 1024, maxTokens = 128, topKDocuments = 2, speakReplies = true)
        SettingsStore(context).save(expected)
        assertEquals(expected, SettingsStore(context).load())
    }

    @Test fun historicalCapsDoNotRewritePersistedPreferencesOnRead() {
        val context = RuntimeEnvironment.getApplication() as Context
        val prefs = context.getSharedPreferences("jarvis_settings_v2", Context.MODE_PRIVATE)
        assertTrue(prefs.edit().putString("prompt", "historique").putInt("ctx", 4096).putInt("max", 512)
            .putInt("topk", 8).putBoolean("speak", true).putString("future_setting", "preserved").commit())
        val before = prefs.all.toMap()
        assertEquals(JarvisSettings("historique", 2048, 256, 4, true), SettingsStore(context).load())
        assertEquals(before, prefs.all)
        SettingsStore(context).save(SettingsStore(context).load().copy(speakReplies = false))
        assertEquals("preserved", prefs.getString("future_setting", null))
        assertEquals("historique", SettingsStore(context).load().systemPrompt)
    }
}
