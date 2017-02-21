/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2017 Aaron Hastings
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
    public static String loadScreenShape(Context context) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        return prefs.getString("screenShape", "round");
    }

    /**
     * Save the currently-selected stop name to shared preferences.
     * @param context Context.
     * @param screenShape Shape of the wearable's screen ("round" or "square").
     * @return Successfully saved.
     */
    public static boolean saveScreenShape(Context context, String screenShape) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance";

        SharedPreferences.Editor prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();

        prefs.putString("screenShape", screenShape);

        return prefs.commit();
    }
}
