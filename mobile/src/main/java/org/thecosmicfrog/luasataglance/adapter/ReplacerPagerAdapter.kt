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

