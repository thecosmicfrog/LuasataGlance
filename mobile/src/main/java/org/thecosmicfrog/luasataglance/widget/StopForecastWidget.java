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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.util.Auth;
import org.thecosmicfrog.luasataglance.object.EnglishGaeilgeMap;
import org.thecosmicfrog.luasataglance.object.StopForecast;
import org.thecosmicfrog.luasataglance.object.Tram;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in
 * {@link StopForecastWidgetConfigureActivity StopForecastWidgetConfigureActivity}
 */
public class StopForecastWidget extends AppWidgetProvider {

    private static final String LOG_TAG = StopForecastWidget.class.getSimpleName();

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
        if (intent.getAction().equals(WIDGET_CLICK_STOP_NAME)) {
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

        // Construct the RemoteViews object.
        views = new RemoteViews(context.getPackageName(), R.layout.stop_forecast_widget);

        // Set up Intents to register taps on the widget.
        Intent intentWidgetClickStopName = new Intent(context, StopForecastWidget.class);
        Intent intentWidgetClickStopForecast = new Intent(context, StopForecastWidget.class);

        intentWidgetClickStopName.setAction(WIDGET_CLICK_STOP_NAME);
        intentWidgetClickStopForecast.setAction(WIDGET_CLICK_STOP_FORECAST);

        PendingIntent pendingIntentWidgetClickStopName =
                PendingIntent.getBroadcast(context, 0, intentWidgetClickStopName, 0);
        PendingIntent pendingIntentWidgetClickStopForecast =
                PendingIntent.getBroadcast(context, 0, intentWidgetClickStopForecast, 0);

        views.setOnClickPendingIntent(
                R.id.relativelayout_stop_name, pendingIntentWidgetClickStopName
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
    static void loadStopForecast(Context context, String stopName) {
        // Instantiate an object of EnglishGaeilgeMap.
        mapEnglishGaeilge = new EnglishGaeilgeMap();

        // Set the stop name in the widget.
        views.setTextViewText(TEXTVIEW_STOP_NAME, stopName);

        // Keep track of the selected stop.
        saveSelectedStopName(context, stopName);

        /*
         * Create a two-field List containing the Context and stop name to load a stop forecast
         * for. This is a slightly ugly way of ensuring the appropriate Context is worked on by the
         * AsyncTask.
         */
        Object[] contextAndStopName = {context, stopName};
        List<Object> listContextAndStopName = Arrays.asList(contextAndStopName);

        //noinspection unchecked
        new FetchLuasTimes().execute(listContextAndStopName);
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

    static class FetchLuasTimes extends AsyncTask<List<Object>, Void, List<Object>> {

        private final String LOG_TAG = FetchLuasTimes.class.getSimpleName();

        private final String GAEILGE = "ga";

        private final String localeDefault;

        Map<String, String> stopCodes;

        public FetchLuasTimes() {
            localeDefault = Locale.getDefault().toString();

            /*
             * If the user's default locale is set to Irish (Gaeilge), build a Map
             * of Irish stop names:codes.
             * If not, default to English.
             */
            if (localeDefault.startsWith(GAEILGE)) {
                stopCodes = new HashMap<String, String>() {
                    {
                        // Red Line
                        put("Iosta na Rinne", "LUAS57");
                        put("Duga Spencer", "LUAS56");
                        put("Cearnóg an Mhéara - CNÉ", "LUAS55");
                        put("Duga Sheoirse", "LUAS54");
                        put("Conghaile", "LUAS23");
                        put("Busáras", "LUAS22");
                        put("Sráid na Mainistreach", "LUAS21");
                        put("Jervis", "LUAS20");
                        put("Na Ceithre Cúirteanna", "LUAS19");
                        put("Margadh na Feirme", "LUAS18");
                        put("Árd-Mhúsaem", "LUAS17");
                        put("Heuston", "LUAS16");
                        put("Ospidéal San Séamas", "LUAS15");
                        put("Fatima", "LUAS14");
                        put("Rialto", "LUAS13");
                        put("Bóthar na Siúire", "LUAS12");
                        put("An Droichead Órga", "LUAS11");
                        put("Droimeanach", "LUAS10");
                        put("An Capall Dubh", "LUAS9");
                        put("An Cloigín Gorm", "LUAS8");
                        put("An Chill Mhór", "LUAS7");
                        put("An Bhó Dhearg", "LUAS6");
                        put("Coill an Rí", "LUAS5");
                        put("Belgard", "LUAS4");
                        put("Baile an Chócaigh", "LUAS3");
                        put("Ospidéal Thamhlachta", "LUAS2");
                        put("Tamhlacht", "LUAS1");
                        put("Fothar Chardain", "LUAS49");
                        put("Baile an tSíbrigh", "LUAS50");
                        put("Campas Gnó Iarthar na Cathrach", "LUAS51");
                        put("Baile Uí Fhoirtcheirn", "LUAS52");
                        put("Teach Sagard", "LUAS53");

                        // Green Line
                        put("Faiche Stiabhna", "LUAS24");
                        put("Sráid Fhearchair", "LUAS25");
                        put("Charlemont", "LUAS26");
                        put("Raghnallach", "LUAS27");
                        put("Coill na Feá", "LUAS28");
                        put("Cowper", "LUAS29");
                        put("Baile an Mhuilinn", "LUAS30");
                        put("Na Glasáin", "LUAS31");
                        put("Dún Droma", "LUAS32");
                        put("Baile Amhlaoibh", "LUAS33");
                        put("Cill Mochuda", "LUAS34");
                        put("Stigh Lorgan", "LUAS35");
                        put("Áth an Ghainimh", "LUAS36");
                        put("An Pháirc Láir", "LUAS37");
                        put("Gleann an Chairn", "LUAS38");
                        put("An Eachrais", "LUAS39");
                        put("Gleann Bhaile na Lobhar", "LUAS40");
                        put("Coill Bhaile Uí Ógáin", "LUAS42");
                        put("Carraig Mhaighin", "LUAS44");
                        put("Baile an Locháin", "LUAS46");
                        put("Coill na Silíní", "LUAS47");
                        put("Gleann Bhríde", "LUAS48");
                    }
                };
            } else {
                stopCodes = new HashMap<String, String>() {
                    {
                        // Red Line
                        put("The Point", "LUAS57");
                        put("Spencer Dock", "LUAS56");
                        put("Mayor Square - NCI", "LUAS55");
                        put("George's Dock", "LUAS54");
                        put("Connolly", "LUAS23");
                        put("Busáras", "LUAS22");
                        put("Abbey Street", "LUAS21");
                        put("Jervis", "LUAS20");
                        put("Four Courts", "LUAS19");
                        put("Smithfield", "LUAS18");
                        put("Museum", "LUAS17");
                        put("Heuston", "LUAS16");
                        put("James's", "LUAS15");
                        put("Fatima", "LUAS14");
                        put("Rialto", "LUAS13");
                        put("Suir Road", "LUAS12");
                        put("Goldenbridge", "LUAS11");
                        put("Drimnagh", "LUAS10");
                        put("Blackhorse", "LUAS9");
                        put("Bluebell", "LUAS8");
                        put("Kylemore", "LUAS7");
                        put("Red Cow", "LUAS6");
                        put("Kingswood", "LUAS5");
                        put("Belgard", "LUAS4");
                        put("Cookstown", "LUAS3");
                        put("Hospital", "LUAS2");
                        put("Tallaght", "LUAS1");
                        put("Fettercairn", "LUAS49");
                        put("Cheeverstown", "LUAS50");
                        put("Citywest Campus", "LUAS51");
                        put("Fortunestown", "LUAS52");
                        put("Saggart", "LUAS53");

                        // Green Line
                        put("St. Stephen's Green", "LUAS24");
                        put("Harcourt", "LUAS25");
                        put("Charlemont", "LUAS26");
                        put("Ranelagh", "LUAS27");
                        put("Beechwood", "LUAS28");
                        put("Cowper", "LUAS29");
                        put("Milltown", "LUAS30");
                        put("Windy Arbour", "LUAS31");
                        put("Dundrum", "LUAS32");
                        put("Balally", "LUAS33");
                        put("Kilmacud", "LUAS34");
                        put("Stillorgan", "LUAS35");
                        put("Sandyford", "LUAS36");
                        put("Central Park", "LUAS37");
                        put("Glencairn", "LUAS38");
                        put("The Gallops", "LUAS39");
                        put("Leopardstown Valley", "LUAS40");
                        put("Ballyogan Wood", "LUAS42");
                        put("Carrickmines", "LUAS44");
                        put("Laughanstown", "LUAS46");
                        put("Cherrywood", "LUAS47");
                        put("Brides Glen", "LUAS48");
                    }
                };
            }
        }

        @SafeVarargs
        @Override
        protected final List<Object> doInBackground(List<Object>... params) {
            if (params.length == 0)
                return null;

            List<Object> listParams = params[0];

            // Pull the Context out of the List.
            Context context = (Context) listParams.get(0);

            HttpURLConnection httpUrlConnection = null;
            BufferedReader reader = null;

            String luasTimesJson = null;

            // HTTP parameters to pass to the API.
            String format = "json";
            String stopName = (String) listParams.get(1);
            String stopId = stopCodes.get(stopName);

            try {
                setIsLoading(context, true);

                // Build the API URL.
                final String BASE_URL =
                        "http://www.dublinked.ie/cgi-bin/rtpi/realtimebusinformation?";
                final String PARAM_STOPID = "stopid";
                final String PARAM_FORMAT = "format";

                Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter(PARAM_STOPID, stopId)
                        .appendQueryParameter(PARAM_FORMAT, format)
                        .build();

                URL url = new URL(builtUri.toString());

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

                httpUrlConnection = (HttpURLConnection) url.openConnection();
                httpUrlConnection.setRequestMethod("GET");
                httpUrlConnection.setRequestProperty("Authorization", BASIC_AUTH);
                httpUrlConnection.connect();

                InputStream inputStream = httpUrlConnection.getInputStream();
                StringBuilder stringBuilder = new StringBuilder();

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }

                if (inputStream == null || stringBuilder.length() == 0)
                    luasTimesJson = null;
                else
                    luasTimesJson = stringBuilder.toString();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                if (httpUrlConnection != null)
                    httpUrlConnection.disconnect();

                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException ioe) {
                        Log.e(LOG_TAG, "Error closing stream.", ioe);
                    }
                }
            }

            if (luasTimesJson != null) {
                try {
                    StopForecast stopForecast = getLuasDataFromJson(luasTimesJson);
                    Object[] contextAndStopForecast = {context, stopForecast};
                    List<Object> listContextAndStopForecast = Arrays.asList(contextAndStopForecast);

                    return listContextAndStopForecast;
                } catch (JSONException je) {
                    je.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(List<Object> listContextAndStopForecast) {
            Context context = (Context) listContextAndStopForecast.get(0);
            StopForecast stopForecast = (StopForecast) listContextAndStopForecast.get(1);

            updateStopForecast(context, stopForecast);
            setIsLoading(context, false);

            updateAppWidget(context, views);
        }

        /**
         * Update the current stop forecast with newer information from the server.
         * @param context Context.
         * @param sf Latest stop forecast from server.
         */
        private void updateStopForecast(Context context, StopForecast sf) {
            mapEnglishGaeilge = new EnglishGaeilgeMap();

            // If a valid stop forecast exists...
            if (sf != null) {
                if (sf.getErrorMessage() != null) {
                    if (sf.getErrorMessage().equals("")) {
                        /*
                         * No error message on server. Change the stop name TextView to green.
                         */
                        views.setInt(R.id.textview_stop_name,
                                "setBackgroundResource",
                                R.color.message_success
                        );
                    } else {
                        Log.e(LOG_TAG, "Server has returned a service disruption or error.");

                        views.setInt(R.id.textview_stop_name,
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

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy: constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         * @param forecastJsonStr The latest stop forecast from the server, in JSON format.
         */
        private StopForecast getLuasDataFromJson(String forecastJsonStr)
                throws JSONException {

            StopForecast stopForecast = new StopForecast();

            // These are the names of the JSON objects that need to be extracted.
            final String LUAS_ERRORMESSAGE = "errormessage";
            final String LUAS_RESULTS = "results";
            final String LUAS_DESTINATION = "destination";
            final String LUAS_DIRECTION = "direction";
            final String LUAS_DUETIME = "duetime";

            JSONObject tramsJson = new JSONObject(forecastJsonStr);

            /*
             * If a message is returned from the server, add it to the StopForecast object.
             * Otherwise, set the message field to null.
             */
            if (tramsJson.has(LUAS_ERRORMESSAGE)) {
                stopForecast.setErrorMessage(tramsJson.getString(LUAS_ERRORMESSAGE));
            } else {
                stopForecast.setErrorMessage(null);
            }

            /*
             * If a list of trams is returned from the server, add it to the StopForecast object
             * as an array of both inbound and output trams.
             * Otherwise, set both fields to null.
             */
            if (tramsJson.has(LUAS_RESULTS)) {
                JSONArray tramsArray = tramsJson.getJSONArray(LUAS_RESULTS);

                Tram[] trams = new Tram[tramsArray.length()];

                for (int i = 0; i < tramsArray.length(); i++) {
                    String destination;
                    String direction;
                    String duetime;

                    // Get the JSON object representing the trams.
                    JSONObject tramObject = tramsArray.getJSONObject(i);

                    // Strip out the annoying "LUAS " prefix from the destination.
                    destination = tramObject.getString(LUAS_DESTINATION).replace("LUAS ", "");

                    direction = tramObject.getString(LUAS_DIRECTION);
                    duetime = tramObject.getString(LUAS_DUETIME);

                    trams[i] = new Tram(destination, direction, duetime);

                    switch (trams[i].getDirection()) {
                        case "I":
                            stopForecast.addInboundTram(trams[i]);

                            break;

                        case "O":
                            stopForecast.addOutboundTram(trams[i]);

                            break;

                        default:
                            // If for some reason the direction doesn't make sense.
                            Log.e(LOG_TAG, "Invalid direction: " + trams[i].getDirection());
                    }
                }
            } else {
                /*
                 * If there is no "trams" object in the JSON returned from the server,
                 * there are no inbound or outbound trams forecast. This can happen
                 * frequently for some stops, such as Connolly, which ceases service
                 * earlier than others.
                 * In this case, set empty ArrayLists for inbound and outbound trams.
                 */
                stopForecast.setInboundTrams(new ArrayList<Tram>());
                stopForecast.setOutboundTrams(new ArrayList<Tram>());
            }

            return stopForecast;
        }
    }
}
