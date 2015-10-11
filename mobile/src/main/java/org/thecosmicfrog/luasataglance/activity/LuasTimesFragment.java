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

package org.thecosmicfrog.luasataglance.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.api.ApiMethods;
import org.thecosmicfrog.luasataglance.api.ApiTimes;
import org.thecosmicfrog.luasataglance.object.EnglishGaeilgeMap;
import org.thecosmicfrog.luasataglance.object.NotifyTimesMap;
import org.thecosmicfrog.luasataglance.object.StopForecast;
import org.thecosmicfrog.luasataglance.object.StopNameIdMap;
import org.thecosmicfrog.luasataglance.object.Tram;
import org.thecosmicfrog.luasataglance.util.Preferences;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class LuasTimesFragment extends Fragment {

    private final String LOG_TAG = LuasTimesFragment.class.getSimpleName();
    private final String RED_LINE = "red_line";
    private final String GREEN_LINE = "green_line";
    private final String INBOUND = "inbound";
    private final String OUTBOUND = "outbound";
    private final String TUTORIAL_SWIPE_REFRESH = "swipe_refresh";
    private final String TUTORIAL_NOTIFICATIONS = "notifications";
    private final String TUTORIAL_FAVOURITES = "favourites";

    private static StopNameIdMap mapStopNameId;
    private static String localeDefault;

    private View rootView = null;
    private Menu menu;
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
    private CardView cardViewTutorialSwipeRefresh;
    private CardView cardViewTutorialNotifications;
    private CardView cardViewTutorialFavourites;
    private TableRow[] tableRowInboundStops;
    private TableRow[] tableRowOutboundStops;
    private TextView[] textViewInboundStopNames;
    private TextView[] textViewInboundStopTimes;
    private TextView[] textViewOutboundStopNames;
    private TextView[] textViewOutboundStopTimes;
    private TimerTask timerTaskReload;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_main, container, false);

        setHasOptionsMenu(true);

        // Initialise correct locale.
        localeDefault = Locale.getDefault().toString();

        // Instantiate a new StopNameIdMap.
        mapStopNameId = new StopNameIdMap(localeDefault);

        // Initialise user interface.
        initTabs();

        // If a Favourite stop brought us to this activity, load that stop's forecast.
        if (getActivity().getIntent().hasExtra("stopName")) {
            setTabAndSpinner();
        }

        // Keep track of the currently-focused tab.
        currentTab = tabHost.getCurrentTabTag();

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();

        // Stop the auto-reload TimerTask.
        timerTaskReload.cancel();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Display tutorial for SwipeRefreshLayout, if required.
        displayTutorial(TUTORIAL_SWIPE_REFRESH, true);

        /*
         * Reload stop forecast.
         * Induce 10 second delay if app is launching from cold start (timerTaskReload == null) in
         * order to prevent two HTTP requests in rapid succession.
         */
        if (timerTaskReload == null)
            autoReloadStopForecast(10000);
        else
            autoReloadStopForecast(0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Stop the auto-reload TimerTask.
        timerTaskReload.cancel();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Set the menu to a class variable for easy manipulation.
        this.menu = menu;

        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_news || id == R.id.action_news_alert) {
            startActivity(new Intent(
                            getContext(),
                            NewsActivity.class)
            );
        }

        if (id == R.id.action_about) {
            View dialogAbout = getActivity().getLayoutInflater().inflate(R.layout.dialog_about, null);

            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.app_name)
                    .setView(dialogAbout)
                    .show();
        }

        return super.onOptionsItemSelected(item);
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
         * Set appropriate colours for Red and Green Line tabs.
         */
        tabHost.getTabWidget().getChildAt(0).getBackground().setColorFilter(
                ContextCompat.getColor(getContext(), R.color.tab_red_line),
                PorterDuff.Mode.SRC_ATOP
        );

        tabHost.getTabWidget().getChildAt(1).getBackground().setColorFilter(
                ContextCompat.getColor(getContext(), R.color.tab_green_line),
                PorterDuff.Mode.SRC_ATOP
        );

        for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++) {
            TextView textViewTabText =
                    (TextView) tabHost.getTabWidget().getChildAt(i).findViewById(
                            android.R.id.title
                    );

            // Set the text color of the tabs to white.
            textViewTabText.setTextColor(
                    ContextCompat.getColor(getContext(),
                            android.R.color.white)
            );

            // Increase the default text size.
            textViewTabText.setTextSize(15.0f);
        }

        /*
         * When the tab changes, keep track of which is the currently-focused tab, then
         * initialise that tab to ensure variables are up to date.
         */
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                currentTab = tabHost.getCurrentTabTag();
                initStopForecast(currentTab);
            }
        });

        /*
         * Use a Floating Action Button (FAB) to open the Favourites Dialog.
         */
        FloatingActionButton fabFavourites =
                (FloatingActionButton) rootView.findViewById(R.id.floating_action_button);
        fabFavourites.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                displayTutorial(TUTORIAL_FAVOURITES, false);

                /*
                 * Open Favourites DialogFragment.
                 */
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FavouritesDialog favouritesDialog = new FavouritesDialog();
                favouritesDialog.show(fragmentManager, "dialog_favourites");
            }
        });

        /*
         * Set up Red Line tab.
         */
        progressBarRedLineLoadingCircle =
                (ProgressBar) rootView.findViewById(R.id.red_line_progressbar_loading_bar);
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
                loadStopForecast(redLineSpinnerStop.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        /*
         * Only display the tutorials in the Red Line tab for simplicity.
         */
        cardViewTutorialSwipeRefresh =
                (CardView) rootView.findViewById(R.id.cardview_tutorial_swipe_refresh);
        cardViewTutorialNotifications =
                (CardView) rootView.findViewById(R.id.cardview_tutorial_notifications);
        cardViewTutorialFavourites =
                (CardView) rootView.findViewById(R.id.cardview_tutorial_favourites);

        redLineSwipeRefreshLayout =
                (SwipeRefreshLayout) rootView.findViewById(R.id.red_line_swiperefreshlayout);
        redLineSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // Hide the SwipeRefreshLayout tutorial, if it is visible.
                        displayTutorial(TUTORIAL_SWIPE_REFRESH, false);

                        // Show the notifications tutorial.
                        displayTutorial(TUTORIAL_NOTIFICATIONS, true);

                        // Start by clearing the currently-displayed stop forecast.
                        clearStopForecast();

                        // Start the refresh animation.
                        redLineSwipeRefreshLayout.setRefreshing(true);
                        loadStopForecast(redLineSpinnerStop.getSelectedItem().toString());
                    }
                });

        /*
         * Set up Green Line tab.
         */
        progressBarGreenLineLoadingCircle =
                (ProgressBar) rootView.findViewById(R.id.green_line_progressbar_loading_bar);
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
                        // Start by clearing the currently-displayed stop forecast.
                        clearStopForecast();

                        // Start the refresh animation.
                        greenLineSwipeRefreshLayout.setRefreshing(true);
                        loadStopForecast(greenLineSpinnerStop.getSelectedItem().toString());
                    }
                });
    }

    /**
     * Determine if this is the first time the app has been launched and, if so, display a brief
     * tutorial on how to use a particular feature of the app.
     */
    private void displayTutorial(String tutorial, boolean shouldDisplay) {
        switch(tutorial) {
            case TUTORIAL_SWIPE_REFRESH:
                cardViewTutorialSwipeRefresh
                        = (CardView) rootView.findViewById(
                        R.id.cardview_tutorial_swipe_refresh
                );

                if (shouldDisplay) {
                    if (!Preferences.loadHasRunOnce(getContext(), tutorial)) {
                        Log.i(LOG_TAG, "First time launching. Displaying swipe refresh tutorial.");

                        cardViewTutorialSwipeRefresh.setVisibility(View.VISIBLE);

                        Preferences.saveHasRunOnce(getContext(), tutorial, true);
                    }
                } else {
                    cardViewTutorialSwipeRefresh.setVisibility(View.GONE);
                }

                break;

            case TUTORIAL_NOTIFICATIONS:
                cardViewTutorialNotifications
                        = (CardView) rootView.findViewById(
                        R.id.cardview_tutorial_notifications
                );

                if (shouldDisplay) {
                    if (!Preferences.loadHasRunOnce(getContext(), tutorial)) {
                        Log.i(LOG_TAG, "First time launching. Displaying notifications tutorial.");

                        cardViewTutorialNotifications.setVisibility(View.VISIBLE);
                    }
                } else {
                    cardViewTutorialNotifications.setVisibility(View.GONE);
                }

                break;

            case TUTORIAL_FAVOURITES:
                cardViewTutorialFavourites
                        = (CardView) rootView.findViewById(
                        R.id.cardview_tutorial_favourites
                );

                if (shouldDisplay) {
                    if (!Preferences.loadHasRunOnce(getContext(), tutorial)) {
                        Log.i(LOG_TAG, "First time launching. Displaying favourites tutorial.");

                        cardViewTutorialFavourites.setVisibility(View.VISIBLE);

                        Preferences.saveHasRunOnce(getContext(), tutorial, true);
                    }
                } else {
                    cardViewTutorialFavourites.setVisibility(View.GONE);
                }

                break;

            default:
                // If for some reason the specified tutorial doesn't make sense.
                Log.wtf(LOG_TAG, "Invalid tutorial specified.");
        }
    }

    /**
     * Set the current tab and the position of the Spinner.
     */
    private void setTabAndSpinner() {
        String[] redLineArrayStops = getResources().getStringArray(
                R.array.red_line_array_stops
        );
        String[] greenLineArrayStops = getResources().getStringArray(
                R.array.green_line_array_stops
        );

        List<String> redLineListStops = Arrays.asList(redLineArrayStops);
        List<String> greenLineListStops = Arrays.asList(greenLineArrayStops);

        String stopNameFromIntent = getActivity().getIntent().getStringExtra("stopName");

        if (redLineListStops.contains(stopNameFromIntent)) {
            tabHost.setCurrentTab(0);
            redLineSpinnerStop.setSelection(
                    redLineAdapterStop
                            .getPosition(stopNameFromIntent)
            );
        } else if (greenLineListStops.contains(stopNameFromIntent)) {
            tabHost.setCurrentTab(1);
            greenLineSpinnerStop.setSelection(
                    greenLineAdapterStop
                            .getPosition(stopNameFromIntent)
            );
        }
    }

    /**
     * Automatically reload the stop forecast after a defined period.
     * @param delayTimeMillis The delay (ms) before starting the timer.
     */
    private void autoReloadStopForecast(int delayTimeMillis) {
        final int RELOAD_TIME_MILLIS = 10000;

        timerTaskReload = new TimerTask() {
            @Override
            public void run() {
                /*
                 * Ensure the currently-focused tab is known to avoid loading
                 * a stop forecast for the wrong line.
                 */
                currentTab = tabHost.getCurrentTabTag();

                // Check Fragment is attached to Activity in order to avoid NullPointerExceptions.
                if (isAdded()){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            switch (currentTab) {
                                case RED_LINE:
                                    loadStopForecast(
                                            redLineSpinnerStop.getSelectedItem().toString()
                                    );

                                    break;

                                case GREEN_LINE:
                                    loadStopForecast(
                                            greenLineSpinnerStop.getSelectedItem().toString()
                                    );

                                    break;

                                default:
                                    // If for some reason the line doesn't make sense.
                                    Log.wtf(LOG_TAG, "Invalid line specified.");
                            }
                        }
                    });
                }
            }
        };

        // Schedule the auto-reload task to run.
        new Timer().schedule(timerTaskReload, delayTimeMillis, RELOAD_TIME_MILLIS);
    }

    /**
     * Load the stop forecast for a particular stop.
     * @param stopName The stop for which to load a stop forecast.
     */
    private void loadStopForecast(String stopName) {
        final String API_URL_PREFIX = "https://api";
        final String API_URL_POSTFIX = ".thecosmicfrog.org/cgi-bin";
        final String API_ACTION = "times";

        // Keep track of the currently-focused tab.
        currentTab = tabHost.getCurrentTabTag();

        setIsLoading(currentTab, true);

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
                // Check Fragment is attached to Activity in order to avoid NullPointerExceptions.
                if (isAdded()) {
                    // Then create a stop forecast with this data.
                    StopForecast stopForecast = createStopForecast(apiTimes);

                    clearStopForecast();

                    // Update the stop forecast.
                    updateStopForecast(stopForecast);

                    // Stop the refresh animations.
                    setIsLoading(currentTab, false);
                    redLineSwipeRefreshLayout.setRefreshing(false);
                    greenLineSwipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Log.e(LOG_TAG, "Failure during call to server.");

                /*
                 * If we get a message or a response from the server, there's likely an issue with
                 * the client request or the server's response itself.
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
                        // If for some reason the direction doesn't make sense.
                        Log.wtf(LOG_TAG, "Invalid direction: " + tram.getDirection());
                }
            }
        }

        stopForecast.setMessage(apiTimes.getMessage());

        return stopForecast;
    }

    /**
     * Make progress circle spin or not spin.
     * Must run on UI thread as only this thread can change views. This is achieved using the
     * runOnUiThread() method. Parameters must be final due to Java scope restrictions.
     * @param line Name of tab in which progress circle should spin.
     * @param loading Whether or not progress circle should spin.
     */
    private void setIsLoading(final String line, final boolean loading) {
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
                                progressBarRedLineLoadingCircle.setVisibility(View.INVISIBLE);

                            break;

                        case GREEN_LINE:
                            if (loading)
                                progressBarGreenLineLoadingCircle.setVisibility(View.VISIBLE);
                            else
                                progressBarGreenLineLoadingCircle.setVisibility(View.INVISIBLE);

                            break;

                        default:
                            // If for some reason the line doesn't make sense.
                            Log.wtf(LOG_TAG, "Invalid line specified.");
                    }
                }
            });
        }
    }

    /**
     * Initialise the arrays which hold a stop forecast.
     * @param line Line for which to initialise stop forecast arrays.
     */
    private void initStopForecast(String line) {
        switch (line) {
            case RED_LINE:
                tableRowInboundStops = new TableRow[] {
                        (TableRow) rootView.findViewById(
                                R.id.red_line_tablerow_inbound_stop1),
                        (TableRow) rootView.findViewById(
                                R.id.red_line_tablerow_inbound_stop2),
                        (TableRow) rootView.findViewById(
                                R.id.red_line_tablerow_inbound_stop3),
                        (TableRow) rootView.findViewById(
                                R.id.red_line_tablerow_inbound_stop4)
                };

                tableRowOutboundStops = new TableRow[] {
                        (TableRow) rootView.findViewById(
                                R.id.red_line_tablerow_outbound_stop1),
                        (TableRow) rootView.findViewById(
                                R.id.red_line_tablerow_outbound_stop2),
                        (TableRow) rootView.findViewById(
                                R.id.red_line_tablerow_outbound_stop3),
                        (TableRow) rootView.findViewById(
                                R.id.red_line_tablerow_outbound_stop4)
                };

                textViewInboundStopNames = new TextView[] {
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_inbound_stop1_name),
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_inbound_stop2_name),
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_inbound_stop3_name),
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_inbound_stop4_name)
                };

                textViewInboundStopTimes = new TextView[] {
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_inbound_stop1_time),
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_inbound_stop2_time),
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_inbound_stop3_time),
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_inbound_stop4_time)
                };

                textViewOutboundStopNames = new TextView[] {
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_outbound_stop1_name),
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_outbound_stop2_name),
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_outbound_stop3_name),
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_outbound_stop4_name)
                };

                textViewOutboundStopTimes = new TextView[] {
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_outbound_stop1_time),
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_outbound_stop2_time),
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_outbound_stop3_time),
                        (TextView) rootView.findViewById(
                                R.id.red_line_textview_outbound_stop4_time)
                };

                break;

            case GREEN_LINE:
                tableRowInboundStops = new TableRow[] {
                        (TableRow) rootView.findViewById(
                                R.id.green_line_tablerow_inbound_stop1),
                        (TableRow) rootView.findViewById(
                                R.id.green_line_tablerow_inbound_stop2),
                        (TableRow) rootView.findViewById(
                                R.id.green_line_tablerow_inbound_stop3),
                        (TableRow) rootView.findViewById(
                                R.id.green_line_tablerow_inbound_stop4)
                };

                tableRowOutboundStops = new TableRow[] {
                        (TableRow) rootView.findViewById(
                                R.id.green_line_tablerow_outbound_stop1),
                        (TableRow) rootView.findViewById(
                                R.id.green_line_tablerow_outbound_stop2),
                        (TableRow) rootView.findViewById(
                                R.id.green_line_tablerow_outbound_stop3),
                        (TableRow) rootView.findViewById(
                                R.id.green_line_tablerow_outbound_stop4)
                };

                textViewInboundStopNames = new TextView[] {
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_inbound_stop1_name),
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_inbound_stop2_name),
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_inbound_stop3_name),
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_inbound_stop4_name)
                };

                textViewInboundStopTimes = new TextView[] {
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_inbound_stop1_time),
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_inbound_stop2_time),
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_inbound_stop3_time),
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_inbound_stop4_time)
                };

                textViewOutboundStopNames = new TextView[] {
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_outbound_stop1_name),
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_outbound_stop2_name),
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_outbound_stop3_name),
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_outbound_stop4_name)
                };

                textViewOutboundStopTimes = new TextView[] {
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_outbound_stop1_time),
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_outbound_stop2_time),
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_outbound_stop3_time),
                        (TextView) rootView.findViewById(
                                R.id.green_line_textview_outbound_stop4_time)
                };

                break;

            default:
                // If for some reason the line doesn't make sense.
                Log.wtf(LOG_TAG, "Invalid line specified.");
        }

        initStopForecastOnClickListeners();
    }

    /**
     * Initialise OnClickListeners for a stop forecast.
     */
    private void initStopForecastOnClickListeners() {
        final String STOP_FORECAST = "stop_forecast";

        localeDefault = Locale.getDefault().toString();
        final NotifyTimesMap mapNotifyTimes = new NotifyTimesMap(localeDefault, STOP_FORECAST);

        for (int i = 0; i < tableRowInboundStops.length; i++) {
            final int index = i;

            tableRowInboundStops[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showNotifyTimeDialog(INBOUND, index, mapNotifyTimes);
                }
            });
        }

        for (int i = 0; i < tableRowOutboundStops.length; i++) {
            final int index = i;

            tableRowOutboundStops[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showNotifyTimeDialog(OUTBOUND, index, mapNotifyTimes);
                }
            });
        }
    }

    /**
     * Show dialog for choosing notification times.
     * @param direction Tram direction (inbound or outbound).
     * @param index Index representing which specific tram to notify for.
     * @param mapNotifyTimes Map of human-readable due times to machine-readable integers.
     */
    private void showNotifyTimeDialog(String direction, int index, NotifyTimesMap mapNotifyTimes) {
        localeDefault = Locale.getDefault().toString();
        String notifyStopTimeStr = "";

        switch (direction) {
            case INBOUND:
                notifyStopTimeStr = textViewInboundStopTimes[index].getText().toString();

                break;

            case OUTBOUND:
                notifyStopTimeStr = textViewOutboundStopTimes[index].getText().toString();

                break;

            default:
                // If for some reason the direction doesn't make sense.
                Log.wtf(LOG_TAG, "Invalid direction: " + direction);
        }

        if (notifyStopTimeStr.equals(""))
            return;

        if (notifyStopTimeStr.equalsIgnoreCase("DUE")
                || notifyStopTimeStr.equalsIgnoreCase("AM")
                || notifyStopTimeStr.equalsIgnoreCase("1 min")
                || notifyStopTimeStr.equalsIgnoreCase("1 nóim")
                || notifyStopTimeStr.equalsIgnoreCase("2 mins")
                || notifyStopTimeStr.equalsIgnoreCase("2 nóim")) {
            Toast.makeText(
                    getActivity(),
                    getResources().getString(R.string.cannot_schedule_notification),
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        /*
         * When the user opens the notification dialog as part of the tutorial, scroll back up to
         * the top so that the next tutorial is definitely visible. This should only ever run once.
         */
        if (!Preferences.loadHasRunOnce(getContext(), TUTORIAL_NOTIFICATIONS)) {
            ScrollView scrollView = (ScrollView) rootView.findViewById(R.id.red_line_scrollview);
            scrollView.setScrollY(0);
        }

        Preferences.saveHasRunOnce(getContext(), TUTORIAL_NOTIFICATIONS, true);

        // We're done with the notifications tutorial. Hide it.
        displayTutorial(TUTORIAL_NOTIFICATIONS, false);

        // Then, display the final tutorial.
        displayTutorial(TUTORIAL_FAVOURITES, true);

        Preferences.saveNotifyStopTimeExpected(
                getActivity(),
                mapNotifyTimes.get(notifyStopTimeStr)
        );

        new NotifyTimeDialog(getActivity()).show();
    }

    /**
     * Clear the stop forecast displayed in the current tab.
     */
    private void clearStopForecast() {
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
                Log.wtf(LOG_TAG, "Invalid line specified.");
        }

        /*
         * Clear the stop forecast.
         */
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 4; i++) {
                    textViewInboundStopNames[i].setText("");
                    textViewInboundStopTimes[i].setText("");

                    textViewOutboundStopNames[i].setText("");
                    textViewOutboundStopTimes[i].setText("");
                }
            }
        });
    }

    /**
     * Draw stop forecast to screen.
     * @param sf StopForecast object containing data for requested stop.
     */
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private void updateStopForecast(StopForecast sf) {
        final String GAEILGE = "ga";

        // Instantiate a new EnglishGaeilgeMap.
        EnglishGaeilgeMap mapEnglishGaeilge = new EnglishGaeilgeMap();

        TextView textViewMessageTitle;
        TextView textViewMessage;

        switch (currentTab) {
            case RED_LINE:
                // If a valid stop forecast exists...
                if (sf != null) {
                    String message;

                    textViewMessageTitle =
                            (TextView) rootView.findViewById(
                                    R.id.red_line_textview_message_title
                            );

                    textViewMessage =
                            (TextView) rootView.findViewById(
                                    R.id.red_line_textview_message
                            );

                    if (localeDefault.startsWith(GAEILGE)) {
                        message = getResources().getString(R.string.message_success);
                    } else {
                        message = sf.getMessage();
                    }

                    if (message.contains(
                            getResources().getString(R.string.message_success))) {
                        /*
                         * No error message on server. Change the message title TextView to
                         * green and set a default success message.
                         */
                        textViewMessageTitle.setBackgroundResource(R.color.message_success);
                        textViewMessage.setText(message);

                        /*
                         * Make the Alert menu item invisible.
                         */
                        MenuItem menuItemNewsAlert = menu.findItem(R.id.action_news_alert);
                        menuItemNewsAlert.setVisible(false);
                    } else {
                        /*
                         * Change the color of the message title TextView to red and set the
                         * error message from the server.
                         */
                        textViewMessageTitle.setBackgroundResource(R.color.message_error);
                        textViewMessage.setText(message);

                        /*
                         * Make the Alert menu item visible.
                         */
                        MenuItem menuItemNewsAlert = menu.findItem(R.id.action_news_alert);
                        menuItemNewsAlert.setVisible(true);
                    }

                    /*
                     * Create arrays of TextView objects for each entry in the TableLayout.
                     */
                    initStopForecast(RED_LINE);

                    /*
                     * Pull in all trams from the StopForecast, but only display up to three
                     * inbound and outbound trams.
                     */
                    if (sf.getInboundTrams() != null) {
                        if (sf.getInboundTrams().size() == 0) {
                            textViewInboundStopNames[0].setText(R.string.no_trams_forecast);
                        } else {
                            String inboundTram;

                            for (int i = 0; i < sf.getInboundTrams().size(); i++) {
                                if (i < 4) {
                                    if (localeDefault.startsWith(GAEILGE)) {
                                        inboundTram = mapEnglishGaeilge.get(
                                                sf.getInboundTrams()
                                                        .get(i)
                                                        .getDestination()
                                        );
                                    } else {
                                        inboundTram = sf.getInboundTrams()
                                                .get(i)
                                                .getDestination();
                                    }

                                    textViewInboundStopNames[i].setText(
                                            inboundTram
                                    );

                                    if (sf.getInboundTrams()
                                            .get(i).getDueMinutes().equalsIgnoreCase("DUE")) {
                                        String dueMinutes;

                                        if (localeDefault.startsWith(GAEILGE)) {
                                            dueMinutes = mapEnglishGaeilge.get("DUE");
                                        } else {
                                            dueMinutes = "DUE";
                                        }

                                        textViewInboundStopTimes[i].setText(
                                                dueMinutes
                                        );
                                    } else if (localeDefault.startsWith(GAEILGE)) {
                                        textViewInboundStopTimes[i].setText(
                                                sf.getInboundTrams()
                                                        .get(i)
                                                        .getDueMinutes() + " nóim"
                                        );
                                    } else if (Integer.parseInt(sf.getInboundTrams()
                                            .get(i).getDueMinutes()) > 1) {
                                        textViewInboundStopTimes[i].setText(
                                                sf.getInboundTrams()
                                                        .get(i)
                                                        .getDueMinutes() + " mins"
                                        );
                                    } else {
                                        textViewInboundStopTimes[i].setText(
                                                sf.getInboundTrams()
                                                        .get(i)
                                                        .getDueMinutes() + " min"
                                        );
                                    }
                                }
                            }
                        }
                    }

                    if (sf.getOutboundTrams() != null) {
                        if (sf.getOutboundTrams().size() == 0) {
                            textViewOutboundStopNames[0].setText(R.string.no_trams_forecast);
                        } else {
                            String outboundTram;

                            for (int i = 0; i < sf.getOutboundTrams().size(); i++) {
                                if (i < 4) {
                                    if (localeDefault.startsWith(GAEILGE)) {
                                        outboundTram = mapEnglishGaeilge.get(
                                                sf.getOutboundTrams()
                                                        .get(i)
                                                        .getDestination()
                                        );
                                    } else {
                                        outboundTram =
                                                sf.getOutboundTrams().get(i).getDestination();
                                    }

                                    textViewOutboundStopNames[i].setText(outboundTram);

                                    if (sf.getOutboundTrams()
                                            .get(i).getDueMinutes().equalsIgnoreCase("DUE")) {
                                        String dueMinutes;

                                        if (localeDefault.startsWith(GAEILGE)) {
                                            dueMinutes = mapEnglishGaeilge.get("DUE");
                                        } else {
                                            dueMinutes = "DUE";
                                        }

                                        textViewOutboundStopTimes[i].setText(
                                                dueMinutes
                                        );
                                    } else if (localeDefault.startsWith(GAEILGE)) {
                                        textViewOutboundStopTimes[i].setText(
                                                sf.getOutboundTrams()
                                                        .get(i)
                                                        .getDueMinutes() + " nóim"
                                        );
                                    } else if (Integer.parseInt(sf.getOutboundTrams()
                                            .get(i).getDueMinutes()) > 1) {
                                        textViewOutboundStopTimes[i].setText(
                                                sf.getOutboundTrams()
                                                        .get(i)
                                                        .getDueMinutes() + " mins"
                                        );
                                    } else {
                                        textViewOutboundStopTimes[i].setText(
                                                sf.getOutboundTrams()
                                                        .get(i)
                                                        .getDueMinutes() + " min"
                                        );
                                    }
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
                    String message;

                    textViewMessageTitle =
                            (TextView) rootView.findViewById(
                                    R.id.green_line_textview_message_title
                            );

                    textViewMessage =
                            (TextView) rootView.findViewById(
                                    R.id.green_line_textview_message
                            );

                    if (localeDefault.startsWith(GAEILGE)) {
                        message = getResources().getString(R.string.message_success);
                    } else {
                        message = sf.getMessage();
                    }

                    if (message.contains(
                            getResources().getString(R.string.message_success))) {
                        /*
                         * No error message on server. Change the message title TextView to
                         * green and set a default success message.
                         */
                        textViewMessageTitle.setBackgroundResource(R.color.message_success);
                        textViewMessage.setText(message);
                    } else {
                        /*
                         * Change the color of the message title TextView to red and set the
                         * error message from the server.
                         */
                        textViewMessageTitle.setBackgroundResource(R.color.message_error);
                        textViewMessage.setText(message);
                    }

                    /*
                     * Create arrays of TextView objects for each entry in the TableLayout.
                     */
                    initStopForecast(GREEN_LINE);

                    /*
                     * Pull in all trams from the StopForecast, but only display up to three
                     * inbound and outbound trams.
                     */
                    if (sf.getInboundTrams() != null) {
                        if (sf.getInboundTrams().size() == 0) {
                            textViewInboundStopNames[0].setText(R.string.no_trams_forecast);
                        } else {
                            String inboundTram;

                            for (int i = 0; i < sf.getInboundTrams().size(); i++) {
                                if (i < 4) {
                                    if (localeDefault.startsWith(GAEILGE)) {
                                        inboundTram = mapEnglishGaeilge.get(
                                                sf.getInboundTrams()
                                                        .get(i)
                                                        .getDestination());
                                    } else {
                                        inboundTram =
                                                sf.getInboundTrams()
                                                        .get(i)
                                                        .getDestination();
                                    }

                                    textViewInboundStopNames[i].setText(inboundTram);

                                    if (sf.getInboundTrams()
                                            .get(i).getDueMinutes().equalsIgnoreCase("DUE")) {
                                        String dueMinutes;

                                        if (localeDefault.startsWith(GAEILGE)) {
                                            dueMinutes = mapEnglishGaeilge.get("DUE");
                                        } else {
                                            dueMinutes = "DUE";
                                        }

                                        textViewInboundStopTimes[i].setText(
                                                dueMinutes
                                        );
                                    } else if (Integer.parseInt(sf.getInboundTrams()
                                            .get(i).getDueMinutes()) > 1) {
                                        textViewInboundStopTimes[i].setText(
                                                sf.getInboundTrams()
                                                        .get(i)
                                                        .getDueMinutes() + " mins"
                                        );
                                    } else {
                                        textViewInboundStopTimes[i].setText(
                                                sf.getInboundTrams()
                                                        .get(i)
                                                        .getDueMinutes() + " min"
                                        );
                                    }
                                }
                            }
                        }
                    }

                    if (sf.getOutboundTrams() != null) {
                        if (sf.getOutboundTrams().size() == 0) {
                            textViewOutboundStopNames[0].setText(R.string.no_trams_forecast);
                        } else {
                            String outboundTram;

                            for (int i = 0; i < sf.getOutboundTrams().size(); i++) {
                                if (i < 4) {
                                    if (localeDefault.startsWith(GAEILGE)) {
                                        outboundTram = mapEnglishGaeilge.get(
                                                sf.getOutboundTrams()
                                                        .get(i)
                                                        .getDestination()
                                        );
                                    } else {
                                        outboundTram =
                                                sf.getOutboundTrams()
                                                        .get(i)
                                                        .getDestination();
                                    }

                                    textViewOutboundStopNames[i].setText(outboundTram);

                                    if (sf.getOutboundTrams()
                                            .get(i).getDueMinutes().equalsIgnoreCase("DUE")) {
                                        String dueMinutes;

                                        if (localeDefault.startsWith(GAEILGE)) {
                                            dueMinutes = mapEnglishGaeilge.get("DUE");
                                        } else {
                                            dueMinutes = "DUE";
                                        }

                                        textViewOutboundStopTimes[i].setText(
                                                dueMinutes
                                        );
                                    } else if (Integer.parseInt(sf.getOutboundTrams()
                                            .get(i).getDueMinutes()) > 1) {
                                        textViewOutboundStopTimes[i].setText(
                                                sf.getOutboundTrams()
                                                        .get(i)
                                                        .getDueMinutes() + " mins"
                                        );
                                    } else {
                                        textViewOutboundStopTimes[i].setText(
                                                sf.getOutboundTrams()
                                                        .get(i)
                                                        .getDueMinutes() + " min"
                                        );
                                    }
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
                Log.wtf(LOG_TAG, "Unknown tab.");
        }
    }
}
