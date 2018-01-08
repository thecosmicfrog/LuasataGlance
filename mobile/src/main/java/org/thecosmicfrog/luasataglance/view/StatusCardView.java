/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2017 Aaron Hastings
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
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.widget.TextView;

import org.thecosmicfrog.luasataglance.R;

public class StatusCardView extends CardView {

    private final String LOG_TAG = SpinnerCardView.class.getSimpleName();

    private TextView textViewStatusTitle;
    private TextView textViewStatus;

    public StatusCardView(Context context) {
        super(context);

        init(context);
    }

    public StatusCardView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public StatusCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    /**
     * Initialise custom View.
     * @param context Context.
     */
    public void init(Context context) {
        inflate(context, R.layout.cardview_status, this);

        textViewStatusTitle = findViewById(R.id.textview_status_title);
        textViewStatus = findViewById(R.id.textview_status);
    }

    public void setStatus(String status) {
        textViewStatus = findViewById(R.id.textview_status);
        textViewStatus.setText(status);
    }

    public void setStatusColor(int color) {
        textViewStatusTitle.setBackgroundResource(color);
    }
}
