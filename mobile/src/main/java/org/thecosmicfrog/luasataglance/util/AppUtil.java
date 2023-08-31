/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2019 Aaron Hastings
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
 * along with Luas at a Glance.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.thecosmicfrog.luasataglance.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

public final class AppUtil {

    /**
     * Check whether or not we are in an Android emulator.
     * @return Whether or not we are in an Android emulator.
     */
    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    /**
     * If the user has changed the permissions of Luas at a Glance using the Android system settings, ensure the "ShouldNotAskAgain"
     * preferences are reset.
     * @param context Context.
     */
    public static void resetShouldNotAskAgainIfPermissionsChangedOutsideApp(Context context) {
        int fineLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarseLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
        int notificationsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS);

        boolean locationPermissionGranted =
                fineLocationPermission == PackageManager.PERMISSION_GRANTED ||
                coarseLocationPermission == PackageManager.PERMISSION_GRANTED;
        boolean notificationsPermissionGranted = notificationsPermission == PackageManager.PERMISSION_GRANTED;

        if (locationPermissionGranted) {
            Preferences.savePermissionLocationShouldNotAskAgain(context, false);
        }

        if (notificationsPermissionGranted) {
            Preferences.savePermissionNotificationsShouldNotAskAgain(context, false);
        }
    }
}
