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
import androidx.appcompat.app.AppCompatDelegate
import org.thecosmicfrog.luasataglance.R

object ThemeUtil {

    /**
     * Apply a theme by its preference value.
     *
     * @param context Context.
     * @param theme One of the values in `array_theme_values`.
     */
    fun applyTheme(context: Context, theme: String?) {
        AppCompatDelegate.setDefaultNightMode(themeSetting(context, theme).appCompatNightMode)
    }

    /**
     * Apply a theme the user has just picked, and tell Android which colours to draw the splash screen in.
     *
     * @param context Context.
     * @param theme One of the values in `array_theme_values`.
     */
    fun applyPickedTheme(context: Context, theme: String?) {
        applyTheme(context, theme)

        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

        /* Only affects the splash screen. */
        uiModeManager.setApplicationNightMode(themeSetting(context, theme).splashNightMode)
    }

    /**
     * Match a stored preference value against the three the picker offers.
     *
     * @param context Context.
     * @param theme One of the values in `array_theme_values`.
     * @return The matching setting, or [ThemeSetting.SYSTEM] for anything unrecognised.
     */
    private fun themeSetting(context: Context, theme: String?) = when (theme) {
        context.getString(R.string.pref_value_theme_light) -> ThemeSetting.LIGHT
        context.getString(R.string.pref_value_theme_dark) -> ThemeSetting.DARK

        /* An unrecognised value comes out as the Android system option. */
        else -> ThemeSetting.SYSTEM
    }

    /**
     * A Theme setting and the night mode it means to each API.
     */
    private enum class ThemeSetting(val appCompatNightMode: Int, val splashNightMode: Int) {

        /* UiModeManager has no "match the device" constant. MODE_NIGHT_AUTO follows the device's light and dark setting. */
        SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, UiModeManager.MODE_NIGHT_AUTO),
        LIGHT(AppCompatDelegate.MODE_NIGHT_NO, UiModeManager.MODE_NIGHT_NO),
        DARK(AppCompatDelegate.MODE_NIGHT_YES, UiModeManager.MODE_NIGHT_YES)
    }
}
