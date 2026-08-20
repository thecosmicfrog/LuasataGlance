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

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowToast
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.Preferences

/**
 * Tests for [NotifyTimesReceiver], which sets the alarm behind a tram reminder. [NotifyTimesAlarmReceiver] posts the
 * notification when it fires.
 */
@RunWith(RobolectricTestRunner::class)
class NotifyTimesReceiverTest {

    private lateinit var context: Context
    private lateinit var shadowAlarmManager: ShadowAlarmManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowAlarmManager = shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
    }

    @Test
    fun `a reminder schedules one alarm`() {
        scheduleReminder(stopName = "Tallaght", tramDueInMins = 9, notifyAtMins = 5)

        assertThat(shadowAlarmManager.scheduledAlarms).hasSize(1)
    }

    @Test
    fun `the alarm is addressed to NotifyTimesAlarmReceiver`() {
        /*
         * Without a component on the Intent the alarm goes out as a plain broadcast that nothing left running can catch, and the
         * reminder is lost with nothing logged.
         */
        scheduleReminder(stopName = "Tallaght", tramDueInMins = 9, notifyAtMins = 5)

        assertThat(scheduledIntent().component?.className).isEqualTo(NotifyTimesAlarmReceiver::class.java.name)
    }

    @Test
    fun `the stop name and notify time travel on the intent`() {
        /* Nothing from the time the reminder was set is still in memory when the alarm fires, so the extras are the only source. */
        scheduleReminder(stopName = "Saggart", tramDueInMins = 12, notifyAtMins = 7)

        val intent = scheduledIntent()

        assertThat(intent.getStringExtra(Constant.NOTIFY_STOP_NAME)).isEqualTo("Saggart")
        assertThat(intent.getIntExtra(Constant.NOTIFY_TIME, -1)).isEqualTo(7)
    }

    @Test
    fun `the alarm is one Android will not hold back`() {
        /* Only setAlarmClock() alarms carry an AlarmClockInfo, and they are the only kind Android delivers on time. */
        scheduleReminder(stopName = "Tallaght", tramDueInMins = 9, notifyAtMins = 5)

        assertThat(shadowAlarmManager.scheduledAlarms.single().alarmClockInfo).isNotNull()
    }

    @Test
    fun `the alarm fires 30 seconds before the requested time`() {
        /* A minute is the difference between catching and missing a tram, so a reminder set for 5 minutes fires at 4.5. */
        val now = System.currentTimeMillis()

        scheduleReminder(stopName = "Tallaght", tramDueInMins = 9, notifyAtMins = 5)

        val expectedDelayMillis = (9 - 5) * 60000L - 30000L
        val actualDelayMillis = shadowAlarmManager.scheduledAlarms.single().triggerAtMs - now

        /* The clock moves on between reading it here and the receiver reading it, so allow for that but nothing larger. */
        assertThat(actualDelayMillis).isAtLeast(expectedDelayMillis)
        assertThat(actualDelayMillis).isAtMost(expectedDelayMillis + 5000L)
    }

    @Test
    fun `a reminder for the moment the tram arrives is still scheduled`() {
        /* Asking to be told 9 minutes before a tram due in 9 leaves 30 seconds once the safety net comes off, which is allowed. */
        scheduleReminder(stopName = "Tallaght", tramDueInMins = 9, notifyAtMins = 8)

        assertThat(shadowAlarmManager.scheduledAlarms).hasSize(1)
    }

    @Test
    fun `nothing is scheduled when the tram arrives before the requested time`() {
        scheduleReminder(stopName = "Tallaght", tramDueInMins = 3, notifyAtMins = 10)

        assertThat(shadowAlarmManager.scheduledAlarms).isEmpty()
    }

    @Test
    fun `the user is told when the tram arrives before the requested time`() {
        scheduleReminder(stopName = "Tallaght", tramDueInMins = 3, notifyAtMins = 10)

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(context.getString(R.string.notify_invalid_time))
    }

    @Test
    fun `the user is told when a reminder is scheduled`() {
        scheduleReminder(stopName = "Tallaght", tramDueInMins = 9, notifyAtMins = 5)

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(context.getString(R.string.notify_successful))
    }

    @Test
    fun `a new reminder replaces the one before it`() {
        /*
         * Both use the same request code, so FLAG_UPDATE_CURRENT is what refreshes the extras. Without it the second reminder
         * would fire carrying the first one's stop.
         */
        scheduleReminder(stopName = "Tallaght", tramDueInMins = 9, notifyAtMins = 5)
        scheduleReminder(stopName = "Saggart", tramDueInMins = 12, notifyAtMins = 7)

        assertThat(shadowAlarmManager.scheduledAlarms).hasSize(1)
        assertThat(scheduledIntent().getStringExtra(Constant.NOTIFY_STOP_NAME)).isEqualTo("Saggart")
    }

    /**
     * Reads back the Intent the alarm will deliver when it fires.
     */
    private fun scheduledIntent(): Intent =
        shadowOf(shadowAlarmManager.scheduledAlarms.single().operation).savedIntent

    /**
     * Asks for a reminder the way NotifyTimeActivity does, through shared preferences plus an Intent extra.
     *
     * @param stopName      Stop the tram is expected at.
     * @param tramDueInMins Minutes until the tram arrives, as the forecast reported it.
     * @param notifyAtMins  Minutes before arrival the user asked to be told.
     */
    private fun scheduleReminder(stopName: String, tramDueInMins: Int, notifyAtMins: Int) {
        Preferences.saveNotifyStopName(context, stopName)
        Preferences.saveNotifyStopTimeExpected(context, tramDueInMins)

        NotifyTimesReceiver().onReceive(
            context,
            Intent(context, NotifyTimesReceiver::class.java).putExtra(Constant.NOTIFY_TIME, notifyAtMins)
        )
    }
}
