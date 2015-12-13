/**
 * @author Aaron Hastings
 *
 * Copyright 2015 Aaron Hastings
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

package org.thecosmicfrog.luasataglance.util;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.view.View;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.activity.NewsActivity;
import org.thecosmicfrog.luasataglance.activity.SettingsActivity;

public final class Settings {

    public static void getSettings(Context context, FragmentActivity fragmentActivity, MenuItem item) {
        final String NEWS_TYPE = "newsType";
        final String NEWS_TYPE_LUAS_NEWS = "luasNews";
        final String NEWS_TYPE_TRAVEL_UPDATES = "travelUpdates";

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_news) {
            context.startActivity(
                    new Intent(
                            context,
                            NewsActivity.class
                    ).putExtra(NEWS_TYPE, NEWS_TYPE_LUAS_NEWS)
            );
        }

        if (id == R.id.action_news_alert) {
            context.startActivity(
                    new Intent(
                            context,
                            NewsActivity.class
                    ).putExtra(NEWS_TYPE, NEWS_TYPE_TRAVEL_UPDATES)
            );
        }

        if (id == R.id.action_settings) {
            context.startActivity(
                    new Intent(
                            context,
                            SettingsActivity.class
                    )
            );
        }

        if (id == R.id.action_about) {
            View dialogAbout =
                    fragmentActivity.getLayoutInflater().inflate(R.layout.dialog_about, null);

            new AlertDialog.Builder(context)
                    .setView(dialogAbout)
                    .show();
        }
    }
}
