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
package org.thecosmicfrog.luasataglance.model

import org.thecosmicfrog.luasataglance.util.Constant
import java.util.*

class StopIdLineMap : HashMap<String?, String?>() {
    init {
        /* Red Line */
        put("TPT", Constant.RED_LINE)
        put("SDK", Constant.RED_LINE)
        put("MYS", Constant.RED_LINE)
        put("GDK", Constant.RED_LINE)
        put("CON", Constant.RED_LINE)
        put("BUS", Constant.RED_LINE)
        put("ABB", Constant.RED_LINE)
        put("JER", Constant.RED_LINE)
        put("FOU", Constant.RED_LINE)
        put("SMI", Constant.RED_LINE)
        put("MUS", Constant.RED_LINE)
        put("HEU", Constant.RED_LINE)
        put("JAM", Constant.RED_LINE)
        put("FAT", Constant.RED_LINE)
        put("RIA", Constant.RED_LINE)
        put("SUI", Constant.RED_LINE)
        put("GOL", Constant.RED_LINE)
        put("DRI", Constant.RED_LINE)
        put("BLA", Constant.RED_LINE)
        put("BLU", Constant.RED_LINE)
        put("KYL", Constant.RED_LINE)
        put("RED", Constant.RED_LINE)
        put("KIN", Constant.RED_LINE)
        put("BEL", Constant.RED_LINE)
        put("COO", Constant.RED_LINE)
        put("HOS", Constant.RED_LINE)
        put("TAL", Constant.RED_LINE)
        put("FET", Constant.RED_LINE)
        put("CVN", Constant.RED_LINE)
        put("CIT", Constant.RED_LINE)
        put("FOR", Constant.RED_LINE)
        put("SAG", Constant.RED_LINE)

        /* Green Line */
        put("BRO", Constant.GREEN_LINE)
        put("CAB", Constant.GREEN_LINE)
        put("PHI", Constant.GREEN_LINE)
        put("GRA", Constant.GREEN_LINE)
        put("BRD", Constant.GREEN_LINE)
        put("DOM", Constant.GREEN_LINE)
        put("PAR", Constant.GREEN_LINE)
        put("OUP", Constant.GREEN_LINE)
        put("OGP", Constant.GREEN_LINE)
        put("MAR", Constant.GREEN_LINE)
        put("WES", Constant.GREEN_LINE)
        put("TRY", Constant.GREEN_LINE)
        put("DAW", Constant.GREEN_LINE)
        put("STS", Constant.GREEN_LINE)
        put("HAR", Constant.GREEN_LINE)
        put("CHA", Constant.GREEN_LINE)
        put("RAN", Constant.GREEN_LINE)
        put("BEE", Constant.GREEN_LINE)
        put("COW", Constant.GREEN_LINE)
        put("MIL", Constant.GREEN_LINE)
        put("WIN", Constant.GREEN_LINE)
        put("DUN", Constant.GREEN_LINE)
        put("BAL", Constant.GREEN_LINE)
        put("KIL", Constant.GREEN_LINE)
        put("STI", Constant.GREEN_LINE)
        put("SAN", Constant.GREEN_LINE)
        put("CPK", Constant.GREEN_LINE)
        put("GLE", Constant.GREEN_LINE)
        put("GAL", Constant.GREEN_LINE)
        put("LEO", Constant.GREEN_LINE)
        put("BAW", Constant.GREEN_LINE)
        put("CCK", Constant.GREEN_LINE)
        put("LAU", Constant.GREEN_LINE)
        put("CHE", Constant.GREEN_LINE)
        put("BRI", Constant.GREEN_LINE)
    }
}

