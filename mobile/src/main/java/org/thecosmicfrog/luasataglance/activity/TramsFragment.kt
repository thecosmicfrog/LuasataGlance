/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2020 Aaron Hastings
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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.Settings

class TramsFragment : Fragment() {

    private val logTag = TramsFragment::class.java.simpleName
    private var rootView: View? = null

    companion object {
        fun newInstance(): Fragment {
            val tramsFragment = TramsFragment()
            val bundle = Bundle()

            tramsFragment.arguments = bundle

            return tramsFragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        /* Inflate the layout for this Fragment. */
        rootView = inflater.inflate(R.layout.fragment_trams, container, false)

        return rootView
    }

    override fun onResume() {
        super.onResume()

        if (isAdded) {
            initOverflowMenu()

            initFragment()
        }
    }

    private fun initOverflowMenu() {
        val imageButton = rootView?.findViewById<ImageButton>(R.id.imagebutton_overflow_menu)
        imageButton?.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val popupMenuOverflowMenu = PopupMenu(context as Context, v as View)

                popupMenuOverflowMenu.inflate(R.menu.menu_overflow)
                popupMenuOverflowMenu.show()

                popupMenuOverflowMenu.setOnMenuItemClickListener(
                    object : android.widget.PopupMenu.OnMenuItemClickListener,
                        PopupMenu.OnMenuItemClickListener {
                        override fun onMenuItemClick(item: MenuItem?): Boolean {
                            Settings.getSettings(
                                context,
                                item
                            )
                            return true
                        }
                    })
            }
        })
    }

    private fun initFragment() {
        val viewPager = rootView?.findViewById<ViewPager>(R.id.trams_viewpager)
        val tabLayout = rootView?.findViewById<TabLayout>(R.id.trams_tablayout)

        /* Only add tabs if they don't already exist. */
        if (tabLayout?.tabCount ?: 0 < 2) {
            tabLayout?.addTab(
                tabLayout.newTab().setTag(Constant.RED_LINE).setText(
                    getString(R.string.tab_red_line)
                )
            )
            tabLayout?.addTab(
                tabLayout.newTab().setTag(Constant.GREEN_LINE).setText(
                    getString(R.string.tab_green_line)
                )
            )
            tabLayout?.tabGravity = TabLayout.GRAVITY_FILL
            tabLayout?.setBackgroundColor(
                ContextCompat.getColor(context as Context, R.color.luas_purple)
            )

            tabLayout?.setOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    viewPager?.currentItem = tab?.position as Int

                    changeTabIndicatorColor(tabLayout)
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {}

                override fun onTabUnselected(tab: TabLayout.Tab?) {}
            })

            val pagerAdapter = PagerAdapter(
                childFragmentManager,
                tabLayout?.tabCount as Int
            )

            viewPager?.adapter = pagerAdapter
            viewPager?.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))

            changeTabIndicatorColor(tabLayout)
        }
    }

    private fun changeTabIndicatorColor(tabLayout: TabLayout) {
        when (tabLayout.selectedTabPosition) {
            0 ->
                tabLayout.setSelectedTabIndicatorColor(
                    ContextCompat.getColor(context as Context, R.color.tab_red_line)
                )

            1 ->
                tabLayout.setSelectedTabIndicatorColor(
                    ContextCompat.getColor(context as Context, R.color.tab_green_line)
                )

            else -> return
        }
    }
}

