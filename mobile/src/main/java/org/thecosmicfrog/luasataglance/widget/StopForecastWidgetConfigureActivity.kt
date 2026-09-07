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
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.activity.StopSelectActivity
import org.thecosmicfrog.luasataglance.util.Preferences
import java.io.IOException

/**
 * The configuration screen for the [StopForecastWidget] AppWidget. Extends [StopSelectActivity] and persists the selection through
 * [WidgetStopStore], keyed by the AppWidget ID being configured, then broadcasts an update to the widget.
 */
class StopForecastWidgetConfigureActivity : StopSelectActivity() {

    private val logTag: String = StopForecastWidgetConfigureActivity::class.java.simpleName

    private var mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override val toolbarTitleRes = R.string.title_activity_widget_select_stops
    override val fabTextRes = R.string.widget_select_confirm

    public override fun onCreate(bundle: Bundle?) {
        setResult(RESULT_CANCELED)

        /*
         * The AppWidget ID has to be resolved before super.onCreate, because the base class calls loadSavedStops during its own
         * onCreate and that is now keyed by this ID.
         */
        mAppWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        super.onCreate(bundle)

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) finish()
    }

    override fun loadSavedStops(): List<CharSequence> {
        return WidgetStopStore.load(this, mAppWidgetId) ?: emptyList()
    }

    override fun onSave() {
        /*
         * RESULT_OK on an empty selection leaves the launcher holding a widget with no stop to draw. Backing out rather than
         * saving still drops it, since onCreate sets RESULT_CANCELED.
         */
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, R.string.widget_select_at_least_one_stop, Toast.LENGTH_LONG).show()

            return
        }

        try {
            WidgetStopStore.save(this, mAppWidgetId, selectedItems)
            Preferences.saveWidgetSelectedStopName(
                this, mAppWidgetId, selectedItems[0].toString()
            )
        } catch (e: IOException) {
            Log.e(logTag, Log.getStackTraceString(e))
        }

        sendBroadcast(Intent(this, StopForecastWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(mAppWidgetId))
        })

        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId))
        Preferences.saveIndexNextStopToLoad(this, 0)
        finish()
    }
}
