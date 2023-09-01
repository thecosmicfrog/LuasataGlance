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

package org.thecosmicfrog.luasataglance.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.service.MessagingService;

public final class NotificationUtil {

    private static final String LOG_TAG = NotificationUtil.class.getSimpleName();

    /**
     * Show notification.
     * @param context Context.
     * @param remoteMessage RemoteMessage model.
     */
    public static void showNotification(Context context, RemoteMessage remoteMessage) {
        final int REQUEST_CODE_OPEN_FARES_ACTIVITY = 1;
        final int REQUEST_CODE_OPEN_FAVOURITES_ACTIVITY = 2;
        final int REQUEST_CODE_OPEN_MAIN_ACTIVITY = 0;
        final int REQUEST_CODE_OPEN_MAPS_ACTIVITY = 3;
        final int REQUEST_CODE_OPEN_NEWS_ACTIVITY = 4;
        final int REQUEST_CODE_OPEN_SETTINGS_ACTIVITY = 5;

        Class activityToOpen = Constant.CLASS_MAIN_ACTIVITY;
        int requestCode = REQUEST_CODE_OPEN_MAIN_ACTIVITY;

        String remoteMessageActivityToOpen =
                remoteMessage.getData().get(Constant.REMOTEMESSAGE_KEY_ACTIVITY_TO_OPEN);

        if (remoteMessageActivityToOpen != null) {
            switch (remoteMessage.getData().get(Constant.REMOTEMESSAGE_KEY_ACTIVITY_TO_OPEN)) {
                case Constant.REMOTEMESSAGE_VALUE_ACTIVITY_FARES:
                    activityToOpen = Constant.CLASS_FARES_ACTIVITY;
                    requestCode = REQUEST_CODE_OPEN_FARES_ACTIVITY;

                    break;

                case Constant.REMOTEMESSAGE_VALUE_ACTIVITY_FAVOURITES:
                    activityToOpen = Constant.CLASS_FAVOURITES_ACTIVITY;
                    requestCode = REQUEST_CODE_OPEN_FAVOURITES_ACTIVITY;

                    break;

                case Constant.REMOTEMESSAGE_VALUE_ACTIVITY_MAIN:
                    activityToOpen = Constant.CLASS_MAIN_ACTIVITY;
                    requestCode = REQUEST_CODE_OPEN_MAIN_ACTIVITY;

                    break;

                case Constant.REMOTEMESSAGE_VALUE_ACTIVITY_MAPS:
                    activityToOpen = Constant.CLASS_MAPS_ACTIVITY;
                    requestCode = REQUEST_CODE_OPEN_MAPS_ACTIVITY;

                    break;

                case Constant.REMOTEMESSAGE_VALUE_ACTIVITY_NEWS:
                    activityToOpen = Constant.CLASS_NEWS_ACTIVITY;
                    requestCode = REQUEST_CODE_OPEN_NEWS_ACTIVITY;

                    break;

                case Constant.REMOTEMESSAGE_VALUE_ACTIVITY_SETTINGS:
                    activityToOpen = Constant.CLASS_SETTINGS_ACTIVITY;
                    requestCode = REQUEST_CODE_OPEN_SETTINGS_ACTIVITY;

                    break;

                default:
                    Log.e(
                            LOG_TAG,
                            "Remote message activityToOpen key does not correspond to any " +
                                    "known value. Default is MainActivity."
                    );
            }
        }

        /*
         * Prepare an Intent/PendingIntent to open the MainActivity.
         */
        Intent intentOpenActivity = new Intent(context, activityToOpen);
        intentOpenActivity.setAction(MessagingService.class.getName());
        intentOpenActivity.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        PendingIntent pendingIntentOpenActivity = PendingIntent.getActivity(
                context,
                requestCode,
                intentOpenActivity,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        /*
         * Create a NotificationManager.
         */
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE
                );

        /* Android Oreo and above require a NotificationChannel to be created. */
        NotificationChannel notificationChannel =
                new NotificationChannel(
                        "fcmNotification",
                        "Firebase Cloud Messaging notification",
                        NotificationManager.IMPORTANCE_HIGH
                );

        /* Configure notification channel. */
        notificationChannel.setDescription("Firebase Cloud Messaging notification");
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(context.getColor(R.color.luas_purple));
        notificationChannel.setVibrationPattern(new long[] {100, 1000, 1000, 1000, 1000});
        notificationChannel.enableVibration(true);

        notificationManager.createNotificationChannel(notificationChannel);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, "fcmNotification")
                        .setPriority(Notification.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntentOpenActivity)
                        .setContentTitle(remoteMessage.getNotification().getTitle())
                        .setContentText(remoteMessage.getNotification().getBody())
                        .setSmallIcon(R.drawable.laag_logo_notification)
                        .setVibrate(new long[] {300})
                        .setSound(
                                RingtoneManager.getDefaultUri(
                                        RingtoneManager.TYPE_NOTIFICATION
                                )
                        )
                        .setAutoCancel(true);

        /* Display notification. */
        notificationManager.notify(1, notificationBuilder.build());
    }
}
