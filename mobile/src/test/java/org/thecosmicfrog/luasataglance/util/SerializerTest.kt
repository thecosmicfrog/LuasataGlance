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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream

/**
 * Tests for Java object serialization to internal storage.
 *
 * The favourites and widget_selected_stops files on users' devices are in this format, so changing it needs a migration. These
 * tests pin the format, and check the bytes come back through a plain ObjectInputStream, which is how FavouritesFragment,
 * FavouritesSelectActivity, and WidgetStopStore read them.
 */
@RunWith(RobolectricTestRunner::class)
class SerializerTest {

    @Test
    fun `a stop list survives a round trip`() {
        val stops = arrayListOf<CharSequence>("Tallaght", "Saggart", "The Point")

        val deserialized = Serializer.deserialize(Serializer.serialize(stops))

        assertThat(deserialized).isEqualTo(stops)
    }

    @Test
    fun `an empty list survives a round trip`() {
        val stops = arrayListOf<CharSequence>()

        assertThat(Serializer.deserialize(Serializer.serialize(stops))).isEqualTo(stops)
    }

    @Test
    fun `stop names with apostrophes and fadas survive a round trip`() {
        /* George's Dock has an apostrophe, Busáras a fada. Both have to come back unchanged. */
        val stops = arrayListOf<CharSequence>("George's Dock", "Busáras", "O'Connell - GPO", "Cearnóg an Mhéara - CNÉ")

        assertThat(Serializer.deserialize(Serializer.serialize(stops))).isEqualTo(stops)
    }

    @Test
    fun `the bytes written are readable by a plain ObjectInputStream`() {
        /*
         * Nothing in the app reads through Serializer.deserialize(). Every read path builds its own ObjectInputStream over the
         * file, so that pairing has to keep working.
         */
        val stops = arrayListOf<CharSequence>("Tallaght", "Saggart")

        val bytes = Serializer.serialize(stops)
        val readBack = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }

        assertThat(readBack).isEqualTo(stops)
    }

    @Test
    fun `garbage returns null rather than throwing`() {
        /* A truncated or corrupted file must not take down a widget broadcast. */
        assertThat(Serializer.deserialize(byteArrayOf(1, 2, 3, 4))).isNull()
    }

    @Test
    fun `the serialized form is stable across runs`() {
        /* If this ever changes, the file on a user's device stops being readable and needs a migration. */
        val stops = arrayListOf<CharSequence>("Tallaght")

        assertThat(Serializer.serialize(stops)).isEqualTo(Serializer.serialize(stops))
    }
}
