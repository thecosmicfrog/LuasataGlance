/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2026 Aaron Hastings
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
package org.thecosmicfrog.luasataglance.view

import android.content.Context
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.content.ContextCompat
import org.thecosmicfrog.luasataglance.R

/**
 * A Spinner whose dropdown arrow follows `icon_accent`.
 *
 * Used by the stop picker in [SpinnerCardView] and the notify-time picker
 * in [org.thecosmicfrog.luasataglance.activity.NotifyTimeActivity].
 */
class LaagSpinner(context: Context, attrs: AttributeSet?) : AppCompatSpinner(context, attrs) {

    init {
        /* `mutate` first, or the filter reaches every other view sharing this drawable. */
        background?.mutate()?.colorFilter = BlendModeColorFilter(
            ContextCompat.getColor(context, R.color.icon_accent),
            BlendMode.SRC_ATOP
        )
    }
}
