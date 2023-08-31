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
package org.thecosmicfrog.luasataglance.activity

import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.model.StopNameIdMap
import org.thecosmicfrog.luasataglance.api.ApiFares
import org.thecosmicfrog.luasataglance.api.ApiMethods
import retrofit.Callback
import retrofit.RestAdapter
import retrofit.RetrofitError
import retrofit.client.Response
import java.util.*

class FaresActivity : AppCompatActivity() {

    private val logTag = FaresActivity::class.java.simpleName

    private var adapterLines: ArrayAdapter<CharSequence>? = null
    private var adapterStops: ArrayAdapter<CharSequence>? = null
    private var adapterStopsAdults: ArrayAdapter<CharSequence>? = null
    private var adapterStopsChildren: ArrayAdapter<CharSequence>? = null
    private var scrollViewFares: ScrollView? = null
    private var spinnerFaresLine: Spinner? = null
    private var spinnerFaresOrigin: Spinner? = null
    private var spinnerFaresDestination: Spinner? = null
    private var spinnerFaresAdults: Spinner? = null
    private var spinnerFaresChildren: Spinner? = null
    private var textViewFaresOffPeak: TextView? = null
    private var textViewFaresPeak: TextView? = null
    private var mapStopNameId: StopNameIdMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fares)

        /* Set status bar colour. */
        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = ContextCompat.getColor(applicationContext,
                R.color.luas_purple_statusbar)

        initializeActivity()
    }

    /**
     * Initialize Activity.
     */
    private fun initializeActivity() {
        /* Initialise correct locale. */
        val localeDefault = Locale.getDefault().toString()

        /* Instantiate a new StopNameIdMap. */
        mapStopNameId = StopNameIdMap(localeDefault)

        scrollViewFares = findViewById(R.id.scrollview_fares)

        spinnerFaresLine = findViewById(R.id.spinner_fares_line)
        spinnerFaresLine?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View, position: Int,
                                        l: Long) {
                var resArrayStops = 0

                when (position) {
                    0 -> {
                        resArrayStops = R.array.array_stops_redline
                    }
                    1 -> {
                        resArrayStops = R.array.array_stops_greenline
                    }
                    else -> {
                        Log.wtf(logTag, "Spinner position not 0 or 1. Setting to Red Line.")
                    }
                }

                adapterStops = ArrayAdapter.createFromResource(
                        applicationContext,
                        resArrayStops,
                        R.layout.spinner_stops
                )

                spinnerFaresOrigin?.adapter = adapterStops
                spinnerFaresDestination?.adapter = adapterStops

                /* If the user changes line, reset the displayed fares. */
                clearCalculatedFares()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        spinnerFaresOrigin = findViewById(R.id.spinner_fares_origin)
        spinnerFaresOrigin?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
                loadFaresBasedOnSpinnerSelected()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        spinnerFaresDestination = findViewById(R.id.spinner_fares_destination)
        spinnerFaresDestination?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
                loadFaresBasedOnSpinnerSelected()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        spinnerFaresAdults = findViewById(R.id.spinner_fares_adults)
        spinnerFaresAdults?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
                loadFaresBasedOnSpinnerSelected()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        spinnerFaresChildren = findViewById(R.id.spinner_fares_children)
        spinnerFaresChildren?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
                loadFaresBasedOnSpinnerSelected()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        setSpinnerColor(spinnerFaresLine)
        setSpinnerColor(spinnerFaresOrigin)
        setSpinnerColor(spinnerFaresDestination)
        setSpinnerColor(spinnerFaresAdults)
        setSpinnerColor(spinnerFaresChildren)

        adapterLines = ArrayAdapter.createFromResource(
                this,
                R.array.array_lines,
                R.layout.spinner_stops
        )

        adapterStopsAdults = ArrayAdapter.createFromResource(
                this,
                R.array.array_number_pax,
                R.layout.spinner_stops
        )

        adapterStopsChildren = ArrayAdapter.createFromResource(
                this,
                R.array.array_number_pax,
                R.layout.spinner_stops
        )

        adapterLines?.setDropDownViewResource(R.layout.spinner_stops)
        adapterStopsAdults?.setDropDownViewResource(R.layout.spinner_stops)
        adapterStopsChildren?.setDropDownViewResource(R.layout.spinner_stops)

        spinnerFaresLine?.adapter = adapterLines
        spinnerFaresAdults?.adapter = adapterStopsAdults
        spinnerFaresChildren?.adapter = adapterStopsChildren

        /* Start with a default of 1 adult. */
        spinnerFaresAdults?.setSelection(1)
    }

    override fun onResume() {
        super.onResume()

        setIsLoading(false)

        textViewFaresOffPeak = findViewById(R.id.textview_fares_offpeak)
        textViewFaresPeak = findViewById(R.id.textview_fares_peak)
    }

    /**
     * Utility method to load calculated fares based on the current values of all Spinners.
     */
    private fun loadFaresBasedOnSpinnerSelected() {
        if (spinnerFaresOrigin?.selectedItem.toString() ==
                getString(R.string.select_a_stop) || spinnerFaresDestination?.selectedItem.toString() ==
                getString(R.string.select_a_stop)) {
            return
        }

        setIsLoading(true)

        val apiUrl = "https://api.thecosmicfrog.org/cgi-bin"
        val apiAction = "farecalc"

        /*
         * Prepare Retrofit API call.
         */
        val restAdapter = RestAdapter.Builder()
                .setEndpoint(apiUrl)
                .build()

        val methods = restAdapter.create(ApiMethods::class.java)

        val callback: Callback<ApiFares> = object : Callback<ApiFares> {
            override fun success(apiFares: ApiFares, response: Response) {
                val fareOffPeak = apiFares.offpeak
                val farePeak = apiFares.peak
                textViewFaresOffPeak?.text = "€$fareOffPeak"
                textViewFaresPeak?.text = "€$farePeak"

                /*
                 * Now that we've got the fare values, scroll down to ensure the fares and fare
                 * disclaimer is displayed to the user.
                 */
                if (scrollViewFares != null) {
                    scrollViewFares?.post { scrollViewFares?.fullScroll(ScrollView.FOCUS_DOWN) }
                }

                setIsLoading(false)
            }

            override fun failure(retrofitError: RetrofitError) {
                setIsLoading(false)

                clearCalculatedFares()

                Toast.makeText(
                        applicationContext,
                        getString(R.string.message_error),
                        Toast.LENGTH_LONG
                ).show()

                Log.e(logTag, "Failure during call to server.")

                /*
                 * If we get a message or a response from the server, there's likely an issue with
                 * the client request or the server's response itself.
                 */
                Log.e(logTag, retrofitError.message!!)
                Log.e(logTag, retrofitError.response.url!!)
                Log.e(logTag, retrofitError.response.status.toString())
                Log.e(logTag, retrofitError.response.headers.toString())
                Log.e(logTag, retrofitError.response.body.toString())
                Log.e(logTag, retrofitError.response.reason!!)

                /*
                 * If we don't receive a message or response, we can still get an idea of what's
                 * going on by getting the "kind" of error.
                 */
                Log.e(logTag, retrofitError.kind.toString())
            }
        }

        /* Call API and get fares from server. */
        methods.getFares(
                apiAction,
                mapStopNameId?.get(spinnerFaresOrigin?.selectedItem.toString()),
                mapStopNameId?.get(spinnerFaresDestination?.selectedItem.toString()),
                spinnerFaresAdults?.selectedItem.toString(),
                spinnerFaresChildren?.selectedItem.toString(),
                callback
        )
    }

    /**
     * Reset the calculated fares to €0.00.
     */
    private fun clearCalculatedFares() {
        textViewFaresOffPeak?.text = getString(R.string.fares_zero)
        textViewFaresPeak?.text = getString(R.string.fares_zero)
    }

    /**
     * Make progress bar animate or not.
     * @param loading Whether or not progress bar should animate.
     */
    private fun setIsLoading(loading: Boolean) {
        val progressBarFares = findViewById<ProgressBar>(R.id.progressbar_fares)
        runOnUiThread {
            if (loading) {
                progressBarFares.visibility = View.VISIBLE
            } else {
                progressBarFares.visibility = View.INVISIBLE
            }
        }
    }

    /**
     * Cosmetic method to set the Spinner's arrow to purple.
     * @param spinner Spinner
     */
    private fun setSpinnerColor(spinner: Spinner?) {
        /* Set the Spinner's colour to Luas purple. */
        if (spinner?.background?.constantState != null) {
            val spinnerDrawable = spinner.background?.constantState?.newDrawable()

            spinnerDrawable?.setColorFilter(
                    ContextCompat.getColor(applicationContext, R.color.luas_purple),
                    PorterDuff.Mode.SRC_ATOP
            )
            spinner.background = spinnerDrawable
        }
    }
}

