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
package org.thecosmicfrog.luasataglance.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.activity.MainActivity
import org.thecosmicfrog.luasataglance.util.Constant

/**
 * Posts the notification for a tram reminder scheduled by [NotifyTimesReceiver].
 *
 * Android has almost always killed the app by the time the alarm fires, since a user sets a reminder and then goes elsewhere.
 * A receiver hooked up with registerReceiver only lives as long as the app does, so declaring this one in the manifest and
 * pointing the alarm straight at it is what makes Android start the app again to deliver it. Nothing from when the reminder was
 * scheduled is still in memory by then, so the stop name and the notify time have to arrive on the Intent.
 */
class NotifyTimesAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notifyTimeUserRequestedMins = intent.getIntExtra(Constant.NOTIFY_TIME, 5)
        val notifyStopName = intent.getStringExtra(Constant.NOTIFY_STOP_NAME)

        createNotificationChannel(context)

        /* Prepare an Intent/PendingIntent to open the MainActivity with the stop-to-notify-for as a String extra. */
        val intentOpenMainActivity = Intent(context, MainActivity::class.java).apply {
            setPackage(context.packageName)
            action = NotifyTimesReceiver::class.java.name
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(Constant.NOTIFY_STOP_NAME, notifyStopName)
        }

        val pendingIntentOpenMainActivity = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_MAIN_ACTIVITY,
            intentOpenMainActivity,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        /*
         * Tell the user their tram is expected in N, appending either "minutes" or "minute" depending on the time chosen.
         */
        val stringBuilderContentText = StringBuilder()
        stringBuilderContentText.append(
            context.getString(R.string.notification_tram_expected)
        ).append(
            notifyTimeUserRequestedMins.toString()
        )

        if (notifyTimeUserRequestedMins > 1) stringBuilderContentText.append(
            context.getString(R.string.notification_minutes)
        ) else stringBuilderContentText.append(
            context.getString(R.string.notification_minute)
        )

        /*
         * Setting MAX priority due to the time-sensitive nature of trams. Sound and vibration are set on the channel rather than
         * here, since a notification's own settings are ignored from API 26 onwards.
         */
        val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentIntent(pendingIntentOpenMainActivity)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(stringBuilderContentText.toString())
            .setSmallIcon(R.drawable.laag_logo_notification)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        /* Display notification. */
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    /**
     * Create the NotificationChannel to post the reminder on.
     * @param context Context.
     */
    private fun createNotificationChannel(context: Context) {
        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = NOTIFICATION_CHANNEL_NAME
            enableLights(true)
            lightColor = context.getColor(R.color.luas_purple)
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "notifyTimes"
        private const val NOTIFICATION_CHANNEL_NAME = "Notify Times"
        private const val NOTIFICATION_ID = 1
        private const val REQUEST_CODE_OPEN_MAIN_ACTIVITY = 0

        /* Vibrate twice for 1 second, with a 1 second delay between them. */
        private val VIBRATION_PATTERN = longArrayOf(100, 1000, 1000, 1000, 1000)
    }
}
