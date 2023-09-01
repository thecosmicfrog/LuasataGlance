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

package org.thecosmicfrog.luasataglance.view;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import androidx.cardview.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.thecosmicfrog.luasataglance.R;

public class SpinnerCardView extends CardView {

    private final String LOG_TAG = SpinnerCardView.class.getSimpleName();

    private ArrayAdapter<CharSequence> adapterStops;
    private Spinner spinnerStops;

    public SpinnerCardView(Context context) {
        super(context);

        init(context);
    }

    public SpinnerCardView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public SpinnerCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    /**
     * Initialise custom View.
     * @param context Context.
     */
    public void init(Context context) {
        inflate(context, R.layout.cardview_spinner, this);

        spinnerStops = findViewById(R.id.card_view_spinner);

        /*
         * Set the Spinner's colour to Luas purple.
         */
        if (spinnerStops.getBackground().getConstantState() != null) {
            Drawable spinnerDrawable =
                    spinnerStops.getBackground().getConstantState().newDrawable();

            spinnerDrawable.setColorFilter(
                    ContextCompat.getColor(getContext(), R.color.luas_purple),
                    PorterDuff.Mode.SRC_ATOP
            );

            spinnerStops.setBackground(spinnerDrawable);
        }
    }

    /**
     * Initialise the ArrayAdapter for stops.
     * @param resArrayStops Resource ID for array of stops.
     */
    private void initAdapterStops(int resArrayStops) {
        adapterStops = ArrayAdapter.createFromResource(
                getContext(), resArrayStops, R.layout.spinner_stops
        );
        adapterStops.setDropDownViewResource(R.layout.spinner_stops);
        spinnerStops.setAdapter(adapterStops);
    }

    /**
     * Setter method which also triggers an initialisation of the ArrayAdapter for stops.
     * @param line Line to initialise.
     */
    public void setLine(String line) {
        final String RED_LINE = "red_line";
        final String GREEN_LINE = "green_line";

        int resArrayStops = 0;

        switch (line) {
            case RED_LINE:
                resArrayStops = R.array.array_stops_redline;

                break;

            case GREEN_LINE:
                resArrayStops = R.array.array_stops_greenline;

                break;

            default:
                /* If for some reason the line doesn't make sense. */
                Log.wtf(LOG_TAG, "Invalid line specified.");
        }

        initAdapterStops(resArrayStops);
    }

    public Spinner getSpinnerStops() {
        return spinnerStops;
    }

    public void setSelection(String stopName) {
        spinnerStops.setSelection(adapterStops.getPosition(stopName));
    }
}
