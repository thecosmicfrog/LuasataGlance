package org.thecosmicfrog.luasataglance;

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

import java.util.List;
import java.util.concurrent.TimeUnit;

public class StopForecastActivity extends Activity implements MessageApi.MessageListener {

    private final String LOG_TAG = StopForecastActivity.class.getSimpleName();

    private static final long CONNECTION_TIME_OUT_MS = 100;
    private static final String MOBILE_PATH = "/mobile";

    private GoogleApiClient googleApiClient;
    private String nodeId;

    private WatchViewStub stub;

    private ProgressBar progressBarLoadingCircle;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView textViewStopName;

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
    protected void onDestroy() {
        super.onDestroy();

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
     * Make progress circle spin or not spin.
     * Must run on UI thread as only this thread can change views. This is achieved using the
     * runOnUiThread() method. Parameters must be final due to Java scope restrictions.
     * @param loading Whether or not progress circle should spin.
     */
    public void setIsLoading(final boolean loading) {
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

    private void initGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        retrieveDeviceNode();
    }

    private void retrieveDeviceNode() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (googleApiClient != null &&
                        !(googleApiClient.isConnected() || googleApiClient.isConnecting())) {
                    Log.i(LOG_TAG, "Connecting...");
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
                    StopForecast sf =
                            (StopForecast) Serializer.deserialize(messageEvent.getData());

                    if (sf != null) {
                        if (sf.getMessage() != null) {
                            /*
                             * Change the color of the stop name TextView depending on the status.
                             */
                            TextView textViewStopName =
                                    (TextView) findViewById(R.id.textview_stop_name);

                            if (sf.getMessage().equals(
                                    getResources().getString(R.string.message_success)))
                                textViewStopName.setBackgroundResource(R.color.message_success);
                            else
                                textViewStopName.setBackgroundResource(R.color.message_error);

                            /*
                             * To make best use of the wearable's screen real estate, re-use one of
                             * the inbound stop TextViews for the status message.
                             */
                            TextView textViewInboundStop1Name =
                                    (TextView) findViewById(R.id.textview_inbound_stop1_name);
                            textViewInboundStop1Name.setText(sf.getMessage());
                        }

                        /*
                         * Create arrays of TextView objects for each entry in the TableLayout.
                         */
                        TextView[] textViewInboundStopNames = new TextView[]{
                                (TextView) findViewById(R.id.textview_inbound_stop1_name),
                                (TextView) findViewById(R.id.textview_inbound_stop2_name),
                        };

                        TextView[] textViewInboundStopTimes = new TextView[]{
                                (TextView) findViewById(R.id.textview_inbound_stop1_time),
                                (TextView) findViewById(R.id.textview_inbound_stop2_time),
                        };

                        TextView[] textViewOutboundStopNames = new TextView[]{
                                (TextView) findViewById(R.id.textview_outbound_stop1_name),
                                (TextView) findViewById(R.id.textview_outbound_stop2_name),
                        };

                        TextView[] textViewOutboundStopTimes = new TextView[]{
                                (TextView) findViewById(R.id.textview_outbound_stop1_time),
                                (TextView) findViewById(R.id.textview_outbound_stop2_time),
                        };

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

                        if (sf.getInboundTrams() != null) {
                            for (int i = 0; i < sf.getInboundTrams().size(); i++) {
                                if (i < 2) {
                                    textViewInboundStopNames[i].setText(
                                            sf.getInboundTrams().get(i).getDestination()
                                    );

                                    if (sf.getInboundTrams()
                                            .get(i).getDueMinutes().equalsIgnoreCase("DUE")) {
                                        textViewInboundStopTimes[i].setText(
                                                sf.getInboundTrams().get(i).getDueMinutes()
                                        );
                                    } else {
                                        textViewInboundStopTimes[i].setText(
                                                sf.getInboundTrams().get(i).getDueMinutes()  + "m"
                                        );
                                    }
                                }
                            }
                        }

                        if (sf.getOutboundTrams() != null) {
                            for (int i = 0; i < sf.getOutboundTrams().size(); i++) {
                                if (i < 2) {
                                    textViewOutboundStopNames[i].setText(
                                            sf.getOutboundTrams().get(i).getDestination()
                                    );

                                    if (sf.getOutboundTrams()
                                            .get(i).getDueMinutes().equalsIgnoreCase("DUE")) {
                                        textViewOutboundStopTimes[i].setText(
                                                sf.getOutboundTrams().get(i).getDueMinutes()
                                        );
                                    } else {
                                        textViewOutboundStopTimes[i].setText(
                                                sf.getOutboundTrams().get(i).getDueMinutes()  + "m"
                                        );
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