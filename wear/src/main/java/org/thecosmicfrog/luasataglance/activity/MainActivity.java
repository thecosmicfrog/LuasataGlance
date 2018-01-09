/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2018 Aaron Hastings
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
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageButton;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.util.Preferences;

import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private final long CONNECTION_TIME_OUT_MS = 5000;

    private GoogleApiClient googleApiClient;
    private Button buttonRedLine;
    private Button buttonGreenLine;
    private ImageButton imageButtonFavourites;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);

        /*
         * Set up an OnApplyWindowInsetsListener so that we know what shape the wearable's screen
         * is (round or square). Save this value to shared preferences.
         */
        stub.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                stub.onApplyWindowInsets(insets);

                if (insets.isRound())
                    Preferences.saveScreenShape(getApplicationContext(), "round");
                else
                    Preferences.saveScreenShape(getApplicationContext(), "square");

                return insets;
            }
        });

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

                imageButtonFavourites =
                        (ImageButton) stub.findViewById(R.id.imagebutton_favourites);
                imageButtonFavourites.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(
                                new Intent(getApplicationContext(), FavouritesActivity.class)
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
