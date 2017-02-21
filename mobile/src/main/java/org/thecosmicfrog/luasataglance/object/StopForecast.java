/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2017 Aaron Hastings
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
import java.util.ArrayList;
import java.util.List;

public class StopForecast implements Serializable {

    private static final long serialVersionUID = 0L;

    private final String LOG_TAG = StopForecast.class.getSimpleName();

    private String message;
    private List<Tram> inboundTrams;
    private List<Tram> outboundTrams;

    public StopForecast() {
        inboundTrams = new ArrayList<>();
        outboundTrams = new ArrayList<>();
    }

    public void addInboundTram(Tram t) {
        /* Check there are actually inbound trams running. */
        if (t != null)
            inboundTrams.add(t);
    }

    public void addOutboundTram(Tram t) {
        /* Check there are actually outbound trams running. */
        if (t != null)
            outboundTrams.add(t);
    }

    public void setMessage(String m) {
        message = m;
    }

    public void setInboundTrams(List<Tram> i) {
        inboundTrams = i;
    }

    public void setOutboundTrams(List<Tram> o) {
        outboundTrams = o;
    }

    public String getMessage() {
        return message;
    }

    public List<Tram> getInboundTrams() {
        return inboundTrams;
    }

    public List<Tram> getOutboundTrams() {
        return outboundTrams;
    }
}
