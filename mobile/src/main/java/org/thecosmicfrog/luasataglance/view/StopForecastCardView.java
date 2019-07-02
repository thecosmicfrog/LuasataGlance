/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2019 Aaron Hastings
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

package org.thecosmicfrog.luasataglance.view;

import android.content.Context;
import androidx.cardview.widget.CardView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.TableRow;
import android.widget.TextView;

import org.thecosmicfrog.luasataglance.R;

public class StopForecastCardView extends CardView {

    private final String LOG_TAG = SpinnerCardView.class.getSimpleName();

    private TextView textViewDirection;
    private TableRow[] tableRowStops;
    private TextView[] textViewStopNames;
    private TextView[] textViewStopTimes;

    public StopForecastCardView(Context context) {
        super(context);

        init(context);
    }

    public StopForecastCardView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public StopForecastCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    /**
     * Initialise custom View.
     * @param context Context.
     */
    public void init(Context context) {
        View view = inflate(context, R.layout.cardview_stop_forecast, this);

        textViewDirection = findViewById(R.id.textview_direction);

        tableRowStops = new TableRow[] {
                findViewById(R.id.tablerow_stop1),
                findViewById(R.id.tablerow_stop2),
                findViewById(R.id.tablerow_stop3),
                findViewById(R.id.tablerow_stop4),
                findViewById(R.id.tablerow_stop5)
        };

        textViewStopNames = new TextView[] {
                findViewById(R.id.textview_stop1_name),
                findViewById(R.id.textview_stop2_name),
                findViewById(R.id.textview_stop3_name),
                findViewById(R.id.textview_stop4_name),
                findViewById(R.id.textview_stop5_name)
        };

        textViewStopTimes = new TextView[] {
                findViewById(R.id.textview_stop1_time),
                findViewById(R.id.textview_stop2_time),
                findViewById(R.id.textview_stop3_time),
                findViewById(R.id.textview_stop4_time),
                findViewById(R.id.textview_stop5_time)
        };

        adjustTableRowsByScreenDensity(view);
    }

    /**
     * Adjust the number of TableRows based on the user's screen density in DPI.
     * @param view View.
     */
    private void adjustTableRowsByScreenDensity(View view) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int densityDpi = displayMetrics.densityDpi;

        Log.i(LOG_TAG, "Screen density is " + densityDpi + " DPI.");

        if (densityDpi > 320) {
            Log.i(LOG_TAG, "Making 4th TableRow visible.");

            TableRow tableRowStop4 = (TableRow) view.findViewById(R.id.tablerow_stop4);
            tableRowStop4.setVisibility(View.VISIBLE);
        }

        if (densityDpi > 500) {
            Log.i(LOG_TAG, "Making 5th TableRow visible.");

            TableRow tableRowStop5 = (TableRow) view.findViewById(R.id.tablerow_stop5);
            tableRowStop5.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Clear the stop forecast.
     */
    public void clearStopForecast() {
        for (int i = 0; i < 5; i++) {
            textViewStopNames[i].setText("");
            textViewStopTimes[i].setText("");

            textViewStopNames[i].setText("");
            textViewStopTimes[i].setText("");
        }
    }

    public TableRow[] getTableRowStops() {
        return tableRowStops;
    }

    public TextView[] getTextViewStopTimes() {
        return textViewStopTimes;
    }

    public void setStopForecastDirection(String direction) {
        textViewDirection.setText(direction);
    }

    public void setNoTramsForecast() {
        textViewStopNames[0].setText(R.string.no_trams_forecast);
    }

    public void setStopNames(int index, String tram) {
        textViewStopNames[index].setText(tram);
    }

    public void setStopTimes(int index, String dueMinutes) {
        textViewStopTimes[index].setText(dueMinutes);
    }
}
