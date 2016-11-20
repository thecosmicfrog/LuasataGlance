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

package org.thecosmicfrog.luasataglance.activity;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.api.ApiFares;
import org.thecosmicfrog.luasataglance.api.ApiMethods;
import org.thecosmicfrog.luasataglance.object.StopNameIdMap;

import java.util.Locale;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class FaresActivity extends AppCompatActivity {

    private final String LOG_TAG = FaresActivity.class.getSimpleName();

    private ArrayAdapter<CharSequence> adapterLines;
    private ArrayAdapter<CharSequence> adapterStops;
    private ArrayAdapter<CharSequence> adapterStopsAdults;
    private ArrayAdapter<CharSequence> adapterStopsChildren;
    private Spinner spinnerFaresLine;
    private Spinner spinnerFaresOrigin;
    private Spinner spinnerFaresDestination;
    private Spinner spinnerFaresAdults;
    private Spinner spinnerFaresChildren;
    private TextView textViewFaresOffPeak;
    private TextView textViewFaresPeak;
    private StopNameIdMap mapStopNameId;
    private String localeDefault;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fares);

        /* Initialise correct locale. */
        localeDefault = Locale.getDefault().toString();

        /* Instantiate a new StopNameIdMap. */
        mapStopNameId = new StopNameIdMap(localeDefault);

        spinnerFaresLine =
                (Spinner) findViewById(R.id.spinner_fares_line);
        spinnerFaresLine.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position,
                                       long l) {
                int resArrayStops = 0;

                if (position == 0) {
                    resArrayStops = R.array.array_stops_redline;
                } else if (position == 1) {
                    resArrayStops = R.array.array_stops_greenline;
                } else {
                    Log.wtf(LOG_TAG, "Spinner position not 0 or 1. Setting to Red Line.");
                }

                adapterStops = ArrayAdapter.createFromResource(
                        getApplicationContext(),
                        resArrayStops,
                        R.layout.spinner_stops
                );

                spinnerFaresOrigin.setAdapter(adapterStops);
                spinnerFaresDestination.setAdapter(adapterStops);

                /* If the user changes line, reset the displayed fares. */
                clearCalculatedFares();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        spinnerFaresOrigin =
                (Spinner) findViewById(R.id.spinner_fares_origin);
        spinnerFaresOrigin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                loadFaresBasedOnSpinnerSelected();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        spinnerFaresDestination =
                (Spinner) findViewById(R.id.spinner_fares_destination);
        spinnerFaresDestination.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                loadFaresBasedOnSpinnerSelected();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        spinnerFaresAdults =
                (Spinner) findViewById(R.id.spinner_fares_adults);
        spinnerFaresAdults.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                loadFaresBasedOnSpinnerSelected();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        spinnerFaresChildren =
                (Spinner) findViewById(R.id.spinner_fares_children);
        spinnerFaresChildren.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                loadFaresBasedOnSpinnerSelected();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        setSpinnerColor(spinnerFaresLine);
        setSpinnerColor(spinnerFaresOrigin);
        setSpinnerColor(spinnerFaresDestination);
        setSpinnerColor(spinnerFaresAdults);
        setSpinnerColor(spinnerFaresChildren);

        adapterLines = ArrayAdapter.createFromResource(
                this,
                R.array.array_lines,
                R.layout.spinner_stops
        );
        adapterStopsAdults = ArrayAdapter.createFromResource(
                this,
                R.array.array_number_pax,
                R.layout.spinner_stops
        );
        adapterStopsChildren = ArrayAdapter.createFromResource(
                this,
                R.array.array_number_pax,
                R.layout.spinner_stops
        );

        adapterLines.setDropDownViewResource(R.layout.spinner_stops);
        adapterStopsAdults.setDropDownViewResource(R.layout.spinner_stops);
        adapterStopsChildren.setDropDownViewResource(R.layout.spinner_stops);

        spinnerFaresLine.setAdapter(adapterLines);
        spinnerFaresAdults.setAdapter(adapterStopsAdults);
        spinnerFaresChildren.setAdapter(adapterStopsChildren);

        /* Start with a default of 1 adult. */
        spinnerFaresAdults.setSelection(1);
    }

    @Override
    protected void onResume() {
        super.onResume();

        textViewFaresOffPeak =
                (TextView) findViewById(R.id.textview_fares_offpeak);
        textViewFaresPeak =
                (TextView) findViewById(R.id.textview_fares_peak);
    }

    /**
     * Utility method to load calculated fares based on the current values of all Spinners.
     */
    private void loadFaresBasedOnSpinnerSelected() {
        if (spinnerFaresOrigin.getSelectedItem().toString().equals(
                getString(R.string.select_a_stop))
                ||
                spinnerFaresDestination.getSelectedItem().toString().equals(
                getString(R.string.select_a_stop))) {
            return;
        }

        final String API_URL_PREFIX = "https://api";
        final String API_URL_POSTFIX = ".thecosmicfrog.org/cgi-bin";
        final String API_ACTION = "farecalc";

        /*
         * Randomly choose an API endpoint to query. This provides load balancing and redundancy
         * in case of server failures.
         * All API endpoints are of the form: "apiN.thecosmicfrog.org", where N is determined by
         * the formula below.
         * The constant NUM_API_ENDPOINTS governs how many endpoints there currently are.
         */
        final int NUM_API_ENDPOINTS = 2;

        String apiEndpointToQuery = Integer.toString(
                (int) (Math.random() * NUM_API_ENDPOINTS + 1)
        );

        String apiUrl = API_URL_PREFIX + apiEndpointToQuery + API_URL_POSTFIX;

        /*
         * Prepare Retrofit API call.
         */
        final RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(apiUrl)
                .build();

        ApiMethods methods = restAdapter.create(ApiMethods.class);

        Callback<ApiFares> callback = new Callback<ApiFares>() {
            @Override
            public void success(ApiFares apiFares, Response response) {
                String fareOffPeak = apiFares.getOffpeak();
                String farePeak = apiFares.getPeak();

                textViewFaresOffPeak.setText("€" + fareOffPeak);
                textViewFaresPeak.setText("€" + farePeak);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Log.e(LOG_TAG, "Failure during call to server.");

                /*
                 * If we get a message or a response from the server, there's likely an issue with
                 * the client request or the server's response itself.
                 */
                if (retrofitError.getMessage() != null) {
                    Log.e(LOG_TAG, retrofitError.getMessage());
                }

                if (retrofitError.getResponse() != null) {
                    Log.e(LOG_TAG, retrofitError.getResponse().getUrl());
                    Log.e(LOG_TAG, Integer.toString(retrofitError.getResponse().getStatus()));
                    Log.e(LOG_TAG, retrofitError.getResponse().getHeaders().toString());
                    Log.e(LOG_TAG, retrofitError.getResponse().getBody().toString());
                    Log.e(LOG_TAG, retrofitError.getResponse().getReason());
                }

                /*
                 * If we don't receive a message or response, we can still get an idea of what's
                 * going on by getting the "kind" of error.
                 */
                if (retrofitError.getKind() != null) {
                    Log.e(LOG_TAG, retrofitError.getKind().toString());
                }
            }
        };

        /* Call API and get fares from server. */
        methods.getFares(
                API_ACTION,
                mapStopNameId.get(spinnerFaresOrigin.getSelectedItem().toString()),
                mapStopNameId.get(spinnerFaresDestination.getSelectedItem().toString()),
                spinnerFaresAdults.getSelectedItem().toString(),
                spinnerFaresChildren.getSelectedItem().toString(),
                callback
        );
    }

    /**
     * Reset the calculated fares to €0.00.
     */
    private void clearCalculatedFares() {
        textViewFaresOffPeak.setText(getString(R.string.fares_zero));
        textViewFaresPeak.setText(getString(R.string.fares_zero));
    }

    /**
     * Cosmetic method to set the Spinner's arrow to purple.
     * @param spinner Spinner
     */
    private void setSpinnerColor(Spinner spinner) {
        /* Set the Spinner's colour to Luas purple. */
        Drawable spinnerDrawable =
                spinner.getBackground().getConstantState().newDrawable();

        spinnerDrawable.setColorFilter(
                ContextCompat.getColor(getApplicationContext(), R.color.luas_purple),
                PorterDuff.Mode.SRC_ATOP
        );

        spinner.setBackground(spinnerDrawable);
    }
}
