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

package org.thecosmicfrog.luasataglance.receiver;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.activity.MainActivity;
import org.thecosmicfrog.luasataglance.util.Constant;
import org.thecosmicfrog.luasataglance.util.Preferences;

public class NotifyTimesReceiver extends BroadcastReceiver {

    private final String LOG_TAG = NotifyTimesReceiver.class.getSimpleName();

    @Override
    public void onReceive(final Context context, Intent intent) {
        final int notifyTimeUserRequestedMins = intent.getIntExtra(Constant.NOTIFY_TIME, 5);
        final int NOTIFY_TIME_SAFETY_NET_MILLIS = 30000;

        String notifyStopNameExpected = Preferences.notifyStopName(context);
        int notifyStopTimeExpected = Preferences.notifyStopTimeExpected(context);

        /*
         * Define when a user should be notified that their tram is on its way. To do this, we
         * simply take the number of minutes the tram is expected in and subtract the due time
         * the user has asked to be notified at.
         *
         * 1 minute can be the difference between missing and catching a tram. Always insert an
         * artificial 30 second "safety net".
         * Example: If the user has set a notification that should fire after 5 minutes, the
         *          notification will actually fire after 4.5 minutes.
         */
        int notifyDelayMillis =
                (notifyStopTimeExpected - notifyTimeUserRequestedMins)
                        * 60000
                        - NOTIFY_TIME_SAFETY_NET_MILLIS;

        /*
         * If the notification time makes no sense, inform the user and don't proceed.
         */
        if (notifyDelayMillis < 0) {
            Toast.makeText(
                    context,
                    context.getString(R.string.notify_invalid_time),
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        /*
         * Inform user the notification has been scheduled successfully.
         */
        Toast.makeText(
                context,
                context.getString(R.string.notify_successful),
                Toast.LENGTH_SHORT
        ).show();

        scheduleNotification(
                context,
                notifyStopNameExpected,
                notifyTimeUserRequestedMins,
                notifyDelayMillis
        );
    }

    /**
     * Schedule notification for tram.
     * @param context Context.
     * @param notifyStopName Name of stop to notify for.
     * @param notifyTimeUserRequestedMins Minutes before tram arrival the user has requested to
     *                                    be notified at.
     * @param notifyDelayMillis Milliseconds to wait before firing off notification.
     */
    private void scheduleNotification(Context context,
                                      final String notifyStopName,
                                      final int notifyTimeUserRequestedMins,
                                      int notifyDelayMillis) {
        final int REQUEST_CODE_OPEN_MAIN_ACTIVITY = 0;
        final int REQUEST_CODE_SCHEDULE_NOTIFICATION = 1;

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                /*
                 * Create a StringBuilder in order to format the notification message correctly.
                 * Start by adding a message telling the user their tram is expected in
                 * N.
                 */
                StringBuilder stringBuilderContentText = new StringBuilder();
                stringBuilderContentText.append(
                        context.getString(R.string.notification_tram_expected)
                ).append(
                        Integer.toString(notifyTimeUserRequestedMins)
                );

                /*
                 * Append either "minutes" or "minute" depending on the time chosen.
                 */
                if (notifyTimeUserRequestedMins > 1)
                    stringBuilderContentText.append(
                            context.getString(R.string.notification_minutes)
                    );
                else
                    stringBuilderContentText.append(
                            context.getString(R.string.notification_minute)
                    );

                /*
                 * Prepare an Intent/PendingIntent to open the MainActivity with the
                 * stop-to-notify-for as a String extra.
                 */
                Intent intentOpenMainActivity = new Intent(context, MainActivity.class);
                intentOpenMainActivity.setPackage(context.getPackageName());
                intentOpenMainActivity.setAction(NotifyTimesReceiver.class.getName());
                intentOpenMainActivity.setFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP
                );
                intentOpenMainActivity.putExtra(Constant.NOTIFY_STOP_NAME, notifyStopName);

                PendingIntent pendingIntentOpenMainActivity = PendingIntent.getActivity(
                        context,
                        REQUEST_CODE_OPEN_MAIN_ACTIVITY,
                        intentOpenMainActivity,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

                /*
                 * Create a NotificationManager.
                 */
                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );


                /* Android Oreo and above require a NotificationChannel to be created. */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel notificationChannel =
                            new NotificationChannel(
                                    "notifyTimes",
                                    "Notify Times",
                                    NotificationManager.IMPORTANCE_HIGH
                            );

                    /* Configure notification channel. */
                    notificationChannel.setDescription("Notify Times");
                    notificationChannel.enableLights(true);
                    notificationChannel.setLightColor(context.getColor(R.color.luas_purple));
                    notificationChannel.setVibrationPattern(new long[] {100, 1000, 1000, 1000, 1000});
                    notificationChannel.enableVibration(true);

                    notificationManager.createNotificationChannel(notificationChannel);
                }

                /*
                 * Create the NotificationBuilder, setting an appropriate title and the message
                 * built in the StringBuilder. The default notification sound should be played
                 * and the device should vibrate twice for 1 second with a 1 second delay
                 * between them. Setting MAX priority due to the time-sensitive nature of trams.
                 */
                NotificationCompat.Builder notificationBuilder =
                        new NotificationCompat.Builder(context, "notifyTimes")
                                .setPriority(Notification.PRIORITY_MAX)
                                .setContentIntent(pendingIntentOpenMainActivity)
                                .setContentTitle(
                                        context.getString(
                                                R.string.notification_title
                                        )
                                )
                                .setContentText(stringBuilderContentText.toString())
                                .setSmallIcon(R.drawable.laag_logo_notification)
                                .setVibrate(new long[] {100, 1000, 1000, 1000, 1000})
                                .setSound(
                                        RingtoneManager.getDefaultUri(
                                                RingtoneManager.TYPE_NOTIFICATION
                                        )
                                )
                                .setAutoCancel(true);

                /* Display notification. */
                notificationManager.notify(1, notificationBuilder.build());
            }
        };

        context.getApplicationContext().registerReceiver(
                broadcastReceiver,
                new IntentFilter("org.thecosmicfrog.luasataglance")
        );

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_SCHEDULE_NOTIFICATION,
                new Intent(context.getPackageName()),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(
                Context.ALARM_SERVICE
        );

        /*
         * Due to the restrictions enforced by Doze, we need to ensure our notification will fire.
         * Check to see which API the device is using, and trigger the appropriate set() method.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            /* Wake up the devices in Doze mode. */
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + notifyDelayMillis, pendingIntent
            );
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            /* Wake up the device in Idle mode. */
            alarmManager.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + notifyDelayMillis, pendingIntent
            );
        } else {
            /* Wake up the device. */
            alarmManager.set(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + notifyDelayMillis, pendingIntent
            );
        }
    }
}
