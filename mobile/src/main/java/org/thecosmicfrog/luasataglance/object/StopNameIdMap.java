/**
 * @author Aaron Hastings
 *
 * Copyright 2015 Aaron Hastings
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

package org.thecosmicfrog.luasataglance.object;

import java.util.HashMap;

public class StopNameIdMap extends HashMap<String, String> {
    public StopNameIdMap(String localeDefault) {
        if (localeDefault.startsWith("ga")) {
            // Red Line
            put("Iosta na Rinne", "TPT");
            put("Duga Spencer", "SDK");
            put("Cearnóg an Mhéara - CNÉ", "MYS");
            put("Duga Sheoirse", "GDK");
            put("Conghaile", "CON");
            put("Busáras", "BUS");
            put("Sráid na Mainistreach", "ABB");
            put("Jervis", "JER");
            put("Na Ceithre Cúirteanna", "FOU");
            put("Margadh na Feirme", "SMI");
            put("Árd-Mhúsaem", "MUS");
            put("Heuston", "HEU");
            put("Ospidéal San Séamas", "JAM");
            put("Fatima", "FAT");
            put("Rialto", "RIA");
            put("Bóthar na Siúire", "SUI");
            put("An Droichead Órga", "GOL");
            put("Droimeanach", "DRI");
            put("An Capall Dubh", "BLA");
            put("An Cloigín Gorm", "BLU");
            put("An Chill Mhór", "KYL");
            put("An Bhó Dhearg", "RED");
            put("Coill an Rí", "KIN");
            put("Belgard", "BEL");
            put("Baile an Chócaigh", "COO");
            put("Ospidéal Thamhlachta", "HOS");
            put("Tamhlacht", "TAL");
            put("Fothar Chardain", "FET");
            put("Baile an tSíbrigh", "CVN");
            put("Campas Gnó Iarthar na Cathrach", "CIT");
            put("Baile Uí Fhoirtcheirn", "FOR");
            put("Teach Sagard", "SAG");

            // Green Line
            put("Faiche Stiabhna", "STS");
            put("Sráid Fhearchair", "HAR");
            put("Charlemont", "CHA");
            put("Raghnallach", "RAN");
            put("Coill na Feá", "BEE");
            put("Cowper", "COW");
            put("Baile an Mhuilinn", "MIL");
            put("Na Glasáin", "WIN");
            put("Dún Droma", "DUN");
            put("Baile Amhlaoibh", "BAL");
            put("Cill Mochuda", "KIL");
            put("Stigh Lorgan", "STI");
            put("Áth an Ghainimh", "SAN");
            put("An Pháirc Láir", "CPK");
            put("Gleann an Chairn", "GLE");
            put("An Eachrais", "GAL");
            put("Gleann Bhaile na Lobhar", "LEO");
            put("Coill Bhaile Uí Ógáin", "BAW");
            put("Carraig Mhaighin", "CCK");
            put("Baile an Locháin", "LAU");
            put("Coill na Silíní", "CHE");
            put("Gleann Bhríde", "BRI");
        } else {
            // Red Line
            put("The Point", "TPT");
            put("Spencer Dock", "SDK");
            put("Mayor Square - NCI", "MYS");
            put("George's Dock", "GDK");
            put("Connolly", "CON");
            put("Busáras", "BUS");
            put("Abbey Street", "ABB");
            put("Jervis", "JER");
            put("Four Courts", "FOU");
            put("Smithfield", "SMI");
            put("Museum", "MUS");
            put("Heuston", "HEU");
            put("James's", "JAM");
            put("Fatima", "FAT");
            put("Rialto", "RIA");
            put("Suir Road", "SUI");
            put("Goldenbridge", "GOL");
            put("Drimnagh", "DRI");
            put("Blackhorse", "BLA");
            put("Bluebell", "BLU");
            put("Kylemore", "KYL");
            put("Red Cow", "RED");
            put("Kingswood", "KIN");
            put("Belgard", "BEL");
            put("Cookstown", "COO");
            put("Hospital", "HOS");
            put("Tallaght", "TAL");
            put("Fettercairn", "FET");
            put("Cheeverstown", "CVN");
            put("Citywest Campus", "CIT");
            put("Fortunestown", "FOR");
            put("Saggart", "SAG");

            // Green Line
            put("St. Stephen's Green", "STS");
            put("Harcourt", "HAR");
            put("Charlemont", "CHA");
            put("Ranelagh", "RAN");
            put("Beechwood", "BEE");
            put("Cowper", "COW");
            put("Milltown", "MIL");
            put("Windy Arbour", "WIN");
            put("Dundrum", "DUN");
            put("Balally", "BAL");
            put("Kilmacud", "KIL");
            put("Stillorgan", "STI");
            put("Sandyford", "SAN");
            put("Central Park", "CPK");
            put("Glencairn", "GLE");
            put("The Gallops", "GAL");
            put("Leopardstown Valley", "LEO");
            put("Ballyogan Wood", "BAW");
            put("Carrickmines", "CCK");
            put("Laughanstown", "LAU");
            put("Cherrywood", "CHE");
            put("Brides Glen", "BRI");
        }
    }
}
