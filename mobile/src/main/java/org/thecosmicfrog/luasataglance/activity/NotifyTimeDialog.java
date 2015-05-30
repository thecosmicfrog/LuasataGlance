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

package org.thecosmicfrog.luasataglance.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.object.NotifyTimesMap;

import java.util.Locale;
import java.util.Map;

public class NotifyTimeDialog extends Dialog {

    private final String LOG_TAG = NotifyTimeDialog.class.getSimpleName();

    private final String DIALOG = "dialog";

    private Map<String, Integer> mapNotifyTimes;

    private Button buttonNotifyTime;

    private String localeDefault;

    public NotifyTimeDialog(Context context) {
        super(context);

        localeDefault = Locale.getDefault().toString();

        setTitle(R.string.notify_title);

        mapNotifyTimes = new NotifyTimesMap(localeDefault, DIALOG);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_notifytime);

        final Spinner spinnerNotifyTime = (Spinner) findViewById(R.id.spinner_notifytime);
        ArrayAdapter adapterNotifyTime = ArrayAdapter.createFromResource(
                getContext(), R.array.array_notifytime_mins, R.layout.spinner_stops
        );
        adapterNotifyTime.setDropDownViewResource(R.layout.spinner_stops);
        spinnerNotifyTime.setAdapter(adapterNotifyTime);

        buttonNotifyTime = (Button) findViewById(R.id.button_notifytime);
        buttonNotifyTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Create an Intent to send the user-selected notification time back to
                 * LuasTimesFragment.
                 */
                Intent intent = new Intent();
                intent.setAction("org.thecosmicfrog.luasataglance.activity.NotifyTimeDialog");
                intent.putExtra(
                        "notifyTime",
                        mapNotifyTimes.get(spinnerNotifyTime.getSelectedItem().toString())
                );

                // Send the Intent.
                getContext().sendBroadcast(intent);

                Log.i(LOG_TAG, "Intent broadcast.");

                // Dismiss the Dialog.
                dismiss();
            }
        });
    }
}
