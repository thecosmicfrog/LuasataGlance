/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2025 Aaron Hastings
 *
 * This file is part of Luas at a Glance.
 *
 * Luas at a Glance is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Luas at a Glance is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Luas at a Glance.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package org.thecosmicfrog.luasataglance.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import androidx.core.content.ContextCompat

object AppUtil {

    private val logTag = AppUtil::class.java.simpleName

    @JvmStatic
    fun getScreenHeight(windowManager: WindowManager, resources: Resources, context: Context) {
        val display: Display = windowManager.defaultDisplay
        val displayMetrics = DisplayMetrics()
        display.getMetrics(displayMetrics)

        val density: Float = resources.displayMetrics.density
        val dpHeight = displayMetrics.heightPixels / density

        Preferences.saveScreenHeight(
            context,
            dpHeight
        )
    }

    val isEmulator: Boolean
        get() = (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT)

    /**
     * Check whether or not we are running in Firebase Test Lab.
     * @return Whether or not we are running in Firebase Test Lab.
     */
    @JvmStatic
    fun isRunningInFirebaseTestLab(context: Context?): Boolean {
        val settingFirebaseTestLab = Settings.System.getString(
            context?.contentResolver,
            "firebase.test.lab"
        )

        return settingFirebaseTestLab != null && settingFirebaseTestLab == "true"
    }

    /**
     * If the user has changed the permissions of Luas at a Glance using the Android system settings, ensure the "ShouldNotAskAgain"
     * preferences are reset.
     * @param context Context.
     */
    fun resetShouldNotAskAgainIfPermissionsChangedOutsideApp(context: Context?) {
        val fineLocationPermission = ContextCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocationPermission = ContextCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_COARSE_LOCATION)
        val notificationsPermission = ContextCompat.checkSelfPermission(context!!, Manifest.permission.POST_NOTIFICATIONS)

        val locationPermissionGranted =
            fineLocationPermission == PackageManager.PERMISSION_GRANTED ||
                    coarseLocationPermission == PackageManager.PERMISSION_GRANTED
        val notificationsPermissionGranted = notificationsPermission == PackageManager.PERMISSION_GRANTED

        if (locationPermissionGranted) {
            Preferences.savePermissionLocationShouldNotAskAgain(context, false)
        }

        if (notificationsPermissionGranted) {
            Preferences.savePermissionNotificationsShouldNotAskAgain(context, false)
        }
    }
}

