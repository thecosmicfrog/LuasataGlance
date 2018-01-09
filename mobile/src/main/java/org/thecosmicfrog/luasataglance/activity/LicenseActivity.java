/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2018 Aaron Hastings
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

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.thecosmicfrog.luasataglance.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LicenseActivity extends AppCompatActivity {

    private final String LOG_TAG = LicenseActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_license);

        TextView textViewLicenseContent = (TextView) findViewById(R.id.textview_license_content);

        InputStream inputStream = getResources().openRawResource(R.raw.license);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        try {
            String line = bufferedReader.readLine();

            while (line != null) {
                textViewLicenseContent.append(line + " ");

                line = bufferedReader.readLine();
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Something went wrong while reading the LICENSE file.");
            Log.e(LOG_TAG, Log.getStackTraceString(e));

            finish();
        }
    }
}
