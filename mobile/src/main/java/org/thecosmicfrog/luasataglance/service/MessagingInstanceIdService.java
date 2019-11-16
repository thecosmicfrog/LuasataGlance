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

package org.thecosmicfrog.luasataglance.service;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import org.thecosmicfrog.luasataglance.util.AppUtil;

public class MessagingInstanceIdService extends FirebaseInstanceIdService {

    private final String LOG_TAG = MessagingInstanceIdService.class.getSimpleName();

    public MessagingInstanceIdService() {
    }

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();

        /* Get updated InstanceID token. */
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        String refreshedTokenToLog;

        if (refreshedToken != null) {
            /* If we're in an emulator, just log the token as-is for easy debugging. */
            if (AppUtil.isEmulator()) {
                refreshedTokenToLog = refreshedToken;
            } else {
                refreshedTokenToLog =
                        refreshedToken.replaceFirst("(.{10}).+(.{10})", "$1...$2");
            }

            Log.d(LOG_TAG, "Refreshed token: " + refreshedTokenToLog);
        }
    }
}
