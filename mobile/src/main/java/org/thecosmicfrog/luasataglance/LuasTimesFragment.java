package org.thecosmicfrog.luasataglance;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;

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

public class LuasTimesFragment extends Fragment {

    private final String LOG_TAG = LuasTimesFragment.class.getSimpleName();

    private View rootView = null;

    public LuasTimesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_main, container, false);

        /*
         * Set up tabs.
         */
        TabHost tabHost = (TabHost) rootView.findViewById(R.id.tabHost);
        tabHost.setup();

        TabHost.TabSpec tabSpec = tabHost.newTabSpec("red_line");
        tabSpec.setContent(R.id.tab_red_line);
        tabSpec.setIndicator("Red Line");
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("green_line");
        tabSpec.setContent(R.id.tab_green_line);
        tabSpec.setIndicator("Green Line");
        tabHost.addTab(tabSpec);

        final Spinner spinnerStop = (Spinner) rootView.findViewById(R.id.spinner_stop);
        final ArrayAdapter<CharSequence> adapterStop = ArrayAdapter.createFromResource(
                getActivity(), R.array.red_line_stops_array, android.R.layout.simple_spinner_item
        );
        adapterStop.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStop.setAdapter(adapterStop);

        spinnerStop.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                new FetchLuasTimes().execute(spinnerStop.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        return rootView;
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

            Log.v(LOG_TAG, "Station: " + station);
            Log.v(LOG_TAG, "Station code: " + stationCode);

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

                Log.v(LOG_TAG, "Luas times: " + luasTimesJson);
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
        protected void onPostExecute(StopForecast sf) {
            if (sf != null) {
                TextView messageTextView = (TextView) rootView.findViewById(R.id.textview_message);
                messageTextView.setText(sf.getMessage());

                ArrayAdapter<Tram> adapterInboundTrams = new ArrayAdapter<>(
                        getActivity(),
                        R.layout.list_item_trams,
                        R.id.textview_list_item_destination,
                        sf.getInboundTrams());

                ListView listViewInboundTrams = (ListView) rootView.findViewById(R.id.listview_inbound_trams);
                listViewInboundTrams.setAdapter(adapterInboundTrams);

                ArrayAdapter<Tram> adapterOutboundTrams = new ArrayAdapter<>(
                        getActivity(),
                        R.layout.list_item_trams,
                        R.id.textview_list_item_due_minutes,
                        sf.getOutboundTrams());

                ListView listViewOutboundTrams = (ListView) rootView.findViewById(R.id.listview_outbound_trams);
                listViewOutboundTrams.setAdapter(adapterOutboundTrams);
            } else {
                TextView messageTextView = (TextView) rootView.findViewById(R.id.textview_message);
                messageTextView.setText(R.string.message_error);
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

            String message = tramsJson.getString(LUAS_MESSAGE);
            stopForecast.setMessage(message);

            JSONArray tramsArray = tramsJson.getJSONArray(LUAS_TRAMS);


            Tram[] trams = new Tram[tramsArray.length()];

            for(int i = 0; i < tramsArray.length(); i++) {
                String destination;
                String direction;
                String dueMinutes;

                // Get the JSON object representing the trams.
                JSONObject tramObject = tramsArray.getJSONObject(i);

                destination = tramObject.getString(LUAS_DESTINATION);
                direction = tramObject.getString(LUAS_DIRECTION);
                dueMinutes = tramObject.getString(LUAS_DUEMINUTES);

                trams[i] = new Tram(destination, direction, dueMinutes);

                Log.v(LOG_TAG, trams[i].getDestination());
                Log.v(LOG_TAG, trams[i].getDirection());
                Log.v(LOG_TAG, trams[i].getDueMinutes());

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

            Log.v(LOG_TAG, "Message: " + stopForecast.getMessage());

            Log.v(LOG_TAG, "Inbound trams:");
            for (Tram t : stopForecast.getInboundTrams()) {
                Log.v(LOG_TAG, t.getDestination());
                Log.v(LOG_TAG, t.getDirection());
                Log.v(LOG_TAG, t.getDueMinutes());
            }

            Log.v(LOG_TAG, "Outbound trams:");
            for (Tram t : stopForecast.getOutboundTrams()) {
                Log.v(LOG_TAG, t.getDestination());
                Log.v(LOG_TAG, t.getDirection());
                Log.v(LOG_TAG, t.getDueMinutes());
            }

            return stopForecast;
        }
    }
}