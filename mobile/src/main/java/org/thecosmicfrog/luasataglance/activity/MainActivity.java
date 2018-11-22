/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2018 Aaron Hastings
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
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.util.Analytics;
import org.thecosmicfrog.luasataglance.util.Constant;
import org.thecosmicfrog.luasataglance.util.Preferences;
import org.thecosmicfrog.luasataglance.view.TutorialCardView;

public class MainActivity extends AppCompatActivity {

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private static ImageView imageViewBottomNavAlerts;
    private static TextView textViewBottomNavAlerts;

    private TextView textViewBottomNavMap;
    private TextView textViewBottomNavFavourites;
    private TextView textViewBottomNavFares;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        /* Set the ActionBar elevation to 0. */
        if (getSupportActionBar() != null)
            getSupportActionBar().setElevation(0f);

        /* Set status bar colour. */
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(
                    ContextCompat.getColor(getApplicationContext(),
                            R.color.luas_purple_statusbar)
            );
        }

        /*
         * Initialise ViewPager and TabLayout.
         */
        final ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tablayout);

        if (tabLayout != null && viewPager != null) {
            tabLayout.addTab(
                    tabLayout.newTab().setTag(Constant.RED_LINE).setText(
                            getString(R.string.tab_red_line)
                    )
            );
            tabLayout.addTab(
                    tabLayout.newTab().setTag(Constant.GREEN_LINE).setText(
                            getString(R.string.tab_green_line)
                    )
            );
            tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
            tabLayout.setBackgroundColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.luas_purple)
            );

            tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    viewPager.setCurrentItem(tab.getPosition());

                    setTabIndicatorColor(tabLayout);
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });

            final PagerAdapter pagerAdapter = new PagerAdapter(
                    getSupportFragmentManager(),
                    tabLayout.getTabCount()
            );

            viewPager.setAdapter(pagerAdapter);
            viewPager.addOnPageChangeListener(
                    new TabLayout.TabLayoutOnPageChangeListener(tabLayout)
            );

            setTabIndicatorColor(tabLayout);
        } else {
            Log.wtf(LOG_TAG, "tabLayout or viewPager is null.");
        }

        /*
         * Bottom navigation bar - Luas Map.
         */
        RelativeLayout relativeLayoutBottomNavMap =
                (RelativeLayout) findViewById(R.id.relativelayout_bottomnav_map);
        if (relativeLayoutBottomNavMap != null) {
            relativeLayoutBottomNavMap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    /*
                     * Open Maps Activity.
                     * If we have a selected stop saved to shared preferences, open the map on that
                     * stop. Otherwise, open the map at a default position.
                     */
                    if (Preferences.selectedStopName(getApplicationContext(), "no_line") != null) {
                        startActivity(
                                new Intent(
                                        getApplicationContext(),
                                        MapsActivity.class
                                ).putExtra(
                                        Constant.STOP_NAME,
                                        Preferences.selectedStopName(
                                                getApplicationContext(),
                                                Constant.NO_LINE
                                        )
                                )
                        );
                    } else {
                        startActivity(
                                new Intent(
                                        getApplicationContext(),
                                        MapsActivity.class
                                )
                        );
                    }

                    Analytics.selectContent(
                            getApplicationContext(),
                            "button_tapped",
                            "map_tapped"
                    );
                }
            });
        }

        /*
         * Bottom navigation bar - Favourites.
         */
        RelativeLayout relativeLayoutBottomNavFavourites =
                (RelativeLayout) findViewById(R.id.relativelayout_bottomnav_favourites);
        if (relativeLayoutBottomNavFavourites != null) {
            relativeLayoutBottomNavFavourites.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TutorialCardView tutorialCardViewFavourites =
                            (TutorialCardView) findViewById(R.id.tutorialcardview_favourites);
                    if (tutorialCardViewFavourites != null) {
                        tutorialCardViewFavourites.setVisibility(View.GONE);
                    }

                    /* Open Favourites Activity. */
                    startActivity(
                            new Intent(
                                    getApplicationContext(),
                                    FavouritesActivity.class
                            )
                    );

                    Analytics.selectContent(
                            getApplicationContext(),
                            "button_tapped",
                            "favourites_tapped"
                    );
                }
            });
        }

        /*
         * Bottom navigation bar - Fares.
         */
        RelativeLayout relativeLayoutBottomNavFares =
                (RelativeLayout) findViewById(R.id.relativelayout_bottomnav_fares);
        if (relativeLayoutBottomNavFares != null) {
            relativeLayoutBottomNavFares.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    /* Open Fares Activity. */
                    startActivity(
                            new Intent(
                                    getApplicationContext(),
                                    FaresActivity.class
                            )
                    );

                    Analytics.selectContent(
                            getApplicationContext(),
                            "button_tapped",
                            "fares_tapped"
                    );
                }
            });
        }

        /*
         * Bottom navigation bar - Alerts.
         */
        RelativeLayout relativeLayoutBottomNavAlerts =
                (RelativeLayout) findViewById(R.id.relativelayout_bottomnav_alerts);
        if (relativeLayoutBottomNavAlerts != null) {
            relativeLayoutBottomNavAlerts.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(
                            new Intent(
                                    getApplicationContext(),
                                    NewsActivity.class
                            ).putExtra(Constant.NEWS_TYPE, Constant.NEWS_TYPE_TRAVEL_UPDATES)
                    );

                    Analytics.selectContent(
                            getApplicationContext(),
                            "button_tapped",
                            "alerts_tapped"
                    );
                }
            });
        }

        imageViewBottomNavAlerts = (ImageView) findViewById(R.id.imageview_bottomnav_alerts);
        textViewBottomNavAlerts = (TextView) findViewById(R.id.textview_bottomnav_alerts);

        showWhatsNewDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();

        adjustBottomNavByScreen();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        /* If the Intent has changed, update the Activity's Intent. */
        setIntent(intent);
    }

    private void adjustBottomNavByScreen() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        final int DENSITY_DPI = displayMetrics.densityDpi;

        Log.i(LOG_TAG, "Screen density is " + DENSITY_DPI + " DPI.");

        textViewBottomNavMap =
                (TextView) findViewById(R.id.textview_bottomnav_map);
        textViewBottomNavFavourites =
                (TextView) findViewById(R.id.textview_bottomnav_favourites);
        textViewBottomNavFares =
                (TextView) findViewById(R.id.textview_bottomnav_fares);
        textViewBottomNavAlerts =
                (TextView) findViewById(R.id.textview_bottomnav_alerts);

        /*
         * If the screen density is particularly low, or if the number of lines in the bottom
         * navigation Favourites button (longest string) goes beyond 1, shorten the strings to
         * something more compact.
         */
        textViewBottomNavFavourites.post(new Runnable() {
            @Override
            public void run() {
                if (DENSITY_DPI <= 320 || textViewBottomNavFavourites.getLineCount() >= 2) {
                    Log.i(LOG_TAG, "Shortening bottom navigation TextViews.");

                    textViewBottomNavMap.setText(getString(R.string.bottomnav_map_short));
                    textViewBottomNavFavourites.setText(
                            getString(R.string.bottomnav_favourites_short)
                    );
                }
            }
        });
    }

    /**
     * Set the colour of the tab indicator to either red or green.
     * @param tabLayout TabLayout to manipulate.
     */
    private void setTabIndicatorColor(TabLayout tabLayout) {
        if (tabLayout.getSelectedTabPosition() == 0) {
            tabLayout.setSelectedTabIndicatorColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.tab_red_line)
            );
        } else {
            tabLayout.setSelectedTabIndicatorColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.tab_green_line)
            );
        }
    }

    /**
     * Show What's New dialog to user if they have recently updated the app.
     */
    private void showWhatsNewDialog() {
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

    public static ImageView getImageViewBottomNavAlerts() {
        return imageViewBottomNavAlerts;
    }

    public static TextView getTextViewBottomNavAlerts() {
        return textViewBottomNavAlerts;
    }
}
