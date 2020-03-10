/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2020 Aaron Hastings
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

package org.thecosmicfrog.luasataglance.model;

import java.util.HashMap;

public class NotifyTimesMap extends HashMap<String, Integer> {

    public NotifyTimesMap(String locale) {
        final String GAEILGE = "ga";

        String minsBeforeArrival;

        if (locale.startsWith(GAEILGE)) {
            minsBeforeArrival = "nóim roimh theacht";
        } else {
            minsBeforeArrival = "mins before arrival";
        }

        put("2 " + minsBeforeArrival, 2);
        put("3 " + minsBeforeArrival, 3);
        put("4 " + minsBeforeArrival, 4);
        put("5 " + minsBeforeArrival, 5);
        put("6 " + minsBeforeArrival, 6);
        put("7 " + minsBeforeArrival, 7);
        put("8 " + minsBeforeArrival, 8);
        put("9 " + minsBeforeArrival, 9);
        put("10 " + minsBeforeArrival, 10);
        put("11 " + minsBeforeArrival, 11);
        put("12 " + minsBeforeArrival, 12);
        put("13 " + minsBeforeArrival, 13);
        put("14 " + minsBeforeArrival, 14);
        put("15 " + minsBeforeArrival, 15);
    }
}

