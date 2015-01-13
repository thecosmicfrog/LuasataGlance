package org.thecosmicfrog.luasataglance;

import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.concurrent.TimeUnit;

public class WearMessageListenerService extends WearableListenerService {

    private final String LOG_TAG = WearMessageListenerService.class.getSimpleName();

    private static GoogleApiClient googleApiClient;

    private static final long CONNECTION_TIME_OUT_MS = 100;
    private static final String WEAR_PATH = "/wear";
    private String nodeId;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/mobile")) {
            nodeId = messageEvent.getSourceNodeId();
            Log.v(LOG_TAG, "Node ID of watch: " + nodeId);
            showToast(messageEvent.getPath());

            reply(WEAR_PATH);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void reply(final String path) {
        googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Wearable.API)
                .build();

        Log.v(LOG_TAG, "In reply()");
        Log.v(LOG_TAG, "Path: " + path);

        if (googleApiClient != null && !(googleApiClient.isConnected() || googleApiClient.isConnecting()))
            googleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);

        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(googleApiClient, nodeId, path, null).await();
        if (result.getStatus().isSuccess())
            Log.v(LOG_TAG, "Success sent to: " + nodeId);
//        googleApiClient.disconnect();
    }
}
