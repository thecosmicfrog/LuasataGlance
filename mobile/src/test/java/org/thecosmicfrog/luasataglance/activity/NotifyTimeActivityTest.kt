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
package org.thecosmicfrog.luasataglance.activity

import android.Manifest
import android.app.Application
import android.widget.Button
import android.widget.Spinner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.receiver.NotifyTimesReceiver
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.Preferences

/**
 * Tests for [NotifyTimeActivity], the dialog that asks how many minutes before a tram the user wants telling.
 *
 * The spinner is built from the tram's due time, so a tram due in 9 minutes offers 2 to 8 and nothing the receiver would then
 * refuse. Both permissions are granted up front, since without them the Activity puts a rationale dialog up instead.
 */
@RunWith(RobolectricTestRunner::class)
class NotifyTimeActivityTest {

    private lateinit var application: Application

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()

        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    /**
     * Opens the dialog the way StopForecastUtil.showNotifyTimeDialog() does, with the tram's due time already saved.
     *
     * @param tramDueInMins Minutes until the tram arrives, as the forecast reported it.
     */
    private fun launchFor(tramDueInMins: Int): NotifyTimeActivity {
        Preferences.saveNotifyStopName(application, "Tallaght")
        Preferences.saveNotifyStopTimeExpected(application, tramDueInMins)

        return Robolectric.buildActivity(NotifyTimeActivity::class.java).setup().get()
    }

    /**
     * The rows the spinner shows, top to bottom.
     */
    private fun spinnerEntries(activity: NotifyTimeActivity): List<String> {
        val spinner = activity.findViewById<Spinner>(R.id.spinner_notifytime)

        return (0 until spinner.adapter.count).map { spinner.adapter.getItem(it).toString() }
    }

    @Test
    fun `a tram due in 9 minutes offers 2 to 8 minutes before arrival`() {
        val activity = launchFor(tramDueInMins = 9)

        assertThat(spinnerEntries(activity)).containsExactly(
            "2 mins before arrival",
            "3 mins before arrival",
            "4 mins before arrival",
            "5 mins before arrival",
            "6 mins before arrival",
            "7 mins before arrival",
            "8 mins before arrival"
        ).inOrder()
    }

    @Test
    fun `a tram due in 3 minutes offers only 2 minutes before arrival`() {
        val activity = launchFor(tramDueInMins = 3)

        assertThat(spinnerEntries(activity)).containsExactly("2 mins before arrival")
    }

    @Test
    @Config(qualifiers = "ga")
    fun `the Irish dialog offers the same times in Irish`() {
        val activity = launchFor(tramDueInMins = 4)

        assertThat(spinnerEntries(activity)).containsExactly("2 nóim roimh theacht", "3 nóim roimh theacht").inOrder()
    }

    @Test
    fun `scheduling sends the selected minutes to NotifyTimesReceiver`() {
        val activity = launchFor(tramDueInMins = 9)

        /* The last row, 8 minutes before arrival. */
        activity.findViewById<Spinner>(R.id.spinner_notifytime).setSelection(6)
        activity.findViewById<Button>(R.id.button_notifytime).performClick()

        val intent = shadowOf(application).broadcastIntents.last()

        assertThat(intent.component?.className).isEqualTo(NotifyTimesReceiver::class.java.name)
        assertThat(intent.getIntExtra(Constant.NOTIFY_TIME, -1)).isEqualTo(8)
        assertThat(intent.getStringExtra(Constant.NOTIFY_STOP_NAME)).isEqualTo("Tallaght")
        assertThat(activity.isFinishing).isTrue()
    }

    @Test
    fun `a tram too close for any reminder closes the dialog`() {
        /* StopForecastUtil refuses these before starting the Activity, so this only happens on a stale preference. */
        val activity = launchFor(tramDueInMins = 2)

        assertThat(activity.isFinishing).isTrue()
        assertThat(shadowOf(application).broadcastIntents.none { it.component?.className == NotifyTimesReceiver::class.java.name })
            .isTrue()
    }
}
