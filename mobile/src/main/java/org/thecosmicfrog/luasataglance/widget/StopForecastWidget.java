package org.thecosmicfrog.luasataglance.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.object.EnglishGaeilgeMap;
import org.thecosmicfrog.luasataglance.object.StopForecast;
import org.thecosmicfrog.luasataglance.object.Tram;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link StopForecastWidgetConfigureActivity StopForecastWidgetConfigureActivity}
 */
public class StopForecastWidget extends AppWidgetProvider {

    private final static String LOG_TAG = StopForecastWidget.class.getSimpleName();

    static final int PROGRESSBAR = R.id.progressbar;
    static final int TEXTVIEW_STOP_NAME = R.id.textview_stop_name;
    static final int TEXTVIEW_INBOUND_STOP1_NAME = R.id.textview_inbound_stop1_name;
    static final int TEXTVIEW_INBOUND_STOP1_TIME = R.id.textview_inbound_stop1_time;
    static final int TEXTVIEW_INBOUND_STOP2_NAME = R.id.textview_inbound_stop2_name;
    static final int TEXTVIEW_INBOUND_STOP2_TIME = R.id.textview_inbound_stop2_time;
    static final int TEXTVIEW_OUTBOUND_STOP1_NAME = R.id.textview_outbound_stop1_name;
    static final int TEXTVIEW_OUTBOUND_STOP1_TIME = R.id.textview_outbound_stop1_time;
    static final int TEXTVIEW_OUTBOUND_STOP2_NAME = R.id.textview_outbound_stop2_name;
    static final int TEXTVIEW_OUTBOUND_STOP2_TIME = R.id.textview_outbound_stop2_time;

    private final static String WIDGET_CLICK_STOP_NAME = "WidgetClickStopName";
    private final static String WIDGET_CLICK_STOP_FORECAST = "WidgetClickStopForecast";

    private static RemoteViews views;
    private static int currAppWidgetId;
    private static AppWidgetManager currAppWidgetManager;
    private static Resources res;
    private static String selectedStopName;

    static int[] textViewInboundStopNames = {
            TEXTVIEW_INBOUND_STOP1_NAME,
            TEXTVIEW_INBOUND_STOP2_NAME
    };

    static int[] textViewInboundStopTimes = {
            TEXTVIEW_INBOUND_STOP1_TIME,
            TEXTVIEW_INBOUND_STOP2_TIME
    };

    static int[] textViewOutboundStopNames = {
            TEXTVIEW_OUTBOUND_STOP1_NAME,
            TEXTVIEW_OUTBOUND_STOP2_NAME
    };

    static int[] textViewOutboundStopTimes = {
            TEXTVIEW_OUTBOUND_STOP1_TIME,
            TEXTVIEW_OUTBOUND_STOP2_TIME
    };

    private static TimerTask timerTaskStopForecastTimeout;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            StopForecastWidgetConfigureActivity.deleteTitlePref(context, appWidgetIds[i]);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (intent.getAction().equals(WIDGET_CLICK_STOP_NAME)) {
            views.setTextViewText(R.id.textview_inbound_stop1_name, "Stop name clicked.");
            updateAppWidget();
        }

        if (intent.getAction().equals(WIDGET_CLICK_STOP_FORECAST)) {
            stopForecastTimeout(0, 5000);
            loadStopForecast(selectedStopName);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        currAppWidgetId = appWidgetId;
        currAppWidgetManager = appWidgetManager;

        res = context.getResources();

        // Construct the RemoteViews object
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

        // Set the selected stop as the stop chosen from the widget configuration Activity.
        selectedStopName = StopForecastWidgetConfigureActivity.loadTitlePref(context, appWidgetId);
        views.setTextViewText(R.id.textview_stop_name, selectedStopName);

        // Set up 30-second timeout for stop forecast.
        stopForecastTimeout(0, 5000);

        if (selectedStopName != null)
            loadStopForecast(selectedStopName);

        // Instruct the widget manager to update the widget.
        updateAppWidget();
    }

    static void updateAppWidget() {
        currAppWidgetManager.updateAppWidget(currAppWidgetId, views);
    }

    /**
     * Set up timeout for stop forecast, which clears the stop forecast and displays a holding
     * message after a set period.
     * This is a necessary evil due to their currently being no way for a widget to know when it
     * is "active" or "visible to the user". This implementation is in order to not hammer the
     * battery and network.
     * @param delayTimeMillis The delay (ms) before starting the timer.
     * @param timeoutTimeMillis The period (ms) after which the stop forecast should be considered
     *                          expired and cleared.
     */
    static void stopForecastTimeout(int delayTimeMillis, int timeoutTimeMillis) {
        if (timerTaskStopForecastTimeout != null)
            timerTaskStopForecastTimeout.cancel();

        timerTaskStopForecastTimeout = new TimerTask() {
            @Override
            public void run() {
                clearStopForecast();
                views.setTextViewText(TEXTVIEW_INBOUND_STOP1_NAME, res.getString(R.string.tap_to_load_times));

                updateAppWidget();
            }
        };

        new Timer().schedule(timerTaskStopForecastTimeout, delayTimeMillis, timeoutTimeMillis);
    }

    static void loadStopForecast(String stopName) {
        new FetchLuasTimes().execute(stopName);
    }

    /**
     * Make progress bar appear or disappear.
     * @param loading Whether or not progress circle should spin.
     */
    static void setIsLoading(boolean loading) {
        views.setProgressBar(PROGRESSBAR, 0, 0, loading);

        updateAppWidget();
    }

    /**
     * Clear the stop forecast displayed in the current tab.
     */
    static void clearStopForecast() {
        /*
         * Clear the stop forecast.
         */
        for (int i = 0; i < 2; i++) {
            views.setTextViewText(textViewInboundStopNames[i], "");
            views.setTextViewText(textViewInboundStopTimes[i], "");

            views.setTextViewText(textViewOutboundStopNames[i], "");
            views.setTextViewText(textViewOutboundStopTimes[i], "");
        }
    }

    static class FetchLuasTimes extends AsyncTask<String, Void, StopForecast> {

        private final String LOG_TAG = FetchLuasTimes.class.getSimpleName();

        private final String GAEILGE = "ga";

        private final String localeDefault;

        Map<String, String> stopCodes;

        public FetchLuasTimes() {
            localeDefault = Locale.getDefault().toString();
            Log.i(LOG_TAG, "Default locale: " + localeDefault);

            /*
             * If the user's default locale is set to Irish (Gaeilge), build a Map
             * of Irish stop names:codes.
             * If not, default to English.
             */
            if (localeDefault.startsWith(GAEILGE)) {
                stopCodes = new HashMap<String, String>() {
                    {
                        // Red Line
                        put("Iosta na Rinne", "TPT");
                        put("Duga Spencer", "SDK");
                        put("Cearnóg an Mhéara - CNÉ", "MYS");
                        put("Duga Sheoirse", "GDK");
                        put("Conghaile", "CON");
                        put("Busáras", "BUS");
                        put("Sráid na Mainistreach", "ABB");
                        put("Jervis", "JER");
                        put("Na Ceithre Cúirteanna", "FOU");
                        put("Margadh na Feirme", "SMI");
                        put("Árd-Mhúsaem", "MUS");
                        put("Heuston", "HEU");
                        put("Ospidéal San Séamas", "JAM");
                        put("Fatima", "FAT");
                        put("Rialto", "RIA");
                        put("Bóthar na Siúire", "SUI");
                        put("An Droichead Órga", "GOL");
                        put("Droimeanach", "DRI");
                        put("An Capall Dubh", "BLA");
                        put("An Cloigín Gorm", "BLU");
                        put("An Chill Mhór", "KYL");
                        put("An Bhó Dhearg", "RED");
                        put("Coill an Rí", "KIN");
                        put("Belgard", "BEL");
                        put("Baile an Chócaigh", "COO");
                        put("Ospidéal Thamhlachta", "HOS");
                        put("Tamhlacht", "TAL");
                        put("Fothar Chardain", "FET");
                        put("Baile an tSíbrigh", "CVN");
                        put("Campas Gnó Iarthar na Cathrach", "CIT");
                        put("Baile Uí Fhoirtcheirn", "FOR");
                        put("Teach Sagard", "SAG");

                        // Green Line
                        put("Faiche Stiabhna", "STS");
                        put("Sráid Fhearchair", "HAR");
                        put("Charlemont", "CHA");
                        put("Raghnallach", "RAN");
                        put("Coill na Feá", "BEE");
                        put("Cowper", "COW");
                        put("Baile an Mhuilinn", "MIL");
                        put("Na Glasáin", "WIN");
                        put("Dún Droma", "DUN");
                        put("Baile Amhlaoibh", "BAL");
                        put("Cill Mochuda", "KIL");
                        put("Stigh Lorgan", "STI");
                        put("Áth an Ghainimh", "SAN");
                        put("An Pháirc Láir", "CPK");
                        put("Gleann an Chairn", "GLE");
                        put("An Eachrais", "GAL");
                        put("Gleann Bhaile na Lobhar", "LEO");
                        put("Coill Bhaile Uí Ógáin", "BAW");
                        put("Carraig Mhaighin", "CCK");
                        put("Baile an Locháin", "LAU");
                        put("Coill na Silíní", "CHE");
                        put("Gleann Bhríde", "BRI");
                    }
                };
            } else {
                stopCodes = new HashMap<String, String>() {
                    {
                        // Red Line
                        put("The Point", "TPT");
                        put("Spencer Dock", "SDK");
                        put("Mayor Square - NCI", "MYS");
                        put("George's Dock", "GDK");
                        put("Connolly", "CON");
                        put("Busáras", "BUS");
                        put("Abbey Street", "ABB");
                        put("Jervis", "JER");
                        put("Four Courts", "FOU");
                        put("Smithfield", "SMI");
                        put("Museum", "MUS");
                        put("Heuston", "HEU");
                        put("James's", "JAM");
                        put("Fatima", "FAT");
                        put("Rialto", "RIA");
                        put("Suir Road", "SUI");
                        put("Goldenbridge", "GOL");
                        put("Drimnagh", "DRI");
                        put("Blackhorse", "BLA");
                        put("Bluebell", "BLU");
                        put("Kylemore", "KYL");
                        put("Red Cow", "RED");
                        put("Kingswood", "KIN");
                        put("Belgard", "BEL");
                        put("Cookstown", "COO");
                        put("Hospital", "HOS");
                        put("Tallaght", "TAL");
                        put("Fettercairn", "FET");
                        put("Cheeverstown", "CVN");
                        put("Citywest Campus", "CIT");
                        put("Fortunestown", "FOR");
                        put("Saggart", "SAG");

                        // Green Line
                        put("St. Stephen's Green", "STS");
                        put("Harcourt", "HAR");
                        put("Charlemont", "CHA");
                        put("Ranelagh", "RAN");
                        put("Beechwood", "BEE");
                        put("Cowper", "COW");
                        put("Milltown", "MIL");
                        put("Windy Arbour", "WIN");
                        put("Dundrum", "DUN");
                        put("Balally", "BAL");
                        put("Kilmacud", "KIL");
                        put("Stillorgan", "STI");
                        put("Sandyford", "SAN");
                        put("Central Park", "CPK");
                        put("Glencairn", "GLE");
                        put("The Gallops", "GAL");
                        put("Leopardstown Valley", "LEO");
                        put("Ballyogan Wood", "BAW");
                        put("Carrickmines", "CCK");
                        put("Laughanstown", "LAU");
                        put("Cherrywood", "CHE");
                        put("Brides Glen", "BRI");
                    }
                };
            }
        }

        @Override
        protected StopForecast doInBackground(String... params) {
            /*
             * Start by clearing the currently-displayed stop forecast.
             */
//            clearStopForecast();

            if (params.length == 0)
                return null;

            HttpURLConnection httpUrlConnection = null;
            BufferedReader reader = null;

            String luasTimesJson = null;

            // HTTP parameters to pass to the API.
            String action = "times";
            String station = params[0];
            String stationCode = stopCodes.get(station);

            try {
                setIsLoading(true);

                // Build the API URL.
                final String BASE_URL = "https://api.thecosmicfrog.org/cgi-bin/luas-api.php?";
                final String PARAM_ACTION = "action";
                final String PARAM_STATION = "station";

                Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter(PARAM_ACTION, action)
                        .appendQueryParameter(PARAM_STATION, stationCode)
                        .build();

                URL url = new URL(builtUri.toString());

                httpUrlConnection = (HttpURLConnection) url.openConnection();
                httpUrlConnection.setRequestMethod("GET");
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
//                setIsLoading(false);

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
                    return getLuasDataFromJson(luasTimesJson);
                } catch (JSONException je) {
                    je.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(StopForecast sf) {
            updateStopForecast(sf);
            setIsLoading(false);

            updateAppWidget();
        }

        private void updateStopForecast(StopForecast sf) {
            EnglishGaeilgeMap englishGaeilgeMap = new EnglishGaeilgeMap();

            // If a valid stop forecast exists...
            if (sf != null) {
                /*
                 * Pull in all trams from the StopForecast, but only display up to three inbound
                 * and outbound trams.
                 */
                if (sf.getInboundTrams() != null) {
                    if (sf.getInboundTrams().size() == 0) {
                        views.setTextViewText(TEXTVIEW_INBOUND_STOP1_NAME, "No trams forecast");
                    } else {
                        String inboundTram;

                        for (int i = 0; i < sf.getInboundTrams().size(); i++) {
                            if (i < 2) {
                                if (localeDefault.startsWith(GAEILGE)) {
                                    inboundTram = englishGaeilgeMap.get(sf.getInboundTrams().get(i).getDestination());
                                } else {
                                    inboundTram = sf.getInboundTrams().get(i).getDestination();
                                }

                                views.setTextViewText(textViewInboundStopNames[i], inboundTram);

                                if (sf.getInboundTrams()
                                        .get(i).getDueMinutes().equalsIgnoreCase("DUE")) {
                                    String dueMinutes;

                                    if (localeDefault.startsWith(GAEILGE)) {
                                        dueMinutes = englishGaeilgeMap.get("DUE");
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
                        views.setTextViewText(TEXTVIEW_OUTBOUND_STOP1_NAME, "No trams forecast");
                    } else {
                        String outboundTram;

                        for (int i = 0; i < sf.getOutboundTrams().size(); i++) {
                            if (i < 2) {
                                if (localeDefault.startsWith(GAEILGE)) {
                                    outboundTram = englishGaeilgeMap.get(sf.getOutboundTrams().get(i).getDestination());
                                } else {
                                    outboundTram = sf.getOutboundTrams().get(i).getDestination();
                                }

                                views.setTextViewText(textViewOutboundStopNames[i], outboundTram);

                                if (sf.getOutboundTrams()
                                        .get(i).getDueMinutes().equalsIgnoreCase("DUE")) {
                                    String dueMinutes;

                                    if (localeDefault.startsWith(GAEILGE)) {
                                        dueMinutes = englishGaeilgeMap.get("DUE");
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
                Log.v(LOG_TAG, "Else");
                /*
                 * If no stop forecast can be retrieved, set a generic error message and
                 * change the color of the message title box red.
                 */
//                textViewInboundStopNames =
//                        (TextView) rootView.findViewById(
//                                R.id.red_line_textview_message_title
//                        );
//                textViewInboundStopNames.setBackgroundResource(R.color.message_error);
//
//                textViewMessage =
//                        (TextView) rootView.findViewById(R.id.red_line_textview_message);
//                textViewMessage.setText(R.string.message_error);
            }
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy: constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private StopForecast getLuasDataFromJson(String forecastJsonStr)
                throws JSONException {

            StopForecast stopForecast = new StopForecast();

            // These are the names of the JSON objects that need to be extracted.
            final String LUAS_MESSAGE = "message";
            final String LUAS_TRAMS = "trams";
            final String LUAS_DESTINATION = "destination";
            final String LUAS_DIRECTION = "direction";
            final String LUAS_DUEMINUTES = "dueMinutes";

            JSONObject tramsJson = new JSONObject(forecastJsonStr);

            /*
             * If a message is returned from the server, add it to the StopForecast object.
             * Otherwise, set the message field to null.
             */
            if (tramsJson.has(LUAS_MESSAGE)) {
                stopForecast.setMessage(tramsJson.getString(LUAS_MESSAGE));
            } else {
                stopForecast.setMessage(null);
            }

            /*
             * If a list of trams is returned from the server, add it to the StopForecast object
             * as an array of both inbound and output trams.
             * Otherwise, set both fields to null.
             */
            if (tramsJson.has(LUAS_TRAMS)) {
                JSONArray tramsArray = tramsJson.getJSONArray(LUAS_TRAMS);

                Tram[] trams = new Tram[tramsArray.length()];

                for (int i = 0; i < tramsArray.length(); i++) {
                    String destination;
                    String direction;
                    String dueMinutes;

                    // Get the JSON object representing the trams.
                    JSONObject tramObject = tramsArray.getJSONObject(i);

                    destination = tramObject.getString(LUAS_DESTINATION);
                    direction = tramObject.getString(LUAS_DIRECTION);
                    dueMinutes = tramObject.getString(LUAS_DUEMINUTES);

                    trams[i] = new Tram(destination, direction, dueMinutes);

                    switch (trams[i].getDirection()) {
                        case "Inbound":
                            stopForecast.addInboundTram(trams[i]);

                            break;

                        case "Outbound":
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


