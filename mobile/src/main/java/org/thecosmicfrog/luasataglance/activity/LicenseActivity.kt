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

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.databinding.ActivityLicenseBinding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class LicenseActivity : AppCompatActivity() {

    private val logTag = LicenseActivity::class.java.simpleName

    private lateinit var viewBinding: ActivityLicenseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityLicenseBinding.inflate(layoutInflater)
        val rootView = viewBinding.root

        setContentView(rootView)

        val textViewLicenseContent = viewBinding.textviewLicenseContent
        val inputStream = resources.openRawResource(R.raw.license)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))

        try {
            var line = bufferedReader.readLine()

            while (line != null) {
                textViewLicenseContent.append("$line ")
                line = bufferedReader.readLine()
            }
        } catch (e: IOException) {
            Log.e(logTag, "Something went wrong while reading the LICENSE file.")
            Log.e(logTag, Log.getStackTraceString(e))

            finish()
        }
    }
}

