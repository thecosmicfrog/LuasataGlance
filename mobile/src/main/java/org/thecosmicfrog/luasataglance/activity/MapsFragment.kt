/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2026 Aaron Hastings
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

package org.thecosmicfrog.luasataglance.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.exception.StopMarkerNotFoundException
import org.thecosmicfrog.luasataglance.model.StopCoords
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.Preferences
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest

class MapsFragment : Fragment(), OnMapReadyCallback, EasyPermissions.PermissionCallbacks,
    EasyPermissions.RationaleCallbacks {

    private val logTag = MapsFragment::class.java.simpleName
    private val permissionsLocation = Manifest.permission.ACCESS_FINE_LOCATION
    private var rootView: View? = null
    private var map: GoogleMap? = null

    private lateinit var stopCoordsRedLine: Array<DoubleArray>
    private lateinit var stopCoordsGreenLine: Array<DoubleArray>
    private lateinit var listMarkers: MutableList<Marker>

    companion object {
        const val requestCodeLocation = 101

        fun newInstance(): Fragment {
            val mapsFragment = MapsFragment()
            val bundle = Bundle()

            mapsFragment.arguments = bundle

            return mapsFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_maps, container, false)

        return rootView
    }

    override fun onResume() {
        super.onResume()

        if (!isAdded) return

        listMarkers = mutableListOf()

        stopCoordsRedLine = StopCoords(Constant.RED_LINE).stopCoords
        stopCoordsGreenLine = StopCoords(Constant.GREEN_LINE).stopCoords

        /* Obtain the SupportMapFragment and get notified when the map is ready to be used. */
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        if (isVisibleToUser) {
            if (!Preferences.permissionLocationShouldNotAskAgain(context)) {
                EasyPermissions.requestPermissions(
                    PermissionRequest.Builder(
                            this,
                            requestCodeLocation,
                            permissionsLocation
                        ).setRationale(
                            R.string.rationale_location
                        ).setPositiveButtonText(
                            R.string.rationale_ask_accept
                        ).setNegativeButtonText(
                            R.string.rationale_ask_decline
                        ).setTheme(
                            android.R.style.Theme_Material_Light_Dialog_Alert
                        ).build()
                )
            }
        }
    }

    /**
     * @param googleMap GoogleMap.
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        initCustomInfoWindow()

        setMyLocationEnabled()

        /* Set the default Camera position and zoom. */
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(53.34167328, -6.265131), 12.0f))

        val stopNamesRedLine = resources.getStringArray(R.array.array_stops_redline)
        val stopNamesGreenLine = resources.getStringArray(R.array.array_stops_greenline)

        val listStopNamesRedLine = stopNamesRedLine.toMutableList()
        val listStopNamesGreenLine = stopNamesGreenLine.toMutableList()

        /* Compile a List of all stops. */
        val listStopNamesAll = mutableListOf<String>()
        listStopNamesAll.addAll(listStopNamesRedLine)
        listStopNamesAll.addAll(listStopNamesGreenLine)

        /* Draw map markers. */
        drawMarkers(listStopNamesRedLine, listStopNamesGreenLine)

        /* Draw Polylines between Markers. */
        drawPolylines(googleMap, listStopNamesRedLine, listStopNamesGreenLine)

        /*
         * When a user taps on a stop's info window, it should open the appropriate stop forecast.
         */
        map?.setOnInfoWindowClickListener { marker ->
            context?.let { ctx ->
                val localBroadcastManager = LocalBroadcastManager.getInstance(ctx)

                val intent = Intent(Constant.INTENT_ACTION_LOAD_STOP)
                intent.putExtra(Constant.INTENT_EXTRA_STOP_NAME, marker.title)

                localBroadcastManager.sendBroadcast(intent)
            }
        }

        /*
         * Move the Camera to the position of the stop that this Activity was opened from.
         * Also, open the Marker's info window.
         */
        if (activity?.intent?.hasExtra(Constant.STOP_NAME) as Boolean) {
            try {
                val marker =
                    findStopMarker(activity?.intent?.getStringExtra(Constant.STOP_NAME) as String)

                map?.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 14.0f))

                marker.showInfoWindow()
            } catch (e: StopMarkerNotFoundException) {
                Log.e(logTag, Log.getStackTraceString(e))
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (hasAllPermissionsGranted(grantResults)) {
            Preferences.savePermissionLocationGranted(context, true)
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        Log.i(logTag, "Location permission granted.")
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Log.i(logTag, "Location permission denied.")
    }

    override fun onRationaleAccepted(requestCode: Int) {
        Log.i(logTag, "Location rationale accepted.")
    }

    override fun onRationaleDenied(requestCode: Int) {
        Log.i(logTag, "Location rationale denied.")

        Preferences.savePermissionLocationShouldNotAskAgain(context, true)
    }

    /**
     * Enable "my location" feature in Google Maps dialog.
     */
    @AfterPermissionGranted(requestCodeLocation)
    private fun setMyLocationEnabled() {
        if (EasyPermissions.hasPermissions(context as Context, permissionsLocation)) {
            try {
                if (Preferences.permissionLocationGranted(context)) {
                    Log.i(logTag, "Enabling user's location.")

                    map?.isMyLocationEnabled = true
                }
            } catch (e: SecurityException) {
                Log.w(logTag, "Location permission not granted.")
            } catch (e: Exception) {
                Log.e(logTag, "Unknown error occurred while setting user's location.")
                Log.e(logTag, e.stackTrace.toString())
            }
        }
    }

    /**
     * Check if all permissions have been granted.
     * @param grantResults Grant results.
     * @return All permissioned granted or not.
     */
    private fun hasAllPermissionsGranted(grantResults: IntArray) : Boolean {
        for (grantResult in grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false
            }
        }

        return true
    }

    /**
     * Draw markers for each stop.
     * @param listStopNamesRedLine List of Red Line stop names.
     * @param listStopNamesGreenLine List of Green Line stop names.
     */
    private fun drawMarkers(listStopNamesRedLine: List<String>,
                            listStopNamesGreenLine: List<String>) {
        for (i in listStopNamesRedLine.indices) {
            val latLng = LatLng(stopCoordsRedLine[i][0], stopCoordsRedLine[i][1])
            
            val markerOptions =
                MarkerOptions()
                    .position(latLng)
                    .title(listStopNamesRedLine[i])
                    .icon(
                        BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_RED
                        )
                    )
            
            val marker = map?.addMarker(markerOptions)
            
            listMarkers.add(marker as Marker)
        }

        for (i in listStopNamesGreenLine.indices) {
            val latLng = LatLng(stopCoordsGreenLine[i][0], stopCoordsGreenLine[i][1])

            val markerOptions =
                MarkerOptions()
                    .position(latLng)
                    .title(listStopNamesGreenLine[i])
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))

            val marker = map?.addMarker(markerOptions)

            listMarkers.add(marker as Marker)
        }
    }

    /**
     * Draw lines between stops.
     * This is done very manually for now.
     * @param googleMap GoogleMap model on which to draw Polylines.
     * @param listStopNamesRedLine List of Red Line stop names.
     * @param listStopNamesGreenLine List of Green Line stop names.
     */
    private fun drawPolylines(googleMap: GoogleMap?, listStopNamesRedLine: List<String>,
                              listStopNamesGreenLine: List<String>) {
        /* Draw Polylines from The Point to George's Dock. */
        for (i in 0..2) {
            googleMap?.addPolyline(
                PolylineOptions().add(
                    LatLng(stopCoordsRedLine[i][0], stopCoordsRedLine[i][1]),
                    LatLng(stopCoordsRedLine[i + 1][0], stopCoordsRedLine[i + 1][1])
                ).width(12.0f).color(
                    ContextCompat.getColor(context as Context, R.color.tab_red_line)
                )
            )
        }

        /* Draw Polyline from George's Dock to Busáras. */
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(53.34952800, -6.24757500),
                LatLng(53.35011668, -6.25158298)
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_red_line)
            )
        )

        /* Draw Polylines from Connolly to Tallaght. */
        for (i in 4..25) {
            googleMap?.addPolyline(
                PolylineOptions().add(
                    LatLng(stopCoordsRedLine[i][0], stopCoordsRedLine[i][1]),
                    LatLng(stopCoordsRedLine[i + 1][0], stopCoordsRedLine[i + 1][1])
                ).width(12.0f).color(
                    ContextCompat.getColor(context as Context, R.color.tab_red_line)
                )
            )
        }

        /* Draw Polyline from Belgard to Fettercairn. */
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(53.29929352, -6.37505436),
                LatLng(53.29336849, -6.39591122)
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_red_line)
            )
        )

        /* Draw Polylines from Fettercairn to Saggart. */
        for (i in 27 until listStopNamesRedLine.size - 1) {
            googleMap?.addPolyline(
                PolylineOptions().add(
                    LatLng(stopCoordsRedLine[i][0], stopCoordsRedLine[i][1]),
                    LatLng(stopCoordsRedLine[i + 1][0], stopCoordsRedLine[i + 1][1])
                ).width(12.0f).color(
                    ContextCompat.getColor(context as Context, R.color.tab_red_line)
                )
            )
        }

        /* Draw Polylines from Broombridge to Parnell. */
        for (i in 0..5) {
            googleMap?.addPolyline(
                PolylineOptions().add(
                    LatLng(stopCoordsGreenLine[i][0], stopCoordsGreenLine[i][1]),
                    LatLng(
                        stopCoordsGreenLine[i + 1][0],
                        stopCoordsGreenLine[i + 1][1]
                    )
                ).width(12.0f).color(
                    ContextCompat.getColor(context as Context, R.color.tab_green_line)
                )
            )
        }

        /* Draw Polylines from Parnell to Marlborough. */
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(stopCoordsGreenLine[6][0], stopCoordsGreenLine[6][1]),
                LatLng(stopCoordsGreenLine[9][0], stopCoordsGreenLine[9][1])
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_green_line)
            )
        )

        /* Draw Polyline from Marlborough to the corner of Hawkins Street and College Street. */
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(stopCoordsGreenLine[9][0], stopCoordsGreenLine[9][1]),
                LatLng(53.34575198, -6.25701415)
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_green_line)
            )
        )

        /* Draw Polyline from the corner of Hawkins Street and College Street to Trinity. */
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(53.34575198, -6.25701415),
                LatLng(stopCoordsGreenLine[11][0], stopCoordsGreenLine[11][1])
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_green_line)
            )
        )

        /*
         * Draw Polylines around College Green, Grafton Street and Nassau Street, up to Dawson
         * Street.
         */
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(stopCoordsGreenLine[11][0], stopCoordsGreenLine[11][1]),
                LatLng(53.34495296, -6.25920819)
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_green_line)
            )
        )
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(53.34495296, -6.25920819),
                LatLng(53.34442293, -6.25948714)
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_green_line)
            )
        )
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(53.34442293, -6.25948714),
                LatLng(53.34398738, -6.25921892)
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_green_line)
            )
        )
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(53.34398738, -6.25921892),
                LatLng(53.34334845, -6.25924306)
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_green_line)
            )
        )
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(53.34334845, -6.25924306),
                LatLng(53.34318512, -6.25905799)
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_green_line)
            )
        )
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(53.34318512, -6.25905799),
                LatLng(53.34293531, -6.25772225)
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_green_line)
            )
        )
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(53.34293531, -6.25772225),
                LatLng(stopCoordsGreenLine[12][0], stopCoordsGreenLine[12][1])
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_green_line)
            )
        )

        /* Draw Polylines from Dawson to the end of Dawson Street. */
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(stopCoordsGreenLine[12][0], stopCoordsGreenLine[12][1]),
                LatLng(53.33950349, -6.25881123)
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_green_line)
            )
        )

        /* Draw Polylines from the end of Dawson Street to St. Stephen's Green. */
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(53.33950349, -6.25881123),
                LatLng(53.33952431, -6.25876563)
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_green_line)
            )
        )
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(53.33952431, -6.25876563),
                LatLng(53.33987183, -6.26049566)
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_green_line)
            )
        )
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(53.33987183, -6.26049566),
                LatLng(53.33975012, -6.26091944)
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_green_line)
            )
        )
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(53.33975012, -6.26091944),
                LatLng(stopCoordsGreenLine[13][0], stopCoordsGreenLine[13][1])
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_green_line)
            )
        )

        /* Draw Polylines from Trinity to Westmoreland. */
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(stopCoordsGreenLine[11][0], stopCoordsGreenLine[11][1]),
                LatLng(53.34532925, -6.25917064)
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_green_line)
            )
        )
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(53.34532925, -6.25917064),
                LatLng(stopCoordsGreenLine[10][0], stopCoordsGreenLine[10][1])
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_green_line)
            )
        )

        /* Draw Polylines from Westmoreland to O'Connell Street Stops. */
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(stopCoordsGreenLine[10][0], stopCoordsGreenLine[10][1]),
                LatLng(53.34693688, -6.25911700)
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_green_line)
            )
        )
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(53.34693688, -6.25911700),
                LatLng(stopCoordsGreenLine[7][0], stopCoordsGreenLine[7][1])
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_green_line)
            )
        )

        /* Draw Polyline from O'Connell - Upper to Parnell Street and close the loop. */
        googleMap?.addPolyline(
            PolylineOptions().add(
                LatLng(stopCoordsGreenLine[7][0], stopCoordsGreenLine[7][1]),
                LatLng(53.352594325768045, -6.261551109496622)
            ).width(12.0f).color(
                ContextCompat.getColor(context as Context, R.color.tab_green_line)
            )
        )

        /* Draw Polylines from St. Stephen's Green to Brides Glen. */
        for (i in 13 until listStopNamesGreenLine.size - 1) {
            googleMap?.addPolyline(
                PolylineOptions().add(
                    LatLng(stopCoordsGreenLine[i][0], stopCoordsGreenLine[i][1]),
                    LatLng(
                        stopCoordsGreenLine[i + 1][0],
                        stopCoordsGreenLine[i + 1][1]
                    )
                ).width(12.0f).color(
                    ContextCompat.getColor(context as Context, R.color.tab_green_line)
                )
            )
        }
    }

    /**
     * Find the Marker corresponding to a specific stop.
     * @param stopName Name of stop to find corresponding marker for.
     * @return Marker for specified stop name.
     */
    private fun findStopMarker(stopName: String) : Marker {
        for (marker in listMarkers) {
            if (marker.title.equals(stopName, true)) {
                return marker
            }
        }

        /* If for some reason no stops are found, return an empty Marker. */
        Log.wtf(logTag, "No stop markers found for stop: $stopName")

        throw StopMarkerNotFoundException()
    }

    /**
     * Initialise the custom info window for the map.
     */
    private fun initCustomInfoWindow() {
        map?.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View? {
                val view = layoutInflater.inflate(R.layout.infowindow_maps, null)

                view.findViewById<TextView>(R.id.title).text = marker.title
                setMapInfoWindowLineIndicator(view, marker.title)

                return view
            }

            override fun getInfoContents(marker: Marker): View? {
                return null
            }
        })
    }

    /**
     * Set an aesthetically-pleasing indicator colour for map info windows based on the stop name.
     * @param view The info window view containing the line indicator.
     * @param stopName The name of the stop.
     */
    private fun setMapInfoWindowLineIndicator(view: View, stopName: String?) {
        val lineIndicator = view.findViewById<View>(R.id.view_line_indicator)
        val backgroundDrawable = lineIndicator.background

        val redLineStops = resources.getStringArray(R.array.array_stops_redline)
        val greenLineStops = resources.getStringArray(R.array.array_stops_greenline)

        when (stopName) {
            in redLineStops -> {
                backgroundDrawable.setTint(
                    ContextCompat.getColor(requireContext(), R.color.tab_red_line)
                )
            }
            in greenLineStops -> {
                backgroundDrawable.setTint(
                    ContextCompat.getColor(requireContext(), R.color.tab_green_line)
                )
            }
            else -> {
                backgroundDrawable.setTint(
                    ContextCompat.getColor(requireContext(), android.R.color.transparent)
                )
                Log.wtf("MapInfoWindow", "Stop name not found in red or green line arrays.")
            }
        }
    }
}

