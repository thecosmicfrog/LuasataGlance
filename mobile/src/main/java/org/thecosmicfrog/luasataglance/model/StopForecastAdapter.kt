package org.thecosmicfrog.luasataglance.model

import android.view.LayoutInflater
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
        val (destination, dueMinutes) = listStopForecastInfo[position]

        holder.textViewDestination?.text = destination
        holder.textViewDueMinutes?.text = dueMinutes
    }

    override fun getItemCount(): Int {
        return listStopForecastInfo.size
    }
}

