package com.stark.jarvislocal

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 25, 35], application = Mvp2Application::class)
class MvpIntentUiTest {
    private fun all(view: View): List<View> = listOf(view) + if (view is ViewGroup)
        (0 until view.childCount).flatMap { all(view.getChildAt(it)) } else emptyList()
    private fun idle(app: Mvp2Application) {
        val end = System.nanoTime() + 5_000_000_000
        do { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(5) }
        while (app.runtime.state.value.busy && System.nanoTime() < end)
        assertFalse(app.runtime.state.value.busy)
    }
    @Test fun explicitReadOnlyLaboratoryUsesRealDatabaseWithoutLoadingModelOrWritingChat() {
        val activity = Robolectric.buildActivity(Mvp2Activity::class.java).setup()
        val app = RuntimeEnvironment.getApplication() as Mvp2Application
        try {
            idle(app)
            val views = all(activity.get().findViewById(android.R.id.content))
            val toggle = views.filterIsInstance<CheckBox>().first { it.text.startsWith("Activer le laboratoire") }
            assertFalse(toggle.isChecked)
            assertFalse(app.graph.intentFacade.isEnabled)
            app.graph.db.addDocument("fixture-only.txt", "text/plain", listOf("Synthetic fixture"))
            toggle.performClick()
            assertTrue(app.graph.intentFacade.isEnabled)
            views.filterIsInstance<Button>().first { it.text == "Intentions : lister les documents SQLite" }.performClick()
            idle(app)
            val dialog = ShadowAlertDialog.getLatestAlertDialog()
            assertTrue(org.robolectric.Shadows.shadowOf(dialog).message.toString().contains("fixture-only.txt"))
            assertFalse(app.runtime.state.value.loaded)
            assertTrue(app.graph.db.recentMessages().isEmpty())
            assertTrue(app.graph.db.allMemories().isEmpty())
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
            toggle.performClick()
            assertFalse(app.graph.intentFacade.isEnabled)
        } finally { activity.pause().stop().destroy(); idle(app); app.graph.db.close() }
    }
}
