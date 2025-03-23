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

package org.thecosmicfrog.luasataglance.util

import androidx.recyclerview.widget.RecyclerView
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.databinding.FragmentGreenlineBinding
import org.thecosmicfrog.luasataglance.databinding.FragmentRedlineBinding

class LineFragmentViewBindingAdapter(b1: FragmentRedlineBinding?, b2: FragmentGreenlineBinding?) {

    /*
     * This class is used to wrap the identical resources in both Red Line and Green Line fragments, so that view bindings can be
     * used without too much hacky code in the LineFragment class.
     * Source: https://stackoverflow.com/a/67680181/2083329
     */
    val stopForecastConstraintLayout = b1?.redlineStopforecastconstraintlayout ?: b2?.greenlineStopforecastconstraintlayout
    val recyclerViewStopForecastsInbound = b1?.redlineStopforecastconstraintlayout?.findViewById<RecyclerView>(R.id.recyclerview_stop_forecasts_inbound)
        ?: b2?.greenlineStopforecastconstraintlayout?.findViewById(R.id.recyclerview_stop_forecasts_inbound)

    val recyclerViewStopForecastsOutbound = b1?.redlineStopforecastconstraintlayout?.findViewById<RecyclerView>(R.id.recyclerview_stop_forecasts_outbound)
        ?: b2?.greenlineStopforecastconstraintlayout?.findViewById(R.id.recyclerview_stop_forecasts_outbound)
    val progressbar = b1?.redlineProgressbar ?: b2?.greenlineProgressbar
    val scrollview = b1?.redlineScrollview ?: b2?.greenlineScrollview
    val spinnerCardView = b1?.redlineSpinnerCardView ?: b2?.greenlineSpinnerCardView
    val statuscardview = b1?.redlineStatuscardview ?: b2?.greenlineStatuscardview
    val tutorialcardviewSelectStop = b1?.tutorialcardviewSelectStop ?: b2?.tutorialcardviewSelectStop
    val swiperefreshlayout = b1?.redlineSwiperefreshlayout ?: b2?.greenlineSwiperefreshlayout
}
