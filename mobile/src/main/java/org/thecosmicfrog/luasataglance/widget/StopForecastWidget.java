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

package org.thecosmicfrog.luasataglance.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;
import android.widget.RemoteViews;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.api.ApiMethods;
import org.thecosmicfrog.luasataglance.api.ApiTimes;
import org.thecosmicfrog.luasataglance.object.EnglishGaeilgeMap;
import org.thecosmicfrog.luasataglance.object.StopForecast;
import org.thecosmicfrog.luasataglance.object.StopNameIdMap;
import org.thecosmicfrog.luasataglance.object.Tram;
import org.thecosmicfrog.luasataglance.util.Auth;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in
 * {@link StopForecastWidgetConfigureActivity StopForecastWidgetConfigureActivity}
 */
public class StopForecastWidget extends AppWidgetProvider {

    private static final String LOG_TAG = StopForecastWidget.class.getSimpleName();

    private static final String API_FORMAT = "json";
    private static final String API_URL = "http://www.dublinked.ie/cgi-bin/rtpi";
    private static final String GAEILGE = "ga";

    private static final String PREFS_NAME = "org.thecosmicfrog.luasataglane.StopForecastWidget";
    private static final String FILE_WIDGET_SELECTED_STOPS = "widget_selected_stops";
    private static final int LOAD_LIMIT_MILLIS = 4000;
    private static final int STOP_FORECAST_TIMEOUT_MILLIS = 20000;

    private static final int PROGRESSBAR = R.id.progressbar;
    private static final int TEXTVIEW_STOP_NAME = R.id.textview_stop_name;
    private static final int TEXTVIEW_INBOUND_STOP1_NAME = R.id.textview_inbound_stop1_name;
    private static final int TEXTVIEW_INBOUND_STOP1_TIME = R.id.textview_inbound_stop1_time;
    private static final int TEXTVIEW_INBOUND_STOP2_NAME = R.id.textview_inbound_stop2_name;
    private static final int TEXTVIEW_INBOUND_STOP2_TIME = R.id.textview_inbound_stop2_time;
    private static final int TEXTVIEW_OUTBOUND_STOP1_NAME = R.id.textview_outbound_stop1_name;
    private static final int TEXTVIEW_OUTBOUND_STOP1_TIME = R.id.textview_outbound_stop1_time;
    private static final int TEXTVIEW_OUTBOUND_STOP2_NAME = R.id.textview_outbound_stop2_name;
    private static final int TEXTVIEW_OUTBOUND_STOP2_TIME = R.id.textview_outbound_stop2_time;

    private static final String WIDGET_CLICK_STOP_NAME = "WidgetClickStopName";
    private static final String WIDGET_CLICK_LEFT_ARROW = "WidgetClickLeftArrow";
    private static final String WIDGET_CLICK_RIGHT_ARROW = "WidgetClickRightArrow";
    private static final String WIDGET_CLICK_STOP_FORECAST = "WidgetClickStopForecast";

    private static int[] textViewInboundStopNames = {
            TEXTVIEW_INBOUND_STOP1_NAME,
            TEXTVIEW_INBOUND_STOP2_NAME
    };

    private static int[] textViewInboundStopTimes = {
            TEXTVIEW_INBOUND_STOP1_TIME,
            TEXTVIEW_INBOUND_STOP2_TIME
    };

    private static int[] textViewOutboundStopNames = {
            TEXTVIEW_OUTBOUND_STOP1_NAME,
            TEXTVIEW_OUTBOUND_STOP2_NAME
    };

    private static int[] textViewOutboundStopTimes = {
            TEXTVIEW_OUTBOUND_STOP1_TIME,
            TEXTVIEW_OUTBOUND_STOP2_TIME
    };

    private static RemoteViews views;
    private static Resources res;

    private static TimerTask timerTaskStopForecastTimeout;

    private static long stopForecastLastClickTime = 0;

    private static List<CharSequence> listSelectedStops;
    private static int indexNextStopToLoad;
    private static EnglishGaeilgeMap mapEnglishGaeilge;
    private static StopNameIdMap mapStopNameId;
    private static String localeDefault;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them.
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
        }
    }

    @Override
    public void onEnabled(Context context) {
        Log.i(LOG_TAG, "Widget first created.");

        // Construct the RemoteViews object.
        views = new RemoteViews(context.getPackageName(), R.layout.stop_forecast_widget);
    }

    @Override
    public void onDisabled(Context context) {
        Log.i(LOG_TAG, "Widget disabled.");
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        super.onReceive(context, intent);

        String selectedStopName = loadSelectedStopName(context);

        // Construct the RemoteViews object.
        views = new RemoteViews(context.getPackageName(), R.layout.stop_forecast_widget);

        if (loadListSelectedStops(context) != null)
            listSelectedStops = loadListSelectedStops(context);

        /*
         * If the user taps the stop name, switch to the next stop in their list of selected stops
         * for the widget.
         */
        if (intent.getAction().equals(WIDGET_CLICK_LEFT_ARROW)) {
            /*
             * Reset the stop forecast timeout.
             */
            stopForecastTimeout(context, 0, STOP_FORECAST_TIMEOUT_MILLIS);

            /*
             * Move on to the previous index in the list. If we're on the first index, reset back to
             * the last index [listSelectedStops.size() - 1].
             */
            if (indexNextStopToLoad != 0)
                indexNextStopToLoad--;
            else
                indexNextStopToLoad = listSelectedStops.size() - 1;

            loadStopForecast(context, listSelectedStops.get(indexNextStopToLoad).toString());
        }

        if (intent.getAction().equals(WIDGET_CLICK_RIGHT_ARROW)) {
            /*
             * Reset the stop forecast timeout.
             */
            stopForecastTimeout(context, 0, STOP_FORECAST_TIMEOUT_MILLIS);

            /*
             * Move on to the next index in the list. If we're on the last index, reset back to the
             * first index (0).
             */
            if (indexNextStopToLoad != listSelectedStops.size() - 1)
                indexNextStopToLoad++;
            else
                indexNextStopToLoad = 0;

            loadStopForecast(context, listSelectedStops.get(indexNextStopToLoad).toString());
        }

        /*
         * If the user taps the stop forecast display, load the forecast for that stop, setting up
         * a timeout as well.
         */
        if (intent.getAction().equals(WIDGET_CLICK_STOP_FORECAST)) {
            /*
             * Induce an artificial limit on number of allowed sequential clicks in order to prevent
             * server hammering.
             */
            if (SystemClock.elapsedRealtime() - stopForecastLastClickTime < LOAD_LIMIT_MILLIS)
                return;

            stopForecastLastClickTime = SystemClock.elapsedRealtime();

            /*
             * Start by resetting the stop forecast timeout.
             */
            stopForecastTimeout(context, 0, STOP_FORECAST_TIMEOUT_MILLIS);

            loadStopForecast(context, selectedStopName);
        }
    }

    /**
     * Save the currently-selected stop name to shared preferences.
     * @param context Context.
     * @param selectedStopName Name of the stop to save to shared preferences.
     */
    static void saveSelectedStopName(Context context, String selectedStopName) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString("selectedStopName", selectedStopName);
        prefs.apply();
    }

    /**
     * Load the currently-selected stop name from shared preferences.
     * @param context Context
     * @return Selected stop name.
     */
    static String loadSelectedStopName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);

        return prefs.getString("selectedStopName", null);
    }

    /**
     * Load list of user-selected stops from file.
     * @param context Context.
     * @return List of user-selected stops.
     */
    static List<CharSequence> loadListSelectedStops(Context context) {
        try {
            /*
             * Open the "widget_selected_stops" file and read in the List object of selected stops
             * contained within.
             */
            InputStream fileInput = context.openFileInput(FILE_WIDGET_SELECTED_STOPS);
            InputStream buffer = new BufferedInputStream(fileInput);
            ObjectInput objectInput = new ObjectInputStream(buffer);

            //noinspection unchecked
            listSelectedStops = (List<CharSequence>) objectInput.readObject();

            return listSelectedStops;
        } catch (ClassNotFoundException | FileNotFoundException fnfe) {
            /*
             * If the favourites file doesn't exist, the user has probably not set up this
             * feature yet. Handle the exception gracefully by displaying a TextView with
             * instructions on how to add favourites.
             */
            Log.i(LOG_TAG, "Widget selected stops not yet set up.");
        } catch (IOException e) {
            /*
             * Something has gone wrong; the file may have been corrupted. Delete the file.
             */
            Log.e(LOG_TAG, Log.getStackTraceString(e));
            Log.i(LOG_TAG, "Deleting widget selected stops file.");
            context.deleteFile(FILE_WIDGET_SELECTED_STOPS);
        }

        return null;
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        String selectedStopName = null;

        res = context.getResources();

        // Initialise correct locale.
        localeDefault = Locale.getDefault().toString();

        // Construct the RemoteViews object.
        views = new RemoteViews(context.getPackageName(), R.layout.stop_forecast_widget);

        // Set up Intents to register taps on the widget.
        Intent intentWidgetClickLeftArrow = new Intent(context, StopForecastWidget.class);
        Intent intentWidgetClickRightArrow = new Intent(context, StopForecastWidget.class);
        Intent intentWidgetClickStopForecast = new Intent(context, StopForecastWidget.class);

        intentWidgetClickLeftArrow.setAction(WIDGET_CLICK_LEFT_ARROW);
        intentWidgetClickRightArrow.setAction(WIDGET_CLICK_RIGHT_ARROW);
        intentWidgetClickStopForecast.setAction(WIDGET_CLICK_STOP_FORECAST);

        PendingIntent pendingIntentWidgetClickLeftArrow =
                PendingIntent.getBroadcast(context, 0, intentWidgetClickLeftArrow, 0);
        PendingIntent pendingIntentWidgetClickRightArrow =
                PendingIntent.getBroadcast(context, 0, intentWidgetClickRightArrow, 0);
        PendingIntent pendingIntentWidgetClickStopForecast =
                PendingIntent.getBroadcast(context, 0, intentWidgetClickStopForecast, 0);

        views.setOnClickPendingIntent(
                R.id.textview_stop_name_left_arrow, pendingIntentWidgetClickLeftArrow
        );
        views.setOnClickPendingIntent(
                R.id.textview_stop_name_right_arrow, pendingIntentWidgetClickRightArrow
        );
        views.setOnClickPendingIntent(
                R.id.linearlayout_stop_forecast, pendingIntentWidgetClickStopForecast
        );

        if (loadListSelectedStops(context) != null) {
            listSelectedStops = loadListSelectedStops(context);

            selectedStopName = listSelectedStops.get(0).toString();
            saveSelectedStopName(context, selectedStopName);

            loadStopForecast(context, selectedStopName);
        }

        // Set up 20-second timeout for stop forecast.
        stopForecastTimeout(context, 0, STOP_FORECAST_TIMEOUT_MILLIS);

        if (selectedStopName != null)
            loadStopForecast(context, selectedStopName);

        // Instruct the widget manager to update the widget.
        updateAppWidget(context, views);
    }

    /**
     * Temporary overloaded method.
     * @param context Context.
     * @param views RemoteViews object to update.
     */
    static void updateAppWidget(Context context, RemoteViews views) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, StopForecastWidget.class);

        appWidgetManager.updateAppWidget(thisWidget, views);
    }

    /**
     * Set up timeout for stop forecast, which clears the stop forecast and displays a holding
     * message after a set period.
     * This is a necessary evil due to their currently being no way for a widget to know when it
     * is "active" or "visible to the user". This implementation is in order to not hammer the
     * battery and network.
     * @param context Context.
     * @param delayTimeMillis The delay (ms) before starting the timer.
     * @param timeoutTimeMillis The period (ms) after which the stop forecast should be considered
     *                          expired and cleared.
     */
    static void stopForecastTimeout(
            final Context context, int delayTimeMillis, int timeoutTimeMillis) {
        if (timerTaskStopForecastTimeout != null)
            timerTaskStopForecastTimeout.cancel();

        final RemoteViews remoteViews = new RemoteViews(
                context.getPackageName(),
                R.layout.stop_forecast_widget
        );

        timerTaskStopForecastTimeout = new TimerTask() {
            @Override
            public void run() {
                clearStopForecast(remoteViews);
                remoteViews.setTextViewText(
                        TEXTVIEW_INBOUND_STOP1_NAME,
                        context.getResources().getString(R.string.tap_to_load_times)
                );

                updateAppWidget(context, remoteViews);
            }
        };

        new Timer().schedule(timerTaskStopForecastTimeout, delayTimeMillis, timeoutTimeMillis);
    }

    /**
     * Load the stop forecast for a particular stop.
     * @param context Context.
     * @param stopName The stop for which to load a stop forecast.
     */
    static void loadStopForecast(final Context context, String stopName) {
        // Instantiate a new EnglishGaeilgeMap.
        mapEnglishGaeilge = new EnglishGaeilgeMap();

        // Instantiate a new StopNameIdMap.
        mapStopNameId = new StopNameIdMap(localeDefault);

        // Set the stop name in the widget.
        views.setTextViewText(TEXTVIEW_STOP_NAME, stopName);

        // Keep track of the selected stop.
        saveSelectedStopName(context, stopName);

        /*
         * Dublinked is protected by HTTP Basic auth. Send the username and password as
         * part of the request. This username and password should not be important from a
         * security perspective, as the auth is just used as a simple rate limiter.
         */
        final String BASIC_AUTH =
                "Basic " + Base64.encodeToString(
                        (Auth.DUBLINKED_USER + ":" + Auth.DUBLINKED_PASS).getBytes(),
                        Base64.NO_WRAP
                );

        setIsLoading(context, true);

        /*
         * Prepare Retrofit API call.
         */
        final RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(API_URL)
                .build();

        ApiMethods methods = restAdapter.create(ApiMethods.class);

        Callback callback = new Callback() {
            @Override
            public void success(Object o, Response response) {
                // Cast the returned Object to a usable ApiTimes object.
                ApiTimes apiTimes = (ApiTimes) o;

                // Then create a stop forecast with this data.
                StopForecast stopForecast = createStopForecast(apiTimes);

                // Update the stop forecast.
                updateStopForecast(context, stopForecast);

                // Stop the refresh animations.
                setIsLoading(context, false);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Log.e(LOG_TAG, "Failure in call to server.");
                Log.e(LOG_TAG, retrofitError.getMessage());
            }
        };

        /*
         * Call API and get stop forecast from server.
         */
        methods.getStopForecast(
                BASIC_AUTH,
                API_FORMAT,
                mapStopNameId.get(stopName),
                callback
        );
    }

    /**
     * Create a usable stop forecast with the data returned from the server.
     * @param apiTimes ApiTimes object created by Retrofit, containing raw stop forecast data.
     * @return Usable stop forecast.
     */
    private static StopForecast createStopForecast(ApiTimes apiTimes) {
        StopForecast stopForecast = new StopForecast();

        for (ApiTimes.Result result : apiTimes.results) {
            Tram tram = new Tram(
                    // Strip out the annoying "LUAS " prefix from the destination.
                    result.destination.replace("LUAS ", ""),
                    result.direction,
                    result.duetime
            );

            switch(tram.getDirection()) {
                case "I":
                    stopForecast.addInboundTram(tram);

                    break;

                case "O":
                    stopForecast.addOutboundTram(tram);

                    break;

                default:
                    // If for some reason the direction doesn't make sense.
                    Log.e(LOG_TAG, "Invalid direction: " + tram.getDirection());
            }
        }

        stopForecast.setErrorMessage(apiTimes.getErrorMessage());

        return stopForecast;
    }

    /**
     * Make progress bar appear or disappear.
     * @param context Context.
     * @param loading Whether or not progress bar should animate.
     */
    static void setIsLoading(Context context, boolean loading) {
        views.setProgressBar(PROGRESSBAR, 0, 0, loading);

        updateAppWidget(context, views);
    }

    /**
     * Clear the stop forecast currently displayed in the widget.
     * @param remoteViewToClear RemoteViews object to clear.
     */
    static void clearStopForecast(RemoteViews remoteViewToClear) {
        /*
         * Clear the stop forecast.
         */
        for (int i = 0; i < 2; i++) {
            remoteViewToClear.setTextViewText(textViewInboundStopNames[i], "");
            remoteViewToClear.setTextViewText(textViewInboundStopTimes[i], "");

            remoteViewToClear.setTextViewText(textViewOutboundStopNames[i], "");
            remoteViewToClear.setTextViewText(textViewOutboundStopTimes[i], "");
        }
    }

    /**
     * Update the current stop forecast with newer information from the server.
     * @param context Context.
     * @param sf Latest stop forecast from server.
     */
    private static void updateStopForecast(Context context, StopForecast sf) {
        mapEnglishGaeilge = new EnglishGaeilgeMap();

        // If a valid stop forecast exists...
        if (sf != null) {
            if (sf.getErrorMessage() != null) {
                if (sf.getErrorMessage().equals("")) {
                    /*
                     * No error message on server. Change the stop name TextView to green.
                     */
                    views.setInt(R.id.linearlayout_stop_name,
                            "setBackgroundResource",
                            R.color.message_success
                    );
                } else if (sf.getErrorMessage().equalsIgnoreCase(
                        context.getString(R.string.message_no_results))) {
                    views.setInt(R.id.linearlayout_stop_name,
                            "setBackgroundResource",
                            R.color.message_not_running
                    );
                } else {
                    Log.w(LOG_TAG, "Server has returned a service disruption or error.");

                    views.setInt(R.id.linearlayout_stop_name,
                            "setBackgroundResource",
                            R.color.message_error
                    );

                    /*
                     * To make best use of the widget's real estate, re-use one of the inbound
                     * stop TextViews for the status message.
                     */
                    views.setTextViewText(
                            R.id.textview_inbound_stop1_name,
                            sf.getErrorMessage()
                    );
                }
            }

            /*
             * Pull in all trams from the StopForecast, but only display up to two inbound
             * and outbound trams.
             */
            if (sf.getInboundTrams() != null) {
                if (sf.getInboundTrams().size() == 0) {
                    views.setTextViewText(TEXTVIEW_INBOUND_STOP1_NAME,
                            context.getResources().getString(R.string.no_trams_forecast)
                    );
                } else {
                    String inboundTram;

                    for (int i = 0; i < sf.getInboundTrams().size(); i++) {
                        if (i < 2) {
                            if (localeDefault.startsWith(GAEILGE)) {
                                inboundTram = mapEnglishGaeilge.get(
                                        sf.getInboundTrams().get(i).getDestination()
                                );
                            } else {
                                inboundTram = sf.getInboundTrams().get(i).getDestination();
                            }

                            views.setTextViewText(textViewInboundStopNames[i], inboundTram);

                            if (sf.getInboundTrams()
                                    .get(i).getDueMinutes().equalsIgnoreCase("DUE")) {
                                String dueMinutes;

                                if (localeDefault.startsWith(GAEILGE)) {
                                    dueMinutes = mapEnglishGaeilge.get("DUE");
                                } else {
                                    dueMinutes = "DUE";
                                }

                                views.setTextViewText(
                                        textViewInboundStopTimes[i],
                                        dueMinutes
                                );
                            } else if (localeDefault.startsWith(GAEILGE)) {
                                views.setTextViewText(
                                        textViewInboundStopTimes[i],
                                        sf.getInboundTrams().get(i).getDueMinutes() + "n"
                                );
                            } else {
                                views.setTextViewText(
                                        textViewInboundStopTimes[i],
                                        sf.getInboundTrams().get(i).getDueMinutes() + "m"
                                );
                            }
                        }
                    }
                }
            }

            if (sf.getOutboundTrams() != null) {
                if (sf.getOutboundTrams().size() == 0) {
                    views.setTextViewText(TEXTVIEW_OUTBOUND_STOP1_NAME,
                            context.getResources().getString(R.string.no_trams_forecast)
                    );
                } else {
                    String outboundTram;

                    for (int i = 0; i < sf.getOutboundTrams().size(); i++) {
                        if (i < 2) {
                            if (localeDefault.startsWith(GAEILGE)) {
                                outboundTram = mapEnglishGaeilge.get(
                                        sf.getOutboundTrams().get(i).getDestination()
                                );
                            } else {
                                outboundTram = sf.getOutboundTrams().get(i).getDestination();
                            }

                            views.setTextViewText(textViewOutboundStopNames[i], outboundTram);

                            if (sf.getOutboundTrams()
                                    .get(i).getDueMinutes().equalsIgnoreCase("DUE")) {
                                String dueMinutes;

                                if (localeDefault.startsWith(GAEILGE)) {
                                    dueMinutes = mapEnglishGaeilge.get("DUE");
                                } else {
                                    dueMinutes = "DUE";
                                }

                                views.setTextViewText(
                                        textViewOutboundStopTimes[i],
                                        dueMinutes
                                );
                            } else if (localeDefault.startsWith(GAEILGE)) {
                                views.setTextViewText(
                                        textViewOutboundStopTimes[i],
                                        sf.getOutboundTrams().get(i).getDueMinutes() + "n"
                                );
                            } else {
                                views.setTextViewText(
                                        textViewOutboundStopTimes[i],
                                        sf.getOutboundTrams().get(i).getDueMinutes() + "m"
                                );
                            }
                        }
                    }
                }
            }
        } else {
            Log.e(LOG_TAG, "Error in stop forecast (equals null).");

            /*
             * If no stop forecast can be retrieved, set a generic error message and
             * change the color of the message title box red.
             */
            views.setInt(TEXTVIEW_STOP_NAME,
                    "setBackgroundResource",
                    R.color.message_error
            );

            views.setTextViewText(
                    TEXTVIEW_INBOUND_STOP1_NAME,
                    res.getString(R.string.message_error)
            );
        }
    }
}
