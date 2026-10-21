package com.stark.jarvislocal

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** No JNI, no GGUF, no recorder and no actual SDK speech: software qualification only. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[24,25,35], application=Mvp2Application::class)
class Mvp3QualificationTest {
    private val app get()=RuntimeEnvironment.getApplication() as Mvp2Application
    private fun idle() {
        val end=System.nanoTime()+5_000_000_000
        while(app.runtime.state.value.busy && System.nanoTime()<end) {shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5)}
        assertFalse("operation must settle",app.runtime.state.value.busy)
        shadowOf(Looper.getMainLooper()).idle()
    }
    private fun all(v:View):List<View> = listOf(v)+(if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList())
    @Test fun isolatedDiagnosticPackageHasNoNetworkMicrophoneOrAutomaticModel() {
        val c=Robolectric.buildActivity(Mvp2Activity::class.java).setup()
        try {
            idle();assertEquals("com.stark.jarvislocal.mvp003",app.packageName)
            val p=app.packageManager.getPackageInfo(app.packageName,PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
            assertTrue(p.none{it in setOf("android.permission.INTERNET","android.permission.ACCESS_NETWORK_STATE","android.permission.RECORD_AUDIO")})
            assertFalse(app.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP != 0)
            assertFalse(app.runtime.state.value.loaded);assertNull(app.runtime.state.value.model)
            assertFalse(app.graph.microphoneEnabled);assertEquals(TtsStatus.STOPPED,app.graph.speech.status.value)
            assertTrue(disabledExtensions.none{it.available})
            assertTrue(all(c.get().findViewById(android.R.id.content)).filterIsInstance<TextView>().any{it.text.contains("diagnostic")})
        }finally{c.pause().stop().destroy();idle();app.graph.db.close()}
    }
    @Test fun absentModelRefusesLoadAndConversationWithoutWritingAssistantOrUser() {
        val c=Robolectric.buildActivity(Mvp2Activity::class.java).setup()
        try {
            idle();assertTrue(app.runtime.load(JarvisSettings()));idle()
            assertFalse(app.runtime.state.value.loaded);assertTrue(app.runtime.state.value.detail.contains("absent"))
            assertTrue(app.runtime.send("explique un quasar",JarvisSettings()));idle()
            assertTrue(app.runtime.state.value.detail.contains("Charger explicitement"))
            assertTrue(app.graph.db.recentMessages().isEmpty());assertTrue(app.graph.db.allMemories().isEmpty())
        }finally{c.pause().stop().destroy();idle();app.graph.db.close()}
    }
    @Test fun explicitMemoryDocumentsAndSettingsSurviveBackgroundRecreationAndDatabaseReopen() {
        val c=Robolectric.buildActivity(Mvp2Activity::class.java).setup()
        idle()
        val settings=JarvisSettings(systemPrompt="SYSTEM SYNTHETIQUE DISTINCT",maxTokens=128,speakReplies=true)
        assertTrue(app.runtime.send("/remember souvenir public de qualification",settings));idle()
        val id=app.graph.db.addDocument("synthetic.pdf","application/pdf",listOf("passage public synthétique"))
        val before=app.graph.db.recentMessages()
        val state=Bundle();c.pause().saveInstanceState(state).stop().destroy();idle()
        assertEquals(TtsStatus.STOPPED,app.graph.speech.status.value)
        val reopened=LocalDatabase(app)
        try {
            assertEquals(before,reopened.recentMessages())
            assertEquals("souvenir public de qualification",reopened.allMemories().single().text)
            assertEquals(id,reopened.allChunks().single().documentId)
            assertEquals(listOf("synthetic.pdf"),reopened.documentNames())
            assertEquals(settings.systemPrompt,SettingsStore(app).load().systemPrompt)
            assertFalse(SettingsStore(app).load().speakReplies)
        }finally{reopened.close()}
        val second=Robolectric.buildActivity(Mvp2Activity::class.java).setup(state)
        try {
            idle();assertSame(app.runtime,(second.get().application as Mvp2Application).runtime)
            assertFalse(app.runtime.state.value.loaded);assertEquals(before,app.runtime.state.value.messages)
            assertTrue(app.runtime.send("/docs",settings));idle()
            assertEquals(4,app.graph.db.recentMessages().size);assertEquals(1,app.graph.db.allMemories().size)
        }finally{second.pause().stop().destroy();idle();app.graph.db.close()}
    }
    @Test fun rotationDoesNotCreateSecondRuntimeOrWarmUpModel() {
        val c=Robolectric.buildActivity(Mvp2Activity::class.java).setup()
        try {
            idle();val runtime=app.runtime;c.recreate();idle()
            assertSame(runtime,app.runtime);assertFalse(runtime.state.value.loaded)
            assertEquals(TtsStatus.STOPPED,app.graph.speech.status.value)
        }finally{c.pause().stop().destroy();idle();app.graph.db.close()}
    }
    @Test fun resourceLimitsAndUnknownSensorsContinueToFailClosed() {
        val safe=MvpDeviceState(false,4L*1024*1024*1024,false,80,false,250,0)
        val bytes=512L*1024*1024;MvpSafety.check(bytes,safe)
        for(state in listOf(safe.copy(lowMemory=true),safe.copy(lowRam=true),safe.copy(availableBytes=1),
            safe.copy(batteryPercent=-1),safe.copy(batteryPercent=20),safe.copy(temperatureTenths=-1),
            safe.copy(temperatureTenths=400),safe.copy(thermalStatus=2))) {
            try{MvpSafety.check(bytes,state);fail("unsafe state accepted: $state")}catch(_:IllegalArgumentException){}
        }
        // API24-28: no thermal API; battery temperature guard remains mandatory.
        MvpSafety.check(bytes,safe.copy(thermalStatus=null))
        val policy=MvpImportPolicy();assertEquals(100L*1024*1024,policy.minimum)
        assertEquals(512L*1024*1024,policy.maximum);assertEquals(256L*1024*1024,policy.reserve)
    }
}
