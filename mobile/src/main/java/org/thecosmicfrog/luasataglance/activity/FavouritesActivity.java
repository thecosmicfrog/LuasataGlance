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

package org.thecosmicfrog.luasataglance.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.databinding.ActivityFavouritesBinding;
import org.thecosmicfrog.luasataglance.util.Constant;
import org.thecosmicfrog.luasataglance.util.Preferences;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

public class FavouritesActivity extends FragmentActivity {

    private final String LOG_TAG = FavouritesActivity.class.getSimpleName();

    private ActivityFavouritesBinding viewBinding;
    private ArrayAdapter<CharSequence> adapterFavouriteStops;
    private List<CharSequence> listFavouriteStops;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewBinding = ActivityFavouritesBinding.inflate(getLayoutInflater());
        View rootView = viewBinding.getRoot();

        final String TUTORIAL_FAVOURITES = "favourites";

        /* Use a Material Dialog theme. */
        setTheme(android.R.style.Theme_Material_Dialog);

        /* This is a Dialog. Get rid of the default Window title. */
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(rootView);

        ImageButton buttonEdit = viewBinding.imagebuttonEdit;
        buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(
                                getApplicationContext(),
                                FavouritesSelectActivity.class)
                );
            }
        });

        listFavouriteStops = getListFavouriteStops();

        /* ArrayAdapter for favourite stops. */
        adapterFavouriteStops = new ArrayAdapter<>(
                getApplicationContext(),
                R.layout.listview_favourites,
                listFavouriteStops
        );

        /*
         * Populate ListView with the user's favourite stops, as read from file.
         */
        ListView listViewFavouriteStops = viewBinding.listviewFavouriteStops;

        if (listFavouriteStops != null) {
            listViewFavouriteStops.setAdapter(adapterFavouriteStops);
            listViewFavouriteStops.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String stopName = adapterFavouriteStops.getItem(position).toString();

                    /*
                     * When a favourite stop is clicked, open the MainActivity, passing the stop
                     * name as an extra parameter.
                     * Since we don't want to litter the back stack with multiple instances of
                     * MainActivity, we also clear the top task. Also disable the new Activity
                     * animation to make the transition seamless.
                     */
                    startActivity(
                            new Intent(
                                    getApplicationContext(),
                                    MainActivity.class
                            ).putExtra(
                                    Constant.STOP_NAME,
                                    stopName
                            ).setFlags(
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            ).setFlags(
                                    Intent.FLAG_ACTIVITY_NO_ANIMATION
                            )
                    );
                }
            });
        }

        Preferences.saveHasRunOnce(getApplicationContext(), TUTORIAL_FAVOURITES, true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        TextView textViewFavouritesNoneSelected = viewBinding.textviewFavouritesNoneSelected;
        textViewFavouritesNoneSelected.setVisibility(View.GONE);

        /*
         * Updates of the ListView must be performed on the UI thread.
         */
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (listFavouriteStops != null) {
                    /* ArrayAdapter for favourite stops. */
                    adapterFavouriteStops = new ArrayAdapter<>(
                            getApplicationContext(),
                            R.layout.listview_favourites,
                            listFavouriteStops
                    );

                    listFavouriteStops = getListFavouriteStops();

                    /*
                     * Populate ListView with the user's favourite stops, as read from file.
                     */
                    ListView listViewFavouriteStops = viewBinding.listviewFavouriteStops;

                    /*
                     * Update ArrayAdapter with newly-selected favourite stops, then set it to the
                     * ListView.
                     */
                    adapterFavouriteStops.clear();
                    adapterFavouriteStops.addAll(listFavouriteStops);
                    adapterFavouriteStops.notifyDataSetChanged();
                    listViewFavouriteStops.setAdapter(adapterFavouriteStops);
                }
            }
        });
    }

    private List<CharSequence> getListFavouriteStops() {
        final String FILE_FAVOURITES = "favourites";

        try {
            /*
             * Open input objects.
             */
            InputStream fileInput = openFileInput(FILE_FAVOURITES);
            InputStream buffer = new BufferedInputStream(fileInput);
            ObjectInput objectInput = new ObjectInputStream(buffer);

            /*
             * Read in List of favourite stops from file.
             */
            @SuppressWarnings("unchecked")
            List<CharSequence> listFavouriteStops = (List<CharSequence>) objectInput.readObject();

            /*
             * Close input objects.
             */
            objectInput.close();
            buffer.close();
            fileInput.close();

            return listFavouriteStops;
        } catch (ClassNotFoundException | FileNotFoundException e) {
            /*
             * If the favourites file doesn't exist, the user has probably not set up this
             * feature yet. Handle the exception gracefully by displaying a TextView with
             * instructions on how to add favourites.
             */
            Log.i(LOG_TAG, "Favourites not yet set up.");

            TextView textViewFavouritesNoneSelected = viewBinding.textviewFavouritesNoneSelected;
            textViewFavouritesNoneSelected.setVisibility(View.VISIBLE);
        } catch (IOException e) {
            /*
             * Something has gone wrong; the file may have been corrupted. Delete the file.
             */
            Log.e(LOG_TAG, Log.getStackTraceString(e));
            Log.i(LOG_TAG, "Deleting favourites file.");
            deleteFile(FILE_FAVOURITES);
        }

        /* Something has gone wrong. Return an empty ArrayList. */
        return new ArrayList<>();
    }
}
