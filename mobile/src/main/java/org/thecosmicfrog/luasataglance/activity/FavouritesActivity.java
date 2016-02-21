package org.thecosmicfrog.luasataglance.activity;

import android.content.Intent;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
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

public class FavouritesActivity extends FragmentActivity {

    private final String LOG_TAG = FavouritesActivity.class.getSimpleName();
    private final String STOP_NAME = "stopName";

    private ArrayAdapter<CharSequence> adapterFavouriteStops;
    private List<CharSequence> listFavouriteStops;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*
         * If the user is on Lollipop or above, use a Material Dialog theme. Otherwise, fall back to
         * the default theme set in AndroidManifest.xml.
         */
        if (Build.VERSION.SDK_INT >= 21)
            setTheme(android.R.style.Theme_Material_Dialog);

        /* This is a Dialog. Get rid of the default Window title. */
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_favourites);

        ImageButton buttonEdit = (ImageButton) findViewById(R.id.imagebutton_edit);
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
        ListView listViewFavouriteStops = (ListView) findViewById(
                R.id.listview_favourite_stops
        );

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
                                    STOP_NAME,
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
    }

    @Override
    protected void onResume() {
        super.onResume();

        TextView textViewFavouritesNoneSelected
                = (TextView) findViewById(R.id.textview_favourites_none_selected);
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
                    ListView listViewFavouriteStops = (ListView) findViewById(
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

            TextView textViewFavouritesNoneSelected
                    = (TextView) findViewById(R.id.textview_favourites_none_selected);
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
