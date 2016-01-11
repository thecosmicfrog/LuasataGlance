/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2016 Aaron Hastings
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

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

    private final String LOG_TAG = FavouritesActivity.class.getSimpleName();
    private final String PATH_FAVOURITES_OPEN_APP_MOBILE = "/favourites_open_app_mobile";
    private final String PATH_FAVOURITES_FETCH_MOBILE = "/favourites_fetch_mobile";
    private final String PATH_FAVOURITES_FETCH_WEAR = "/favourites_fetch_wear";
    private final String REQUEST_FETCH_FAVOURITES = "fetch_favourites";
    private final String REQUEST_OPEN_MOBILE_APP = "open_favourites_activity";
    private final long CONNECTION_TIME_OUT_MS = 5000;

    private GoogleApiClient googleApiClient;
    private String nodeId;
    private WatchViewStub stub;
    private String shape;
    private ArrayAdapter<CharSequence> adapterFavouriteStops;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_favourites);

        initGoogleApiClient();

        /* Add the MessageListener. */
        Wearable.MessageApi.addListener(googleApiClient, this);

        retrieveDeviceNodeAndSendRequestToHost(REQUEST_FETCH_FAVOURITES);

        /* Load the screen shape from shared preferences. */
        shape = Preferences.loadScreenShape(getApplicationContext());

        stub = (WatchViewStub) findViewById(R.id.watch_view_stub);

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub watchViewStub) {
                ImageButton imageButtonFavouritesNoneSelected =
                        (ImageButton) findViewById(R.id.imagebutton_favourites_none_selected);
                imageButtonFavouritesNoneSelected.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        /*
                         * When the user taps the button, request the host device to open its app.
                         */
                        retrieveDeviceNodeAndSendRequestToHost(REQUEST_OPEN_MOBILE_APP);

                        /* Inform the user the mobile app is opening. */
                        Toast.makeText(
                                getApplicationContext(),
                                getResources().getString(
                                        R.string.favourites_opening_mobile
                                ),
                                Toast.LENGTH_LONG
                        ).show();

                        /* No point in leaving this Activity open. Finish up. */
                        finish();
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /* Remove the MessageListener. */
        Wearable.MessageApi.removeListener(googleApiClient, this);
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(PATH_FAVOURITES_FETCH_WEAR)) {
            /*
             * We've received a reply from the host. Create a new List and draw it to the screen.
             */
            List <CharSequence> listFavouriteStops =
                    (List<CharSequence>) Serializer.deserialize(messageEvent.getData());

            drawFavourites(listFavouriteStops);
        }
    }

    /**
     * Draw the List of favourite stops to the screen.
     * @param listFavouriteStops List of favourite stops from host device.
     */
    private void drawFavourites(final List<CharSequence> listFavouriteStops) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                /*
                 * Hide the loading circle.
                 */
                ProgressBar progressBarLoadingCircle =
                        (ProgressBar) findViewById(R.id.progressbar_loading_circle);
                progressBarLoadingCircle.setVisibility(View.GONE);

                /*
                 * If the list of Favourite stops is empty, display a message to the user.
                 */
                if (listFavouriteStops.size() <= 0) {
                    LinearLayout linearLayoutFavouritesNoneSelected =
                            (LinearLayout) findViewById(
                                    R.id.linearlayout_favourites_none_selected
                            );
                    linearLayoutFavouritesNoneSelected.setVisibility(View.VISIBLE);
                }

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

                listViewFavouriteStops.setAdapter(adapterFavouriteStops);
                listViewFavouriteStops.setOnItemClickListener(
                        new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position,
                                                    long id) {
                                /*
                                 * When a favourite stop is clicked, open the MainActivity, passing
                                 * the stop name as an extra parameter.
                                 */
                                String stopName =
                                        adapterFavouriteStops.getItem(position).toString();

                                startActivity(
                                        new Intent(
                                                getApplicationContext(),
                                                StopForecastActivity.class
                                        ).putExtra("stopName", stopName)
                                );
                            }
                        });
            }
        });
    }

    /**
     * Initialise Google API Client.
     */
    private void initGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
    }

    /**
     * Retrieve device node from connected device.
     */
    private void retrieveDeviceNodeAndSendRequestToHost(final String request) {
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

                    if (request.equals(REQUEST_FETCH_FAVOURITES))
                        requestFavouritesFromHost();
                    else if (request.equals(REQUEST_OPEN_MOBILE_APP))
                        openMobileApp();
                }
            }
        }).start();
    }

    /**
     * Send a message to the device host, requesting its List of Favourites.
     */
    private void requestFavouritesFromHost() {
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

                    /*
                     * This is just a message to the host to let it know we want its List of
                     * Favourites. As such, its message body can be empty.
                     */
                    MessageApi.SendMessageResult result =
                            Wearable.MessageApi.sendMessage(
                                    googleApiClient,
                                    nodeId,
                                    PATH_FAVOURITES_FETCH_MOBILE,
                                    Serializer.serialize("")
                            ).await();

                    if (result.getStatus().isSuccess())
                        Log.i(LOG_TAG, "Success sent to: " + nodeId);
                }
            }).start();
        }
    }

    /**
     * Request Luas at a Glance mobile app to open itself.
     */
    private void openMobileApp() {
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

                    /*
                     * This is just a message to the host to let it know we want to open the
                     * Favourites activity. As such, its message body can be empty.
                     */
                    MessageApi.SendMessageResult result =
                            Wearable.MessageApi.sendMessage(
                                    googleApiClient,
                                    nodeId,
                                    PATH_FAVOURITES_OPEN_APP_MOBILE,
                                    Serializer.serialize("")
                            ).await();

                    if (result.getStatus().isSuccess())
                        Log.i(LOG_TAG, "Success sent to: " + nodeId);
                }
            }).start();
        }
    }
}
