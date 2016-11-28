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

import android.content.Intent;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.exception.StopMarkerNotFoundException;
import org.thecosmicfrog.luasataglance.object.StopCoords;
import org.thecosmicfrog.luasataglance.util.Constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private final String LOG_TAG = MapsActivity.class.getSimpleName();

    private GoogleMap map;
    private double[][] stopCoordsRedLine;
    private double[][] stopCoordsGreenLine;
    private List<Marker> listMarkers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        listMarkers = new ArrayList<>();

        stopCoordsRedLine = new StopCoords(Constant.RED_LINE).getStopCoords();
        stopCoordsGreenLine = new StopCoords(Constant.GREEN_LINE).getStopCoords();

        /*
         * If the user is on Lollipop or above, use a Material Dialog theme. Otherwise, fall back to
         * the default theme set in AndroidManifest.xml.
         */
        if (Build.VERSION.SDK_INT >= 21)
            setTheme(android.R.style.Theme_Material_Dialog);

        /* Workaround for annoying bug that causes Google Maps in a Dialog to be dimmed. */
        if (Build.VERSION.SDK_INT <= 22)
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        /* This is a Dialog. Get rid of the default Window title. */
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_maps);

        /* Obtain the SupportMapFragment and get notified when the map is ready to be used. */
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        /* Set the default Camera position and zoom. */
        map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(new LatLng(53.34167328, -6.265131), 12.0f)
        );

        String[] stopNamesRedLine = getResources().getStringArray(
                R.array.array_stops_redline
        );
        String[] stopNamesGreenLine = getResources().getStringArray(
                R.array.array_stops_greenline
        );

        List<String> listStopNamesRedLine = new ArrayList<>(Arrays.asList(stopNamesRedLine));
        List<String> listStopNamesGreenLine = new ArrayList<>(Arrays.asList(stopNamesGreenLine));

        /* Remove the two "Select a stop..." entries from the List. */
        listStopNamesRedLine.remove(getString(R.string.select_a_stop));
        listStopNamesGreenLine.remove(getString(R.string.select_a_stop));

        /* Compile a List of all stops. */
        List<String> listStopNamesAll = new ArrayList<>(listStopNamesRedLine);
        listStopNamesAll.addAll(listStopNamesGreenLine);

        /* Draw map Markers. */
        drawMarkers(listStopNamesRedLine, listStopNamesGreenLine);

        /* Draw Polylines between Markers. */
        drawPolylines(googleMap, listStopNamesRedLine, listStopNamesGreenLine);

        /*
         * When a user taps on a stop's info window, it should open the appropriate stop forecast.
         */
        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                startActivity(
                        new Intent(
                                getApplicationContext(),
                                MainActivity.class
                        ).putExtra(Constant.STOP_NAME, marker.getTitle())
                );
            }
        });

        /*
         * Move the Camera to the position of the stop that this Activity was opened from.
         * Also, open the Marker's info window.
         */
        if (getIntent().hasExtra(Constant.STOP_NAME)) {
            try {
                Marker marker = findStopMarker(getIntent().getStringExtra(Constant.STOP_NAME));

                map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 14.0f));

                marker.showInfoWindow();
            } catch (StopMarkerNotFoundException e) {
                Log.e(LOG_TAG, Log.getStackTraceString(e));
            }
        }
    }

    /**
     * Draw markers for each stop.
     * @param listStopNamesRedLine List of Red Line stop names.
     * @param listStopNamesGreenLine List of Green Line stop names.
     */
    private void drawMarkers(List<String> listStopNamesRedLine,
                             List<String> listStopNamesGreenLine) {
        for (int i = 0; i < listStopNamesRedLine.size(); i++) {
            LatLng latLng = new LatLng(stopCoordsRedLine[i][0], stopCoordsRedLine[i][1]);

            MarkerOptions markerOptions =
                    new MarkerOptions()
                            .position(latLng)
                            .title(listStopNamesRedLine.get(i))
                            .icon(
                                    BitmapDescriptorFactory.defaultMarker(
                                            BitmapDescriptorFactory.HUE_RED
                                    )
                            );

            Marker marker = map.addMarker(markerOptions);

            listMarkers.add(marker);
        }

        for (int i = 0; i < listStopNamesGreenLine.size(); i++) {
            LatLng latLng = new LatLng(stopCoordsGreenLine[i][0], stopCoordsGreenLine[i][1]);

            MarkerOptions markerOptions =
                    new MarkerOptions()
                            .position(latLng)
                            .title(listStopNamesGreenLine.get(i))
                            .icon(
                                    BitmapDescriptorFactory.defaultMarker(
                                            BitmapDescriptorFactory.HUE_GREEN
                                    )
                            );

            Marker marker = map.addMarker(markerOptions);

            listMarkers.add(marker);
        }
    }

    /**
     * Draw lines between stops.
     * This is done very manually for now.
     * @param googleMap GoogleMap object on which to draw Polylines.
     * @param listStopNamesRedLine List of Red Line stop names.
     * @param listStopNamesGreenLine List of Green Line stop names.
     */
    private void drawPolylines(GoogleMap googleMap, List<String> listStopNamesRedLine,
                               List<String> listStopNamesGreenLine) {
        /* Draw Polylines from The Point to George's Dock. */
        for (int i = 0; i < 3; i++) {
            googleMap.addPolyline(
                    new PolylineOptions().add(
                            new LatLng(stopCoordsRedLine[i][0], stopCoordsRedLine[i][1]),
                            new LatLng(stopCoordsRedLine[i + 1][0], stopCoordsRedLine[i + 1][1])
                    ).width(12.0f).color(
                            ContextCompat.getColor(getApplicationContext(), R.color.tab_red_line)
                    )
            );
        }

        /* Draw Polyline from George's Dock to Busáras. */
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(53.34952800, -6.24757500),
                        new LatLng(53.35011668, -6.25158298)
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_red_line)
                )
        );

        /* Draw Polylines from Connolly to Tallaght. */
        for (int i = 4; i < 26; i++) {
            googleMap.addPolyline(
                    new PolylineOptions().add(
                            new LatLng(stopCoordsRedLine[i][0], stopCoordsRedLine[i][1]),
                            new LatLng(stopCoordsRedLine[i + 1][0], stopCoordsRedLine[i + 1][1])
                    ).width(12.0f).color(
                            ContextCompat.getColor(getApplicationContext(), R.color.tab_red_line)
                    )
            );
        }

        /* Draw Polyline from Belgard to Fettercairn. */
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(53.29929352, -6.37505436),
                        new LatLng(53.29336849, -6.39591122)
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_red_line)
                )
        );

        /* Draw Polylines from Fettercairn to Saggart. */
        for (int i = 27; i < listStopNamesRedLine.size() - 1; i++) {
            googleMap.addPolyline(
                    new PolylineOptions().add(
                            new LatLng(stopCoordsRedLine[i][0], stopCoordsRedLine[i][1]),
                            new LatLng(stopCoordsRedLine[i + 1][0], stopCoordsRedLine[i + 1][1])
                    ).width(12.0f).color(
                            ContextCompat.getColor(getApplicationContext(), R.color.tab_red_line)
                    )
            );
        }

        /* Draw Polylines from St. Stephen's Green to Brides Glen. */
        for (int i = 0; i < listStopNamesGreenLine.size() - 1; i++) {
            googleMap.addPolyline(
                    new PolylineOptions().add(
                            new LatLng(stopCoordsGreenLine[i][0], stopCoordsGreenLine[i][1]),
                            new LatLng(stopCoordsGreenLine[i + 1][0], stopCoordsGreenLine[i + 1][1])
                    ).width(12.0f).color(
                            ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                    )
            );
        }
    }

    /**
     * Find the Marker corresponding to a specific stop.
     * @param stopName Name of stop to find corresponding marker for.
     * @return Marker for specified stop name.
     */
    private Marker findStopMarker(String stopName) throws StopMarkerNotFoundException {
        for (Marker marker : listMarkers) {
            if (marker.getTitle().equalsIgnoreCase(stopName)) {
                return marker;
            }
        }

        /* If for some reason no stops are found, return an empty Marker. */
        Log.wtf(LOG_TAG, "No stop markers found for stop: " + stopName);

        throw new StopMarkerNotFoundException();
    }
}
