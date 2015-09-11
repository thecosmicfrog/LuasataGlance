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

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.thecosmicfrog.luasataglance.object.StopForecast;
import org.thecosmicfrog.luasataglance.object.Tram;
import org.thecosmicfrog.luasataglance.util.Auth;
import org.thecosmicfrog.luasataglance.util.Serializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WearMessageListenerService extends WearableListenerService {

    private final String LOG_TAG = WearMessageListenerService.class.getSimpleName();

    private static GoogleApiClient googleApiClient;

    private static final long CONNECTION_TIME_OUT_MS = 100;
    private static final String WEAR_PATH = "/wear";
    private String nodeId;

    @Override
    public void onCreate() {
        super.onCreate();

        googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Wearable.API)
                .build();

        Wearable.MessageApi.addListener(googleApiClient, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Wearable.MessageApi.removeListener(googleApiClient, this);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/mobile")) {
            nodeId = messageEvent.getSourceNodeId();

            String stopName = Serializer.deserialize(messageEvent.getData()).toString();

            if (stopName != null)
                fetchStopForecast(stopName);
        }
    }

    public void fetchStopForecast(String stopName) {
        new FetchLuasTimes().execute(stopName);
    }

    private void reply(final String path, final StopForecast sf) {
        googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Wearable.API)
                .build();

        if (googleApiClient != null &&
                !(googleApiClient.isConnected() || googleApiClient.isConnecting())) {
            googleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
        }

        MessageApi.SendMessageResult result =
                Wearable.MessageApi.sendMessage(
                        googleApiClient,
                        nodeId,
                        path,
                        Serializer.serialize(sf)
                ).await();

        if (result.getStatus().isSuccess())
            Log.i(LOG_TAG, "Return message sent to: " + nodeId);
    }

    public class FetchLuasTimes extends AsyncTask<String, Void, StopForecast> {

        private final String LOG_TAG = FetchLuasTimes.class.getSimpleName();

        Map<String, String> stopCodes = new HashMap<String, String>() {
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

        @Override
        protected StopForecast doInBackground(String... params) {
            if (params.length == 0)
                return null;

            HttpURLConnection httpUrlConnection = null;
            BufferedReader reader = null;

            String luasTimesJson = null;

            // HTTP parameters to pass to the API.
            String format = "json";
            String stopName = params[0];
            String stopId = stopCodes.get(stopName);

            try {
                /*
                 * Build the API URL.
                 */
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

                if (inputStream == null)
                    luasTimesJson = null;

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }

                if (stringBuilder.length() == 0)
                    luasTimesJson = null;

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

            try {
                return getLuasDataFromJson(luasTimesJson);
            } catch (JSONException je) {
                je.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(final StopForecast sf) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    reply(WEAR_PATH, sf);
                }
            }).start();
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
            final String LUAS_ERRORMESSAGE = "errormessage";
            final String LUAS_RESULTS = "results";
            final String LUAS_DESTINATION = "destination";
            final String LUAS_DIRECTION = "direction";
            final String LUAS_DUETIME = "duetime";

            JSONObject tramsJson = new JSONObject(forecastJsonStr);

            /*
             * If a message is returned from the server, add it to the StopForecast object.
             * Otherwise, set the message field to an empty String.
             */
            if (tramsJson.has(LUAS_ERRORMESSAGE)) {
                stopForecast.setErrorMessage(tramsJson.getString(LUAS_ERRORMESSAGE));
            } else {
                stopForecast.setErrorMessage("");
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
