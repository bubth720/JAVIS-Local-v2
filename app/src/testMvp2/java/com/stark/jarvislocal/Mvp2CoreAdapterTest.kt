package com.stark.jarvislocal

import android.app.Application
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[24,25,35],application=Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class Mvp2CoreAdapterTest {
    private val app get()=RuntimeEnvironment.getApplication() as Application
    private lateinit var db:LocalDatabase
    @Before fun setup(){app.deleteDatabase("jarvis_local_v2.db");db=LocalDatabase(app)}
    @After fun cleanup(){db.close();app.deleteDatabase("jarvis_local_v2.db")}
    private class Backend:LocalLanguageModel {
        var system="";var prompt="";var loaded=false;var wait:CompletableDeferred<Unit>?=null
        override suspend fun warmUp(settings:JarvisSettings){loaded=true}
        override suspend fun generate(prompt:String,systemPrompt:String,settings:JarvisSettings):String {
            this.prompt=prompt;system=systemPrompt;wait?.await();return "SYNTHETIC CORE TEST REPLY"
        }
        override fun isLoaded()=loaded
        override suspend fun unload():UnloadResult{loaded=false;return UnloadResult.CONFIRMED}
    }
    private fun port(engine:InferenceController):CoreConversationAdapter {
        val settings=SettingsStore(app);return CoreConversationAdapter(db,settings,JarvisCore(db,RagEngine(db),engine,settings))
    }
    @Test fun realCorePersistsSyntheticBackendAnswerAndDistinctSystemAcrossReopen()=runTest {
        val backend=Backend();val engine=InferenceController(isolatedInferenceFactory({backend}));val port=port(engine)
        engine.warmUp(JarvisSettings());port.chat("explique un quasar",JarvisSettings(systemPrompt="SYSTEM SEPARATE"))
        assertEquals("SYSTEM SEPARATE",backend.system);assertFalse(backend.prompt.contains("SYSTEM SEPARATE"))
        assertEquals(2,port.recent().size);assertTrue(db.allMemories().isEmpty())
        db.close();db=LocalDatabase(app);assertEquals("SYNTHETIC CORE TEST REPLY",db.recentMessages().last().text)
    }
    @Test fun explicitMemoryUsesExistingCommandAndNeverLearnsNormalMessages()=runTest {
        val backend=Backend();val engine=InferenceController(isolatedInferenceFactory({backend}));val port=port(engine)
        assertFalse(port.needsModel("/remember public synthetic memory"));port.chat("/remember public synthetic memory",JarvisSettings())
        assertEquals("public synthetic memory",db.allMemories().single().text)
        assertEquals(2,port.recent().size);assertTrue(port.needsModel("question personnelle"))
        engine.warmUp(JarvisSettings());port.chat("explique un quasar",JarvisSettings())
        assertEquals(1,db.allMemories().size)
    }
    @Test fun adapterSessionSurvivesRuntimeCompositionAndRestoresHistory()=runTest {
        val backend=Backend();val engine=InferenceController(isolatedInferenceFactory({backend}));val port=port(engine)
        port.chat("/remember fixture confirmée",JarvisSettings())
        val store=MvpModelStore(File(app.noBackupFilesDir,"unused-mvp2"))
        val runtime=MvpRuntime(store,engine,this,{},StandardTestDispatcher(testScheduler),conversation=port)
        runtime.restore();runCurrent();assertEquals(2,runtime.state.value.messages.size)
        assertTrue(runtime.send("/docs",JarvisSettings()));runCurrent();assertEquals(4,runtime.state.value.messages.size)
        engine.warmUp(JarvisSettings());port.chat("explique un quasar",JarvisSettings());assertEquals(6,port.recent().size)
    }
    @Test fun cancelledInferenceCannotWriteLateAssistantToSqlite()=runTest {
        val backend=Backend();val engine=InferenceController(isolatedInferenceFactory({backend}));val port=port(engine)
        engine.warmUp(JarvisSettings());backend.wait=CompletableDeferred()
        val work=launch { port.chat("explique un quasar",JarvisSettings()) }
        runCurrent();work.cancel();runCurrent();assertEquals(1,db.recentMessages().size)
        backend.wait!!.complete(Unit);runCurrent();assertEquals(1,db.recentMessages().size)
        assertTrue(db.allMemories().isEmpty())
    }
}
