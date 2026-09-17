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
package org.thecosmicfrog.luasataglance.model

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
import org.thecosmicfrog.luasataglance.receiver.NotifyTimesReceiver
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.Preferences

/**
 * Tests for [NotifyTimeOptions], which decides what the reminder spinner offers for a given tram.
 */
@RunWith(RobolectricTestRunner::class)
class NotifyTimeOptionsTest {

    private lateinit var context: Context
    private lateinit var shadowAlarmManager: ShadowAlarmManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowAlarmManager = shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
    }

    @Test
    fun `a tram due in 9 minutes offers 2 to 8`() {
        assertThat(NotifyTimeOptions.forTramDueIn(9)).containsExactly(2, 3, 4, 5, 6, 7, 8).inOrder()
    }

    @Test
    fun `a tram due in 3 minutes offers only 2`() {
        assertThat(NotifyTimeOptions.forTramDueIn(3)).containsExactly(2)
    }

    @Test
    fun `a tram due in 2 minutes or less offers nothing`() {
        assertThat(NotifyTimeOptions.forTramDueIn(2)).isEmpty()
        assertThat(NotifyTimeOptions.forTramDueIn(1)).isEmpty()
        assertThat(NotifyTimeOptions.forTramDueIn(0)).isEmpty()
    }

    @Test
    fun `a tram due in 16 minutes offers 2 to 15`() {
        assertThat(NotifyTimeOptions.forTramDueIn(16)).containsExactlyElementsIn(2..15).inOrder()
    }

    @Test
    fun `a distant tram is capped at 15`() {
        assertThat(NotifyTimeOptions.forTramDueIn(25)).containsExactlyElementsIn(2..15).inOrder()
    }

    @Test
    fun `a row with a number of minutes can schedule`() {
        assertThat(NotifyTimeOptions.canSchedule("3")).isTrue()
        assertThat(NotifyTimeOptions.canSchedule("15")).isTrue()
    }

    @Test
    fun `a row showing DUE, 1, or 2 cannot schedule`() {
        assertThat(NotifyTimeOptions.canSchedule("DUE")).isFalse()
        assertThat(NotifyTimeOptions.canSchedule("ANN")).isFalse()
        assertThat(NotifyTimeOptions.canSchedule("1")).isFalse()
        assertThat(NotifyTimeOptions.canSchedule("2")).isFalse()
    }

    @Test
    fun `the no trams row cannot schedule`() {
        /* LineViewModel gives that row an empty due time. */
        assertThat(NotifyTimeOptions.canSchedule("")).isFalse()
        assertThat(NotifyTimeOptions.canSchedule(null)).isFalse()
    }

    @Test
    fun `the receiver schedules every option offered`() {
        /* Each reminder replaces the one before, so a refusal shows up as a stale NOTIFY_TIME rather than a missing alarm. */
        (3..30).forEach { tramDueInMins ->
            NotifyTimeOptions.forTramDueIn(tramDueInMins).forEach { notifyAtMins ->
                scheduleReminder(tramDueInMins, notifyAtMins)

                assertThat(scheduledNotifyTime()).isEqualTo(notifyAtMins)
            }
        }
    }

    @Test
    fun `the receiver refuses the first option not offered`() {
        /* Above 16 the list stops at 15 because of the cap, not because the receiver would refuse 16. */
        (3..16).forEach { tramDueInMins ->
            scheduleReminder(tramDueInMins, NotifyTimeOptions.forTramDueIn(tramDueInMins).last() + 1)

            assertThat(shadowAlarmManager.scheduledAlarms).isEmpty()
        }
    }

    /**
     * Reads NOTIFY_TIME back off the one scheduled alarm.
     */
    private fun scheduledNotifyTime(): Int =
        shadowOf(shadowAlarmManager.scheduledAlarms.single().operation).savedIntent.getIntExtra(Constant.NOTIFY_TIME, -1)

    /**
     * Asks for a reminder the way NotifyTimeActivity does, through shared preferences plus an Intent extra.
     *
     * @param tramDueInMins Minutes until the tram arrives, as the forecast reported it.
     * @param notifyAtMins  Minutes before arrival the user asked to be told.
     */
    private fun scheduleReminder(tramDueInMins: Int, notifyAtMins: Int) {
        Preferences.saveNotifyStopName(context, "Tallaght")
        Preferences.saveNotifyStopTimeExpected(context, tramDueInMins)

        NotifyTimesReceiver().onReceive(
            context,
            Intent(context, NotifyTimesReceiver::class.java).putExtra(Constant.NOTIFY_TIME, notifyAtMins)
        )
    }
}
