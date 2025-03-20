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

package org.thecosmicfrog.luasataglance.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import org.thecosmicfrog.luasataglance.service.WidgetListenerService;

public class OnAppUpdateReceiver extends BroadcastReceiver {

    private final String LOG_TAG = OnAppUpdateReceiver.class.getSimpleName();

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction() != null
                && intent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED)) {
            stopWidgetListenerServer(context);
        }
    }

    /**
     * When the app is updated (e.g. via the Google Play Store), ensure the WidgetListenerService is
     * stopped, to ensure consistency across app updates.
     * @param context Context.
     */
    private void stopWidgetListenerServer(@NonNull Context context) {
        Log.i(LOG_TAG, "App updated. Stopping WidgetListenerService if running.");

        Intent intentWidgetListenerService =
                new Intent(context.getApplicationContext(), WidgetListenerService.class);

        /* Stop the WidgetListenerService. */
        context.stopService(intentWidgetListenerService);
    }
}
