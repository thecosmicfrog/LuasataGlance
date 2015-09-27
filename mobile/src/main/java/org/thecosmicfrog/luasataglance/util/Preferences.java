/**
 * @author Aaron Hastings
 *
 * Copyright 2015 Aaron Hastings
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

public final class Preferences {
    /**
     * Load the currently-selected stop name from shared preferences.
     * @param context Context
     * @return Selected stop name, or null if none found.
     */
    public static String loadSelectedStopName(Context context) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance.StopForecastWidget";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        return prefs.getString("selectedStopName", null);
    }

    /**
     * Save the currently-selected stop name to shared preferences.
     * @param context Context.
     * @param selectedStopName Name of the stop to save to shared preferences.
     * @return Successfully saved.
     */
    public static boolean saveSelectedStopName(Context context, String selectedStopName) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance.StopForecastWidget";

        SharedPreferences.Editor prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();

        prefs.putString("selectedStopName", selectedStopName);

        return prefs.commit();
    }

    /**
     * Load index of the next stop to load from shared preferences.
     * @param context Context
     * @return Index of the next stop to load, or 0 (first list entry) if none found.
     */
    public static int loadIndexNextStopToLoad(Context context) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance.StopForecastWidget";

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
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance.StopForecastWidget";

        SharedPreferences.Editor prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();

        prefs.putInt("indexNextStopToLoad", indexNextStopToLoad);

        return prefs.commit();
    }
}
