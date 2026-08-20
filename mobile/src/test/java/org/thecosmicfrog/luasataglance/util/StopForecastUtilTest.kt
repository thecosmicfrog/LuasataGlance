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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.thecosmicfrog.luasataglance.api.ApiTimes
import org.thecosmicfrog.luasataglance.model.StopForecastStatus
import org.thecosmicfrog.luasataglance.model.StopForecastStatusDirection
import org.thecosmicfrog.luasataglance.model.Tram

/**
 * Tests for [StopForecastUtil.createStopForecast], which turns a Gson-parsed [ApiTimes] into a StopForecast.
 *
 * Gson fills ApiTimes from whatever the server sent, so trams, status, and message can all come back null, and direction can be
 * a string nobody expected. These tests pass in those shapes.
 */
@RunWith(RobolectricTestRunner::class)
class StopForecastUtilTest {

    @Test
    fun `trams are sorted into the direction they belong to`() {
        val apiTimes = apiTimes(
            trams = listOf(
                tram("Tallaght", "Inbound", "3"),
                tram("Saggart", "Outbound", "DUE"),
                tram("Connolly", "Inbound", "11")
            )
        )

        val stopForecast = StopForecastUtil.createStopForecast(apiTimes)

        assertThat(stopForecast.inboundTrams.map { it.destination }).containsExactly("Tallaght", "Connolly").inOrder()
        assertThat(stopForecast.outboundTrams.map { it.destination }).containsExactly("Saggart")
    }

    @Test
    fun `an unrecognised direction is dropped rather than guessed at`() {
        val apiTimes = apiTimes(
            trams = listOf(
                tram("Tallaght", "Inbound", "3"),
                tram("Nowhere", "Sideways", "4"),
                tram("Elsewhere", null, "5")
            )
        )

        val stopForecast = StopForecastUtil.createStopForecast(apiTimes)

        assertThat(stopForecast.inboundTrams).hasSize(1)
        assertThat(stopForecast.outboundTrams).isEmpty()
    }

    @Test
    fun `a null entry in the tram list is skipped`() {
        val apiTimes = apiTimes(trams = listOf(tram("Tallaght", "Inbound", "3"), null))

        val stopForecast = StopForecastUtil.createStopForecast(apiTimes)

        assertThat(stopForecast.inboundTrams).hasSize(1)
    }

    @Test
    fun `a null tram list gives an empty forecast rather than throwing`() {
        val stopForecast = StopForecastUtil.createStopForecast(apiTimes(trams = null))

        assertThat(stopForecast.inboundTrams).isEmpty()
        assertThat(stopForecast.outboundTrams).isEmpty()
    }

    @Test
    fun `an empty tram list gives an empty forecast`() {
        val stopForecast = StopForecastUtil.createStopForecast(apiTimes(trams = emptyList()))

        assertThat(stopForecast.inboundTrams).isEmpty()
        assertThat(stopForecast.outboundTrams).isEmpty()
    }

    @Test
    fun `the service message is carried through`() {
        val stopForecast = StopForecastUtil.createStopForecast(apiTimes(message = "Green Line services operating normally"))

        assertThat(stopForecast.message).isEqualTo("Green Line services operating normally")
    }

    @Test
    fun `per-direction status is carried through for both directions`() {
        val apiTimes = apiTimes(
            stopForecastStatus = StopForecastStatus(
                stopForecastStatusDirectionInbound = StopForecastStatusDirection("Inbound fine", true, true),
                stopForecastStatusDirectionOutbound = StopForecastStatusDirection("Outbound disrupted", false, false)
            )
        )

        val stopForecast = StopForecastUtil.createStopForecast(apiTimes)

        assertThat(stopForecast.stopForecastStatusDirectionInbound.message).isEqualTo("Inbound fine")
        assertThat(stopForecast.stopForecastStatusDirectionInbound.forecastsEnabled).isTrue()
        assertThat(stopForecast.stopForecastStatusDirectionInbound.operatingNormally).isTrue()
        assertThat(stopForecast.stopForecastStatusDirectionOutbound.message).isEqualTo("Outbound disrupted")
        assertThat(stopForecast.stopForecastStatusDirectionOutbound.forecastsEnabled).isFalse()
        assertThat(stopForecast.stopForecastStatusDirectionOutbound.operatingNormally).isFalse()
    }

    @Test
    fun `a missing status block leaves the forecast usable`() {
        /* The parser treats the status block as optional, so a forecast arriving without one still has to render. */
        val stopForecast = StopForecastUtil.createStopForecast(apiTimes(stopForecastStatus = null))

        assertThat(stopForecast.stopForecastStatusDirectionInbound.message).isNull()
        assertThat(stopForecast.stopForecastStatusDirectionInbound.operatingNormally).isNull()
        assertThat(stopForecast.stopForecastStatusDirectionOutbound.message).isNull()
    }

    @Test
    fun `direction matching is case sensitive, as the API sends it`() {
        /* Pins current behaviour. If the API ever changes case, this failing is the intended way to find out. */
        val stopForecast = StopForecastUtil.createStopForecast(apiTimes(trams = listOf(tram("Tallaght", "inbound", "3"))))

        assertThat(stopForecast.inboundTrams).isEmpty()
    }

    @Test
    fun `a tram with no due time is still kept, since only the direction decides where it goes`() {
        val stopForecast = StopForecastUtil.createStopForecast(apiTimes(trams = listOf(tram("Tallaght", "Inbound", null))))

        assertThat(stopForecast.inboundTrams).hasSize(1)
        assertThat(stopForecast.inboundTrams.first().dueMinutes).isNull()
    }

    private fun tram(destination: String?, direction: String?, dueMinutes: String?) =
        Tram(destination = destination, direction = direction, dueMinutes = dueMinutes)

    private fun apiTimes(
        createdTime: String? = "2026-08-19T18:00:00",
        message: String? = "Message",
        stopForecastStatus: StopForecastStatus? = StopForecastStatus(),
        trams: List<Tram?>? = emptyList()
    ) = ApiTimes(
        createdTime = createdTime,
        message = message,
        stopForecastStatus = stopForecastStatus,
        trams = trams
    )
}
