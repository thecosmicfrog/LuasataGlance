package org.thecosmicfrog.luasataglance.activity;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.thecosmicfrog.luasataglance.R;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.List;

public class FavouritesActivity extends ActionBarActivity {

    private final String LOG_TAG = FavouritesSelectActivity.class.getSimpleName();

    private final String FILE_FAVOURITES = "favourites";

    private ArrayAdapter<CharSequence> adapterFavouriteStops;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourites);
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            /*
             * Start by making sure the TextView isn't displayed. This should only display
             * when a user has no favourites saved.
             */
            TextView textViewFavouritesNoneSelected
                    = (TextView) findViewById(R.id.textview_favourites_none_selected);
            textViewFavouritesNoneSelected.setVisibility(View.GONE);
            /*
             * Open the "favourites" file and read in the List object of favourite stops
             * contained within.
             */
            InputStream fileInput = openFileInput(FILE_FAVOURITES);
            InputStream buffer = new BufferedInputStream(fileInput);
            ObjectInput objectInput = new ObjectInputStream(buffer);

            List<CharSequence> listFavouriteStops = (List<CharSequence>) objectInput.readObject();

            // ArrayAdapter for favourite stops.
            adapterFavouriteStops = new ArrayAdapter<>(
                    getApplicationContext(),
                    R.layout.listview_favourites,
                    listFavouriteStops
            );

            /*
             * Populate ListView with the user's favourite stops, as read from file.
             */
            ListView listViewFavouriteStops = (ListView) findViewById(R.id.listview_favourite_stops);
            listViewFavouriteStops.setAdapter(adapterFavouriteStops);
            listViewFavouriteStops.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    /*
                     * When a favourite stop is clicked, open the MainActivity, passing the stop
                     * name as an extra parameter.
                     */
                    String stopName = adapterFavouriteStops.getItem(position).toString();

                    startActivity(new Intent(
                                    getApplicationContext(),
                                    MainActivity.class)
                                    .putExtra("stopName", stopName)
                    );
                }
            });
        } catch (ClassNotFoundException | FileNotFoundException fnfe) {
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_favourites, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_favourites_edit) {
            startActivity(new Intent(
                            getApplicationContext(),
                            FavouritesSelectActivity.class)
            );
        }

        return super.onOptionsItemSelected(item);
    }
}