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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.adapter.ReplacerPagerAdapter
import org.thecosmicfrog.luasataglance.databinding.ActivityMainBinding
import org.thecosmicfrog.luasataglance.util.AppUtil.getScreenHeight
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.view.NonSwipeableViewPager

class MainActivity : AppCompatActivity() {

    private val logTag = MainActivity::class.java.simpleName

    private lateinit var binding: ActivityMainBinding
    private lateinit var nonSwipeableViewPagerReplacer: NonSwipeableViewPager

    private var broadcastReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            enableEdgeToEdge()

            /* Ensure the system status and navigation bars are light in colour regardless of light/dark mode. */
            WindowInsetsControllerCompat(window, binding.root).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }

            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.updatePadding(top = insets.top, bottom = insets.bottom)
                WindowInsetsCompat.CONSUMED
            }

            window.isNavigationBarContrastEnforced = false
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true)

            window.statusBarColor = resources.getColor(R.color.luas_purple_statusbar)
            window.navigationBarColor = resources.getColor(R.color.luas_purple_statusbar)
        }

        setContentView(binding.root)

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                nonSwipeableViewPagerReplacer.currentItem = Constant.BOTTOMNAV_MENU_ITEM_INDEX_TRAMS
            }
        }
        broadcastReceiver?.let {
            LocalBroadcastManager.getInstance(applicationContext).registerReceiver(
                it,
                IntentFilter(Constant.INTENT_ACTION_LOAD_STOP)
            )
        }

        setUpAppNavigation()

        getScreenHeight(windowManager, resources, applicationContext)

        configureAppAesthetics()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()

        broadcastReceiver?.let {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(it)
        }
        broadcastReceiver = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        /* If the Intent has changed, update the Activity's Intent. */
        setIntent(intent)
    }

    private fun configureAppAesthetics() {
        /* Hide the ActionBar for aesthetic reasons. */
        supportActionBar?.hide()
    }

    private fun setUpAppNavigation() {
        val onNavigationItemSelectedListener =
            BottomNavigationView.OnNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menuitem_bottomnav_trams ->
                        nonSwipeableViewPagerReplacer.currentItem =
                            Constant.BOTTOMNAV_MENU_ITEM_INDEX_TRAMS

                    R.id.menuitem_bottomnav_favourites ->
                        nonSwipeableViewPagerReplacer.currentItem =
                            Constant.BOTTOMNAV_MENU_ITEM_INDEX_FAVOURITES

                    R.id.menuitem_bottomnav_map ->
                        nonSwipeableViewPagerReplacer.currentItem =
                            Constant.BOTTOMNAV_MENU_ITEM_INDEX_MAP

                    R.id.menuitem_bottomnav_alerts ->
                        nonSwipeableViewPagerReplacer.currentItem =
                            Constant.BOTTOMNAV_MENU_ITEM_INDEX_ALERTS
                }

                false
            }

        val bottomNavigationView = binding.bottomnavigationview
        bottomNavigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)
        val bottomNavigationViewItemCount = bottomNavigationView.menu.size()

        val replacerPagerAdapter = ReplacerPagerAdapter(
            supportFragmentManager,
            bottomNavigationViewItemCount
        )

        nonSwipeableViewPagerReplacer = binding.nonswipeableviewpagerReplacer
        nonSwipeableViewPagerReplacer.swipingEnabled = false
        nonSwipeableViewPagerReplacer.offscreenPageLimit = bottomNavigationViewItemCount - 1
        nonSwipeableViewPagerReplacer.adapter = replacerPagerAdapter
        nonSwipeableViewPagerReplacer.addOnPageChangeListener(
            object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(
                    position: Int, positionOffset: Float,
                    positionOffsetPixels: Int) {}

                override fun onPageSelected(position: Int) {
                    /*
                     * When the page changes, highlight the relevant BottomNavigationView item.
                     */
                    when (position) {
                        Constant.BOTTOMNAV_MENU_ITEM_INDEX_TRAMS ->
                            bottomNavigationView.menu.findItem(
                                R.id.menuitem_bottomnav_trams
                            ).isChecked = true

                        Constant.BOTTOMNAV_MENU_ITEM_INDEX_FAVOURITES ->
                            bottomNavigationView.menu.findItem(
                                R.id.menuitem_bottomnav_favourites
                            ).isChecked = true

                        Constant.BOTTOMNAV_MENU_ITEM_INDEX_MAP ->
                            bottomNavigationView.menu.findItem(
                                R.id.menuitem_bottomnav_map
                            ).isChecked = true

                        Constant.BOTTOMNAV_MENU_ITEM_INDEX_ALERTS ->
                            bottomNavigationView.menu.findItem(
                                R.id.menuitem_bottomnav_alerts
                            ).isChecked = true
                    }
                }

                override fun onPageScrollStateChanged(state: Int) {}
            }
        )
    }
}
