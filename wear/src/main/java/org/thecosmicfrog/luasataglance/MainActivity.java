package org.thecosmicfrog.luasataglance;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

    private final String LOG_TAG = MainActivity.class.getSimpleName();

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
                buttonRedLine = (Button) stub.findViewById(R.id.button_red_line);
                buttonRedLine.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.v(LOG_TAG, "Red Line");
                        startActivity(new Intent(getApplicationContext(), RedLineActivity.class));
                    }
                });

                buttonGreenLine = (Button) stub.findViewById(R.id.button_green_line);
                buttonGreenLine.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.v(LOG_TAG, "Green Line");
                        startActivity(new Intent(getApplicationContext(), GreenLineActivity.class));
                    }
                });
            }
        });
    }
}