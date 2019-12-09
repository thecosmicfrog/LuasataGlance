/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2019 Aaron Hastings
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

import java.io.Serializable

class Tram(val destination: String, val direction: String, val dueMinutes: String) : Serializable {

    override fun toString(): String {
        if (dueMinutes != "DUE") {
            if (dueMinutes.toInt() > 1) {
                return String.format("%s\t%s\t%s", destination, direction, "$dueMinutes mins")
            }
        }

        return String.format("%s\t%s\t%s", destination, direction, "$dueMinutes min")
    }

    companion object {
        private const val serialVersionUID = 0L
    }
}

