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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.adapter.TabsPagerAdapter
import org.thecosmicfrog.luasataglance.model.Stops
import org.thecosmicfrog.luasataglance.util.AppUtil
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.Preferences
import org.thecosmicfrog.luasataglance.util.Settings

class TramsFragment : Fragment() {

    private val logTag = TramsFragment::class.java.simpleName

    private var rootView: View? = null
    private var viewPager: ViewPager? = null
    private var broadcastReceiver: BroadcastReceiver? = null

    companion object {
        fun newInstance(): Fragment {
            val tramsFragment = TramsFragment()
            val bundle = Bundle()

            tramsFragment.arguments = bundle

            return tramsFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        /* Inflate the layout for this Fragment. */
        rootView = inflater.inflate(R.layout.fragment_trams, container, false)

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (!isAdded) return

                val stopName = intent?.getStringExtra(Constant.INTENT_EXTRA_STOP_NAME)
                val stopLine = Stops.line(Stops.idForName(requireContext(), stopName))

                viewPager?.currentItem = when (stopLine) {
                    Constant.RED_LINE -> 0
                    Constant.GREEN_LINE -> 1
                    else -> 0
                }
            }
        }

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initFragment()
    }

    override fun onResume() {
        super.onResume()

        if (!isAdded) return

        initOverflowMenu()
        showWhatsNewDialog()

        broadcastReceiver?.let {
            LocalBroadcastManager.getInstance(context as Context).registerReceiver(
                it,
                IntentFilter(Constant.INTENT_ACTION_LOAD_STOP)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        broadcastReceiver?.let { broadcastReceiver ->
            context?.let { ctx ->
                LocalBroadcastManager.getInstance(ctx).unregisterReceiver(broadcastReceiver)
            }
        }
        broadcastReceiver = null

        viewPager?.adapter = null
        viewPager = null
        rootView = null
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
                            Settings.getSettings(context, item)

                            return true
                        }
                    }
                )
            }
        })
    }

    private fun initFragment() {
        viewPager = rootView?.findViewById<ViewPager>(R.id.trams_viewpager)
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

            tabLayout?.setOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    viewPager?.currentItem = tab?.position as Int

                    changeTabIndicatorColor(tabLayout)
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {}

                override fun onTabUnselected(tab: TabLayout.Tab?) {}
            })

            val pagerAdapter =
                TabsPagerAdapter(
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
                    ContextCompat.getColor(requireContext(), R.color.tab_red_line)
                )

            1 ->
                tabLayout.setSelectedTabIndicatorColor(
                    ContextCompat.getColor(requireContext(), R.color.tab_green_line)
                )

            else -> return
        }
    }

    private fun showWhatsNewDialog() {
        /* Don't show the What's New dialog if we're running in Firebase Test Lab. */
        if (AppUtil.isRunningInFirebaseTestLab(context)) {
            Log.i(
                logTag,
                "Running in Firebase Test Lab. Not showing What's New dialog."
            )

            return
        }

        /*
         * Load two values for the current app version. One comes from strings.xml and the other
         * comes from shared preferences. The value from strings.xml should be considered the
         * definitive value.
         */
        val appVersionCurrent = getString(R.string.version_name).replace(".", "")
        val appVersionSaved = Preferences.currentAppVersion(context)
        val appVersionCurrentNumeric = appVersionCurrent.toDouble()
        val appVersionSavedNumeric = appVersionSaved.toDouble()

        /*
         * If the definitive current app version is greater than the version stored in shared
         * preferences, the user has recently updated the app to a newer version.
         * In this case, display the What's New dialog.
         */
        if (appVersionCurrentNumeric > appVersionSavedNumeric) {
            Log.i(
                logTag,
                "User has updated to version $appVersionCurrent from $appVersionSaved. " +
                        "Displaying What's New Dialog."
            )

            startActivity(
                Intent(
                    context,
                    WhatsNewActivity::class.java
                )
            )

            /* Overwrite the previous current app version with the known new value. */
            Preferences.saveCurrentAppVersion(context, appVersionCurrent)
        }
    }
}

