package com.stark.jarvislocal

import android.content.res.Configuration
import android.os.Looper
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 35], application = LifecycleTestApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class MainActivityLifecycleTest {
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
