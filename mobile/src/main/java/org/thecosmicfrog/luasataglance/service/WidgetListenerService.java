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

package org.thecosmicfrog.luasataglance.service;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.api.ApiMethods;
import org.thecosmicfrog.luasataglance.api.ApiTimes;
import org.thecosmicfrog.luasataglance.object.EnglishGaeilgeMap;
import org.thecosmicfrog.luasataglance.object.StopForecast;
import org.thecosmicfrog.luasataglance.object.StopNameIdMap;
import org.thecosmicfrog.luasataglance.object.Tram;

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

public class WidgetListenerService extends Service {

    private final String LOG_TAG = WidgetListenerService.class.getSimpleName();
    private final String SELECTED_STOP_NAME = "selectedStopName";
    private final int TEXTVIEW_STOP_NAME = R.id.textview_stop_name;
    private final int TEXTVIEW_INBOUND_STOP1_NAME = R.id.textview_inbound_stop1_name;
    private final int TEXTVIEW_INBOUND_STOP1_TIME = R.id.textview_inbound_stop1_time;
    private final int TEXTVIEW_INBOUND_STOP2_NAME = R.id.textview_inbound_stop2_name;
    private final int TEXTVIEW_INBOUND_STOP2_TIME = R.id.textview_inbound_stop2_time;
    private final int TEXTVIEW_OUTBOUND_STOP1_NAME = R.id.textview_outbound_stop1_name;
    private final int TEXTVIEW_OUTBOUND_STOP1_TIME = R.id.textview_outbound_stop1_time;
    private final int TEXTVIEW_OUTBOUND_STOP2_NAME = R.id.textview_outbound_stop2_name;
    private final int TEXTVIEW_OUTBOUND_STOP2_TIME = R.id.textview_outbound_stop2_time;

    private final int[] TEXTVIEW_INBOUND_STOP_NAMES = {
            TEXTVIEW_INBOUND_STOP1_NAME,
            TEXTVIEW_INBOUND_STOP2_NAME
    };

    private final int[] TEXTVIEW_INBOUND_STOP_TIMES = {
            TEXTVIEW_INBOUND_STOP1_TIME,
            TEXTVIEW_INBOUND_STOP2_TIME
    };

    private final int[] TEXTVIEW_OUTBOUND_STOP_NAMES = {
            TEXTVIEW_OUTBOUND_STOP1_NAME,
            TEXTVIEW_OUTBOUND_STOP2_NAME
    };

    private final int[] TEXTVIEW_OUTBOUND_STOP_TIMES = {
            TEXTVIEW_OUTBOUND_STOP1_TIME,
            TEXTVIEW_OUTBOUND_STOP2_TIME
    };

    private static TimerTask timerTaskStopForecastTimeout;

    private EnglishGaeilgeMap mapEnglishGaeilge;
    private List<CharSequence> listSelectedStops;
    private String localeDefault;

    public WidgetListenerService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final int STOP_FORECAST_TIMEOUT_MILLIS = 20000;

        if (isNetworkAvailable(getApplicationContext())) {
            Log.i(LOG_TAG, "Network available. Starting WidgetListenerService.");

            /* Initialise correct locale. */
            localeDefault = Locale.getDefault().toString();

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());

            int[] allWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

            /* Ensure sure we actually have an array of widget IDs. */
            if (allWidgetIds != null) {
                for (int widgetId : allWidgetIds) {
                    RemoteViews views = new RemoteViews(
                            getApplicationContext().getPackageName(),
                            R.layout.stop_forecast_widget
                    );

                    /*
                     * Reset the stop forecast timeout.
                     */
                    stopForecastTimeout(
                            appWidgetManager,
                            widgetId,
                            views,
                            STOP_FORECAST_TIMEOUT_MILLIS
                    );

                    if (loadListSelectedStops(getApplicationContext()) != null) {
                        String selectedStopName;
                        listSelectedStops = loadListSelectedStops(getApplicationContext());

                        if (intent.hasExtra(SELECTED_STOP_NAME)) {
                            selectedStopName = intent.getStringExtra(SELECTED_STOP_NAME);
                        } else {
                            selectedStopName = listSelectedStops.get(0).toString();
                        }

                        saveSelectedStopName(getApplicationContext(), selectedStopName);
                    }

                    loadStopForecast(
                            getApplicationContext(),
                            appWidgetManager,
                            widgetId,
                            views,
                            intent.getStringExtra(SELECTED_STOP_NAME)
                    );

                    appWidgetManager.partiallyUpdateAppWidget(widgetId, views);
                }
            }
        }

        stopSelf();

        /* Necessary? Trivial? Further research required. */
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        /* TODO: Return the communication channel to the service. */
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Check if network is available.
     * @param context Context.
     * @return Network available or not.
     */
    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
    }

    /**
     * Load the stop forecast for a particular stop.
     * @param context Context.
     * @param stopName The stop for which to load a stop forecast.
     */
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private void loadStopForecast(final Context context,final AppWidgetManager appWidgetManager,
                                  final int widgetId, final RemoteViews views, String stopName) {
        if (stopName != null) {
            /* API constants. */
            final String API_URL_PREFIX = "https://api";
            final String API_URL_POSTFIX = ".thecosmicfrog.org/cgi-bin";
            final String API_ACTION = "times";

            /* Instantiate a new EnglishGaeilgeMap. */
            mapEnglishGaeilge = new EnglishGaeilgeMap();

            /* Instantiate a new StopNameIdMap. */
            StopNameIdMap mapStopNameId = new StopNameIdMap(localeDefault);

            /* Set the stop name in the widget. */
            views.setTextViewText(TEXTVIEW_STOP_NAME, stopName);

            /* Keep track of the selected stop. */
            saveSelectedStopName(context, stopName);

            setIsLoading(appWidgetManager, widgetId, views, true);

            /*
             * Randomly choose an API endpoint to query. This provides load balancing and redundancy
             * in case of server failures.
             * All API endpoints are of the form: "apiN.thecosmicfrog.org", where N is determined by
             * the formula below.
             * The constant NUM_API_ENDPOINTS governs how many endpoints there currently are.
             */
            final int NUM_API_ENDPOINTS = 2;

            String apiEndpointToQuery = Integer.toString(
                    (int) (Math.random() * NUM_API_ENDPOINTS + 1)
            );

            String apiUrl = API_URL_PREFIX + apiEndpointToQuery + API_URL_POSTFIX;

            /*
             * Prepare Retrofit API call.
             */
            final RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(apiUrl)
                    .build();

            ApiMethods methods = restAdapter.create(ApiMethods.class);

            Callback<ApiTimes> callback = new Callback<ApiTimes>() {
                @Override
                public void success(ApiTimes apiTimes, Response response) {
                    /* Then create a stop forecast with this data. */
                    StopForecast stopForecast = createStopForecast(apiTimes);

                    /* Update the stop forecast. */
                    updateStopForecast(context, views, stopForecast);

                    appWidgetManager.partiallyUpdateAppWidget(widgetId, views);

                    /* Stop the refresh animations. */
                    setIsLoading(appWidgetManager, widgetId, views, false);
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    Log.e(LOG_TAG, "Failure during call to server.");

                    /*
                     * If we get a message or a response from the server, there's likely an issue
                     * with the client request or the server's response itself.
                     */
                    if (retrofitError.getMessage() != null)
                        Log.e(LOG_TAG, retrofitError.getMessage());

                    if (retrofitError.getResponse() != null) {
                        Log.e(LOG_TAG, retrofitError.getResponse().getUrl());
                        Log.e(LOG_TAG, Integer.toString(retrofitError.getResponse().getStatus()));
                        Log.e(LOG_TAG, retrofitError.getResponse().getHeaders().toString());
                        Log.e(LOG_TAG, retrofitError.getResponse().getBody().toString());
                        Log.e(LOG_TAG, retrofitError.getResponse().getReason());
                    }

                    /*
                     * If we don't receive a message or response, we can still get an idea of what's
                     * going on by getting the "kind" of error.
                     */
                    if (retrofitError.getKind() != null)
                        Log.e(LOG_TAG, retrofitError.getKind().toString());
                }
            };

            /*
             * Call API and get stop forecast from server.
             */
            methods.getStopForecast(
                    API_ACTION,
                    mapStopNameId.get(stopName),
                    callback
            );
        }
    }

    /**
     * Create a usable stop forecast with the data returned from the server.
     * @param apiTimes ApiTimes object created by Retrofit, containing raw stop forecast data.
     * @return Usable stop forecast.
     */
    private StopForecast createStopForecast(ApiTimes apiTimes) {
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
                        Log.e(LOG_TAG, "Invalid direction: " + tram.getDirection());
                }
            }
        }

        stopForecast.setMessage(apiTimes.getMessage());

        return stopForecast;
    }

    /**
     * Make progress bar appear or disappear.
     * @param loading Whether or not progress bar should animate.
     */
    private void setIsLoading(AppWidgetManager appWidgetManager,
                              int widgetId,
                              RemoteViews views,
                              boolean loading) {
        views.setProgressBar(
                R.id.progressbar,
                0,
                0,
                loading
        );

        appWidgetManager.partiallyUpdateAppWidget(widgetId, views);
    }

    /**
     * Clear the stop forecast currently displayed in the widget.
     * @param remoteViewToClear RemoteViews object to clear.
     */
    private void clearStopForecast(RemoteViews remoteViewToClear) {
        /*
         * Clear the stop forecast.
         */
        for (int i = 0; i < 2; i++) {
            remoteViewToClear.setTextViewText(TEXTVIEW_INBOUND_STOP_NAMES[i], "");
            remoteViewToClear.setTextViewText(TEXTVIEW_INBOUND_STOP_TIMES[i], "");

            remoteViewToClear.setTextViewText(TEXTVIEW_OUTBOUND_STOP_NAMES[i], "");
            remoteViewToClear.setTextViewText(TEXTVIEW_OUTBOUND_STOP_TIMES[i], "");
        }
    }

    /**
     * Save the currently-selected stop name to shared preferences.
     * @param context Context.
     * @param selectedStopName Name of the stop to save to shared preferences.
     * @return Successfully saved.
     */
    private boolean saveSelectedStopName(Context context, String selectedStopName) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance.StopForecastWidget";

        SharedPreferences.Editor prefs =
                context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();

        prefs.putString("selectedStopName", selectedStopName);

        return prefs.commit();
    }

    /**
     * Set up timeout for stop forecast, which clears the stop forecast and displays a holding
     * message after a set period.
     * This is a necessary evil due to their currently being no way for a widget to know when it
     * is "active" or "visible to the user". This implementation is in order to not hammer the
     * battery and network.
     * @param appWidgetManager AppWidgetManager.
     * @param widgetId App widget ID.
     * @param views RemoteViews.
     * @param timeoutTimeMillis The period (ms) after which the stop forecast should be considered
     *                          expired and cleared.
     */
    private void stopForecastTimeout(
            final AppWidgetManager appWidgetManager,
            final int widgetId,
            final RemoteViews views,
            int timeoutTimeMillis) {
        final int DELAY_TIME_MILLIS = 0;

        if (timerTaskStopForecastTimeout != null)
            timerTaskStopForecastTimeout.cancel();

        timerTaskStopForecastTimeout = new TimerTask() {
            @Override
            public void run() {
                clearStopForecast(views);
                views.setTextViewText(
                        TEXTVIEW_INBOUND_STOP1_NAME,
                        getResources().getString(R.string.tap_to_load_times)
                );

                appWidgetManager.partiallyUpdateAppWidget(widgetId, views);
            }
        };

        new Timer().schedule(timerTaskStopForecastTimeout, DELAY_TIME_MILLIS, timeoutTimeMillis);
    }

    /**
     * Load list of user-selected stops from file.
     * @param context Context.
     * @return List of user-selected stops.
     */
    private List<CharSequence> loadListSelectedStops(Context context) {
        final String FILE_WIDGET_SELECTED_STOPS = "widget_selected_stops";

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
        } catch (ClassNotFoundException | FileNotFoundException e) {
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

    /**
     * Update the current stop forecast with newer information from the server.
     * @param context Context.
     * @param stopForecast Latest stop forecast from server.
     */
    private void updateStopForecast(Context context, RemoteViews views, StopForecast stopForecast) {
        final String GAEILGE = "ga";

        String message;

        if (localeDefault.startsWith(GAEILGE)) {
            message = getResources().getString(R.string.message_success);
        } else {
            message = stopForecast.getMessage();
        }

        mapEnglishGaeilge = new EnglishGaeilgeMap();

        /* If a valid stop forecast exists... */
        if (stopForecast != null) {
            if (stopForecast.getMessage() != null) {
                if (message.contains(getString(R.string.message_success))) {
                    /*
                     * No error message on server. Change the stop name TextView to green.
                     */
                    views.setInt(
                            R.id.linearlayout_stop_name,
                            "setBackgroundResource",
                            R.color.message_success
                    );
                } else {
                    Log.w(LOG_TAG, "Server has returned a service disruption or error.");

                    views.setInt(
                            R.id.linearlayout_stop_name,
                            "setBackgroundResource",
                            R.color.message_error
                    );

                    /*
                     * To make best use of the widget's real estate, re-use one of the inbound
                     * stop TextViews for the status message.
                     */
                    views.setTextViewText(
                            R.id.textview_inbound_stop1_name,
                            message
                    );
                }
            }

            /*
             * Pull in all trams from the StopForecast, but only display up to two inbound
             * and outbound trams.
             */
            if (stopForecast.getInboundTrams() != null) {
                if (stopForecast.getInboundTrams().size() == 0) {
                    views.setTextViewText(
                            TEXTVIEW_INBOUND_STOP1_NAME,
                            context.getResources().getString(R.string.no_trams_forecast)
                    );
                } else {
                    String inboundTram;

                    for (int i = 0; i < stopForecast.getInboundTrams().size(); i++) {
                        if (i < 2) {
                            if (localeDefault.startsWith(GAEILGE)) {
                                inboundTram = mapEnglishGaeilge.get(
                                        stopForecast.getInboundTrams().get(i).getDestination()
                                );
                            } else {
                                inboundTram =
                                        stopForecast.getInboundTrams().get(i).getDestination();
                            }

                            views.setTextViewText(TEXTVIEW_INBOUND_STOP_NAMES[i], inboundTram);

                            if (stopForecast.getInboundTrams()
                                    .get(i).getDueMinutes().equalsIgnoreCase("DUE")) {
                                String dueMinutes;

                                if (localeDefault.startsWith(GAEILGE)) {
                                    dueMinutes = mapEnglishGaeilge.get("DUE");
                                } else {
                                    dueMinutes = "DUE";
                                }

                                views.setTextViewText(
                                        TEXTVIEW_INBOUND_STOP_TIMES[i],
                                        dueMinutes
                                );
                            } else if (localeDefault.startsWith(GAEILGE)) {
                                views.setTextViewText(
                                        TEXTVIEW_INBOUND_STOP_TIMES[i],
                                        stopForecast.getInboundTrams().get(i).getDueMinutes() + "n"
                                );
                            } else {
                                views.setTextViewText(
                                        TEXTVIEW_INBOUND_STOP_TIMES[i],
                                        stopForecast.getInboundTrams().get(i).getDueMinutes() + "m"
                                );
                            }
                        }
                    }
                }
            }

            if (stopForecast.getOutboundTrams() != null) {
                if (stopForecast.getOutboundTrams().size() == 0) {
                    views.setTextViewText(
                            TEXTVIEW_OUTBOUND_STOP1_NAME,
                            context.getResources().getString(R.string.no_trams_forecast)
                    );
                } else {
                    String outboundTram;

                    for (int i = 0; i < stopForecast.getOutboundTrams().size(); i++) {
                        if (i < 2) {
                            if (localeDefault.startsWith(GAEILGE)) {
                                outboundTram = mapEnglishGaeilge.get(
                                        stopForecast.getOutboundTrams().get(i).getDestination()
                                );
                            } else {
                                outboundTram =
                                        stopForecast.getOutboundTrams().get(i).getDestination();
                            }

                            views.setTextViewText(TEXTVIEW_OUTBOUND_STOP_NAMES[i], outboundTram);

                            if (stopForecast.getOutboundTrams()
                                    .get(i).getDueMinutes().equalsIgnoreCase("DUE")) {
                                String dueMinutes;

                                if (localeDefault.startsWith(GAEILGE)) {
                                    dueMinutes = mapEnglishGaeilge.get("DUE");
                                } else {
                                    dueMinutes = "DUE";
                                }

                                views.setTextViewText(
                                        TEXTVIEW_OUTBOUND_STOP_TIMES[i],
                                        dueMinutes
                                );
                            } else if (localeDefault.startsWith(GAEILGE)) {
                                views.setTextViewText(
                                        TEXTVIEW_OUTBOUND_STOP_TIMES[i],
                                        stopForecast.getOutboundTrams().get(i).getDueMinutes() + "n"
                                );
                            } else {
                                views.setTextViewText(
                                        TEXTVIEW_OUTBOUND_STOP_TIMES[i],
                                        stopForecast.getOutboundTrams().get(i).getDueMinutes() + "m"
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
            views.setInt(
                    TEXTVIEW_STOP_NAME,
                    "setBackgroundResource",
                    R.color.message_error
            );

            views.setTextViewText(
                    TEXTVIEW_INBOUND_STOP1_NAME,
                    getResources().getString(R.string.message_error)
            );
        }
    }
}
