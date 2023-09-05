/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2023 Aaron Hastings
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
package org.thecosmicfrog.luasataglance.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.core.app.NotificationCompat
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.activity.MainActivity
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.Preferences

class NotifyTimesReceiver : BroadcastReceiver() {
    private val logTag = NotifyTimesReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent) {
        val notifyTimeUserRequestedMins = intent.getIntExtra(Constant.NOTIFY_TIME, 5)
        val notifyTimeSafetyNetMillis = 30000
        val notifyStopNameExpected = Preferences.notifyStopName(context)
        val notifyStopTimeExpected = Preferences.notifyStopTimeExpected(context)

        /*
         * Define when a user should be notified that their tram is on its way. To do this, we
         * simply take the number of minutes the tram is expected in and subtract the due time
         * the user has asked to be notified at.
         *
         * 1 minute can be the difference between missing and catching a tram. Always insert an
         * artificial 30 second "safety net".
         * Example: If the user has set a notification that should fire after 5 minutes, the
         *          notification will actually fire after 4.5 minutes.
         */
        val notifyDelayMillis = (
                (notifyStopTimeExpected - notifyTimeUserRequestedMins)
                * 60000
                - notifyTimeSafetyNetMillis
        )

        /* If the notification time makes no sense, inform the user and don't proceed. */
        if (notifyDelayMillis < 0) {
            Toast.makeText(
                context,
                context.getString(R.string.notify_invalid_time),
                Toast.LENGTH_LONG
            ).show()

            return
        }

        /* Inform user the notification has been scheduled successfully. */
        Toast.makeText(
            context,
            context.getString(R.string.notify_successful),
            Toast.LENGTH_SHORT
        ).show()

        scheduleNotification(
            context,
            notifyStopNameExpected,
            notifyTimeUserRequestedMins,
            notifyDelayMillis
        )
    }

    /**
     * Schedule notification for tram.
     * @param context Context.
     * @param notifyStopName Name of stop to notify for.
     * @param notifyTimeUserRequestedMins Minutes before tram arrival the user has requested to
     * be notified at.
     * @param notifyDelayMillis Milliseconds to wait before firing off notification.
     */
    private fun scheduleNotification(context: Context, notifyStopName: String, notifyTimeUserRequestedMins: Int,
        notifyDelayMillis: Int) {
        val requestCodeOpenMainActivity = 0
        val requestCodeScheduleNotification = 1
        val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                /*
                 * Create a StringBuilder in order to format the notification message correctly.
                 * Start by adding a message telling the user their tram is expected in N.
                 */
                val stringBuilderContentText = StringBuilder()
                stringBuilderContentText.append(
                    context.getString(R.string.notification_tram_expected)
                ).append(
                    notifyTimeUserRequestedMins.toString()
                )

                /* Append either "minutes" or "minute" depending on the time chosen. */
                if (notifyTimeUserRequestedMins > 1) stringBuilderContentText.append(
                    context.getString(R.string.notification_minutes)
                ) else stringBuilderContentText.append(
                    context.getString(R.string.notification_minute)
                )

                /* Prepare an Intent/PendingIntent to open the MainActivity with the stop-to-notify-for as a String extra. */
                val intentOpenMainActivity = Intent(context, MainActivity::class.java)
                intentOpenMainActivity.setPackage(context.packageName)
                intentOpenMainActivity.action = NotifyTimesReceiver::class.java.name
                intentOpenMainActivity.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                intentOpenMainActivity.putExtra(Constant.NOTIFY_STOP_NAME, notifyStopName)

                val pendingIntentOpenMainActivity = PendingIntent.getActivity(
                    context,
                    requestCodeOpenMainActivity,
                    intentOpenMainActivity,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                /* Create a NotificationManager and NotificationChannel. */
                val notificationManager = context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

                val notificationChannel = NotificationChannel(
                    "notifyTimes",
                    "Notify Times",
                    NotificationManager.IMPORTANCE_HIGH
                )

                /* Configure notification channel. */
                notificationChannel.description = "Notify Times"
                notificationChannel.enableLights(true)
                notificationChannel.lightColor = context.getColor(R.color.luas_purple)
                notificationChannel.vibrationPattern = longArrayOf(100, 1000, 1000, 1000, 1000)
                notificationChannel.enableVibration(true)
                notificationChannel.importance = NotificationManager.IMPORTANCE_HIGH
                notificationManager.createNotificationChannel(notificationChannel)

                /*
                 * Create the NotificationBuilder, setting an appropriate title and the message
                 * built in the StringBuilder. The default notification sound should be played
                 * and the device should vibrate twice for 1 second with a 1 second delay
                 * between them. Setting MAX priority due to the time-sensitive nature of trams.
                 */
                val notificationBuilder = NotificationCompat.Builder(context, "notifyTimes")
                    .setContentIntent(pendingIntentOpenMainActivity)
                    .setContentTitle(context.getString(R.string.notification_title))
                    .setContentText(stringBuilderContentText.toString())
                    .setSmallIcon(R.drawable.laag_logo_notification)
                    .setVibrate(longArrayOf(100, 1000, 1000, 1000, 1000))
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setAutoCancel(true)

                /* Display notification. */
                notificationManager.notify(1, notificationBuilder.build())
            }
        }

        /* Neat Kotlin trick to save using a full if/else for SDK version check. */
        val receiverExported: Int? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Context.RECEIVER_EXPORTED else null
        context.applicationContext.registerReceiver(
            broadcastReceiver,
            IntentFilter("org.thecosmicfrog.luasataglance"),
            receiverExported ?: 0
        )

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeScheduleNotification,
            Intent(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        /* Wake up the device. */
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + notifyDelayMillis,
            pendingIntent
        )
    }
}
