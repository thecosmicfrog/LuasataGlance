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

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.thecosmicfrog.luasataglance.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private val logTag = AboutActivity::class.java.simpleName

    private lateinit var viewBinding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityAboutBinding.inflate(layoutInflater)
        val rootView = viewBinding.root

        setContentView(rootView)

        val textViewLicense = viewBinding.textviewLicense
        textViewLicense.setOnClickListener {
            startActivity(
                Intent(
                    applicationContext,
                    LicenseActivity::class.java
                )
            )
        }
    }
}

