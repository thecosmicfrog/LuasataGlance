package org.thecosmicfrog.luasataglance;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends Activity {

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private TextView textViewRedLine;
    private TextView textViewGreenLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                textViewRedLine = (TextView) stub.findViewById(R.id.textview_red_line);
                textViewRedLine.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.v(LOG_TAG, "Red Line");
                    }
                });

                textViewGreenLine = (TextView) stub.findViewById(R.id.textview_green_line);
                textViewGreenLine.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.v(LOG_TAG, "Green Line");
                    }
                });
            }
        });
    }
}

// Spinner code...
//final Spinner redLineSpinnerStop = (Spinner) findViewById(R.id.spinner_stop);
//final ArrayAdapter<CharSequence> redLineAdapterStop = ArrayAdapter.createFromResource(
//        getApplicationContext(), R.array.red_line_stops_array, R.layout.spinner_stops
//);
//redLineAdapterStop.setDropDownViewResource(R.layout.spinner_stops);
//redLineSpinnerStop.setAdapter(redLineAdapterStop);