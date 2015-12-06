/**
 * @author Aaron Hastings
 *
 * Copyright 2015 Aaron Hastings
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

import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.activity.NotifyTimeDialog;
import org.thecosmicfrog.luasataglance.api.ApiTimes;
import org.thecosmicfrog.luasataglance.object.NotifyTimesMap;
import org.thecosmicfrog.luasataglance.object.StopForecast;
import org.thecosmicfrog.luasataglance.object.Tram;
import org.thecosmicfrog.luasataglance.view.SpinnerCardView;
import org.thecosmicfrog.luasataglance.view.StopForecastCardView;
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
     * Initialise OnClickListeners for a stop forecast.
     * @param rootView Root View.
     */
    public static void initStopForecastOnClickListeners(final View rootView) {
        final SpinnerCardView redLineSpinnerCardView =
                (SpinnerCardView) rootView.findViewById(
                        R.id.red_line_spinner_card_view
                );
        final SpinnerCardView greenLineSpinnerCardView =
                (SpinnerCardView) rootView.findViewById(
                        R.id.green_line_spinner_card_view
                );

        final StopForecastCardView redLineInboundStopForecastCardView =
                (StopForecastCardView) rootView.findViewById(
                        R.id.red_line_inbound_stopforecastcardview
                );
        final StopForecastCardView redLineOutboundStopForecastCardView =
                (StopForecastCardView) rootView.findViewById(
                        R.id.red_line_outbound_stopforecastcardview
                );
        final StopForecastCardView greenLineInboundStopForecastCardView =
                (StopForecastCardView) rootView.findViewById(
                        R.id.green_line_inbound_stopforecastcardview
                );
        final StopForecastCardView greenLineOutboundStopForecastCardView =
                (StopForecastCardView) rootView.findViewById(
                        R.id.green_line_outbound_stopforecastcardview
                );

        TableRow[] redLineTableRowInboundStops =
                redLineInboundStopForecastCardView.getTableRowStops();
        TableRow[] redLineTableRowOutboundStops =
                redLineOutboundStopForecastCardView.getTableRowStops();
        TableRow[] greenLineTableRowInboundStops =
                greenLineInboundStopForecastCardView.getTableRowStops();
        TableRow[] greenLineTableRowOutboundStops =
                greenLineOutboundStopForecastCardView.getTableRowStops();

        for (int i = 0; i < 4; i++) {
            final int index = i;

            redLineTableRowInboundStops[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showNotifyTimeDialog(
                            rootView,
                            redLineSpinnerCardView.getSpinnerStops().getSelectedItem().toString(),
                            redLineInboundStopForecastCardView.getTextViewStopTimes(),
                            index
                    );
                }
            });

            redLineTableRowOutboundStops[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showNotifyTimeDialog(
                            rootView,
                            redLineSpinnerCardView.getSpinnerStops().getSelectedItem().toString(),
                            redLineOutboundStopForecastCardView.getTextViewStopTimes(),
                            index
                    );
                }
            });

            greenLineTableRowInboundStops[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showNotifyTimeDialog(
                            rootView,
                            greenLineSpinnerCardView.getSpinnerStops().getSelectedItem().toString(),
                            greenLineInboundStopForecastCardView.getTextViewStopTimes(),
                            index
                    );
                }
            });

            greenLineTableRowOutboundStops[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showNotifyTimeDialog(
                            rootView,
                            greenLineSpinnerCardView.getSpinnerStops().getSelectedItem().toString(),
                            greenLineOutboundStopForecastCardView.getTextViewStopTimes(),
                            index
                    );
                }
            });
        }
    }

    /**
     * Show dialog for choosing notification times.
     * @param rootView Root View.
     * @param stopName Stop name to notify for.
     * @param textViewStopTimes Array of TextViews for times in a stop forecast.
     * @param index Index representing which specific tram to notify for.
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
        if (!Preferences.loadHasRunOnce(rootView.getContext(), TUTORIAL_NOTIFICATIONS)) {
            ScrollView scrollView = (ScrollView) rootView.findViewById(R.id.red_line_scrollview);
            scrollView.setScrollY(0);
        }

        Preferences.saveHasRunOnce(rootView.getContext(), TUTORIAL_NOTIFICATIONS, true);

        // We're done with the notifications tutorial. Hide it.
        displayTutorial(rootView, TUTORIAL_NOTIFICATIONS, false);

        // Then, display the final tutorial.
        displayTutorial(rootView, TUTORIAL_FAVOURITES, true);

        Preferences.saveNotifyStopName(
                rootView.getContext(),
                stopName
        );

        Preferences.saveNotifyStopTimeExpected(
                rootView.getContext(),
                mapNotifyTimes.get(notifyStopTimeStr)
        );

        new NotifyTimeDialog(rootView.getContext()).show();
    }

    /**
     * Determine if this is the first time the app has been launched and, if so, display a brief
     * tutorial on how to use a particular feature of the app.
     * @param rootView Root View.
     * @param tutorial Tutorial to display.
     * @param shouldDisplay Whether or not tutorial should display.
     */
    public static void displayTutorial(View rootView, String tutorial, boolean shouldDisplay) {
        SwipeRefreshLayout redLineSwipeRefreshLayout =
                (SwipeRefreshLayout) rootView.findViewById(R.id.red_line_swiperefreshlayout);
        SwipeRefreshLayout greenLineSwipeRefreshLayout =
                (SwipeRefreshLayout) rootView.findViewById(R.id.green_line_swiperefreshlayout);

        switch(tutorial) {
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
                    if (!Preferences.loadHasRunOnce(rootView.getContext(), tutorial)) {
                        Log.i(LOG_TAG, "First time launching. Displaying select stop tutorial.");

                        tutorialCardViewSelectStop.setVisibility(View.VISIBLE);

                        Preferences.saveHasRunOnce(rootView.getContext(), tutorial, true);
                    }
                } else {
                    tutorialCardViewSelectStop.setVisibility(View.GONE);

                    redLineSwipeRefreshLayout.setEnabled(true);
                    greenLineSwipeRefreshLayout.setEnabled(true);
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
                    if (!Preferences.loadHasRunOnce(rootView.getContext(), tutorial)) {
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
                    if (!Preferences.loadHasRunOnce(rootView.getContext(), tutorial)) {
                        Log.i(LOG_TAG, "First time launching. Displaying favourites tutorial.");

                        tutorialCardViewFavourites.setVisibility(View.VISIBLE);

                        Preferences.saveHasRunOnce(rootView.getContext(), tutorial, true);
                    }
                } else {
                    tutorialCardViewFavourites.setVisibility(View.GONE);
                }

                break;

            default:
                // If for some reason the specified tutorial doesn't make sense.
                Log.wtf(LOG_TAG, "Invalid tutorial specified.");
        }
    }

    /**
     * Clear the inbound and outbound stop forecast displayed in the specified tab.
     * @param rootView Root View.
     * @param line Tab (Red or Green Line) to clear stop forecast for.
     */
    public static void clearLineStopForecast(View rootView, String line) {
        switch (line) {
            case RED_LINE:
                StopForecastCardView redLineInboundStopForecastCardView =
                        (StopForecastCardView) rootView.findViewById(
                                R.id.red_line_inbound_stopforecastcardview
                        );
                StopForecastCardView redLineOutboundStopForecastCardView =
                        (StopForecastCardView) rootView.findViewById(
                                R.id.red_line_outbound_stopforecastcardview
                        );

                redLineInboundStopForecastCardView.clearStopForecast();
                redLineOutboundStopForecastCardView.clearStopForecast();

                break;

            case GREEN_LINE:
                StopForecastCardView greenLineInboundStopForecastCardView =
                        (StopForecastCardView) rootView.findViewById(
                                R.id.green_line_inbound_stopforecastcardview
                        );
                StopForecastCardView greenLineOutboundStopForecastCardView =
                        (StopForecastCardView) rootView.findViewById(
                                R.id.green_line_outbound_stopforecastcardview
                        );

                greenLineInboundStopForecastCardView.clearStopForecast();
                greenLineOutboundStopForecastCardView.clearStopForecast();

                break;

            default:
                // If for some reason the line doesn't make sense.
                Log.wtf(LOG_TAG, "Invalid line specified.");
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
                        // If for some reason the direction doesn't make sense.
                        Log.wtf(LOG_TAG, "Invalid direction: " + tram.getDirection());
                }
            }
        }

        stopForecast.setMessage(apiTimes.getMessage());

        return stopForecast;
    }

    /**
     * Make progress bar animate or not.
     * @param fragment Fragment in which loading bar is present.
     * @param rootView Root View.
     * @param line Name of tab in which progress bar should animate.
     * @param loading Whether or not progress bar should animate.
     */
    public static void setIsLoading(Fragment fragment, View rootView, final String line,
                                    final boolean loading) {
        final ProgressBar progressBarRedLine =
                (ProgressBar) rootView.findViewById(R.id.red_line_progressbar);
        final ProgressBar progressBarGreenLine =
                (ProgressBar) rootView.findViewById(R.id.green_line_progressbar);

        /*
         * Only run if Fragment is attached to Activity. Without this check, the app is liable
         * to crash when the screen is rotated many times in a given period of time.
         */
        if (fragment.isAdded()) {
            fragment.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (line) {
                        case RED_LINE:
                            if (loading)
                                progressBarRedLine.setVisibility(View.VISIBLE);
                            else
                                progressBarRedLine.setVisibility(View.INVISIBLE);

                            break;

                        case GREEN_LINE:
                            if (loading)
                                progressBarGreenLine.setVisibility(View.VISIBLE);
                            else
                                progressBarGreenLine.setVisibility(View.INVISIBLE);

                            break;

                        default:
                            // If for some reason the line doesn't make sense.
                            Log.wtf(LOG_TAG, "Invalid line specified.");
                    }
                }
            });
        }
    }
}
