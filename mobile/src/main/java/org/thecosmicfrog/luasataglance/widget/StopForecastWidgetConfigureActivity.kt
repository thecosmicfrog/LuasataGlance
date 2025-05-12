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
package org.thecosmicfrog.luasataglance.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.util.SparseBooleanArray
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.util.Preferences
import org.thecosmicfrog.luasataglance.util.Serializer
import org.thecosmicfrog.luasataglance.widget.StopForecastWidget.Companion.updateAppWidget
import java.io.IOException
import androidx.core.util.size

/**
 * The configuration screen for the [StopForecastWidget] AppWidget.
 */
class StopForecastWidgetConfigureActivity : AppCompatActivity() {
    private val logTag: String = StopForecastWidgetConfigureActivity::class.java.simpleName

    private var mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var adapterSelectedStops: ArrayAdapter<String?>? = null
    private var checkedItems: SparseBooleanArray? = null
    private var selectedItems: MutableList<CharSequence?>? = null

    public override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        //        getSupportActionBar().setBackgroundDrawable(
//                new ColorDrawable(
//                        ContextCompat.getColor(getApplication(), R.color.luas_purple)
//                )
//        );

        /*
         * Set the result to CANCELED.  This will cause the widget host to cancel
         * out of the widget placement if the user presses the back button.
         */
        setResult(RESULT_CANCELED)

        setContentView(R.layout.stop_forecast_widget_configure)

        /*
         * Build arrays for Red Line and Green Line stops from resources, then create Lists
         * from those arrays. Finally, build a List of all stops by concatenating the first
         * two Lists.
         */
        val redLineArrayStops = getResources().getStringArray(R.array.array_stops_redline)
        val greenLineArrayStops = getResources().getStringArray(
            R.array.array_stops_greenline
        )

        val redLineListStops = listOf<String?>(*redLineArrayStops)
        val greenLineListStops = listOf<String?>(*greenLineArrayStops)

        val listAllStops: MutableList<String?> = ArrayList<String?>(redLineListStops)
        listAllStops.addAll(greenLineListStops)

        /* ArrayAdapter for favourite stops. */
        adapterSelectedStops = ArrayAdapter<String?>(
            applicationContext,
            R.layout.checkedtextview_stops,
            listAllStops
        )

        /*
         * Populate ListView with all stops on both lines.
         */
        val listViewStops = findViewById<ListView>(R.id.listview_stops)
        listViewStops.adapter = adapterSelectedStops
        listViewStops.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                /*
                     * When a list item is clicked, it is graphically "checked" and also added
                     * to a List of all currently selected stops.
                     */
                checkedItems = listViewStops.checkedItemPositions
                selectedItems = ArrayList<CharSequence?>()

                checkedItems?.let {
                    for (i in 0..<checkedItems!!.size) {
                        val pos = checkedItems!!.keyAt(i)

                        if (checkedItems!!.valueAt(i)) {
                            selectedItems?.add(adapterSelectedStops!!.getItem(pos))
                        }
                    }
                }
            }
        }

        /*
         * Use a Floating Action Button (FAB) to save the selected widget stops.
         */
        val fabWidgetSave =
            findViewById<ExtendedFloatingActionButton>(R.id.fab_widget_save)
        fabWidgetSave.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.message_success))
        fabWidgetSave.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                saveWidgetFavourites()
            }
        })

        /* Find the widget id from the intent. */
        val intent = getIntent()
        val extras = intent.extras
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        /*
         * If this activity was started with an intent without an app widget ID, finish with an error.
         */
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
        }
    }

    private fun saveWidgetFavourites() {
        val fileWidgetSelectedStops = "widget_selected_stops"

        try {
            if (selectedItems != null && !selectedItems!!.isEmpty()) {
                val file = openFileOutput(
                    fileWidgetSelectedStops,
                    MODE_PRIVATE
                )

                file.write(Serializer.serialize(selectedItems))

                file.close()
            }
        } catch (e: IOException) {
            Log.e(logTag, Log.getStackTraceString(e))
        }

        val context: Context = this@StopForecastWidgetConfigureActivity

        /* It is the responsibility of the configuration activity to update the app widget. */
        val appWidgetManager = AppWidgetManager.getInstance(context)
        updateAppWidget(context, appWidgetManager, mAppWidgetId)

        /* Make sure we pass back the original appWidgetId. */
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
        setResult(RESULT_OK, resultValue)

        /* Reset next index to load to avoid out of bounds exceptions. */
        Preferences.saveIndexNextStopToLoad(context, 0)

        /* We're finished here. Close the activity. */
        finish()
    }

    companion object {
        private const val PREFS_NAME = "org.thecosmicfrog.luasataglance.widget.StopForecastWidget"
        private const val PREF_PREFIX_KEY = "appwidget_"
    }
}
