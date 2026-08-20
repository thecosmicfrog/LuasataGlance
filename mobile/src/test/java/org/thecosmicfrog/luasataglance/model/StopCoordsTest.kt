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
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.util.Constant

/**
 * Tests that every stop on a line has a map marker.
 *
 * MapsFragment walks the stop array and the coordinate array together, and throws StopMarkerNotFoundException when a stop has no
 * coordinates. Adding a stop to strings.xml and forgetting StopCoords is the way that happens.
 */
@RunWith(RobolectricTestRunner::class)
class StopCoordsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `every Red Line stop has coordinates`() {
        val stopsRedLine = context.resources.getStringArray(R.array.array_stops_redline)

        assertThat(StopCoords(Constant.RED_LINE).stopCoords).hasLength(stopsRedLine.size)
    }

    @Test
    fun `every Green Line stop has coordinates`() {
        val stopsGreenLine = context.resources.getStringArray(R.array.array_stops_greenline)

        assertThat(StopCoords(Constant.GREEN_LINE).stopCoords).hasLength(stopsGreenLine.size)
    }

    @Test
    fun `an unrecognised line gives no coordinates rather than throwing`() {
        assertThat(StopCoords("purple_line").stopCoords).isEmpty()
    }

    @Test
    fun `every coordinate is a latitude and a longitude`() {
        bothLines().forEach { coords ->
            assertThat(coords).hasLength(2)
        }
    }

    @Test
    fun `every stop sits within greater Dublin`() {
        /* Catches a digit typed wrong or a pair entered the wrong way round, which would put a marker in the sea. */
        bothLines().forEach { coords ->
            assertThat(coords[0]).isAtLeast(SOUTHERNMOST_LATITUDE)
            assertThat(coords[0]).isAtMost(NORTHERNMOST_LATITUDE)
            assertThat(coords[1]).isAtLeast(WESTERNMOST_LONGITUDE)
            assertThat(coords[1]).isAtMost(EASTERNMOST_LONGITUDE)
        }
    }

    @Test
    fun `no two stops share coordinates`() {
        assertThat(bothLines().map { it.toList() }).containsNoDuplicates()
    }

    private fun bothLines() =
        (StopCoords(Constant.RED_LINE).stopCoords + StopCoords(Constant.GREEN_LINE).stopCoords).toList()

    companion object {
        /* Approximate greater Dublin area - just for sanity checking. */
        private const val SOUTHERNMOST_LATITUDE = 53.2
        private const val NORTHERNMOST_LATITUDE = 53.5
        private const val WESTERNMOST_LONGITUDE = -6.5
        private const val EASTERNMOST_LONGITUDE = -6.1
    }
}
