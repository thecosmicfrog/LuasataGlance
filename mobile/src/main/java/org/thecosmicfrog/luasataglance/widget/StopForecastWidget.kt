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

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.activity.MainActivity
import org.thecosmicfrog.luasataglance.service.WidgetListenerService
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.Preferences
import java.io.BufferedInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.ObjectInput
import java.io.ObjectInputStream

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in
 * [StopForecastWidgetConfigureActivity]
 */
class StopForecastWidget : AppWidgetProvider() {

    private val textviewTapToLoadTimes = R.id.textview_tap_to_load_times
    private val textviewInboundStop1Name = R.id.textview_inbound_stop1_name
    private val textviewInboundStop1Time = R.id.textview_inbound_stop1_time
    private val textviewInboundStop2Name = R.id.textview_inbound_stop2_name
    private val textviewInboundStop2Time = R.id.textview_inbound_stop2_time
    private val textviewOutboundStop1Name = R.id.textview_outbound_stop1_name
    private val textviewOutboundStop1Time = R.id.textview_outbound_stop1_time
    private val textviewOutboundStop2Name = R.id.textview_outbound_stop2_name
    private val textviewOutboundStop2Time = R.id.textview_outbound_stop2_time
    private val stopForecastTimeoutMillis = 15000

    private val textviewInboundStopNames = intArrayOf(
        textviewInboundStop1Name,
        textviewInboundStop2Name
    )

    private val textviewInboundStopTimes = intArrayOf(
        textviewInboundStop1Time,
        textviewInboundStop2Time
    )

    private val textviewOutboundStopNames = intArrayOf(
        textviewOutboundStop1Name,
        textviewOutboundStop2Name
    )

    private val textviewOutboundStopTimes = intArrayOf(
        textviewOutboundStop1Time,
        textviewOutboundStop2Time
    )

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        /* Construct a RemoteViews model. */
        remoteViews = RemoteViews(context.packageName, R.layout.stop_forecast_widget)

        /* There may be multiple widgets active, so update all of them. */
        for (appWidgetId in appWidgetIds) {
            Log.i(logTag, "Widget updating with ID: $appWidgetId")

            updateAppWidget(context, appWidgetManager, appWidgetId)
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            Log.i(logTag, "Widget deleted with ID: $appWidgetId")
        }
    }

    override fun onEnabled(context: Context?) {
        Log.i(logTag, "Widget first created.")
    }

    override fun onDisabled(context: Context?) {
        Log.i(logTag, "Widget disabled.")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, StopForecastWidget::class.java)
        val appWidgetsIds = appWidgetManager.getAppWidgetIds(thisWidget)

        /* Construct a RemoteViews model. */
        remoteViews = RemoteViews(context.packageName, R.layout.stop_forecast_widget)

        var indexNextStopToLoad = Preferences.indexNextStopToLoad(context)
        val listSelectedStops: MutableList<*>? = loadListSelectedStops(context)

        if (listSelectedStops != null && intent.action != null) {
            /*
             * If the user taps the stop name, open the app at that stop.
             */
            if (intent.action == widgetClickStopName) {
                val stopName = Preferences.widgetSelectedStopName(context)

                context.startActivity(
                    Intent(
                        context,
                        MainActivity::class.java
                    ).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    ).putExtra(
                        Constant.STOP_NAME,
                        stopName
                    )
                )
            }

            /*
             * If the user taps one of the widget arrows, move to the next/previous stop.
             */
            if (intent.action == widgetClickArrowLeft) {
                /*
                 * Move on to the previous index in the list. If we're on the first index, reset
                 * back to the last index [listSelectedStops.size() - 1].
                 */
                if (indexNextStopToLoad != 0) indexNextStopToLoad--
                else indexNextStopToLoad = listSelectedStops.size - 1

                prepareLoadStopForecast(
                    context,
                    appWidgetManager,
                    appWidgetsIds,
                    remoteViews,
                    indexNextStopToLoad
                )
            }

            if (intent.action == widgetClickArrowRight) {
                /*
                 * Move on to the next index in the list. If we're on the last index, reset back to
                 * the first index (0).
                 */
                if (indexNextStopToLoad != listSelectedStops.size - 1) indexNextStopToLoad++
                else indexNextStopToLoad = 0

                prepareLoadStopForecast(
                    context,
                    appWidgetManager,
                    appWidgetsIds,
                    remoteViews,
                    indexNextStopToLoad
                )
            }

            /*
             * If the user taps the stop forecast display, load the forecast for that stop, setting
             * up a timeout as well.
             */
            if (intent.action == widgetClickStopForecast) {
                val loadLimitMillis = 2000

                /*
                 * Induce an artificial limit on number of allowed sequential clicks in order to
                 * prevent server hammering.
                 */
                if (SystemClock.elapsedRealtime() - stopForecastLastClickTime < loadLimitMillis) return

                stopForecastLastClickTime = SystemClock.elapsedRealtime()

                for (appWidgetId in appWidgetsIds) {
                    stopForecastTimeout(
                        appWidgetManager,
                        appWidgetId,
                        remoteViews,
                        stopForecastTimeoutMillis
                    )
                }
                prepareLoadStopForecast(
                    context,
                    appWidgetManager,
                    appWidgetsIds,
                    remoteViews,
                    indexNextStopToLoad
                )
            }
        }
    }

    /**
     * If we have a list of selected stops, get the next one we need to load, save it to local
     * storage, then fire up the service.
     * @param context Context.
     * @param appWidgetManager AppWidgetManager.
     * @param appWidgetsIds Array of all widget IDs.
     * @param remoteViews RemoteViews model to prepare loading of stop forecast for.
     * @param indexNextStopToLoad Index of next stop to load.
     */
    private fun prepareLoadStopForecast(context: Context, appWidgetManager: AppWidgetManager, appWidgetsIds: IntArray,
                                        remoteViews: RemoteViews?, indexNextStopToLoad: Int) {
        if (remoteViews == null) return

        clearStopForecast(remoteViews)

        Preferences.saveIndexNextStopToLoad(context, indexNextStopToLoad)

        for (appWidgetId in appWidgetsIds) {
            stopForecastTimeout(
                appWidgetManager,
                appWidgetId,
                remoteViews,
                stopForecastTimeoutMillis
            )

            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews)
        }

        val listSelectedStops: MutableList<*>? = loadListSelectedStops(context)

        if (listSelectedStops != null) {
            val selectedStopName = listSelectedStops[indexNextStopToLoad].toString()
            Preferences.saveWidgetSelectedStopName(context, selectedStopName)

            startWidgetListenerService(context, appWidgetsIds)
        }
    }

    /**
     * Clear the stop forecast currently displayed in the widget.
     * @param remoteViewToClear RemoteViews model to clear.
     */
    private fun clearStopForecast(remoteViewToClear: RemoteViews) {
        for (i in 0..1) {
            remoteViewToClear.setTextViewText(textviewInboundStopNames[i], "")
            remoteViewToClear.setTextViewText(textviewInboundStopTimes[i], "")

            remoteViewToClear.setTextViewText(textviewOutboundStopNames[i], "")
            remoteViewToClear.setTextViewText(textviewOutboundStopTimes[i], "")
        }
    }

    /**
     * Set up timeout for stop forecast, which clears the stop forecast and displays a holding
     * message after a set period.
     * This is a necessary evil due to their currently being no way for a widget to know when it
     * is "active" or "visible to the user". This implementation is in order to not hammer the
     * battery and network.
     * @param appWidgetManager AppWidgetManager.
     * @param appWidgetId App widget ID.
     * @param remoteViews RemoteViews.
     * @param timeoutTimeMillis The period (ms) after which the stop forecast should be considered
     * expired and cleared.
     */
    private fun stopForecastTimeout(appWidgetManager: AppWidgetManager, appWidgetId: Int, remoteViews: RemoteViews?,
                                    timeoutTimeMillis: Int) {
        /*
         * Create a Runnable to execute the clearStopForecast() method after a set delay.
         */
        val runnableClearStopForecastAfterTimeout: Runnable = object : Runnable {
            override fun run() {
                if (remoteViews == null) return

                clearStopForecast(remoteViews)

                remoteViews.setViewVisibility(
                    textviewTapToLoadTimes,
                    View.VISIBLE
                )

                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews)
            }
        }

        /*
         * If a Handler already exists, remove its callbacks and messages so we don't have multiple
         * timeouts firing.
         */
        if (handlerClearStopForecastAfterTimeout != null) {
            handlerClearStopForecastAfterTimeout?.removeCallbacksAndMessages(null)
        }

        /* Wait for a set delay, then trigger the clearing of the stop forecast via the Runnable. */
        handlerClearStopForecastAfterTimeout = Handler()
        handlerClearStopForecastAfterTimeout?.postDelayed(
            runnableClearStopForecastAfterTimeout,
            timeoutTimeMillis.toLong()
        )
    }

    companion object {
        private val logTag = StopForecastWidget::class.java.simpleName
        private val widgetClickStopName = "WidgetClickStopName"
        private val widgetClickArrowLeft = "WidgetClickArrowLeft"
        private val widgetClickArrowRight = "WidgetClickArrowRight"
        private val widgetClickStopForecast = "WidgetClickStopForecast"

        private var stopForecastLastClickTime: Long = 0

        private var remoteViews: RemoteViews? = null
        private var handlerClearStopForecastAfterTimeout: Handler? = null

        /**
         * Start WidgetListenerService, passing in the selected stop name.
         * @param context Context.
         * @param appWidgetsIds Array of all widget IDs.
         */
        private fun startWidgetListenerService(context: Context, appWidgetsIds: IntArray?) {
            val selectedStopName = Preferences.widgetSelectedStopName(context)

            /*
             * Prepare an Intent to start the WidgetListenerService. Pass in all the widget IDs.
             */
            val intentWidgetListenerService =
                Intent(context.applicationContext, WidgetListenerService::class.java)
            intentWidgetListenerService.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            intentWidgetListenerService.putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_IDS,
                appWidgetsIds
            )
            intentWidgetListenerService.putExtra(
                Constant.SELECTED_STOP_NAME,
                selectedStopName
            )

            /* Start the WidgetListenerService. */
            context.startForegroundService(intentWidgetListenerService)
        }

        /**
         * Load list of user-selected stops from file.
         * @param context Context.
         * @return List of user-selected stops.
         */
        private fun loadListSelectedStops(context: Context): MutableList<CharSequence?>? {
            val fileWidgetSelectedStops = "widget_selected_stops"

            try {
                /*
                 * Open the "widget_selected_stops" file and read in the List model of selected stops
                 * contained within.
                 */
                val fileInput: InputStream = context.openFileInput(fileWidgetSelectedStops)
                val buffer: InputStream = BufferedInputStream(fileInput)
                val objectInput: ObjectInput = ObjectInputStream(buffer)

                val listSelectedStops = objectInput.readObject() as MutableList<CharSequence?>?

                /* Close files and streams. */
                objectInput.close()
                buffer.close()
                fileInput.close()

                return listSelectedStops
            } catch (e: ClassNotFoundException) {
                /*
                 * If the favourites file doesn't exist, the user has probably not set up this
                 * feature yet. Handle the exception gracefully by displaying a TextView with
                 * instructions on how to add favourites.
                 */
                Log.i(logTag, "Widget selected stops not yet set up.")
            } catch (e: FileNotFoundException) {
                Log.i(logTag, "Widget selected stops not yet set up.")
            } catch (e: IOException) {
                /*
                 * Something has gone wrong; the file may have been corrupted. Delete the file.
                 */
                Log.e(logTag, Log.getStackTraceString(e))
                Log.i(logTag, "Deleting widget selected stops file.")
                context.deleteFile(fileWidgetSelectedStops)
            }

            return null
        }

        /**
         * Wrapper around updateAppWidget().
         * @param context Context.
         * @param appWidgetManager AppWidgetManager.
         * @param appWidgetId ID of widget to update.
         */
        @JvmStatic
        fun updateAppWidget(context: Context?, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            /*
             * Set up Intents to register taps on the widget.
             */
            val intentWidgetClickStopName = Intent(context, StopForecastWidget::class.java)
            val intentWidgetClickArrowLeft = Intent(context, StopForecastWidget::class.java)
            val intentWidgetClickArrowRight = Intent(context, StopForecastWidget::class.java)
            val intentWidgetClickStopForecast = Intent(context, StopForecastWidget::class.java)

            intentWidgetClickStopName.action = widgetClickStopName
            intentWidgetClickArrowLeft.action = widgetClickArrowLeft
            intentWidgetClickArrowRight.action = widgetClickArrowRight
            intentWidgetClickStopForecast.action = widgetClickStopForecast

            val pendingIntentWidgetClickStopName =
                PendingIntent.getBroadcast(context, 0, intentWidgetClickStopName, PendingIntent.FLAG_IMMUTABLE)
            val pendingIntentWidgetClickArrowLeft =
                PendingIntent.getBroadcast(context, 0, intentWidgetClickArrowLeft, PendingIntent.FLAG_IMMUTABLE)
            val pendingIntentWidgetClickArrowRight =
                PendingIntent.getBroadcast(context, 0, intentWidgetClickArrowRight, PendingIntent.FLAG_IMMUTABLE)
            val pendingIntentWidgetClickStopForecast =
                PendingIntent.getBroadcast(context, 0, intentWidgetClickStopForecast, PendingIntent.FLAG_IMMUTABLE)

            remoteViews?.setOnClickPendingIntent(
                R.id.textview_stop_name, pendingIntentWidgetClickStopName
            )
            remoteViews?.setOnClickPendingIntent(
                R.id.textview_stop_name_left_arrow, pendingIntentWidgetClickArrowLeft
            )
            remoteViews?.setOnClickPendingIntent(
                R.id.textview_stop_name_right_arrow, pendingIntentWidgetClickArrowRight
            )
            remoteViews?.setOnClickPendingIntent(
                R.id.linearlayout_stop_forecast, pendingIntentWidgetClickStopForecast
            )

            /* Instruct the widget manager to update the widget. */
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }
}
