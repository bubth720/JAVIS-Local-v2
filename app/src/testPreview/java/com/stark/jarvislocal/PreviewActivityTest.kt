package com.stark.jarvislocal

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAlertDialog

/** Preview launcher and real graph without weights. No native LLM or physical Android proof. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[24,25,35],application=JarvisApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class PreviewActivityTest {
    private val app get() = RuntimeEnvironment.getApplication() as JarvisApplication
    private fun idleUntil(done: () -> Boolean) {
        val end=System.nanoTime()+5_000_000_000L
        while(!done() && System.nanoTime()<end) {shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5)}
        shadowOf(Looper.getMainLooper()).idle();assertTrue(done())
    }
    private fun assertPrivateAndUnloaded() {
        assertFalse(app.getDatabasePath("jarvis_local_v2.db").exists())
        assertTrue(app.getSharedPreferences("jarvis_settings_v2",0).all.isEmpty())
        assertTrue(app.graph.llm.state.value.modelUnloaded)
    }
    @Test fun launcherIsIsolatedAndDoesNotPrepareAnyEngine() {
        val c=Robolectric.buildActivity(PreviewActivity::class.java).setup()
        try {
            assertEquals("com.stark.jarvislocal.preview001",app.packageName)
            assertTrue(app.resources.getBoolean(R.bool.jarvis_preview))
            val permissions=app.packageManager.getPackageInfo(app.packageName,PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
            assertFalse(permissions.contains("android.permission.INTERNET"))
            assertFalse(permissions.contains("android.permission.RECORD_AUDIO"))
            idleUntil {c.get().findViewById<TextView>(R.id.previewModelStatus).text.contains("Modèle absent")}
            assertFalse(app.graph.textLab.active);assertFalse(app.graph.session.state.value.busy)
            assertPrivateAndUnloaded()
        } finally {c.pause().stop().destroy()}
    }
    @Test fun openingLabRequiresConfirmationAndOnlyPassesExplicitOptIn() {
        val c=Robolectric.buildActivity(PreviewActivity::class.java).setup()
        try {
            c.get().findViewById<Button>(R.id.previewStart).performClick()
            val dialog=ShadowAlertDialog.getLatestAlertDialog()
            assertNotNull(dialog);assertFalse(app.graph.textLab.active)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            val next=shadowOf(c.get()).nextStartedActivity
            assertNotNull(next)
            assertEquals(MainActivity::class.java.name,next.component!!.className)
            assertTrue(next.getBooleanExtra(MainActivity.EXTRA_TEXT_LAB,false))
            assertFalse(app.graph.textLab.active);assertPrivateAndUnloaded()
        } finally {c.pause().stop().destroy()}
    }
    @Test fun missingModelRetryAndExitNeverFallBackToHistoricalModeOrPersistence() {
        val c=Robolectric.buildActivity(MainActivity::class.java,
            Intent(app,MainActivity::class.java).putExtra(MainActivity.EXTRA_TEXT_LAB,true)).setup()
        try {
            idleUntil {!app.graph.session.state.value.busy}
            assertTrue(app.graph.textLab.active);assertEquals(SessionPhase.ERROR,app.graph.session.state.value.phase)
            assertFalse(app.graph.session.state.value.prepared)
            for(id in listOf(R.id.mic,R.id.memory,R.id.settings,R.id.importPdf)) {
                val button=c.get().findViewById<Button>(id);assertFalse(button.isEnabled);button.performClick()
            }
            c.get().findViewById<TextView>(R.id.statusText).performClick()
            idleUntil {!app.graph.session.state.value.busy}
            assertEquals(SessionPhase.ERROR,app.graph.session.state.value.phase)
            c.get().onBackPressedDispatcher.onBackPressed()
            assertFalse(app.graph.textLab.active);assertTrue(c.get().isFinishing)
            assertTrue(app.graph.textLab.state.value.messages.isEmpty());assertPrivateAndUnloaded()
        } finally {c.pause().stop().destroy()}
    }
    @Test fun directMainEntryWithoutOptInReturnsToWelcome() {
        val c=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            assertTrue(c.get().isFinishing)
            assertEquals(PreviewActivity::class.java.name,shadowOf(c.get()).nextStartedActivity.component!!.className)
            assertFalse(app.graph.textLab.active);assertPrivateAndUnloaded()
        } finally {c.pause().stop().destroy()}
    }
}
