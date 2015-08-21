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

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.object.EnglishGaeilgeMap;
import org.thecosmicfrog.luasataglance.object.NotifyTimesMap;
import org.thecosmicfrog.luasataglance.object.StopForecast;
import org.thecosmicfrog.luasataglance.object.Tram;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

public class LuasTimesFragment extends Fragment {

    private final String LOG_TAG = LuasTimesFragment.class.getSimpleName();

    private final String GAEILGE = "ga";

    private View rootView = null;

    private final String RED_LINE = "red_line";
    private final String GREEN_LINE = "green_line";
    private final String STOP_FORECAST = "stop_forecast";
    private final String INBOUND = "inbound";
    private final String OUTBOUND = "outbound";

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
    private LinearLayout linearLayoutSwipeRefreshTutorial;
    private TableRow[] tableRowInboundStops;
    private TableRow[] tableRowOutboundStops;
    private TextView[] textViewInboundStopNames;
    private TextView[] textViewInboundStopTimes;
    private TextView[] textViewOutboundStopNames;
    private TextView[] textViewOutboundStopTimes;

    private TimerTask timerTaskReload;

    private static AlarmManager alarmManager;
    private static BroadcastReceiver broadcastReceiver;
    private static PendingIntent pendingIntent;

    private static String localeDefault;
    private static String notifyStopName;
    private static String notifyStopTimeStr;
    private static int notifyStopTimeExpected;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_main, container, false);

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
        displaySwipeRefreshTutorial();

        /*
         * Reload stop forecast every 10 seconds.
         * Induce 10 second delay if app is launching from cold start (timerTaskReload == null) in
         * order to prevent two HTTP requests in rapid succession.
         */
        if (timerTaskReload == null)
            autoReloadStopForecast(10000, 10000);
        else
            autoReloadStopForecast(0, 10000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Stop the auto-reload TimerTask.
        timerTaskReload.cancel();
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
                getResources().getColor(R.color.tab_red_line),
                PorterDuff.Mode.SRC_ATOP
        );

        tabHost.getTabWidget().getChildAt(1).getBackground().setColorFilter(
                getResources().getColor(R.color.tab_green_line),
                PorterDuff.Mode.SRC_ATOP
        );

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
                loadStopForecast(redLineSpinnerStop.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Only display the SwipeRefreshLayout tutorial in the Red Line tab for simplicity.
        linearLayoutSwipeRefreshTutorial
                = (LinearLayout) rootView.findViewById(R.id.linearlayout_swipe_refresh_tutorial);

        redLineSwipeRefreshLayout =
                (SwipeRefreshLayout) rootView.findViewById(R.id.red_line_swiperefreshlayout);
        redLineSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // Hide the SwipeRefreshTutorial, if it is visible.
                        linearLayoutSwipeRefreshTutorial.setVisibility(View.GONE);

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
     * tutorial on how to use the SwipeRefreshLayout to reload a stop forecast.
     */
    private void displaySwipeRefreshTutorial() {
        /*
         * Start by making sure the TextView isn't displayed. This should only display
         * when a user has no favourites saved.
         */
        linearLayoutSwipeRefreshTutorial
                = (LinearLayout) rootView.findViewById(R.id.linearlayout_swipe_refresh_tutorial);
        linearLayoutSwipeRefreshTutorial.setVisibility(View.GONE);

        try {
            final String FILE_HAS_RUN_ONCE = "has_run_once";

            /*
             * If the swipe_refresh_first_time file doesn't exist, this is likely the first time the
             * user has launched the app. Handle the exception gracefully by displaying a TextView
             * with instructions on how to use the SwipeRefreshLayout to reload the stop forecast.
             */
            File fileHasRunOnce =
                    new File(getActivity().getFilesDir().getPath() + "/" + FILE_HAS_RUN_ONCE);

            if (!fileHasRunOnce.exists()) {
                Log.i(LOG_TAG, "First time launching. Displaying SwipeRefreshLayout tutorial.");

                linearLayoutSwipeRefreshTutorial
                        = (LinearLayout) rootView.findViewById(
                        R.id.linearlayout_swipe_refresh_tutorial
                );
                linearLayoutSwipeRefreshTutorial.setVisibility(View.VISIBLE);

                // Create a new blank file to signify the user has run app at least once.
                if (fileHasRunOnce.createNewFile())
                    Log.i(LOG_TAG, "File created: " + FILE_HAS_RUN_ONCE);
                else
                    Log.e(LOG_TAG, "Failed to write file: " + FILE_HAS_RUN_ONCE);
            }
        } catch (IOException ioe) {
            Log.e(LOG_TAG, Log.getStackTraceString(ioe));
        }
    }

    /**
     * Set the current tab and the position of the Spinner.
     */
    private void setTabAndSpinner() {
        String[] redLineArrayStops = getResources().getStringArray(R.array.red_line_array_stops);
        String[] greenLineArrayStops = getResources().getStringArray(R.array.green_line_array_stops);

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
     * @param reloadTimeMillis The period (ms) after which the stop forecast should reload.
     */
    private void autoReloadStopForecast(int delayTimeMillis, int reloadTimeMillis) {
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
                                    Log.e(LOG_TAG, "Invalid line specified.");
                            }
                        }
                    });
                }
            }
        };

        // Schedule the auto-reload task to run.
        new Timer().schedule(timerTaskReload, delayTimeMillis, reloadTimeMillis);
    }

    /**
     * Load the stop forecast for a particular stop.
     * @param stopName The stop for which to load a stop forecast.
     */
    public void loadStopForecast(String stopName) {
        currentTab = tabHost.getCurrentTabTag();

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
                };

                tableRowOutboundStops = new TableRow[] {
                        (TableRow) rootView.findViewById(
                                R.id.red_line_tablerow_outbound_stop1),
                        (TableRow) rootView.findViewById(
                                R.id.red_line_tablerow_outbound_stop2),
                        (TableRow) rootView.findViewById(
                                R.id.red_line_tablerow_outbound_stop3),
                };

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

                initStopForecastOnClickListeners(RED_LINE);

                break;

            case GREEN_LINE:
                tableRowInboundStops = new TableRow[] {
                        (TableRow) rootView.findViewById(
                                R.id.green_line_tablerow_inbound_stop1),
                        (TableRow) rootView.findViewById(
                                R.id.green_line_tablerow_inbound_stop2),
                        (TableRow) rootView.findViewById(
                                R.id.green_line_tablerow_inbound_stop3),
                };

                tableRowOutboundStops = new TableRow[] {
                        (TableRow) rootView.findViewById(
                                R.id.green_line_tablerow_outbound_stop1),
                        (TableRow) rootView.findViewById(
                                R.id.green_line_tablerow_outbound_stop2),
                        (TableRow) rootView.findViewById(
                                R.id.green_line_tablerow_outbound_stop3),
                };

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

                initStopForecastOnClickListeners(GREEN_LINE);

                break;

            default:
        }
    }

    private void initStopForecastOnClickListeners(String line) {
        localeDefault = Locale.getDefault().toString();
        final NotifyTimesMap mapNotifyTimes = new NotifyTimesMap(localeDefault, STOP_FORECAST);

        switch(line) {
            case RED_LINE:
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

                break;

            case GREEN_LINE:
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

                break;

            default:

        }
    }

    private void showNotifyTimeDialog(String direction, int index, NotifyTimesMap mapNotifyTimes) {
        localeDefault = Locale.getDefault().toString();

        switch (direction) {
            case INBOUND:
                notifyStopName = textViewInboundStopNames[index].getText().toString();
                notifyStopTimeStr = textViewInboundStopTimes[index].getText().toString();

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

                notifyStopTimeExpected = mapNotifyTimes.get(notifyStopTimeStr);

                new NotifyTimeDialog(getActivity()).show();

                break;

            case OUTBOUND:
                notifyStopName = textViewOutboundStopNames[index].getText().toString();
                notifyStopTimeStr = textViewOutboundStopTimes[index].getText().toString();

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

                notifyStopTimeExpected = mapNotifyTimes.get(notifyStopTimeStr);

                new NotifyTimeDialog(getActivity()).show();

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

    public static class NotifyTimesReceiver extends BroadcastReceiver {

        private final String LOG_TAG = NotifyTimesReceiver.class.getSimpleName();

        @Override
        public void onReceive(final Context context, Intent intent) {
            final int notifyTimeUserRequestedMins = intent.getIntExtra("notifyTime", 5);
            final int NOTIFY_TIME_SAFETY_NET_MILLIS = 30000;

            /*
             * Define when a user should be notified that their tram is on its way. To do this, we
             * simply take the number of minutes the tram is expected in and subtract the due time
             * the user has asked to be notified at.
             *
             * 1 minute can be the difference between missing and catching a tram. Always insert an
             * artificial 30 second "safety net".
             * Example: If the user has set a notification that should fire after 5 minutes, the
             *          notification will actually fire after 4.5 minutes.
             */
            int notifyDelayMillis =
                    (notifyStopTimeExpected - notifyTimeUserRequestedMins)
                            * 60000
                            - NOTIFY_TIME_SAFETY_NET_MILLIS;

            /*
             * If the notification time makes no sense, inform the user and don't proceed.
             */
            if (notifyDelayMillis < 0) {
                Toast.makeText(
                        context,
                        context.getResources().getString(R.string.notify_invalid_time),
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            /*
             * Inform user the notification has been scheduled successfully.
             */
            Toast.makeText(
                    context,
                    context.getResources().getString(R.string.notify_successful),
                    Toast.LENGTH_SHORT
            ).show();

            scheduleNotification(context, notifyTimeUserRequestedMins, notifyDelayMillis);
        }

        /**
         * Schedule notification for tram.
         * @param context Context.
         * @param notifyTimeUserRequestedMins Minutes before tram arrival the user has requested to
         *                                    be notified at.
         * @param notifyDelayMillis Milliseconds to wait before firing off notification.
         */
        private void scheduleNotification(Context context,
                                          final int notifyTimeUserRequestedMins,
                                          int notifyDelayMillis) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    /*
                     * Create a StringBuilder in order to format the notification message correctly.
                     * Start by adding a message telling the user their tram is expected in
                     * N.
                     */
                    StringBuilder stringBuilderContentText = new StringBuilder();
                    stringBuilderContentText.append(
                            context.getResources().getString(R.string.notification_tram_expected)
                    ).append(
                            Integer.toString(notifyTimeUserRequestedMins)
                    );

                    /*
                     * Append either "minutes" or "minute" depending on the time chosen.
                     */
                    if (notifyTimeUserRequestedMins > 1)
                        stringBuilderContentText.append(
                                context.getResources().getString(R.string.notification_minutes)
                        );
                    else
                        stringBuilderContentText.append(
                                context.getResources().getString(R.string.notification_minute)
                        );

                    /*
                     * Create the NotificationBuilder, setting an appropriate title and the message
                     * built in the StringBuilder. The default notification sound should be played
                     * and the device should vibrate twice for 1 second with a 1 second delay
                     * between them.
                     */
                    NotificationCompat.Builder notificationBuilder =
                            new NotificationCompat.Builder(context)
                                    .setSmallIcon(R.drawable.laag_logo)
                                    .setContentTitle(
                                            context.getResources().getString(
                                                    R.string.notification_title
                                            )
                                    )
                                    .setContentText(stringBuilderContentText.toString())
                                    .setVibrate(new long[] {100, 1000, 1000, 1000, 1000})
                                    .setSound(
                                            RingtoneManager.getDefaultUri(
                                                    RingtoneManager.TYPE_NOTIFICATION
                                            )
                                    );

                    /*
                     * Create a NotificationManager and display the notification to the user.
                     */
                    NotificationManager notificationManager =
                            (NotificationManager) context.getSystemService(
                                    Context.NOTIFICATION_SERVICE
                            );
                    notificationManager.notify(1, notificationBuilder.build());
                }
            };

            context.getApplicationContext().registerReceiver(
                    broadcastReceiver,
                    new IntentFilter("org.thecosmicfrog.luasataglance")
            );

            pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    new Intent("org.thecosmicfrog.luasataglance"),
                    0
            );

            alarmManager = (AlarmManager) context.getSystemService(
                    Context.ALARM_SERVICE
            );
            alarmManager.set(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + notifyDelayMillis, pendingIntent
            );
        }
    }

    public class FetchLuasTimes extends AsyncTask<String, Void, StopForecast> {

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
                clearStopForecast();

                /*
                 * Update UI elements specific to the tab currently selected.
                 */
                updateStopForecast(sf);

                // Stop the refresh animation.
                redLineSwipeRefreshLayout.setRefreshing(false);
                greenLineSwipeRefreshLayout.setRefreshing(false);
            }
        }

        private void updateStopForecast(StopForecast sf) {
            EnglishGaeilgeMap mapEnglishGaeilge = new EnglishGaeilgeMap();

            switch (currentTab) {
                case RED_LINE:
                    // If a valid stop forecast exists...
                    if (sf != null) {
                        if (sf.getMessage() != null) {
                            String message;

                            if (localeDefault.startsWith(GAEILGE)) {
                                message = getResources().getString(R.string.message_success);
                            } else {
                                message = sf.getMessage();
                            }

                            textViewMessageTitle =
                                    (TextView) rootView.findViewById(
                                            R.id.red_line_textview_message_title
                                    );

                            /*
                             * Change the color of the message title TextView depending on the
                             * status.
                             */
                            if (message.contains(
                                    getResources().getString(R.string.message_success)))
                                textViewMessageTitle.setBackgroundResource(R.color.message_success);
                            else
                                textViewMessageTitle.setBackgroundResource(R.color.message_error);

                            /*
                             * Set the status message from the server.
                             */
                            textViewMessage =
                                    (TextView) rootView.findViewById(
                                            R.id.red_line_textview_message
                                    );
                            textViewMessage.setText(message);
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
                                    if (i < 3) {
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
                                    if (i < 3) {
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
                        if (sf.getMessage() != null) {
                            String message;

                            if (localeDefault.startsWith(GAEILGE)) {
                                message = getResources().getString(R.string.message_success);
                            } else {
                                message = sf.getMessage();
                            }

                            textViewMessageTitle =
                                    (TextView) rootView.findViewById(
                                            R.id.green_line_textview_message_title
                                    );

                            /*
                             * Change the color of the message title TextView depending on the
                             * status.
                             */
                            if (message.contains(
                                    getResources().getString(R.string.message_success)))
                                textViewMessageTitle.setBackgroundResource(R.color.message_success);
                            else
                                textViewMessageTitle.setBackgroundResource(R.color.message_error);

                            /*
                             * Set the status message from the server.
                             */
                            textViewMessage =
                                    (TextView) rootView.findViewById(
                                            R.id.green_line_textview_message
                                    );
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
                                    if (i < 3) {
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
                                    if (i < 3) {
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
                    Log.e(LOG_TAG, "Unknown tab.");
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