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
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.maps.SupportMapFragment
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.databinding.FragmentMapsBinding
import org.thecosmicfrog.luasataglance.exception.StopMarkerNotFoundException
import org.thecosmicfrog.luasataglance.model.Stops
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.Preferences
import org.thecosmicfrog.luasataglance.util.StopForecastUtil
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import java.net.URI

class MapsFragment : Fragment(), EasyPermissions.PermissionCallbacks, EasyPermissions.RationaleCallbacks {

    private val logTag = MapsFragment::class.java.simpleName
    private val permissionsLocation = Manifest.permission.ACCESS_FINE_LOCATION
    private var binding: FragmentMapsBinding? = null
    private var map: MapLibreMap? = null

    private lateinit var listMarkers: MutableList<Marker>

    companion object {
        const val requestCodeLocation = 101

        private const val SOURCE_TRACKS = "luas-tracks"
        private const val MARKER_LAYER = "org.maplibre.annotations.points"
        private const val LINE_WIDTH = 4.0f
        private const val MY_LOCATION_ZOOM = 14.0

        fun newInstance(): Fragment {
            val mapsFragment = MapsFragment()
            val bundle = Bundle()

            mapsFragment.arguments = bundle

            return mapsFragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapLibre.getInstance(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMapsBinding.inflate(inflater, container, false)

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listMarkers = mutableListOf()

        binding?.fabMyLocation?.setOnClickListener { jumpToMyLocation() }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?

        mapFragment?.getMapAsync { mapLibreMap ->
            map = mapLibreMap

            mapLibreMap.setStyle(getString(R.string.map_style_url)) { style ->
                onStyleLoaded(mapLibreMap, style)
            }
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        if (isVisibleToUser) {
            if (!Preferences.permissionLocationShouldNotAskAgain(context)) {
                requestLocationPermission()
            }
        }
    }

    /**
     * Draws everything on to the map once its style is loaded.
     *
     * @param mapLibreMap The map.
     * @param style       The loaded style, which the location component needs.
     */
    private fun onStyleLoaded(mapLibreMap: MapLibreMap, style: Style) {
        initCustomInfoWindow(mapLibreMap)

        setMyLocationEnabled()

        /* Set the default Camera position and zoom. */
        mapLibreMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(53.34167328, -6.265131), 12.0))

        /* Draw map markers. */
        drawMarkers(mapLibreMap)

        drawLines(style)

        /* Centre camera on marker when tapped. */
        mapLibreMap.setOnMarkerClickListener { marker ->
            mapLibreMap.animateCamera(CameraUpdateFactory.newLatLng(marker.position))

            /* False, so MapLibre still opens the info window. Tapping the window opens the stop forecast. */
            false
        }

        /* When a user taps on a stop's info window, it should open the appropriate stop forecast. */
        mapLibreMap.setOnInfoWindowClickListener { marker ->
            context?.let { ctx ->
                val localBroadcastManager = LocalBroadcastManager.getInstance(ctx)

                val intent = Intent(Constant.INTENT_ACTION_LOAD_STOP)
                intent.putExtra(Constant.INTENT_EXTRA_STOP_NAME, marker.title)

                localBroadcastManager.sendBroadcast(intent)
            }

            /* False, so MapLibre closes the info window. The broadcast above opens the stop forecast. */
            false
        }

        /*
         * Move the Camera to the position of the stop that this Activity was opened from.
         * Also, open the Marker's info window.
         */
        if (activity?.intent?.hasExtra(Constant.STOP_NAME) == true) {
            try {
                val marker = findStopMarker(activity?.intent?.getStringExtra(Constant.STOP_NAME) as String)

                mapLibreMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 13.0))

                mapLibreMap.selectMarker(marker)
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
     * Switch on MapLibre's "blue dot".
     */
    @AfterPermissionGranted(requestCodeLocation)
    private fun setMyLocationEnabled() {
        val mapLibreMap = map ?: return

        /* EasyPermissions calls this again once the user grants permission, which can be before the style has loaded. */
        val style = mapLibreMap.style ?: return

        if (!EasyPermissions.hasPermissions(requireContext(), permissionsLocation)) {
            return
        }

        try {
            Log.i(logTag, "Enabling user's location.")

            mapLibreMap.locationComponent.apply {
                /* The "My Location" button calls this too, so guard against activating twice. */
                if (!isLocationComponentActivated) {
                    activateLocationComponent(
                        LocationComponentActivationOptions.builder(requireContext(), style).build()
                    )
                }

                isLocationComponentEnabled = true

                /* NONE, so showing the position does not drag the camera away from where the user left it. */
                cameraMode = CameraMode.NONE
            }
        } catch (_: SecurityException) {
            Log.w(logTag, "Location permission not granted.")
        } catch (e: Exception) {
            Log.e(logTag, "Unknown error occurred while setting user's location.")
            Log.e(logTag, e.stackTrace.toString())
        }
    }

    /**
     * Ask for location permission, showing the rationale dialog first.
     */
    private fun requestLocationPermission() {
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
                    R.style.LuasAtAGlanceRationaleDialog
                ).build()
        )
    }

    /**
     * Move the camera to where the user is.
     */
    private fun jumpToMyLocation() {
        /* An explicit tap outranks the prompt shown on first view, so ask again even after a refusal. */
        if (!EasyPermissions.hasPermissions(requireContext(), permissionsLocation)) {
            requestLocationPermission()

            return
        }

        val mapLibreMap = map ?: return

        /* Permission may have just been granted, so switch the blue dot on before reading a position. */
        setMyLocationEnabled()

        val location = mapLibreMap.locationComponent.let {
            if (it.isLocationComponentActivated) it.lastKnownLocation else null
        }

        if (location == null) {
            /* No location yet, so there is nowhere to move to. */
            activity?.let { StopForecastUtil.showSnackbar(it, getString(R.string.map_location_unavailable)) }

            return
        }

        mapLibreMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), MY_LOCATION_ZOOM)
        )
    }

    /**
     * Check if all permissions have been granted.
     *
     * @param grantResults Grant results.
     * @return All permissions granted or not.
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
     * Build a marker icon from a vector drawable.
     *
     * @param drawableRes The pin drawable.
     * @return Icon for a Marker.
     */
    private fun markerIcon(@DrawableRes drawableRes: Int): Icon {
        val drawable = requireNotNull(ContextCompat.getDrawable(requireContext(), drawableRes))

        return IconFactory.getInstance(requireContext()).fromBitmap(drawable.toBitmap())
    }

    /**
     * Draw markers for each stop.
     *
     * @param mapLibreMap Map on which to draw Markers.
     */
    private fun drawMarkers(mapLibreMap: MapLibreMap) {
        val icons = mapOf(
            Constant.RED_LINE to markerIcon(R.drawable.ic_map_marker_red_line),
            Constant.GREEN_LINE to markerIcon(R.drawable.ic_map_marker_green_line)
        )

        for (stop in Stops.all) {
            val markerOptions =
                MarkerOptions()
                    .position(LatLng(stop.latitude, stop.longitude))
                    .title(getString(stop.nameRes))
                    /* The snippet is never drawn. It carries the stop ID so the info window does not have to match on name. */
                    .snippet(stop.id)
                    .icon(icons.getValue(stop.line))

            listMarkers.add(mapLibreMap.addMarker(markerOptions))
        }
    }

    /**
     * Draw the two Luas lines from the track geometry in assets.
     *
     * The coordinates are the tram tracks, from the OpenStreetMap route relations, rebuilt by `tools/generate_luas_tracks.py`.
     *
     * @param style The loaded style, which owns the source and both layers.
     */
    private fun drawLines(style: Style) {
        style.addSource(GeoJsonSource(SOURCE_TRACKS, URI("asset://luas_tracks.geojson")))

        val lineColours = listOf("red" to R.color.tab_red_line, "green" to R.color.tab_green_line)

        for ((line, colourRes) in lineColours) {
            val layer = LineLayer("$SOURCE_TRACKS-$line", SOURCE_TRACKS)
                .withFilter(Expression.eq(Expression.get("line"), Expression.literal(line)))
                .withProperties(
                    PropertyFactory.lineColor(ContextCompat.getColor(requireContext(), colourRes)),
                    PropertyFactory.lineWidth(LINE_WIDTH),
                    /* The file holds hundreds of short ways rather than one path per line, so square ends show as notches. */
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                )

            /*
             * addLayer puts a layer above every existing one, including the markers. Below MARKER_LAYER keeps the pins and the
             * location dot clear of the track.
             */
            if (style.getLayer(MARKER_LAYER) != null) {
                style.addLayerBelow(layer, MARKER_LAYER)
            } else {
                Log.w(logTag, "$MARKER_LAYER is missing, so the tracks will cover the markers.")

                style.addLayer(layer)
            }
        }
    }

    /**
     * Find the Marker corresponding to a specific stop.
     *
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
     *
     * @param mapLibreMap Map whose info windows to render.
     */
    private fun initCustomInfoWindow(mapLibreMap: MapLibreMap) {
        mapLibreMap.setInfoWindowAdapter { marker ->
            val view = layoutInflater.inflate(R.layout.infowindow_maps, null)

            view.findViewById<TextView>(R.id.title).text = marker.title
            setMapInfoWindowLineIndicator(view, marker.snippet)

            view
        }
    }

    /**
     * Set an aesthetically-pleasing indicator colour for map info windows based on the stop.
     *
     * @param view   The info window view containing the line indicator.
     * @param stopId The stop ID carried in the Marker's snippet.
     */
    private fun setMapInfoWindowLineIndicator(view: View, stopId: String?) {
        val lineIndicator = view.findViewById<View>(R.id.view_line_indicator)
        val backgroundDrawable = lineIndicator.background

        val colorRes = when (Stops.line(stopId)) {
            Constant.RED_LINE -> R.color.tab_red_line
            Constant.GREEN_LINE -> R.color.tab_green_line
            else -> {
                Log.wtf("MapInfoWindow", "Stop ID on neither the Red nor the Green Line: $stopId")

                android.R.color.transparent
            }
        }

        backgroundDrawable.setTint(ContextCompat.getColor(requireContext(), colorRes))
    }

    override fun onDestroyView() {
        map = null
        binding = null

        super.onDestroyView()
    }
}
