/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2016 Aaron Hastings
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
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.RemoteViews;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.activity.MainActivity;
import org.thecosmicfrog.luasataglance.service.WidgetListenerService;
import org.thecosmicfrog.luasataglance.util.Preferences;

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
    private static final String STOP_NAME = "stopName";
    private static final String SELECTED_STOP_NAME = "selectedStopName";
    private static final String WIDGET_CLICK_STOP_NAME = "WidgetClickStopName";
    private static final String WIDGET_CLICK_LEFT_ARROW = "WidgetClickLeftArrow";
    private static final String WIDGET_CLICK_RIGHT_ARROW = "WidgetClickRightArrow";
    private static final String WIDGET_CLICK_STOP_FORECAST = "WidgetClickStopForecast";

    private static long stopForecastLastClickTime = 0;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        /* There may be multiple widgets active, so update all of them. */
        for (int appWidgetId : appWidgetIds) {
            Log.i(LOG_TAG, "Widget updating with ID: " + appWidgetId);

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

        int indexNextStopToLoad = Preferences.indexNextStopToLoad(context);
        List listSelectedStops = loadListSelectedStops(context);

        if (listSelectedStops != null) {
            /*
             * If the user taps the stop name, open the app at that stop.
             */
            if (intent.getAction().equals(WIDGET_CLICK_STOP_NAME)) {
                String stopName = Preferences.widgetSelectedStopName(context);

                context.startActivity(
                        new Intent(
                                context,
                                MainActivity.class
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra(STOP_NAME, stopName)
                );
            }

            /*
             * If the user taps one of the widget arrows, move to the next/previous stop.
             */
            if (intent.getAction().equals(WIDGET_CLICK_LEFT_ARROW)) {
                /*
                 * Move on to the previous index in the list. If we're on the first index, reset
                 * back to the last index [listSelectedStops.size() - 1].
                 */
                if (indexNextStopToLoad != 0)
                    indexNextStopToLoad--;
                else
                    indexNextStopToLoad = listSelectedStops.size() - 1;

                Preferences.saveIndexNextStopToLoad(context, indexNextStopToLoad);

                prepareLoadStopForecast(context, allWidgetIds);
            }

            if (intent.getAction().equals(WIDGET_CLICK_RIGHT_ARROW)) {
                /*
                 * Move on to the next index in the list. If we're on the last index, reset back to
                 * the first index (0).
                 */
                if (indexNextStopToLoad != listSelectedStops.size() - 1)
                    indexNextStopToLoad++;
                else
                    indexNextStopToLoad = 0;

                Preferences.saveIndexNextStopToLoad(context, indexNextStopToLoad);

                prepareLoadStopForecast(context, allWidgetIds);
            }

            /*
             * If the user taps the stop forecast display, load the forecast for that stop, setting
             * up a timeout as well.
             */
            if (intent.getAction().equals(WIDGET_CLICK_STOP_FORECAST)) {
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
    private static void prepareLoadStopForecast(@NonNull Context context, int[] allWidgetIds) {
        List listSelectedStops = loadListSelectedStops(context);

        if (listSelectedStops != null) {
            int indexNextStopToLoad = Preferences.indexNextStopToLoad(context);

            String selectedStopName = listSelectedStops.get(indexNextStopToLoad).toString();
            Preferences.saveWidgetSelectedStopName(context, selectedStopName);

            startWidgetListenerService(context, allWidgetIds);
        }
    }

    /**
     * Start WidgetListenerService, passing in the selected stop name.
     * @param context Context.
     * @param allWidgetIds Array of all widget IDs.
     */
    private static void startWidgetListenerService(@NonNull Context context, int[] allWidgetIds) {
        String selectedStopName = Preferences.widgetSelectedStopName(context);

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
                SELECTED_STOP_NAME,
                selectedStopName
        );

        /* Start the WidgetListenerService. */
        context.startService(intentWidgetListenerService);
    }



    /**
     * Load list of user-selected stops from file.
     * @param context Context.
     * @return List of user-selected stops.
     */
    private static List<CharSequence> loadListSelectedStops(Context context) {
        final String FILE_WIDGET_SELECTED_STOPS = "widget_selected_stops";

        try {
            /*
             * Open the "widget_selected_stops" file and read in the List object of selected stops
             * contained within.
             */
            InputStream fileInput = context.openFileInput(FILE_WIDGET_SELECTED_STOPS);
            InputStream buffer = new BufferedInputStream(fileInput);
            ObjectInput objectInput = new ObjectInputStream(buffer);

            @SuppressWarnings("unchecked")
            List<CharSequence> listSelectedStops = (List<CharSequence>) objectInput.readObject();

            return listSelectedStops;
        } catch (ClassNotFoundException | FileNotFoundException e) {
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
        /* Construct the RemoteViews object. */
        RemoteViews views =
                new RemoteViews(context.getPackageName(), R.layout.stop_forecast_widget);

        /*
         * Set up Intents to register taps on the widget.
         */
        Intent intentWidgetClickStopName = new Intent(context, StopForecastWidget.class);
        Intent intentWidgetClickLeftArrow = new Intent(context, StopForecastWidget.class);
        Intent intentWidgetClickRightArrow = new Intent(context, StopForecastWidget.class);
        Intent intentWidgetClickStopForecast = new Intent(context, StopForecastWidget.class);

        intentWidgetClickStopName.setAction(WIDGET_CLICK_STOP_NAME);
        intentWidgetClickLeftArrow.setAction(WIDGET_CLICK_LEFT_ARROW);
        intentWidgetClickRightArrow.setAction(WIDGET_CLICK_RIGHT_ARROW);
        intentWidgetClickStopForecast.setAction(WIDGET_CLICK_STOP_FORECAST);

        PendingIntent pendingIntentWidgetClickStopName =
                PendingIntent.getBroadcast(context, 0, intentWidgetClickStopName, 0);
        PendingIntent pendingIntentWidgetClickLeftArrow =
                PendingIntent.getBroadcast(context, 0, intentWidgetClickLeftArrow, 0);
        PendingIntent pendingIntentWidgetClickRightArrow =
                PendingIntent.getBroadcast(context, 0, intentWidgetClickRightArrow, 0);
        PendingIntent pendingIntentWidgetClickStopForecast =
                PendingIntent.getBroadcast(context, 0, intentWidgetClickStopForecast, 0);

        views.setOnClickPendingIntent(
                R.id.textview_stop_name, pendingIntentWidgetClickStopName
        );
        views.setOnClickPendingIntent(
                R.id.textview_stop_name_left_arrow, pendingIntentWidgetClickLeftArrow
        );
        views.setOnClickPendingIntent(
                R.id.textview_stop_name_right_arrow, pendingIntentWidgetClickRightArrow
        );
        views.setOnClickPendingIntent(
                R.id.linearlayout_stop_forecast, pendingIntentWidgetClickStopForecast
        );

        /* Instruct the widget manager to update the widget. */
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
