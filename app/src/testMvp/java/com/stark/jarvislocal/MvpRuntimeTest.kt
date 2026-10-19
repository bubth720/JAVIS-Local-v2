package com.stark.jarvislocal

import android.app.Application
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[24,25,35],application=Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class MvpRuntimeTest {
    @get:Rule val temp=TemporaryFolder()
    private fun store(): MvpModelStore {
        val b=ByteArray(128)
        ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).put(byteArrayOf(0x47,0x47,0x55,0x46)).putInt(3).putLong(1).putLong(1)
        return MvpModelStore(File(temp.root,"store"),MvpImportPolicy(24,4096,0)).also {
            it.install(MvpModelManifest("synthetic","CC0",MessageDigest.getInstance("SHA-256").digest(b)
                .joinToString(""){v->"%02x".format(v.toInt() and 255)},128),{b.inputStream()},{})
        }
    }
    private class Fake : LocalLanguageModel {
        var loaded=false;var loads=0;var release=UnloadResult.CONFIRMED
        var wait:CompletableDeferred<Unit>?=null
        var warmWait:CompletableDeferred<Unit>?=null
        var system="";var prompt=""
        override suspend fun warmUp(settings:JarvisSettings){loads++;warmWait?.await();loaded=true}
        override suspend fun generate(prompt:String,systemPrompt:String,settings:JarvisSettings):String {
            this.prompt=prompt;system=systemPrompt;wait?.await();return "SYNTHETIC TEST RESPONSE"
        }
        override fun isLoaded()=loaded
        override suspend fun unload():UnloadResult {if(release==UnloadResult.CONFIRMED)loaded=false;return release}
    }
    @Test fun explicitLoadAndSeparatedPromptWithConversationContext()=runTest {
        val fake=Fake();val runtime=MvpRuntime(store(),InferenceController(isolatedInferenceFactory({fake})),this,{},StandardTestDispatcher(testScheduler))
        assertEquals(0,fake.loads)
        runtime.restore();runCurrent();assertFalse(runtime.state.value.loaded)
        runtime.load(JarvisSettings());runCurrent();assertTrue(runtime.state.value.loaded)
        runtime.send("question",JarvisSettings(systemPrompt="SYSTEM DISTINCT"));runCurrent()
        assertEquals("SYSTEM DISTINCT",fake.system);assertFalse(fake.prompt.contains("SYSTEM DISTINCT"))
        runtime.send("suivante",JarvisSettings(systemPrompt="SYSTEM DISTINCT"));runCurrent()
        assertTrue(fake.prompt.contains("question"));assertEquals(4,runtime.state.value.messages.size)
    }
    @Test fun cancellationDrainsAndRejectsConcurrentLoadAndLateResponse()=runTest {
        val fake=Fake();val runtime=MvpRuntime(store(),InferenceController(isolatedInferenceFactory({fake})),this,{},StandardTestDispatcher(testScheduler))
        runtime.load(JarvisSettings());runCurrent();fake.wait=CompletableDeferred()
        runtime.send("question",JarvisSettings());runCurrent();runtime.cancel();runCurrent()
        assertTrue(runtime.state.value.busy);assertFalse(runtime.load(JarvisSettings()))
        assertFalse(runtime.unload());fake.wait!!.complete(Unit);runCurrent()
        assertFalse(runtime.state.value.busy);assertTrue(runtime.state.value.loaded)
        assertFalse(runtime.state.value.messages.any{it.role=="assistant"})
        runtime.unload();runCurrent();assertFalse(runtime.state.value.loaded)
    }
    @Test fun timeoutRetainsBusyUntilRealSimulatedReturn()=runTest {
        val fake=Fake();val runtime=MvpRuntime(store(),InferenceController(isolatedInferenceFactory({fake})),this,{},StandardTestDispatcher(testScheduler),50)
        runtime.load(JarvisSettings());runCurrent();fake.wait=CompletableDeferred()
        runtime.send("question",JarvisSettings());runCurrent();advanceTimeBy(51);runCurrent()
        assertTrue(runtime.state.value.busy);assertFalse(runtime.send("second",JarvisSettings()))
        fake.wait!!.complete(Unit);runCurrent()
        assertFalse(runtime.state.value.messages.any{it.role=="assistant"});assertTrue(runtime.state.value.detail.contains("Délai"))
    }
    @Test fun failedReleaseBlocksReimportAndReload()=runTest {
        val s=store();val fake=Fake();val runtime=MvpRuntime(s,InferenceController(isolatedInferenceFactory({fake})),this,{},StandardTestDispatcher(testScheduler))
        runtime.load(JarvisSettings());runCurrent();fake.release=UnloadResult.FAILED
        runtime.unload();runCurrent();assertTrue(runtime.state.value.detail.contains("quarantaine"))
        var opened=false
        runtime.importModel(s.selected()!!){opened=true;s.selectedFile().inputStream()};runCurrent()
        assertFalse(opened);runtime.load(JarvisSettings());runCurrent();assertEquals(1,fake.loads)
    }
    @Test fun resourceRefusalHappensBeforeNativeLoad()=runTest {
        val fake=Fake();val runtime=MvpRuntime(store(),InferenceController(isolatedInferenceFactory({fake})),this,
            {error("synthetic budget refusal")},StandardTestDispatcher(testScheduler))
        runtime.load(JarvisSettings());runCurrent();assertEquals(0,fake.loads);assertFalse(runtime.state.value.busy)
    }
    @Test fun cancellationBeforeDispatchAndWarmupCannotOpenGateEarly()=runTest {
        val fake=Fake();val runtime=MvpRuntime(store(),InferenceController(isolatedInferenceFactory({fake})),this,{},StandardTestDispatcher(testScheduler))
        runtime.load(JarvisSettings());runtime.cancel();runCurrent();assertEquals(0,fake.loads);assertFalse(runtime.state.value.busy)
        fake.warmWait=CompletableDeferred();runtime.load(JarvisSettings());runCurrent()
        assertFalse(runtime.load(JarvisSettings()));runtime.cancel();runCurrent();assertTrue(runtime.state.value.busy)
        fake.warmWait!!.complete(Unit);runCurrent();assertFalse(runtime.state.value.busy);assertEquals(1,fake.loads)
    }
    @Test fun deterioratingResourceSignalInvalidatesOutputButKeepsBusyUntilReturn()=runTest {
        val fake=Fake();var safe=true
        val runtime=MvpRuntime(store(),InferenceController(isolatedInferenceFactory({fake})),this,
            {require(safe){"synthetic thermal refusal"}},StandardTestDispatcher(testScheduler))
        runtime.load(JarvisSettings());runCurrent();fake.wait=CompletableDeferred()
        runtime.send("question",JarvisSettings());runCurrent();safe=false
        advanceTimeBy(1001);runCurrent();assertTrue(runtime.state.value.busy)
        assertFalse(runtime.load(JarvisSettings()));fake.wait!!.complete(Unit);runCurrent()
        assertFalse(runtime.state.value.messages.any{it.role=="assistant"});assertTrue(runtime.state.value.loaded)
        runtime.unload();runCurrent();assertFalse(runtime.state.value.loaded)
    }
    @Test fun importedFileCannotBeChangedWhileResidentAndCanBeReselectedAfterRelease()=runTest {
        val s=store();val fake=Fake();val runtime=MvpRuntime(s,InferenceController(isolatedInferenceFactory({fake})),this,{},StandardTestDispatcher(testScheduler))
        runtime.load(JarvisSettings());runCurrent();var opened=false
        runtime.importModel(s.selected()!!){opened=true;s.selectedFile().inputStream()};runCurrent();assertFalse(opened)
        runtime.unload();runCurrent()
        runtime.importModel(s.selected()!!){opened=true;s.selectedFile().inputStream()};runCurrent();assertTrue(opened)
        runtime.load(JarvisSettings());runCurrent();assertEquals(2,fake.loads)
    }

}
