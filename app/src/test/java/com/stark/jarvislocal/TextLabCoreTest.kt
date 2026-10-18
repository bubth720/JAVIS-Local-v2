package com.stark.jarvislocal

import android.app.Application
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Actual CORE/controller/admission/SQLite, backend simulated; never JNI or real GGUF. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[24,25,35],application=Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TextLabCoreTest {
    private val ctx get()=RuntimeEnvironment.getApplication()
    @Test fun messageAndFollowupUseOneExistingBackendWithoutReadingOrWritingPersonalHistory() = runTest {
        val db=LocalDatabase(ctx)
        db.addMessage(ChatMessage(role="user",text="DURABLE_FIXTURE_NOT_FOR_LAB"));db.addMemory("MEMORY_FIXTURE_NOT_FOR_LAB")
        val old=db.recentMessages(100);val memories=db.allMemories()
        val settings=SettingsStore(ctx).apply { save(JarvisSettings(systemPrompt="SYSTEM fixture",speakReplies=true)) }
        val preferences=ctx.getSharedPreferences("jarvis_settings_v2",0).all.toMap()
        db.writableDatabase.execSQL("CREATE TRIGGER reject_lab_message BEFORE INSERT ON messages BEGIN SELECT RAISE(ABORT, 'lab must not write'); END")
        db.writableDatabase.execSQL("CREATE TRIGGER reject_lab_memory BEFORE INSERT ON memories BEGIN SELECT RAISE(ABORT, 'lab must not write'); END")
        val backend=FakeLlamaBackend()
        val native=LlamaLocalEngine({true},"synthetic.gguf",backend,ProcessInferenceAdmission())
        val controller=InferenceController(isolatedInferenceFactory({native}))
        val core=JarvisCore(db,RagEngine(db),controller,settings)
        val lab=TextLabConversation(core::chatVolatile);lab.setActive(true)
        lab.chat("bonjour") // Must reach the backend, bypassing historical canned greeting.
        assertEquals(listOf("UTILISATEUR: bonjour\nASSISTANT:","SYSTEM fixture"),backend.arguments)
        lab.chat("question de suivi")
        assertTrue(backend.arguments[0].contains("USER: bonjour"));assertTrue(backend.arguments[0].contains("ASSISTANT: answer"))
        assertFalse(backend.arguments[0].contains("DURABLE_FIXTURE"));assertFalse(backend.arguments[0].contains("MEMORY_FIXTURE"))
        assertEquals(1,backend.loads);assertEquals(0,backend.releases)
        lab.chat("/remember DONNEE_SYNTHETIQUE")
        assertTrue(backend.arguments[0].contains("/remember DONNEE_SYNTHETIQUE"))
        assertEquals(old,db.recentMessages(100));assertEquals(memories,db.allMemories())
        assertEquals(preferences,ctx.getSharedPreferences("jarvis_settings_v2",0).all)
        assertEquals(StopResult.UNSUPPORTED,native.requestStop())
        controller.unload();db.close()
    }
    @Test fun oversizedQuestionRefusesBeforeAnyModelLoad() = runTest {
        val db=LocalDatabase(ctx);val b=FakeLlamaBackend()
        val e=LlamaLocalEngine({true},"synthetic.gguf",b,ProcessInferenceAdmission())
        val c=InferenceController(isolatedInferenceFactory({e}));val core=JarvisCore(db,RagEngine(db),c,SettingsStore(ctx))
        assertTrue(runCatching {core.chatVolatile("x".repeat(5000),emptyList())}.isFailure)
        assertEquals(0,b.loads);assertTrue(db.recentMessages().isEmpty());db.close()
    }
    @Test fun emptyNativeResultIsNotReplacedWithALabSuccess() = runTest {
        val db=LocalDatabase(ctx);val b=FakeLlamaBackend().apply {answer=CompletableDeferred("")}
        val e=LlamaLocalEngine({true},"synthetic.gguf",b,ProcessInferenceAdmission())
        val c=InferenceController(isolatedInferenceFactory({e}));val core=JarvisCore(db,RagEngine(db),c,SettingsStore(ctx))
        assertTrue(runCatching {core.chatVolatile("public fixture",emptyList())}.isFailure)
        assertTrue(db.recentMessages().isEmpty());c.unload();db.close()
    }
    @Test fun historicalGreetingAndStorageRemainTheDefaultCorePath() = runTest {
        val db=LocalDatabase(ctx);val b=FakeLlamaBackend()
        val e=LlamaLocalEngine({true},"synthetic.gguf",b,ProcessInferenceAdmission())
        val c=InferenceController(isolatedInferenceFactory({e}));val core=JarvisCore(db,RagEngine(db),c,SettingsStore(ctx))
        val answer=core.chat("bonjour")
        assertTrue(answer.text.contains("Bonjour"));assertEquals(2,db.recentMessages().size);assertEquals(0,b.loads);db.close()
    }
    @Test fun stopRejectsLateNativeResultWithoutFreeingResidentModel() = runTest {
        val db=LocalDatabase(ctx);val b=FakeLlamaBackend().apply {answer=CompletableDeferred()}
        val e=LlamaLocalEngine({true},"synthetic.gguf",b,ProcessInferenceAdmission())
        val c=InferenceController(isolatedInferenceFactory({e}));val core=JarvisCore(db,RagEngine(db),c,SettingsStore(ctx))
        val lab=TextLabConversation(core::chatVolatile);lab.setActive(true)
        val job=launch {runCatching {lab.chat("public pending fixture")}}
        runCurrent();lab.invalidatePending();c.requestStop();job.cancel();runCurrent()
        assertFalse(job.isCompleted);assertEquals(0,b.releases)
        b.answer!!.complete("synthetic late response");runCurrent()
        assertTrue(job.isCompleted);assertEquals(1,lab.state.value.messages.size)
        assertTrue(db.recentMessages().isEmpty());assertTrue(c.isLoaded());assertEquals(0,b.releases)
        c.unload();assertEquals(1,b.releases);db.close()
    }
    @Test fun labCannotEscapeFailedUnloadOrLoadAnotherBackend() = runTest {
        val db=LocalDatabase(ctx);val b=FakeLlamaBackend()
        val e=LlamaLocalEngine({true},"synthetic.gguf",b,ProcessInferenceAdmission())
        var creations=0
        val c=InferenceController(isolatedInferenceFactory({creations++;e}))
        val core=JarvisCore(db,RagEngine(db),c,SettingsStore(ctx))
        core.chatVolatile("public fixture",emptyList());b.failRelease=true
        assertEquals(UnloadResult.FAILED,c.unload());assertTrue(c.state.value.quarantined)
        val lab=TextLabConversation(core::chatVolatile);lab.setActive(true)
        assertTrue(runCatching {lab.chat("second fixture")}.isFailure)
        assertEquals(1,creations);assertEquals(1,b.loads);assertEquals(1,b.releases)
        assertTrue(c.state.value.quarantined);assertTrue(db.recentMessages().isEmpty());db.close()
    }

}
