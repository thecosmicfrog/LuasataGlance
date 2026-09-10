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
package org.thecosmicfrog.luasataglance.adapter

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.model.Stops
import org.thecosmicfrog.luasataglance.util.Constant

/**
 * Lists stops as cards, each with a stripe in its line's colour and a checkbox.
 *
 * @param stops         The stops to list, in the order they are shown.
 * @param selectedStops The stops currently checked, updated in place as the user taps.
 * @param singleSelect  Whether checking a stop unchecks the others, as the default stop screen needs.
 */
class StopSelectAdapter(
    private val stops: ArrayList<CharSequence?>,
    private val selectedStops: ArrayList<CharSequence?>?,
    private val singleSelect: Boolean = false
) : RecyclerView.Adapter<StopSelectAdapter.ViewHolder>() {

    private val logTag = StopSelectAdapter::class.java.simpleName

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.checkbox_stop)
        val stopName: TextView = view.findViewById(R.id.text_stop_name)
        val lineIndicator: View = view.findViewById(R.id.view_line_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.cardview_stop_selection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val stop = stops[position]
        holder.stopName.text = stop

        /* Clear any existing listeners to prevent duplicate calls. */
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = selectedStops?.contains(stop) == true

        /* Toggle the checkbox state. */
        val onClickListener = View.OnClickListener {
            if (singleSelect) {
                selectOnly(holder, stop)
            } else {
                if (selectedStops?.contains(stop) == true) {
                    selectedStops.remove(stop)
                } else {
                    selectedStops?.add(stop)
                }
                holder.checkBox.isChecked = selectedStops?.contains(stop) == true
            }
        }

        /* Allow the user to tap either the entire card or the checkbox to toggle the selection. */
        holder.itemView.setOnClickListener(onClickListener)
        holder.checkBox.setOnClickListener(onClickListener)

        stop?.let {
            setLineIndicatorColor(holder, it)
        }
    }

    /**
     * Set an aesthetically-pleasing indicator colour based on the stop name and its associated line.
     *
     * @param holder The ViewHolder for the RecyclerView.
     * @param stop The name of the stop.
     */
    private fun setLineIndicatorColor(holder: ViewHolder, stop: CharSequence) {
        val context = holder.itemView.context
        val stopId = Stops.idForName(context, stop.toString())

        when (Stops.line(stopId)) {
            Constant.RED_LINE -> {
                holder.lineIndicator.setBackgroundColor(context.getColor(R.color.tab_red_line))
            }
            Constant.GREEN_LINE -> {
                holder.lineIndicator.setBackgroundColor(context.getColor(R.color.tab_green_line))
            }
            else -> {
                holder.lineIndicator.setBackgroundColor(Color.TRANSPARENT)

                /* The default stop screen lists "None", which is not a stop, so only a real stop with no line is a bug. */
                if (stopId != null) Log.wtf(logTag, "Stop name not found in red or green line arrays.")
            }
        }
    }

    /**
     * Check a single stop, unchecking whichever stop was checked before.
     *
     * @param holder The ViewHolder for the row tapped.
     * @param stop   The name of the stop tapped.
     */
    private fun selectOnly(holder: ViewHolder, stop: CharSequence?) {
        val selected = selectedStops ?: return
        val previousPosition = stops.indexOf(selected.firstOrNull())

        selected.clear()
        selected.add(stop)

        /* The row checked before this one has to be redrawn as well, or two rows are left looking checked. */
        if (previousPosition >= 0) notifyItemChanged(previousPosition)
        notifyItemChanged(holder.bindingAdapterPosition)
    }

    override fun getItemCount() = stops.size
}
