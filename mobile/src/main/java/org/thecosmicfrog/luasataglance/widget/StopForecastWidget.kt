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

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.core.net.toUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.activity.MainActivity
import org.thecosmicfrog.luasataglance.api.ApiProvider
import org.thecosmicfrog.luasataglance.model.StopNameIdMap
import org.thecosmicfrog.luasataglance.model.Tram
import org.thecosmicfrog.luasataglance.util.Preferences
import org.thecosmicfrog.luasataglance.util.StopForecastUtil
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

/**
 * AppWidgetProvider for the Luas stop forecast home screen widget.
 *
 * Displays real-time inbound and outbound tram times for a user-selected stop. Supports multiple widget instances, stop navigation,
 * manual refresh, and automatic layout adjustment when the widget is resized.
 */
class StopForecastWidget : AppWidgetProvider() {

    /*
     * Without this the process can be killed before Retrofit answers. A new receiver instance is created per broadcast, so this
     * state belongs to one broadcast.
     */
    private var pendingResult: PendingResult? = null

    /*
     * goAsync gives up its result only once, but onUpdate can start a fetch per widget, so the one result is counted out and
     * finished when the last fetch is done.
     */
    private val outstandingFetches = AtomicInteger(0)

    /* SupervisorJob so one widget's failed fetch does not cancel its siblings. */
    private val fetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Called by the system on the widget's first placement and on each scheduled update. Triggers a full update (including a fresh
     * API fetch) for every active widget instance.
     *
     * @param context          Context.
     * @param appWidgetManager Manager the updates are pushed through.
     * @param appWidgetIds     IDs of every active widget instance.
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, true)
        }
    }

    /**
     * Called when the widget is resized by the user.
     *
     * Cancels any pending timeout alarm, then triggers a fresh fetch so that the number of visible tram rows is recalculated to fit
     * the new dimensions.
     *
     * @param context          Context.
     * @param appWidgetManager Manager the update is pushed through.
     * @param appWidgetId      ID of the widget instance that was resized.
     * @param newOptions       The instance's new options, as reported by the launcher.
     */
    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int,
                                           newOptions: Bundle?) {
        cancelTimeout(context, appWidgetId)
        updateAppWidget(context, appWidgetManager, appWidgetId, true)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    /**
     * Entry point for all broadcast intents directed at this widget.
     *
     * Routes each custom action to the appropriate handler:
     * - [ACTION_OPEN_APP]   — launches the main app at the currently selected stop
     * - [ACTION_REFRESH]    — cancels the timeout and forces a fresh forecast fetch
     * - [ACTION_PREV_STOP]  — navigates to the previous stop in the configured list
     * - [ACTION_NEXT_STOP]  — navigates to the next stop in the configured list
     * - [ACTION_TIMEOUT]    — shows the holding screen if data has not arrived in time
     *
     * @param context Context.
     * @param intent  The broadcast, carrying the action and the widget ID it applies to.
     */
    override fun onReceive(context: Context, intent: Intent) {
        pendingResult = goAsync()

        /* Held on behalf of this method, so a broadcast that starts no fetch still finishes. */
        outstandingFetches.incrementAndGet()

        try {
            super.onReceive(context, intent)
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

            when (intent.action) {
                ACTION_OPEN_APP -> openApp(context, appWidgetId)
                ACTION_REFRESH -> {
                    cancelTimeout(context, appWidgetId)
                    updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId, true)
                }
                ACTION_PREV_STOP -> changeStop(context, appWidgetId, -1)
                ACTION_NEXT_STOP -> changeStop(context, appWidgetId, 1)
                ACTION_TIMEOUT -> showHoldingScreen(context, appWidgetId)
            }
        } finally {
            releaseFetch()
        }
    }

    /**
     * Called when one or more widget instances are removed from the home screen.
     *
     * Cancels each instance's pending timeout alarm and deletes the state it owns, so that preferences and stop list files do not
     * accumulate for widgets that no longer exist.
     *
     * @param context      Context.
     * @param appWidgetIds IDs of the widget instances that were removed.
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            cancelTimeout(context, appWidgetId)
            Preferences.removeWidgetSelectedStopName(context, appWidgetId)
            WidgetStopStore.delete(context, appWidgetId)
        }
        super.onDeleted(context, appWidgetIds)
    }

    /**
     * Called when the last widget instance is removed.
     *
     * Clears the shared state left over from before storage became per-instance. The migration window closes with the last widget,
     * and leaving it would hand a stale stop to the next widget placed.
     *
     * @param context Context.
     */
    override fun onDisabled(context: Context) {
        Preferences.removeLegacyWidgetSelectedStopName(context)
        WidgetStopStore.deleteLegacy(context)
        super.onDisabled(context)
    }

    /**
     * Launches the main app, navigating directly to the stop currently selected in the widget.
     *
     * @param context     Context the Activity is started from.
     * @param appWidgetId ID of the widget instance whose stop is opened.
     */
    private fun openApp(context: Context, appWidgetId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("stopName", Preferences.widgetSelectedStopName(context, appWidgetId))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Navigates to the adjacent stop in the user's configured stop list.
     *
     * The name and placeholder rows are drawn immediately, but the forecast is fetched only after [STOP_CHANGE_DEBOUNCE_MS], so
     * that a user stepping through stops quickly does not spam the API.
     *
     * @param context     Context.
     * @param appWidgetId ID of the widget instance being navigated.
     * @param direction   -1 for previous, +1 for next. Wraps around at list boundaries.
     */
    private fun changeStop(context: Context, appWidgetId: Int, direction: Int) {
        cancelTimeout(context, appWidgetId)
        val widgetStops = WidgetStopStore.load(context, appWidgetId)
        if (widgetStops.isNullOrEmpty()) return

        val currentStopName = Preferences.widgetSelectedStopName(context, appWidgetId)
        val currentIndex = widgetStops.indexOf(currentStopName).takeIf { it != -1 } ?: 0

        val newIndex = (currentIndex + direction + widgetStops.size) % widgetStops.size
        val newStopName = widgetStops[newIndex]
        val appWidgetManager = AppWidgetManager.getInstance(context)

        Preferences.saveWidgetSelectedStopName(context, appWidgetId, newStopName)

        updateAppWidget(context, appWidgetManager, appWidgetId, false)
        showShimmer(context, appWidgetManager, appWidgetId, newStopName)

        launchFetch(context, appWidgetManager, appWidgetId, newStopName, STOP_CHANGE_DEBOUNCE_MS)

        /*
         * The fetch outlives this broadcast, so nothing is holding the process up. Set the revert now, in case it is killed
         * before the forecast arrives and the widget is left on its placeholder rows.
         */
        setTimeout(context, appWidgetId)
    }

    /**
     * Builds and pushes a [RemoteViews] update for the widget, then optionally kicks off an async forecast fetch.
     *
     * The synchronous portion sets the stop name, directional labels, and click handlers. If [forceUpdate] is true,
     * [fetchAndDisplayTrams] is launched on [Dispatchers.IO] to load live tram data.
     *
     * @param context          Context.
     * @param appWidgetManager Manager the update is pushed through.
     * @param appWidgetId      ID of the widget instance being updated.
     * @param forceUpdate      If true, a fresh API call is made after the UI is prepared.
     */
    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, forceUpdate: Boolean) {
        val views = RemoteViews(context.packageName, R.layout.stop_forecast_widget)
        val stopName = Preferences.widgetSelectedStopName(context, appWidgetId)

        if (stopName == null) {
            showHoldingScreen(context, appWidgetId, context.getText(R.string.widget_message_configure))

            return
        }

        views.setTextViewText(R.id.textview_stop_name, stopName)

        /* Only the inbound label is static. The outbound one is a row inside trams_container. */
        views.setTextViewText(R.id.textview_inbound, directionLabels(context, stopName).first)

        /* Hide holding screen and show forecast. */
        views.setViewVisibility(R.id.holding_screen, View.GONE)
        views.setViewVisibility(R.id.forecast_content, View.VISIBLE)

        /*
         * Set back explicitly because showErrorRow hides it. A partial update that hides a view outlives the next full update, so
         * relying on the layout default leaves it hidden.
         */
        views.setViewVisibility(R.id.textview_inbound, View.VISIBLE)

        /* Set up pending intents. */
        views.setOnClickPendingIntent(R.id.textview_stop_name, getPendingIntent(context, appWidgetId, ACTION_OPEN_APP))
        views.setOnClickPendingIntent(R.id.widget_body, getPendingIntent(context, appWidgetId, ACTION_REFRESH))
        views.setOnClickPendingIntent(R.id.button_prev_stop, getPendingIntent(context, appWidgetId, ACTION_PREV_STOP))
        views.setOnClickPendingIntent(R.id.button_next_stop, getPendingIntent(context, appWidgetId, ACTION_NEXT_STOP))

        appWidgetManager.updateAppWidget(appWidgetId, views)

        if (forceUpdate) {
            launchFetch(context, appWidgetManager, appWidgetId, stopName, 0L)
        }
    }

    /**
     * Launches the forecast fetch for one widget, optionally after a delay.
     *
     * Only one fetch is ever pending per widget. Starting another cancels the one before it, which is what keeps a run of arrow
     * taps down to a single request.
     *
     * @param context          Context.
     * @param appWidgetManager Manager the resulting update is pushed through.
     * @param appWidgetId      ID of the widget instance the forecast is for.
     * @param stopName         Stop to fetch a forecast for.
     * @param delayMs          Time to wait before fetching. Zero fetches straight away.
     */
    private fun launchFetch(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, stopName: String,
                            delayMs: Long) {
        /*
         * A delayed fetch cannot hold the broadcast open. The system delivers this receiver's broadcasts one at a time, so the
         * next tap would not arrive until this fetch had finished, and there would be nothing left to cancel.
         */
        val holdBroadcast = delayMs == 0L

        if (holdBroadcast) outstandingFetches.incrementAndGet()

        val job = fetchScope.launch {
            try {
                delay(delayMs.milliseconds)

                /*
                 * Bounded so a hung request cannot hold the broadcast open indefinitely. The timeout alarm still covers what
                 * the user sees.
                 */
                val finished = withTimeoutOrNull(FETCH_TIMEOUT_MS.milliseconds) {
                    fetchAndDisplayTrams(context, appWidgetManager, appWidgetId, stopName)
                }

                /*
                 * Reported out here because the coroutine that timed out was cancelled and cannot draw anything itself.
                 */
                if (finished == null) {
                    Log.e("StopForecastWidget", "Timed out fetching forecast")
                    showErrorRow(context, appWidgetManager, appWidgetId)
                }
            } finally {
                if (holdBroadcast) releaseFetch()
            }
        }

        pendingFetches.put(appWidgetId, job)?.cancel()
        job.invokeOnCompletion { pendingFetches.remove(appWidgetId, job) }
    }

    /**
     * Releases one claim on the broadcast, finishing it once nothing is left in flight.
     */
    private fun releaseFetch() {
        if (outstandingFetches.decrementAndGet() == 0) {
            pendingResult?.finish()
            pendingResult = null
        }
    }

    /**
     * Fetches the stop forecast from the API and updates the widget with real tram data.
     *
     * Shows shimmer placeholder rows while the request is in flight, then replaces them with live tram times on success. The number
     * of rows shown is calculated from the current widget height. Sets a 15-second [AlarmManager] timeout after a successful fetch.
     * On failure, the containers are left empty.
     *
     * @param context          Context.
     * @param appWidgetManager Manager the update is pushed through.
     * @param appWidgetId      ID of the widget instance being updated.
     * @param stopName         Stop to fetch a forecast for.
     */
    private suspend fun fetchAndDisplayTrams(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int,
                                             stopName: String) {
        val maxTotalTrams = maxTotalTramsFor(context, appWidgetManager, appWidgetId)
        val outboundLabel = directionLabels(context, stopName).second

        showShimmer(context, appWidgetManager, appWidgetId, stopName)

        val stopId = StopNameIdMap(Locale.getDefault().toString())[stopName]

        if (stopId == null) {
            Log.e("StopForecastWidget", "Could not find stop ID for stop: $stopName")
            showErrorRow(context, appWidgetManager, appWidgetId)

            return
        }

        /* Prepare a partial update that clears the shimmer rows. */
        val views = RemoteViews(context.packageName, R.layout.stop_forecast_widget)
        views.removeAllViews(R.id.trams_container)

        try {
            val response = ApiProvider.getApiMethods().getStopForecast("times", "3", stopId)

            if (!response.isSuccessful) {
                Log.e("StopForecastWidget", "Error fetching forecast: ${response.code()}")
                showErrorRow(context, appWidgetManager, appWidgetId)

                return
            }

            val stopForecast = response.body()?.let { StopForecastUtil.createStopForecast(it) }
            val inboundTrams = stopForecast?.inboundTrams ?: emptyList()
            val outboundTrams = stopForecast?.outboundTrams ?: emptyList()

            /*
             * A direction with no trams still needs a row to say so, so it asks the budget for one. Counting it here rather than
             * adding it afterwards is what stops an empty direction pushing the total past what fits when the other direction has a
             * full forecast.
             */
            val (inboundToShow, outboundToShow) = splitTramBudget(
                maxTotalTrams,
                if (inboundTrams.isEmpty()) 1 else inboundTrams.size,
                if (outboundTrams.isEmpty()) 1 else outboundTrams.size
            )

            /* Order matters. The label has to land between the two sets of rows. */
            addDirectionRows(context, views, R.id.trams_container, inboundTrams, inboundToShow)
            views.addView(R.id.trams_container, directionLabelView(context, outboundLabel))
            addDirectionRows(context, views, R.id.trams_container, outboundTrams, outboundToShow)
            addFillerRows(
                context,
                views,
                R.id.trams_container,
                maxTotalTrams - (inboundToShow + outboundToShow)
            )

            setTimeout(context, appWidgetId)
        } catch (e: CancellationException) {
            /* The fetch timeout. Rethrown so the caller sees and reports it, rather than being swallowed as a request failure. */
            throw e
        } catch (e: Exception) {
            Log.e("StopForecastWidget", "Exception fetching forecast", e)
            showErrorRow(context, appWidgetManager, appWidgetId)

            return
        }

        /*
         * Replace shimmer rows with real tram data, preserving the stop name, inbound label, and click handlers set by
         * updateAppWidget.
         */
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
    }

    /**
     * Fills the tram container with placeholder rows.
     *
     * Shown while a fetch is in flight or waiting out its debounce, so that stepping between stops does not leave the body empty.
     * The rows match the forecast's own layout, so nothing shifts when the data lands.
     *
     * @param context          Context.
     * @param appWidgetManager Manager the update is pushed through.
     * @param appWidgetId      ID of the widget instance being updated.
     * @param stopName         Stop the direction labels are resolved from.
     */
    private fun showShimmer(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, stopName: String) {
        val maxTotalTrams = maxTotalTramsFor(context, appWidgetManager, appWidgetId)
        val outboundLabel = directionLabels(context, stopName).second

        /* The same per-direction cap the forecast will use, so the rows do not jump. */
        val shimmerRowsPerDirection = maxTotalTrams / 2

        val views = RemoteViews(context.packageName, R.layout.stop_forecast_widget)

        views.removeAllViews(R.id.trams_container)
        repeat(shimmerRowsPerDirection) {
            views.addView(R.id.trams_container, RemoteViews(context.packageName, R.layout.item_tram_shimmer))
        }

        views.addView(R.id.trams_container, directionLabelView(context, outboundLabel))
        repeat(shimmerRowsPerDirection) {
            views.addView(R.id.trams_container, RemoteViews(context.packageName, R.layout.item_tram_shimmer))
        }

        addFillerRows(context, views, R.id.trams_container, maxTotalTrams - (shimmerRowsPerDirection * 2))

        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
    }

    /**
     * Replaces the forecast rows with a single row carrying the network error message.
     *
     * Rendered as a tram row rather than on the holding screen, so the header and the stop navigation stay usable. The body keeps
     * the refresh [PendingIntent] set by [updateAppWidget], so the error is recoverable by tapping it.
     *
     * Times out back to the holding screen the same way a loaded forecast does, so the widget does not sit on a stale error
     * indefinitely.
     *
     * @param context          Context.
     * @param appWidgetManager Manager the update is pushed through.
     * @param appWidgetId      ID of the widget instance being updated.
     */
    private fun showErrorRow(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.stop_forecast_widget)
        views.removeAllViews(R.id.trams_container)

        /* Neither direction has data, so its label would only be misleading. */
        views.setViewVisibility(R.id.textview_inbound, View.GONE)

        val errorView = RemoteViews(context.packageName, R.layout.item_tram)
        errorView.setTextViewText(R.id.tram_destination, context.getText(R.string.widget_message_error))
        views.addView(R.id.trams_container, errorView)

        /* Without the filler the single row would stretch to the full container height. */
        addFillerRows(context, views, R.id.trams_container, maxTotalTramsFor(context, appWidgetManager, appWidgetId) - 1)

        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)

        setTimeout(context, appWidgetId)
    }

    /**
     * Adds a row of [RemoteViews] tram entries to the given container.
     *
     * @param context        Context.
     * @param parent         RemoteViews the rows are added to.
     * @param containerId    Resource ID of the target tram container.
     * @param trams          Full list of trams for this direction.
     * @param tramsToDisplay Maximum number of rows to add.
     */
    private fun addTramViews(context: Context, parent: RemoteViews, containerId: Int, trams: List<Tram>, tramsToDisplay: Int) {
        trams.take(tramsToDisplay).forEach { tram ->
            val tramView = RemoteViews(context.packageName, R.layout.item_tram)
            tramView.setTextViewText(R.id.tram_destination, tram.destination)
            tramView.setTextViewText(R.id.tram_due_time, tram.dueMinutes)
            parent.addView(containerId, tramView)
        }
    }

    /**
     * Adds one direction's worth of rows, which is either its trams or a note that it has none.
     *
     * Both cases add exactly [rowCount] rows, never more, so that the caller can work out the filler count from the budget alone.
     * [splitTramBudget] never allocates a direction more rows than it asked for, and an empty direction asks for one.
     *
     * @param context     Context.
     * @param parent      RemoteViews the rows are added to.
     * @param containerId Resource ID of the target tram container.
     * @param trams       Full list of trams for this direction, possibly empty.
     * @param rowCount    Number of rows this direction has been allocated.
     */
    private fun addDirectionRows(context: Context, parent: RemoteViews, containerId: Int, trams: List<Tram>, rowCount: Int) {
        if (trams.isEmpty()) {
            repeat(rowCount) {
                val noTramsView = RemoteViews(context.packageName, R.layout.item_tram)

                /* getText, not getString, because the string carries bold markup. */
                noTramsView.setTextViewText(
                    R.id.tram_destination,
                    context.getText(R.string.no_trams_forecast_short)
                )

                parent.addView(containerId, noTramsView)
            }
        } else {
            addTramViews(context, parent, containerId, trams, rowCount)
        }
    }

    /**
     * Pads the container out to the full row count the height estimate was based on.
     *
     * These reuse `item_tram` rather than a layout of their own, so that a filler row and a real row can never drift apart in
     * height.
     *
     * @param context     Context.
     * @param parent      RemoteViews the rows are added to.
     * @param containerId Resource ID of the target tram container.
     * @param count       Number of empty rows to add. Zero or fewer adds nothing.
     */
    private fun addFillerRows(context: Context, parent: RemoteViews, containerId: Int, count: Int) {
        repeat(maxOf(count, 0)) {
            parent.addView(containerId, RemoteViews(context.packageName, R.layout.item_tram))
        }
    }

    /**
     * Reads the widget's currently reported height and converts it to a row count.
     *
     * @param context          Context.
     * @param appWidgetManager Manager the reported size is read from.
     * @param appWidgetId      ID of the widget instance being measured.
     * @return Total tram row count across both directions.
     */
    private fun maxTotalTramsFor(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int): Int {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val height = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val sizes: ArrayList<SizeF>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
            } else {
                @Suppress("DEPRECATION")
                options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES)
            }
            sizes?.maxByOrNull { it.height }?.height?.toInt()
                ?: options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        } else {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        }

        return calculateMaxTotalTrams(context, height)
    }

    /**
     * Estimates how many tram rows to render at the widget's current height.
     *
     * This is deliberately an estimate. The height the launcher reports is not the height the widget gets to draw in, and the
     * shortfall is launcher-specific, so an exact answer is not available to us. [R.dimen.widget_reported_height_slack] biases the
     * result low, and the rows themselves are weighted so that being out by one changes their height rather than clipping the last
     * of them.
     *
     * Always even, so that the two directions can be given the same number of rows.
     *
     * @param context        Context.
     * @param widgetHeightDp Reported widget height in dp, from [AppWidgetManager.getAppWidgetOptions].
     * @return Total tram row count across both directions (at least 1).
     */
    private fun calculateMaxTotalTrams(context: Context, widgetHeightDp: Int): Int {
        val resources = context.resources
        val density = resources.displayMetrics.density

        val tramItemHeightDp = (resources.getDimension(R.dimen.widget_tram_item_height) / density).toInt()

        /*
         * The chrome is the header plus both direction labels. Those views are pinned to these same dimens in the layout, so this
         * sum stays correct at any system font size.
         */
        val headerHeightDp = (resources.getDimension(R.dimen.widget_header_height) / density).toInt()
        val labelHeightDp = (resources.getDimension(R.dimen.widget_direction_label_height) / density).toInt()
        val chromeHeightDp = headerHeightDp + (labelHeightDp * 2)
        val slackDp = (resources.getDimension(R.dimen.widget_reported_height_slack) / density).toInt()

        if (tramItemHeightDp == 0) {
            return 1
        }

        val availableHeight = widgetHeightDp - chromeHeightDp - slackDp
        val rows = availableHeight / tramItemHeightDp

        /* An odd row is dropped rather than given to one direction, which would look lopsided. */
        return (rows - (rows % 2)).coerceAtLeast(MIN_TOTAL_TRAMS)
    }

    /**
     * Divides the available row capacity between the two directions.
     *
     * Both directions get the same cap, half the total each, so neither is ever shown more rows than the other. Spare capacity is
     * deliberately not lent to the other direction, since a five-and-two split reads as a bug rather than as a quiet tram stop. The
     * two returned counts therefore always sum to at most [maxTotalTrams].
     *
     * @param maxTotalTrams Row capacity across both directions.
     * @param inboundSize   Number of rows the inbound direction has to show.
     * @param outboundSize  Number of rows the outbound direction has to show.
     * @return Number of inbound rows to show, paired with the number of outbound rows.
     */
    private fun splitTramBudget(maxTotalTrams: Int, inboundSize: Int, outboundSize: Int): Pair<Int, Int> {
        val perDirection = maxTotalTrams / 2

        return minOf(inboundSize, perDirection) to minOf(outboundSize, perDirection)
    }

    /**
     * Resolves the direction labels for a stop.
     *
     * Red Line stops use Eastbound/Westbound. Green Line stops use Northbound/Southbound.
     *
     * @param context  Context.
     * @param stopName Stop whose line decides which pair of labels is used.
     * @return Inbound label paired with the outbound label.
     */
    private fun directionLabels(context: Context, stopName: String): Pair<String, String> {
        val redLineStops = context.resources.getStringArray(R.array.array_stops_redline)

        return if (stopName in redLineStops) {
            context.getString(R.string.eastbound) to context.getString(R.string.westbound)
        } else {
            context.getString(R.string.northbound) to context.getString(R.string.southbound)
        }
    }

    /**
     * Builds the outbound direction label row that separates the two sets of tram rows.
     *
     * It lives inside `trams_container` rather than in the static layout, so that the weighted tram rows above and below it divide
     * one container between them. Its own layout carries a fixed height and no weight, so it is excluded from that division.
     *
     * @param context Context.
     * @param label   Direction name to show in the row.
     * @return The label row, ready to be added to `trams_container`.
     */
    private fun directionLabelView(context: Context, label: String): RemoteViews {
        return RemoteViews(context.packageName, R.layout.item_direction_label).apply {
            setTextViewText(R.id.textview_direction, label)
        }
    }

    /**
     * Builds a [PendingIntent] that broadcasts the given action back to this widget.
     *
     * The intent [android.content.Intent.data] URI is set to a unique value per action and widget ID to prevent the system from
     * collapsing distinct intents into one.
     *
     * @param context     Context the broadcast is sent from.
     * @param appWidgetId ID of the widget instance the action applies to.
     * @param action      One of the ACTION_* constants defined in the companion object.
     * @return A broadcast [PendingIntent] unique to this action and widget ID.
     */
    private fun getPendingIntent(context: Context, appWidgetId: Int, action: String): PendingIntent {
        val intent = Intent(context, StopForecastWidget::class.java).apply {
            this.action = action
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = "$action://$appWidgetId".toUri()
        }
        return PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Schedules an [AlarmManager] alarm 15 seconds from now.
     *
     * When it fires, [ACTION_TIMEOUT] returns the widget to the holding screen. Tram times go stale quickly, so what is on screen
     * reverts to an invitation to reload rather than sitting there looking current. Called after a forecast loads and after an
     * error, since an error goes stale just as fast. [cancelTimeout] drops it when a new load starts.
     *
     * @param context     Context.
     * @param appWidgetId ID of the widget instance the alarm is set for.
     */
    private fun setTimeout(context: Context, appWidgetId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = getPendingIntent(context, appWidgetId, ACTION_TIMEOUT)
        alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 15000, intent)
    }

    /**
     * Cancels any pending [ACTION_TIMEOUT] alarm for this widget instance.
     *
     * @param context     Context.
     * @param appWidgetId ID of the widget instance whose alarm is cancelled.
     */
    private fun cancelTimeout(context: Context, appWidgetId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = getPendingIntent(context, appWidgetId, ACTION_TIMEOUT)
        alarmManager.cancel(intent)
    }

    /**
     * Hides the forecast content and shows the holding screen.
     *
     * Uses [AppWidgetManager.partiallyUpdateAppWidget] so that click handlers set by [updateAppWidget] are preserved - a full
     * [AppWidgetManager.updateAppWidget] call would replace the entire RemoteViews and wipe all [PendingIntent]s, leaving the
     * widget unresponsive to touches.
     *
     * @param context     Context.
     * @param appWidgetId ID of the widget instance being updated.
     * @param message     Optional text to display on the holding screen. If null, the existing string resource default
     *                    ("Tap to load times") is shown.
     */
    private fun showHoldingScreen(context: Context, appWidgetId: Int, message: CharSequence? = null) {
        val views = RemoteViews(context.packageName, R.layout.stop_forecast_widget)
        views.setViewVisibility(R.id.forecast_content, View.GONE)
        views.setViewVisibility(R.id.holding_screen, View.VISIBLE)
        message?.let { views.setTextViewText(R.id.holding_screen_text, it) }
        AppWidgetManager.getInstance(context).partiallyUpdateAppWidget(appWidgetId, views)
    }

    companion object {
        private const val ACTION_OPEN_APP = "org.thecosmicfrog.luasataglance.widget.ACTION_OPEN_APP"
        private const val ACTION_REFRESH = "org.thecosmicfrog.luasataglance.widget.ACTION_REFRESH"
        private const val ACTION_PREV_STOP = "org.thecosmicfrog.luasataglance.widget.ACTION_PREV_STOP"
        private const val ACTION_NEXT_STOP = "org.thecosmicfrog.luasataglance.widget.ACTION_NEXT_STOP"
        private const val ACTION_TIMEOUT = "org.thecosmicfrog.luasataglance.widget.ACTION_TIMEOUT"

        /* Under the time a broadcast held open by goAsync is allowed to run for. */
        private const val FETCH_TIMEOUT_MS = 8000L

        /*
         * Delay period before fetching a stop forecast. Used to avoid spamming the Luas API as a user clicks the next/previous stop
         * arrows. Commonly known as a "debounce". Long enough to step past a stop without fetching for it, short enough not to feel
         * like an app lag.
         */
        private const val STOP_CHANGE_DEBOUNCE_MS = 750L

        /*
         * The fetch each widget is waiting on, so that a later tap can cancel an earlier one. Static because every broadcast gets
         * a new receiver instance, which cannot see the job the previous tap started.
         */
        private val pendingFetches = ConcurrentHashMap<Int, Job>()

        /*
         * Two rows per direction. The rows are weighted, so asking for these at a height that does not really fit them makes them
         * shorter rather than dropping any. A widget too short to read them at all is ruled out by minResizeHeight.
         */
        private const val MIN_TOTAL_TRAMS = 4
    }
}
