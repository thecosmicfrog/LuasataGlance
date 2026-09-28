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
package org.thecosmicfrog.luasataglance.activity

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.databinding.ActivityNotifyTimeBinding
import org.thecosmicfrog.luasataglance.model.NotifyTimeOptions
import org.thecosmicfrog.luasataglance.receiver.NotifyTimesReceiver
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.Preferences
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks
import pub.devrel.easypermissions.EasyPermissions.RationaleCallbacks
import pub.devrel.easypermissions.PermissionRequest
import androidx.core.net.toUri

class NotifyTimeActivity : AppCompatActivity(), PermissionCallbacks, RationaleCallbacks {

    private lateinit var context: Context
    private lateinit var viewBinding: ActivityNotifyTimeBinding

    private var notifyTimeOptions: List<Int> = emptyList()

    private val logTag = NotifyTimeActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initActivity()

        /* The due time of the tapped tram, saved by StopForecastUtil.showNotifyTimeDialog() just before starting this. */
        notifyTimeOptions = NotifyTimeOptions.forTramDueIn(Preferences.notifyStopTimeExpected(context))

        /* StopForecastUtil refuses such a tram before starting this Activity, so only a stale preference gets this far. */
        if (notifyTimeOptions.isEmpty()) {
            Toast.makeText(this, R.string.cannot_schedule_notification, Toast.LENGTH_LONG).show()
            finish()

            return
        }

        initViews()
        checkRequiredPermissions()
    }

    /**
     * Initialise Activity.
     */
    private fun initActivity() {
        viewBinding = ActivityNotifyTimeBinding.inflate(layoutInflater)
        context = viewBinding.root.context

        setContentView(viewBinding.root)
    }

    /**
     * Initialise Views.
     */
    private fun initViews() {
        initNotifyTimeSpinner()
        initNotifyButton()
        showReplaceWarningIfReminderPending()
    }

    /**
     * Warn that scheduling will replace a reminder that has not fired yet. Only one reminder can exist at a time.
     */
    private fun showReplaceWarningIfReminderPending() {
        if (Preferences.notifyTriggerTime(context) > System.currentTimeMillis()) {
            viewBinding.textviewNotifytimeReplacesExisting.visibility = View.VISIBLE
        }
    }

    /**
     * Fill the Spinner with only the times that make sense for this tram. A tram due in 9 minutes offers 2 to 8.
     */
    private fun initNotifyTimeSpinner() {
        viewBinding.spinnerNotifytime.adapter = ArrayAdapter(
            applicationContext,
            R.layout.spinner_notify_time,
            notifyTimeOptions.map { getString(R.string.notify_mins_before_arrival, it) }
        ).apply {
            setDropDownViewResource(R.layout.spinner_notify_time)
        }
    }

    /**
     * Initialise Notify Button.
     */
    private fun initNotifyButton() {
        viewBinding.buttonNotifytime.setOnClickListener {
            if (!checkAndRequestExactAlarmPermission()) return@setOnClickListener

            sendNotificationIntent()

            finish()
        }
    }

    /**
     * Check required permissions are granted.
     */
    private fun checkRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            checkNotificationPermission()
        } else {
            checkAndRequestExactAlarmPermission()
        }
    }

    /**
     * Check user has granted notification permissions.
     */
    private fun checkNotificationPermission() {
        when {
            Preferences.permissionNotificationsShouldNotAskAgain(applicationContext) -> {
                showToastAndFinish(R.string.please_allow_notification_permissions)
            }
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED -> {
                showNotificationPermissionDialog()
            }
            else -> checkAndRequestExactAlarmPermission()
        }
    }

    /**
     * Show notification permission dialog.
     */
    private fun showNotificationPermissionDialog() {
        AlertDialog.Builder(this, R.style.LuasAtAGlancePermissionsRequestDialog)
            .setMessage(getString(R.string.rationale_notifications))
            .setPositiveButton(getString(R.string.rationale_ask_accept)) { dialog, _ ->
                requestNotificationPermissions()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.rationale_ask_decline)) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .create()
            .show()
    }

    /**
     * Check if exact alarm permission is granted and request if not.
     */
    private fun checkAndRequestExactAlarmPermission(): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        return if (!alarmManager.canScheduleExactAlarms()) {
            showExactAlarmPermissionDialog()
            false
        } else {
            true
        }
    }

    /**
     * Show exact alarm permission dialog.
     */
    private fun showExactAlarmPermissionDialog() {
        AlertDialog.Builder(this, R.style.LuasAtAGlancePermissionsRequestDialog)
            .setMessage(getString(R.string.rationale_exact_alarm))
            .setPositiveButton(getString(R.string.rationale_ask_accept)) { dialog, _ ->
                openExactAlarmSystemSettings()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.rationale_ask_decline)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    /**
     * Send notification Intent.
     */
    private fun sendNotificationIntent() {
        Intent().apply {
            setPackage(packageName)
            setClass(applicationContext, NotifyTimesReceiver::class.java)
            action = NotifyTimeActivity::class.java.name
            putExtra(Constant.NOTIFY_STOP_NAME, Preferences.notifyStopName(applicationContext))
            putExtra(Constant.NOTIFY_TIME, notifyTimeOptions[viewBinding.spinnerNotifytime.selectedItemPosition])
        }.also { sendBroadcast(it) }
    }

    /**
     * Show Toast and close the Activity. Purposely not using a Snackbar, as finish() closes this Activity straight away and the
     * Snackbar would close with it before it could be read by the user. A Toast stays on screen after the Activity has gone.
     */
    private fun showToastAndFinish(@StringRes messageResId: Int) {
        Toast.makeText(this, messageResId, Toast.LENGTH_LONG).show()

        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when {
            !hasAllPermissionsGranted(grantResults) -> {
                finish()
            }
            requestCode == Constant.REQUEST_CODE_NOTIFY_TIMES &&
                    Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                /* Add a small delay to ensure the notification dialog is fully dismissed before requesting the next permission. */
                viewBinding.root.postDelayed({
                    checkAndRequestExactAlarmPermission()
                }, 200)
            }
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String?>) {
        Log.i(logTag, "Notifications permission granted.")
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String?>) {
        Log.i(logTag, "Notifications permission denied.")

        /* User has disabled notifications. Close the dialog immediately. */
        finish()
    }

    override fun onRationaleAccepted(requestCode: Int) {
        Log.i(logTag, "Notifications rationale accepted.")
    }

    override fun onRationaleDenied(requestCode: Int) {
        Log.i(logTag, "Notifications rationale denied.")

        /* User has disabled notifications. Close the dialog immediately. */
        finish()

        Preferences.savePermissionNotificationsShouldNotAskAgain(applicationContext, true)
    }

    /**
     * Request notification permissions from user by opening the system settings.
     */
    private fun requestNotificationPermissions() {
        val permissionsNotifications = arrayOf(Manifest.permission.POST_NOTIFICATIONS)

        EasyPermissions.requestPermissions(
            PermissionRequest.Builder(this, Constant.REQUEST_CODE_NOTIFY_TIMES, *permissionsNotifications)
                .setRationale(R.string.rationale_notifications)
                .setPositiveButtonText(R.string.rationale_ask_accept)
                .setNegativeButtonText(R.string.rationale_ask_decline)
                .setTheme(R.style.LuasAtAGlanceRationaleDialog)
                .build()
        )
    }

    /**
     * Open the Android system settings for exact alarm permission so user can enable it.
     */
    private fun openExactAlarmSystemSettings() {
        try {
            Intent().apply {
                action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                data = "package:$packageName".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }.also { startActivity(it) }
        } catch (e: Exception) {
            Log.e(logTag, "Failed to open exact alarm settings", e)

            /* Open the "App info" settings instead (user has to scroll down to "Alarms and reminders"). */
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:$packageName".toUri()
            })
        }
    }

    /**
     * Check if all permissions have been granted.
     * @param grantResults Grant results.
     * @return All permissions granted or not.
     */
    private fun hasAllPermissionsGranted(grantResults: IntArray): Boolean {
        for (grantResult in grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false
            }
        }

        return true
    }
}
