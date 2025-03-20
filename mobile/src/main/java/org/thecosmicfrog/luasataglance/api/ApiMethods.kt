/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2025 Aaron Hastings
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

import retrofit.Callback
import retrofit.http.GET
import retrofit.http.Query

interface ApiMethods {

    @GET("/luas-api.php")
    fun getFares(
        @Query("action") action: String?,
        @Query("from") from: String?,
        @Query("to") to: String?,
        @Query("adults") adults: String?,
        @Query("children") children: String?,
        cb: Callback<ApiFares>
    )

    @GET("/luas-api.php")
    fun getStopForecast(
        @Query("action") action: String?,
        @Query("ver") ver: String?,
        @Query("station") station: String?,
        cb: Callback<ApiTimes?>
    )
}

