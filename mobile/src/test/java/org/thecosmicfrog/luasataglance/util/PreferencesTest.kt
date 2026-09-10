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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.thecosmicfrog.luasataglance.R

/**
 * Tests for [Preferences], the wrapper every shared preference in the app goes through.
 *
 * Every getter repeats its key as a string literal, so a getter and its setter can name different keys and still compile. The
 * round trips catch that. The per-widget keys get more attention, since two widgets on one key means two widgets stuck on the
 * same stop.
 */
@RunWith(RobolectricTestRunner::class)
class PreferencesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `the current app version round trips`() {
        Preferences.saveCurrentAppVersion(context, "0.185")

        assertThat(Preferences.currentAppVersion(context)).isEqualTo("0.185")
    }

    @Test
    fun `an unset app version reports as not found`() {
        assertThat(Preferences.currentAppVersion(context)).isEqualTo("-1")
    }

    @Test
    fun `the welcome screen counts as unshown until it says otherwise`() {
        /* MainActivity launches WelcomeActivity on a false, so a fresh install has to read false rather than throwing. */
        assertThat(Preferences.welcomeShown(context)).isFalse()

        Preferences.saveWelcomeShown(context, true)

        assertThat(Preferences.welcomeShown(context)).isTrue()
    }

    @Test
    fun `the next stop index round trips and defaults to the first entry`() {
        assertThat(Preferences.indexNextStopToLoad(context)).isEqualTo(0)

        Preferences.saveIndexNextStopToLoad(context, 7)

        assertThat(Preferences.indexNextStopToLoad(context)).isEqualTo(7)
    }

    @Test
    fun `the notify stop and its expected time round trip`() {
        Preferences.saveNotifyStopName(context, "Tallaght")
        Preferences.saveNotifyStopTimeExpected(context, 9)

        assertThat(Preferences.notifyStopName(context)).isEqualTo("Tallaght")
        assertThat(Preferences.notifyStopTimeExpected(context)).isEqualTo(9)
    }

    @Test
    fun `permission state round trips`() {
        Preferences.savePermissionLocationGranted(context, true)
        Preferences.savePermissionLocationShouldNotAskAgain(context, true)
        Preferences.savePermissionNotificationsShouldNotAskAgain(context, true)

        assertThat(Preferences.permissionLocationGranted(context)).isTrue()
        assertThat(Preferences.permissionLocationShouldNotAskAgain(context)).isTrue()
        assertThat(Preferences.permissionNotificationsShouldNotAskAgain(context)).isTrue()
    }

    @Test
    fun `screen height round trips`() {
        Preferences.saveScreenHeight(context, 1920f)

        assertThat(Preferences.screenHeight(context)).isEqualTo(1920f)
    }

    @Test
    fun `each line remembers its own selected stop`() {
        Preferences.saveSelectedStopName(context, Constant.RED_LINE, "Tallaght")
        Preferences.saveSelectedStopName(context, Constant.GREEN_LINE, "Sandyford")

        assertThat(Preferences.selectedStopName(context, Constant.RED_LINE)).isEqualTo("Tallaght")
        assertThat(Preferences.selectedStopName(context, Constant.GREEN_LINE)).isEqualTo("Sandyford")
    }

    @Test
    fun `the lineless key follows whichever stop was selected most recently`() {
        /*
         * saveSelectedStopName writes two keys, the line's own and the lineless one. So the lineless key holds the last stop
         * picked on either line, not a stop of its own. The per-line keys keep the Red and Green tabs independent.
         */
        Preferences.saveSelectedStopName(context, Constant.RED_LINE, "Tallaght")
        assertThat(Preferences.selectedStopName(context, Constant.NO_LINE)).isEqualTo("Tallaght")

        Preferences.saveSelectedStopName(context, Constant.GREEN_LINE, "Sandyford")
        assertThat(Preferences.selectedStopName(context, Constant.NO_LINE)).isEqualTo("Sandyford")

        assertThat(Preferences.selectedStopName(context, Constant.RED_LINE)).isEqualTo("Tallaght")
    }

    @Test
    fun `the lineless key is unset until a stop is picked`() {
        /* LineFragment null-checks it to decide whether the user has ever chosen a stop, so an unset value has to stay null. */
        assertThat(Preferences.selectedStopName(context, Constant.NO_LINE)).isNull()
    }

    @Test
    fun `the default stop falls back to the none entry`() {
        assertThat(Preferences.defaultStopName(context)).isEqualTo(context.getString(R.string.none))
    }

    @Test
    fun `a default stop survives a save and load`() {
        Preferences.saveDefaultStopName(context, "Tallaght")

        assertThat(Preferences.defaultStopName(context)).isEqualTo("Tallaght")
    }

    @Test
    fun `saving the none entry turns the default stop back off`() {
        Preferences.saveDefaultStopName(context, "Tallaght")
        Preferences.saveDefaultStopName(context, context.getString(R.string.none))

        /* LineFragment compares against R.string.none to decide whether to load a default stop at all. */
        assertThat(Preferences.defaultStopName(context)).isEqualTo(context.getString(R.string.none))
    }

    @Test
    fun `two widgets do not share a stop`() {
        Preferences.saveWidgetSelectedStopName(context, FIRST_WIDGET_ID, "Tallaght")
        Preferences.saveWidgetSelectedStopName(context, SECOND_WIDGET_ID, "Sandyford")

        assertThat(Preferences.widgetSelectedStopName(context, FIRST_WIDGET_ID)).isEqualTo("Tallaght")
        assertThat(Preferences.widgetSelectedStopName(context, SECOND_WIDGET_ID)).isEqualTo("Sandyford")
    }

    @Test
    fun `a widget with no stop of its own falls back to the pre-per-instance one`() {
        writeLegacyWidgetStopName()

        assertThat(Preferences.widgetSelectedStopName(context, FIRST_WIDGET_ID)).isEqualTo(LEGACY_STOP_NAME)
    }

    @Test
    fun `a widget with a stop of its own ignores the pre-per-instance one`() {
        writeLegacyWidgetStopName()
        Preferences.saveWidgetSelectedStopName(context, FIRST_WIDGET_ID, "Tallaght")

        assertThat(Preferences.widgetSelectedStopName(context, FIRST_WIDGET_ID)).isEqualTo("Tallaght")
    }

    @Test
    fun `removing a widget's stop does not disturb another widget`() {
        Preferences.saveWidgetSelectedStopName(context, FIRST_WIDGET_ID, "Tallaght")
        Preferences.saveWidgetSelectedStopName(context, SECOND_WIDGET_ID, "Sandyford")

        Preferences.removeWidgetSelectedStopName(context, FIRST_WIDGET_ID)

        assertThat(Preferences.widgetSelectedStopName(context, FIRST_WIDGET_ID)).isNull()
        assertThat(Preferences.widgetSelectedStopName(context, SECOND_WIDGET_ID)).isEqualTo("Sandyford")
    }

    @Test
    fun `the pre-per-instance stop can be cleared so the next widget placed does not inherit it`() {
        writeLegacyWidgetStopName()

        Preferences.removeLegacyWidgetSelectedStopName(context)

        assertThat(Preferences.widgetSelectedStopName(context, FIRST_WIDGET_ID)).isNull()
    }

    /**
     * Writes the single shared key used before storage became per-widget.
     */
    private fun writeLegacyWidgetStopName() {
        context.getSharedPreferences("org.thecosmicfrog.luasataglance", Context.MODE_PRIVATE)
            .edit()
            .putString("widgetSelectedStopName", LEGACY_STOP_NAME)
            .commit()
    }

    companion object {
        private const val FIRST_WIDGET_ID = 101
        private const val SECOND_WIDGET_ID = 202

        /* Any stop will do. widgetSelectedStopName passes the string straight through without looking at it. */
        private const val LEGACY_STOP_NAME = "Heuston"
    }
}
