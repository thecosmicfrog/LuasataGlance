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
package org.thecosmicfrog.luasataglance.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
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

        val notificationScheduled = scheduleNotification(
            context,
            notifyStopNameExpected,
            notifyTimeUserRequestedMins,
            notifyDelayMillis
        )

        if (notificationScheduled) {
            /* Inform user the notification has been scheduled successfully. */
            Toast.makeText(
                context,
                context.getString(R.string.notify_successful),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Schedule notification for tram.
     * @param context Context.
     * @param notifyStopName Name of stop to notify for.
     * @param notifyTimeUserRequestedMins Minutes before tram arrival the user has requested to be notified at.
     * @param notifyDelayMillis Milliseconds to wait before firing off notification.
     */
    private fun scheduleNotification(context: Context, notifyStopName: String, notifyTimeUserRequestedMins: Int,
                                     notifyDelayMillis: Int): Boolean {
        val requestCodeScheduleNotification = 1
        val requestCodeShowAlarm = 2

        /*
         * Name NotifyTimesAlarmReceiver on the Intent. Without a component named on it, this goes out as a broadcast that only a
         * receiver set up while the app is running could catch, and by the time the alarm fires the app is gone, so the reminder
         * is lost with nothing logged anywhere. FLAG_UPDATE_CURRENT is what keeps the stop name and time current when a new
         * reminder replaces an older one, since two of these count as the same PendingIntent no matter what is in the extras.
         */
        val intentNotify = Intent(context, NotifyTimesAlarmReceiver::class.java).apply {
            setPackage(context.packageName)
            putExtra(Constant.NOTIFY_STOP_NAME, notifyStopName)
            putExtra(Constant.NOTIFY_TIME, notifyTimeUserRequestedMins)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeScheduleNotification,
            intentNotify,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        /* Opened if the user taps the alarm Android shows in the quick settings shade while a reminder is pending. */
        val pendingIntentShowAlarm = PendingIntent.getActivity(
            context,
            requestCodeShowAlarm,
            Intent(context, MainActivity::class.java).apply {
                setPackage(context.packageName)
                action = NotifyTimesReceiver::class.java.name
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(Constant.NOTIFY_STOP_NAME, notifyStopName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        /* Wake up the device. */
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            /*
             * This is the only kind of alarm Android will not delay. Everything else gets held back once it decides the app is
             * used too rarely to be worth waking for (e.g., a user who rarely sets reminders).
             */
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(
                    System.currentTimeMillis() + notifyDelayMillis,
                    pendingIntentShowAlarm
                ),
                pendingIntent
            )

            return true
        } catch (e: SecurityException) {
            Log.w(logTag, "Failed to schedule exact alarm.")
            Toast.makeText(
                context,
                context.getString(R.string.notify_unsuccessful_exact_alarm),
                Toast.LENGTH_LONG
            ).show()
        }

        return false
    }
}
