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
 * along with Luas at a Glance.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package org.thecosmicfrog.luasataglance.service

import android.util.Log
import androidx.preference.PreferenceManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.thecosmicfrog.luasataglance.util.AppUtil
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.NotificationUtil

class MessagingService : FirebaseMessagingService() {

    private val logTag = MessagingService::class.java.simpleName

    override fun onNewToken(s: String) {
        super.onNewToken(s)

        /* If we're in an emulator, just log the token as-is for easy debugging. */
        val newTokenToLog: String = if (AppUtil.isEmulator()) {
            s
        } else {
            s.replaceFirst("(.{10}).+(.{10})".toRegex(), "$1...$2")
        }

        Log.d(logTag, "New token: $newTokenToLog")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val userHasAllowedNotifications = PreferenceManager.getDefaultSharedPreferences(
                applicationContext
        ).getBoolean(Constant.NOTIFICATIONS, true)

        /* Check if message contains a data payload. */
        if (userHasAllowedNotifications) {
            if (remoteMessage.data.isNotEmpty()) {
                Log.i(logTag, "Message data payload: " + remoteMessage.data)
            }

            /* Check if message contains a notification payload. */
            if (remoteMessage.notification != null) {
                Log.i(
                    logTag,
                    "Message notification body: " + remoteMessage.notification!!.body
                )

                NotificationUtil.showNotification(applicationContext, remoteMessage)
            }
        } else {
            Log.i(logTag, "User has disallowed notifications. Not displaying.")
        }
    }
}

