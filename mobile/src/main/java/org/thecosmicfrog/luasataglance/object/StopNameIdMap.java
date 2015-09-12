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
            put("Iosta na Rinne", "LUAS57");
            put("Duga Spencer", "LUAS56");
            put("Cearnóg an Mhéara - CNÉ", "LUAS55");
            put("Duga Sheoirse", "LUAS54");
            put("Conghaile", "LUAS23");
            put("Busáras", "LUAS22");
            put("Sráid na Mainistreach", "LUAS21");
            put("Jervis", "LUAS20");
            put("Na Ceithre Cúirteanna", "LUAS19");
            put("Margadh na Feirme", "LUAS18");
            put("Árd-Mhúsaem", "LUAS17");
            put("Heuston", "LUAS16");
            put("Ospidéal San Séamas", "LUAS15");
            put("Fatima", "LUAS14");
            put("Rialto", "LUAS13");
            put("Bóthar na Siúire", "LUAS12");
            put("An Droichead Órga", "LUAS11");
            put("Droimeanach", "LUAS10");
            put("An Capall Dubh", "LUAS9");
            put("An Cloigín Gorm", "LUAS8");
            put("An Chill Mhór", "LUAS7");
            put("An Bhó Dhearg", "LUAS6");
            put("Coill an Rí", "LUAS5");
            put("Belgard", "LUAS4");
            put("Baile an Chócaigh", "LUAS3");
            put("Ospidéal Thamhlachta", "LUAS2");
            put("Tamhlacht", "LUAS1");
            put("Fothar Chardain", "LUAS49");
            put("Baile an tSíbrigh", "LUAS50");
            put("Campas Gnó Iarthar na Cathrach", "LUAS51");
            put("Baile Uí Fhoirtcheirn", "LUAS52");
            put("Teach Sagard", "LUAS53");

            // Green Line
            put("Faiche Stiabhna", "LUAS24");
            put("Sráid Fhearchair", "LUAS25");
            put("Charlemont", "LUAS26");
            put("Raghnallach", "LUAS27");
            put("Coill na Feá", "LUAS28");
            put("Cowper", "LUAS29");
            put("Baile an Mhuilinn", "LUAS30");
            put("Na Glasáin", "LUAS31");
            put("Dún Droma", "LUAS32");
            put("Baile Amhlaoibh", "LUAS33");
            put("Cill Mochuda", "LUAS34");
            put("Stigh Lorgan", "LUAS35");
            put("Áth an Ghainimh", "LUAS36");
            put("An Pháirc Láir", "LUAS37");
            put("Gleann an Chairn", "LUAS38");
            put("An Eachrais", "LUAS39");
            put("Gleann Bhaile na Lobhar", "LUAS40");
            put("Coill Bhaile Uí Ógáin", "LUAS42");
            put("Carraig Mhaighin", "LUAS44");
            put("Baile an Locháin", "LUAS46");
            put("Coill na Silíní", "LUAS47");
            put("Gleann Bhríde", "LUAS48");
        } else {
            // Red Line
            put("The Point", "LUAS57");
            put("Spencer Dock", "LUAS56");
            put("Mayor Square - NCI", "LUAS55");
            put("George's Dock", "LUAS54");
            put("Connolly", "LUAS23");
            put("Busáras", "LUAS22");
            put("Abbey Street", "LUAS21");
            put("Jervis", "LUAS20");
            put("Four Courts", "LUAS19");
            put("Smithfield", "LUAS18");
            put("Museum", "LUAS17");
            put("Heuston", "LUAS16");
            put("James's", "LUAS15");
            put("Fatima", "LUAS14");
            put("Rialto", "LUAS13");
            put("Suir Road", "LUAS12");
            put("Goldenbridge", "LUAS11");
            put("Drimnagh", "LUAS10");
            put("Blackhorse", "LUAS9");
            put("Bluebell", "LUAS8");
            put("Kylemore", "LUAS7");
            put("Red Cow", "LUAS6");
            put("Kingswood", "LUAS5");
            put("Belgard", "LUAS4");
            put("Cookstown", "LUAS3");
            put("Hospital", "LUAS2");
            put("Tallaght", "LUAS1");
            put("Fettercairn", "LUAS49");
            put("Cheeverstown", "LUAS50");
            put("Citywest Campus", "LUAS51");
            put("Fortunestown", "LUAS52");
            put("Saggart", "LUAS53");

            // Green Line
            put("St. Stephen's Green", "LUAS24");
            put("Harcourt", "LUAS25");
            put("Charlemont", "LUAS26");
            put("Ranelagh", "LUAS27");
            put("Beechwood", "LUAS28");
            put("Cowper", "LUAS29");
            put("Milltown", "LUAS30");
            put("Windy Arbour", "LUAS31");
            put("Dundrum", "LUAS32");
            put("Balally", "LUAS33");
            put("Kilmacud", "LUAS34");
            put("Stillorgan", "LUAS35");
            put("Sandyford", "LUAS36");
            put("Central Park", "LUAS37");
            put("Glencairn", "LUAS38");
            put("The Gallops", "LUAS39");
            put("Leopardstown Valley", "LUAS40");
            put("Ballyogan Wood", "LUAS42");
            put("Carrickmines", "LUAS44");
            put("Laughanstown", "LUAS46");
            put("Cherrywood", "LUAS47");
            put("Brides Glen", "LUAS48");
        }
    }
}
