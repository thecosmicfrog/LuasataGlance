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

import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.util.Preferences

/**
 * Allows the user to select the stop loaded on startup. Extends [StopSelectActivity], picking a single stop rather than a list,
 * and persists the selection to the default stop preference read by [LineFragment].
 */
class DefaultStopSelectActivity : StopSelectActivity() {

    override val toolbarTitleRes = R.string.title_activity_default_stop_select
    override val fabTextRes = R.string.pref_default_stop_save
    override val isSingleSelect = true

    /**
     * All stops, with "None" at the top rather than sorted in among them, since it clears the default stop rather than being a stop
     * the user can travel from.
     *
     * @return "None" followed by the alphabetically sorted stop names.
     */
    override fun loadAllStops(): ArrayList<CharSequence?> {
        return ArrayList<CharSequence?>(listOf(getString(R.string.none))).apply { addAll(super.loadAllStops()) }
    }

    override fun loadSavedStops(): List<CharSequence> {
        return listOf(Preferences.defaultStopName(this) ?: getString(R.string.none))
    }

    override fun onSave() {
        /* Backing out without tapping a stop leaves selectedItems holding the saved stop, so it is written back unchanged. */
        Preferences.saveDefaultStopName(this, (selectedItems.firstOrNull() ?: getString(R.string.none)).toString())

        finish()
    }
}
