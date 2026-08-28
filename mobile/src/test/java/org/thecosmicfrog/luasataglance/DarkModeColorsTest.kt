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
package org.thecosmicfrog.luasataglance

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests the colour tokens in `values/colors.xml` against their `values-night` overrides.
 */
@RunWith(RobolectricTestRunner::class)
class DarkModeColorsTest {

    private lateinit var light: Context
    private lateinit var dark: Context

    /* Every foreground the app draws, against the surface it is drawn on. */
    private val pairs = listOf(
        R.color.on_surface to R.color.background_card,
        R.color.on_surface to R.color.background_screen,
        R.color.on_surface to R.color.background_popup,
        R.color.on_surface to R.color.background_dialog,
        R.color.on_surface_variant to R.color.background_card,
        R.color.text_on_bars to R.color.background_appbar,
        R.color.text_on_bars to R.color.background_navbar,
        R.color.text_on_accent to R.color.background_accent,
        R.color.text_direction_header to R.color.background_direction_group,
        R.color.text_on_widget_accent to R.color.background_widget_accent,
        R.color.status_text_success to R.color.status_fill_success,
        R.color.status_text_error to R.color.status_fill_error
    )

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()

        light = context.inUiMode(Configuration.UI_MODE_NIGHT_NO)
        dark = context.inUiMode(Configuration.UI_MODE_NIGHT_YES)
    }

    @Test
    fun `the brand palette is the same in both modes`() {
        listOf(
            R.color.luas_purple,
            R.color.luas_purple_statusbar,
            R.color.message_success,
            R.color.message_error
        ).forEach { color ->
            assertWithMessage(light.nameOf(color)).that(dark.getColor(color)).isEqualTo(light.getColor(color))
        }
    }

    @Test
    fun `the two line colours are the only brand colours dark mode changes`() {
        assertThat(dark.getColor(R.color.tab_red_line)).isNotEqualTo(light.getColor(R.color.tab_red_line))
        assertThat(dark.getColor(R.color.tab_green_line)).isNotEqualTo(light.getColor(R.color.tab_green_line))
    }

    @Test
    fun `a line colour is no brighter in dark mode than it is in light`() {
        listOf(R.color.tab_red_line, R.color.tab_green_line).forEach { color ->
            assertWithMessage(light.nameOf(color))
                .that(luminance(dark.getColor(color)))
                .isAtMost(luminance(light.getColor(color)) * 1.1)
        }
    }

    @Test
    fun `the surfaces that were one purple in light mode all differ in dark mode`() {
        listOf(
            R.color.background_appbar,
            R.color.background_navbar,
            R.color.background_direction_group,
            R.color.background_accent,
            R.color.text_direction_header
        ).forEach { color ->
            assertWithMessage(light.nameOf(color)).that(dark.getColor(color)).isNotEqualTo(light.getColor(color))
        }
    }

    @Test
    fun `the widget accent is not overridden, since the widget draws on the launcher's wallpaper`() {
        listOf(R.color.background_widget_accent, R.color.text_on_widget_accent).forEach { color ->
            assertWithMessage(light.nameOf(color)).that(dark.getColor(color)).isEqualTo(light.getColor(color))
        }
    }

    @Test
    fun `the direction panel stays darker than the card it holds`() {
        /* Level with background_card, the tram rows flatten into the panel. */
        assertThat(luminance(dark.getColor(R.color.background_direction_group)))
            .isLessThan(luminance(dark.getColor(R.color.background_card)))
    }

    @Test
    fun `every foreground clears 4 point 5 to 1 on the surface it is drawn on`() {
        listOf(light, dark).forEach { context ->
            pairs.forEach { (foreground, background) ->
                assertWithMessage("${context.nameOf(foreground)} on ${context.nameOf(background)}")
                    .that(contrast(context.getColor(foreground), context.getColor(background)))
                    .isAtLeast(4.5)
            }
        }
    }

    @Test
    fun `the selected navigation icon does not take the bar's text colour`() {
        listOf(light, dark).forEach { context ->
            val checked = ContextCompat.getColorStateList(context, R.color.navbar_icon)!!
                .getColorForState(intArrayOf(android.R.attr.state_checked), 0)

            assertThat(checked).isEqualTo(context.getColor(R.color.text_on_accent))
            assertThat(contrast(checked, context.getColor(R.color.background_navbar_indicator))).isAtLeast(4.5)
        }
    }

    /**
     * A copy of this context that resolves resources in one mode.
     *
     * @param uiModeNight Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_NIGHT_YES.
     */
    private fun Context.inUiMode(uiModeNight: Int): Context {
        val configuration = Configuration(resources.configuration)

        configuration.uiMode = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or uiModeNight

        return createConfigurationContext(configuration)
    }

    private fun Context.nameOf(color: Int): String = resources.getResourceEntryName(color)

    /**
     * WCAG relative luminance.
     *
     * @param color Colour to measure.
     * @return Luminance between 0.0 and 1.0.
     */
    private fun luminance(color: Int): Double {
        val channels = listOf(Color.red(color), Color.green(color), Color.blue(color)).map { channel ->
            val value = channel / 255.0

            if (value <= 0.03928) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
        }

        return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2]
    }

    /**
     * WCAG contrast ratio between two colours.
     *
     * @param foreground Foreground colour.
     * @param background Background colour.
     * @return Ratio between 1.0 and 21.0.
     */
    private fun contrast(foreground: Int, background: Int): Double {
        val first = luminance(foreground)
        val second = luminance(background)

        return (maxOf(first, second) + 0.05) / (minOf(first, second) + 0.05)
    }
}
