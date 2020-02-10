/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2020 Aaron Hastings
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

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.google.firebase.perf.FirebasePerformance;

public final class AppUtil {

    private static final String LOG_TAG = AppUtil.class.getSimpleName();

    /**
     * Enable or disable Firebase Performance collection.
     */
    public static void configureFirebasePerformanceCollection(Context context) {
        /* Disable Firebase Performance collection if we're running in Firebase Test Lab. */
        if (isRunningInFirebaseTestLab(context)) {
            Log.i(
                    LOG_TAG,
                    "Running in Firebase Test Lab. Disabling Firebase Performance collection."
            );

            FirebasePerformance.getInstance().setPerformanceCollectionEnabled(false);
        }
    }

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
     * Check whether or not we are running in Firebase Test Lab.
     * @return Whether or not we are running in Firebase Test Lab.
     */
    public static boolean isRunningInFirebaseTestLab(Context context) {
        String settingFirebaseTestLab =
                Settings.System.getString(context.getContentResolver(), "firebase.test.lab");

        Log.i(LOG_TAG, "Running in Firebase Test Lab.");

        return settingFirebaseTestLab != null && settingFirebaseTestLab.equals("true");
    }
}
