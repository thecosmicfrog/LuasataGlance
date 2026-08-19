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
package org.thecosmicfrog.luasataglance.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.thecosmicfrog.luasataglance.R

/**
 * Tests for the widget's row budget arithmetic.
 *
 * calculateMaxTotalTrams() can only estimate, since launchers report more height than the widget gets to draw in. As such, these
 * tests check the rules it has to follow rather than an exact row count: always even, never below four, never more than the budget.
 *
 * Robolectric supplies real resources, so the dimens read here are the ones in values/dimens.xml.
 */
@RunWith(RobolectricTestRunner::class)
class StopForecastWidgetLayoutTest {

    private lateinit var context: Context
    private lateinit var widget: StopForecastWidget

    /* Heights spanning the resizable range, from below the smallest usable widget to a full-column one. */
    private val heightsDp = listOf(0, 40, 80, 96, 100, 120, 150, 200, 250, 300, 350, 400, 500, 600, 800)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        widget = StopForecastWidget()
    }

    @Test
    fun `row count is never below the minimum`() {
        heightsDp.forEach { height ->
            assertThat(widget.calculateMaxTotalTrams(context, height))
                .isAtLeast(StopForecastWidget.MIN_TOTAL_TRAMS)
        }
    }

    @Test
    fun `row count is always even so both directions get the same number of rows`() {
        heightsDp.forEach { height ->
            assertThat(widget.calculateMaxTotalTrams(context, height) % 2).isEqualTo(0)
        }
    }

    @Test
    fun `row count never decreases as the widget gets taller`() {
        val counts = heightsDp.map { widget.calculateMaxTotalTrams(context, it) }

        counts.zipWithNext().forEach { (shorter, taller) ->
            assertThat(taller).isAtLeast(shorter)
        }
    }

    @Test
    fun `a taller widget gets more rows`() {
        /* Every other test in this class would still pass if calculateMaxTotalTrams always returned 4. */
        assertThat(widget.calculateMaxTotalTrams(context, 800))
            .isGreaterThan(widget.calculateMaxTotalTrams(context, 150))
    }

    @Test
    fun `row count matches the chrome and slack the layout reserves`() {
        val resources = context.resources
        val density = resources.displayMetrics.density

        val tramItemHeightDp = (resources.getDimension(R.dimen.widget_tram_item_height) / density).toInt()
        val headerHeightDp = (resources.getDimension(R.dimen.widget_header_height) / density).toInt()
        val labelHeightDp = (resources.getDimension(R.dimen.widget_direction_label_height) / density).toInt()
        val slackDp = (resources.getDimension(R.dimen.widget_reported_height_slack) / density).toInt()
        val chromeHeightDp = headerHeightDp + (labelHeightDp * 2)

        heightsDp.forEach { height ->
            val rows = (height - chromeHeightDp - slackDp) / tramItemHeightDp
            val expected = (rows - (rows % 2)).coerceAtLeast(StopForecastWidget.MIN_TOTAL_TRAMS)

            assertThat(widget.calculateMaxTotalTrams(context, height)).isEqualTo(expected)
        }
    }

    @Test
    fun `split never hands out more rows than the budget`() {
        heightsDp.forEach { height ->
            val budget = widget.calculateMaxTotalTrams(context, height)

            /* Every plausible forecast shape, including more trams than could ever be shown. */
            for (inbound in 0..12) {
                for (outbound in 0..12) {
                    val (inboundRows, outboundRows) = widget.splitTramBudget(budget, inbound, outbound)

                    assertThat(inboundRows + outboundRows).isAtMost(budget)
                }
            }
        }
    }

    @Test
    fun `neither direction is given more rows than the other when both have trams to show`() {
        /* Without an even budget, one direction could show four rows and the other three. */
        val (inboundRows, outboundRows) = widget.splitTramBudget(6, 10, 10)

        assertThat(inboundRows).isEqualTo(outboundRows)
    }

    @Test
    fun `spare capacity is not lent to the other direction`() {
        val (inboundRows, outboundRows) = widget.splitTramBudget(8, 1, 10)

        assertThat(inboundRows).isEqualTo(1)
        assertThat(outboundRows).isEqualTo(4)
    }

    @Test
    fun `a direction is never given more rows than it has trams`() {
        for (budget in listOf(4, 6, 8, 10)) {
            for (inbound in 0..12) {
                for (outbound in 0..12) {
                    val (inboundRows, outboundRows) = widget.splitTramBudget(budget, inbound, outbound)

                    assertThat(inboundRows).isAtMost(inbound)
                    assertThat(outboundRows).isAtMost(outbound)
                }
            }
        }
    }

    @Test
    fun `the minimum budget gives both directions two rows`() {
        val (inboundRows, outboundRows) = widget.splitTramBudget(StopForecastWidget.MIN_TOTAL_TRAMS, 10, 10)

        assertThat(inboundRows).isEqualTo(2)
        assertThat(outboundRows).isEqualTo(2)
    }

    @Test
    fun `rows stay legible at the shortest size the widget can be resized to`() {
        /*
         * MIN_TOTAL_TRAMS and minResizeHeight have to move together. Four rows are asked for even at heights that do not fit
         * them, which works because the rows are weighted and shrink. minResizeHeight stops them shrinking too far.
         */
        val resources = context.resources
        val density = resources.displayMetrics.density

        val headerHeightDp = (resources.getDimension(R.dimen.widget_header_height) / density).toInt()
        val labelHeightDp = (resources.getDimension(R.dimen.widget_direction_label_height) / density).toInt()
        val chromeHeightDp = headerHeightDp + (labelHeightDp * 2)

        val rows = widget.calculateMaxTotalTrams(context, MIN_RESIZE_HEIGHT_DP)
        val heightPerRowDp = (MIN_RESIZE_HEIGHT_DP - chromeHeightDp) / rows

        assertThat(rows).isEqualTo(StopForecastWidget.MIN_TOTAL_TRAMS)
        assertThat(heightPerRowDp).isAtLeast(LEGIBLE_ROW_HEIGHT_DP)
    }

    companion object {
        /* Mirrors android:minResizeHeight in stop_forecast_widget_info.xml, which is a literal rather than a dimen. */
        private const val MIN_RESIZE_HEIGHT_DP = 180

        /* Below this a row's destination and time are no longer comfortably readable. Judged by eye on device. */
        private const val LEGIBLE_ROW_HEIGHT_DP = 20
    }
}
