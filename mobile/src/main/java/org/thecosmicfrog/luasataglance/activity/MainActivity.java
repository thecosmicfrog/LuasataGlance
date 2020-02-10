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

package org.thecosmicfrog.luasataglance.activity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.perf.FirebasePerformance;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.adapter.ReplacerPagerAdapter;
import org.thecosmicfrog.luasataglance.util.Constant;
import org.thecosmicfrog.luasataglance.util.Preferences;
import org.thecosmicfrog.luasataglance.view.NonSwipeableViewPager;

public class MainActivity extends AppCompatActivity {

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private NonSwipeableViewPager nonSwipeableViewPagerReplacer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        configureFirebasePerformanceCollection();

        setContentView(R.layout.activity_main);

        setUpAppNavigation();

        getScreenHeight();

        configureAppAesthetics();

        showWhatsNewDialog();
    }

    private void configureAppAesthetics() {
        /* Hide the ActionBar for aesthetic reasons. */
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        /* Set status and navigation bar colour. */
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(
                ContextCompat.getColor(
                        getApplicationContext(),
                        R.color.luas_purple_statusbar
                )
        );
        window.setNavigationBarColor(
                ContextCompat.getColor(
                        getApplicationContext(),
                        R.color.luas_purple_statusbar
                ));
    }

    private void setUpAppNavigation() {
        BottomNavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener =
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.menuitem_bottomnav_trams:
                                nonSwipeableViewPagerReplacer.setCurrentItem(
                                        Constant.BOTTOMNAV_MENU_ITEM_INDEX_TRAMS
                                );

                                break;

                            case R.id.menuitem_bottomnav_favourites:
                                nonSwipeableViewPagerReplacer.setCurrentItem(
                                        Constant.BOTTOMNAV_MENU_ITEM_INDEX_FAVOURITES
                                );

                                break;

                            case R.id.menuitem_bottomnav_map:
                                nonSwipeableViewPagerReplacer.setCurrentItem(
                                        Constant.BOTTOMNAV_MENU_ITEM_INDEX_MAP
                                );

                                break;

                            case R.id.menuitem_bottomnav_alerts:
                                nonSwipeableViewPagerReplacer.setCurrentItem(
                                        Constant.BOTTOMNAV_MENU_ITEM_INDEX_ALERTS
                                );

                                break;
                        }

                        return false;
                    }
                };

        final BottomNavigationView bottomNavigationView = findViewById(R.id.bottomnavigationview);
        bottomNavigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener);

        int bottomNavigationViewItemCount = bottomNavigationView.getMenu().size();

        ReplacerPagerAdapter replacerPagerAdapter = new ReplacerPagerAdapter(
                getSupportFragmentManager(),
                bottomNavigationViewItemCount
        );

        nonSwipeableViewPagerReplacer = findViewById(R.id.nonswipeableviewpager_replacer);
        nonSwipeableViewPagerReplacer.setSwipingEnabled(false);
        nonSwipeableViewPagerReplacer.setOffscreenPageLimit(bottomNavigationViewItemCount - 1);
        nonSwipeableViewPagerReplacer.setAdapter(replacerPagerAdapter);
        nonSwipeableViewPagerReplacer.addOnPageChangeListener(
                new NonSwipeableViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                /* When the page changes, highlight the relevant BottomNavigationView item. */
                switch (position) {
                    case Constant.BOTTOMNAV_MENU_ITEM_INDEX_TRAMS:
                        bottomNavigationView.getMenu().findItem(
                                R.id.menuitem_bottomnav_trams
                        ).setChecked(true);

                        break;

                    case Constant.BOTTOMNAV_MENU_ITEM_INDEX_FAVOURITES:
                        bottomNavigationView.getMenu().findItem(
                                R.id.menuitem_bottomnav_favourites
                        ).setChecked(true);

                        break;

                    case Constant.BOTTOMNAV_MENU_ITEM_INDEX_MAP:
                        bottomNavigationView.getMenu().findItem(
                                R.id.menuitem_bottomnav_map
                        ).setChecked(true);

                        break;

                    case Constant.BOTTOMNAV_MENU_ITEM_INDEX_ALERTS:
                        bottomNavigationView.getMenu().findItem(
                                R.id.menuitem_bottomnav_alerts
                        ).setChecked(true);

                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        /* If the Intent has changed, update the Activity's Intent. */
        setIntent(intent);
    }

    /**
     * Check whether or not we are running in Firebase Test Lab.
     * @return Whether or not we are running in Firebase Test Lab.
     */
    private boolean isRunningInFirebaseTestLab() {
        String settingFirebaseTestLab =
                Settings.System.getString(getContentResolver(), "firebase.test.lab");

        Log.i(LOG_TAG, "Running in Firebase Test Lab.");

        return settingFirebaseTestLab != null && settingFirebaseTestLab.equals("true");
    }

    /**
     * Show What's New dialog to user if they have recently updated the app.
     */
    private void showWhatsNewDialog() {
        /* Don't show the What's New dialog if we're running in Firebase Test Lab. */
        if (isRunningInFirebaseTestLab()) {
            Log.i(
                    LOG_TAG,
                    "Running in Firebase Test Lab. Not showing What's New dialog."
            );

            return;
        }

        /*
         * Load two values for the current app version. One comes from strings.xml and the other
         * comes from shared preferences. The value from strings.xml should be considered the
         * definitive value.
         */
        String appVersionCurrent =
                getString(R.string.version_name).replace(".", "");
        String appVersionSaved =
                Preferences.currentAppVersion(getApplicationContext());

        double appVersionCurrentNumeric = Double.parseDouble(appVersionCurrent);
        double appVersionSavedNumeric =
                Double.parseDouble(appVersionSaved);

        /*
         * If the definitive current app version is greater than the version stored in shared
         * preferences, the user has recently updated the app to a newer version.
         * In this case, display the What's New dialog.
         */
        if (appVersionCurrentNumeric > appVersionSavedNumeric) {
            Log.i(
                    LOG_TAG,
                    "User has updated to version " + appVersionCurrent + " from "
                            + appVersionSaved + ". Displaying What's New Dialog."
            );

            startActivity(
                    new Intent(
                            getApplicationContext(),
                            WhatsNewActivity.class
                    )
            );

            /* Overwrite the previous current app version with the known new value. */
            Preferences.saveCurrentAppVersion(
                    getApplicationContext(),
                    appVersionCurrent
            );
        }
    }

    private void getScreenHeight() {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);

        float density  = getResources().getDisplayMetrics().density;
        float dpHeight = displayMetrics.heightPixels / density;

        Preferences.saveScreenHeight(getApplicationContext(), dpHeight);
    }

    /**
     * Enable or disable Firebase Performance collection.
     */
    private void configureFirebasePerformanceCollection() {
        /* Disable Firebase Performance collection if we're running in Firebase Test Lab. */
        if (isRunningInFirebaseTestLab()) {
            Log.i(
                    LOG_TAG,
                    "Running in Firebase Test Lab. Disabling Firebase Performance collection."
            );

            FirebasePerformance.getInstance().setPerformanceCollectionEnabled(false);
        }
    }
}

