package org.thecosmicfrog.luasataglance.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.thecosmicfrog.luasataglance.R

class StopForecastAdapter(private val listStopForecastInfo: List<StopForecastInfo>):
    RecyclerView.Adapter<StopForecastViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : StopForecastViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
                R.layout.cardview_stop_forecast,
                parent,
                false
            )

        return StopForecastViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: StopForecastViewHolder, position: Int) {
        val (destination, dueMinutes, minOrMins) = listStopForecastInfo[position]

        holder.textViewDestination?.text = destination
        holder.textViewDueMinutes?.text = dueMinutes
        holder.textViewMinOrMins?.text = minOrMins

        /* If the tram is due, don't show the "min/mins" TextView to better centre the DUE text. */
        if (holder.textViewMinOrMins?.text == "") {
            holder.textViewMinOrMins.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return listStopForecastInfo.size
    }
}

