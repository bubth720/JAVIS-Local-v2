package com.stark.jarvislocal

import android.content.res.Configuration
import android.os.Looper
import android.app.AlertDialog
import android.widget.Button
import android.widget.EditText
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 35], application = LifecycleTestApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class MainActivityLifecycleTest {
    @Test fun memoryDialogPersistsOnWorkerAndReportsRejectedInsertWithoutSuccess() {
        val app = RuntimeEnvironment.getApplication() as LifecycleTestApplication
        app.prepared.complete(Unit)
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get()
            fun submit(text: String) {
                activity.findViewById<Button>(R.id.memory).performClick()
                awaitCondition { ShadowAlertDialog.getLatestAlertDialog()?.isShowing == true }
                val dialog = ShadowAlertDialog.getLatestAlertDialog()
                findEdit(dialog.window!!.decorView).setText(text)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            }
            submit("souvenir synthétique")
            awaitCondition { ShadowToast.getTextOfLatestToast() == "Mémoire enregistrée." }
            assertEquals(listOf("souvenir synthétique"), app.graph.db.allMemories().map { it.text })
            app.graph.db.writableDatabase.execSQL("CREATE TRIGGER reject_memory BEFORE INSERT ON memories BEGIN SELECT RAISE(ABORT, 'synthetic rejection'); END")
            submit("échec synthétique")
            awaitCondition { ShadowToast.getTextOfLatestToast()?.toString()?.startsWith("Mémoire non confirmée") == true }
            assertEquals(listOf("souvenir synthétique"), app.graph.db.allMemories().map { it.text })
        } finally { controller.pause().stop().destroy(); idle() }
    }

    private fun findEdit(view: android.view.View): EditText {
        if (view is EditText) return view
        if (view is android.view.ViewGroup) for (i in 0 until view.childCount) {
            try { return findEdit(view.getChildAt(i)) } catch (ignored: NoSuchElementException) { }
        }
        throw NoSuchElementException("Dialog input absent")
    }

    private fun awaitCondition(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (!condition() && System.nanoTime() < deadline) { idle(); Thread.sleep(5) }
        idle(); assertTrue(condition())
    }

    @Test fun rotationKeepsPreparationAndDestroysOldObserver() {
        val app = RuntimeEnvironment.getApplication() as LifecycleTestApplication
        app.actions.prepareBlock = { app.prepared.await() }
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            idle()
            assertEquals(1, app.actions.preparations)
            val oldActivity = controller.get()
            val configuration = Configuration(oldActivity.resources.configuration).apply { orientation = Configuration.ORIENTATION_LANDSCAPE }
            controller.configurationChange(configuration)
            idle()
            assertNotSame(oldActivity, controller.get())
            assertEquals(Lifecycle.State.DESTROYED, oldActivity.lifecycle.currentState)
            assertEquals(1, app.actions.preparations)
            assertEquals(0, app.actions.speechReleases)
            assertTrue(app.graph.session.state.value.busy)
            app.prepared.complete(Unit); idle()
            assertTrue(app.graph.session.state.value.prepared)
        } finally { app.prepared.complete(Unit); controller.pause().stop().destroy(); idle() }
    }

    @Test fun rotationKeepsGenerationAndRejectedInputIsNotCleared() {
        val app = RuntimeEnvironment.getApplication() as LifecycleTestApplication
        app.prepared.complete(Unit)
        app.actions.prepareBlock = { app.prepared.await() }
        val answer = CompletableDeferred<ChatMessage>()
        app.actions.chatBlock = { answer.await() }
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            idle()
            val oldActivity = controller.get()
            val input = oldActivity.findViewById<EditText>(R.id.input)
            input.setText("première")
            oldActivity.findViewById<Button>(R.id.send).performClick()
            idle()
            assertEquals("", input.text.toString())
            input.setText("à conserver")
            oldActivity.findViewById<Button>(R.id.send).performClick()
            assertEquals("à conserver", input.text.toString())
            val configuration = Configuration(oldActivity.resources.configuration).apply { orientation = Configuration.ORIENTATION_LANDSCAPE }
            controller.configurationChange(configuration); idle()
            assertEquals("à conserver", controller.get().findViewById<EditText>(R.id.input).text.toString())
            assertTrue(app.graph.session.state.value.busy)
            assertEquals(listOf("première"), app.actions.questions)
            assertEquals(0, app.actions.speechReleases)
            answer.complete(ChatMessage(role = "assistant", text = "unique")); idle()
            assertEquals(listOf("unique"), app.actions.answers)
        } finally { answer.complete(ChatMessage(role = "assistant", text = "fin")); controller.pause().stop().destroy(); idle() }
    }

    private fun idle() { shadowOf(Looper.getMainLooper()).idle() }
}

class LifecycleTestApplication : JarvisApplication() {
    internal val actions = FakeSessionActions()
    val prepared = CompletableDeferred<Unit>()
    override fun createGraph(): AppGraph = AppGraph(
        this,
        SessionCoordinator(actions, CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate), Dispatchers.Main.immediate)
    )
}
