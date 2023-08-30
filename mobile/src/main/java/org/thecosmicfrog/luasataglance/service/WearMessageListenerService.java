/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2019 Aaron Hastings
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

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.thecosmicfrog.luasataglance.activity.MainActivity;
import org.thecosmicfrog.luasataglance.api.ApiMethods;
import org.thecosmicfrog.luasataglance.api.ApiTimes;
import org.thecosmicfrog.luasataglance.object.StopForecast;
import org.thecosmicfrog.luasataglance.object.StopNameIdMap;
import org.thecosmicfrog.luasataglance.object.Tram;
import org.thecosmicfrog.luasataglance.util.Constant;
import org.thecosmicfrog.luasataglance.util.Serializer;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static org.thecosmicfrog.luasataglance.util.Constant.PATH_FAVOURITES_FETCH_WEAR;

public class WearMessageListenerService extends WearableListenerService {

    private final String LOG_TAG = WearMessageListenerService.class.getSimpleName();
    private final long CONNECTION_TIME_OUT_MS = 5000;

    private static GoogleApiClient googleApiClient;
    private static StopNameIdMap mapStopNameId;

    private String nodeId;

    @Override
    public void onCreate() {
        super.onCreate();

        /* Initialise correct locale. */
        String localeDefault = Locale.getDefault().toString();

        /* Instantiate a new StopNameIdMap. */
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
        if (messageEvent.getPath().equals(Constant.PATH_FAVOURITES_FETCH_MOBILE)) {
            nodeId = messageEvent.getSourceNodeId();

            List<CharSequence> listFavouriteStops = getListFavouriteStops();

            replyFavourites(PATH_FAVOURITES_FETCH_WEAR, listFavouriteStops);
        }

        if (messageEvent.getPath().equals(Constant.PATH_FAVOURITES_OPEN_APP_MOBILE)) {
            nodeId = messageEvent.getSourceNodeId();

            startActivity(
                    new Intent(
                            this,
                            MainActivity.class
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            );
        }

        if (messageEvent.getPath().equals(Constant.PATH_STOPFORECAST_FETCH_MOBILE)) {
            nodeId = messageEvent.getSourceNodeId();

            String stopName = Serializer.deserialize(messageEvent.getData()).toString();

            if (stopName != null)
                fetchStopForecast(stopName);
        }
    }

    private void fetchStopForecast(String stopName) {
        final String API_URL = "https://api.thecosmicfrog.org/cgi-bin";
        final String API_ACTION = "times";
        final String API_VER = "2";

        /*
         * Prepare Retrofit API call.
         */
        final RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(API_URL)
                .build();

        ApiMethods methods = restAdapter.create(ApiMethods.class);

        Callback<ApiTimes> callback = new Callback<ApiTimes>() {
            @Override
            public void success(ApiTimes apiTimes, Response response) {
                /* If the server returned times. */
                if (apiTimes != null) {
                    /* Then create a stop forecast with this data. */
                    final StopForecast stopForecast = createStopForecast(apiTimes);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            replyStopForecast(Constant.PATH_STOPFORECAST_FETCH_WEAR, stopForecast);
                        }
                    }).start();
                }
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
                    if (retrofitError.getResponse().getUrl() != null) {
                        Log.e(LOG_TAG, retrofitError.getResponse().getUrl());
                    }

                    Log.e(LOG_TAG, Integer.toString(retrofitError.getResponse().getStatus()));

                    if (retrofitError.getResponse().getHeaders() != null) {
                        Log.e(LOG_TAG, retrofitError.getResponse().getHeaders().toString());
                    }

                    if (retrofitError.getResponse().getBody() != null) {
                        Log.e(LOG_TAG, retrofitError.getResponse().getBody().toString());
                    }

                    if (retrofitError.getResponse().getReason() != null) {
                        Log.e(LOG_TAG, retrofitError.getResponse().getReason());
                    }
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
                API_ACTION,
                API_VER,
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

        if (apiTimes.getTrams() != null) {
            for (Tram tram : apiTimes.getTrams()) {
                switch (tram.getDirection()) {
                    case "Inbound":
                        stopForecast.addInboundTram(tram);

                        break;

                    case "Outbound":
                        stopForecast.addOutboundTram(tram);

                        break;

                    default:
                        /* If for some reason the direction doesn't make sense. */
                        Log.e(LOG_TAG, "Invalid direction: " + tram.getDirection());
                }
            }
        }

        stopForecast.setMessage(apiTimes.getMessage());

        return stopForecast;
    }

    private void replyFavourites(final String path, List<CharSequence> listFavouriteStops) {
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
                        Serializer.serialize(listFavouriteStops)
                ).await();

        if (result.getStatus().isSuccess())
            Log.i(LOG_TAG, "Return message sent to: " + nodeId);
    }

    private void replyStopForecast(final String path, final StopForecast sf) {
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

    private List<CharSequence> getListFavouriteStops() {
        final String FILE_FAVOURITES = "favourites";

        try {
            /*
             * Open input objects.
             */
            InputStream fileInput = openFileInput(FILE_FAVOURITES);
            InputStream buffer = new BufferedInputStream(fileInput);
            ObjectInput objectInput = new ObjectInputStream(buffer);

            /*
             * Read in List of favourite stops from file.
             */
            @SuppressWarnings("unchecked")
            List<CharSequence> listFavouriteStops = (List<CharSequence>) objectInput.readObject();

            /*
             * Close input objects.
             */
            objectInput.close();
            buffer.close();
            fileInput.close();

            return listFavouriteStops;
        } catch (ClassNotFoundException | FileNotFoundException e) {
            /*
             * If the favourites file doesn't exist, the user has probably not set up this
             * feature yet. Handle the exception gracefully by displaying a TextView with
             * instructions on how to add favourites.
             */
            Log.i(LOG_TAG, "Favourites not yet set up.");
        } catch (IOException e) {
            /*
             * Something has gone wrong; the file may have been corrupted. Delete the file.
             */
            Log.e(LOG_TAG, Log.getStackTraceString(e));
        }

        /* Something has gone wrong. Return an empty ArrayList. */
        return new ArrayList<>();
    }
}
