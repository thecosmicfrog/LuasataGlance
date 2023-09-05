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
package org.thecosmicfrog.luasataglance.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Toast
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

    private val logTag = NotifyTimeActivity::class.java.simpleName
    private val permissionsNotifications = arrayOf(Manifest.permission.POST_NOTIFICATIONS)

    private var mapNotifyTimes: Map<String, Int>? = null

    private lateinit var viewBinding: ActivityNotifyTimeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityNotifyTimeBinding.inflate(layoutInflater)
        val rootView = viewBinding.root

        val dialog = "dialog"

        /*
         * Use a Material Dialog theme.
         */
        setTheme(android.R.style.Theme_Material_Dialog)

        /* This is a Dialog. Get rid of the default Window title. */
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(rootView)

        if (Preferences.permissionNotificationsShouldNotAskAgain(applicationContext)) {
            /* User has disabled notifications. Close the dialog immediately. */
            finish()

            Toast.makeText(this, R.string.please_allow_notification_permissions, Toast.LENGTH_LONG).show()
        } else {
            EasyPermissions.requestPermissions(
                PermissionRequest.Builder(this, Constant.REQUEST_CODE_NOTIFY_TIMES, *permissionsNotifications)
                    .setRationale(R.string.rationale_notifications)
                    .setPositiveButtonText(R.string.rationale_ask_accept)
                    .setNegativeButtonText(R.string.rationale_ask_decline)
                    .setTheme(android.R.style.Theme_Material_Light_Dialog_Alert)
                    .build()
            )
        }

        val localeDefault = Locale.getDefault().toString()

        mapNotifyTimes = NotifyTimesMap(localeDefault, dialog)

        val spinnerNotifyTime = viewBinding.spinnerNotifytime
        val adapterNotifyTime: ArrayAdapter<*> = ArrayAdapter.createFromResource(
                applicationContext, R.array.array_notifytime_mins, R.layout.spinner_notify_time
        )
        adapterNotifyTime.setDropDownViewResource(R.layout.spinner_notify_time)
        spinnerNotifyTime.adapter = adapterNotifyTime

        /* Set the Spinner's colour to Luas purple. */
        val spinnerDrawable = spinnerNotifyTime.background.constantState?.newDrawable()
        spinnerDrawable?.setColorFilter(
                ContextCompat.getColor(applicationContext, R.color.luas_purple),
                PorterDuff.Mode.SRC_ATOP
        )
        spinnerNotifyTime.background = spinnerDrawable

        val buttonNotifyTime = viewBinding.buttonNotifytime
        buttonNotifyTime.setOnClickListener {
            /*
             * Create an Intent to send the user-selected notification time back to
             * LineFragment.
             */
            val intent = Intent()
            intent.setPackage(packageName)
            intent.setClass(applicationContext, NotifyTimesReceiver::class.java)
            intent.action = NotifyTimeActivity::class.java.name
            intent.putExtra(
                    Constant.NOTIFY_STOP_NAME,
                    Preferences.notifyStopName(applicationContext)
            )

            intent.putExtra(
                    Constant.NOTIFY_TIME,
                    mapNotifyTimes!![spinnerNotifyTime.selectedItem.toString()]
            )

            /* Send the Intent. */
            sendBroadcast(intent)

            /* Dismiss the Dialog. */
            finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)

        /* If the user has not granted notification permissions, just close the dialog. */
        if (!hasAllPermissionsGranted(grantResults)) {
            finish()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String?>) {
        Log.i(logTag, "Notifications permission granted.")
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String?>) {
        Log.i(logTag, "Notifications permission denied.")

        /* User has disabled notifications. Close the dialog immediately. */finish()
    }

    override fun onRationaleAccepted(requestCode: Int) {
        Log.i(logTag, "Notifications rationale accepted.")
    }

    override fun onRationaleDenied(requestCode: Int) {
        Log.i(logTag, "Notifications rationale denied.")

        /* User has disabled notifications. Close the dialog immediately. */finish()
        Preferences.savePermissionNotificationsShouldNotAskAgain(applicationContext, true)
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
