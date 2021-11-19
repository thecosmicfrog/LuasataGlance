package org.thecosmicfrog.luasataglance.model

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.thecosmicfrog.luasataglance.R

class StopForecastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val textViewDestination: TextView? =
        itemView.findViewById(R.id.textview_stop_forecast_destination)

    val textViewDueMinutes: TextView? =
        itemView.findViewById(R.id.textview_stop_forecast_due_time_value)

    val textViewMinOrMins: TextView? =
        itemView.findViewById(R.id.textview_stop_forecast_due_time_min_mins)

    val textViewSetReminder: TextView? =
        itemView.findViewById(R.id.textview_stop_forecast_set_reminder)
}

