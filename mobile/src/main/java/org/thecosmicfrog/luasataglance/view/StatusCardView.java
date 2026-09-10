/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2026 Aaron Hastings
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
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.card.MaterialCardView;

import org.thecosmicfrog.luasataglance.R;

public class StatusCardView extends MaterialCardView {

    private final String LOG_TAG = SpinnerCardView.class.getSimpleName();

    private TextView textViewStatusTitle;
    private TextView textViewStatus;
    private ShimmerFrameLayout shimmerFrameLayoutStatus;

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
     *
     * @param context Context.
     */
    public void init(Context context) {
        inflate(context, R.layout.cardview_status, this);

        /* Remove default MaterialCardView stroke (thin border around the card). */
        setStrokeWidth(0);

        textViewStatusTitle = findViewById(R.id.textview_status_title);
        textViewStatus = findViewById(R.id.textview_status);
        shimmerFrameLayoutStatus = findViewById(R.id.shimmer_status);
    }

    /**
     * Show a status message in place of the shimmer.
     *
     * @param status The status message to display.
     */
    public void setStatus(String status) {
        shimmerFrameLayoutStatus.setVisibility(View.GONE);
        textViewStatus.setVisibility(View.VISIBLE);
        textViewStatus.setText(status);
    }

    /**
     * Show the shimmer in place of a status message.
     */
    public void showShimmer() {
        textViewStatus.setVisibility(View.GONE);
        shimmerFrameLayoutStatus.setVisibility(View.VISIBLE);
    }

    /**
     * Colour the title band.
     *
     * @param fillColor Background colour resource for the band.
     * @param textColor Text colour resource for the title.
     */
    public void setStatusColor(@ColorRes int fillColor, @ColorRes int textColor) {
        textViewStatusTitle.setBackgroundResource(fillColor);
        textViewStatusTitle.setTextColor(ContextCompat.getColor(getContext(), textColor));
    }
}
