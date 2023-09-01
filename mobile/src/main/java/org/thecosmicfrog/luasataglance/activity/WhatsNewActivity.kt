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

import android.os.Build
import android.os.Bundle
import android.view.Window
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import org.thecosmicfrog.luasataglance.R

class WhatsNewActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        /*
         * Use a Material Dialog theme.
         */
        setTheme(android.R.style.Theme_Material_Dialog)

        /* This is a Dialog. Get rid of the default Window title. */
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_whats_new)

        formatAndSetWhatsNewTitles()
    }

    private fun formatAndSetWhatsNewTitles() {
        val textViewWhatsNewTitleCurrent =
                findViewById<TextView>(R.id.textview_whatsnew_title_current)

        textViewWhatsNewTitleCurrent.text = String.format(
                getString(R.string.whatsnew_title_current),
                getString(R.string.version_name),
                getString(R.string.release_date)
        )
    }
}

