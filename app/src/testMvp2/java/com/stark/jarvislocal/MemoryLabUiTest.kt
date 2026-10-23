package com.stark.jarvislocal

import android.app.AlertDialog
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.SQLiteMode
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 25, 35], application = Mvp2Application::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class MemoryLabUiTest {
    private fun all(v: View): List<View> = listOf(v) + if (v is ViewGroup) (0 until v.childCount).flatMap { all(v.getChildAt(it)) } else emptyList()
    private fun idle(app: Mvp2Application) {
        val end = System.nanoTime() + 5_000_000_000
        do { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(5) } while (app.runtime.state.value.busy && System.nanoTime() < end)
        assertFalse(app.runtime.state.value.busy)
    }
    @Test fun backgroundRevokesPendingPreviewAndCannotApplyAStaleConfirmation() {
        val controller = Robolectric.buildActivity(Mvp2Activity::class.java).setup()
        val app = RuntimeEnvironment.getApplication() as Mvp2Application
        try {
            idle(app)
            val views = all(controller.get().findViewById(android.R.id.content))
            views.filterIsInstance<CheckBox>().first { it.text.startsWith("Activer le laboratoire") }.performClick()
            views.filterIsInstance<Button>().first { it.text == "Mémoire confirmée — laboratoire isolé" }.performClick()
            val outer = ShadowAlertDialog.getLatestAlertDialog()
            all(outer.window!!.decorView).filterIsInstance<EditText>().first().setText("synthetic stale preview")
            outer.getButton(AlertDialog.BUTTON_POSITIVE).performClick(); idle(app)
            val preview = ShadowAlertDialog.getLatestAlertDialog()
            controller.pause().stop(); idle(app)
            preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick(); idle(app)
            assertTrue(app.graph.confirmedMemory.listOwned().isEmpty())
        } finally { controller.destroy(); idle(app); app.graph.confirmedMemory.close(); app.graph.db.close() }
    }
    @Test fun previewRejectionAndPositiveConfirmationAreSeparateAndDoNotWriteChat() {
        val controller = Robolectric.buildActivity(Mvp2Activity::class.java).setup()
        val app = RuntimeEnvironment.getApplication() as Mvp2Application
        try {
            idle(app)
            val views = all(controller.get().findViewById(android.R.id.content))
            views.filterIsInstance<CheckBox>().first { it.text.startsWith("Activer le laboratoire") }.performClick()
            fun preview() {
                views.filterIsInstance<Button>().first { it.text == "Mémoire confirmée — laboratoire isolé" }.performClick()
                val outer = ShadowAlertDialog.getLatestAlertDialog()
                all(outer.window!!.decorView).filterIsInstance<EditText>().first().setText("synthetic UI memory")
                outer.getButton(AlertDialog.BUTTON_POSITIVE).performClick(); idle(app)
            }
            preview()
            assertTrue(app.graph.confirmedMemory.listOwned().isEmpty())
            ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick(); idle(app)
            assertTrue(app.graph.confirmedMemory.listOwned().isEmpty())
            preview()
            ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick(); idle(app)
            assertEquals("synthetic UI memory", app.graph.confirmedMemory.listOwned().single().content)
            assertTrue(app.graph.db.recentMessages().isEmpty()); assertTrue(app.graph.db.allMemories().isEmpty())
            assertFalse(app.runtime.state.value.loaded)
        } finally { controller.pause().stop().destroy(); idle(app); app.graph.confirmedMemory.close(); app.graph.db.close() }
    }
}
