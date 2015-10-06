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

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import org.thecosmicfrog.luasataglance.R;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

public class FavouritesDialog extends DialogFragment {

    private final String LOG_TAG = NotifyTimeDialog.class.getSimpleName();

    private ArrayAdapter<CharSequence> adapterFavouriteStops;
    private List<CharSequence> listFavouriteStops;
    private View rootView;

    public FavouritesDialog() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        rootView = inflater.inflate(R.layout.dialog_favourites, null);

        ImageButton buttonEdit = (ImageButton) rootView.findViewById(R.id.imagebutton_edit);
        buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContext().startActivity(new Intent(
                                getContext(),
                                FavouritesSelectActivity.class)
                );
            }
        });

        listFavouriteStops = getListFavouriteStops();

        // ArrayAdapter for favourite stops.
        adapterFavouriteStops = new ArrayAdapter<>(
                getContext(),
                R.layout.listview_favourites,
                listFavouriteStops
        );

        /*
         * Populate ListView with the user's favourite stops, as read from file.
         */
        ListView listViewFavouriteStops = (ListView) rootView.findViewById(
                R.id.listview_favourite_stops
        );

        if (listFavouriteStops != null) {
            listViewFavouriteStops.setAdapter(adapterFavouriteStops);
            listViewFavouriteStops.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    /*
                     * When a favourite stop is clicked, open the MainActivity, passing the stop
                     * name as an extra parameter.
                     */
                    String stopName = adapterFavouriteStops.getItem(position).toString();

                    getContext().startActivity(
                            new Intent(
                                    getContext(),
                                    MainActivity.class
                            ).putExtra("stopName", stopName)
                    );
                }
            });
        }

        builder.setView(rootView);

        return builder.create();
    }

    @Override
    public void onResume() {
        super.onResume();

        TextView textViewFavouritesNoneSelected
                = (TextView) rootView.findViewById(R.id.textview_favourites_none_selected);
        textViewFavouritesNoneSelected.setVisibility(View.GONE);

        /*
         * Updates of the ListView must be performed on the UI thread.
         */
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (listFavouriteStops != null) {
                    // ArrayAdapter for favourite stops.
                    adapterFavouriteStops = new ArrayAdapter<>(
                            getContext(),
                            R.layout.listview_favourites,
                            listFavouriteStops
                    );

                    listFavouriteStops = getListFavouriteStops();

                    /*
                     * Populate ListView with the user's favourite stops, as read from file.
                     */
                    ListView listViewFavouriteStops = (ListView) rootView.findViewById(
                            R.id.listview_favourite_stops
                    );

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
            InputStream fileInput = getContext().openFileInput(FILE_FAVOURITES);
            InputStream buffer = new BufferedInputStream(fileInput);
            ObjectInput objectInput = new ObjectInputStream(buffer);

            /*
             * Read in List of favourite stops from file.
             */
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

            TextView textViewFavouritesNoneSelected
                    = (TextView) rootView.findViewById(R.id.textview_favourites_none_selected);
            textViewFavouritesNoneSelected.setVisibility(View.VISIBLE);
        } catch (IOException e) {
            /*
             * Something has gone wrong; the file may have been corrupted. Delete the file.
             */
            Log.e(LOG_TAG, Log.getStackTraceString(e));
            Log.i(LOG_TAG, "Deleting favourites file.");
            getContext().deleteFile(FILE_FAVOURITES);
        }

        // Something has gone wrong. Return an empty ArrayList.
        return new ArrayList<>();
    }
}
