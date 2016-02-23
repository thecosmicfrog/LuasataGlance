/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2016 Aaron Hastings
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

package org.thecosmicfrog.luasataglance.util;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.activity.NotifyTimeActivity;
import org.thecosmicfrog.luasataglance.api.ApiTimes;
import org.thecosmicfrog.luasataglance.object.NotifyTimesMap;
import org.thecosmicfrog.luasataglance.object.StopForecast;
import org.thecosmicfrog.luasataglance.object.Tram;
import org.thecosmicfrog.luasataglance.view.TutorialCardView;

import java.util.Locale;

public final class StopForecastUtil {

    private static final String LOG_TAG = StopForecastUtil.class.getSimpleName();
    private static final String RED_LINE = "red_line";
    private static final String GREEN_LINE = "green_line";
    private static final String TUTORIAL_SELECT_STOP = "select_stop";
    private static final String TUTORIAL_NOTIFICATIONS = "notifications";
    private static final String TUTORIAL_FAVOURITES = "favourites";
    private static final String STOP_FORECAST = "stop_forecast";

    /**
     * Show dialog for choosing notification times.
     * @param rootView          Root View.
     * @param stopName          Stop name to notify for.
     * @param textViewStopTimes Array of TextViews for times in a stop forecast.
     * @param index             Index representing which specific tram to notify for.
     */
    public static void showNotifyTimeDialog(View rootView, String stopName,
                                            TextView[] textViewStopTimes, int index) {
        String localeDefault = Locale.getDefault().toString();
        String notifyStopTimeStr = textViewStopTimes[index].getText().toString();
        NotifyTimesMap mapNotifyTimes = new NotifyTimesMap(localeDefault, STOP_FORECAST);

        if (notifyStopTimeStr.equals(""))
            return;

        if (notifyStopTimeStr.matches(
                rootView.getResources().getString(R.string.due) + "|" + "1 .*|2 .*")) {
            Toast.makeText(
                    rootView.getContext(),
                    rootView.getContext().getResources().getString(
                            R.string.cannot_schedule_notification
                    ),
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        /*
         * When the user opens the notification dialog as part of the tutorial, scroll back up to
         * the top so that the next tutorial is definitely visible. This should only ever run once.
         */
        if (!Preferences.hasRunOnce(rootView.getContext(), TUTORIAL_NOTIFICATIONS)) {
            ScrollView scrollView = (ScrollView) rootView.findViewById(R.id.redline_scrollview);
            scrollView.setScrollY(0);
        }

        Preferences.saveHasRunOnce(rootView.getContext(), TUTORIAL_NOTIFICATIONS, true);

        /* We're done with the notifications tutorial. Hide it. */
        displayTutorial(rootView, TUTORIAL_NOTIFICATIONS, false);

        /* Then, display the final tutorial. */
        displayTutorial(rootView, TUTORIAL_FAVOURITES, true);

        Preferences.saveNotifyStopName(
                rootView.getContext(),
                stopName
        );

        Preferences.saveNotifyStopTimeExpected(
                rootView.getContext(),
                mapNotifyTimes.get(notifyStopTimeStr)
        );

        rootView.getContext().startActivity(
                new Intent(
                        rootView.getContext(),
                        NotifyTimeActivity.class
                )
        );
    }

    /**
     * Determine if this is the first time the app has been launched and, if so, display a brief
     * tutorial on how to use a particular feature of the app.
     * @param rootView      Root View.
     * @param tutorial      Tutorial to display.
     * @param shouldDisplay Whether or not tutorial should display.
     */
    public static void displayTutorial(View rootView, String tutorial, boolean shouldDisplay) {
        switch (tutorial) {
            case TUTORIAL_SELECT_STOP:
                TutorialCardView tutorialCardViewSelectStop =
                        (TutorialCardView) rootView.findViewById(
                                R.id.tutorialcardview_select_stop
                        );

                tutorialCardViewSelectStop.setTutorial(
                        rootView.getContext().getResources().getText(
                                R.string.select_stop_tutorial
                        )
                );

                if (shouldDisplay) {
                    if (!Preferences.hasRunOnce(rootView.getContext(), tutorial)) {
                        Log.i(LOG_TAG, "First time launching. Displaying select stop tutorial.");

                        tutorialCardViewSelectStop.setVisibility(View.VISIBLE);

                        Preferences.saveHasRunOnce(rootView.getContext(), tutorial, true);
                    }
                } else {
                    tutorialCardViewSelectStop.setVisibility(View.GONE);
                }

                break;

            case TUTORIAL_NOTIFICATIONS:
                TutorialCardView tutorialCardViewNotifications =
                        (TutorialCardView) rootView.findViewById(
                                R.id.tutorialcardview_notifications
                        );

                tutorialCardViewNotifications.setTutorial(
                        rootView.getContext().getResources().getText(
                                R.string.notifications_tutorial
                        )
                );

                if (shouldDisplay) {
                    if (!Preferences.hasRunOnce(rootView.getContext(), tutorial)) {
                        Log.i(LOG_TAG, "First time launching. Displaying notifications tutorial.");

                        tutorialCardViewNotifications.setVisibility(View.VISIBLE);
                    }
                } else {
                    tutorialCardViewNotifications.setVisibility(View.GONE);
                }

                break;

            case TUTORIAL_FAVOURITES:
                TutorialCardView tutorialCardViewFavourites =
                        (TutorialCardView) rootView.findViewById(
                                R.id.tutorialcardview_favourites
                        );

                tutorialCardViewFavourites.setTutorial(
                        rootView.getContext().getResources().getText(
                                R.string.favourites_tutorial
                        )
                );

                if (shouldDisplay) {
                    if (!Preferences.hasRunOnce(rootView.getContext(), tutorial)) {
                        Log.i(LOG_TAG, "First time launching. Displaying favourites tutorial.");

                        tutorialCardViewFavourites.setVisibility(View.VISIBLE);

                        Preferences.saveHasRunOnce(rootView.getContext(), tutorial, true);
                    }
                } else {
                    tutorialCardViewFavourites.setVisibility(View.GONE);
                }

                break;

            default:
                /* If for some reason the specified tutorial doesn't make sense. */
                Log.wtf(LOG_TAG, "Invalid tutorial specified.");
        }
    }

    /**
     * Create a usable stop forecast with the data returned from the server.
     * @param apiTimes ApiTimes object created by Retrofit, containing raw stop forecast data.
     * @return Usable stop forecast.
     */
    public static StopForecast createStopForecast(ApiTimes apiTimes) {
        StopForecast stopForecast = new StopForecast();

        if (apiTimes.getTrams() != null) {
            for (Tram tram : apiTimes.getTrams()) {
                switch (tram.getDirection()) {
                    case "Inbound":
                        stopForecast.addInboundTram(tram);

                        break;

                    case "Outbound":
                        stopForecast.addOutboundTram(tram);

                        break;

                    default:
                        /* If for some reason the direction doesn't make sense. */
                        Log.wtf(LOG_TAG, "Invalid direction: " + tram.getDirection());
                }
            }
        }

        stopForecast.setMessage(apiTimes.getMessage());

        return stopForecast;
    }
}

