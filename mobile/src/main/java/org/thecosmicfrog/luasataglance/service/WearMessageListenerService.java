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

package org.thecosmicfrog.luasataglance.service;

import android.util.Base64;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.thecosmicfrog.luasataglance.api.ApiMethods;
import org.thecosmicfrog.luasataglance.api.ApiTimes;
import org.thecosmicfrog.luasataglance.object.StopForecast;
import org.thecosmicfrog.luasataglance.object.StopNameIdMap;
import org.thecosmicfrog.luasataglance.object.Tram;
import org.thecosmicfrog.luasataglance.util.Auth;
import org.thecosmicfrog.luasataglance.util.Serializer;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class WearMessageListenerService extends WearableListenerService {

    private final String LOG_TAG = WearMessageListenerService.class.getSimpleName();

    private static GoogleApiClient googleApiClient;
    private static String localeDefault;
    private static StopNameIdMap mapStopNameId;

    private final String API_FORMAT = "json";
    private final String API_URL = "http://www.dublinked.ie/cgi-bin/rtpi";
    private static final long CONNECTION_TIME_OUT_MS = 100;
    private static final String WEAR_PATH = "/wear";
    private String nodeId;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialise correct locale.
        localeDefault = Locale.getDefault().toString();

        // Instantiate a new StopNameIdMap.
        mapStopNameId = new StopNameIdMap(localeDefault);

        googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Wearable.API)
                .build();

        Wearable.MessageApi.addListener(googleApiClient, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Wearable.MessageApi.removeListener(googleApiClient, this);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/mobile")) {
            nodeId = messageEvent.getSourceNodeId();

            String stopName = Serializer.deserialize(messageEvent.getData()).toString();

            if (stopName != null)
                fetchStopForecast(stopName);
        }
    }

    public void fetchStopForecast(String stopName) {
        /*
         * Dublinked is protected by HTTP Basic auth. Send the username and password as
         * part of the request. This username and password should not be important from a
         * security perspective, as the auth is just used as a simple rate limiter.
         */
        final String BASIC_AUTH =
                "Basic " + Base64.encodeToString(
                        (Auth.DUBLINKED_USER + ":" + Auth.DUBLINKED_PASS).getBytes(),
                        Base64.NO_WRAP
                );

        /*
         * Prepare Retrofit API call.
         */
        final RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(API_URL)
                .build();

        ApiMethods methods = restAdapter.create(ApiMethods.class);

        Callback callback = new Callback() {
            @Override
            public void success(Object o, Response response) {
                // Cast the returned Object to a usable ApiTimes object.
                ApiTimes apiTimes = (ApiTimes) o;

                // Then create a stop forecast with this data.
                final StopForecast stopForecast = createStopForecast(apiTimes);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        reply(WEAR_PATH, stopForecast);
                    }
                }).start();
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Log.e(LOG_TAG, "Failure during call to server.");

                /*
                 * If we get a message or a response from the server, there's likely an issue with
                 * the client request or the server's response itself.
                 */
                if (retrofitError.getMessage() != null)
                    Log.e(LOG_TAG, retrofitError.getMessage());

                if (retrofitError.getResponse() != null) {
                    Log.e(LOG_TAG, retrofitError.getResponse().getUrl());
                    Log.e(LOG_TAG, Integer.toString(retrofitError.getResponse().getStatus()));
                    Log.e(LOG_TAG, retrofitError.getResponse().getHeaders().toString());
                    Log.e(LOG_TAG, retrofitError.getResponse().getBody().toString());
                    Log.e(LOG_TAG, retrofitError.getResponse().getReason());
                }

                /*
                 * If we don't receive a message or response, we can still get an idea of what's
                 * going on by getting the "kind" of error.
                 */
                if (retrofitError.getKind() != null)
                    Log.e(LOG_TAG, retrofitError.getKind().toString());
            }
        };

        /*
         * Call API and get stop forecast from server.
         */
        methods.getStopForecast(
                BASIC_AUTH,
                API_FORMAT,
                mapStopNameId.get(stopName),
                callback
        );
    }

    /**
     * Create a usable stop forecast with the data returned from the server.
     * @param apiTimes ApiTimes object created by Retrofit, containing raw stop forecast data.
     * @return Usable stop forecast.
     */
    private StopForecast createStopForecast(ApiTimes apiTimes) {
        StopForecast stopForecast = new StopForecast();

        for (ApiTimes.Result result : apiTimes.getResults()) {
            Tram tram = new Tram(
                    // Strip out the annoying "LUAS " prefix from the destination.
                    result.destination.replace("LUAS ", ""),
                    result.direction,
                    result.duetime
            );

            switch(tram.getDirection()) {
                case "I":
                    stopForecast.addInboundTram(tram);

                    break;

                case "O":
                    stopForecast.addOutboundTram(tram);

                    break;

                default:
                    // If for some reason the direction doesn't make sense.
                    Log.e(LOG_TAG, "Invalid direction: " + tram.getDirection());
            }
        }

        stopForecast.setErrorMessage(apiTimes.getErrormessage());

        return stopForecast;
    }

    private void reply(final String path, final StopForecast sf) {
        googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Wearable.API)
                .build();

        if (googleApiClient != null &&
                !(googleApiClient.isConnected() || googleApiClient.isConnecting())) {
            googleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
        }

        MessageApi.SendMessageResult result =
                Wearable.MessageApi.sendMessage(
                        googleApiClient,
                        nodeId,
                        path,
                        Serializer.serialize(sf)
                ).await();

        if (result.getStatus().isSuccess())
            Log.i(LOG_TAG, "Return message sent to: " + nodeId);
    }
}
