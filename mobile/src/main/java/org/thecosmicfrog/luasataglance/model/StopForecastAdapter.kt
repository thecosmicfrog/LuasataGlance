package org.thecosmicfrog.luasataglance.model

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.util.AppUtil
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.Preferences
import org.thecosmicfrog.luasataglance.util.StopForecastUtil

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
        val regexCannotScheduleNotification = Regex(
            "${holder.textViewDueMinutes?.resources?.getString(R.string.due)}\$|1\$|2\$"
        )

        holder.textViewDestination?.text = destination
        holder.textViewDueMinutes?.text = dueMinutes
        holder.textViewMinOrMins?.text = minOrMins


        /* If the tram is due, don't show the "min/mins" TextView to better centre the DUE text. */
        if (holder.textViewMinOrMins?.text.isNullOrBlank()) {
            holder.textViewMinOrMins?.visibility = View.GONE
        }

        when (holder.textViewDueMinutes?.text?.matches(regexCannotScheduleNotification)) {
            true -> {
                holder.textViewSetReminder?.visibility = View.GONE
            }
        }

        /* Set OnClickListener for each item in the RecyclerView. */
        holder.itemView.setOnClickListener {
            StopForecastUtil.showNotifyTimeDialog(
                it.rootView,
                Preferences.selectedStopName(it.context, Constant.NO_LINE),
                holder.textViewDueMinutes?.text as String,
                it.resources
            )
        }
    }

    override fun getItemCount(): Int {
        return listStopForecastInfo.size
    }
}

