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
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.thecosmicfrog.luasataglance.R;

public class GreenLineActivity extends Activity {

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
                        R.array.green_line_array_stops,
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