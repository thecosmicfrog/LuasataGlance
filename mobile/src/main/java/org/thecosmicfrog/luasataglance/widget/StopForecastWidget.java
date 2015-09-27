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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.RemoteViews;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.service.WidgetListenerService;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.List;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in
 * {@link StopForecastWidgetConfigureActivity StopForecastWidgetConfigureActivity}
 */
public class StopForecastWidget extends AppWidgetProvider {

    private static final String LOG_TAG = StopForecastWidget.class.getSimpleName();

    private static long stopForecastLastClickTime = 0;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them.
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            Log.i(LOG_TAG, "Widget deleted with ID: " + appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        Log.i(LOG_TAG, "Widget first created.");
    }

    @Override
    public void onDisabled(Context context) {
        Log.i(LOG_TAG, "Widget disabled.");
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        super.onReceive(context, intent);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, StopForecastWidget.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        int indexNextStopToLoad = loadIndexNextStopToLoad(context);
        List listSelectedStops = loadListSelectedStops(context);

        if (listSelectedStops != null) {
            /*
             * If a user taps one of the widget arrows, move to the next/previous stop.
             */
            if (intent.getAction().equals("WidgetClickLeftArrow")) {
                /*
                 * Move on to the previous index in the list. If we're on the first index, reset
                 * back to the last index [listSelectedStops.size() - 1].
                 */
                if (indexNextStopToLoad != 0)
                    indexNextStopToLoad--;
                else
                    indexNextStopToLoad = listSelectedStops.size() - 1;

                saveIndexNextStopToLoad(context, indexNextStopToLoad);

                prepareLoadStopForecast(context, allWidgetIds);
            }

            if (intent.getAction().equals("WidgetClickRightArrow")) {
                /*
                 * Move on to the next index in the list. If we're on the last index, reset back to
                 * the first index (0).
                 */
                if (indexNextStopToLoad != listSelectedStops.size() - 1)
                    indexNextStopToLoad++;
                else
                    indexNextStopToLoad = 0;

                saveIndexNextStopToLoad(context, indexNextStopToLoad);

                prepareLoadStopForecast(context, allWidgetIds);
            }

            /*
             * If the user taps the stop forecast display, load the forecast for that stop, setting
             * up a timeout as well.
             */
            if (intent.getAction().equals("WidgetClickStopForecast")) {
                final int LOAD_LIMIT_MILLIS = 4000;

                /*
                 * Induce an artificial limit on number of allowed sequential clicks in order to
                 * prevent server hammering.
                 */
                if (SystemClock.elapsedRealtime() - stopForecastLastClickTime < LOAD_LIMIT_MILLIS)
                    return;

                stopForecastLastClickTime = SystemClock.elapsedRealtime();

                prepareLoadStopForecast(context, allWidgetIds);
            }
        }
    }

    /**
     * If we have a list of selected stops, get the next one we need to load, save it to local
     * storage, then fire up the service.
     * @param context Context.
     * @param allWidgetIds Array of all widget IDs.
     */
    static void prepareLoadStopForecast(@NonNull Context context, int[] allWidgetIds) {
        List listSelectedStops = loadListSelectedStops(context);

        if (listSelectedStops != null) {
            int indexNextStopToLoad = loadIndexNextStopToLoad(context);

            String selectedStopName = listSelectedStops.get(indexNextStopToLoad).toString();
            saveSelectedStopName(context, selectedStopName);

            startWidgetListenerService(context, allWidgetIds);
        }
    }

    static void startWidgetListenerService(@NonNull Context context, int[] allWidgetIds) {
        String selectedStopName = loadSelectedStopName(context);

        /*
         * Prepare an Intent to start the WidgetListenerService. Pass in all the widget IDs.
         */
        Intent intentWidgetListenerService =
                new Intent(context.getApplicationContext(), WidgetListenerService.class);
        intentWidgetListenerService.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intentWidgetListenerService.putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_IDS,
                allWidgetIds
        );
        intentWidgetListenerService.putExtra(
                "selectedStopName",
                selectedStopName
        );

        // Start the WidgetListenerService.
        context.startService(intentWidgetListenerService);
    }

    /**
     * Load the currently-selected stop name from shared preferences.
     * @param context Context
     * @return Selected stop name, or null if none found.
     */
    static String loadSelectedStopName(Context context) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance.StopForecastWidget";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        return prefs.getString("selectedStopName", null);
    }

    /**
     * Save the currently-selected stop name to shared preferences.
     * @param context Context.
     * @param selectedStopName Name of the stop to save to shared preferences.
     * @return Successfully saved.
     */
    static boolean saveSelectedStopName(Context context, String selectedStopName) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance.StopForecastWidget";

        SharedPreferences.Editor prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();

        prefs.putString("selectedStopName", selectedStopName);

        return prefs.commit();
    }

    /**
     * Load index of the next stop to load from shared preferences.
     * @param context Context
     * @return Index of the next stop to load, or 0 (first list entry) if none found.
     */
    static int loadIndexNextStopToLoad(Context context) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance.StopForecastWidget";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        return prefs.getInt("indexNextStopToLoad", 0);
    }

    /**
     * Save the currently-selected stop name to shared preferences.
     * @param context Context.
     * @param indexNextStopToLoad Index of the next stop to load to save to shared preferences.
     * @return Successfully saved.
     */
    static boolean saveIndexNextStopToLoad(Context context, int indexNextStopToLoad) {
        final String PREFS_NAME = "org.thecosmicfrog.luasataglance.StopForecastWidget";

        SharedPreferences.Editor prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();

        prefs.putInt("indexNextStopToLoad", indexNextStopToLoad);

        return prefs.commit();
    }

    /**
     * Load list of user-selected stops from file.
     * @param context Context.
     * @return List of user-selected stops.
     */
    static List<CharSequence> loadListSelectedStops(Context context) {
        final String FILE_WIDGET_SELECTED_STOPS = "widget_selected_stops";

        try {
            /*
             * Open the "widget_selected_stops" file and read in the List object of selected stops
             * contained within.
             */
            InputStream fileInput = context.openFileInput(FILE_WIDGET_SELECTED_STOPS);
            InputStream buffer = new BufferedInputStream(fileInput);
            ObjectInput objectInput = new ObjectInputStream(buffer);

            //noinspection unchecked
            List listSelectedStops = (List<CharSequence>) objectInput.readObject();

            return listSelectedStops;
        } catch (ClassNotFoundException | FileNotFoundException fnfe) {
            /*
             * If the favourites file doesn't exist, the user has probably not set up this
             * feature yet. Handle the exception gracefully by displaying a TextView with
             * instructions on how to add favourites.
             */
            Log.i(LOG_TAG, "Widget selected stops not yet set up.");
        } catch (IOException e) {
            /*
             * Something has gone wrong; the file may have been corrupted. Delete the file.
             */
            Log.e(LOG_TAG, Log.getStackTraceString(e));
            Log.i(LOG_TAG, "Deleting widget selected stops file.");
            context.deleteFile(FILE_WIDGET_SELECTED_STOPS);
        }

        return null;
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        // Construct the RemoteViews object.
        RemoteViews views =
                new RemoteViews(context.getPackageName(), R.layout.stop_forecast_widget);

        /*
         * Set up Intents to register taps on the widget.
         */
        Intent intentWidgetClickLeftArrow = new Intent(context, StopForecastWidget.class);
        Intent intentWidgetClickRightArrow = new Intent(context, StopForecastWidget.class);
        Intent intentWidgetClickStopForecast = new Intent(context, StopForecastWidget.class);

        intentWidgetClickLeftArrow.setAction("WidgetClickLeftArrow");
        intentWidgetClickRightArrow.setAction("WidgetClickRightArrow");
        intentWidgetClickStopForecast.setAction("WidgetClickStopForecast");

        PendingIntent pendingIntentWidgetClickLeftArrow =
                PendingIntent.getBroadcast(context, 0, intentWidgetClickLeftArrow, 0);
        PendingIntent pendingIntentWidgetClickRightArrow =
                PendingIntent.getBroadcast(context, 0, intentWidgetClickRightArrow, 0);
        PendingIntent pendingIntentWidgetClickStopForecast =
                PendingIntent.getBroadcast(context, 0, intentWidgetClickStopForecast, 0);

        views.setOnClickPendingIntent(
                R.id.textview_stop_name_left_arrow, pendingIntentWidgetClickLeftArrow
        );
        views.setOnClickPendingIntent(
                R.id.textview_stop_name_right_arrow, pendingIntentWidgetClickRightArrow
        );
        views.setOnClickPendingIntent(
                R.id.linearlayout_stop_forecast, pendingIntentWidgetClickStopForecast
        );

        // Instruct the widget manager to update the widget.
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views);
    }
}
