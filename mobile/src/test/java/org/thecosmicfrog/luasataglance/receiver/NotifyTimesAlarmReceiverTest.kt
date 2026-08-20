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

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.activity.MainActivity
import org.thecosmicfrog.luasataglance.util.Constant

/**
 * Tests for [NotifyTimesAlarmReceiver], which posts the notification once the alarm [NotifyTimesReceiver] set has fired.
 *
 * It runs after Android has restarted the app to deliver that alarm, so everything it needs arrives on the Intent. A stop name
 * that failed to travel shows up here as a notification naming the wrong stop rather than as a crash.
 */
@RunWith(RobolectricTestRunner::class)
class NotifyTimesAlarmReceiverTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Test
    fun `the alarm posts one notification`() {
        deliverAlarm(stopName = "Tallaght", notifyAtMins = 5)

        assertThat(shadowOf(notificationManager).allNotifications).hasSize(1)
    }

    @Test
    fun `the notification goes on the reminder channel`() {
        deliverAlarm(stopName = "Tallaght", notifyAtMins = 5)

        assertThat(shadowOf(notificationManager).notificationChannels.map { it.id }).contains(NOTIFICATION_CHANNEL_ID)
        assertThat(postedNotification().channelId).isEqualTo(NOTIFICATION_CHANNEL_ID)
    }

    @Test
    fun `the channel is created before the notification is posted`() {
        /* Posting to a channel that does not exist yet drops the notification silently. */
        deliverAlarm(stopName = "Tallaght", notifyAtMins = 5)

        val channel = shadowOf(notificationManager).notificationChannels.single { it.id == NOTIFICATION_CHANNEL_ID }

        assertThat(channel.importance).isEqualTo(NotificationManager.IMPORTANCE_HIGH)
        assertThat(channel.shouldVibrate()).isTrue()
        assertThat(channel.sound).isNotNull()
    }

    @Test
    fun `more than one minute reads as minutes`() {
        deliverAlarm(stopName = "Tallaght", notifyAtMins = 5)

        val expected = context.getString(R.string.notification_tram_expected) + "5" +
                context.getString(R.string.notification_minutes)

        assertThat(contentText()).isEqualTo(expected)
    }

    @Test
    fun `one minute reads as minute`() {
        deliverAlarm(stopName = "Tallaght", notifyAtMins = 1)

        val expected = context.getString(R.string.notification_tram_expected) + "1" +
                context.getString(R.string.notification_minute)

        assertThat(contentText()).isEqualTo(expected)
    }

    @Test
    fun `the notification carries the app's own title`() {
        deliverAlarm(stopName = "Tallaght", notifyAtMins = 5)

        assertThat(postedNotification().extras.getString(Notification.EXTRA_TITLE))
            .isEqualTo(context.getString(R.string.notification_title))
    }

    @Test
    fun `tapping the notification opens the app on the stop it is about`() {
        deliverAlarm(stopName = "Saggart", notifyAtMins = 5)

        val intent = shadowOf(postedNotification().contentIntent).savedIntent

        assertThat(intent.component?.className).isEqualTo(MainActivity::class.java.name)
        assertThat(intent.getStringExtra(Constant.NOTIFY_STOP_NAME)).isEqualTo("Saggart")
    }

    @Test
    fun `the notification clears itself once tapped`() {
        deliverAlarm(stopName = "Tallaght", notifyAtMins = 5)

        assertThat(postedNotification().flags and Notification.FLAG_AUTO_CANCEL).isNotEqualTo(0)
    }

    @Test
    fun `an alarm with no stop name still posts`() {
        /* A reminder without its stop is less useful, but a tram the user is about to miss is worth saying something about. */
        NotifyTimesAlarmReceiver().onReceive(
            context,
            Intent(context, NotifyTimesAlarmReceiver::class.java).putExtra(Constant.NOTIFY_TIME, 5)
        )

        assertThat(shadowOf(notificationManager).allNotifications).hasSize(1)
    }

    private fun postedNotification() = shadowOf(notificationManager).allNotifications.single()

    private fun contentText() = postedNotification().extras.getString(Notification.EXTRA_TEXT)

    /**
     * Delivers the alarm the way AlarmManager does, with the stop name and time on the Intent.
     *
     * @param stopName     Stop the tram is expected at.
     * @param notifyAtMins Minutes before arrival the user asked to be told.
     */
    private fun deliverAlarm(stopName: String, notifyAtMins: Int) {
        NotifyTimesAlarmReceiver().onReceive(
            context,
            Intent(context, NotifyTimesAlarmReceiver::class.java)
                .putExtra(Constant.NOTIFY_STOP_NAME, stopName)
                .putExtra(Constant.NOTIFY_TIME, notifyAtMins)
        )
    }

    companion object {
        /* Mirrors the private constant in NotifyTimesAlarmReceiver. Changing one without the other drops every reminder. */
        private const val NOTIFICATION_CHANNEL_ID = "notifyTimes"
    }
}