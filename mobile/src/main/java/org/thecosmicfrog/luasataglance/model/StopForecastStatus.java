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

package org.thecosmicfrog.luasataglance.model;

import com.google.gson.annotations.SerializedName;

public class StopForecastStatus {
    @SerializedName("inbound")
    private StopForecastStatusDirection stopForecastStatusDirectionInbound;

    @SerializedName("outbound")
    private StopForecastStatusDirection stopForecastStatusDirectionOutbound;

    public StopForecastStatus() {
        stopForecastStatusDirectionInbound = new StopForecastStatusDirection();
        stopForecastStatusDirectionOutbound = new StopForecastStatusDirection();
    }

    public StopForecastStatusDirection getStopForecastStatusDirectionInbound() {
        return stopForecastStatusDirectionInbound;
    }

    public StopForecastStatusDirection getStopForecastStatusDirectionOutbound() {
        return stopForecastStatusDirectionOutbound;
    }

    public void setStopForecastStatusDirectionInbound(StopForecastStatusDirection f) {
        stopForecastStatusDirectionInbound = f;
    }

    public void setStopForecastStatusDirectionOutbound(StopForecastStatusDirection f) {
        stopForecastStatusDirectionOutbound = f;
    }
}
