/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2023 Aaron Hastings
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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.api.ApiMethods;
import org.thecosmicfrog.luasataglance.api.ApiTimes;
import org.thecosmicfrog.luasataglance.databinding.FragmentGreenlineBinding;
import org.thecosmicfrog.luasataglance.databinding.FragmentRedlineBinding;
import org.thecosmicfrog.luasataglance.model.EnglishGaeilgeMap;
import org.thecosmicfrog.luasataglance.model.NotifyTimesMap;
import org.thecosmicfrog.luasataglance.model.StopForecast;
import org.thecosmicfrog.luasataglance.model.StopNameIdMap;
import org.thecosmicfrog.luasataglance.util.AppUtil;
import org.thecosmicfrog.luasataglance.util.Constant;
import org.thecosmicfrog.luasataglance.util.LineFragmentViewBindingAdapter;
import org.thecosmicfrog.luasataglance.util.Preferences;
import org.thecosmicfrog.luasataglance.util.Settings;
import org.thecosmicfrog.luasataglance.util.StopForecastUtil;
import org.thecosmicfrog.luasataglance.view.SpinnerCardView;
import org.thecosmicfrog.luasataglance.view.StatusCardView;
import org.thecosmicfrog.luasataglance.view.StopForecastCardView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
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

    private static int resMenuLine;
    private static int resArrayStopsRedLine;
    private static int resArrayStopsGreenLine;
    private static StopNameIdMap mapStopNameId;
    private static String localeDefault;

    private FragmentActivity activity;
    private Context context;
    private LineFragmentViewBindingAdapter viewBinding;
    private Menu menu;
    private TabLayout tabLayout;
    private ProgressBar progressBar;
    private SpinnerCardView spinnerCardView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView scrollView;
    private StatusCardView statusCardView;
    private StopForecastCardView inboundStopForecastCardView;
    private StopForecastCardView outboundStopForecastCardView;
    private ImageView imageViewBottomNavAlerts;
    private TextView textViewBottomNavAlerts;
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
                bundle.putInt(Constant.RES_MENU_LINE, R.menu.menu_red_line);

                break;

            case Constant.GREEN_LINE:
                bundle.putString(Constant.LINE, Constant.GREEN_LINE);
                bundle.putInt(Constant.RES_MENU_LINE, R.menu.menu_green_line);

                break;

            case Constant.NO_LINE:
                Log.e(LineFragment.class.getSimpleName(), "No line specified.");

                break;

            default:
                /* If for some reason the line doesn't make sense. */
                Log.wtf(LineFragment.class.getSimpleName(), "Invalid line specified.");
        }

        lineFragment.setArguments(bundle);

        return lineFragment;
    }

    @Override
    public void onAttach(Context c) {
        super.onAttach(c);

        context = c;
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
        Bundle bundle = getArguments();
        String line = bundle.getString(Constant.LINE);

        viewBinding = getBinding(line, container);

        /* Initialise correct locale. */
        localeDefault = Locale.getDefault().toString();

        /* Instantiate a new StopNameIdMap. */
        mapStopNameId = new StopNameIdMap(localeDefault);

        View rootView = viewBinding.getLinearlayoutFragment().getRootView();

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

        activity = getActivity();

        AppUtil.resetShouldNotAskAgainIfPermissionsChangedOutsideApp(context);

        /* Remove Favourites tutorial if it has been completed once already. */
        if (line.equals(Constant.RED_LINE)
                && Preferences.hasRunOnce(context, Constant.TUTORIAL_FAVOURITES)) {
            StopForecastUtil.displayTutorial(
                    viewBinding,
                    Constant.RED_LINE,
                    Constant.TUTORIAL_FAVOURITES,
                    false
            );
        }

        if (isAdded()) {
            isInitialised = initFragment();

            /*
             * If an Intent did not bring us to this Activity and there is a stop name saved in
             * shared preferences, load that stop.
             * This provides persistence to the app across shutdowns.
             */
            if (!activity.getIntent().hasExtra(Constant.STOP_NAME)) {
                if (Preferences.selectedStopName(context, Constant.NO_LINE) != null) {
                    String stopName = Preferences.selectedStopName(context, Constant.NO_LINE);

                    setTabAndSpinner(stopName);
                }
            }

            imageViewBottomNavAlerts =
                    activity.findViewById(R.id.imageview_bottomnav_alerts);
            textViewBottomNavAlerts =
                    activity.findViewById(R.id.textview_bottomnav_alerts);

            /*
             * If a Favourite stop brought us to this Activity, load that stop's forecast.
             * If a tapped notification brought us to this Activity, load the forecast for the stop
             * sent with that Intent.
             * If the previous cases are not matched, and the user has selected a default stop, load
             * the forecast for that.
             */
            if (activity.getIntent().hasExtra(Constant.STOP_NAME)) {
                String stopName = activity.getIntent().getStringExtra(Constant.STOP_NAME);

                /*
                 * Track whether or not the tab and spinner has been set. If it has, clear the Extra
                 * so it doesn't break the Default Stop setting.
                 */
                boolean hasSetTabAndSpinner = setTabAndSpinner(stopName);

                if (hasSetTabAndSpinner) {
                    activity.getIntent().removeExtra(Constant.STOP_NAME);
                }
            } else if (activity.getIntent().hasExtra(Constant.NOTIFY_STOP_NAME)) {
                /*
                 * Track whether or not the tab and spinner has been set. If it has, clear the Extra
                 * so it doesn't break the Default Stop setting.
                 */
                boolean hasSetTabAndSpinner =
                        setTabAndSpinner(
                                activity.getIntent().getStringExtra(Constant.NOTIFY_STOP_NAME)
                        );

                if (hasSetTabAndSpinner) {
                    activity.getIntent().removeExtra(Constant.NOTIFY_STOP_NAME);
                }
            } else if (activity.getIntent().hasExtra(INTENT_EXTRA_ACTIVITY_TO_OPEN)) {
                activityRouter(
                        activity.getIntent().getStringExtra(INTENT_EXTRA_ACTIVITY_TO_OPEN)
                );

                /* Clear the Extra to avoid opening the same Activity on every start. */
                activity.getIntent().removeExtra(INTENT_EXTRA_ACTIVITY_TO_OPEN);
            } else if (!Preferences.defaultStopName(context).equals(getString(R.string.none))
                    && Preferences.defaultStopName(context) != null) {
                setTabAndSpinner(Preferences.defaultStopName(context));
            }

            /* Display tutorial for selecting a stop, if required. */
            StopForecastUtil.displayTutorial(viewBinding, line, Constant.TUTORIAL_SELECT_STOP, true);

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

                    Preferences.saveSelectedStopName(context, Constant.NO_LINE, stopName);

                    loadStopForecast(stopName, false);

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
        Settings.getSettings(context, item);

        return super.onOptionsItemSelected(item);
    }

    private LineFragmentViewBindingAdapter getBinding(String line, ViewGroup viewGroup) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        switch (line) {
            case Constant.RED_LINE:
                FragmentRedlineBinding fragmentRedlineBinding = FragmentRedlineBinding.inflate(inflater, viewGroup, false);
                return new LineFragmentViewBindingAdapter(fragmentRedlineBinding, null);

            case Constant.GREEN_LINE:
                FragmentGreenlineBinding fragmentGreenlineBinding = FragmentGreenlineBinding.inflate(inflater, viewGroup, false);
                return new LineFragmentViewBindingAdapter(null, fragmentGreenlineBinding);

            default:
                Log.wtf(LOG_TAG, "Invalid line specified.");
        }

        return null;
    }

    /**
     * Initialise local variables for this Fragment instance.
     */
    private void initFragmentVars() {
        resArrayStopsRedLine = getArguments().getInt(Constant.RES_ARRAY_STOPS_RED_LINE);
        resArrayStopsGreenLine = getArguments().getInt(Constant.RES_ARRAY_STOPS_GREEN_LINE);
        line = getArguments().getString(Constant.LINE);
        resMenuLine = getArguments().getInt(Constant.RES_MENU_LINE);
    }

    /**
     * Initialise Fragment and its views.
     */
    private boolean initFragment() {
        tabLayout = activity.findViewById(R.id.tablayout);

        progressBar = viewBinding.getProgressbar();

        setIsLoading(false);

        /* Set up Spinner and onItemSelectedListener. */
        spinnerCardView = viewBinding.getSpinnerCardView();
        spinnerCardView.setLine(line);

        spinnerCardView.getSpinnerStops().setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position,
                                               long id) {
                        /*
                         * onItemSelected() is triggered on creation of the tab. Prevent this by
                         * only triggering when the tab is visible to user. This is to prevent the
                         * Alerts button changing colour out of sync with the currently-visible tab.
                         */
                        if (isVisibleToUser) {
                            /*
                             * If the Spinner's selected item is "Select a stop...", we don't need
                             * to do anything. Just clear the stop forecast and get out of here.
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
                                    viewBinding,
                                    line,
                                    Constant.TUTORIAL_SELECT_STOP,
                                    false
                            );

                            /* Show the notifications tutorial. */
                            StopForecastUtil.displayTutorial(
                                    viewBinding,
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

                            loadStopForecast(selectedStopName, false);

                            if (isVisibleToUser) {
                                Preferences.saveSelectedStopName(
                                        context,
                                        line,
                                        selectedStopName
                                );
                            }
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

        /* Set up Status CardView. */
        statusCardView = viewBinding.getStatuscardview();

        /* Set up SwipeRefreshLayout. */
        swipeRefreshLayout = viewBinding.getSwiperefreshlayout();
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        /* Start by clearing the currently-displayed stop forecast. */
                        clearStopForecast();

                        /* Start the refresh animation. */
                        swipeRefreshLayout.setRefreshing(true);
                        loadStopForecast(
                                Preferences.selectedStopName(context, line),
                                true
                        );
                    }
                }
        );

        scrollView = viewBinding.getScrollview();

        /* Set up stop forecast CardViews. */
        inboundStopForecastCardView = viewBinding.getInboundStopforecastcardview();
        inboundStopForecastCardView.setStopForecastDirection(
                getString(R.string.inbound)
        );

        outboundStopForecastCardView = viewBinding.getOutboundStopforecastcardview();
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

        for (int i = 0; i < 6; i++) {
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
                        new Intent(context, Constant.CLASS_FARES_ACTIVITY)
                );

                break;

            case Constant.REMOTEMESSAGE_VALUE_ACTIVITY_FAVOURITES:
                Log.i(LOG_TAG, "Routing to Activity: " + Constant.CLASS_FAVOURITES_ACTIVITY);
                startActivity(
                        new Intent(context, Constant.CLASS_FAVOURITES_ACTIVITY)
                );

                break;

            case Constant.REMOTEMESSAGE_VALUE_ACTIVITY_MAIN:
                /* We're already in MainActivity. Nothing to do here. */
                Log.i(LOG_TAG, "Already on MainActivity. Not routing anywhere.");
                break;

            case Constant.REMOTEMESSAGE_VALUE_ACTIVITY_MAPS:
                Log.i(LOG_TAG, "Routing to Activity: " + Constant.CLASS_MAPS_ACTIVITY);
                startActivity(
                        new Intent(context, Constant.CLASS_MAPS_ACTIVITY)
                );

                break;

            case Constant.REMOTEMESSAGE_VALUE_ACTIVITY_NEWS:
                Log.i(LOG_TAG, "Routing to Activity: " + Constant.CLASS_NEWS_ACTIVITY);
                startActivity(
                        new Intent(context, Constant.CLASS_NEWS_ACTIVITY)
                );

                break;

            case Constant.REMOTEMESSAGE_VALUE_ACTIVITY_SETTINGS:
                Log.i(LOG_TAG, "Routing to Activity: " + Constant.CLASS_SETTINGS_ACTIVITY);
                startActivity(
                        new Intent(context, Constant.CLASS_SETTINGS_ACTIVITY)
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
                    context,
                    getString(R.string.cannot_schedule_notification),
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        /*
         * When the user opens the notification dialog as part of the tutorial, scroll back up to
         * the top so that the next tutorial is definitely visible. This should only ever run once.
         */
        if (!Preferences.hasRunOnce(context, Constant.TUTORIAL_NOTIFICATIONS)) {
            if (scrollView != null) {
                scrollView.setScrollY(0);
            }
        }

        Preferences.saveHasRunOnce(context, Constant.TUTORIAL_NOTIFICATIONS, true);

        /* We're done with the notifications tutorial. Hide it. */
        StopForecastUtil.displayTutorial(viewBinding, line, Constant.TUTORIAL_NOTIFICATIONS, false);

        /* Then, display the final tutorial. */
        StopForecastUtil.displayTutorial(viewBinding, line, Constant.TUTORIAL_FAVOURITES, true);

        Preferences.saveNotifyStopName(
                context,
                stopName
        );

        Preferences.saveNotifyStopTimeExpected(
                context,
                mapNotifyTimes.get(notifyStopTimeStr)
        );

        context.startActivity(
                new Intent(
                        context,
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
            activity.runOnUiThread(new Runnable() {
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

            spinnerCardView.setSelection(Preferences.selectedStopName(context, line));

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
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (shouldAutoReload) {
                                loadStopForecast(
                                        Preferences.selectedStopName(
                                                activity.getApplicationContext(),
                                                line
                                        ),
                                        false
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
     * @param shouldShowSnackbar Whether or not we should show a Snackbar to the user with the API
     *                           created time.
     */
    private void loadStopForecast(String stopName, final boolean shouldShowSnackbar) {
        final String API_URL = "https://api.thecosmicfrog.org/cgi-bin";
        final String API_ACTION = "times";
        final String API_VER = "3";

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

                        if (shouldShowSnackbar) {
                            String apiCreatedTime = getApiCreatedTime(apiTimes);

                            if (apiCreatedTime != null) {
                                StopForecastUtil.showSnackbar(
                                        activity,
                                        "Times updated at " + apiCreatedTime
                                );
                            }
                        }
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

                    Log.e(LOG_TAG, "Status: " +
                            Integer.toString(retrofitError.getResponse().getStatus()));

                    if (retrofitError.getResponse().getHeaders() != null) {
                        Log.e(LOG_TAG, "Headers: " +
                                retrofitError.getResponse().getHeaders().toString());
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
     * Get the "created" time from the API response and format it so that only the time (and not
     * date) is returned.
     * @param apiTimes ApiTimes model.
     * @return String representing the 24hr time (HH:mm:ss) of the API's "created" time.
     */
    private String getApiCreatedTime(ApiTimes apiTimes) {
        try {
            if (apiTimes.getCreatedTime() != null) {
                Date currentTime = new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss",
                        Locale.getDefault()
                ).parse(apiTimes.getCreatedTime());

                DateFormat dateFormat =
                        new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

                if (currentTime != null) {
                    return dateFormat.format(currentTime);
                }
            }
        } catch (NullPointerException e) {
            Log.e(
                    LOG_TAG,
                    "Failed to find content view during Snackbar creation."
            );
        } catch (ParseException e) {
            Log.e(LOG_TAG, "Failed to parse created time from API.");
        }

        return null;
    }

    /**
     * Draw stop forecast to screen.
     * @param stopForecast StopForecast model containing data for requested stop.
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

            if (stopForecast.getStopForecastStatusDirectionInbound()
                    .getOperatingNormally() != null
                    && stopForecast.getStopForecastStatusDirectionOutbound()
                    .getOperatingNormally() != null) {
                if (stopForecast.getStopForecastStatusDirectionInbound()
                        .getOperatingNormally()
                        && stopForecast.getStopForecastStatusDirectionOutbound()
                        .getOperatingNormally()) {
                    operatingNormally = true;
                }
            }

            if (localeDefault.startsWith(GAEILGE)) {
                status = getString(R.string.message_success);
            } else {
                status = stopForecast.getMessage();
            }

            if (status != null) {
                /* A lot of Luas status messages relate to lifts being out of service. Ignore these. */
                if (operatingNormally || status.toLowerCase().contains("lift")) {
                    /*
                     * No error message on server. Change the message title TextView to
                     * green and set a default success message.
                     */
                    statusCardView.setStatus(status);
                    statusCardView.setStatusColor(R.color.message_success);

                    /* Change the alerts image to the default white image. */
                    imageViewBottomNavAlerts.setImageResource(
                            R.drawable.ic_error_alerts
                    );

                    /* Change the color of the Alerts TextView to white (default). */
                    textViewBottomNavAlerts.setTextColor(
                            ContextCompat.getColor(context, android.R.color.white)
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
                    imageViewBottomNavAlerts.setImageResource(
                            R.drawable.ic_error_alerts_red
                    );

                    /* Change the color of the Alerts TextView to red. */
                    textViewBottomNavAlerts.setTextColor(
                            ContextCompat.getColor(context, R.color.message_error)
                    );
                }
            }

            /*
             * Pull in all trams from the StopForecast, but only display up to five
             * inbound and outbound trams.
             */
            if (stopForecast.getInboundTrams().size() == 0) {
                inboundStopForecastCardView.setNoTramsForecast();
            } else {
                String inboundTram;

                for (int i = 0; i < stopForecast.getInboundTrams().size(); i++) {
                    String dueMinutes =
                            stopForecast.getInboundTrams().get(i).getDueMinutes();

                    if (i < 6) {
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

                        if (dueMinutes != null) {
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

            if (stopForecast.getOutboundTrams().size() == 0) {
                outboundStopForecastCardView.setNoTramsForecast();
            } else {
                String outboundTram;

                for (int i = 0; i < stopForecast.getOutboundTrams().size(); i++) {
                    String dueMinutes =
                            stopForecast.getOutboundTrams().get(i).getDueMinutes();

                    if (i < 6) {
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

                        if (dueMinutes != null) {
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
