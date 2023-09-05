/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2023 Aaron Hastings
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
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.activity.MainActivity;
import org.thecosmicfrog.luasataglance.service.WidgetListenerService;
import org.thecosmicfrog.luasataglance.util.Constant;
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
    private static final String WIDGET_CLICK_STOP_NAME = "WidgetClickStopName";
    private static final String WIDGET_CLICK_ARROW_LEFT = "WidgetClickArrowLeft";
    private static final String WIDGET_CLICK_ARROW_RIGHT = "WidgetClickArrowRight";
    private static final String WIDGET_CLICK_STOP_FORECAST = "WidgetClickStopForecast";

    private static long stopForecastLastClickTime = 0;

    private final int TEXTVIEW_TAP_TO_LOAD_TIMES = R.id.textview_tap_to_load_times;
    private final int TEXTVIEW_INBOUND_STOP1_NAME = R.id.textview_inbound_stop1_name;
    private final int TEXTVIEW_INBOUND_STOP1_TIME = R.id.textview_inbound_stop1_time;
    private final int TEXTVIEW_INBOUND_STOP2_NAME = R.id.textview_inbound_stop2_name;
    private final int TEXTVIEW_INBOUND_STOP2_TIME = R.id.textview_inbound_stop2_time;
    private final int TEXTVIEW_OUTBOUND_STOP1_NAME = R.id.textview_outbound_stop1_name;
    private final int TEXTVIEW_OUTBOUND_STOP1_TIME = R.id.textview_outbound_stop1_time;
    private final int TEXTVIEW_OUTBOUND_STOP2_NAME = R.id.textview_outbound_stop2_name;
    private final int TEXTVIEW_OUTBOUND_STOP2_TIME = R.id.textview_outbound_stop2_time;
    private final int STOP_FORECAST_TIMEOUT_MILLIS = 15000;

    private final int[] TEXTVIEW_INBOUND_STOP_NAMES = {
            TEXTVIEW_INBOUND_STOP1_NAME,
            TEXTVIEW_INBOUND_STOP2_NAME
    };

    private final int[] TEXTVIEW_INBOUND_STOP_TIMES = {
            TEXTVIEW_INBOUND_STOP1_TIME,
            TEXTVIEW_INBOUND_STOP2_TIME
    };

    private final int[] TEXTVIEW_OUTBOUND_STOP_NAMES = {
            TEXTVIEW_OUTBOUND_STOP1_NAME,
            TEXTVIEW_OUTBOUND_STOP2_NAME
    };

    private final int[] TEXTVIEW_OUTBOUND_STOP_TIMES = {
            TEXTVIEW_OUTBOUND_STOP1_TIME,
            TEXTVIEW_OUTBOUND_STOP2_TIME
    };

    private static RemoteViews remoteViews;
    private static Handler handlerClearStopForecastAfterTimeout;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        /* Construct a RemoteViews model. */
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.stop_forecast_widget);

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
        int[] appWidgetsIds = appWidgetManager.getAppWidgetIds(thisWidget);

        /* Construct a RemoteViews model. */
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.stop_forecast_widget);

        int indexNextStopToLoad = Preferences.indexNextStopToLoad(context);
        List listSelectedStops = loadListSelectedStops(context);

        if (listSelectedStops != null && intent.getAction() != null) {
            /*
             * If the user taps the stop name, open the app at that stop.
             */
            if (intent.getAction().equals(WIDGET_CLICK_STOP_NAME)) {
                String stopName = Preferences.widgetSelectedStopName(context);

                context.startActivity(
                        new Intent(
                                context,
                                MainActivity.class
                        ).addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                        ).putExtra(
                                Constant.STOP_NAME,
                                stopName
                        )
                );
            }

            /*
             * If the user taps one of the widget arrows, move to the next/previous stop.
             */
            if (intent.getAction().equals(WIDGET_CLICK_ARROW_LEFT)) {
                /*
                 * Move on to the previous index in the list. If we're on the first index, reset
                 * back to the last index [listSelectedStops.size() - 1].
                 */
                if (indexNextStopToLoad != 0)
                    indexNextStopToLoad--;
                else
                    indexNextStopToLoad = listSelectedStops.size() - 1;

                prepareLoadStopForecast(
                        context,
                        appWidgetManager,
                        appWidgetsIds,
                        remoteViews,
                        indexNextStopToLoad
                );
            }

            if (intent.getAction().equals(WIDGET_CLICK_ARROW_RIGHT)) {
                /*
                 * Move on to the next index in the list. If we're on the last index, reset back to
                 * the first index (0).
                 */
                if (indexNextStopToLoad != listSelectedStops.size() - 1)
                    indexNextStopToLoad++;
                else
                    indexNextStopToLoad = 0;

                prepareLoadStopForecast(
                        context,
                        appWidgetManager,
                        appWidgetsIds,
                        remoteViews,
                        indexNextStopToLoad
                );
            }

            /*
             * If the user taps the stop forecast display, load the forecast for that stop, setting
             * up a timeout as well.
             */
            if (intent.getAction().equals(WIDGET_CLICK_STOP_FORECAST)) {
                final int LOAD_LIMIT_MILLIS = 2000;

                /*
                 * Induce an artificial limit on number of allowed sequential clicks in order to
                 * prevent server hammering.
                 */
                if (SystemClock.elapsedRealtime() - stopForecastLastClickTime < LOAD_LIMIT_MILLIS)
                    return;

                stopForecastLastClickTime = SystemClock.elapsedRealtime();

                for (int appWidgetId : appWidgetsIds) {
                    stopForecastTimeout(
                            appWidgetManager,
                            appWidgetId,
                            remoteViews,
                            STOP_FORECAST_TIMEOUT_MILLIS
                    );
                }
                prepareLoadStopForecast(context,
                        appWidgetManager,
                        appWidgetsIds,
                        remoteViews,
                        indexNextStopToLoad
                );
            }
        }
    }

    /**
     * If we have a list of selected stops, get the next one we need to load, save it to local
     * storage, then fire up the service.
     * @param context Context.
     * @param appWidgetManager AppWidgetManager.
     * @param appWidgetsIds Array of all widget IDs.
     * @param remoteViews RemoteViews model to prepare loading of stop forecast for.
     * @param indexNextStopToLoad Index of next stop to load.
     */
    private void prepareLoadStopForecast(@NonNull Context context,
                                         AppWidgetManager appWidgetManager, int[] appWidgetsIds,
                                         RemoteViews remoteViews, int indexNextStopToLoad) {
        clearStopForecast(remoteViews);

        Preferences.saveIndexNextStopToLoad(context, indexNextStopToLoad);

        for (int appWidgetId : appWidgetsIds) {
            stopForecastTimeout(
                    appWidgetManager,
                    appWidgetId,
                    remoteViews,
                    STOP_FORECAST_TIMEOUT_MILLIS
            );

            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews);
        }

        List listSelectedStops = loadListSelectedStops(context);

        if (listSelectedStops != null) {
            String selectedStopName = listSelectedStops.get(indexNextStopToLoad).toString();
            Preferences.saveWidgetSelectedStopName(context, selectedStopName);

            startWidgetListenerService(context, appWidgetsIds);
        }
    }

    /**
     * Start WidgetListenerService, passing in the selected stop name.
     * @param context Context.
     * @param appWidgetsIds Array of all widget IDs.
     */
    private static void startWidgetListenerService(@NonNull Context context, int[] appWidgetsIds) {
        String selectedStopName = Preferences.widgetSelectedStopName(context);

        /*
         * Prepare an Intent to start the WidgetListenerService. Pass in all the widget IDs.
         */
        Intent intentWidgetListenerService =
                new Intent(context.getApplicationContext(), WidgetListenerService.class);
        intentWidgetListenerService.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intentWidgetListenerService.putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_IDS,
                appWidgetsIds
        );
        intentWidgetListenerService.putExtra(
                Constant.SELECTED_STOP_NAME,
                selectedStopName
        );

        /* Start the WidgetListenerService. */
        context.startForegroundService(intentWidgetListenerService);
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
             * Open the "widget_selected_stops" file and read in the List model of selected stops
             * contained within.
             */
            InputStream fileInput = context.openFileInput(FILE_WIDGET_SELECTED_STOPS);
            InputStream buffer = new BufferedInputStream(fileInput);
            ObjectInput objectInput = new ObjectInputStream(buffer);

            @SuppressWarnings("unchecked")
            List<CharSequence> listSelectedStops = (List<CharSequence>) objectInput.readObject();

            /* Close files and streams. */
            objectInput.close();
            buffer.close();
            fileInput.close();

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

    /**
     * Wrapper around updateAppWidget().
     * @param context Context.
     * @param appWidgetManager AppWidgetManager.
     * @param appWidgetId ID of widget to update.
     */
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        /*
         * Set up Intents to register taps on the widget.
         */
        Intent intentWidgetClickStopName = new Intent(context, StopForecastWidget.class);
        Intent intentWidgetClickArrowLeft = new Intent(context, StopForecastWidget.class);
        Intent intentWidgetClickArrowRight = new Intent(context, StopForecastWidget.class);
        Intent intentWidgetClickStopForecast = new Intent(context, StopForecastWidget.class);

        intentWidgetClickStopName.setAction(WIDGET_CLICK_STOP_NAME);
        intentWidgetClickArrowLeft.setAction(WIDGET_CLICK_ARROW_LEFT);
        intentWidgetClickArrowRight.setAction(WIDGET_CLICK_ARROW_RIGHT);
        intentWidgetClickStopForecast.setAction(WIDGET_CLICK_STOP_FORECAST);

        PendingIntent pendingIntentWidgetClickStopName =
                PendingIntent.getBroadcast(context, 0, intentWidgetClickStopName, PendingIntent.FLAG_IMMUTABLE);
        PendingIntent pendingIntentWidgetClickArrowLeft =
                PendingIntent.getBroadcast(context, 0, intentWidgetClickArrowLeft, PendingIntent.FLAG_IMMUTABLE);
        PendingIntent pendingIntentWidgetClickArrowRight =
                PendingIntent.getBroadcast(context, 0, intentWidgetClickArrowRight, PendingIntent.FLAG_IMMUTABLE);
        PendingIntent pendingIntentWidgetClickStopForecast =
                PendingIntent.getBroadcast(context, 0, intentWidgetClickStopForecast, PendingIntent.FLAG_IMMUTABLE);

        remoteViews.setOnClickPendingIntent(
                R.id.textview_stop_name, pendingIntentWidgetClickStopName
        );
        remoteViews.setOnClickPendingIntent(
                R.id.textview_stop_name_left_arrow, pendingIntentWidgetClickArrowLeft
        );
        remoteViews.setOnClickPendingIntent(
                R.id.textview_stop_name_right_arrow, pendingIntentWidgetClickArrowRight
        );
        remoteViews.setOnClickPendingIntent(
                R.id.linearlayout_stop_forecast, pendingIntentWidgetClickStopForecast
        );

        /* Instruct the widget manager to update the widget. */
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    /**
     * Clear the stop forecast currently displayed in the widget.
     * @param remoteViewToClear RemoteViews model to clear.
     */
    private void clearStopForecast(RemoteViews remoteViewToClear) {
        for (int i = 0; i < 2; i++) {
            remoteViewToClear.setTextViewText(TEXTVIEW_INBOUND_STOP_NAMES[i], "");
            remoteViewToClear.setTextViewText(TEXTVIEW_INBOUND_STOP_TIMES[i], "");

            remoteViewToClear.setTextViewText(TEXTVIEW_OUTBOUND_STOP_NAMES[i], "");
            remoteViewToClear.setTextViewText(TEXTVIEW_OUTBOUND_STOP_TIMES[i], "");
        }
    }

    /**
     * Set up timeout for stop forecast, which clears the stop forecast and displays a holding
     * message after a set period.
     * This is a necessary evil due to their currently being no way for a widget to know when it
     * is "active" or "visible to the user". This implementation is in order to not hammer the
     * battery and network.
     * @param appWidgetManager AppWidgetManager.
     * @param appWidgetId App widget ID.
     * @param remoteViews RemoteViews.
     * @param timeoutTimeMillis The period (ms) after which the stop forecast should be considered
     *                          expired and cleared.
     */
    private void stopForecastTimeout(
            final AppWidgetManager appWidgetManager,
            final int appWidgetId,
            final RemoteViews remoteViews,
            int timeoutTimeMillis) {
        /*
         * Create a Runnable to execute the clearStopForecast() method after a set delay.
         */
        Runnable runnableClearStopForecastAfterTimeout = new Runnable() {
            @Override
            public void run() {
                clearStopForecast(remoteViews);

                remoteViews.setViewVisibility(
                        TEXTVIEW_TAP_TO_LOAD_TIMES,
                        View.VISIBLE
                );

                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews);
            }
        };

        /*
         * If a Handler already exists, remove its callbacks and messages so we don't have multiple
         * timeouts firing.
         */
        if (handlerClearStopForecastAfterTimeout != null) {
            handlerClearStopForecastAfterTimeout.removeCallbacksAndMessages(null);
        }

        /* Wait for a set delay, then trigger the clearing of the stop forecast via the Runnable. */
        handlerClearStopForecastAfterTimeout = new Handler();
        handlerClearStopForecastAfterTimeout.postDelayed(
                runnableClearStopForecastAfterTimeout,
                timeoutTimeMillis
        );
    }
}
