package com.stark.jarvislocal

import android.content.pm.PackageManager
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.Locale
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[24,25,35],application=Mvp2Application::class)
class Mvp2ActivityTest {
    private fun all(v:View):List<View> = listOf(v)+(if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList())
    private fun idle(app:Mvp2Application){val end=System.nanoTime()+5_000_000_000;while(app.runtime.state.value.busy&&System.nanoTime()<end){shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5)};assertFalse(app.runtime.state.value.busy)}
    @Test fun isolatedUnifiedScreenHasStaticAvatarAndVoiceDisabledAtLaunch() {
        val c=Robolectric.buildActivity(Mvp2Activity::class.java).setup();val app=RuntimeEnvironment.getApplication() as Mvp2Application
        try {
            idle(app);assertEquals("com.stark.jarvislocal.mvp002",app.packageName)
            val perms=app.packageManager.getPackageInfo(app.packageName,PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
            assertFalse(perms.contains("android.permission.INTERNET"));assertFalse(perms.contains("android.permission.RECORD_AUDIO"))
            assertEquals(TtsStatus.STOPPED,app.graph.speech.status.value);assertFalse(app.graph.speech.verifiedByUser)
            assertFalse(app.graph.microphoneEnabled);assertFalse(app.runtime.state.value.loaded)
            assertTrue(all(c.get().findViewById(android.R.id.content)).any{it is ImageView})
            assertTrue(disabledExtensions.none{it.available})
        }finally{c.pause().stop().destroy();idle(app);app.graph.db.close()}
    }
    @Test fun cancellingMemoryDialogWritesNoMemoryOrConversation() {
        val c=Robolectric.buildActivity(Mvp2Activity::class.java).setup();val app=RuntimeEnvironment.getApplication() as Mvp2Application
        try{
            idle(app)
            all(c.get().findViewById(android.R.id.content)).filterIsInstance<Button>().first{it.text=="Ajouter un souvenir confirmé"}.performClick()
            shadowOf(Looper.getMainLooper()).idle();val dialog=ShadowAlertDialog.getLatestAlertDialog()
            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick();shadowOf(Looper.getMainLooper()).idle()
            assertTrue(app.graph.db.allMemories().isEmpty());assertTrue(app.graph.db.recentMessages().isEmpty())
        }finally{c.pause().stop().destroy();idle(app);app.graph.db.close()}
    }
    @Test fun networkAndUninstalledVoicesAreRejectedWithoutDownloading() {
        fun voice(name:String,network:Boolean,features:Set<String>)=Voice(name,Locale.FRENCH,Voice.QUALITY_NORMAL,Voice.LATENCY_NORMAL,network,features)
        val network=voice("network",true,emptySet())
        val absent=voice("absent",false,setOf(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED))
        val installed=voice("installed",false,emptySet())
        assertNull(offlineFrenchVoice(setOf(network,absent)))
        assertEquals(installed,offlineFrenchVoice(linkedSetOf(network,absent,installed)))
    }
}
