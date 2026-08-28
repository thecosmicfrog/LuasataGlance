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
package org.thecosmicfrog.luasataglance.util

import android.app.UiModeManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.thecosmicfrog.luasataglance.R

/**
 * Tests [ThemeUtil], which turns the Theme setting into a mode for UiModeManager. Anything it does not recognise comes out as
 * MODE_NIGHT_AUTO, so a wrong value looks like the Android system option rather than failing.
 */
@RunWith(RobolectricTestRunner::class)
class ThemeUtilTest {

    private lateinit var context: Context

    /* The mode ThemeUtil last passed to UiModeManager.setApplicationNightMode. */
    private val appliedNightMode: Int
        get() = shadowOf(context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager).applicationNightMode

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `Light holds the app in light mode whatever the device is set to`() {
        ThemeUtil.applyTheme(context, context.getString(R.string.pref_value_theme_light))

        assertThat(appliedNightMode).isEqualTo(UiModeManager.MODE_NIGHT_NO)
    }

    @Test
    fun `Dark holds the app in dark mode whatever the device is set to`() {
        ThemeUtil.applyTheme(context, context.getString(R.string.pref_value_theme_dark))

        assertThat(appliedNightMode).isEqualTo(UiModeManager.MODE_NIGHT_YES)
    }

    @Test
    fun `System hands the decision back to the device`() {
        ThemeUtil.applyTheme(context, context.getString(R.string.pref_value_theme_dark))
        ThemeUtil.applyTheme(context, context.getString(R.string.pref_value_theme_system))

        assertThat(appliedNightMode).isEqualTo(UiModeManager.MODE_NIGHT_AUTO)
    }

    @Test
    fun `an unrecognised value follows the device rather than crashing`() {
        ThemeUtil.applyTheme(context, "midnight")

        assertThat(appliedNightMode).isEqualTo(UiModeManager.MODE_NIGHT_AUTO)
    }

    @Test
    fun `a null value follows the device`() {
        ThemeUtil.applyTheme(context, null)

        assertThat(appliedNightMode).isEqualTo(UiModeManager.MODE_NIGHT_AUTO)
    }

    @Test
    fun `the picker offers three values and ThemeUtil recognises all three`() {
        /* A typo in array_theme_values would leave the picker looking right and the choice doing nothing. */
        val values = context.resources.getStringArray(R.array.array_theme_values)

        val nightModes = values.map { value ->
            ThemeUtil.applyTheme(context, value)
            appliedNightMode
        }

        assertThat(values).hasLength(3)
        assertThat(nightModes).containsExactly(
            UiModeManager.MODE_NIGHT_AUTO,
            UiModeManager.MODE_NIGHT_NO,
            UiModeManager.MODE_NIGHT_YES
        ).inOrder()
    }

    @Test
    fun `every value the picker offers has a label beside it`() {
        /* ListPreference pairs the two arrays by index, so a shorter one saves the wrong value. */
        val entries = context.resources.getStringArray(R.array.array_theme_entries)
        val values = context.resources.getStringArray(R.array.array_theme_values)

        assertThat(entries).hasLength(values.size)
        entries.forEach { assertThat(it).isNotEmpty() }
    }

    @Test
    @Config(qualifiers = "ga")
    fun `Dark still means dark mode on an Irish device`() {
        /* Translating pref_value_theme_dark would leave an Irish device saving "Dorcha" and matching nothing. */
        ThemeUtil.applyTheme(context, context.getString(R.string.pref_value_theme_dark))

        assertThat(appliedNightMode).isEqualTo(UiModeManager.MODE_NIGHT_YES)
    }

    @Test
    @Config(qualifiers = "ga")
    fun `the picker is labelled in Irish`() {
        val entries = context.resources.getStringArray(R.array.array_theme_entries)

        assertThat(entries.toList()).containsExactly("Córas", "Geal", "Dorcha").inOrder()
    }
}