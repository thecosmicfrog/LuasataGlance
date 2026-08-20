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
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.model.Tram

/**
 * Tests that [StopForecastWidget.fillTramsContainer] puts exactly the budgeted number of rows in the container.
 *
 * The rows carry a layout_weight, so LinearLayout shares the container out between however many are present. One row short and
 * every row stretches, one row over and the last is clipped. A two-tram forecast in a container sized for six once tripled the
 * row height, which is what addFillerRows() is for.
 */
@RunWith(RobolectricTestRunner::class)
class StopForecastWidgetRowsTest {

    private lateinit var context: Context
    private lateinit var widget: StopForecastWidget

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        widget = StopForecastWidget()
    }

    @Test
    fun `a full forecast fills the container to the budget`() {
        val container = fill(budget = 6, inbound = trams(3), outbound = trams(3))

        assertThat(tramRowCount(container)).isEqualTo(6)
    }

    @Test
    fun `a short forecast is padded out to the budget`() {
        /* Without the filler, two rows would share a container sized for six and come out three times their height. */
        val container = fill(budget = 6, inbound = trams(1), outbound = trams(1))

        assertThat(tramRowCount(container)).isEqualTo(6)
    }

    @Test
    fun `an overlong forecast is trimmed to the budget`() {
        val container = fill(budget = 6, inbound = trams(20), outbound = trams(20))

        assertThat(tramRowCount(container)).isEqualTo(6)
    }

    @Test
    fun `an empty direction still fills the container to the budget`() {
        val container = fill(budget = 6, inbound = trams(3), outbound = emptyList())

        assertThat(tramRowCount(container)).isEqualTo(6)
    }

    @Test
    fun `two empty directions still fill the container to the budget`() {
        val container = fill(budget = 6, inbound = emptyList(), outbound = emptyList())

        assertThat(tramRowCount(container)).isEqualTo(6)
    }

    @Test
    fun `every budget and forecast shape lands on the budget exactly`() {
        /* The combination that broke before was one direction empty while the other had a full forecast. */
        for (budget in listOf(4, 6, 8, 10, 12)) {
            for (inboundSize in 0..8) {
                for (outboundSize in 0..8) {
                    val container = fill(budget, trams(inboundSize), trams(outboundSize))

                    assertThat(tramRowCount(container)).isEqualTo(budget)
                }
            }
        }
    }

    @Test
    fun `the outbound label sits between the two directions`() {
        val container = fill(budget = 6, inbound = trams(3), outbound = trams(3))

        assertThat(labelPositions(container)).containsExactly(3)
    }

    @Test
    fun `the label still separates the directions when one is empty`() {
        val container = fill(budget = 6, inbound = emptyList(), outbound = trams(3))

        assertThat(labelPositions(container)).containsExactly(1)
    }

    @Test
    fun `there is one label and not one per direction`() {
        /* The inbound label is static in the widget layout, so only the outbound one is added here. */
        val container = fill(budget = 8, inbound = trams(4), outbound = trams(4))

        assertThat(labelPositions(container)).hasSize(1)
    }

    @Test
    fun `an empty direction says so once rather than repeating itself`() {
        val container = fill(budget = 8, inbound = trams(4), outbound = emptyList())
        val noTrams = context.getText(R.string.no_trams_forecast_short).toString()

        assertThat(destinations(container).filter { it == noTrams }).hasSize(1)
    }

    @Test
    fun `the trams shown are the earliest ones`() {
        val container = fill(budget = 4, inbound = trams(5), outbound = trams(5))

        /* take(), not a slice from the end. The tram after next is no use once the budget is full. */
        assertThat(destinations(container).take(2)).containsExactly("Stop 0", "Stop 1").inOrder()
    }

    /**
     * Runs a forecast through the widget and hands back the container it filled.
     *
     * @param budget   Row budget across both directions.
     * @param inbound  Inbound trams.
     * @param outbound Outbound trams.
     */
    private fun fill(budget: Int, inbound: List<Tram>, outbound: List<Tram>): ViewGroup {
        val views = RemoteViews(context.packageName, R.layout.stop_forecast_widget)

        widget.fillTramsContainer(context, views, budget, inbound, outbound, "Southbound")

        return views.apply(context, FrameLayout(context)).findViewById(R.id.trams_container)
    }

    /**
     * Counts the tram rows, which is every child except the direction label.
     */
    private fun tramRowCount(container: ViewGroup) = container.childCount - labelPositions(container).size

    /**
     * Finds the direction labels by their own text view, which no tram row has.
     */
    private fun labelPositions(container: ViewGroup) =
        (0 until container.childCount).filter { index ->
            container.getChildAt(index).findViewById<TextView>(R.id.textview_direction) != null
        }

    /**
     * Reads the destination text of every tram row, filler rows included.
     */
    private fun destinations(container: ViewGroup) =
        (0 until container.childCount).mapNotNull { index ->
            container.getChildAt(index).findViewById<TextView>(R.id.tram_destination)?.text?.toString()
        }

    private fun trams(count: Int) = (0 until count).map { Tram("Stop $it", "Inbound", it.toString()) }
}