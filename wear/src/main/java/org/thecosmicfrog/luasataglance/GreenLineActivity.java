package org.thecosmicfrog.luasataglance;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class GreenLineActivity extends Activity {

    private final String LOG_TAG = GreenLineActivity.class.getSimpleName();

    private ArrayAdapter<CharSequence> greenLineAdapterStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_green_line);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                greenLineAdapterStop = ArrayAdapter.createFromResource(
                        getApplicationContext(),
                        R.array.green_line_stops_array,
                        R.layout.listview_stops
                );

                ListView listView = (ListView) stub.findViewById(
                        R.id.listview_green_line);
                listView.setAdapter(greenLineAdapterStop);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(
                            AdapterView<?> parent,View view, int position, long id) {
                        String stopName = greenLineAdapterStop.getItem(position).toString();

                        startActivity(new Intent(
                                        getApplicationContext(),
                                        StopForecastActivity.class)
                                        .putExtra("stopName", stopName)
                        );
                    }
                });
            }
        });
    }
}