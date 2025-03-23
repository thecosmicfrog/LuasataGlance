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
package org.thecosmicfrog.luasataglance.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.activity.NotifyTimeActivity
import org.thecosmicfrog.luasataglance.api.ApiTimes
import org.thecosmicfrog.luasataglance.model.StopForecast
import org.thecosmicfrog.luasataglance.model.StopForecastAdapter
import org.thecosmicfrog.luasataglance.model.StopForecastInfo
import org.thecosmicfrog.luasataglance.view.StatusCardView
import java.util.*

object StopForecastUtil {

    private val logTag = StopForecastUtil::class.java.simpleName

    /**
     * Clear the stop forecast by inserting blank StopForecastInfo objects into the inbound and outbound RecyclerViews.
     * @param recyclerViewInbound Inbound RecyclerView.
     * @param recyclerViewOutbound Outbound RecyclerView.
     * @param statusCardView StatusCardView.
     */
    fun clearStopForecast(
        statusCardView: StatusCardView?,
        recyclerViewInbound: RecyclerView?,
        recyclerViewOutbound: RecyclerView?,
    ) {
        val emptyList = listOf(StopForecastInfo("", "", ""))
        val emptyAdapter = StopForecastAdapter(emptyList)

        recyclerViewInbound?.adapter = emptyAdapter
        recyclerViewOutbound?.adapter = emptyAdapter
        statusCardView?.setStatus("")
    }

    /**
     * Determine if this is the first time the app has been launched and, if so, display a brief
     * tutorial on how to use a particular feature of the app.
     * @param rootView      Root View.
     * @param line          Currently-selected line.
     * @param tutorial      Tutorial to display.
     * @param shouldDisplay Whether or not tutorial should display.
     */
    @JvmStatic
    fun displayTutorial(viewBinding: LineFragmentViewBindingAdapter, line: String, tutorial: String?, shouldDisplay: Boolean) {
        /* Only display tutorials on the Red Line tab. */
        if (line == Constant.RED_LINE) {
            when (tutorial) {
                Constant.TUTORIAL_SELECT_STOP -> {
                    val tutorialCardViewSelectStop = viewBinding.tutorialcardviewSelectStop

                    tutorialCardViewSelectStop?.setTutorial(
                        tutorialCardViewSelectStop.context.resources.getText(
                            R.string.select_stop_tutorial
                        )
                    )

                    if (shouldDisplay) {
                        if (!Preferences.hasRunOnce(tutorialCardViewSelectStop?.context, tutorial)) {
                            Log.i(
                                logTag,
                                "First time launching. Displaying select stop tutorial."
                            )

                            tutorialCardViewSelectStop?.visibility = View.VISIBLE

                            Preferences.saveHasRunOnce(tutorialCardViewSelectStop?.context, tutorial, true)
                        }
                    } else {
                        tutorialCardViewSelectStop?.visibility = View.GONE
                    }
                }
                else ->
                    /* If for some reason the specified tutorial doesn't make sense. */
                    Log.wtf(logTag, "Invalid tutorial specified.")
            }
        }
    }

    /**
     * Create a usable stop forecast with the data returned from the server.
     * @param apiTimes ApiTimes model created by Retrofit, containing raw stop forecast data.
     * @return Usable stop forecast.
     */
    @JvmStatic
    fun createStopForecast(apiTimes: ApiTimes): StopForecast {
        val stopForecast = StopForecast()

        stopForecast.message = apiTimes.message
        stopForecast.stopForecastStatusDirectionInbound.message =
            apiTimes.stopForecastStatus?.stopForecastStatusDirectionInbound?.message
        stopForecast.stopForecastStatusDirectionInbound.forecastsEnabled =
            apiTimes.stopForecastStatus?.stopForecastStatusDirectionInbound?.forecastsEnabled
        stopForecast.stopForecastStatusDirectionInbound.operatingNormally =
            apiTimes.stopForecastStatus?.stopForecastStatusDirectionInbound?.operatingNormally
        stopForecast.stopForecastStatusDirectionOutbound.message =
            apiTimes.stopForecastStatus?.stopForecastStatusDirectionOutbound?.message
        stopForecast.stopForecastStatusDirectionOutbound.forecastsEnabled =
            apiTimes.stopForecastStatus?.stopForecastStatusDirectionOutbound?.forecastsEnabled
        stopForecast.stopForecastStatusDirectionOutbound.operatingNormally =
            apiTimes.stopForecastStatus?.stopForecastStatusDirectionOutbound?.operatingNormally

        apiTimes.trams?.forEach {
            when (it?.direction) {
                "Inbound" -> stopForecast.addInboundTram(it)
                "Outbound" -> stopForecast.addOutboundTram(it)
                else ->
                    /* If for some reason the direction doesn't make sense. */
                    Log.wtf(logTag, "Invalid direction: " + it?.direction)
            }
        }

        return stopForecast
    }

    /**
     * Show dialog for choosing notification times.
     * @param context Context for accessing resources and starting activity
     * @param scrollView ScrollView to adjust scroll position
     * @param viewBinding View binding adapter for tutorial display
     * @param line Current line (RED_LINE or GREEN_LINE)
     * @param stopName Stop name to notify for
     * @param notifyStopTimeStr Time string to check for notification
     */
    @JvmStatic
    fun showNotifyTimeDialog(
        context: Context,
        stopName: String,
        notifyStopTimeStr: String
    ) {
        if (notifyStopTimeStr.isEmpty()) return

        /* Don't permit the user to schedule a notification for a tram that is due now, or in 1 or 2 minutes. */
        if (notifyStopTimeStr.matches(Regex("${context.getString(R.string.due)}|1|2"))) {
            Toast.makeText(
                context,
                context.getString(R.string.cannot_schedule_notification),
                Toast.LENGTH_LONG
            ).show()

            return
        }

        Preferences.saveNotifyStopName(context, stopName)
        Preferences.saveNotifyStopTimeExpected(context, Integer.parseInt(notifyStopTimeStr))

        context.startActivity(
            Intent(context, NotifyTimeActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /**
     * Show Snackbar.
     * @param activity Activity on which to display Snackbar.
     * @param message Message to display on Snackbar.
     */
    @JvmStatic
    fun showSnackbar(activity: Activity, message: String) {
        Snackbar.make(
            activity.findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        ).setTextColor(Color.WHITE).show()
    }

    /**
     * Extension function to set Snackbar text color.
     * @param color Color to set Snackbar text to.
     * @return Snackbar.
     */
    fun Snackbar.setTextColor(color: Int): Snackbar {
        val textViewSnackBar =
            view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textViewSnackBar.setTextColor(color)

        return this
    }
}

