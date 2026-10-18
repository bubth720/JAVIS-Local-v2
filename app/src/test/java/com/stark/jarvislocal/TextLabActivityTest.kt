package com.stark.jarvislocal

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.os.Parcel
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
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

/** Real graph without assets: refusal/UI/privacy only. Does not load native libraries or a model. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[24,25,35],application=JarvisApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class TextLabActivityTest {
    private fun idle() {shadowOf(Looper.getMainLooper()).idle()}
    @Test fun explicitLabLaunchRefusesMissingModelAndNeverOpensConversationDatabase() {
        val app=RuntimeEnvironment.getApplication() as JarvisApplication
        val intent=Intent(app,MainActivity::class.java).putExtra(MainActivity.EXTRA_TEXT_LAB,true)
        val activity=Robolectric.buildActivity(MainActivity::class.java,intent).setup()
        try {
            val end=System.nanoTime()+5_000_000_000L
            while(app.graph.session.state.value.busy && System.nanoTime()<end) {idle();Thread.sleep(5)}
            idle();assertTrue(app.graph.textLab.active);assertEquals(SessionPhase.ERROR,app.graph.session.state.value.phase)
            assertFalse(app.graph.session.state.value.prepared)
            assertTrue(activity.get().findViewById<TextView>(R.id.modelText).text.contains("Athéna inactive"))
            assertFalse(app.getDatabasePath("jarvis_local_v2.db").exists())
            for (id in listOf(R.id.memory,R.id.settings,R.id.mic,R.id.importPdf)) {
                assertFalse(activity.get().findViewById<Button>(id).isEnabled)
                activity.get().findViewById<Button>(id).performClick()
            }
            assertFalse(app.getDatabasePath("jarvis_local_v2.db").exists())
            assertTrue(app.getSharedPreferences("jarvis_settings_v2",0).all.isEmpty())
            assertTrue(app.graph.llm.state.value.modelUnloaded)
        } finally {activity.pause().stop().destroy();idle()}
    }
    @Test fun labDraftIsExcludedFromAndroidSavedStateAndScreenshots() {
        val app=RuntimeEnvironment.getApplication() as JarvisApplication
        val intent=Intent(app,MainActivity::class.java).putExtra(MainActivity.EXTRA_TEXT_LAB,true)
        val controller=Robolectric.buildActivity(MainActivity::class.java,intent).setup()
        try {
            val input=controller.get().findViewById<EditText>(R.id.input)
            assertFalse(input.isSaveEnabled)
            assertTrue(controller.get().window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            val marker="PUBLIC_VOLATILE_FIXTURE_9487";input.setText(marker)
            val state=Bundle();controller.saveInstanceState(state)
            val parcel=Parcel.obtain()
            try {
                parcel.writeBundle(state)
                assertFalse(parcel.marshall().toString(Charsets.UTF_16LE).contains(marker))
                assertFalse(parcel.marshall().toString(Charsets.UTF_8).contains(marker))
            } finally {parcel.recycle()}
            assertFalse(app.getDatabasePath("jarvis_local_v2.db").exists())
        } finally {controller.pause().stop().destroy();idle()}
    }
}
