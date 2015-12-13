/**
 * @author Aaron Hastings
 *
 * Copyright 2015 Aaron Hastings
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

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.util.Serializer;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FavouritesSelectActivity extends AppCompatActivity {

    private final String LOG_TAG = FavouritesSelectActivity.class.getSimpleName();
    private final String FILE_FAVOURITES = "favourites";

    private ArrayAdapter<String> adapterFavouriteStops;
    private SparseBooleanArray checkedItems;
    private List<CharSequence> selectedItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourites_select);

        getSupportActionBar().setBackgroundDrawable(
                new ColorDrawable(
                        ContextCompat.getColor(getApplication(), R.color.luas_purple)
                )
        );

        /*
         * Build arrays for Red Line and Green Line stops from resources, then create Lists
         * from those arrays. Finally, build a List of all stops by concatenating the first
         * two Lists.
         */
        String[] redLineArrayStops =
                getResources().getStringArray(R.array.array_stops_red_line);
        String[] greenLineArrayStops =
                getResources().getStringArray(R.array.array_stops_green_line);

        List<String> redLineListStops = Arrays.asList(redLineArrayStops);
        List<String> greenLineListStops = Arrays.asList(greenLineArrayStops);

        List<String> listAllStops = new ArrayList<>(redLineListStops);
        listAllStops.addAll(greenLineListStops);

        // Remove the two "Select a stop..." entries from the List.
        for (int i = 0; i < 2; i++) {
            listAllStops.remove(getResources().getString(R.string.select_a_stop));
        }

        // ArrayAdapter for favourite stops.
        adapterFavouriteStops = new ArrayAdapter<>(
                getApplicationContext(),
                R.layout.checkedtextview_stops,
                listAllStops
        );

        /*
         * Populate ListView with all stops on both lines.
         */
        final ListView listViewStops = (ListView) findViewById(R.id.listview_stops);
        listViewStops.setAdapter(adapterFavouriteStops);
        listViewStops.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /*
                 * When a list item is clicked, it is graphically "checked" and also added
                 * to a List of all currently selected stops.
                 */
                checkedItems = listViewStops.getCheckedItemPositions();
                selectedItems = new ArrayList<>();

                for (int i = 0; i < checkedItems.size(); i++) {
                    int pos = checkedItems.keyAt(i);

                    if (checkedItems.valueAt(i)) {
                        selectedItems.add(adapterFavouriteStops.getItem(pos));
                    }
                }
            }
        });

        /*
         * Use a Floating Action Button (FAB) to save the selected Favourites.
         */
        FloatingActionButton fabFavouritesSave =
                (FloatingActionButton) findViewById(R.id.fab_favourites_save);
        fabFavouritesSave.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.message_success))
        );
        fabFavouritesSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveFavourites();
            }
        });

        /*
         * Keep the FavouritesSelectActivity in sync with the favourites file by ensuring
         * all favourite stops are already checked in the ListView.
         */
        try {
            /*
             * Open the "favourites" file and read in the List object of favourite stops
             * contained within.
             */
            InputStream fileInput = openFileInput(FILE_FAVOURITES);
            InputStream buffer = new BufferedInputStream(fileInput);
            ObjectInput objectInput = new ObjectInputStream(buffer);

            @SuppressWarnings("unchecked")
            List<CharSequence> listFavouriteStops = (List<CharSequence>) objectInput.readObject();

            /*
             * Programmatically check the boxes of already-favourited stops.
             */
            for (int i = 0; i < listFavouriteStops.size(); i++) {
                if (listAllStops.contains(listFavouriteStops.get(i).toString())) {
                    listViewStops.setItemChecked(
                            listAllStops.indexOf(
                                    listFavouriteStops.get(i).toString()
                            ), true);
                }
            }
        } catch (FileNotFoundException e) {
            Log.i(LOG_TAG, "Favourites file doesn't exist.");
        } catch (ClassNotFoundException | IOException e) {
            Log.e(LOG_TAG, Log.getStackTraceString(e));
        }
    }

    private void saveFavourites() {
        try {
            if (selectedItems != null && !selectedItems.isEmpty()) {
                FileOutputStream file = openFileOutput(FILE_FAVOURITES, Context.MODE_PRIVATE);
                file.write(Serializer.serialize(selectedItems));

                file.close();
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, Log.getStackTraceString(e));
        }

        // We're finished here. Close the activity.
        finish();
    }
}
