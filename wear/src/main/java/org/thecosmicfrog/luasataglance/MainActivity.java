package org.thecosmicfrog.luasataglance;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final long CONNECTION_TIME_OUT_MS = 100;

    private GoogleApiClient googleApiClient;

    private Button buttonRedLine;
    private Button buttonGreenLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                initGoogleApiClient();

                buttonRedLine = (Button) stub.findViewById(R.id.button_red_line);
                buttonRedLine.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(
                                new Intent(getApplicationContext(),
                                        RedLineActivity.class
                                )
                        );
                    }
                });

                buttonGreenLine = (Button) stub.findViewById(R.id.button_green_line);
                buttonGreenLine.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(getApplicationContext(), GreenLineActivity.class));
                    }
                });
            }
        });
    }

    private void initGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (googleApiClient != null &&
                        !(googleApiClient.isConnected() || googleApiClient.isConnecting())) {
                    Log.i(LOG_TAG, "Connecting...");
                    googleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                }
            }
        }).start();
    }
}