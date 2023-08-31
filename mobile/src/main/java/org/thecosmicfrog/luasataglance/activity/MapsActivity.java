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

package org.thecosmicfrog.luasataglance.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;

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
import org.thecosmicfrog.luasataglance.model.StopCoords;
import org.thecosmicfrog.luasataglance.util.Constant;
import org.thecosmicfrog.luasataglance.util.Preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        EasyPermissions.PermissionCallbacks, EasyPermissions.RationaleCallbacks {

    private static final int REQUEST_CODE_LOCATION = 101;

    private final String LOG_TAG = MapsActivity.class.getSimpleName();
    private final String[] PERMISSIONS_LOCATION = {Manifest.permission.ACCESS_FINE_LOCATION};

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

        /* Use a Material Dialog theme. */
        setTheme(android.R.style.Theme_Material_Dialog);

        /* This is a Dialog. Get rid of the default Window title. */
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (!Preferences.permissionLocationShouldNotAskAgain(getApplicationContext())) {
            EasyPermissions.requestPermissions(
                    new PermissionRequest.Builder(this, REQUEST_CODE_LOCATION, PERMISSIONS_LOCATION)
                            .setRationale(R.string.rationale_location)
                            .setPositiveButtonText(R.string.rationale_ask_accept)
                            .setNegativeButtonText(R.string.rationale_ask_decline)
                            .setTheme(android.R.style.Theme_Material_Light_Dialog_Alert)
                            .build()
            );
        }

        setContentView(R.layout.activity_maps);

        /* Obtain the SupportMapFragment and get notified when the map is ready to be used. */
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        initButtons();
    }

    /**
     * Initialise any buttons in the activity.
     */
    private void initButtons() {
        /* Initialise close button. */
        ImageButton imageButtonCloseMap = findViewById(R.id.imagebutton_close_map);
        imageButtonCloseMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
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

        setMyLocationEnabled();

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

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (hasAllPermissionsGranted(grantResults)) {
            recreate();
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        Log.i(LOG_TAG, "Location permission granted.");
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Log.i(LOG_TAG, "Location permission denied.");
    }

    @Override
    public void onRationaleAccepted(int requestCode) {
        Log.i(LOG_TAG, "Location rationale accepted.");
    }

    @Override
    public void onRationaleDenied(int requestCode) {
        Log.i(LOG_TAG, "Location rationale denied.");

        Preferences.savePermissionLocationShouldNotAskAgain(getApplicationContext(), true);
    }

    /**
     * Enable "my location" feature in Google Maps dialog.
     */
    @AfterPermissionGranted(REQUEST_CODE_LOCATION)
    private void setMyLocationEnabled() {
        if (EasyPermissions.hasPermissions(this, PERMISSIONS_LOCATION)) {
            try {
                if (map != null) {
                    Log.i(LOG_TAG, "Enabling user's location.");

                    map.setMyLocationEnabled(true);
                }
            } catch (SecurityException e) {
                Log.w(LOG_TAG, "Location permission not granted.");
            } catch (Exception e) {
                Log.e(LOG_TAG, "Unknown error occurred while setting user's location.");
            }
        }
    }

    /**
     * Check if all permissions have been granted.
     * @param grantResults Grant results.
     * @return All permissions granted or not.
     */
    private boolean hasAllPermissionsGranted(@NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }

        return true;
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
     * @param googleMap GoogleMap model on which to draw Polylines.
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

        /* Draw Polylines from Broombridge to Parnell. */
        for (int i = 0; i < 6; i++) {
            googleMap.addPolyline(
                    new PolylineOptions().add(
                            new LatLng(stopCoordsGreenLine[i][0], stopCoordsGreenLine[i][1]),
                            new LatLng(stopCoordsGreenLine[i + 1][0], stopCoordsGreenLine[i + 1][1])
                    ).width(12.0f).color(
                            ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                    )
            );
        }

        /* Draw Polylines from Parnell to Marlborough. */
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(stopCoordsGreenLine[6][0], stopCoordsGreenLine[6][1]),
                        new LatLng(stopCoordsGreenLine[9][0], stopCoordsGreenLine[9][1])
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                )
        );

        /* Draw Polyline from Marlborough to the corner of Hawkins Street and College Street. */
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(stopCoordsGreenLine[9][0], stopCoordsGreenLine[9][1]),
                        new LatLng(53.34575198, -6.25701415)
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                )
        );

        /* Draw Polyline from the corner of Hawkins Street and College Street to Trinity. */
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(53.34575198, -6.25701415),
                        new LatLng(stopCoordsGreenLine[11][0], stopCoordsGreenLine[11][1])
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                )
        );

        /*
         * Draw Polylines around College Green, Grafton Street and Nassau Street, up to Dawson
         * Street.
         */
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(stopCoordsGreenLine[11][0], stopCoordsGreenLine[11][1]),
                        new LatLng(53.34495296, -6.25920819)
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                )
        );
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(53.34495296, -6.25920819),
                        new LatLng(53.34442293, -6.25948714)
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                )
        );
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(53.34442293, -6.25948714),
                        new LatLng(53.34398738, -6.25921892)
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                )
        );
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(53.34398738, -6.25921892),
                        new LatLng(53.34334845, -6.25924306)
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                )
        );
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(53.34334845, -6.25924306),
                        new LatLng(53.34318512, -6.25905799)
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                )
        );
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(53.34318512, -6.25905799),
                        new LatLng(53.34293531, -6.25772225)
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                )
        );
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(53.34293531, -6.25772225),
                        new LatLng(stopCoordsGreenLine[12][0], stopCoordsGreenLine[12][1])
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                )
        );

        /* Draw Polylines from Dawson to the end of Dawson Street. */
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(stopCoordsGreenLine[12][0], stopCoordsGreenLine[12][1]),
                        new LatLng(53.33950349, -6.25881123)
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                )
        );

        /* Draw Polylines from the end of Dawson Street to St. Stephen's Green. */
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(53.33950349, -6.25881123),
                        new LatLng(53.33952431, -6.25876563)
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                )
        );
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(53.33952431, -6.25876563),
                        new LatLng(53.33987183, -6.26049566)
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                )
        );
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(53.33987183, -6.26049566),
                        new LatLng(53.33975012, -6.26091944)
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                )
        );
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(53.33975012, -6.26091944),
                        new LatLng(stopCoordsGreenLine[13][0], stopCoordsGreenLine[13][1])
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                )
        );

        /* Draw Polylines from Trinity to Westmoreland. */
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(stopCoordsGreenLine[11][0], stopCoordsGreenLine[11][1]),
                        new LatLng(53.34532925, -6.25917064)
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                )
        );
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(53.34532925, -6.25917064),
                        new LatLng(stopCoordsGreenLine[10][0], stopCoordsGreenLine[10][1])
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                )
        );

        /* Draw Polylines from Westmoreland to O'Connell Street Stops. */
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(stopCoordsGreenLine[10][0], stopCoordsGreenLine[10][1]),
                        new LatLng(53.34693688, -6.25911700)
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                )
        );
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(53.34693688, -6.25911700),
                        new LatLng(stopCoordsGreenLine[7][0], stopCoordsGreenLine[7][1])
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                )
        );

        /* Draw Polyline from O'Connell - Upper to Parnell Street and close the loop. */
        googleMap.addPolyline(
                new PolylineOptions().add(
                        new LatLng(stopCoordsGreenLine[7][0], stopCoordsGreenLine[7][1]),
                        new LatLng(53.352594325768045, -6.261551109496622)
                ).width(12.0f).color(
                        ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
                )
        );

        /* Draw Polylines from St. Stephen's Green to Bride's Glen. */
        for (int i = 13; i < listStopNamesGreenLine.size() - 1; i++) {
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
