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
package org.thecosmicfrog.luasataglance.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.google.android.material.card.MaterialCardView
import org.thecosmicfrog.luasataglance.R

class SpinnerCardView : MaterialCardView {

    private val logTag: String = SpinnerCardView::class.java.simpleName

    private var adapterStops: ArrayAdapter<CharSequence?>? = null
    internal var spinnerStops: Spinner? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    /**
     * Initialise custom View.
     *
     * @param context Context.
     */
    fun init(context: Context?) {
        inflate(context, R.layout.cardview_spinner, this)

        spinnerStops = findViewById<Spinner>(R.id.card_view_spinner)

        /* Remove default MaterialCardView stroke (thin border around the card). */
        setStrokeWidth(0)
    }

    /**
     * Initialise the ArrayAdapter for stops.
     *
     * @param resArrayStops Resource ID for array of stops.
     */
    private fun initAdapterStops(resArrayStops: Int) {
        val arrayStops = resources.getStringArray(resArrayStops)
        val listStops = arrayStops.toList().sortedBy { it.lowercase() }
        adapterStops = ArrayAdapter(
            context,
            R.layout.spinner_stops,
            listStops
        )
        adapterStops?.setDropDownViewResource(R.layout.spinner_stops)
        spinnerStops?.adapter = adapterStops
    }

    /**
     * Setter method which also triggers an initialisation of the ArrayAdapter for stops.
     *
     * @param line Line to initialise.
     */
    fun setLine(line: String) {
        val redLine = "red_line"
        val greenLine = "green_line"

        var resArrayStops = 0

        when (line) {
            redLine -> resArrayStops = R.array.array_stops_redline

            greenLine -> resArrayStops = R.array.array_stops_greenline

            /* If for some reason the line doesn't make sense. */
            else -> Log.wtf(logTag, "Invalid line specified.")
        }

        initAdapterStops(resArrayStops)
    }

    fun getSpinnerStops(): Spinner? {
        return spinnerStops
    }

    fun setSelection(stopName: String?) {
        adapterStops?.getPosition(stopName)?.let { spinnerStops?.setSelection(it) }
    }
}
