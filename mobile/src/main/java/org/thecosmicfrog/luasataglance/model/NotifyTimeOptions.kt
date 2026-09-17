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

/**
 * The "mins before arrival" values a reminder can be set to, for one tram. NotifyTimeActivity fills its spinner from this, and
 * [org.thecosmicfrog.luasataglance.util.StopForecastUtil] and [org.thecosmicfrog.luasataglance.model.StopForecastAdapter] use it to
 * decide whether a tram row can open the dialog at all.
 */
object NotifyTimeOptions {

    /* A reminder 1 minute before arrival is too late to be of any use, so 2 is the earliest offered. */
    const val MIN_MINS_BEFORE_ARRIVAL = 2

    /* The most warning a reminder gives, however far off the tram is. */
    const val MAX_MINS_BEFORE_ARRIVAL = 15

    /**
     * Minutes before arrival a reminder can be set for, ascending. Empty when the tram is too close for any reminder.
     *
     * A tram due in 9 minutes gives 2 to 8. NotifyTimesReceiver fires 30 seconds ahead of the requested time and refuses anything
     * that would put the alarm in the past.
     *
     * @param tramDueInMins Minutes until the tram arrives, as the forecast reported it.
     */
    fun forTramDueIn(tramDueInMins: Int): List<Int> =
        (MIN_MINS_BEFORE_ARRIVAL..minOf(MAX_MINS_BEFORE_ARRIVAL, tramDueInMins - 1)).toList()

    /**
     * Whether or not a tram row can open the reminder dialog. A row shows "DUE" or "" rather than a number when there is no tram to
     * remind about.
     *
     * @param dueMinutes The row's due time, as displayed.
     */
    fun canSchedule(dueMinutes: String?): Boolean = dueMinutes?.toIntOrNull()?.let { forTramDueIn(it).isNotEmpty() } == true
}
