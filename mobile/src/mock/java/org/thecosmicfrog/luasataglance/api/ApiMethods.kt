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
package org.thecosmicfrog.luasataglance.api

import org.thecosmicfrog.luasataglance.model.StopForecastStatus
import org.thecosmicfrog.luasataglance.model.StopForecastStatusDirection
import org.thecosmicfrog.luasataglance.model.Stops
import org.thecosmicfrog.luasataglance.model.Tram
import org.thecosmicfrog.luasataglance.util.Constant
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.delay

class MockApiMethods : ApiMethods {

    override suspend fun getStopForecast(
        action: String?,
        ver: String?,
        station: String?
    ): Response<ApiTimes> {
        /* Simulate realistic network latency (between 0.5 and 3 seconds). */
        delay(Random.nextLong(500, 3_000))

        val trams = mutableListOf<Tram>()
        var dueTimeRedLine = Random.nextInt(0, 3)
        var dueTimeGreenLine = Random.nextInt(0, 3)

        val line = Stops.line(station)

        /* Generate 8 synthetic inbound trams. */
        repeat(8) {
            val destination = if (line == Constant.RED_LINE) {
                if (Random.nextBoolean()) "The Point" else "Connolly"
            } else {
                if (Random.nextBoolean()) "Broombridge" else "Parnell"
            }
            trams.add(
                Tram(
                    destination = destination,
                    direction = "Inbound",
                    dueMinutes = if (dueTimeRedLine == 0) "DUE" else dueTimeRedLine.toString(),
                )
            )
            dueTimeRedLine += Random.nextInt(1, 5)
        }

        /* Generate 8 synthetic outbound trams. */
        repeat(8) {
            val destination = if (line == Constant.RED_LINE) {
                if (Random.nextBoolean()) "Tallaght" else "Saggart"
            } else {
                if (Random.nextBoolean()) "Brides Glen" else "Sandyford"
            }
            trams.add(
                Tram(
                    destination = destination,
                    direction = "Outbound",
                    dueMinutes = if (dueTimeGreenLine == 0) "DUE" else dueTimeGreenLine.toString(),
                )
            )
            dueTimeGreenLine += Random.nextInt(1, 5)
        }

        val apiTimes = ApiTimes(
            createdTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date()),
            message = "Mock data for $line",
            /* StopForecastStatus() defaults operatingNormally to false, which LineViewModel draws as a red status card. */
            stopForecastStatus = StopForecastStatus(
                StopForecastStatusDirection("Mock data for $line", true, true),
                StopForecastStatusDirection("Mock data for $line", true, true)
            ),
            trams = trams.sortedBy { it.dueMinutes?.toIntOrNull() ?: 0 }
        )

        return Response.success(apiTimes)
    }
}
