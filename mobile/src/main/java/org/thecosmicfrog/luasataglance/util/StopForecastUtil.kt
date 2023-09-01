/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2023 Aaron Hastings
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
import android.util.Log
import android.view.View
import com.google.android.material.snackbar.Snackbar
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.api.ApiTimes
import org.thecosmicfrog.luasataglance.model.StopForecast
import org.thecosmicfrog.luasataglance.view.TutorialCardView

object StopForecastUtil {

    private val logTag = StopForecastUtil::class.java.simpleName

    /**
     * Determine if this is the first time the app has been launched and, if so, display a brief
     * tutorial on how to use a particular feature of the app.
     * @param rootView      Root View.
     * @param line          Currently-selected line.
     * @param tutorial      Tutorial to display.
     * @param shouldDisplay Whether or not tutorial should display.
     */
    @JvmStatic
    fun displayTutorial(rootView: View, line: String, tutorial: String?, shouldDisplay: Boolean) {
        /* Only display tutorials on the Red Line tab. */
        if (line == Constant.RED_LINE) {
            when (tutorial) {
                Constant.TUTORIAL_SELECT_STOP -> {
                    val tutorialCardViewSelectStop: TutorialCardView = rootView.findViewById(
                        R.id.tutorialcardview_select_stop
                    )

                    tutorialCardViewSelectStop.setTutorial(
                        rootView.context.resources.getText(
                            R.string.select_stop_tutorial
                        )
                    )

                    if (shouldDisplay) {
                        if (!Preferences.hasRunOnce(rootView.context, tutorial)) {
                            Log.i(
                                logTag,
                                "First time launching. Displaying select stop tutorial."
                            )

                            tutorialCardViewSelectStop.visibility = View.VISIBLE

                            Preferences.saveHasRunOnce(rootView.context, tutorial, true)
                        }
                    } else {
                        tutorialCardViewSelectStop.visibility = View.GONE
                    }
                }

                Constant.TUTORIAL_NOTIFICATIONS -> {
                    val tutorialCardViewNotifications: TutorialCardView = rootView.findViewById(
                        R.id.tutorialcardview_notifications
                    )

                    tutorialCardViewNotifications.setTutorial(
                        rootView.context.resources.getText(
                            R.string.notifications_tutorial
                        )
                    )

                    if (shouldDisplay) {
                        if (!Preferences.hasRunOnce(rootView.context, tutorial)) {
                            Log.i(
                                logTag,
                                "First time launching. Displaying notifications tutorial."
                            )

                            tutorialCardViewNotifications.visibility = View.VISIBLE
                        }
                    } else {
                        tutorialCardViewNotifications.visibility = View.GONE
                    }
                }
                Constant.TUTORIAL_FAVOURITES -> {
                    val tutorialCardViewFavourites: TutorialCardView = rootView.findViewById(
                        R.id.tutorialcardview_favourites
                    )

                    tutorialCardViewFavourites.setTutorial(
                        rootView.context.resources.getText(
                            R.string.favourites_tutorial
                        )
                    )

                    if (shouldDisplay) {
                        if (!Preferences.hasRunOnce(rootView.context, tutorial)) {
                            Log.i(
                                logTag,
                                "First time launching. Displaying favourites tutorial."
                            )

                            tutorialCardViewFavourites.visibility = View.VISIBLE
                        }
                    } else {
                        tutorialCardViewFavourites.visibility = View.GONE
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
     * Show Snackbar.
     * @param activity Activity on which to display Snackbar.
     * @param message Message to display on Snackbar.
     */
    @JvmStatic
    fun showSnackbar(activity: Activity, message: String?) {
        Snackbar.make(
            activity.findViewById(android.R.id.content),
            message!!,
            Snackbar.LENGTH_LONG
        ).show()
    }
}

