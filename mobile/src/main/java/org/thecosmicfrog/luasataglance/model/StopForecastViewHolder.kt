package org.thecosmicfrog.luasataglance.model

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.thecosmicfrog.luasataglance.R

open class StopForecastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val textViewDestination: TextView? =
        itemView.findViewById(R.id.textview_stop_forecast_destination)

    val textViewDueMinutes: TextView? =
        itemView.findViewById(R.id.textview_stop_forecast_time_value)
}

