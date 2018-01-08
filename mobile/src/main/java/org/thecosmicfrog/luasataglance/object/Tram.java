/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2018 Aaron Hastings
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

import java.io.Serializable;

public class Tram implements Serializable {

    private static final long serialVersionUID = 0L;

    private String destination;
    private String direction;
    private String dueMinutes;

    public Tram(String de, String di, String du) {
        destination = de;
        direction = di;
        dueMinutes = du;
    }

    @Override
    public String toString() {
        if (!dueMinutes.equals("DUE")) {
            if (Integer.parseInt(dueMinutes) > 1)
                return String.format("%s\t%s\t%s", destination, direction, dueMinutes + " mins");
        }

        return String.format("%s\t%s\t%s", destination, direction, dueMinutes + " min");
    }

    public String getDestination() {
        return destination;
    }

    public String getDirection() {
        return direction;
    }

    public String getDueMinutes() {
        return dueMinutes;
    }
}
