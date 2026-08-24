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

import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.model.FavouriteInfo
import org.thecosmicfrog.luasataglance.model.FavouriteViewHolder
import org.thecosmicfrog.luasataglance.model.Stops
import org.thecosmicfrog.luasataglance.util.Constant

class FavouriteAdapter(private val listFavouriteInfo: List<FavouriteInfo>):
    RecyclerView.Adapter<FavouriteViewHolder>() {

    private val logTag = FavouriteAdapter::class.java.simpleName

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : FavouriteViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
                R.layout.cardview_favourite,
                parent,
                false
            )

        return FavouriteViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FavouriteViewHolder, position: Int) {
        val favouriteStopName = listFavouriteInfo[position].stopName

        holder.textViewFavouriteStopName ?.text = favouriteStopName

        setLineIndicatorColor(holder, favouriteStopName)

        setOnClickListeners(holder, favouriteStopName)
    }

    /**
     * Set OnClickListener for each item in the RecyclerView.
     * @param holder The ViewHolder for the RecyclerView.
     * @param favouriteStopName The name of the stop.
     */
    private fun setOnClickListeners(holder: FavouriteViewHolder, favouriteStopName: CharSequence) {
        holder.itemView.setOnClickListener {
            val localBroadcastManager = LocalBroadcastManager.getInstance(holder.itemView.context)

            val intent = Intent(Constant.INTENT_ACTION_LOAD_STOP)
            intent.putExtra(Constant.INTENT_EXTRA_STOP_NAME, favouriteStopName)

            localBroadcastManager.sendBroadcast(intent)
        }
    }

    /**
     * Set line indicator color based on the stop name and its associated line.
     * @param holder The ViewHolder for the RecyclerView.
     * @param favouriteStopName The name of the stop.
     */
    private fun setLineIndicatorColor(holder: FavouriteViewHolder, favouriteStopName: CharSequence) {
        val lineIndicator = holder.itemView.findViewById<View>(R.id.view_line_indicator)
        val context = holder.itemView.context

        when (Stops.line(Stops.idForName(context, favouriteStopName.toString()))) {
            Constant.RED_LINE -> {
                lineIndicator.setBackgroundColor(context.getColor(R.color.tab_red_line))
            }

            Constant.GREEN_LINE -> {
                lineIndicator.setBackgroundColor(context.getColor(R.color.tab_green_line))
            }

            else -> {
                lineIndicator.setBackgroundColor(Color.TRANSPARENT)
                Log.wtf(logTag, "Stop name not found in red or green line arrays.")
            }
        }
    }

    override fun getItemCount(): Int {
        return listFavouriteInfo.size
    }
}
