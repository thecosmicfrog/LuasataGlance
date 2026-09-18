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

import androidx.recyclerview.widget.RecyclerView
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.adapter.StopSelectAdapter
import org.thecosmicfrog.luasataglance.model.Stops

/**
 * Tests the order [StopSelectActivity.loadAllStops] hands the picker, through [DefaultStopSelectActivity].
 *
 * The Favourites and widget pickers inherit the same list, so one subclass stands in for all three. The default stop picker
 * is the one with a "None" row, which has to stay at the top rather than be sorted in among the stops.
 */
@RunWith(RobolectricTestRunner::class)
class StopSelectActivityTest {

    /**
     * The rows the picker shows, top to bottom, read by binding each position the way the RecyclerView would.
     */
    private fun pickerRows(): List<String> {
        val activity = Robolectric.buildActivity(DefaultStopSelectActivity::class.java).setup().get()
        val recyclerView = activity.findViewById<RecyclerView>(R.id.recyclerview_stops)
        val adapter = recyclerView.adapter as StopSelectAdapter
        val holder = adapter.onCreateViewHolder(recyclerView, 0)

        return (0 until adapter.itemCount).map {
            adapter.onBindViewHolder(holder, it)
            holder.stopName.text.toString()
        }
    }

    @Test
    fun `None stays at the top and the stops follow it alphabetically`() {
        val rows = pickerRows()
        val stops = rows.drop(1)

        assertThat(rows.first()).isEqualTo("None")
        assertThat(stops).isEqualTo(stops.sortedWith(Stops.nameOrder))
        assertThat(stops).hasSize(Stops.all.size)
    }

    @Test
    @Config(qualifiers = "ga")
    fun `a fada sorts in with its base letter rather than after Z`() {
        val rows = pickerRows()

        /* Code point order put both of these after Westmoreland, the last unaccented name. */
        assertThat(rows.indexOf("Áth an Ghainimh")).isLessThan(rows.indexOf("Baile Amhlaoibh"))
        assertThat(rows.indexOf("Ó Conaill - AOP")).isLessThan(rows.indexOf("Ospidéal San Séamas"))
        assertThat(rows.last()).isEqualTo("Westmoreland")
    }
}
