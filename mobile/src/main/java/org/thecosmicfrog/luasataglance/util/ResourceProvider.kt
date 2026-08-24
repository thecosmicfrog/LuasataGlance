/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2026 Aaron Hastings
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

package org.thecosmicfrog.luasataglance.util

import android.content.Context
import androidx.annotation.StringRes
import org.thecosmicfrog.luasataglance.model.Stops

/**
 * A simple wrapper around the Android Context to provide string resources.
 * Required for accessing string resources in ViewModel.
 * https://stackoverflow.com/a/61532615
 *
 * @param context Context.
 */
class ResourceProvider(private val context: Context) {

    /**
     * The string with the given resource ID.
     *
     * @param resId String resource ID.
     * @return The string.
     */
    fun getString(@StringRes resId: Int): String {
        return context.getString(resId)
    }

    /**
     * Translates a tram destination from the English name the API sends into the language the app is running in.
     *
     * @param apiName Destination as it arrived from the API, always English.
     * @return The translated name, or [apiName] unchanged when the destination is not one of our stops.
     */
    fun localiseApiName(apiName: String?): String? {
        return Stops.localiseApiName(context, apiName)
    }

    /**
     * The stop ID for a stop name shown to the user, for example the one picked in the spinner.
     *
     * @param stopName Stop name as displayed.
     * @return The stop ID, or null if no stop in this language has that name.
     */
    fun stopId(stopName: String?): String? {
        return Stops.idForName(context, stopName)
    }
}
