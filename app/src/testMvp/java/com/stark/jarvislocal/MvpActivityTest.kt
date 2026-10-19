package com.stark.jarvislocal

import android.content.pm.PackageManager
import android.os.Looper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[24,25,35],application=MvpApplication::class)
class MvpActivityTest {
    @Test fun isolatedLauncherHasNoModelNoNetworkAndNoLegacyDatabase() {
        val controller=Robolectric.buildActivity(MvpActivity::class.java).setup()
        try {
            val app=RuntimeEnvironment.getApplication() as MvpApplication
            assertEquals("com.stark.jarvislocal.mvp001",app.packageName)
            val perms=app.packageManager.getPackageInfo(app.packageName,PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
            assertFalse(perms.contains("android.permission.INTERNET"));assertFalse(perms.contains("android.permission.RECORD_AUDIO"))
            assertFalse(app.runtime.state.value.loaded);assertFalse(app.getDatabasePath("jarvis_local_v2.db").exists())
            shadowOf(Looper.getMainLooper()).idle()
        } finally {controller.pause().stop().destroy()}
    }
    @Test fun rotationReusesApplicationRuntimeWithoutPreparingModel() {
        val controller=Robolectric.buildActivity(MvpActivity::class.java).setup()
        try {
            val app=RuntimeEnvironment.getApplication() as MvpApplication;val runtime=app.runtime
            controller.configurationChange()
            assertSame(runtime,app.runtime);assertFalse(runtime.state.value.loaded)
        } finally {controller.pause().stop().destroy()}
    }
}
