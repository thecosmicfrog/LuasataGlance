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

import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.FragmentActivity
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.model.NotifyTimesMap
import org.thecosmicfrog.luasataglance.receiver.NotifyTimesReceiver
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.Preferences
import java.util.*

class NotifyTimeActivity : FragmentActivity() {

    private val logTag = NotifyTimeActivity::class.java.simpleName

    private var mapNotifyTimes: Map<String, Int>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.LuasAtAGlancePopupDialog)

        /* This is a Dialog. Get rid of the default Window title. */
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_notify_time)

        val localeDefault = Locale.getDefault().toString()

        mapNotifyTimes = NotifyTimesMap(localeDefault)

        val spinnerNotifyTime = findViewById<Spinner>(R.id.spinner_notifytime)
        val adapterNotifyTime: ArrayAdapter<*> = ArrayAdapter.createFromResource(
                applicationContext, R.array.array_notifytime_mins, R.layout.spinner_notify_time
        )
        adapterNotifyTime.setDropDownViewResource(R.layout.spinner_notify_time)
        spinnerNotifyTime.adapter = adapterNotifyTime

        val buttonNotifyTime = findViewById<Button>(R.id.button_notifytime)
        buttonNotifyTime.setOnClickListener {
            /*
             * Create an Intent to send the user-selected notification time back to
             * LineFragment.
             */
            val intent = Intent()
            intent.setPackage(packageName)
            intent.action = NotifyTimeActivity::class.java.name
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
}

