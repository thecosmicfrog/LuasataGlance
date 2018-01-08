/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2017 Aaron Hastings
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

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
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
import android.widget.ProgressBar;
import android.widget.ScrollView;
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
import org.thecosmicfrog.luasataglance.util.Analytics;
import org.thecosmicfrog.luasataglance.util.Constant;
import org.thecosmicfrog.luasataglance.util.Preferences;
import org.thecosmicfrog.luasataglance.util.Settings;
import org.thecosmicfrog.luasataglance.util.StopForecastUtil;
import org.thecosmicfrog.luasataglance.view.SpinnerCardView;
import org.thecosmicfrog.luasataglance.view.StatusCardView;
import org.thecosmicfrog.luasataglance.view.StopForecastCardView;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class LineFragment extends Fragment {

    private final String LOG_TAG = LineFragment.class.getSimpleName();

    private static int resLayoutFragmentLine;
    private static int resMenuLine;
    private static int resProgressBar;
    private static int resSpinnerCardView;
    private static int resStatusCardView;
    private static int resSwipeRefreshLayout;
    private static int resScrollView;
    private static int resInboundStopForecastCardView;
    private static int resOutboundStopForecastCardView;
    private static int resArrayStopsRedLine;
    private static int resArrayStopsGreenLine;
    private static StopNameIdMap mapStopNameId;
    private static String localeDefault;

    private View rootView = null;
    private Menu menu;
    private TabLayout tabLayout;
    private ProgressBar progressBar;
    private SpinnerCardView spinnerCardView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView scrollView;
    private StatusCardView statusCardView;
    private StopForecastCardView inboundStopForecastCardView;
    private StopForecastCardView outboundStopForecastCardView;
    private boolean isInitialised;
    private TimerTask timerTaskReload;
    private boolean shouldAutoReload = false;
    private String line;
    private boolean isVisibleToUser = false;

    public LineFragment() {
        /* Required empty public constructor. */
    }

    public static LineFragment newInstance(String line) {
        LineFragment lineFragment = new LineFragment();
        Bundle bundle = new Bundle();

        bundle.putInt(Constant.RES_ARRAY_STOPS_RED_LINE, R.array.array_stops_redline);
        bundle.putInt(Constant.RES_ARRAY_STOPS_GREEN_LINE, R.array.array_stops_greenline);

        switch (line) {
            case Constant.RED_LINE:
                bundle.putString(Constant.LINE, Constant.RED_LINE);
                bundle.putInt(Constant.RES_LAYOUT_FRAGMENT_LINE, R.layout.fragment_redline);
                bundle.putInt(Constant.RES_MENU_LINE, R.menu.menu_red_line);
                bundle.putInt(Constant.RES_PROGRESSBAR, R.id.redline_progressbar);
                bundle.putInt(Constant.RES_SPINNER_CARDVIEW, R.id.redline_spinner_card_view);
                bundle.putInt(Constant.RES_STATUS_CARDVIEW, R.id.redline_statuscardview);
                bundle.putInt(Constant.RES_SWIPEREFRESHLAYOUT, R.id.redline_swiperefreshlayout);
                bundle.putInt(Constant.RES_SCROLLVIEW, R.id.redline_scrollview);
                bundle.putInt(
                        Constant.RES_INBOUND_STOPFORECASTCARDVIEW,
                        R.id.redline_inbound_stopforecastcardview
                );
                bundle.putInt(
                        Constant.RES_OUTBOUND_STOPFORECASTCARDVIEW,
                        R.id.redline_outbound_stopforecastcardview
                );

                break;

            case Constant.GREEN_LINE:
                bundle.putString(Constant.LINE, Constant.GREEN_LINE);
                bundle.putInt(Constant.RES_LAYOUT_FRAGMENT_LINE, R.layout.fragment_greenline);
                bundle.putInt(Constant.RES_MENU_LINE, R.menu.menu_green_line);
                bundle.putInt(Constant.RES_PROGRESSBAR, R.id.greenline_progressbar);
                bundle.putInt(Constant.RES_SPINNER_CARDVIEW, R.id.greenline_spinner_card_view);
                bundle.putInt(Constant.RES_STATUS_CARDVIEW, R.id.greenline_statuscardview);
                bundle.putInt(Constant.RES_SWIPEREFRESHLAYOUT, R.id.greenline_swiperefreshlayout);
                bundle.putInt(Constant.RES_SCROLLVIEW, R.id.greenline_scrollview);
                bundle.putInt(
                        Constant.RES_INBOUND_STOPFORECASTCARDVIEW,
                        R.id.greenline_inbound_stopforecastcardview
                );
                bundle.putInt(
                        Constant.RES_OUTBOUND_STOPFORECASTCARDVIEW,
                        R.id.greenline_outbound_stopforecastcardview
                );

                break;

            default:
                /* If for some reason the line doesn't make sense. */
                Log.wtf(LineFragment.class.getSimpleName(), "Invalid line specified.");
        }

        lineFragment.setArguments(bundle);

        return lineFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initFragmentVars();

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        /* Inflate the layout for this fragment. */
        rootView = inflater.inflate(resLayoutFragmentLine, container, false);

        /* Initialise correct locale. */
        localeDefault = Locale.getDefault().toString();

        /* Instantiate a new StopNameIdMap. */
        mapStopNameId = new StopNameIdMap(localeDefault);

        if (isAdded()) {
            isInitialised = initFragment();
        }

        /*
         * If an Intent did not bring us to this Activity and there is a stop name saved in shared
         * preferences, load that stop.
         * This provides persistence to the app across shutdowns.
         */
        if (!getActivity().getIntent().hasExtra(Constant.STOP_NAME)) {
            if (Preferences.selectedStopName(getContext(), Constant.NO_LINE) != null) {
                String stopName = Preferences.selectedStopName(getContext(), Constant.NO_LINE);

                setTabAndSpinner(stopName);
            }
        }

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();

        /* Stop the auto-reload TimerTask. */
        timerTaskReload.cancel();
    }

    @Override
    public void onResume() {
        final String INTENT_EXTRA_ACTIVITY_TO_OPEN = "activityToOpen";

        super.onResume();

        /* Remove Favourites tutorial if it has been completed once already. */
        if (line.equals(Constant.RED_LINE)
                && Preferences.hasRunOnce(getContext(), Constant.TUTORIAL_FAVOURITES)) {
            StopForecastUtil.displayTutorial(
                    rootView,
                    Constant.RED_LINE,
                    Constant.TUTORIAL_FAVOURITES,
                    false
            );
        }

        if (isAdded()) {
            /*
             * If a Favourite stop brought us to this Activity, load that stop's forecast.
             * If a tapped notification brought us to this Activity, load the forecast for the stop
             * sent with that Intent.
             * If the previous cases are not matched, and the user has selected a default stop, load
             * the forecast for that.
             */
            if (getActivity().getIntent().hasExtra(Constant.STOP_NAME)) {
                String stopName = getActivity().getIntent().getStringExtra(Constant.STOP_NAME);

                /*
                 * Track whether or not the tab and spinner has been set. If it has, clear the Extra
                 * so it doesn't break the Default Stop setting.
                 */
                boolean hasSetTabAndSpinner = setTabAndSpinner(stopName);

                if (hasSetTabAndSpinner) {
                    getActivity().getIntent().removeExtra(Constant.STOP_NAME);
                }
            } else if (getActivity().getIntent().hasExtra(Constant.NOTIFY_STOP_NAME)) {
                /*
                 * Track whether or not the tab and spinner has been set. If it has, clear the Extra
                 * so it doesn't break the Default Stop setting.
                 */
                boolean hasSetTabAndSpinner =
                        setTabAndSpinner(
                                getActivity().getIntent().getStringExtra(Constant.NOTIFY_STOP_NAME)
                        );

                if (hasSetTabAndSpinner) {
                    getActivity().getIntent().removeExtra(Constant.NOTIFY_STOP_NAME);
                }
            } else if (getActivity().getIntent().hasExtra(INTENT_EXTRA_ACTIVITY_TO_OPEN)) {
                activityRouter(
                        getActivity().getIntent().getStringExtra(INTENT_EXTRA_ACTIVITY_TO_OPEN)
                );

                /* Clear the Extra to avoid opening the same Activity on every start. */
                getActivity().getIntent().removeExtra(INTENT_EXTRA_ACTIVITY_TO_OPEN);
            } else if (!Preferences.defaultStopName(getContext()).equals(getString(R.string.none))
                    && Preferences.defaultStopName(getContext()) != null) {
                setTabAndSpinner(Preferences.defaultStopName(getContext()));
            }

            /* Display tutorial for selecting a stop, if required. */
            StopForecastUtil.displayTutorial(rootView, line, Constant.TUTORIAL_SELECT_STOP, true);

            /*
             * Reload stop forecast.
             * Induce 10 second delay if app is launching from cold start (timerTaskReload == null)
             * in order to prevent two HTTP requests in rapid succession.
             */
            if (timerTaskReload == null) {
                autoReloadStopForecast(10000);
            } else {
                autoReloadStopForecast(0);
            }
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        this.isVisibleToUser = isVisibleToUser;

        if (isInitialised) {
            /* If the Spinner's selected item is "Select a stop...", get out of here. */
            if (spinnerCardView.getSpinnerStops().getSelectedItemPosition() == 0) {
                Log.i(LOG_TAG, "Spinner selected item is \"Select a stop...\"");

                return;
            }

            /* When this tab is visible to the user, load a stop forecast. */
            if (isVisibleToUser) {
                if (spinnerCardView.getSpinnerStops().getSelectedItem() != null) {
                    String stopName =
                            spinnerCardView.getSpinnerStops().getSelectedItem().toString();

                    Preferences.saveSelectedStopName(getContext(), Constant.NO_LINE, stopName);

                    loadStopForecast(stopName);

                    shouldAutoReload = true;
                } else {
                    Log.w(LOG_TAG, "Spinner selected item is null.");
                }
            } else {
                shouldAutoReload = false;
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        /* Set the menu to a class variable for easy manipulation. */
        this.menu = menu;

        /* Inflate the menu; this adds items to the action bar if it is present. */
        inflater.inflate(resMenuLine, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Settings.getSettings(getContext(), item);

        return super.onOptionsItemSelected(item);
    }

    /**
     * Initialise local variables for this Fragment instance.
     */
    private void initFragmentVars() {
        resArrayStopsRedLine = getArguments().getInt(Constant.RES_ARRAY_STOPS_RED_LINE);
        resArrayStopsGreenLine = getArguments().getInt(Constant.RES_ARRAY_STOPS_GREEN_LINE);
        line = getArguments().getString(Constant.LINE);
        resLayoutFragmentLine = getArguments().getInt(Constant.RES_LAYOUT_FRAGMENT_LINE);
        resMenuLine = getArguments().getInt(Constant.RES_MENU_LINE);
        resProgressBar = getArguments().getInt(Constant.RES_PROGRESSBAR);
        resSpinnerCardView = getArguments().getInt(Constant.RES_SPINNER_CARDVIEW);
        resStatusCardView = getArguments().getInt(Constant.RES_STATUS_CARDVIEW);
        resSwipeRefreshLayout = getArguments().getInt(Constant.RES_SWIPEREFRESHLAYOUT);
        resScrollView = getArguments().getInt(Constant.RES_SCROLLVIEW);
        resInboundStopForecastCardView =
                getArguments().getInt(Constant.RES_INBOUND_STOPFORECASTCARDVIEW);
        resOutboundStopForecastCardView =
                getArguments().getInt(Constant.RES_OUTBOUND_STOPFORECASTCARDVIEW);
    }

    /**
     * Initialise Fragment and its views.
     */
    private boolean initFragment() {
        tabLayout = getActivity().findViewById(R.id.tablayout);

        progressBar = rootView.findViewById(resProgressBar);

        setIsLoading(false);

        /* Set up Spinner and onItemSelectedListener. */
        spinnerCardView =
                rootView.findViewById(resSpinnerCardView);
        spinnerCardView.setLine(line);

        spinnerCardView.getSpinnerStops().setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position,
                                               long id) {
                        /*
                         * If the Spinner's selected item is "Select a stop...", we don't need to
                         * do anything. Just clear the stop forecast and get out of here.
                         */
                        if (position == 0) {
                            shouldAutoReload = false;

                            swipeRefreshLayout.setEnabled(false);

                            clearStopForecast();

                            return;
                        } else {
                            swipeRefreshLayout.setEnabled(true);
                        }

                        shouldAutoReload = true;

                        /* Hide the select stop tutorial, if it is visible. */
                        StopForecastUtil.displayTutorial(
                                rootView,
                                line,
                                Constant.TUTORIAL_SELECT_STOP,
                                false
                        );

                        /* Show the notifications tutorial. */
                        StopForecastUtil.displayTutorial(
                                rootView,
                                line,
                                Constant.TUTORIAL_NOTIFICATIONS,
                                true
                        );

                        /*
                         * Get the stop name from the current position of the Spinner, save it to
                         * SharedPreferences, then load a stop forecast with it.
                         */
                        String selectedStopName =
                                spinnerCardView
                                        .getSpinnerStops().getItemAtPosition(position).toString();

                        loadStopForecast(selectedStopName);

                        if (isVisibleToUser) {
                            Preferences.saveSelectedStopName(
                                    getContext(),
                                    line,
                                    selectedStopName
                            );
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

        /* Set up Status CardView. */
        statusCardView =
                rootView.findViewById(resStatusCardView);

        /* Set up SwipeRefreshLayout. */
        swipeRefreshLayout =
                rootView.findViewById(resSwipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        /* Start by clearing the currently-displayed stop forecast. */
                        clearStopForecast();

                        /* Start the refresh animation. */
                        swipeRefreshLayout.setRefreshing(true);
                        loadStopForecast(
                                Preferences.selectedStopName(getContext(), line)
                        );
                    }
                }
        );

        scrollView = rootView.findViewById(resScrollView);

        /* Set up stop forecast CardViews. */
        inboundStopForecastCardView =
                rootView.findViewById(
                    resInboundStopForecastCardView
                );
        inboundStopForecastCardView.setStopForecastDirection(
                getString(R.string.inbound)
        );

        outboundStopForecastCardView =
                rootView.findViewById(
                        resOutboundStopForecastCardView
                );
        outboundStopForecastCardView.setStopForecastDirection(
                getString(R.string.outbound)
        );

        /* Set up onClickListeners for stop forecasts in both tabs. */
        initStopForecastOnClickListeners();

        return true;
    }

    /**
     * Initialise OnClickListeners for a stop forecast.
     */
    private void initStopForecastOnClickListeners() {
        TableRow[] tableRowInboundStops = inboundStopForecastCardView.getTableRowStops();
        TableRow[] tableRowOutboundStops = outboundStopForecastCardView.getTableRowStops();

        for (int i = 0; i < 5; i++) {
            final int index = i;

            tableRowInboundStops[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showNotifyTimeDialog(
                            spinnerCardView.getSpinnerStops().getSelectedItem().toString(),
                            inboundStopForecastCardView.getTextViewStopTimes(),
                            index
                    );
                }
            });

            tableRowOutboundStops[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showNotifyTimeDialog(
                            spinnerCardView.getSpinnerStops().getSelectedItem().toString(),
                            outboundStopForecastCardView.getTextViewStopTimes(),
                            index
                    );
                }
            });
        }
    }

    /**
     * Utility method to open an Activity based on a passed tag value.
     * @param activityToOpen Value of Activity to open.
     */
    private void activityRouter(String activityToOpen) {
        Log.i(LOG_TAG, "Intent received to open Activity.");

        switch (activityToOpen) {
            case Constant.REMOTEMESSAGE_VALUE_ACTIVITY_FARES:
                Log.i(LOG_TAG, "Routing to Activity: " + Constant.CLASS_FARES_ACTIVITY);
                startActivity(
                        new Intent(getContext(), Constant.CLASS_FARES_ACTIVITY)
                );

                break;

            case Constant.REMOTEMESSAGE_VALUE_ACTIVITY_FAVOURITES:
                Log.i(LOG_TAG, "Routing to Activity: " + Constant.CLASS_FAVOURITES_ACTIVITY);
                startActivity(
                        new Intent(getContext(), Constant.CLASS_FAVOURITES_ACTIVITY)
                );

                break;

            case Constant.REMOTEMESSAGE_VALUE_ACTIVITY_MAIN:
                /* We're already in MainActivity. Nothing to do here. */
                Log.i(LOG_TAG, "Already on MainActivity. Not routing anywhere.");
                break;

            case Constant.REMOTEMESSAGE_VALUE_ACTIVITY_MAPS:
                Log.i(LOG_TAG, "Routing to Activity: " + Constant.CLASS_MAPS_ACTIVITY);
                startActivity(
                        new Intent(getContext(), Constant.CLASS_MAPS_ACTIVITY)
                );

                break;

            case Constant.REMOTEMESSAGE_VALUE_ACTIVITY_NEWS:
                Log.i(LOG_TAG, "Routing to Activity: " + Constant.CLASS_NEWS_ACTIVITY);
                startActivity(
                        new Intent(getContext(), Constant.CLASS_NEWS_ACTIVITY)
                );

                break;

            case Constant.REMOTEMESSAGE_VALUE_ACTIVITY_SETTINGS:
                Log.i(LOG_TAG, "Routing to Activity: " + Constant.CLASS_SETTINGS_ACTIVITY);
                startActivity(
                        new Intent(getContext(), Constant.CLASS_SETTINGS_ACTIVITY)
                );

                break;

            default:
                /*
                 * We should have never gotten to this point, as NotificationUtil should
                 * pass MainActivity as its default case.
                 */
                Log.wtf(
                        LOG_TAG,
                        "activityToOpen key does not correspond to any known value."
                );
        }
    }

    /**
     * Show dialog for choosing notification times.
     * @param stopName          Stop name to notify for.
     * @param textViewStopTimes Array of TextViews for times in a stop forecast.
     * @param index             Index representing which specific tram to notify for.
     */
    private void showNotifyTimeDialog(String stopName, TextView[] textViewStopTimes, int index) {
        String localeDefault = Locale.getDefault().toString();
        String notifyStopTimeStr = textViewStopTimes[index].getText().toString();
        NotifyTimesMap mapNotifyTimes = new NotifyTimesMap(localeDefault, Constant.STOP_FORECAST);

        if (notifyStopTimeStr.equals(""))
            return;

        if (notifyStopTimeStr.matches(
                getString(R.string.due) + "|" + "1 .*|2 .*")) {
            Toast.makeText(
                    getContext(),
                    getString(R.string.cannot_schedule_notification),
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        /*
         * When the user opens the notification dialog as part of the tutorial, scroll back up to
         * the top so that the next tutorial is definitely visible. This should only ever run once.
         */
        if (!Preferences.hasRunOnce(rootView.getContext(), Constant.TUTORIAL_NOTIFICATIONS)) {
            if (scrollView != null) {
                scrollView.setScrollY(0);
            }
        }

        Preferences.saveHasRunOnce(rootView.getContext(), Constant.TUTORIAL_NOTIFICATIONS, true);

        /* We're done with the notifications tutorial. Hide it. */
        StopForecastUtil.displayTutorial(rootView, line, Constant.TUTORIAL_NOTIFICATIONS, false);

        /* Then, display the final tutorial. */
        StopForecastUtil.displayTutorial(rootView, line, Constant.TUTORIAL_FAVOURITES, true);

        Preferences.saveNotifyStopName(
                getContext(),
                stopName
        );

        Preferences.saveNotifyStopTimeExpected(
                getContext(),
                mapNotifyTimes.get(notifyStopTimeStr)
        );

        getContext().startActivity(
                new Intent(
                        getContext(),
                        NotifyTimeActivity.class
                )
        );
    }

    /**
     * Make progress bar animate or not.
     * @param loading Whether or not progress bar should animate.
     */
    private void setIsLoading(final boolean loading) {
        if (isAdded()) {
            /*
             * Only run if Fragment is attached to Activity. Without this check, the app is liable
             * to crash when the screen is rotated many times in a given period of time.
             */
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (loading) {
                        progressBar.setVisibility(View.VISIBLE);
                    } else {
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                }
            });
        }
    }

    /**
     * Set the current tab and the position of the Spinner.
     */
    private boolean setTabAndSpinner(String stopName) {
        String[] arrayStopsRedLine = getResources().getStringArray(
                resArrayStopsRedLine
        );
        String[] arrayStopsGreenLine = getResources().getStringArray(
                resArrayStopsGreenLine
        );

        List<String> listStopsRedLine = Arrays.asList(arrayStopsRedLine);
        List<String> listStopsGreenLine = Arrays.asList(arrayStopsGreenLine);
        List<String> listStopsThisLine = null;

        int indexOtherLine = -1;

        /* Initialise some variables specific to this Fragment (i.e. Red Line or Green Line). */
        switch (line) {
            case Constant.RED_LINE:
                listStopsThisLine = listStopsRedLine;
                indexOtherLine = 1;

                break;

            case Constant.GREEN_LINE:
                listStopsThisLine = listStopsGreenLine;
                indexOtherLine = 0;

                break;

            default:
                /* If for some reason the line doesn't make sense. */
                Log.wtf(LOG_TAG, "Invalid line specified.");
        }

        /* Safety check. */
        if (listStopsThisLine == null) {
            Log.e(LOG_TAG, "List of stops for this line is null.");

            return false;
        }

        /*
         * If the List of stops representing this Fragment contains the requested stop name, set the
         * Spinner to that stop.
         * Otherwise, switch to the other tab and load the last-loaded stop in the previous tab.
         */
        if (listStopsThisLine.contains(stopName)) {
            spinnerCardView.setSelection(stopName);

            return true;
        } else {
            TabLayout.Tab tab = tabLayout.getTabAt(indexOtherLine);

            if (tab != null) {
                tab.select();
            }

            spinnerCardView.setSelection(Preferences.selectedStopName(getContext(), line));

            return false;
        }
    }

    /**
     * Clear stop forecast.
     */
    private void clearStopForecast() {
        inboundStopForecastCardView.clearStopForecast();
        outboundStopForecastCardView.clearStopForecast();
    }

    /**
     * Automatically reload the stop forecast after a defined period.
     * @param delayTimeMillis The delay (ms) before starting the timer.
     */
    public void autoReloadStopForecast(int delayTimeMillis) {
        final int RELOAD_TIME_MILLIS = 10000;

        timerTaskReload = new TimerTask() {
            @Override
            public void run() {
                /* Check Fragment is attached to Activity to avoid NullPointerExceptions. */
                if (isAdded()) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (shouldAutoReload) {
                                loadStopForecast(
                                        Preferences.selectedStopName(
                                                getContext(),
                                                line
                                        )
                                );
                            }
                        }
                    });
                }
            }
        };

        /* Schedule the auto-reload task to run. */
        new Timer().schedule(timerTaskReload, delayTimeMillis, RELOAD_TIME_MILLIS);
    }

    /**
     * Load the stop forecast for a particular stop.
     * @param stopName The stop for which to load a stop forecast.
     */
    private void loadStopForecast(String stopName) {
        final String API_URL = "https://api.thecosmicfrog.org/cgi-bin";
        final String API_ACTION = "times";
        final String API_VER = "2";

        setIsLoading(true);

        /*
         * Prepare Retrofit API call.
         */
        final RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(API_URL)
                .build();

        ApiMethods methods = restAdapter.create(ApiMethods.class);

        Callback<ApiTimes> callback = new Callback<ApiTimes>() {
            @Override
            public void success(ApiTimes apiTimes, Response response) {
                /* Check Fragment is attached to Activity to avoid NullPointerExceptions. */
                if (isAdded()) {
                    /* If the server returned times. */
                    if (apiTimes != null) {
                        /* Then create a stop forecast with this data. */
                        StopForecast stopForecast = StopForecastUtil.createStopForecast(apiTimes);

                        clearStopForecast();

                        /* Update the stop forecast. */
                        updateStopForecast(stopForecast);

                        /* Stop the refresh animations. */
                        setIsLoading(false);
                        swipeRefreshLayout.setRefreshing(false);
                    } else {
                        Analytics.nullApitimes(
                                getContext(),
                                "null",
                                "null_apitimes_mobile"
                        );
                    }
                }
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Log.e(LOG_TAG, "Failure during call to server.");

                /*
                 * If we get a message or a response from the server, there's likely an issue with
                 * the client request or the server's response itself.
                 */
                if (retrofitError.getMessage() != null) {
                    Log.e(LOG_TAG, "Message: " + retrofitError.getMessage());
                }

                if (retrofitError.getResponse() != null) {
                    if (retrofitError.getResponse().getUrl() != null) {
                        Log.e(LOG_TAG, "Response: " + retrofitError.getResponse().getUrl());
                    }

                    Log.e(LOG_TAG, "Status: " + Integer.toString(retrofitError.getResponse().getStatus()));

                    if (retrofitError.getResponse().getHeaders() != null) {
                        Log.e(LOG_TAG, "Headers: " + retrofitError.getResponse().getHeaders().toString());
                    }

                    if (retrofitError.getResponse().getBody() != null) {
                        Log.e(LOG_TAG, "Body: " + retrofitError.getResponse().getBody().toString());
                    }

                    if (retrofitError.getResponse().getReason() != null) {
                        Log.e(LOG_TAG, "Reason: " + retrofitError.getResponse().getReason());
                    }
                }

                /*
                 * If we don't receive a message or response, we can still get an idea of what's
                 * going on by getting the "kind" of error.
                 */
                if (retrofitError.getKind() != null) {
                    Log.e(LOG_TAG, "Kind: " + retrofitError.getKind().toString());
                }

                Analytics.httpError(
                        getContext(),
                        "http_error",
                        "http_error_general_mobile"
                );
            }
        };

        /* Call API and get stop forecast from server. */
        methods.getStopForecast(
                API_ACTION,
                API_VER,
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
        final String DUE = "DUE";

        EnglishGaeilgeMap mapEnglishGaeilge = new EnglishGaeilgeMap();
        String min = " " + getString(R.string.min);
        String mins = " " + getString(R.string.mins);
        String minOrMins;

        /* If a valid stop forecast exists... */
        if (stopForecast != null) {
            String status;
            boolean operatingNormally = false;

            if (stopForecast.getStopForecastStatusDirectionInbound().getOperatingNormally()
                    && stopForecast.getStopForecastStatusDirectionOutbound().getOperatingNormally()) {
                operatingNormally = true;
            }

            if (localeDefault.startsWith(GAEILGE)) {
                status = getString(R.string.message_success);
            } else {
                status = stopForecast.getMessage();
            }

            if (operatingNormally) {
                /*
                 * No error message on server. Change the message title TextView to
                 * green and set a default success message.
                 */
                statusCardView.setStatus(status);
                statusCardView.setStatusColor(R.color.message_success);

                /* Change the alerts image to the default white image. */
                MainActivity.getImageViewBottomNavAlerts().setImageResource(
                        R.drawable.ic_error_alerts
                );

                /* Change the color of the Alerts TextView to white (default). */
                MainActivity.getTextViewBottomNavAlerts().setTextColor(
                        ContextCompat.getColor(getContext(), android.R.color.white)
                );
            } else {
                if (status.equals("")) {
                    /*
                     * If the server returns no status message, the Luas RTPI system is
                     * probably down.
                     */
                    statusCardView.setStatus(
                            getString(R.string.message_no_status)
                    );
                } else {
                    /* Set the error message from the server. */
                    statusCardView.setStatus(status);
                }

                /* Change the color of the message title TextView to red. */
                statusCardView.setStatusColor(R.color.message_error);

                /* Change the Alerts image to the red version. */
                MainActivity.getImageViewBottomNavAlerts().setImageResource(
                        R.drawable.ic_error_alerts_red
                );

                /* Change the color of the Alerts TextView to red. */
                MainActivity.getTextViewBottomNavAlerts().setTextColor(
                        ContextCompat.getColor(getContext(), R.color.message_error)
                );
            }

            /*
             * Pull in all trams from the StopForecast, but only display up to five
             * inbound and outbound trams.
             */
            if (stopForecast.getInboundTrams() != null) {
                if (stopForecast.getInboundTrams().size() == 0) {
                    inboundStopForecastCardView.setNoTramsForecast();
                } else {
                    String inboundTram;

                    for (int i = 0; i < stopForecast.getInboundTrams().size(); i++) {
                        String dueMinutes =
                                stopForecast.getInboundTrams().get(i).getDueMinutes();

                        if (i < 5) {
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
                                if (localeDefault.startsWith(GAEILGE)) {
                                    dueMinutes = mapEnglishGaeilge.get(dueMinutes);
                                }

                                minOrMins = "";
                            } else if (Integer.parseInt(dueMinutes) > 1) {
                                minOrMins = mins;
                            } else {
                                minOrMins = min;
                            }

                            inboundStopForecastCardView.setStopNames(
                                    i,
                                    inboundTram
                            );

                            inboundStopForecastCardView.setStopTimes(
                                    i,
                                    dueMinutes + minOrMins
                            );
                        }
                    }
                }
            }

            if (stopForecast.getOutboundTrams() != null) {
                if (stopForecast.getOutboundTrams().size() == 0) {
                    outboundStopForecastCardView.setNoTramsForecast();
                } else {
                    String outboundTram;

                    for (int i = 0; i < stopForecast.getOutboundTrams().size(); i++) {
                        String dueMinutes =
                                stopForecast.getOutboundTrams().get(i).getDueMinutes();

                        if (i < 5) {
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
                                if (localeDefault.startsWith(GAEILGE)) {
                                    dueMinutes = mapEnglishGaeilge.get(dueMinutes);
                                }

                                minOrMins = "";
                            } else if (Integer.parseInt(dueMinutes) > 1) {
                                minOrMins = mins;
                            } else {
                                minOrMins = min;
                            }

                            outboundStopForecastCardView.setStopNames(
                                    i,
                                    outboundTram
                            );

                            outboundStopForecastCardView.setStopTimes(
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
            statusCardView.setStatus(
                    getString(R.string.message_error)
            );
            statusCardView.setStatusColor(R.color.message_error);
        }
    }
}
