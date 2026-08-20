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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.thecosmicfrog.luasataglance.R
import java.util.Locale

/**
 * Tests that [NotifyTimesMap] agrees with the strings the reminder spinner shows.
 *
 * NotifyTimeActivity fills the spinner from R.array.array_notifytime_mins, then looks the selected string up in this map to get
 * the number of minutes. The two lists are written out separately, one in strings.xml and one in Java, so they can drift. When
 * they do the lookup returns null and every reminder quietly falls back to 5 minutes.
 */
@RunWith(RobolectricTestRunner::class)
class NotifyTimesMapTest {

    private lateinit var context: Context

    private val spinnerEntries: List<String>
        get() = context.resources.getStringArray(R.array.array_notifytime_mins).toList()

    /* Built the way NotifyTimeActivity builds it, from the default locale. */
    private val notifyTimes: NotifyTimesMap
        get() = NotifyTimesMap(Locale.getDefault().toString(), "dialog")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `every entry the spinner offers has a number behind it`() {
        spinnerEntries.forEach { entry ->
            assertThat(notifyTimes[entry]).isNotNull()
        }
    }

    @Test
    fun `the map holds nothing the spinner does not offer`() {
        assertThat(notifyTimes.keys).containsExactlyElementsIn(spinnerEntries)
    }

    @Test
    fun `each entry maps to the number it starts with`() {
        spinnerEntries.forEach { entry ->
            assertThat(notifyTimes[entry]).isEqualTo(entry.substringBefore(' ').toInt())
        }
    }

    @Test
    fun `the spinner offers 2 to 15 minutes`() {
        assertThat(notifyTimes.values).containsExactlyElementsIn(2..15)
    }

    @Test
    @Config(qualifiers = "ga")
    fun `every Irish entry the spinner offers has a number behind it`() {
        /* The map switches on the locale string while the spinner switches on values-ga, so each locale needs its own check. */
        spinnerEntries.forEach { entry ->
            assertThat(notifyTimes[entry]).isNotNull()
        }
    }

    @Test
    @Config(qualifiers = "ga")
    fun `the Irish map holds nothing the spinner does not offer`() {
        assertThat(notifyTimes.keys).containsExactlyElementsIn(spinnerEntries)
    }

    @Test
    @Config(qualifiers = "ga")
    fun `the Irish entries map to the same numbers as the English ones`() {
        spinnerEntries.forEach { entry ->
            assertThat(notifyTimes[entry]).isEqualTo(entry.substringBefore(' ').toInt())
        }
    }
}
