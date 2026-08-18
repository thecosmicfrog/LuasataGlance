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
package org.thecosmicfrog.luasataglance.activity

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.adapter.FavouritesSelectAdapter
import org.thecosmicfrog.luasataglance.databinding.ActivityStopSelectBinding

/**
 * Abstract base Activity for selecting a list of stops with checkboxes.
 *
 * Handles the shared UI (toolbar, RecyclerView with [FavouritesSelectAdapter], save FAB) and stop loading. Subclasses provide the
 * toolbar title and FAB text, and implement the load and save behaviour specific to their use case.
 */
abstract class StopSelectActivity : AppCompatActivity() {

    /** String resource for the toolbar title. */
    @get:StringRes
    protected abstract val toolbarTitleRes: Int

    /** String resource for the save FAB label. */
    @get:StringRes
    protected abstract val fabTextRes: Int

    /**
     * The mutable list of currently selected stops, shared with the adapter so that tap events update it in place.
     */
    protected var selectedItems: ArrayList<CharSequence?> = ArrayList()

    private lateinit var viewBinding: ActivityStopSelectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityStopSelectBinding.inflate(layoutInflater)

        setContentView(viewBinding.root)

        viewBinding.toolbarStopSelect.title = getString(toolbarTitleRes)

        selectedItems = ArrayList(loadSavedStops())
        initRecyclerView(loadAllStops())
        initFab()
    }

    /**
     * Load all stops from resources, sorted alphabetically.
     *
     * @return Alphabetically sorted list of all stop names.
     */
    private fun loadAllStops(): ArrayList<CharSequence?> {
        val allStops = resources.getStringArray(R.array.array_stops_all)

        /*
         * Create and return a sorted ArrayList of stop names, skipping the first element in the array ("Select a stop"),
         * casting to CharSequence for adapter compatibility, and sorting alphabetically in a case-insensitive manner.
         */
        return ArrayList(
            (1 until allStops.size)
                .map { allStops[it] as CharSequence? }
                .sortedBy { it?.toString()?.lowercase() }
        )
    }

    /**
     * Initialise the RecyclerView.
     *
     * @param stops The list of stop names to display in the RecyclerView.
     */
    private fun initRecyclerView(stops: ArrayList<CharSequence?>) {
        viewBinding.recyclerviewStops.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = FavouritesSelectAdapter(stops, selectedItems)
        }
    }

    /**
     * Initialise the save FAB.
     */
    private fun initFab() {
        viewBinding.fabSave.apply {
            /* Set the background colour of the FAB to green. */
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.message_success))

            setText(fabTextRes)
            setOnClickListener { onSave() }
        }
    }

    /**
     * Load the previously saved stops to pre-check in the list.
     *
     * @return The saved list of stop names, or an empty list if none have been saved yet.
     */
    protected abstract fun loadSavedStops(): List<CharSequence>

    /**
     * Called when the user taps the save FAB. Implementations should persist [selectedItems] and close the Activity.
     */
    protected abstract fun onSave()
}
