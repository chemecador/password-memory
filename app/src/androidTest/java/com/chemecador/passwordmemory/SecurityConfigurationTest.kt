package com.chemecador.passwordmemory

import android.Manifest
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityConfigurationTest {

    @Test
    fun mergedManifestHasNoNetworkOrAdvertisingPermissionAndDisablesSystemBackup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
        }
        val requestedPermissions = packageInfo.requestedPermissions.orEmpty().toSet()
        val forbiddenPermissions = setOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            ADVERTISING_ID_PERMISSION
        )

        assertTrue(
            "Forbidden permissions: ${requestedPermissions.intersect(forbiddenPermissions)}",
            requestedPermissions.intersect(forbiddenPermissions).isEmpty()
        )
        assertEquals(
            0,
            packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_ALLOW_BACKUP
        )
    }

    @Test
    fun mainActivityBlocksScreenshotsAndRecentsCapture() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val flags = activity.window.attributes.flags
                assertTrue(flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            }
        }
    }

    private companion object {
        const val ADVERTISING_ID_PERMISSION = "com.google.android.gms.permission.AD_ID"
    }
}
