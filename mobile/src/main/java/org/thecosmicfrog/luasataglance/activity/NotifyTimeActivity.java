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

package org.thecosmicfrog.luasataglance.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.object.NotifyTimesMap;
import org.thecosmicfrog.luasataglance.receiver.NotifyTimesReceiver;
import org.thecosmicfrog.luasataglance.util.Constant;
import org.thecosmicfrog.luasataglance.util.Preferences;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

public class NotifyTimeActivity extends FragmentActivity implements
        EasyPermissions.PermissionCallbacks, EasyPermissions.RationaleCallbacks {

    private final String LOG_TAG = NotifyTimeActivity.class.getSimpleName();
    private final String[] PERMISSIONS_NOTIFICATIONS = {Manifest.permission.POST_NOTIFICATIONS};

    private Map<String, Integer> mapNotifyTimes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final String DIALOG = "dialog";

        /* Use a Material Dialog theme. */
        setTheme(android.R.style.Theme_Material_Dialog);

        /* This is a Dialog. Get rid of the default Window title. */
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_notify_time);

        if (Preferences.permissionNotificationsShouldNotAskAgain(getApplicationContext())) {
            /* User has disabled notifications. Close the dialog immediately. */
            finish();

            Toast.makeText(
                    this,
                    R.string.please_allow_notification_permissions,
                    Toast.LENGTH_LONG
            ).show();
        } else {
            EasyPermissions.requestPermissions(
                    new PermissionRequest.Builder(
                            this,
                            Constant.REQUEST_CODE_NOTIFY_TIMES,
                            PERMISSIONS_NOTIFICATIONS)
                            .setRationale(R.string.rationale_notifications)
                            .setPositiveButtonText(R.string.rationale_ask_accept)
                            .setNegativeButtonText(R.string.rationale_ask_decline)
                            .setTheme(android.R.style.Theme_Material_Light_Dialog_Alert)
                            .build()
            );
        }

        String localeDefault = Locale.getDefault().toString();

        mapNotifyTimes = new NotifyTimesMap(localeDefault, DIALOG);

        final Spinner spinnerNotifyTime = findViewById(R.id.spinner_notifytime);
        ArrayAdapter adapterNotifyTime = ArrayAdapter.createFromResource(
                getApplicationContext(), R.array.array_notifytime_mins, R.layout.spinner_notify_time
        );
        adapterNotifyTime.setDropDownViewResource(R.layout.spinner_notify_time);
        spinnerNotifyTime.setAdapter(adapterNotifyTime);

        /* Set the Spinner's colour to Luas purple. */
        Drawable spinnerDrawable =
                spinnerNotifyTime.getBackground().getConstantState().newDrawable();
        spinnerDrawable.setColorFilter(
                ContextCompat.getColor(getApplicationContext(), R.color.luas_purple),
                PorterDuff.Mode.SRC_ATOP
        );
        spinnerNotifyTime.setBackground(spinnerDrawable);

        Button buttonNotifyTime = findViewById(R.id.button_notifytime);
        buttonNotifyTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Create an Intent to send the user-selected notification time back to
                 * LineFragment.
                 */
                Intent intent = new Intent();
                intent.setPackage(getPackageName());
                intent.setClass(getApplicationContext(), NotifyTimesReceiver.class);
                intent.setAction(NotifyTimeActivity.class.getName());
                intent.putExtra(
                        Constant.NOTIFY_STOP_NAME,
                        Preferences.notifyStopName(getApplicationContext())
                );

                intent.putExtra(
                        Constant.NOTIFY_TIME,
                        mapNotifyTimes.get(spinnerNotifyTime.getSelectedItem().toString())
                );

                /* Send the Intent. */
                sendBroadcast(intent);

                /* Dismiss the Dialog. */
                finish();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults);

        /* If the user has not granted notification permissions, just close the dialog. */
        if (!hasAllPermissionsGranted(grantResults)) {
            finish();
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        Log.i(LOG_TAG, "Notifications permission granted.");
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Log.i(LOG_TAG, "Notifications permission denied.");

        /* User has disabled notifications. Close the dialog immediately. */
        finish();
    }

    @Override
    public void onRationaleAccepted(int requestCode) {
        Log.i(LOG_TAG, "Notifications rationale accepted.");
    }

    @Override
    public void onRationaleDenied(int requestCode) {
        Log.i(LOG_TAG, "Notifications rationale denied.");

        /* User has disabled notifications. Close the dialog immediately. */
        finish();

        Preferences.savePermissionNotificationsShouldNotAskAgain(getApplicationContext(), true);
    }

    /**
     * Check if all permissions have been granted.
     * @param grantResults Grant results.
     * @return All permissions granted or not.
     */
    private boolean hasAllPermissionsGranted(@NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }

        return true;
    }
}
