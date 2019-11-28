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
 * along with Luas at a Glance.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package org.thecosmicfrog.luasataglance.activity

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.`object`.NotifyTimesMap
import org.thecosmicfrog.luasataglance.receiver.NotifyTimesReceiver
import org.thecosmicfrog.luasataglance.util.Analytics
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.Preferences
import java.util.*

class NotifyTimeActivity : FragmentActivity() {

    private val logTag = NotifyTimeActivity::class.java.simpleName

    private var mapNotifyTimes: Map<String, Int>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val dialog = "dialog"

        /*
         * If the user is on Lollipop or above, use a Material Dialog theme. Otherwise, fall back to
         * the default theme set in AndroidManifest.xml.
         */
        if (Build.VERSION.SDK_INT >= 21) {
            setTheme(android.R.style.Theme_Material_Dialog)
        }

        /* This is a Dialog. Get rid of the default Window title. */
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_notify_time)

        val localeDefault = Locale.getDefault().toString()

        mapNotifyTimes = NotifyTimesMap(localeDefault, dialog)

        val spinnerNotifyTime = findViewById<Spinner>(R.id.spinner_notifytime)
        val adapterNotifyTime: ArrayAdapter<*> = ArrayAdapter.createFromResource(
                applicationContext, R.array.array_notifytime_mins, R.layout.spinner_notify_time
        )
        adapterNotifyTime.setDropDownViewResource(R.layout.spinner_notify_time)
        spinnerNotifyTime.adapter = adapterNotifyTime

        /* Set the Spinner's colour to Luas purple. */
        val spinnerDrawable = spinnerNotifyTime.background.constantState!!.newDrawable()
        spinnerDrawable.setColorFilter(
                ContextCompat.getColor(applicationContext, R.color.luas_purple),
                PorterDuff.Mode.SRC_ATOP
        )
        spinnerNotifyTime.background = spinnerDrawable

        val buttonNotifyTime = findViewById<Button>(R.id.button_notifytime)
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

            Analytics.selectContent(
                    applicationContext,
                    "schedule_created",
                    "schedule_created"
            )

            /* Dismiss the Dialog. */
            finish()
        }
    }
}