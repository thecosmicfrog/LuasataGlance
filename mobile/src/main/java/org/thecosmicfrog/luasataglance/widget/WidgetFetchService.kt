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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.api.ApiMethods
import org.thecosmicfrog.luasataglance.api.ApiProvider
import kotlin.time.Duration.Companion.milliseconds

/**
 * Fetches the forecasts [StopForecastWidget] shows, as a short foreground service.
 */
class WidgetFetchService : Service() {

    /* Tests swap in a fake. */
    @VisibleForTesting
    internal var apiMethods: ApiMethods = ApiProvider.getApiMethods()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /* The fetch each widget is waiting on, so that a later tap can cancel an earlier one. */
    private val fetches = mutableMapOf<Int, Job>()

    /* stopSelf(lastStartId) does nothing if a newer tap has restarted the service. */
    private var lastStartId = 0

    private val widget = StopForecastWidget()

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Starts one widget's fetch, cancelling any that widget already had waiting.
     *
     * @param intent  Carries the widget ID, its stop, and how long to wait before fetching.
     * @param flags   Unused.
     * @param startId Identifies this start to [stopSelf].
     * @return [START_NOT_STICKY], since a fetch is not worth repeating after Android has killed the app.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        /* Android crashes the app if a service started with startForegroundService stops before calling this. */
        startInForeground()

        lastStartId = startId

        val appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val stopName = intent?.getStringExtra(EXTRA_STOP_NAME)

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || stopName == null) {
            stopIfIdle()

            return START_NOT_STICKY
        }

        val job = scope.launch {
            fetch(appWidgetId, stopName, intent.getLongExtra(EXTRA_DELAY_MS, 0L))
        }

        fetches.put(appWidgetId, job)?.cancel()

        job.invokeOnCompletion {
            /* A cancelled fetch finishes after its replacement is in the map. */
            if (fetches[appWidgetId] === job) fetches.remove(appWidgetId)
            stopIfIdle()
        }

        return START_NOT_STICKY
    }

    /**
     * Called after 3 minutes without a tap. Stops at once, since Android declares an ANR otherwise.
     *
     * @param startId The start that timed out.
     */
    override fun onTimeout(startId: Int) {
        scope.cancel()
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /**
     * Waits out the debounce, then fetches and draws the forecast.
     *
     * @param appWidgetId ID of the widget instance the forecast is for.
     * @param stopName    Stop to fetch a forecast for.
     * @param delayMs     Time to wait before fetching. Zero fetches straight away.
     */
    private suspend fun fetch(appWidgetId: Int, stopName: String, delayMs: Long) {
        delay(delayMs.milliseconds)

        val appWidgetManager = AppWidgetManager.getInstance(this)

        /* Otherwise a hung request leaves the placeholder rows up. */
        val finished = withTimeoutOrNull(FETCH_TIMEOUT_MS.milliseconds) {
            widget.fetchAndDisplayTrams(this@WidgetFetchService, appWidgetManager, appWidgetId, stopName, apiMethods)
        }

        /* The cancelled coroutine cannot draw, so the timeout is reported here. */
        if (finished == null) {
            Log.e("WidgetFetchService", "Timed out fetching forecast")
            widget.showErrorRow(this, appWidgetManager, appWidgetId)
        }
    }

    /**
     * Stops the service once no widget has a fetch running or waiting.
     */
    private fun stopIfIdle() {
        if (fetches.isEmpty()) stopSelf(lastStartId)
    }

    /**
     * Moves the service into the foreground. The notification is silent.
     */
    private fun startInForeground() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        notificationManager.createNotificationChannel(
            NotificationChannel(NOTIFICATION_CHANNEL_ID, getString(R.string.widget_notification_channel),
                NotificationManager.IMPORTANCE_MIN)
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.laag_logo_notification)
            .setContentTitle(getString(R.string.widget_notification_loading))
            .setSilent(true)
            .build()

        /* Android 13 and earlier have no short service type. */
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
        } else {
            0
        }

        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    companion object {
        @VisibleForTesting
        internal const val EXTRA_STOP_NAME = "org.thecosmicfrog.luasataglance.widget.EXTRA_STOP_NAME"

        @VisibleForTesting
        internal const val EXTRA_DELAY_MS = "org.thecosmicfrog.luasataglance.widget.EXTRA_DELAY_MS"

        private const val NOTIFICATION_CHANNEL_ID = "widgetFetch"

        /* NotifyTimesAlarmReceiver's reminder is 1. */
        private const val NOTIFICATION_ID = 2

        /* Under OkHttp's own 10 second timeouts. */
        @VisibleForTesting
        internal const val FETCH_TIMEOUT_MS = 8000L

        /**
         * Starts a fetch for one widget.
         *
         * Throws [android.app.ForegroundServiceStartNotAllowedException] outside a widget tap. Returns null if battery use is
         * Restricted.
         *
         * @param context     Context the service is started from.
         * @param appWidgetId ID of the widget instance the forecast is for.
         * @param stopName    Stop to fetch a forecast for.
         * @param delayMs     Time to wait before fetching. Zero fetches straight away.
         * @return The service, or null if Android refused to start it.
         */
        fun start(context: Context, appWidgetId: Int, stopName: String, delayMs: Long): ComponentName? {
            return context.startForegroundService(intent(context, appWidgetId, stopName, delayMs))
        }

        /**
         * Builds the Intent [start] sends.
         *
         * @param context     Context the Intent is addressed from.
         * @param appWidgetId ID of the widget instance the forecast is for.
         * @param stopName    Stop to fetch a forecast for.
         * @param delayMs     Time to wait before fetching.
         * @return An explicit Intent for this service.
         */
        @VisibleForTesting
        internal fun intent(context: Context, appWidgetId: Int, stopName: String, delayMs: Long): Intent {
            return Intent(context, WidgetFetchService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(EXTRA_STOP_NAME, stopName)
                putExtra(EXTRA_DELAY_MS, delayMs)
            }
        }
    }
}
