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
package org.thecosmicfrog.luasataglance.model

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.Preferences
import org.thecosmicfrog.luasataglance.util.StopForecastUtil

class StopForecastAdapter(
    private val listStopForecastInfo: List<StopForecastInfo>): RecyclerView.Adapter<StopForecastViewHolder>() {

    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : StopForecastViewHolder {
        context = parent.context

        val itemView = LayoutInflater.from(parent.context).inflate(
                R.layout.cardview_stop_forecast,
                parent,
                false
            )

        return StopForecastViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: StopForecastViewHolder, position: Int) {
        val stopForecast = listStopForecastInfo[position]
        val destValue = holder.textViewDestination?.resources?.getString(R.string.no_trams_forecast)
        val dueMinsValue = holder.textViewDueMinutes?.resources?.getString(R.string.due)
        val regexCannotScheduleNotification = Regex(
            "${destValue}\$|${dueMinsValue}\$|1\$|2\$|^\$"
        )

        holder.textViewDestination?.text = stopForecast.destination
        holder.textViewDueMinutes?.text = stopForecast.dueMinutes
        holder.textViewMinOrMins?.text = stopForecast.minOrMins
        holder.textViewMinOrMins?.visibility = if (stopForecast.showMinOrMins) View.VISIBLE else View.GONE

        /*
         * If the tram is arriving soon, or if there are no trams scheduled, don't show the
         * "Tap to set reminder" text.
         */
        when (holder.textViewDueMinutes?.text?.matches(regexCannotScheduleNotification) == true ||
                holder.textViewDestination?.text?.matches(regexCannotScheduleNotification) == true) {
            true -> {
                holder.textViewSetReminder?.visibility = View.GONE
            }
            else -> {}
        }

        /* Set OnClickListener for each item in the RecyclerView. */
        holder.itemView.setOnClickListener {
            StopForecastUtil.showNotifyTimeDialog(
                context,
                Preferences.selectedStopName(context, Constant.NO_LINE),
                holder.textViewDueMinutes?.text.toString()
            )
        }
    }

    override fun getItemCount(): Int {
        return listStopForecastInfo.size
    }
}

