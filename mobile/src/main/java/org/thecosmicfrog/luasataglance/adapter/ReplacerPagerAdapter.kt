/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2025 Aaron Hastings
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

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import org.thecosmicfrog.luasataglance.activity.AlertsFragment
import org.thecosmicfrog.luasataglance.activity.FavouritesFragment
import org.thecosmicfrog.luasataglance.activity.MapsFragment
import org.thecosmicfrog.luasataglance.activity.TramsFragment

class ReplacerPagerAdapter(fm: FragmentManager?,
                           private val numPages: Int) : FragmentStatePagerAdapter(fm!!) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> TramsFragment.newInstance()
            1 -> FavouritesFragment.newInstance()
            2 -> MapsFragment.newInstance()
            3 -> AlertsFragment.newInstance()

            else -> Fragment()
        }
    }

    override fun getCount(): Int {
        return numPages
    }
}

