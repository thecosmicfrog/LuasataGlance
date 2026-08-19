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
package org.thecosmicfrog.luasataglance.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.thecosmicfrog.luasataglance.util.Serializer

/**
 * Tests for the per-widget stop list held in internal storage.
 *
 * The file format predates the per-instance naming and is on users' devices already, so both the current layout and the fallback
 * to the old shared file are covered here. Getting the fallback wrong either loses an existing widget's configuration or hands it
 * to a newly placed one.
 */
@RunWith(RobolectricTestRunner::class)
class WidgetStopStoreTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `a stop list survives a save and load`() {
        WidgetStopStore.save(context, FIRST_WIDGET_ID, listOf("Tallaght", "Saggart"))

        assertThat(WidgetStopStore.load(context, FIRST_WIDGET_ID)).containsExactly("Saggart", "Tallaght")
    }

    @Test
    fun `stops come back alphabetically rather than in the order they were ticked`() {
        /* The picker hands them back in tick order, which says nothing about where the next and previous arrows will go. */
        WidgetStopStore.save(context, FIRST_WIDGET_ID, listOf("Tallaght", "Abbey Street", "Heuston", "Busáras"))

        assertThat(WidgetStopStore.load(context, FIRST_WIDGET_ID))
            .containsExactly("Abbey Street", "Busáras", "Heuston", "Tallaght")
            .inOrder()
    }

    @Test
    fun `sorting does not depend on capitalisation`() {
        WidgetStopStore.save(context, FIRST_WIDGET_ID, listOf("the Point", "Abbey Street", "belgard"))

        assertThat(WidgetStopStore.load(context, FIRST_WIDGET_ID))
            .containsExactly("Abbey Street", "belgard", "the Point")
            .inOrder()
    }

    @Test
    fun `two widgets keep separate stop lists`() {
        WidgetStopStore.save(context, FIRST_WIDGET_ID, listOf("Tallaght"))
        WidgetStopStore.save(context, SECOND_WIDGET_ID, listOf("Sandyford"))

        assertThat(WidgetStopStore.load(context, FIRST_WIDGET_ID)).containsExactly("Tallaght")
        assertThat(WidgetStopStore.load(context, SECOND_WIDGET_ID)).containsExactly("Sandyford")
    }

    @Test
    fun `an unconfigured widget loads nothing`() {
        assertThat(WidgetStopStore.load(context, FIRST_WIDGET_ID)).isNull()
    }

    @Test
    fun `a widget with no file of its own falls back to the pre-per-instance file`() {
        writeLegacyStopList("Heuston", "Museum")

        assertThat(WidgetStopStore.load(context, FIRST_WIDGET_ID)).containsExactly("Heuston", "Museum").inOrder()
    }

    @Test
    fun `a widget with a file of its own ignores the pre-per-instance file`() {
        writeLegacyStopList("Heuston")
        WidgetStopStore.save(context, FIRST_WIDGET_ID, listOf("Tallaght"))

        assertThat(WidgetStopStore.load(context, FIRST_WIDGET_ID)).containsExactly("Tallaght")
    }

    @Test
    fun `deleting one widget's list leaves the other alone`() {
        WidgetStopStore.save(context, FIRST_WIDGET_ID, listOf("Tallaght"))
        WidgetStopStore.save(context, SECOND_WIDGET_ID, listOf("Sandyford"))

        WidgetStopStore.delete(context, FIRST_WIDGET_ID)

        assertThat(WidgetStopStore.load(context, FIRST_WIDGET_ID)).isNull()
        assertThat(WidgetStopStore.load(context, SECOND_WIDGET_ID)).containsExactly("Sandyford")
    }

    @Test
    fun `deleting a widget's list falls back to the pre-per-instance file if one is there`() {
        /* load() tries the per-instance file first, so deleting it exposes the legacy file rather than returning null. */
        writeLegacyStopList("Heuston")
        WidgetStopStore.save(context, FIRST_WIDGET_ID, listOf("Tallaght"))

        WidgetStopStore.delete(context, FIRST_WIDGET_ID)

        assertThat(WidgetStopStore.load(context, FIRST_WIDGET_ID)).containsExactly("Heuston")
    }

    @Test
    fun `the pre-per-instance file can be cleared once the last widget is gone`() {
        writeLegacyStopList("Heuston")

        WidgetStopStore.deleteLegacy(context)

        assertThat(WidgetStopStore.load(context, FIRST_WIDGET_ID)).isNull()
    }

    @Test
    fun `an empty saved list loads as an empty list, not null`() {
        WidgetStopStore.save(context, FIRST_WIDGET_ID, emptyList())

        assertThat(WidgetStopStore.load(context, FIRST_WIDGET_ID)).isEmpty()
    }

    @Test
    fun `a corrupted file loads as null rather than throwing`() {
        /* This is read from onReceive(), so anything escaping takes the app down with it. */
        context.openFileOutput("widget_selected_stops_$FIRST_WIDGET_ID", Context.MODE_PRIVATE).use {
            it.write(byteArrayOf(1, 2, 3, 4))
        }

        assertThat(WidgetStopStore.load(context, FIRST_WIDGET_ID)).isNull()
    }

    /**
     * Writes the single shared file used before storage became per-widget. Nothing in the app writes it anymore, so a test
     * standing in for an old install has to write it directly.
     */
    private fun writeLegacyStopList(vararg stops: String) {
        context.openFileOutput("widget_selected_stops", Context.MODE_PRIVATE).use { fileOutput ->
            fileOutput.write(Serializer.serialize(ArrayList<CharSequence>(stops.toList())))
        }
    }

    companion object {
        private const val FIRST_WIDGET_ID = 101
        private const val SECOND_WIDGET_ID = 202
    }
}
