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
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceActivity
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import org.thecosmicfrog.luasataglance.R

class SettingsActivity : PreferenceActivity(), Preference.OnPreferenceChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.preferences)
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_default_stop)))
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        /* Hack to add toolbar to SettingsActivity. */
        val linearLayoutRootView = findViewById<View?>(android.R.id.list).parent.parent.parent as LinearLayout

        val toolbar = LayoutInflater.from(this).inflate(
            R.layout.settings_toolbar,
            linearLayoutRootView,
            false
        ) as Toolbar

        linearLayoutRootView.addView(toolbar, 0)

        toolbar.setNavigationOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                finish()
            }
        })

        /* Set toolbar elevation. */
        toolbar.elevation = 8.0f
    }

    /**
     * Attaches a listener so the summary is always updated with the preference value.
     * Also fires the listener once, to initialize the summary (so it shows up before the value
     * is changed.)
     */
    private fun bindPreferenceSummaryToValue(preference: Preference) {
        /* Set the listener to watch for value changes. */
        preference.setOnPreferenceChangeListener(this)

        /*
         * Trigger the listener immediately with the preference's current value.
         */
        onPreferenceChange(
            preference,
            PreferenceManager
                .getDefaultSharedPreferences(preference.getContext())
                .getString(
                    preference.getKey(),
                    getString(R.string.none)
                )!!
        )
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val stringValue = newValue.toString()

        if (preference is ListPreference) {
            /*
             * For ListPreferences, look up the correct display value in the preference's
             * 'entries' list (since they have separate labels/values).
             */
            val prefIndex = preference.findIndexOfValue(stringValue)

            if (prefIndex >= 0) preference.setSummary(preference.getEntries()[prefIndex])
        } else {
            /* For other preferences, set the summary to the value's simple string representation. */
            preference.setSummary(stringValue)
        }

        return true
    }
}
