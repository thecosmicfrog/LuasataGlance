package org.thecosmicfrog.luasataglance;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
    public void onDestroy() {
        super.onDestroy();
        Wearable.MessageApi.removeListener(googleApiClient, this);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/mobile")) {
            nodeId = messageEvent.getSourceNodeId();

            String stopName = Serializer.deserialize(messageEvent.getData()).toString();

            Log.v(LOG_TAG, "Stop Name: " + stopName);

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
            Log.v(LOG_TAG, "Success sent to: " + nodeId);
    }

    public class FetchLuasTimes extends AsyncTask<String, Void, StopForecast> {

        private final String LOG_TAG = FetchLuasTimes.class.getSimpleName();

        Map<String, String> stopCodes = new HashMap<String, String>() {
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

        @Override
        protected StopForecast doInBackground(String... params) {
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
                StringBuffer buffer = new StringBuffer();

                if (inputStream == null)
                    luasTimesJson = null;

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                }

                if (buffer.length() == 0)
                    luasTimesJson = null;

                luasTimesJson = buffer.toString();
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
                            Log.e(LOG_TAG, "Invalid direction: " + trams[i].getDirection());
                    }
                }
            } else {
                stopForecast.setInboundTrams(null);
                stopForecast.setOutboundTrams(null);
            }

            return stopForecast;
        }
    }
}
