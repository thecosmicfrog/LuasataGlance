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
import org.thecosmicfrog.luasataglance.util.Constant

/**
 * Tests [Stops], the one table every stop is declared in.
 */
@RunWith(RobolectricTestRunner::class)
class StopsTest {

    private lateinit var context: Context

    /* The picker's list starts with a "None" entry, which is not a stop. */
    private val stopsAll: List<String>
        get() = context.resources.getStringArray(R.array.array_stops_all).drop(1)

    private val stopsRedLine: List<String>
        get() = context.resources.getStringArray(R.array.array_stops_redline).toList()

    private val stopsGreenLine: List<String>
        get() = context.resources.getStringArray(R.array.array_stops_greenline).toList()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    /* Structure. */
    @Test
    fun `the table holds every stop the picker offers`() {
        assertThat(Stops.all).hasSize(stopsAll.size)
    }

    @Test
    fun `no two stops share a stop ID`() {
        assertThat(Stops.all.map { it.id }).containsNoDuplicates()
    }

    @Test
    fun `every stop is attributed to one of the two lines`() {
        Stops.all.forEach { stop ->
            assertThat(stop.line).isAnyOf(Constant.RED_LINE, Constant.GREEN_LINE)
        }
    }

    @Test
    fun `each line list holds only that line's stops`() {
        Stops.redLine.forEach { assertThat(it.line).isEqualTo(Constant.RED_LINE) }
        Stops.greenLine.forEach { assertThat(it.line).isEqualTo(Constant.GREEN_LINE) }
    }

    @Test
    fun `an unknown stop ID resolves to nothing rather than throwing`() {
        assertThat(Stops.byId("ZZZ")).isNull()
        assertThat(Stops.byId(null)).isNull()
        assertThat(Stops.line("ZZZ")).isNull()
        assertThat(Stops.name(context, "ZZZ")).isNull()
    }

    /* Agreement with the string arrays. */
    @Test
    fun `the Red Line list is in the same order as its array`() {
        /* MapsFragment.drawPolylines walks these by index, using ranges that encode where the line branches. */
        assertThat(Stops.redLine.map { context.getString(it.nameRes) }).containsExactlyElementsIn(stopsRedLine).inOrder()
    }

    @Test
    fun `the Green Line list is in the same order as its array`() {
        assertThat(Stops.greenLine.map { context.getString(it.nameRes) }).containsExactlyElementsIn(stopsGreenLine).inOrder()
    }

    @Test
    fun `the combined array is the two line lists, in order`() {
        assertThat(Stops.all.map { context.getString(it.nameRes) }).containsExactlyElementsIn(stopsAll).inOrder()
    }

    @Test
    @Config(qualifiers = "ga")
    fun `the Irish arrays are in the same order as the table`() {
        assertThat(Stops.redLine.map { context.getString(it.nameRes) }).containsExactlyElementsIn(stopsRedLine).inOrder()
        assertThat(Stops.greenLine.map { context.getString(it.nameRes) }).containsExactlyElementsIn(stopsGreenLine).inOrder()
    }

    /* Names in both languages. */
    @Test
    fun `every stop has an English name`() {
        Stops.all.forEach { stop ->
            assertThat(context.getString(stop.nameRes)).isNotEmpty()
        }
    }

    @Test
    @Config(qualifiers = "ga")
    fun `every stop has an Irish name`() {
        Stops.all.forEach { stop ->
            assertThat(context.getString(stop.nameRes)).isNotEmpty()
        }
    }

    @Test
    fun `no two stops share an English name`() {
        assertThat(Stops.all.map { context.getString(it.nameRes) }).containsNoDuplicates()
    }

    @Test
    @Config(qualifiers = "ga")
    fun `no two stops share an Irish name`() {
        assertThat(Stops.all.map { context.getString(it.nameRes) }).containsNoDuplicates()
    }

    /* Looking a stop up by the name shown to the user. */
    @Test
    fun `an English name resolves to its stop ID`() {
        assertThat(Stops.idForName(context, "Tallaght")).isEqualTo("TAL")
        assertThat(Stops.idForName(context, "Brides Glen")).isEqualTo("BRI")
    }

    @Test
    @Config(qualifiers = "ga")
    fun `an Irish name resolves to the same stop ID as its English name`() {
        assertThat(Stops.idForName(context, "Tamhlacht")).isEqualTo("TAL")
        assertThat(Stops.idForName(context, "Gleann Bhríde")).isEqualTo("BRI")
    }

    @Test
    fun `every displayed name resolves back to the stop it came from`() {
        Stops.all.forEach { stop ->
            assertThat(Stops.idForName(context, context.getString(stop.nameRes))).isEqualTo(stop.id)
        }
    }

    @Test
    @Config(qualifiers = "ga")
    fun `every displayed Irish name resolves back to the stop it came from`() {
        Stops.all.forEach { stop ->
            assertThat(Stops.idForName(context, context.getString(stop.nameRes))).isEqualTo(stop.id)
        }
    }

    @Test
    fun `a name that is not a stop resolves to nothing`() {
        assertThat(Stops.idForName(context, "Not A Stop")).isNull()
        assertThat(Stops.idForName(context, null)).isNull()
    }

    /* Translating what the API sends back. */
    @Test
    @Config(qualifiers = "ga")
    fun `an English name from the API resolves even when the device is Irish`() {
        /* The API answers in English whatever the phone is set to. */
        assertThat(Stops.idForEnglishName(context, "Tallaght")).isEqualTo("TAL")
        assertThat(Stops.idForEnglishName(context, "Brides Glen")).isEqualTo("BRI")
    }

    @Test
    @Config(qualifiers = "ga")
    fun `a destination is translated when the device is Irish`() {
        assertThat(Stops.localiseApiName(context, "Tallaght")).isEqualTo("Tamhlacht")
        assertThat(Stops.localiseApiName(context, "Brides Glen")).isEqualTo("Gleann Bhríde")
    }

    @Test
    fun `a destination is left alone when the device is English`() {
        assertThat(Stops.localiseApiName(context, "Tallaght")).isEqualTo("Tallaght")
    }

    @Test
    @Config(qualifiers = "ga")
    fun `a destination that is not one of our stops falls back to what the API sent`() {
        assertThat(Stops.localiseApiName(context, "Not A Stop")).isEqualTo("Not A Stop")
        assertThat(Stops.localiseApiName(context, null)).isNull()
    }

    @Test
    fun `every English name the API could send resolves to a stop`() {
        Stops.all.forEach { stop ->
            assertThat(Stops.idForEnglishName(context, context.getString(stop.nameRes))).isEqualTo(stop.id)
        }
    }

    /* The "None" entry in the default stop picker. */
    @Test
    fun `the first entry in the picker is the None string itself`() {
        /*
         * The default stop ListPreference stores array_stops_all[0], and LineFragment compares what it stored against
         * R.string.none. If the two differ, LineFragment passes "None" to setTabAndSpinner, which selects the other tab.
         */
        assertThat(context.resources.getStringArray(R.array.array_stops_all)[0]).isEqualTo(context.getString(R.string.none))
    }

    @Test
    @Config(qualifiers = "ga")
    fun `the first entry in the Irish picker is the Irish None string`() {
        assertThat(context.resources.getStringArray(R.array.array_stops_all)[0]).isEqualTo(context.getString(R.string.none))
    }

    @Test
    fun `None is not the name of a stop`() {
        assertThat(Stops.idForName(context, context.getString(R.string.none))).isNull()
    }

    /* Coordinates. */
    @Test
    fun `every stop sits within greater Dublin`() {
        /* Catches a digit typed wrong or a latitude and longitude entered the wrong way round. */
        Stops.all.forEach { stop ->
            assertThat(stop.latitude).isAtLeast(SOUTHERNMOST_LATITUDE)
            assertThat(stop.latitude).isAtMost(NORTHERNMOST_LATITUDE)
            assertThat(stop.longitude).isAtLeast(WESTERNMOST_LONGITUDE)
            assertThat(stop.longitude).isAtMost(EASTERNMOST_LONGITUDE)
        }
    }

    @Test
    fun `no two stops share coordinates`() {
        assertThat(Stops.all.map { it.latitude to it.longitude }).containsNoDuplicates()
    }

    companion object {
        /* Approximate greater Dublin area - just for sanity checking. */
        private const val SOUTHERNMOST_LATITUDE = 53.2
        private const val NORTHERNMOST_LATITUDE = 53.5
        private const val WESTERNMOST_LONGITUDE = -6.5
        private const val EASTERNMOST_LONGITUDE = -6.1
    }
}
