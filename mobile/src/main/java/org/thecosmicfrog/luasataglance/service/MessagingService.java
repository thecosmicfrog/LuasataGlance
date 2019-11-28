/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2019 Aaron Hastings
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

package org.thecosmicfrog.luasataglance.service;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.thecosmicfrog.luasataglance.util.AppUtil;
import org.thecosmicfrog.luasataglance.util.Constant;
import org.thecosmicfrog.luasataglance.util.NotificationUtil;

public class MessagingService extends FirebaseMessagingService {

    private final String LOG_TAG = MessagingService.class.getSimpleName();

    public MessagingService() {
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);

        String newTokenToLog;

        /* If we're in an emulator, just log the token as-is for easy debugging. */
        if (AppUtil.isEmulator()) {
            newTokenToLog = s;
        } else {
            newTokenToLog =
                    s.replaceFirst("(.{10}).+(.{10})", "$1...$2");
        }

        Log.d(LOG_TAG, "New token: " + newTokenToLog);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        boolean userHasAllowedNotifications =
                PreferenceManager.getDefaultSharedPreferences(
                        getApplicationContext()
                ).getBoolean(Constant.NOTIFICATIONS, true);

        if (userHasAllowedNotifications) {
            /* Check if message contains a data payload. */
            if (remoteMessage.getData().size() > 0) {
                Log.i(LOG_TAG, "Message data payload: " + remoteMessage.getData());
            }

            /* Check if message contains a notification payload. */
            if (remoteMessage.getNotification() != null) {
                Log.i(
                        LOG_TAG,
                        "Message notification body: " + remoteMessage.getNotification().getBody()
                );

                NotificationUtil.showNotification(getApplicationContext(), remoteMessage);
            }
        } else {
            Log.i(LOG_TAG, "User has disallowed notifications. Not displaying.");
        }
    }
}
