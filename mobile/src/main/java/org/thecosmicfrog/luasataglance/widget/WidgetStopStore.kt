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
import android.util.Log
import org.thecosmicfrog.luasataglance.util.Serializer
import java.io.BufferedInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.ObjectInputStream

/**
 * Reads, writes, and deletes the list of stops that a single widget instance is configured with.
 *
 * Each instance owns its own file, named [FILE_NAME_PREFIX] followed by the AppWidget ID, so that two widgets on the same home
 * screen cannot overwrite one another's configuration.
 *
 * Both the widget provider and its configuration activity read this list, so the naming scheme and the legacy fallback live here
 * rather than being duplicated in each.
 */
object WidgetStopStore {

    private val logTag = WidgetStopStore::class.java.simpleName

    private const val FILE_NAME_PREFIX = "widget_selected_stops_"

    /**
     * The single, shared file written by every version of the app before per-instance storage existed. It is read as a fallback,
     * but never written, so that widgets placed before this change keep working until the user next saves them.
     */
    private const val FILE_NAME_LEGACY = "widget_selected_stops"

    /**
     * Loads the stops configured for one widget instance, sorted alphabetically.
     *
     * The picker hands its selection back in the order the stops were ticked, which tells the user nothing about where the next
     * and previous arrows will take them. Sorting on read rather than on save means widgets configured before this change get the
     * same order without being reconfigured.
     *
     * @return The list of stop names, or null if neither the per-instance file nor the
     *         legacy file could be read.
     */
    fun load(context: Context, appWidgetId: Int): List<String>? {
        return (read(context, fileName(appWidgetId)) ?: read(context, FILE_NAME_LEGACY))?.sortedBy { it.lowercase() }
    }

    /**
     * Persists the stops configured for one widget instance.
     *
     * @throws IOException if the file cannot be written. Callers are expected to handle this.
     */
    fun save(context: Context, appWidgetId: Int, stops: List<CharSequence?>) {
        context.openFileOutput(fileName(appWidgetId), Context.MODE_PRIVATE).use { fileOutput ->
            fileOutput.write(Serializer.serialize(ArrayList(stops)))
        }
    }

    /**
     * Deletes the stop list belonging to one widget instance. Called when that instance is removed from the home screen, so that
     * its file does not outlive it.
     */
    fun delete(context: Context, appWidgetId: Int) {
        context.deleteFile(fileName(appWidgetId))
    }

    /**
     * Deletes the shared pre-per-instance stop list. Called once the last widget is gone, since nothing can read it again and it
     * would otherwise be handed to the next widget placed.
     */
    fun deleteLegacy(context: Context) {
        context.deleteFile(FILE_NAME_LEGACY)
    }

    /**
     * Builds the internal storage filename for a given widget instance.
     */
    private fun fileName(appWidgetId: Int) = "$FILE_NAME_PREFIX$appWidgetId"

    /**
     * Deserialises a stop list from internal storage.
     *
     * @return The list of stop names, or null if the file does not exist or cannot be read.
     */
    private fun read(context: Context, fileName: String): List<String>? {
        try {
            context.openFileInput(fileName).use { fileInput ->
                ObjectInputStream(BufferedInputStream(fileInput)).use { objectInput ->
                    @Suppress("UNCHECKED_CAST")
                    return (objectInput.readObject() as? List<CharSequence>)?.map { it.toString() }
                }
            }
        } catch (_: FileNotFoundException) {
            /* Expected. load() tries the per-instance file before falling back to the legacy one. */
            return null
        } catch (e: Exception) {
            /*
             * Deliberately broad. Reading a file format this old fails in several unrelated ways, and readObject throws
             * ClassNotFoundException. This runs from onReceive, so anything escaping here takes the app down.
             */
            Log.e(logTag, Log.getStackTraceString(e))

            return null
        }
    }
}
