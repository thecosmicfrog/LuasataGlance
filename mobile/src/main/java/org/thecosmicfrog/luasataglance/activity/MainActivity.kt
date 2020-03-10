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
 * along with Luas at a Glance.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package org.thecosmicfrog.luasataglance.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.adapter.ReplacerPagerAdapter
import org.thecosmicfrog.luasataglance.databinding.ActivityMainBinding
import org.thecosmicfrog.luasataglance.util.AppUtil.configureFirebasePerformanceCollection
import org.thecosmicfrog.luasataglance.util.AppUtil.getScreenHeight
import org.thecosmicfrog.luasataglance.util.AppUtil.isRunningInFirebaseTestLab
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.Preferences
import org.thecosmicfrog.luasataglance.view.NonSwipeableViewPager

class MainActivity : AppCompatActivity() {

    private val logTag = MainActivity::class.java.simpleName

    private lateinit var binding: ActivityMainBinding
    private lateinit var nonSwipeableViewPagerReplacer: NonSwipeableViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureFirebasePerformanceCollection(applicationContext)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        setUpAppNavigation()

        getScreenHeight(windowManager, resources, applicationContext)

        configureAppAesthetics()

        showWhatsNewDialog()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        /* If the Intent has changed, update the Activity's Intent. */
        setIntent(intent)
    }

    private fun configureAppAesthetics() {
        /* Hide the ActionBar for aesthetic reasons. */
        supportActionBar?.hide()

        /* Set status and navigation bar colour. */
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = ContextCompat.getColor(
            applicationContext,
            R.color.luas_purple_statusbar
        )
        window.navigationBarColor = ContextCompat.getColor(
            applicationContext,
            R.color.luas_purple_statusbar
        )
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
            })
    }

    /**
     * Show What's New dialog to user if they have recently updated the app.
     */
    private fun showWhatsNewDialog() {
        /* Don't show the What's New dialog if we're running in Firebase Test Lab. */
        if (isRunningInFirebaseTestLab(applicationContext)) {
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
        val appVersionSaved = Preferences.currentAppVersion(applicationContext)
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
                    applicationContext,
                    WhatsNewActivity::class.java
                )
            )

            /* Overwrite the previous current app version with the known new value. */
            Preferences.saveCurrentAppVersion(
                applicationContext,
                appVersionCurrent
            )
        }
    }
}

