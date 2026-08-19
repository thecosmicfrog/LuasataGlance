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
 * Tests that the several places a stop has to be listed agree with one another.
 *
 * A stop appears in the English string arrays, the Irish string arrays, both halves of [StopNameIdMap], [EnglishGaeilgeMap], and
 * [StopIdLineMap].
 */
@RunWith(RobolectricTestRunner::class)
class StopMapConsistencyTest {

    private lateinit var context: Context

    /* The stop lists lead with a "no stop selected" sentinel, which is not a stop and is in none of the maps. */
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

    @Test
    fun `the combined stop list is exactly the two line lists`() {
        assertThat(stopsAll).containsExactlyElementsIn(stopsRedLine + stopsGreenLine)
    }

    @Test
    fun `no stop is listed twice`() {
        assertThat(stopsAll).containsNoDuplicates()
    }

    @Test
    fun `every stop the user can pick has a station ID`() {
        val mapStopNameId = StopNameIdMap("en")

        stopsAll.forEach { stopName ->
            assertThat(mapStopNameId[stopName]).isNotNull()
        }
    }

    @Test
    fun `no two stops share a station ID`() {
        val mapStopNameId = StopNameIdMap("en")
        val stopIds = stopsAll.map { mapStopNameId[it] }

        assertThat(stopIds).containsNoDuplicates()
    }

    @Test
    fun `the English map holds nothing beyond the stops on the list`() {
        /* A left-behind entry for a renamed or removed stop is invisible in the app until something looks it up. */
        assertThat(StopNameIdMap("en").keys).containsExactlyElementsIn(stopsAll)
    }

    @Test
    fun `every station ID is attributed to a line`() {
        val mapStopNameId = StopNameIdMap("en")
        val mapStopIdLine = StopIdLineMap()

        stopsAll.forEach { stopName ->
            assertThat(mapStopIdLine).containsKey(mapStopNameId[stopName])
        }
    }

    @Test
    fun `stops are attributed to the line whose list they appear on`() {
        val mapStopNameId = StopNameIdMap("en")
        val mapStopIdLine = StopIdLineMap()

        stopsRedLine.forEach { stopName ->
            assertThat(mapStopIdLine[mapStopNameId[stopName]]).isEqualTo(Constant.RED_LINE)
        }

        stopsGreenLine.forEach { stopName ->
            assertThat(mapStopIdLine[mapStopNameId[stopName]]).isEqualTo(Constant.GREEN_LINE)
        }
    }

    @Test
    fun `every stop has an Irish name`() {
        val mapEnglishGaeilge = EnglishGaeilgeMap()

        stopsAll.forEach { stopName ->
            assertThat(mapEnglishGaeilge).containsKey(stopName)
        }
    }

    @Test
    fun `an Irish name resolves to the same station ID as its English name`() {
        val mapEnglishGaeilge = EnglishGaeilgeMap()
        val mapStopNameIdEnglish = StopNameIdMap("en")
        val mapStopNameIdGaeilge = StopNameIdMap("ga")

        stopsAll.forEach { stopName ->
            val stopNameGaeilge = mapEnglishGaeilge[stopName]

            assertThat(mapStopNameIdGaeilge[stopNameGaeilge]).isEqualTo(mapStopNameIdEnglish[stopName])
        }
    }

    @Test
    fun `the two halves of the stop name map are the same size`() {
        assertThat(StopNameIdMap("ga")).hasSize(StopNameIdMap("en").size)
    }

    @Test
    fun `the map keyed by Irish name is only chosen for an Irish locale`() {
        /* The locale string arrives as a full tag such as "ga_IE", so the check is a prefix rather than an equality. */
        assertThat(StopNameIdMap("ga_IE")["Tamhlacht"]).isEqualTo("TAL")
        assertThat(StopNameIdMap("en_IE")["Tallaght"]).isEqualTo("TAL")
        assertThat(StopNameIdMap(null)["Tallaght"]).isEqualTo("TAL")
    }

    @Test
    @Config(qualifiers = "ga")
    fun `the Irish stop list matches the Irish half of the stop name map`() {
        /*
         * Catches a half-finished translation, where values-ga has a given stop but StopNameIdMap does not, leaving a stop an
         * Irish-language user can select but which resolves to no station ID.
         */
        val mapStopNameIdGaeilge = StopNameIdMap("ga")

        stopsAll.forEach { stopName ->
            assertThat(mapStopNameIdGaeilge[stopName]).isNotNull()
        }
    }

    @Test
    @Config(qualifiers = "ga")
    fun `the Irish stop list is the same length as the English one`() {
        assertThat(stopsAll).hasSize(StopNameIdMap("en").size)
    }

    @Test
    @Config(qualifiers = "ga")
    fun `the Irish combined stop list is exactly the two Irish line lists`() {
        assertThat(stopsAll).containsExactlyElementsIn(stopsRedLine + stopsGreenLine)
    }
}
