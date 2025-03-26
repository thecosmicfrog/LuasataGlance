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
package org.thecosmicfrog.luasataglance.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.api.RetrofitClient
import org.thecosmicfrog.luasataglance.model.EnglishGaeilgeMap
import org.thecosmicfrog.luasataglance.model.StopForecast
import org.thecosmicfrog.luasataglance.model.StopNameIdMap
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.StopForecastUtil
import retrofit2.HttpException
import java.io.BufferedInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.ObjectInput
import java.io.ObjectInputStream
import java.util.Locale

class WidgetListenerService : Service() {

    private val logTag: String = WidgetListenerService::class.java.getSimpleName()
    private val textViewStopName: Int = R.id.textview_stop_name
    private val textViewTapToLoadTimes: Int = R.id.textview_tap_to_load_times
    private val textViewInboundStop1Name: Int = R.id.textview_inbound_stop1_name
    private val textViewInboundStop1Time: Int = R.id.textview_inbound_stop1_time
    private val textViewInboundStop2Name: Int = R.id.textview_inbound_stop2_name
    private val textViewInboundStop2Time: Int = R.id.textview_inbound_stop2_time
    private val textViewOutboundStop1Name: Int = R.id.textview_outbound_stop1_name
    private val textViewOutboundStop1Time: Int = R.id.textview_outbound_stop1_time
    private val textViewOutboundStop2Name: Int = R.id.textview_outbound_stop2_name
    private val textViewOutboundStop2Time: Int = R.id.textview_outbound_stop2_time

    private val textViewInboundStopNames = intArrayOf(
        textViewInboundStop1Name,
        textViewInboundStop2Name
    )

    private val textViewInboundStopTimes = intArrayOf(
        textViewInboundStop1Time,
        textViewInboundStop2Time
    )

    private val textViewOutboundStopNames = intArrayOf(
        textViewOutboundStop1Name,
        textViewOutboundStop2Name
    )

    private val textViewOutboundStopTimes = intArrayOf(
        textViewOutboundStop1Time,
        textViewOutboundStop2Time
    )

    private var mapEnglishGaeilge: EnglishGaeilgeMap? = null
    private var listSelectedStops: MutableList<CharSequence?>? = null
    private var localeDefault: String? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startForeground(1, buildForegroundNotification().build())

        if (isNetworkAvailable(applicationContext)) {
            Log.i(logTag, "Network available. Starting WidgetListenerService.")

            /* Initialise correct locale. */
            localeDefault = Locale.getDefault().toString()

            val appWidgetManager: AppWidgetManager =
                AppWidgetManager.getInstance(applicationContext)

            val allWidgetIds: IntArray? = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)

            /* Ensure sure we actually have an array of widget IDs. */
            if (allWidgetIds != null) {
                for (widgetId in allWidgetIds) {
                    val views = RemoteViews(
                        applicationContext.packageName,
                        R.layout.stop_forecast_widget
                    )

                    if (loadListSelectedStops(applicationContext) != null) {
                        val selectedStopName: String?
                        listSelectedStops = loadListSelectedStops(getApplicationContext())

                        if (intent.hasExtra(Constant.SELECTED_STOP_NAME)) {
                            selectedStopName = intent.getStringExtra(Constant.SELECTED_STOP_NAME)
                        } else {
                            selectedStopName = listSelectedStops!!.get(0).toString()
                        }

                        saveSelectedStopName(applicationContext, selectedStopName)
                    }

                    loadStopForecast(
                        applicationContext,
                        appWidgetManager,
                        widgetId,
                        views,
                        intent.getStringExtra(Constant.SELECTED_STOP_NAME)
                    )

                    appWidgetManager.partiallyUpdateAppWidget(widgetId, views)
                }
            } else {
                Log.e(logTag, "No widget IDs received.")
                stopService(intent)
            }
        }

        /* Necessary? Trivial? Further research required. */
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        /* TODO: Return the communication channel to the service. */
        throw UnsupportedOperationException("Not yet implemented")
    }

    /**
     * Check if network is available.
     * @param context Context.
     * @return Network available or not.
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager: ConnectivityManager? =
            context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager?

        var networkInfo: NetworkInfo? = null
        if (connectivityManager != null) {
            networkInfo = connectivityManager.getActiveNetworkInfo()
        }

        return networkInfo != null && networkInfo.isConnected()
    }

    private fun loadStopForecast(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        views: RemoteViews,
        stopName: String?
    ) {
        if (stopName == null) return

        setIsLoading(appWidgetManager, widgetId, views, true)
        views.setViewVisibility(textViewTapToLoadTimes, View.GONE)
        views.setTextViewText(textViewStopName, stopName)
        saveSelectedStopName(context, stopName)

        mapEnglishGaeilge = EnglishGaeilgeMap()
        val mapStopNameId = StopNameIdMap(localeDefault)

        /* Launch coroutine in service scope. */
        CoroutineScope(Dispatchers.IO + Job()).launch {
            try {
                val response = RetrofitClient.apiMethods.getStopForecast(
                    action = "times",
                    ver = "3",
                    station = mapStopNameId[stopName]
                )

                if (response.isSuccessful) {
                    val apiTimes = response.body()
                    if (apiTimes != null) {
                        val stopForecast = StopForecastUtil.createStopForecast(apiTimes)
                        updateStopForecastUi(context, views, stopForecast)
                    }
                } else {
                    Log.e(logTag, "Error calling URL: ${response.raw().request.url}")
                    Log.e(logTag, "Response status code: ${response.code()}")
                    Log.e(logTag, "Response headers: ${response.headers()}")

                    response.errorBody()?.string()?.let { errorBody ->
                        Log.e(logTag, "Response error body: $errorBody")
                    }

                    Log.e(logTag, "Response message: ${response.message()}")

                    updateStopForecastUi(context, views, null)
                }

            } catch (e: Exception) {
                when (e) {
                    is IOException -> {
                        Log.e(logTag, "I/O error message: ${e.message}")
                        Log.e(logTag, "I/O error cause: ${e.cause}")
                    }
                    is HttpException -> {
                        Log.e(logTag, "HTTP error: ${e.message}")
                        Log.e(logTag, "HTTP error status code: ${e.code()}")
                        Log.e(logTag, "Response message: ${e.response()?.message()}")
                        e.response()?.errorBody()?.string()?.let { errorBody ->
                            Log.e(logTag, "Response error body: $errorBody")
                        }
                    }
                    else -> {
                        Log.e(logTag, "Unexpected error: ${e.message}")
                        Log.e(logTag, "Error type: ${e.javaClass.simpleName}")
                        e.printStackTrace()
                    }
                }

                updateStopForecastUi(context, views, null)
            } finally {
                setIsLoading(appWidgetManager, widgetId, views, false)
                appWidgetManager.partiallyUpdateAppWidget(widgetId, views)
                stopSelf()
                stopForeground(true)
            }
        }
    }

    /**
     * Make progress bar appear or disappear.
     * @param loading Whether or not progress bar should animate.
     */
    private fun setIsLoading(
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        views: RemoteViews,
        loading: Boolean
    ) {
        views.setProgressBar(
            R.id.progressbar,
            0,
            0,
            loading
        )

        appWidgetManager.partiallyUpdateAppWidget(widgetId, views)
    }

    /**
     * Save the currently-selected stop name to shared preferences.
     * @param context Context.
     * @param selectedStopName Name of the stop to save to shared preferences.
     * @return Successfully saved.
     */
    private fun saveSelectedStopName(context: Context, selectedStopName: String?): Boolean {
        val prefsName = "org.thecosmicfrog.luasataglance.StopForecastWidget"

        val prefs: SharedPreferences.Editor =
            context.getSharedPreferences(prefsName, MODE_PRIVATE).edit()

        prefs.putString("selectedStopName", selectedStopName)

        return prefs.commit()
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

            listSelectedStops = objectInput.readObject() as MutableList<CharSequence?>?

            /* Close files and streams. */
            objectInput.close()
            buffer.close()
            fileInput.close()

            return listSelectedStops
        } catch (_: ClassNotFoundException) {
            /*
             * If the favourites file doesn't exist, the user has probably not set up this
             * feature yet. Handle the exception gracefully by displaying a TextView with
             * instructions on how to add favourites.
             */
            Log.i(logTag, "Widget selected stops not yet set up.")
        } catch (_: FileNotFoundException) {
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
     * Update the current stop forecast with newer information from the server.
     * @param context Context.
     * @param stopForecast Latest stop forecast from server.
     */
    private fun updateStopForecastUi(context: Context, views: RemoteViews, stopForecast: StopForecast?) {
        val gaeilge = "ga"

        /* If a valid stop forecast exists... */
        if (stopForecast != null) {
            val status: String?
            var operatingNormally = false

            if (stopForecast.stopForecastStatusDirectionInbound.operatingNormally == true
                && stopForecast.stopForecastStatusDirectionOutbound.operatingNormally == true
            ) {
                operatingNormally = true
            }

            if (localeDefault!!.startsWith(gaeilge)) {
                status = getString(R.string.message_success)
            } else {
                status = stopForecast.message
            }

            /* A lot of Luas status messages relate to lifts being out of service. Ignore these. */
            if (operatingNormally || status!!.lowercase(Locale.getDefault()).contains("lift")) {
                /*
                 * No error message on server. Change the stop name TextView to green.
                 */
                views.setInt(
                    R.id.linearlayout_stop_name,
                    "setBackgroundResource",
                    R.color.message_success
                )
            } else {
                Log.w(logTag, "Server has returned a service disruption or error.")

                views.setInt(
                    R.id.linearlayout_stop_name,
                    "setBackgroundResource",
                    R.color.message_error
                )
            }


            /*
             * Pull in all trams from the StopForecast, but only display up to two inbound
             * and outbound trams.
             */
            if (stopForecast.inboundTrams != null) {
                if (stopForecast.inboundTrams.size == 0) {
                    views.setTextViewText(
                        textViewInboundStop1Name,
                        context.getString(R.string.no_trams_forecast_short)
                    )
                } else {
                    var inboundTram: String?

                    for (i in stopForecast.inboundTrams.indices) {
                        if (i < 2) {
                            if (localeDefault!!.startsWith(gaeilge)) {
                                inboundTram = mapEnglishGaeilge?.get(
                                    stopForecast.inboundTrams.get(i).destination
                                )
                            } else {
                                inboundTram =
                                    stopForecast.inboundTrams.get(i).destination
                            }

                            views.setTextViewText(textViewInboundStopNames[i], inboundTram)

                            if (stopForecast.inboundTrams
                                    .get(i).dueMinutes.equals("DUE", ignoreCase = true)
                            ) {
                                val dueMinutes: String?

                                if (localeDefault!!.startsWith(gaeilge)) {
                                    dueMinutes = mapEnglishGaeilge?.get("DUE")
                                } else {
                                    dueMinutes = "DUE"
                                }

                                views.setTextViewText(
                                    textViewInboundStopTimes[i],
                                    dueMinutes
                                )
                            } else if (localeDefault!!.startsWith(gaeilge)) {
                                views.setTextViewText(
                                    textViewInboundStopTimes[i],
                                    stopForecast.inboundTrams.get(i).dueMinutes + "n"
                                )
                            } else {
                                views.setTextViewText(
                                    textViewInboundStopTimes[i],
                                    stopForecast.inboundTrams.get(i).dueMinutes + "m"
                                )
                            }
                        }
                    }
                }
            }

            if (stopForecast.outboundTrams != null) {
                if (stopForecast.outboundTrams.size == 0) {
                    views.setTextViewText(
                        textViewOutboundStop1Name,
                        context.getString(R.string.no_trams_forecast_short)
                    )
                } else {
                    var outboundTram: String?

                    for (i in stopForecast.outboundTrams.indices) {
                        if (i < 2) {
                            if (localeDefault!!.startsWith(gaeilge)) {
                                outboundTram = mapEnglishGaeilge?.get(
                                    stopForecast.outboundTrams[i].destination
                                )
                            } else {
                                outboundTram =
                                    stopForecast.outboundTrams[i].destination
                            }

                            views.setTextViewText(textViewOutboundStopNames[i], outboundTram)

                            if (stopForecast.outboundTrams[i].dueMinutes.equals("DUE", ignoreCase = true)
                            ) {
                                val dueMinutes: String?

                                if (localeDefault!!.startsWith(gaeilge)) {
                                    dueMinutes = mapEnglishGaeilge?.get("DUE")
                                } else {
                                    dueMinutes = "DUE"
                                }

                                views.setTextViewText(
                                    textViewOutboundStopTimes[i],
                                    dueMinutes
                                )
                            } else if (localeDefault!!.startsWith(gaeilge)) {
                                views.setTextViewText(
                                    textViewOutboundStopTimes[i],
                                    stopForecast.outboundTrams[i].dueMinutes + "n"
                                )
                            } else {
                                views.setTextViewText(
                                    textViewOutboundStopTimes[i],
                                    stopForecast.outboundTrams[i].dueMinutes + "m"
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Log.e(logTag, "Error in stop forecast (equals null).")

            /*
             * If no stop forecast can be retrieved, set a generic error message and
             * change the colour of the message title box red.
             */
            views.setInt(
                R.id.linearlayout_stop_name,
                "setBackgroundResource",
                R.color.message_error
            )

            views.setTextViewText(
                textViewInboundStop1Name,
                getString(R.string.widget_message_error)
            )
        }
    }

    /**
     * Build a Notification to use when running WidgetListenerService in the foreground.
     * @return Notification with details on what is being executed.
     */
    private fun buildForegroundNotification(): NotificationCompat.Builder {
        /*
         * Create a NotificationManager.
         */
        val notificationManager: NotificationManager? =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager?

        val notificationChannel =
            NotificationChannel(
                "widgetGetStopForecast",
                "Widget get stop forecast",
                NotificationManager.IMPORTANCE_HIGH
            )

        /* Configure notification channel. */
        notificationChannel.description = "Widget get stop forecast"
        notificationChannel.importance = NotificationManager.IMPORTANCE_LOW

        notificationManager?.createNotificationChannel(notificationChannel)

        return NotificationCompat.Builder(
            applicationContext,
            "widgetGetStopForecast"
        )
            .setOngoing(true)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.widget_retrieving_stop_forecast))
            .setSmallIcon(R.drawable.laag_logo_notification)
    }
}
