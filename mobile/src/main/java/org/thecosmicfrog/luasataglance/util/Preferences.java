/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2016 Aaron Hastings
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
import android.util.Log;

import org.thecosmicfrog.luasataglance.R;

public final class Preferences {

    /**
     * Load the currently-selected stop name from shared preferences.
     * @param context Context
     * @return Selected stop name, or null if none found.
     */
    public static String loadSelectedStopName(Context context, String line) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (line.equalsIgnoreCase("no_line")) {
            return prefs.getString("selectedStopName", null);
        }

        return prefs.getString(line + "_selectedStopName", null);
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
     * Load the currently-selected stop name from shared preferences.
     * @param context Context
     * @return Selected stop name, or null if none found.
     */
    public static String loadWidgetSelectedStopName(Context context) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        return prefs.getString("widgetSelectedStopName", null);
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

    /**
     * Load the currently-selected stop name from shared preferences.
     * @param context Context
     * @return Selected stop name, or null if none found.
     */
    public static String loadDefaultStopName(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        return prefs.getString(
                context.getString(R.string.pref_key_default_stop),
                context.getString(R.string.none)
        );
    }

    /**
     * Load index of the next stop to load from shared preferences.
     * @param context Context
     * @return Index of the next stop to load, or 0 (first list entry) if none found.
     */
    public static int loadIndexNextStopToLoad(Context context) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        return prefs.getInt("indexNextStopToLoad", 0);
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
     * Load name of stop-to-notify-for from shared preferences.
     * @param context Context
     * @return Stop-to-notify-for.
     */
    public static String loadNotifyStopName(Context context) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        return prefs.getString("notifyStopName", null);
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
     * Load integer value of minutes until next tram from shared preferences.
     * @param context Context
     * @return Integer value of minutes until next tram, or 0 if none found.
     */
    public static int loadNotifyStopTimeExpected(Context context) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        return prefs.getInt("notifyStopTimeExpected", 0);
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
     * Whether or not a particular tutorial has been completed by the user.
     * @param context Context
     * @return Tutorial completed.
     */
    public static boolean loadHasRunOnce(Context context, String tutorialName) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        return prefs.getBoolean(tutorialName, false);
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
}
