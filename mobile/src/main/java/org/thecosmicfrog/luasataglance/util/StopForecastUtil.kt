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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.activity.NotifyTimeActivity
import org.thecosmicfrog.luasataglance.api.ApiTimes
import org.thecosmicfrog.luasataglance.model.StopForecast
import org.thecosmicfrog.luasataglance.model.StopForecastShimmerAdapter

object StopForecastUtil {

    private val logTag = StopForecastUtil::class.java.simpleName

    /**
     * Clear the stop forecast by inserting blank StopForecastInfo objects into the inbound and outbound RecyclerViews.
     * @param recyclerViewInbound Inbound RecyclerView.
     * @param recyclerViewOutbound Outbound RecyclerView.
     */
    fun clearStopForecast(recyclerViewInbound: RecyclerView?, recyclerViewOutbound: RecyclerView?) {
        val stopForecastShimmerAdapter = StopForecastShimmerAdapter()
        recyclerViewInbound?.adapter = stopForecastShimmerAdapter
        recyclerViewOutbound?.adapter = stopForecastShimmerAdapter
    }

    /**
     * Set the titles for the inbound and outbound directions.
     * On the Red Line, the inbound direction is "Eastbound" and the outbound direction is "Westbound".
     * On the Green Line, the inbound direction is "Northbound" and the outbound direction is "Southbound".
     *
     * Inbound and outbound are legacy Luas terms and just cause confusion for users.
     * @param context Context.
     * @param line Line to set titles for.
     * @param viewBinding ViewBinding.
     */
    fun setStopForecastDirectionTitles(context: Context, line: String?, viewBinding: LineFragmentViewBindingAdapter?) {
        val (inboundText, outboundText) = when (line) {
            Constant.RED_LINE -> context.getString(R.string.eastbound) to context.getString(R.string.westbound)
            Constant.GREEN_LINE -> context.getString(R.string.northbound) to context.getString(R.string.southbound)
            else -> {
                /* If for some reason the line doesn't make sense, set the titles to empty strings. */
                Log.wtf(logTag, "Invalid line specified.")
                "" to ""
            }
        }

        viewBinding?.textViewStopForecastInbound?.text = inboundText
        viewBinding?.textViewStopForecastOutbound?.text = outboundText
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
            showSnackbar((context as Activity), context.getString(R.string.cannot_schedule_notification))

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
        val bottomNavigationBar = activity.findViewById<BottomNavigationView>(R.id.bottomnavigationview)

        Snackbar.make(
            activity.findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        ).setAnchorView(bottomNavigationBar).show()
    }
}

