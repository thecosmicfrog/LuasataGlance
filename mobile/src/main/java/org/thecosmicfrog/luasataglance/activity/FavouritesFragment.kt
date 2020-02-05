/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2020 Aaron Hastings
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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.Preferences
import java.io.BufferedInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.ObjectInputStream
import java.lang.Exception

class FavouritesFragment : Fragment() {

    private val logTag = FavouritesFragment::class.java.simpleName
    private var rootView: View? = null

    private var adapterFavouriteStops: ArrayAdapter<CharSequence>? = null
    private var listFavouriteStops: List<CharSequence>? = null
    private var listViewFavouriteStops: ListView? = null

    companion object {
        fun newInstance(): Fragment {
            val favouritesFragment = FavouritesFragment()
            val bundle = Bundle()

            favouritesFragment.arguments = bundle

            return favouritesFragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        /* Inflate the layout for this Fragment. */
        rootView = inflater.inflate(R.layout.fragment_favourites, container, false)

        return rootView
    }

    override fun onResume() {
        super.onResume()

        val tutorialFavourites = "favourites"

        if (isAdded) {
            val fabFavouritesEdit =
                rootView?.findViewById<FloatingActionButton>(R.id.fab_favourites_edit)
            fabFavouritesEdit?.setOnClickListener {
                startActivity(
                    Intent(
                        context,
                        FavouritesSelectActivity::class.java
                    )
                )
            }

            listViewFavouriteStops = rootView?.findViewById(R.id.listview_favourite_stops)

            val textViewFavouritesNoneSelected = rootView?.findViewById<TextView>(
                R.id.textview_favourites_none_selected
            )
            textViewFavouritesNoneSelected?.visibility = View.GONE

            listViewFavouriteStops?.setOnItemClickListener { _, _, position, _ ->
                val stopName = adapterFavouriteStops?.getItem(position) as String

                /*
                 * When a favourite stop is clicked, open the MainActivity, passing the stop
                 * name as an extra parameter.
                 * Since we don't want to litter the back stack with multiple instances of
                 * MainActivity, we also clear the top task. Also disable the new Activity
                 * animation to make the transition seamless.
                 */
                startActivity(
                    Intent(
                        context,
                        MainActivity::class.java
                    ).putExtra(
                        Constant.STOP_NAME,
                        stopName
                    ).setFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                    ).setFlags(
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                    )
                )
            }

            Preferences.saveHasRunOnce(context, tutorialFavourites, true)

            listFavouriteStops = openListFavouritesStops()

            /* ArrayAdapter for favourite stops. */
            adapterFavouriteStops = ArrayAdapter(
                context as Context,
                R.layout.listview_favourites,
                listFavouriteStops as List<CharSequence>
            )

            /*
             * Update ArrayAdapter with newly-selected favourite stops, then set it to the ListView.
             */
            listViewFavouriteStops?.adapter = adapterFavouriteStops
            adapterFavouriteStops?.clear()
            adapterFavouriteStops?.addAll(openListFavouritesStops())
            adapterFavouriteStops?.notifyDataSetChanged()
        }
    }

    private fun openListFavouritesStops(): MutableList<CharSequence> {
        val fileFavourites = "favourites"

        try {
            /* Open input objects. */
            val fileInput = activity?.openFileInput(fileFavourites) as InputStream
            val buffer = BufferedInputStream(fileInput)
            val objectInput = ObjectInputStream(buffer)

            /* Read in List of favourite stops from file. */
            val listFavouriteStops = objectInput.readObject() as MutableList<CharSequence>

            /* Close input objects. */
            objectInput.close()
            buffer.close()
            fileInput.close()

            return listFavouriteStops
        } catch (e: Exception) {
            when(e) {
                is ClassNotFoundException, is FileNotFoundException -> {
                    /*
                     * If the favourites file doesn't exist, the user has probably not set up this
                     * feature yet. Handle the exception gracefully by displaying a TextView with
                     * instructions on how to add favourites.
                     */
                    Log.i(logTag, "Favourites not yet set up.")

                    val textViewFavouritesNoneSelected = rootView?.findViewById<TextView>(
                        R.id.textview_favourites_none_selected
                    )
                    textViewFavouritesNoneSelected?.visibility = View.VISIBLE
                }

                else -> {
                    /* Something has gone wrong; the file may have been corrupted. Delete it. */
                    Log.e(logTag, Log.getStackTraceString(e))
                    Log.i(logTag, "Deleting favourites file.")

                    context?.deleteFile(fileFavourites)
                }
            }
        }

        /* Something has gone wrong. Return an empty MutableList. */
        return mutableListOf()
    }
}

