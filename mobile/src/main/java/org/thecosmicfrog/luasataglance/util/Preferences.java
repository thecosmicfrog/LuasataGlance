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
 * along with Luas at a Glance.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.thecosmicfrog.luasataglance.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.thecosmicfrog.luasataglance.R;

public final class Preferences {

    /*
     * ============================================================================================
     *  Load from shared preferences.
     * ============================================================================================
     */

    /**
     * Load the current app version from shared preferences.
     * @param context Context.
     * @return Current app version, or -1 if not found.
     */
    public static String currentAppVersion(Context context) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        return prefs.getString("currentAppVersion", "-1");
    }

    /**
     * Load the currently-selected stop name from shared preferences.
     * @param context Context.
     * @return Selected stop name, or null if none found.
     */
    public static String defaultStopName(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        return prefs.getString(
                context.getString(R.string.pref_key_default_stop),
                context.getString(R.string.none)
        );
    }

    /**
     * Whether or not a particular tutorial has been completed by the user.
     * @param context Context.
     * @return Tutorial completed.
     */
    public static boolean hasRunOnce(Context context, String tutorialName) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        return prefs.getBoolean(tutorialName, false);
    }

    /**
     * Load index of the next stop to load from shared preferences.
     * @param context Context.
     * @return Index of the next stop to load, or 0 (first list entry) if none found.
     */
    public static int indexNextStopToLoad(Context context) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        return prefs.getInt("indexNextStopToLoad", 0);
    }

    /**
     * Load name of stop-to-notify-for from shared preferences.
     * @param context Context.
     * @return Stop-to-notify-for.
     */
    public static String notifyStopName(Context context) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        return prefs.getString("notifyStopName", null);
    }

    /**
     * Load integer value of minutes until next tram from shared preferences.
     * @param context Context.
     * @return Integer value of minutes until next tram, or 0 if none found.
     */
    public static int notifyStopTimeExpected(Context context) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        return prefs.getInt("notifyStopTimeExpected", 0);
    }

    /**
     * Whether or not a user should be prompted for location permission.
     * @param context Context.
     * @return User should be asked again.
     */
    public static boolean permissionLocationShouldNotAskAgain(Context context) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        return prefs.getBoolean("permission_location_should_not_ask_again", false);
    }

    /**
     * Whether or not a user should be prompted for notifications permission.
     * @param context Context.
     * @return User should be asked again.
     */
    public static boolean permissionNotificationsShouldNotAskAgain(Context context) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        return prefs.getBoolean("permission_notifications_should_not_ask_again", false);
    }

    /**
     * Load the device screen height in DP from shared preferences.
     * @param context Context.
     * @return Device screen height in DP, or -1.0 if none found.
     */
    public static float screenHeight(Context context) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        return prefs.getFloat("screenHeight", -1.0f);
    }

    /**
     * Load the currently-selected stop name from shared preferences.
     * @param context Context.
     * @return Selected stop name, or null if none found.
     */
    public static String selectedStopName(Context context, String line) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (line.equalsIgnoreCase("no_line")) {
            return prefs.getString("selectedStopName", null);
        }

        return prefs.getString(line + "_selectedStopName", null);
    }

    /**
     * Load the currently-selected stop name from shared preferences.
     * @param context Context.
     * @return Selected stop name, or null if none found.
     */
    public static String widgetSelectedStopName(Context context) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        return prefs.getString("widgetSelectedStopName", null);
    }


    /*
     * ============================================================================================
     *  Save to shared preferences.
     * ============================================================================================
     */

    /**
     * Save the current app version according to strings.xml.
     * @param context Context.
     * @param currentAppVersion Current app version according to strings.xml.
     * @return Successfully saved.
     */
    public static boolean saveCurrentAppVersion(Context context, String currentAppVersion) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences.Editor prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();

        prefs.putString("currentAppVersion", currentAppVersion);

        return prefs.commit();
    }

    /**
     * Store boolean value of whether a particular tutorial have been completed by the user.
     * @param context Context.
     * @param tutorialName Tutorial that has been completed or not.
     * @param hasRun Whether or not tutorial has been completed.
     * @return Successfully saved.
     */
    public static boolean saveHasRunOnce(Context context, String tutorialName, boolean hasRun) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences.Editor prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();

        prefs.putBoolean(tutorialName, hasRun);

        return prefs.commit();
    }

    /**
     * Save the currently-selected stop name to shared preferences.
     * @param context Context.
     * @param indexNextStopToLoad Index of the next stop to load to save to shared preferences.
     * @return Successfully saved.
     */
    public static boolean saveIndexNextStopToLoad(Context context, int indexNextStopToLoad) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences.Editor prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();

        prefs.putInt("indexNextStopToLoad", indexNextStopToLoad);

        return prefs.commit();
    }

    /**
     * Save name of stop-to-notify-for to shared preferences.
     * @param context Context.
     * @param notifyStopName Name of stop to notify for.
     * @return Successfully saved.
     */
    public static boolean saveNotifyStopName(Context context, String notifyStopName) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences.Editor prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();

        prefs.putString("notifyStopName", notifyStopName);

        return prefs.commit();
    }

    /**
     * Save integer value of minutes until next tram to shared preferences.
     * @param context Context.
     * @param notifyStopTimeExpected Value of minutes until next tram.
     * @return Successfully saved.
     */
    public static boolean saveNotifyStopTimeExpected(Context context, int notifyStopTimeExpected) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences.Editor prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();

        prefs.putInt("notifyStopTimeExpected", notifyStopTimeExpected);

        return prefs.commit();
    }

    /**
     * Store boolean value of whether or not a user should be prompted for location permission.
     * @param context Context.
     * @param shouldNotAskAgain Whether or not user should be asked again.
     * @return Successfully saved.
     */
    public static boolean savePermissionLocationShouldNotAskAgain(Context context,
                                                                  boolean shouldNotAskAgain) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences.Editor prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();

        prefs.putBoolean("permission_location_should_not_ask_again", shouldNotAskAgain);

        return prefs.commit();
    }

    /**
     * Store boolean value of whether or not a user should be prompted for location permission.
     * @param context Context.
     * @param shouldNotAskAgain Whether or not user should be asked again.
     * @return Successfully saved.
     */
    public static boolean savePermissionNotificationsShouldNotAskAgain(Context context,
                                                                  boolean shouldNotAskAgain) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences.Editor prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();

        prefs.putBoolean("permission_notifications_should_not_ask_again", shouldNotAskAgain);

        return prefs.commit();
    }

    /**
     * Save the device screen height in DP to shared preferences.
     * @param context Context.
     * @param screenHeight Device screen height in DP.
     * @return Successfully saved.
     */
    public static boolean saveScreenHeight(Context context,
                                           float screenHeight) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences.Editor prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();

        prefs.putFloat("screenHeight", screenHeight);

        return prefs.commit();
    }

    /**
     * Save the currently-selected stop name to shared preferences.
     * @param context Context.
     * @param selectedStopName Name of the stop to save to shared preferences.
     * @return Successfully saved.
     */
    public static boolean saveSelectedStopName(Context context, String line,
                                               String selectedStopName) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences.Editor prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();

        /*
         * Save two preferences. The currently-selected stop name, and the same thing but with a
         * currently-selected tab prefix.
         */
        prefs.putString("selectedStopName", selectedStopName);
        prefs.putString(line + "_selectedStopName", selectedStopName);

        return prefs.commit();
    }

    /**
     * Save the currently-selected stop name to shared preferences.
     * @param context Context.
     * @param widgetSelectedStopName Name of the stop to save to shared preferences.
     * @return Successfully saved.
     */
    public static boolean saveWidgetSelectedStopName(Context context,
                                                     String widgetSelectedStopName) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences.Editor prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();

        prefs.putString("widgetSelectedStopName", widgetSelectedStopName);

        return prefs.commit();
    }
}
