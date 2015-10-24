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

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.object.StopForecast;
import org.thecosmicfrog.luasataglance.util.Serializer;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class StopForecastActivity extends Activity implements MessageApi.MessageListener {

    private static final long CONNECTION_TIME_OUT_MS = 100;
    private static final String MOBILE_PATH = "/mobile";

    private final String LOG_TAG = StopForecastActivity.class.getSimpleName();

    private GoogleApiClient googleApiClient;
    private String nodeId;
    private WatchViewStub stub;
    private ProgressBar progressBarLoadingCircle;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView textViewStopName;
    private TextView[] textViewInboundStopNames;
    private TextView[] textViewInboundStopTimes;
    private TextView[] textViewOutboundStopNames;
    private TextView[] textViewOutboundStopTimes;
    private TimerTask timerTaskReload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stop_forecast);

        initGoogleApiClient();

        // Add the MessageListener.
        Wearable.MessageApi.addListener(googleApiClient, this);

        stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                /*
                 * Create arrays of TextView objects for each entry in the TableLayout.
                 */
                textViewInboundStopNames = new TextView[]{
                        (TextView) findViewById(R.id.textview_inbound_stop1_name),
                        (TextView) findViewById(R.id.textview_inbound_stop2_name),
                };

                textViewInboundStopTimes = new TextView[]{
                        (TextView) findViewById(R.id.textview_inbound_stop1_time),
                        (TextView) findViewById(R.id.textview_inbound_stop2_time),
                };

                textViewOutboundStopNames = new TextView[]{
                        (TextView) findViewById(R.id.textview_outbound_stop1_name),
                        (TextView) findViewById(R.id.textview_outbound_stop2_name),
                };

                textViewOutboundStopTimes = new TextView[]{
                        (TextView) findViewById(R.id.textview_outbound_stop1_time),
                        (TextView) findViewById(R.id.textview_outbound_stop2_time),
                };

                clearStopForecast();

                if (getIntent().hasExtra("stopName")) {
                    progressBarLoadingCircle =
                            (ProgressBar) findViewById(R.id.progressbar_loading_circle);
                    setIsLoading(false);

                    swipeRefreshLayout =
                            (SwipeRefreshLayout) findViewById(R.id.swiperefreshlayout);
                    swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                        @Override
                        public void onRefresh() {
                            // Start the refresh animation.
                            swipeRefreshLayout.setRefreshing(true);

                            requestStopTimesFromHostDevice(getIntent().getStringExtra("stopName"));
                        }
                    });

                    textViewStopName = (TextView) findViewById(R.id.textview_stop_name);
                    textViewStopName.setText(getIntent().getStringExtra("stopName"));
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        /*
         * Stop the auto-reload TimerTask so as to prevent multiple TimerTasks running each time
         * the Activity is started.
         */
        timerTaskReload.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         * Reload stop forecast every 3 seconds.
         * Induce 3 second delay if app is launching from cold start (timerTaskReload == null) in
         * order to prevent two HTTP requests in rapid succession.
         */
        if (timerTaskReload == null)
            autoReloadStopForecast(3000, 3000);
        else
            autoReloadStopForecast(0, 3000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /*
         * Stop the auto-reload TimerTask so as to prevent multiple TimerTasks running each time
         * the Activity is started.
         */
        timerTaskReload.cancel();

        // Remove the MessageListener.
        Wearable.MessageApi.removeListener(googleApiClient, this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            setIsLoading(true);

            requestStopTimesFromHostDevice(getIntent().getStringExtra("stopName"));
        }
    }

    /**
     * Clear the stop forecast displayed in the current tab.
     */
    private void clearStopForecast() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 2; i++) {
                    textViewInboundStopNames[i].setText("");
                    textViewInboundStopTimes[i].setText("");

                    textViewOutboundStopNames[i].setText("");
                    textViewOutboundStopTimes[i].setText("");
                }
            }
        });
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setIsLoading(true);

                        requestStopTimesFromHostDevice(getIntent().getStringExtra("stopName"));
                    }
                });
            }
        };

        // Schedule the auto-reload task to run.
        new Timer().schedule(timerTaskReload, delayTimeMillis, reloadTimeMillis);
    }

    /**
     * Make progress circle spin or not spin.
     * Must run on UI thread as only this thread can change view. This is achieved using the
     * runOnUiThread() method. Parameters must be final due to Java scope restrictions.
     * @param loading Whether or not progress circle should spin.
     */
    private void setIsLoading(final boolean loading) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (loading)
                    progressBarLoadingCircle.setVisibility(View.VISIBLE);
                else
                    progressBarLoadingCircle.setVisibility(View.INVISIBLE);
            }
        });
    }

    /**
     * Request host device to query server for stop forecast.
     * @param stopName Stop to query forecast for.
     */
    private void requestStopTimesFromHostDevice(final String stopName) {
        if (nodeId != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (googleApiClient != null &&
                            !(googleApiClient.isConnected() || googleApiClient.isConnecting())) {
                        Log.i(LOG_TAG, "Connecting...");

                        googleApiClient.blockingConnect(
                                CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS
                        );
                    }

                    MessageApi.SendMessageResult result =
                            Wearable.MessageApi.sendMessage(
                                    googleApiClient,
                                    nodeId,
                                    MOBILE_PATH,
                                    Serializer.serialize(stopName)
                            ).await();

                    if (result.getStatus().isSuccess())
                        Log.i(LOG_TAG, "Success sent to: " + nodeId);
                }
            }).start();
        }
    }

    /**
     * Initialise Google API Client.
     */
    private void initGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        retrieveDeviceNode();
    }

    /**
     * Retrieve device node from connected device.
     */
    private void retrieveDeviceNode() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (googleApiClient != null &&
                        !(googleApiClient.isConnected() || googleApiClient.isConnecting())) {
                    Log.i(LOG_TAG, "Google API Client connecting...");

                    googleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                }

                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(googleApiClient).await();

                List<Node> nodes = result.getNodes();

                if (nodes.size() > 0)
                    nodeId = nodes.get(0).getId();
            }
        }).start();
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/wear")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    StopForecast stopForecast =
                            (StopForecast) Serializer.deserialize(messageEvent.getData());

                    // If a valid stop forecast exists...
                    if (stopForecast != null) {
                        if (stopForecast.getMessage().contains(
                                getString(R.string.message_success))) {
                            /*
                             * No error message on server. Change the message title TextView to
                             * green and set a default success message.
                             */
                            TextView textViewStopName =
                                    (TextView) findViewById(R.id.textview_stop_name);
                                textViewStopName.setBackgroundResource(R.color.message_success);
                        } else {
                            /*
                             * To make best use of the wearable's screen real estate, re-use one of
                             * the inbound stop TextViews for the status message.
                             */
                            TextView textViewStopName =
                                    (TextView) findViewById(R.id.textview_stop_name);
                            textViewStopName.setBackgroundResource(R.color.message_error);

                            TextView textViewInboundStop1Name =
                                    (TextView) findViewById(R.id.textview_inbound_stop1_name);
                            textViewInboundStop1Name.setText(stopForecast.getMessage());
                        }

                        /*
                         * Pull in all trams from the StopForecast, but only display up to two
                         * inbound and outbound trams. Start by clearing the TextViews.
                         */
                        for (int i = 0; i < 2; i++) {
                            textViewInboundStopNames[i].setText("");
                            textViewInboundStopTimes[i].setText("");

                            textViewOutboundStopNames[i].setText("");
                            textViewOutboundStopTimes[i].setText("");
                        }

                        if (stopForecast.getInboundTrams() != null) {
                            if (stopForecast.getInboundTrams().size() == 0) {
                                textViewInboundStopNames[0].setText(R.string.no_trams_forecast);
                            } else {
                                for (int i = 0; i < stopForecast.getInboundTrams().size(); i++) {
                                    if (i < 2) {
                                        textViewInboundStopNames[i].setText(
                                                stopForecast.getInboundTrams()
                                                        .get(i)
                                                        .getDestination()
                                        );

                                        if (stopForecast.getInboundTrams()
                                                .get(i).getDueMinutes().equalsIgnoreCase("DUE")) {
                                            textViewInboundStopTimes[i].setText(
                                                    stopForecast.getInboundTrams()
                                                            .get(i)
                                                            .getDueMinutes()
                                            );
                                        } else {
                                            textViewInboundStopTimes[i].setText(
                                                    stopForecast.getInboundTrams()
                                                            .get(i)
                                                            .getDueMinutes()  + "m"
                                            );
                                        }
                                    }
                                }
                            }
                        }

                        if (stopForecast.getOutboundTrams() != null) {
                            if (stopForecast.getOutboundTrams().size() == 0) {
                                textViewOutboundStopNames[0].setText(R.string.no_trams_forecast);
                            } else {
                                for (int i = 0; i < stopForecast.getOutboundTrams().size(); i++) {
                                    if (i < 2) {
                                        textViewOutboundStopNames[i].setText(
                                                stopForecast.getOutboundTrams()
                                                        .get(i)
                                                        .getDestination()
                                        );

                                        if (stopForecast.getOutboundTrams()
                                                .get(i).getDueMinutes().equalsIgnoreCase("DUE")) {
                                            textViewOutboundStopTimes[i].setText(
                                                    stopForecast.getOutboundTrams()
                                                            .get(i)
                                                            .getDueMinutes()
                                            );
                                        } else {
                                            textViewOutboundStopTimes[i].setText(
                                                    stopForecast.getOutboundTrams()
                                                            .get(i)
                                                            .getDueMinutes()  + "m"
                                            );
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Stop the refresh animations.
                    swipeRefreshLayout.setRefreshing(false);
                    setIsLoading(false);
                }
            });
        }
    }
}
