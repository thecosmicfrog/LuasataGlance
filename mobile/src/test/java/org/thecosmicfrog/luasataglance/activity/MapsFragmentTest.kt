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

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.model.Stop
import org.thecosmicfrog.luasataglance.util.Constant

/**
 * Tests the hand-off from the map's directions FAB to the user's maps app. A wrong URI still opens a map, just on the wrong place
 * or with a garbled pin label, so nothing else catches it.
 */
@RunWith(RobolectricTestRunner::class)
class MapsFragmentTest {

    private lateinit var application: Application

    /* MapsFragment launches from its host Activity. Launching from the Application would need FLAG_ACTIVITY_NEW_TASK. */
    private lateinit var activity: Activity

    private val milltown = Stop("MIL", R.string.stop_mil, Constant.GREEN_LINE, 53.3099143, -6.2517338)

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        activity = Robolectric.buildActivity(Activity::class.java).create().get()

        /* Robolectric starts any Activity it is asked for. This makes it throw ActivityNotFoundException like a device would. */
        shadowOf(application).checkActivities(true)
    }

    /**
     * Register a fake app that handles ACTION_VIEW for one URI scheme, standing in for a maps app or a browser.
     *
     * @param scheme URI scheme the app handles, such as "geo" or "https".
     */
    private fun installHandlerFor(scheme: String) {
        val component = ComponentName("org.example.$scheme", "org.example.$scheme.ViewActivity")
        val filter = IntentFilter(Intent.ACTION_VIEW).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addDataScheme(scheme)
        }

        shadowOf(application.packageManager).addActivityIfNotPresent(component)
        shadowOf(application.packageManager).addIntentFilterForActivity(component, filter)
    }

    /* The geo: URI. */
    @Test
    fun `geo URI centres on the stop and drops a labelled pin there`() {
        val uri = MapsFragment.directionsUri(milltown, "Milltown")

        assertThat(uri.toString()).isEqualTo("geo:53.3099143,-6.2517338?q=53.3099143,-6.2517338(Milltown)")
    }

    @Test
    fun `geo URI encodes spaces and keeps apostrophes in the label`() {
        val uri = MapsFragment.directionsUri(milltown, "St. Stephen's Green")

        assertThat(uri.toString()).endsWith("(St.%20Stephen's%20Green)")
    }

    @Test
    fun `geo URI encodes a fada as UTF-8`() {
        val uri = MapsFragment.directionsUri(milltown, "Na Glasáin")

        assertThat(uri.toString()).endsWith("(Na%20Glas%C3%A1in)")
    }

    @Test
    fun `geo URI label survives the round trip`() {
        val uri = MapsFragment.directionsUri(milltown, "Ó Conaill - AOP")

        /* What the maps app reads back after decoding, which is the caption it puts on the pin. */
        assertThat(Uri.decode(uri.toString())).endsWith("(Ó Conaill - AOP)")
    }

    /* The web fallback. */
    @Test
    fun `web fallback asks for walking directions to the stop`() {
        val uri = MapsFragment.directionsWebUri(milltown)

        assertThat(uri.toString())
            .isEqualTo("https://www.google.com/maps/dir/?api=1&destination=53.3099143,-6.2517338&travelmode=walking")
    }

    /* Which one gets launched. */
    @Test
    fun `a maps app gets the geo URI`() {
        installHandlerFor("geo")
        installHandlerFor("https")

        MapsFragment.openDirections(activity, milltown, "Milltown")

        val started = shadowOf(activity).nextStartedActivity

        assertThat(started.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(started.data).isEqualTo(MapsFragment.directionsUri(milltown, "Milltown"))
        assertThat(shadowOf(activity).nextStartedActivity).isNull()
    }

    @Test
    fun `no maps app falls back to Google Maps in the browser`() {
        installHandlerFor("https")

        MapsFragment.openDirections(activity, milltown, "Milltown")

        val started = shadowOf(activity).nextStartedActivity

        assertThat(started.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(started.data).isEqualTo(MapsFragment.directionsWebUri(milltown))
        assertThat(shadowOf(activity).nextStartedActivity).isNull()
    }
}
