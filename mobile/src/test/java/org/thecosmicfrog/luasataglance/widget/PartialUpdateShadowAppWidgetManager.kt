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
package org.thecosmicfrog.luasataglance.widget

import android.appwidget.AppWidgetManager
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import android.widget.TextView
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowAppWidgetManager
import org.thecosmicfrog.luasataglance.R

/**
 * Robolectric's [ShadowAppWidgetManager], which ignores partial updates, plus the reads the widget tests need. The shimmer, the
 * forecast, and the error row are all partial updates.
 */
@Implements(AppWidgetManager::class)
class PartialUpdateShadowAppWidgetManager : ShadowAppWidgetManager() {

    /**
     * Applies a partial update to the widget's view, as a launcher would.
     *
     * @param appWidgetId ID of the widget to update.
     * @param views       The update.
     */
    @Implementation
    protected fun partiallyUpdateAppWidget(appWidgetId: Int, views: RemoteViews) {
        views.reapply(RuntimeEnvironment.getApplication(), getViewFor(appWidgetId))
    }

    /**
     * Reads the destination text of every tram row, in order. Filler rows give empty strings.
     *
     * @param appWidgetId ID of the widget to read.
     * @return One entry per tram row.
     */
    fun destinations(appWidgetId: Int): List<String> {
        val container = container(appWidgetId)

        return (0 until container.childCount).mapNotNull { index ->
            container.getChildAt(index).findViewById<TextView>(R.id.tram_destination)?.text?.toString()
        }
    }

    /**
     * Counts the shimmer rows, as the rows with neither a destination nor a direction label.
     *
     * @param appWidgetId ID of the widget to read.
     * @return Number of shimmer rows.
     */
    fun placeholderRowCount(appWidgetId: Int): Int {
        val container = container(appWidgetId)

        return (0 until container.childCount).count { index ->
            val row = container.getChildAt(index)
            row.findViewById<View>(R.id.tram_destination) == null && row.findViewById<View>(R.id.textview_direction) == null
        }
    }

    /**
     * Reads the holding screen's text, or null when the holding screen is hidden.
     *
     * @param appWidgetId ID of the widget to read.
     * @return The text, or null.
     */
    fun holdingScreenText(appWidgetId: Int): String? {
        val view = getViewFor(appWidgetId)
        if (view.findViewById<View>(R.id.holding_screen).visibility != View.VISIBLE) return null

        return view.findViewById<TextView>(R.id.holding_screen_text).text.toString()
    }

    /**
     * Finds the widget's tram container.
     *
     * @param appWidgetId ID of the widget to read.
     * @return The container.
     */
    private fun container(appWidgetId: Int): ViewGroup = getViewFor(appWidgetId).findViewById(R.id.trams_container)
}
