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

package org.thecosmicfrog.luasataglance.api;

import java.util.List;

public class ApiTimes {

    private String errorcode;
    private String errormessage;
    private int numberofresults;
    private String stopid;
    private String timestamp;
    private List<Result> results;

    public class Result {
        public String duetime;
        public String direction;
        public String destination;
    }

    public String getErrorcode() {
        return errorcode;
    }

    public String getErrormessage() {
        return errormessage;
    }

    public int getNumberofresults() {
        return numberofresults;
    }

    public String getStopid() {
        return stopid;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public List<Result> getResults() {
        return results;
    }
}
