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

package org.thecosmicfrog.luasataglance.model;

import java.util.HashMap;

public class NotifyTimesMap extends HashMap<String, Integer> {

    public NotifyTimesMap(String locale, String type) {
        final String DIALOG = "dialog";
        final String GAEILGE = "ga";
        final String STOP_FORECAST = "stop_forecast";

        String minsBeforeArrival;
        String mins;

        if (locale.startsWith(GAEILGE)) {
            minsBeforeArrival = "nóim roimh theacht";
            mins = "nóim";
        } else {
            minsBeforeArrival = "mins before arrival";
            mins = "mins";
        }

        switch (type) {
            case DIALOG:
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

                break;

            case STOP_FORECAST:
                put("2 " + mins, 2);
                put("3 " + mins, 3);
                put("4 " + mins, 4);
                put("5 " + mins, 5);
                put("6 " + mins, 6);
                put("7 " + mins, 7);
                put("8 " + mins, 8);
                put("9 " + mins, 9);
                put("10 " + mins, 10);
                put("11 " + mins, 11);
                put("12 " + mins, 12);
                put("13 " + mins, 13);
                put("14 " + mins, 14);
                put("15 " + mins, 15);
                put("16 " + mins, 16);
                put("17 " + mins, 17);
                put("18 " + mins, 18);
                put("19 " + mins, 19);
                put("20 " + mins, 20);
                put("21 " + mins, 21);
                put("22 " + mins, 22);
                put("23 " + mins, 23);
                put("24 " + mins, 24);
                put("25 " + mins, 25);
                put("26 " + mins, 26);
                put("27 " + mins, 27);
                put("28 " + mins, 28);
                put("29 " + mins, 29);
                put("30 " + mins, 30);
        }
    }
}
