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

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.adapter.FavouritesSelectAdapter
import org.thecosmicfrog.luasataglance.databinding.ActivityFavouritesSelectBinding
import org.thecosmicfrog.luasataglance.util.Serializer
import java.io.BufferedInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.ObjectInput
import java.io.ObjectInputStream

class FavouritesSelectActivity : AppCompatActivity() {

    private val logTag: String = FavouritesSelectActivity::class.java.getSimpleName()
    private val fileFavourites = "favourites"

    private var viewBinding: ActivityFavouritesSelectBinding? = null
    private var adapter: FavouritesSelectAdapter? = null
    private var selectedStops: ArrayList<CharSequence?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityFavouritesSelectBinding.inflate(layoutInflater)
        val rootView: FrameLayout? = viewBinding?.getRoot()
        setContentView(rootView)

        supportActionBar?.setBackgroundDrawable(
            ContextCompat.getColor(application, R.color.luas_purple).toDrawable()
        )

        selectedStops = ArrayList<CharSequence?>()
        val listAllStops = loadAllStops()

        initRecyclerView(listAllStops)
        initFab()
        loadExistingfavourites()
    }

    /**
     * Load the list of stops from resources.
     * @return List of all stops across all lines.
     */
    private fun loadAllStops(): ArrayList<CharSequence?> {
        val allStops = getResources().getStringArray(R.array.array_stops_all)
        val listAllStops = ArrayList<CharSequence?>()

        /* Skip the first element ("None" at index 0) and add the rest. */
        for (i in 1..<allStops.size) {
            if (allStops[i] != getString(R.string.select_a_stop)) {
                listAllStops.add(allStops[i])
            }
        }

        return listAllStops
    }

    /**
     * Initialise the RecyclerView with the list of stops.
     * @param stops The list of stops to display.
     */
    private fun initRecyclerView(stops: ArrayList<CharSequence?>) {
        val recyclerView = viewBinding?.recyclerviewStops
        recyclerView?.setLayoutManager(LinearLayoutManager(this))

        adapter = FavouritesSelectAdapter(stops, selectedStops)
        recyclerView?.setAdapter(adapter)
        adapter?.notifyDataSetChanged()
    }

    /**
     * Initialise the edit FAB.
     */
    private fun initFab() {
        val fabFavouritesSave = viewBinding!!.fabFavouritesSave
        fabFavouritesSave.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.message_success))
        fabFavouritesSave.setOnClickListener(View.OnClickListener { v: View? -> saveFavourites() })
    }

    /**
     * Load the existing favourites from the file on disk.
     */
    private fun loadExistingfavourites() {
        try {
            val fileInput: InputStream = openFileInput(fileFavourites)
            val buffer: InputStream = BufferedInputStream(fileInput)
            val objectInput: ObjectInput = ObjectInputStream(buffer)

            val listFavouriteStops = objectInput.readObject() as MutableList<CharSequence?>

            selectedStops?.addAll(listFavouriteStops)
            adapter?.notifyDataSetChanged()

            objectInput.close()
            buffer.close()
            fileInput.close()
        } catch (e: FileNotFoundException) {
            Log.i(logTag, "Favourites file doesn't exist.")
        } catch (e: ClassNotFoundException) {
            Log.e(logTag, Log.getStackTraceString(e))
        } catch (e: IOException) {
            Log.e(logTag, Log.getStackTraceString(e))
        }
    }

    /**
     * Save the selected stops to the favourites file on disk.
     */
    private fun saveFavourites() {
        try {
            if (!selectedStops!!.isEmpty()) {
                val allStops = getResources().getStringArray(R.array.array_stops_all)

                /* Sorting magic to ensure stops are saved in the order they appear on the map. */
                selectedStops?.sortWith(Comparator { stop1: CharSequence?, stop2: CharSequence? ->
                    val indexStop1 = listOf<String?>(*allStops).indexOf(stop1.toString())
                    val indexStop2 = listOf<String?>(*allStops).indexOf(stop2.toString())
                    indexStop1.compareTo(indexStop2)
                })

                val file = openFileOutput(fileFavourites, MODE_PRIVATE)
                file.write(Serializer.serialize(selectedStops))
                file.close()
            }
        } catch (e: IOException) {
            Log.e(logTag, Log.getStackTraceString(e))
        }

        finish()
    }
}
