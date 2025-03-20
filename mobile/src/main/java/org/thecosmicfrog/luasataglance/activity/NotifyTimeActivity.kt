/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2025 Aaron Hastings
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
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.databinding.ActivityNotifyTimeBinding
import org.thecosmicfrog.luasataglance.model.NotifyTimesMap
import org.thecosmicfrog.luasataglance.receiver.NotifyTimesReceiver
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.Preferences
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks
import pub.devrel.easypermissions.EasyPermissions.RationaleCallbacks
import pub.devrel.easypermissions.PermissionRequest
import java.util.Locale

class NotifyTimeActivity : FragmentActivity(), PermissionCallbacks, RationaleCallbacks {

    private lateinit var context: Context
    private lateinit var viewBinding: ActivityNotifyTimeBinding
    private var mapNotifyTimes: Map<String, Int>? = null
    private val logTag = NotifyTimeActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initActivity()
        initViews()
        checkRequiredPermissions()
    }

    /**
     * Initialise Activity.
     */
    private fun initActivity() {
        setTheme(android.R.style.Theme_Material_Dialog)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

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
    }

    /**
     * Initialise Spinner.
     */
    private fun initNotifyTimeSpinner() {
        val localeDefault = Locale.getDefault().toString()
        mapNotifyTimes = NotifyTimesMap(localeDefault, "dialog")

        viewBinding.spinnerNotifytime.apply {
            adapter = ArrayAdapter.createFromResource(
                applicationContext,
                R.array.array_notifytime_mins,
                R.layout.spinner_notify_time
            ).apply {
                setDropDownViewResource(R.layout.spinner_notify_time)
            }

            background = background.constantState?.newDrawable()?.apply {
                setColorFilter(
                    ContextCompat.getColor(context, R.color.luas_purple),
                    PorterDuff.Mode.SRC_ATOP
                )
            }
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
        if (Build.VERSION.SDK_INT >= VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return if (!isExactAlarmPermissionGranted(alarmManager)) {
                showExactAlarmPermissionDialog()
                false
            } else {
                true
            }
        }
        return true
    }

    /**
     * Check if exact alarm permission is granted.
     */
    private fun isExactAlarmPermissionGranted(alarmManager: AlarmManager): Boolean {
        return if (Build.VERSION.SDK_INT >= VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
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
            putExtra(
                Constant.NOTIFY_TIME,
                mapNotifyTimes!![viewBinding.spinnerNotifytime.selectedItem.toString()]
            )
        }.also { sendBroadcast(it) }
    }

    /**
     * Show confirmation Toast and close the Activity.
     */
    private fun showToastAndFinish(@StringRes messageResId: Int) {
        Toast.makeText(this, messageResId, Toast.LENGTH_LONG).show()

        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
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
                .setTheme(android.R.style.Theme_Material_Light_Dialog_Alert)
                .build()
        )
    }

    /**
     * Open the Android system settings for exact alarm permission so user can enable it.
     */
    private fun openExactAlarmSystemSettings() {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.S) {
            try {
                Intent().apply {
                    action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }.also { startActivity(it) }
            } catch (e: Exception) {
                Log.e(logTag, "Failed to open exact alarm settings", e)

                /* Open the "App info" settings instead (user has to scroll down to "Alarms and reminders". */
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
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
