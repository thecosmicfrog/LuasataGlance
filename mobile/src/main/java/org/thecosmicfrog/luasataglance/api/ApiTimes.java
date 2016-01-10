/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2016 Aaron Hastings
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

package org.thecosmicfrog.luasataglance.api;

import org.thecosmicfrog.luasataglance.object.Tram;

import java.util.List;

public class ApiTimes {

    private String message;
    private List<Tram> trams;

    public String getMessage() {
        return message;
    }

    public List<Tram> getTrams() {
        return trams;
    }

    public void setMessage(String m) {
        message = m;
    }

    public void setTrams(List<Tram> t) {
        trams = t;
    }
}
