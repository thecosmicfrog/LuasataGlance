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

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.adapter.FavouritesSelectAdapter;
import org.thecosmicfrog.luasataglance.databinding.ActivityFavouritesSelectBinding;
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

    private ActivityFavouritesSelectBinding viewBinding;
    private FavouritesSelectAdapter adapter;
    private ArrayList<CharSequence> selectedStops;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewBinding = ActivityFavouritesSelectBinding.inflate(getLayoutInflater());
        View rootView = viewBinding.getRoot();
        setContentView(rootView);

        getSupportActionBar().setBackgroundDrawable(
                new ColorDrawable(ContextCompat.getColor(getApplication(), R.color.luas_purple))
        );

        selectedStops = new ArrayList<>();
        ArrayList<CharSequence> listAllStops = loadAllStops();

        initRecyclerView(listAllStops);
        initFab();
        loadExistingfavourites();
    }

    /**
     * Load the list of stops from resources.
     * @return List of all stops across all lines.
     */
    private ArrayList<CharSequence> loadAllStops() {
        String[] allStops = getResources().getStringArray(R.array.array_stops_all);
        ArrayList<CharSequence> listAllStops = new ArrayList<>();

        /* Skip the first element ("None" at index 0) and add the rest. */
        for (int i = 1; i < allStops.length; i++) {
            if (!allStops[i].equals(getString(R.string.select_a_stop))) {
                listAllStops.add(allStops[i]);
            }
        }

        return listAllStops;
    }

    /**
     * Initialise the RecyclerView with the list of stops.
     * @param stops The list of stops to display.
     */
    private void initRecyclerView(ArrayList<CharSequence> stops) {
        RecyclerView recyclerView = viewBinding.recyclerviewStops;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FavouritesSelectAdapter(stops, selectedStops);
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    /**
     * Initialise the edit FAB.
     */
    private void initFab() {
        FloatingActionButton fabFavouritesSave = viewBinding.fabFavouritesSave;
        fabFavouritesSave.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.message_success))
        );
        fabFavouritesSave.setOnClickListener(v -> saveFavourites());
    }

    /**
     * Load the existing favourites from the file on disk.
     */
    private void loadExistingfavourites() {
        try {
            InputStream fileInput = openFileInput(FILE_FAVOURITES);
            InputStream buffer = new BufferedInputStream(fileInput);
            ObjectInput objectInput = new ObjectInputStream(buffer);

            @SuppressWarnings("unchecked")
            List<CharSequence> listFavouriteStops = (List<CharSequence>) objectInput.readObject();

            selectedStops.addAll(listFavouriteStops);
            adapter.notifyDataSetChanged();

            objectInput.close();
            buffer.close();
            fileInput.close();
        } catch (FileNotFoundException e) {
            Log.i(LOG_TAG, "Favourites file doesn't exist.");
        } catch (ClassNotFoundException | IOException e) {
            Log.e(LOG_TAG, Log.getStackTraceString(e));
        }
    }

    /**
     * Save the selected stops to the favourites file on disk.
     */
    private void saveFavourites() {
        try {
            if (!selectedStops.isEmpty()) {
                String[] allStops = getResources().getStringArray(R.array.array_stops_all);

                /* Sorting magic to ensure stops are saved in the order they appear on the map. */
                selectedStops.sort((stop1, stop2) -> {
                    int indexStop1 = Arrays.asList(allStops).indexOf(stop1.toString());
                    int indexStop2 = Arrays.asList(allStops).indexOf(stop2.toString());
                    return Integer.compare(indexStop1, indexStop2);
                });

                FileOutputStream file = openFileOutput(FILE_FAVOURITES, Context.MODE_PRIVATE);
                file.write(Serializer.serialize(selectedStops));
                file.close();
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, Log.getStackTraceString(e));
        }

        finish();
    }
}
