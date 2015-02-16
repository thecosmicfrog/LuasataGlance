package org.thecosmicfrog.luasataglance.activity;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.util.Serializer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class FavouritesSelectActivity extends ActionBarActivity {

    private final String LOG_TAG = FavouritesSelectActivity.class.getSimpleName();

    private final String FILE_FAVOURITES = "favourites";

    private ArrayAdapter<String> adapterFavouriteStops;
    private SparseBooleanArray checkedItems;
    private List<String> selectedItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourites_select);

        /*
         * Build arrays for Red Line and Green Line stops from resources, then create Lists
         * from those arrays. Finally, build a List of all stops by concatenating the first
         * two Lists.
         */
        String[] redLineArrayStops = getResources().getStringArray(R.array.red_line_array_stops);
        String[] greenLineArrayStops = getResources().getStringArray(R.array.green_line_array_stops);

        List<String> redLineListStops = Arrays.asList(redLineArrayStops);
        List<String> greenLineListStops = Arrays.asList(greenLineArrayStops);

        List<String> listAllStops = new ArrayList<>(redLineListStops);
        listAllStops.addAll(greenLineListStops);

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_favourites_select, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        /*
         * When the Save button is clicked, serialize the List of selected stops and write the
         * resulting object to a file.
         */
        if (id == R.id.action_favourites_save) {
            try {
                if (selectedItems != null && !selectedItems.isEmpty()) {
                    FileOutputStream file = openFileOutput(FILE_FAVOURITES, Context.MODE_PRIVATE);
                    file.write(Serializer.serialize(selectedItems));

                    file.close();
                }
            } catch (IOException ioe) {
                Log.e(LOG_TAG, ioe.getMessage());
            }

            // We're finished here. Close the activity.
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
