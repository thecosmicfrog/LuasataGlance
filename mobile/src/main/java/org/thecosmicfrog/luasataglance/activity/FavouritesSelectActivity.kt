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
package org.thecosmicfrog.luasataglance.activity

import android.util.Log
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.util.Serializer
import java.io.BufferedInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.ObjectInputStream

/**
 * Allows the user to select their favourite stops. Extends [StopSelectActivity] and
 * persists the selection to the "favourites" file in internal storage.
 */
class FavouritesSelectActivity : StopSelectActivity() {

    private val logTag: String = FavouritesSelectActivity::class.java.simpleName

    override val toolbarTitleRes = R.string.title_activity_favourites_select
    override val fabTextRes = R.string.favourites_save_selected

    override fun loadSavedStops(): List<CharSequence> {
        return try {
            openFileInput("favourites").use { fileInput ->
                ObjectInputStream(BufferedInputStream(fileInput)).use { objectInput ->
                    @Suppress("UNCHECKED_CAST")
                    (objectInput.readObject() as? List<CharSequence>) ?: emptyList()
                }
            }
        } catch (e: FileNotFoundException) {
            Log.i(logTag, "Favourites file doesn't exist.")
            emptyList()
        } catch (e: ClassNotFoundException) {
            Log.e(logTag, Log.getStackTraceString(e))
            emptyList()
        } catch (e: IOException) {
            Log.e(logTag, Log.getStackTraceString(e))
            emptyList()
        }
    }

    override fun onSave() {
        try {
            if (selectedItems.isNotEmpty()) {
                openFileOutput("favourites", MODE_PRIVATE).use { file ->
                    file.write(Serializer.serialize(selectedItems))
                }
            }
        } catch (e: IOException) {
            Log.e(logTag, Log.getStackTraceString(e))
        }

        finish()
    }
}
