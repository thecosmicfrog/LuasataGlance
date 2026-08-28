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
package org.thecosmicfrog.luasataglance.util

import android.app.UiModeManager
import android.content.Context
import org.thecosmicfrog.luasataglance.R

object ThemeUtil {

    /**
     * Apply a theme by its preference value.
     *
     * @param context Context.
     * @param theme One of the values in `array_theme_values`.
     */
    fun applyTheme(context: Context, theme: String?) {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

        uiModeManager.setApplicationNightMode(
            when (theme) {
                context.getString(R.string.pref_value_theme_light) -> UiModeManager.MODE_NIGHT_NO
                context.getString(R.string.pref_value_theme_dark) -> UiModeManager.MODE_NIGHT_YES

                /* UiModeManager has no "match the device" constant. MODE_NIGHT_AUTO follows the device's light and dark setting. */
                else -> UiModeManager.MODE_NIGHT_AUTO
            }
        )
    }
}
