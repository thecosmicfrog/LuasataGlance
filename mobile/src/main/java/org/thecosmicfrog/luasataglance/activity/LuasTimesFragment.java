package org.thecosmicfrog.luasataglance.activity;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.object.StopForecast;
import org.thecosmicfrog.luasataglance.object.Tram;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LuasTimesFragment extends Fragment {

    private final String LOG_TAG = LuasTimesFragment.class.getSimpleName();

    private View rootView = null;

    private final String RED_LINE = "red_line";
    private final String GREEN_LINE = "green_line";

    private TabHost tabHost;
    private String currentTab;
    private ProgressBar progressBarRedLineLoadingCircle;
    private ProgressBar progressBarGreenLineLoadingCircle;
    private ArrayAdapter<CharSequence> redLineAdapterStop;
    private ArrayAdapter<CharSequence> greenLineAdapterStop;
    private Spinner redLineSpinnerStop;
    private Spinner greenLineSpinnerStop;
    private SwipeRefreshLayout redLineSwipeRefreshLayout;
    private SwipeRefreshLayout greenLineSwipeRefreshLayout;
    private TextView textViewMessageTitle;
    private TextView textViewMessage;
    private TextView[] textViewInboundStopNames;
    private TextView[] textViewInboundStopTimes;
    private TextView[] textViewOutboundStopNames;
    private TextView[] textViewOutboundStopTimes;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_main, container, false);

        initTabs();

        if (getActivity().getIntent().hasExtra("stopName")) {
            setTabAndSpinner();
        }

        return rootView;
    }

    /**
     * Initialise both tabs.
     */
    private void initTabs() {
        /*
         * Set up tabs.
         */
        tabHost = (TabHost) rootView.findViewById(R.id.tabHost);
        tabHost.setup();

        TabHost.TabSpec tabSpec = tabHost.newTabSpec(RED_LINE);
        tabSpec.setContent(R.id.tab_red_line);
        tabSpec.setIndicator("Red Line");
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec(GREEN_LINE);
        tabSpec.setContent(R.id.tab_green_line);
        tabSpec.setIndicator("Green Line");
        tabHost.addTab(tabSpec);

        /*
         * Set up Red Line tab.
         */
        progressBarRedLineLoadingCircle =
                (ProgressBar) rootView.findViewById(R.id.red_line_progressbar_loading_circle);
        setIsLoading(RED_LINE, false);

        redLineSpinnerStop = (Spinner) rootView.findViewById(R.id.red_line_spinner_stop);
        redLineAdapterStop = ArrayAdapter.createFromResource(
                getActivity(), R.array.red_line_array_stops, R.layout.spinner_stops
        );
        redLineAdapterStop.setDropDownViewResource(R.layout.spinner_stops);
        redLineSpinnerStop.setAdapter(redLineAdapterStop);

        redLineSpinnerStop.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentTab = tabHost.getCurrentTabTag();

                loadStopForecast(redLineSpinnerStop.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        redLineSwipeRefreshLayout =
                (SwipeRefreshLayout) rootView.findViewById(R.id.red_line_swiperefreshlayout);
        redLineSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // Start the refresh animation.
                        redLineSwipeRefreshLayout.setRefreshing(true);
                        loadStopForecast(redLineSpinnerStop.getSelectedItem().toString());
                    }
                });

        /*
         * Set up Green Line tab.
         */
        progressBarGreenLineLoadingCircle =
                (ProgressBar) rootView.findViewById(R.id.green_line_progressbar_loading_circle);
        setIsLoading(GREEN_LINE, false);

        greenLineSpinnerStop = (Spinner) rootView.findViewById(R.id.green_line_spinner_stop);

        greenLineAdapterStop = ArrayAdapter.createFromResource(
                getActivity(), R.array.green_line_array_stops, R.layout.spinner_stops
        );
        greenLineAdapterStop.setDropDownViewResource(R.layout.spinner_stops);
        greenLineSpinnerStop.setAdapter(greenLineAdapterStop);

        greenLineSpinnerStop.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentTab = tabHost.getCurrentTabTag();

                loadStopForecast(greenLineSpinnerStop.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        greenLineSwipeRefreshLayout =
                (SwipeRefreshLayout) rootView.findViewById(R.id.green_line_swiperefreshlayout);
        greenLineSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // Start the refresh animation.
                        greenLineSwipeRefreshLayout.setRefreshing(true);
                        loadStopForecast(greenLineSpinnerStop.getSelectedItem().toString());
                    }
                });
    }

    /**
     * Set the current tab and the position of the Spinner.
     */
    private void setTabAndSpinner() {
        String[] redLineArrayStops = getResources().getStringArray(R.array.red_line_array_stops);
        String[] greenLineArrayStops = getResources().getStringArray(R.array.green_line_array_stops);

        List<String> redLineListStops = Arrays.asList(redLineArrayStops);
        List<String> greenLineListStops = Arrays.asList(greenLineArrayStops);

        if (redLineListStops.contains(getActivity().getIntent().getStringExtra("stopName"))) {
            tabHost.setCurrentTab(0);
            redLineSpinnerStop.setSelection(
                    redLineAdapterStop
                            .getPosition(getActivity().getIntent().getStringExtra("stopName"))
            );
        } else if (greenLineListStops.contains(getActivity().getIntent().getStringExtra("stopName"))) {
            tabHost.setCurrentTab(1);
            greenLineSpinnerStop.setSelection(
                    greenLineAdapterStop
                            .getPosition(getActivity().getIntent().getStringExtra("stopName"))
            );
        }
    }

    public void loadStopForecast(String stopName) {
        new FetchLuasTimes().execute(stopName);
    }

    /**
     * Make progress circle spin or not spin.
     * Must run on UI thread as only this thread can change views. This is achieved using the
     * runOnUiThread() method. Parameters must be final due to Java scope restrictions.
     * @param line Name of tab in which progress circle should spin.
     * @param loading Whether or not progress circle should spin.
     */
    public void setIsLoading(final String line, final boolean loading) {
        /*
         * Only run if Fragment is attached to Activity. Without this check, the app is liable
         * to crash when the screen is rotated many times in a given period of time.
         */
        if (isAdded()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (line) {
                        case RED_LINE:
                            if (loading)
                                progressBarRedLineLoadingCircle.setVisibility(View.VISIBLE);
                            else
                                progressBarRedLineLoadingCircle.setVisibility(View.GONE);
                            break;
                        case GREEN_LINE:
                            if (loading)
                                progressBarGreenLineLoadingCircle.setVisibility(View.VISIBLE);
                            else
                                progressBarGreenLineLoadingCircle.setVisibility(View.GONE);
                            break;
                        default:
                            // If for some reason the line doesn't make sense.
                            Log.e(LOG_TAG, "Invalid line specified.");
                    }
                }
            });
        }
    }

    private void initStopForecast(String line) {
        switch (line) {
            case RED_LINE:
                textViewInboundStopNames = new TextView[] {
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_inbound_stop1_name),
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_inbound_stop2_name),
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_inbound_stop3_name),
                };

                textViewInboundStopTimes = new TextView[] {
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_inbound_stop1_time),
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_inbound_stop2_time),
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_inbound_stop3_time),
                };

                textViewOutboundStopNames = new TextView[] {
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_outbound_stop1_name),
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_outbound_stop2_name),
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_outbound_stop3_name),
                };

                textViewOutboundStopTimes = new TextView[] {
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_outbound_stop1_time),
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_outbound_stop2_time),
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_outbound_stop3_time),
                };

                break;

            case GREEN_LINE:
                textViewInboundStopNames = new TextView[] {
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_inbound_stop1_name),
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_inbound_stop2_name),
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_inbound_stop3_name),
                };

                textViewInboundStopTimes = new TextView[] {
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_inbound_stop1_time),
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_inbound_stop2_time),
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_inbound_stop3_time),
                };

                textViewOutboundStopNames = new TextView[] {
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_outbound_stop1_name),
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_outbound_stop2_name),
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_outbound_stop3_name),
                };

                textViewOutboundStopTimes = new TextView[] {
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_outbound_stop1_time),
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_outbound_stop2_time),
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_outbound_stop3_time),
                };

                break;

            default:
        }
    }

    /**
     * Clear the stop forecast displayed in the current tab.
     */
    public void clearStopForecast() {
        /*
         * Initialise the stop forecast based on which tab is selected.
         */
        switch (currentTab) {
            case RED_LINE:
                initStopForecast(RED_LINE);

                break;

            case GREEN_LINE:
                initStopForecast(GREEN_LINE);

                break;

            default:
                // If for some reason the line doesn't make sense.
                Log.e(LOG_TAG, "Invalid line specified.");
        }

        /*
         * Clear the stop forecast.
         */
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 3; i++) {
                    textViewInboundStopNames[i].setText("");
                    textViewInboundStopTimes[i].setText("");

                    textViewOutboundStopNames[i].setText("");
                    textViewOutboundStopTimes[i].setText("");
                }
            }
        });
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
            /*
             * Start by clearing the currently-displayed stop forecast.
             */
            clearStopForecast();

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
                setIsLoading(currentTab, true);

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
                setIsLoading(currentTab, false);

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
            /*
             * Only run if Fragment is attached to Activity. Without this check, the app is liable
             * to crash when the screen is rotated many times in a given period of time.
             */
            if (isAdded()) {
                /*
                 * Update UI elements specific to the tab currently selected.
                 */
                switch (currentTab) {
                    case RED_LINE:
                        // If a valid stop forecast exists...
                        if (sf != null) {
                            if (sf.getMessage() != null) {
                                /*
                                 * Set the status message from the server.
                                 */
                                textViewMessageTitle =
                                        (TextView) rootView.findViewById(
                                                R.id.red_line_textview_message_title
                                        );

                                /*
                                 * Change the color of the message title TextView depending on the status.
                                 */
                                if (sf.getMessage().equals(
                                        getResources().getString(R.string.message_success)))
                                    textViewMessageTitle.setBackgroundResource(R.color.message_success);
                                else
                                    textViewMessageTitle.setBackgroundResource(R.color.message_error);

                                textViewMessage =
                                        (TextView) rootView.findViewById(R.id.red_line_textview_message);
                                textViewMessage.setText(sf.getMessage());
                            }

                            /*
                             * Create arrays of TextView objects for each entry in the TableLayout.
                             */
                            initStopForecast(RED_LINE);

                            /*
                             * Pull in all trams from the StopForecast, but only display up to three inbound
                             * and outbound trams.
                             */
                            if (sf.getInboundTrams() != null) {
                                for (int i = 0; i < sf.getInboundTrams().size(); i++) {
                                    if (i < 3) {
                                        textViewInboundStopNames[i].setText(
                                                sf.getInboundTrams().get(i).getDestination()
                                        );

                                        if (sf.getInboundTrams()
                                                .get(i).getDueMinutes().equalsIgnoreCase("DUE")) {
                                            textViewInboundStopTimes[i].setText(
                                                    sf.getInboundTrams().get(i).getDueMinutes()
                                            );
                                        } else if (Integer.parseInt(sf.getInboundTrams()
                                                .get(i).getDueMinutes()) > 1) {
                                            textViewInboundStopTimes[i].setText(
                                                    sf.getInboundTrams().get(i).getDueMinutes() + " mins"
                                            );
                                        } else {
                                            textViewInboundStopTimes[i].setText(
                                                    sf.getInboundTrams().get(i).getDueMinutes() + " min"
                                            );
                                        }
                                    }
                                }
                            }

                            if (sf.getOutboundTrams() != null) {
                                for (int i = 0; i < sf.getOutboundTrams().size(); i++) {
                                    if (i < 3) {
                                        textViewOutboundStopNames[i].setText(
                                                sf.getOutboundTrams().get(i).getDestination()
                                        );

                                        if (sf.getOutboundTrams()
                                                .get(i).getDueMinutes().equalsIgnoreCase("DUE")) {
                                            textViewOutboundStopTimes[i].setText(
                                                    sf.getOutboundTrams().get(i).getDueMinutes()
                                            );
                                        } else if (Integer.parseInt(sf.getOutboundTrams()
                                                .get(i).getDueMinutes()) > 1) {
                                            textViewOutboundStopTimes[i].setText(
                                                    sf.getOutboundTrams().get(i).getDueMinutes() + " mins"
                                            );
                                        } else {
                                            textViewOutboundStopTimes[i].setText(
                                                    sf.getOutboundTrams().get(i).getDueMinutes() + " min"
                                            );
                                        }
                                    }
                                }
                            }
                        } else {
                            /*
                             * If no stop forecast can be retrieved, set a generic error message and
                             * change the color of the message title box red.
                             */
                            textViewMessageTitle =
                                    (TextView) rootView.findViewById(
                                            R.id.red_line_textview_message_title
                                    );
                            textViewMessageTitle.setBackgroundResource(R.color.message_error);

                            textViewMessage =
                                    (TextView) rootView.findViewById(R.id.red_line_textview_message);
                            textViewMessage.setText(R.string.message_error);
                        }

                        break;

                    case GREEN_LINE:
                        // If a valid stop forecast exists...
                        if (sf != null) {
                            if (sf.getMessage() != null) {
                                /*
                                 * Set the status message from the server.
                                 */
                                textViewMessageTitle =
                                        (TextView) rootView.findViewById(
                                                R.id.green_line_textview_message_title
                                        );

                                /*
                                 * Change the color of the message title TextView depending on the status.
                                 */
                                if (sf.getMessage().equals(
                                        getResources().getString(R.string.message_success)))
                                    textViewMessageTitle.setBackgroundResource(R.color.message_success);
                                else
                                    textViewMessageTitle.setBackgroundResource(R.color.message_error);

                                textViewMessage =
                                        (TextView) rootView.findViewById(R.id.green_line_textview_message);
                                textViewMessage.setText(sf.getMessage());
                            }

                            /*
                             * Create arrays of TextView objects for each entry in the TableLayout.
                             */
                            initStopForecast(GREEN_LINE);

                            /*
                             * Pull in all trams from the StopForecast, but only display up to three inbound
                             * and outbound trams.
                             */
                            if (sf.getInboundTrams() != null) {
                                for (int i = 0; i < sf.getInboundTrams().size(); i++) {
                                    if (i < 3) {
                                        textViewInboundStopNames[i].setText(
                                                sf.getInboundTrams().get(i).getDestination()
                                        );

                                        if (sf.getInboundTrams()
                                                .get(i).getDueMinutes().equalsIgnoreCase("DUE")) {
                                            textViewInboundStopTimes[i].setText(
                                                    sf.getInboundTrams().get(i).getDueMinutes()
                                            );
                                        } else if (Integer.parseInt(sf.getInboundTrams()
                                                .get(i).getDueMinutes()) > 1) {
                                            textViewInboundStopTimes[i].setText(
                                                    sf.getInboundTrams().get(i).getDueMinutes() + " mins"
                                            );
                                        } else {
                                            textViewInboundStopTimes[i].setText(
                                                    sf.getInboundTrams().get(i).getDueMinutes() + " min"
                                            );
                                        }
                                    }
                                }
                            }

                            if (sf.getOutboundTrams() != null) {
                                for (int i = 0; i < sf.getOutboundTrams().size(); i++) {
                                    if (i < 3) {
                                        textViewOutboundStopNames[i].setText(
                                                sf.getOutboundTrams().get(i).getDestination()
                                        );

                                        if (sf.getOutboundTrams()
                                                .get(i).getDueMinutes().equalsIgnoreCase("DUE")) {
                                            textViewOutboundStopTimes[i].setText(
                                                    sf.getOutboundTrams().get(i).getDueMinutes()
                                            );
                                        } else if (Integer.parseInt(sf.getOutboundTrams()
                                                .get(i).getDueMinutes()) > 1) {
                                            textViewOutboundStopTimes[i].setText(
                                                    sf.getOutboundTrams().get(i).getDueMinutes() + " mins"
                                            );
                                        } else {
                                            textViewOutboundStopTimes[i].setText(
                                                    sf.getOutboundTrams().get(i).getDueMinutes() + " min"
                                            );
                                        }
                                    }
                                }
                            }
                        } else {
                            /*
                             * If no stop forecast can be retrieved, set a generic error message and
                             * change the color of the message title box red.
                             */
                            textViewMessageTitle =
                                    (TextView) rootView.findViewById(
                                            R.id.green_line_textview_message_title
                                    );
                            textViewMessageTitle.setBackgroundResource(R.color.message_error);

                            textViewMessage =
                                    (TextView) rootView.findViewById(R.id.green_line_textview_message);
                            textViewMessage.setText(R.string.message_error);
                        }

                        break;

                    default:
                        // If for some reason the current selected tab doesn't make sense.
                        Log.e(LOG_TAG, "Unknown tab.");
                }

                // Stop the refresh animation.
                redLineSwipeRefreshLayout.setRefreshing(false);
                greenLineSwipeRefreshLayout.setRefreshing(false);
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
                stopForecast.setInboundTrams(null);
                stopForecast.setOutboundTrams(null);
            }

            return stopForecast;
        }
    }
}