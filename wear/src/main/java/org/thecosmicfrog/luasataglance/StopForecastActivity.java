package org.thecosmicfrog.luasataglance;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stop_forecast);

        initGoogleApiClient();
        Wearable.MessageApi.addListener(googleApiClient, this);


        stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                initActivity();
            }
        });
    }

    private void initActivity() {
        if (getIntent().hasExtra("stopName"))
            Log.v(LOG_TAG, "stopName: " + getIntent().getStringExtra("stopName"));
    }

    private void sendMessage() {
        if (nodeId != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (googleApiClient != null && !(googleApiClient.isConnected() || googleApiClient.isConnecting()))
                        googleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);

                    Wearable.MessageApi.sendMessage(googleApiClient, nodeId, MOBILE_PATH, null).await();
                }
            }).start();
        }
    }

    private void initGoogleApiClient() {
        googleApiClient = getGoogleApiClient(this);
        retrieveDeviceNode();
    }

    private GoogleApiClient getGoogleApiClient(Context context) {
        return new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
    }

    private void retrieveDeviceNode() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (googleApiClient != null && !(googleApiClient.isConnected() || googleApiClient.isConnecting())) {
                    Log.v(LOG_TAG, "Connecting...");
                    googleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                }

                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(googleApiClient).await();

                List<Node> nodes = result.getNodes();

                for (Node node : nodes) {
                    Log.v(LOG_TAG, "Node: " + node.toString());
                }

                if (nodes.size() > 0)
                    nodeId = nodes.get(0).getId();

                Log.v(LOG_TAG, "Node ID of phone: " + nodeId);

//                googleApiClient.disconnect();
            }
        }).start();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/wear")) {
            TextView textView = (TextView) findViewById(R.id.textview_inbound_stop1_name);
            textView.setText("I'm working!");
        }
    }
}
