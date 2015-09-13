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

package org.thecosmicfrog.luasataglance.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
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

/**
 * The configuration screen for the {@link StopForecastWidget StopForecastWidget} AppWidget.
 */
public class StopForecastWidgetConfigureActivity extends ActionBarActivity {

    private final String LOG_TAG = StopForecastWidgetConfigureActivity.class.getSimpleName();

    private final String FILE_WIDGET_SELECTED_STOPS = "widget_selected_stops";

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private static final String PREFS_NAME =
            "org.thecosmicfrog.luasataglance.widget.StopForecastWidget";
    private static final String PREF_PREFIX_KEY = "appwidget_";

    private ArrayAdapter<String> adapterSelectedStops;
    private SparseBooleanArray checkedItems;
    private List<CharSequence> selectedItems;

    public StopForecastWidgetConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        getSupportActionBar().setBackgroundDrawable(
                new ColorDrawable(
                        getResources().getColor(R.color.luas_purple)
                )
        );

        /*
         * Set the result to CANCELED.  This will cause the widget host to cancel
         * out of the widget placement if the user presses the back button.
         */
        setResult(RESULT_CANCELED);

        setContentView(R.layout.stop_forecast_widget_configure);

        /*
         * Build arrays for Red Line and Green Line stops from resources, then create Lists
         * from those arrays. Finally, build a List of all stops by concatenating the first
         * two Lists.
         */
        String[] redLineArrayStops = getResources().getStringArray(R.array.red_line_array_stops);
        String[] greenLineArrayStops = getResources().getStringArray(
                R.array.green_line_array_stops
        );

        List<String> redLineListStops = Arrays.asList(redLineArrayStops);
        List<String> greenLineListStops = Arrays.asList(greenLineArrayStops);

        List<String> listAllStops = new ArrayList<>(redLineListStops);
        listAllStops.addAll(greenLineListStops);

        // ArrayAdapter for favourite stops.
        adapterSelectedStops = new ArrayAdapter<>(
                getApplicationContext(),
                R.layout.checkedtextview_stops,
                listAllStops
        );

        /*
         * Populate ListView with all stops on both lines.
         */
        final ListView listViewStops = (ListView) findViewById(R.id.listview_stops);
        listViewStops.setAdapter(adapterSelectedStops);
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
                        selectedItems.add(adapterSelectedStops.getItem(pos));
                    }
                }
            }
        });

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an
        // error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // Re-use the menu from the FavouritesSelectActivity.
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
                    FileOutputStream file = openFileOutput(
                            FILE_WIDGET_SELECTED_STOPS,
                            Context.MODE_PRIVATE
                    );

                    file.write(Serializer.serialize(selectedItems));

                    file.close();
                }
            } catch (IOException ioe) {
                Log.e(LOG_TAG, Log.getStackTraceString(ioe));
            }

            final Context context = StopForecastWidgetConfigureActivity.this;

            // It is the responsibility of the configuration activity to update the app widget.
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            StopForecastWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);

            // Make sure we pass back the original appWidgetId.
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);

            // We're finished here. Close the activity.
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
