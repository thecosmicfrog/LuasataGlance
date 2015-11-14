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
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TabHost;
import android.widget.TextView;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.api.ApiMethods;
import org.thecosmicfrog.luasataglance.api.ApiTimes;
import org.thecosmicfrog.luasataglance.object.EnglishGaeilgeMap;
import org.thecosmicfrog.luasataglance.object.StopForecast;
import org.thecosmicfrog.luasataglance.object.StopNameIdMap;
import org.thecosmicfrog.luasataglance.util.Preferences;
import org.thecosmicfrog.luasataglance.view.SpinnerCardView;
import org.thecosmicfrog.luasataglance.view.StatusCardView;
import org.thecosmicfrog.luasataglance.view.StopForecastCardView;
import org.thecosmicfrog.luasataglance.util.StopForecastUtil;

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
    private final String STOP_NAME = "stopName";
    private final String NOTIFY_STOP_NAME = "notifyStopName";
    private final String TUTORIAL_SWIPE_REFRESH = "swipe_refresh";
    private final String TUTORIAL_NOTIFICATIONS = "notifications";
    private final String TUTORIAL_FAVOURITES = "favourites";

    private static StopNameIdMap mapStopNameId;
    private static String localeDefault;

    private View rootView = null;
    private Menu menu;
    private TabHost tabHost;
    private String currentTab;
    private SpinnerCardView redLineSpinnerCardView;
    private SpinnerCardView greenLineSpinnerCardView;
    private SwipeRefreshLayout redLineSwipeRefreshLayout;
    private SwipeRefreshLayout greenLineSwipeRefreshLayout;
    private StatusCardView redLineStatusCardView;
    private StatusCardView greenLineStatusCardView;
    private StopForecastCardView redLineInboundStopForecastCardView;
    private StopForecastCardView redLineOutboundStopForecastCardView;
    private StopForecastCardView greenLineInboundStopForecastCardView;
    private StopForecastCardView greenLineOutboundStopForecastCardView;
    private TimerTask timerTaskReload;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_luas_times, container, false);

        setHasOptionsMenu(true);

        // Initialise correct locale.
        localeDefault = Locale.getDefault().toString();

        // Instantiate a new StopNameIdMap.
        mapStopNameId = new StopNameIdMap(localeDefault);

        // Initialise user interface.
        initTabs();

        /*
         * If a Favourite stop brought us to this Activity, load that stop's forecast.
         */
        if (getActivity().getIntent().hasExtra(STOP_NAME)) {
            String stopName = getActivity().getIntent().getStringExtra(STOP_NAME);

            setTabAndSpinner(stopName);
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

        /*
         * If a tapped notification brought us to this Activity, load the forecast for the stop
         * sent with that Intent.
         */
        if (getActivity().getIntent().hasExtra(NOTIFY_STOP_NAME))
            setTabAndSpinner(getActivity().getIntent().getStringExtra(NOTIFY_STOP_NAME));

        // Display tutorial for SwipeRefreshLayout, if required.
        StopForecastUtil.displayTutorial(rootView, TUTORIAL_SWIPE_REFRESH, true);

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
        final String NEWS_TYPE = "newsType";
        final String NEWS_TYPE_LUAS_NEWS = "luasNews";
        final String NEWS_TYPE_TRAVEL_UPDATES = "travelUpdates";

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_news) {
            startActivity(
                    new Intent(
                            getContext(),
                            NewsActivity.class
                    ).putExtra(NEWS_TYPE, NEWS_TYPE_LUAS_NEWS)
            );
        }

        if (id == R.id.action_news_alert) {
            startActivity(
                    new Intent(
                            getContext(),
                            NewsActivity.class
                    ).putExtra(NEWS_TYPE, NEWS_TYPE_TRAVEL_UPDATES)
            );
        }

        if (id == R.id.action_about) {
            View dialogAbout =
                    getActivity().getLayoutInflater().inflate(R.layout.dialog_about, null);

            new AlertDialog.Builder(getContext())
                    .setView(dialogAbout)
                    .show();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Initialise both tabs.
     */
    private void initTabs() {
        /********************************
         * BEGIN GENERAL FRAGMENT SETUP *
         ********************************/
        /*
         * Set up TabHost.
         */
        tabHost = (TabHost) rootView.findViewById(R.id.tabHost);
        tabHost.setup();

        TabHost.TabSpec tabSpec = tabHost.newTabSpec(RED_LINE);
        tabSpec.setContent(R.id.tab_red_line);
        tabSpec.setIndicator(getResources().getString(R.string.tab_red_line));
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec(GREEN_LINE);
        tabSpec.setContent(R.id.tab_green_line);
        tabSpec.setIndicator(getResources().getString(R.string.tab_green_line));
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

                StopForecastUtil.clearLineStopForecast(rootView, currentTab);
                loadStopForecast(Preferences.loadSelectedStopName(getContext(), currentTab));
            }
        });

        /*
         * Use a Floating Action Button (FAB) to open the Favourites Dialog.
         */
        FloatingActionButton fabFavourites =
                (FloatingActionButton) rootView.findViewById(R.id.fab_favourites);
        fabFavourites.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                StopForecastUtil.displayTutorial(rootView, TUTORIAL_FAVOURITES, false);

                /*
                 * Open Favourites DialogFragment.
                 */
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FavouritesDialog favouritesDialog = new FavouritesDialog();
                favouritesDialog.show(fragmentManager, "dialog_favourites");
            }
        });

        /****************************
         * BEGIN RED LINE TAB SETUP *
         ****************************/
        StopForecastUtil.setIsLoading(this, rootView, RED_LINE, false);

        /*
         * Set up Spinner and onItemSelectedListener.
         */
        redLineSpinnerCardView =
                (SpinnerCardView) rootView.findViewById(R.id.red_line_spinner_card_view);
        redLineSpinnerCardView.setLine(RED_LINE);

        redLineSpinnerCardView.getSpinnerStops().setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position,
                                               long id) {
                        /*
                         * Get the stop name from the current position of the Spinner, save it to
                         * SharedPreferences, then load a stop forecast with it.
                         */
                        String selectedStopName =
                                redLineSpinnerCardView
                                        .getSpinnerStops().getItemAtPosition(position).toString();

                        Preferences.saveSelectedStopName(
                                getContext(),
                                RED_LINE,
                                selectedStopName
                        );

                        loadStopForecast(selectedStopName);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

        /*
         * Set up Status CardView.
         */
        redLineStatusCardView =
                (StatusCardView) rootView.findViewById(R.id.red_line_statuscardview);

        /*
         * Set up SwipeRefreshLayout.
         */
        redLineSwipeRefreshLayout =
                (SwipeRefreshLayout) rootView.findViewById(R.id.red_line_swiperefreshlayout);
        redLineSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // Hide the SwipeRefreshLayout tutorial, if it is visible.
                        StopForecastUtil.displayTutorial(rootView, TUTORIAL_SWIPE_REFRESH, false);

                        // Show the notifications tutorial.
                        StopForecastUtil.displayTutorial(rootView, TUTORIAL_NOTIFICATIONS, true);

                        // Start by clearing the currently-displayed stop forecast.
                        StopForecastUtil.clearLineStopForecast(rootView, RED_LINE);

                        // Start the refresh animation.
                        redLineSwipeRefreshLayout.setRefreshing(true);
                        loadStopForecast(
                                Preferences.loadSelectedStopName(getContext(), RED_LINE)
                        );
                    }
                }
        );

        /*
         * Set up stop forecast CardViews.
         */
        redLineInboundStopForecastCardView =
                (StopForecastCardView) rootView.findViewById(
                        R.id.red_line_inbound_stopforecastcardview
                );
        redLineInboundStopForecastCardView.setStopForecastDirection(
                getResources().getString(R.string.inbound)
        );

        redLineOutboundStopForecastCardView =
                (StopForecastCardView) rootView.findViewById(
                        R.id.red_line_outbound_stopforecastcardview
                );
        redLineOutboundStopForecastCardView.setStopForecastDirection(
                getResources().getString(R.string.outbound)
        );

        /******************************
         * BEGIN GREEN LINE TAB SETUP *
         ******************************/
        /*
         * Set up Spinner and onItemSelectedListener.
         */
        greenLineSpinnerCardView =
                (SpinnerCardView) rootView.findViewById(R.id.green_line_spinner_card_view);
        greenLineSpinnerCardView.setLine(GREEN_LINE);

        greenLineSpinnerCardView.getSpinnerStops().setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position,
                                               long id) {
                        /*
                         * Get the stop name from the current position of the Spinner, save it to
                         * SharedPreferences, then load a stop forecast with it.
                         */
                        String selectedStopName =
                                greenLineSpinnerCardView
                                        .getSpinnerStops().getItemAtPosition(position).toString();

                        Preferences.saveSelectedStopName(
                                getContext(),
                                GREEN_LINE,
                                selectedStopName
                        );

                        loadStopForecast(selectedStopName);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

        /*
         * Set up Status CardView.
         */
        greenLineStatusCardView =
                (StatusCardView) rootView.findViewById(R.id.green_line_statuscardview);

        /*
         * Set up SwipeRefreshLayout.
         */
        greenLineSwipeRefreshLayout =
                (SwipeRefreshLayout) rootView.findViewById(R.id.green_line_swiperefreshlayout);
        greenLineSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // Start by clearing the currently-displayed stop forecast.
                        StopForecastUtil.clearLineStopForecast(rootView, GREEN_LINE);

                        // Start the refresh animation.
                        greenLineSwipeRefreshLayout.setRefreshing(true);
                        loadStopForecast(
                                Preferences.loadSelectedStopName(getContext(), GREEN_LINE)
                        );
                    }
                });

        /*
         * Set up stop forecast CardViews.
         */
        greenLineInboundStopForecastCardView =
                (StopForecastCardView) rootView.findViewById(
                        R.id.green_line_inbound_stopforecastcardview
                );
        greenLineInboundStopForecastCardView.setStopForecastDirection(
                getResources().getString(R.string.inbound)
        );

        greenLineOutboundStopForecastCardView =
                (StopForecastCardView) rootView.findViewById(
                        R.id.green_line_outbound_stopforecastcardview
                );
        greenLineOutboundStopForecastCardView.setStopForecastDirection(
                getResources().getString(R.string.outbound)
        );

        /*
         * Set up onClickListeners for stop forecasts in both tabs.
         */
        StopForecastUtil.initStopForecastOnClickListeners(rootView);
    }

    /**
     * Set the current tab and the position of the Spinner.
     */
    private void setTabAndSpinner(String stopName) {
        String[] redLineArrayStops = getResources().getStringArray(
                R.array.red_line_array_stops
        );
        String[] greenLineArrayStops = getResources().getStringArray(
                R.array.green_line_array_stops
        );

        List<String> redLineListStops = Arrays.asList(redLineArrayStops);
        List<String> greenLineListStops = Arrays.asList(greenLineArrayStops);

        if (redLineListStops.contains(stopName)) {
            tabHost.setCurrentTab(0);

            redLineSpinnerCardView.setSelection(stopName);
        } else if (greenLineListStops.contains(stopName)) {
            tabHost.setCurrentTab(1);

            greenLineSpinnerCardView.setSelection(stopName);
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
                                            Preferences.loadSelectedStopName(
                                                    getContext(),
                                                    RED_LINE
                                            )
                                    );

                                    break;

                                case GREEN_LINE:
                                    loadStopForecast(
                                            Preferences.loadSelectedStopName(
                                                    getContext(),
                                                    GREEN_LINE
                                            )
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
        final Fragment fragment = this;
        final String API_URL_PREFIX = "https://api";
        final String API_URL_POSTFIX = ".thecosmicfrog.org/cgi-bin";
        final String API_ACTION = "times";

        // Keep track of the currently-focused tab.
        currentTab = tabHost.getCurrentTabTag();

        StopForecastUtil.setIsLoading(this, rootView, currentTab, true);

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
                    StopForecast stopForecast = StopForecastUtil.createStopForecast(apiTimes);

                    StopForecastUtil.clearLineStopForecast(rootView, currentTab);

                    // Update the stop forecast.
                    updateStopForecast(stopForecast);

                    // Stop the refresh animations.
                    StopForecastUtil.setIsLoading(fragment, rootView, currentTab, false);
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
     * Draw stop forecast to screen.
     * @param stopForecast StopForecast object containing data for requested stop.
     */
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private void updateStopForecast(StopForecast stopForecast) {
        final String GAEILGE = "ga";
        final int ONE = 1;
        final int MANY = 2;
        final String DUE = "DUE";

        EnglishGaeilgeMap mapEnglishGaeilge = new EnglishGaeilgeMap();
        String min = " " + getResources().getString(R.string.min);
        String mins = " " + getResources().getString(R.string.mins);
        String minOrMins;

        switch (currentTab) {
            case RED_LINE:
                // If a valid stop forecast exists...
                if (stopForecast != null) {
                    String status;

                    if (localeDefault.startsWith(GAEILGE)) {
                        status = getResources().getString(R.string.message_success);
                    } else {
                        status = stopForecast.getMessage();
                    }

                    if (status.contains(
                            getResources().getString(R.string.message_success))) {
                        /*
                         * No error message on server. Change the message title TextView to
                         * green and set a default success message.
                         */
                        redLineStatusCardView.setStatus(status);
                        redLineStatusCardView.setStatusColor(R.color.message_success);

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
                        redLineStatusCardView.setStatus(status);
                        redLineStatusCardView.setStatusColor(R.color.message_error);

                        /*
                         * Make the Alert menu item visible.
                         */
                        MenuItem menuItemNewsAlert = menu.findItem(R.id.action_news_alert);
                        menuItemNewsAlert.setVisible(true);
                    }

                    /*
                     * Pull in all trams from the StopForecast, but only display up to three
                     * inbound and outbound trams.
                     */
                    if (stopForecast.getInboundTrams() != null) {
                        if (stopForecast.getInboundTrams().size() == 0) {
                            redLineInboundStopForecastCardView.setNoTramsForecast();
                        } else {
                            String inboundTram;

                            for (int i = 0; i < stopForecast.getInboundTrams().size(); i++) {
                                String dueMinutes =
                                        stopForecast.getInboundTrams().get(i).getDueMinutes();

                                if (i < 4) {
                                    if (localeDefault.startsWith(GAEILGE)) {
                                        inboundTram = mapEnglishGaeilge.get(
                                                stopForecast.getInboundTrams()
                                                        .get(i)
                                                        .getDestination()
                                        );
                                    } else {
                                        inboundTram = stopForecast.getInboundTrams()
                                                .get(i)
                                                .getDestination();
                                    }

                                    if (dueMinutes.equalsIgnoreCase(DUE)) {
                                        if (localeDefault.startsWith(GAEILGE))
                                            dueMinutes = mapEnglishGaeilge.get(dueMinutes);

                                        minOrMins = "";
                                    } else if (Integer.parseInt(dueMinutes) > 1) {
                                        minOrMins = mins;
                                    } else {
                                        minOrMins = min;
                                    }

                                    redLineInboundStopForecastCardView.setStopNames(
                                            i,
                                            inboundTram
                                    );

                                    redLineInboundStopForecastCardView.setStopTimes(
                                            i,
                                            dueMinutes + minOrMins
                                    );
                                }
                            }
                        }
                    }

                    if (stopForecast.getOutboundTrams() != null) {
                        if (stopForecast.getOutboundTrams().size() == 0) {
                            redLineOutboundStopForecastCardView.setNoTramsForecast();
                        } else {
                            String outboundTram;

                            for (int i = 0; i < stopForecast.getOutboundTrams().size(); i++) {
                                String dueMinutes =
                                        stopForecast.getOutboundTrams().get(i).getDueMinutes();

                                if (i < 4) {
                                    if (localeDefault.startsWith(GAEILGE)) {
                                        outboundTram = mapEnglishGaeilge.get(
                                                stopForecast.getOutboundTrams()
                                                        .get(i)
                                                        .getDestination()
                                        );
                                    } else {
                                        outboundTram =
                                                stopForecast.getOutboundTrams()
                                                        .get(i).getDestination();
                                    }

                                    if (dueMinutes.equalsIgnoreCase(DUE)) {
                                        if (localeDefault.startsWith(GAEILGE))
                                            dueMinutes = mapEnglishGaeilge.get(dueMinutes);

                                        minOrMins = "";
                                    } else if (Integer.parseInt(dueMinutes) > 1) {
                                        minOrMins = mins;
                                    } else {
                                        minOrMins = min;
                                    }

                                    redLineOutboundStopForecastCardView.setStopNames(
                                            i,
                                            outboundTram
                                    );

                                    redLineOutboundStopForecastCardView.setStopTimes(
                                            i,
                                            dueMinutes + minOrMins
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
                    redLineStatusCardView.setStatus(
                            getResources().getString(R.string.message_error)
                    );
                    redLineStatusCardView.setStatusColor(R.color.message_error);
                }

                break;

            case GREEN_LINE:
                // If a valid stop forecast exists...
                if (stopForecast != null) {
                    String status;

                    if (localeDefault.startsWith(GAEILGE)) {
                        status = getResources().getString(R.string.message_success);
                    } else {
                        status = stopForecast.getMessage();
                    }

                    if (status.contains(
                            getResources().getString(R.string.message_success))) {
                        /*
                         * No error message on server. Change the message title TextView to
                         * green and set a default success message.
                         */
                        greenLineStatusCardView.setStatus(status);
                        greenLineStatusCardView.setStatusColor(R.color.message_success);

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
                        greenLineStatusCardView.setStatus(status);
                        greenLineStatusCardView.setStatusColor(R.color.message_error);

                        /*
                         * Make the Alert menu item visible.
                         */
                        MenuItem menuItemNewsAlert = menu.findItem(R.id.action_news_alert);
                        menuItemNewsAlert.setVisible(true);
                    }

                    /*
                     * Pull in all trams from the StopForecast, but only display up to three
                     * inbound and outbound trams.
                     */
                    if (stopForecast.getInboundTrams() != null) {
                        if (stopForecast.getInboundTrams().size() == 0) {
                            greenLineInboundStopForecastCardView.setNoTramsForecast();
                        } else {
                            String inboundTram;

                            for (int i = 0; i < stopForecast.getInboundTrams().size(); i++) {
                                String dueMinutes =
                                        stopForecast.getInboundTrams().get(i).getDueMinutes();

                                if (i < 4) {
                                    if (localeDefault.startsWith(GAEILGE)) {
                                        inboundTram = mapEnglishGaeilge.get(
                                                stopForecast.getInboundTrams()
                                                        .get(i)
                                                        .getDestination());
                                    } else {
                                        inboundTram =
                                                stopForecast.getInboundTrams()
                                                        .get(i)
                                                        .getDestination();
                                    }

                                    if (dueMinutes.equalsIgnoreCase(DUE)) {
                                        if (localeDefault.startsWith(GAEILGE))
                                            dueMinutes = mapEnglishGaeilge.get(dueMinutes);

                                        minOrMins = "";
                                    } else if (Integer.parseInt(dueMinutes) > 1) {
                                        minOrMins = mins;
                                    } else {
                                        minOrMins = min;
                                    }

                                    greenLineInboundStopForecastCardView.setStopNames(
                                            i,
                                            inboundTram
                                    );

                                    greenLineInboundStopForecastCardView.setStopTimes(
                                            i,
                                            dueMinutes + minOrMins
                                    );
                                }
                            }
                        }
                    }

                    if (stopForecast.getOutboundTrams() != null) {
                        if (stopForecast.getOutboundTrams().size() == 0) {
                            greenLineOutboundStopForecastCardView.setNoTramsForecast();
                        } else {
                            String outboundTram;

                            for (int i = 0; i < stopForecast.getOutboundTrams().size(); i++) {
                                String dueMinutes =
                                        stopForecast.getOutboundTrams().get(i).getDueMinutes();

                                if (i < 4) {
                                    if (localeDefault.startsWith(GAEILGE)) {
                                        outboundTram = mapEnglishGaeilge.get(
                                                stopForecast.getOutboundTrams()
                                                        .get(i)
                                                        .getDestination()
                                        );
                                    } else {
                                        outboundTram =
                                                stopForecast.getOutboundTrams()
                                                        .get(i)
                                                        .getDestination();
                                    }

                                    if (dueMinutes.equalsIgnoreCase(DUE)) {
                                        if (localeDefault.startsWith(GAEILGE))
                                            dueMinutes = mapEnglishGaeilge.get(dueMinutes);

                                        minOrMins = "";
                                    } else if (Integer.parseInt(dueMinutes) > 1) {
                                        minOrMins = mins;
                                    } else {
                                        minOrMins = min;
                                    }

                                    greenLineOutboundStopForecastCardView.setStopNames(
                                            i,
                                            outboundTram
                                    );

                                    greenLineOutboundStopForecastCardView.setStopTimes(
                                            i,
                                            dueMinutes + minOrMins
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
                    greenLineStatusCardView.setStatus(
                            getResources().getString(R.string.message_error)
                    );
                    greenLineStatusCardView.setStatusColor(R.color.message_error);
                }

                break;

            default:
                // If for some reason the current selected tab doesn't make sense.
                Log.wtf(LOG_TAG, "Unknown tab.");
        }
    }
}
