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

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.util.Analytics;

public class AboutActivity extends FragmentActivity {

    private final String LOG_TAG = AboutActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*
         * If the user is on Lollipop or above, use a Material Dialog theme. Otherwise, fall back to
         * the default theme set in AndroidManifest.xml.
         */
        if (Build.VERSION.SDK_INT >= 21)
            setTheme(android.R.style.Theme_Material_Dialog);

        /* This is a Dialog. Get rid of the default Window title. */
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

        TextView textViewSourceCode = (TextView) findViewById(R.id.textview_sourcecode);
        textViewSourceCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Analytics.selectContent(
                        getApplicationContext(),
                        "link_tapped",
                        "sourcecode_tapped"
                );
            }
        });

        TextView textViewLicense = (TextView) findViewById(R.id.textview_license);
        textViewLicense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(
                        new Intent(
                                getApplicationContext(),
                                LicenseActivity.class
                        )
                );

                Analytics.selectContent(
                        getApplicationContext(),
                        "link_tapped",
                        "license_tapped"
                );
            }
        });
    }
}
