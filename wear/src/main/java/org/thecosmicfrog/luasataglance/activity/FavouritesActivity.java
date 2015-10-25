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
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.util.Preferences;
import org.thecosmicfrog.luasataglance.util.Serializer;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class FavouritesActivity extends Activity implements MessageApi.MessageListener {

    private static final long CONNECTION_TIME_OUT_MS = 100;

    private final String LOG_TAG = FavouritesActivity.class.getSimpleName();
    private final String PATH_FAVOURITES_MOBILE = "/favourites_mobile";
    private final String PATH_FAVOURITES_WEAR = "/favourites_wear";

    private GoogleApiClient googleApiClient;
    private String nodeId;
    private WatchViewStub stub;
    private String shape;
    private ArrayAdapter<CharSequence> adapterFavouriteStops;
    private List<CharSequence> listFavouriteStops;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_favourites);

        initGoogleApiClient();

        retrieveDeviceNode();

        // Add the MessageListener.
        Wearable.MessageApi.addListener(googleApiClient, this);

        // Load the screen shape from shared preferences.
        shape = Preferences.loadScreenShape(getApplicationContext());

        stub = (WatchViewStub) findViewById(R.id.watch_view_stub);

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        initGoogleApiClient();

        retrieveDeviceNode();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Wearable.MessageApi.removeListener(googleApiClient, this);
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(PATH_FAVOURITES_WEAR)) {
            drawFavourites(messageEvent);
        }
    }

    private void drawFavourites(final MessageEvent messageEvent) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listFavouriteStops = (List<CharSequence>) Serializer.deserialize(messageEvent.getData());

                /*
                 * ArrayAdapter for favourite stops.
                 * Alter the layout depending on the screen shape.
                 */
                int layoutListViewFavourites;

                if (shape.equals("round"))
                    layoutListViewFavourites = R.layout.round_listview_favourites;
                else
                    layoutListViewFavourites = R.layout.rect_listview_favourites;

                adapterFavouriteStops = new ArrayAdapter<>(
                        getApplicationContext(),
                        layoutListViewFavourites,
                        listFavouriteStops
                );

                /*
                 * Populate ListView with the user's favourite stops, as read from file.
                 */
                ListView listViewFavouriteStops = (ListView) stub.findViewById(
                        R.id.listview_favourite_stops
                );

                if (listFavouriteStops != null) {
                    listViewFavouriteStops.setAdapter(adapterFavouriteStops);
                    listViewFavouriteStops.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            /*
                             * When a favourite stop is clicked, open the MainActivity, passing the stop
                             * name as an extra parameter.
                             */
                            String stopName = adapterFavouriteStops.getItem(position).toString();

                            startActivity(
                                    new Intent(
                                            getApplicationContext(),
                                            StopForecastActivity.class
                                    ).putExtra("stopName", stopName)
                            );
                        }
                    });
                }
            }
        });
    }

    private void initGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
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

                if (nodes.size() > 0) {
                    nodeId = nodes.get(0).getId();

                    requestFavouritesFromDevice();

                }
            }
        }).start();
    }

    private void requestFavouritesFromDevice() {
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
                                    PATH_FAVOURITES_MOBILE,
                                    Serializer.serialize("")
                            ).await();

                    if (result.getStatus().isSuccess())
                        Log.i(LOG_TAG, "Success sent to: " + nodeId);
                }
            }).start();
        }
    }
}
