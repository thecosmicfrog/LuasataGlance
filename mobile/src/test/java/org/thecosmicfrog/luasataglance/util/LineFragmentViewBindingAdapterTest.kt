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
package org.thecosmicfrog.luasataglance.util

import android.view.LayoutInflater
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.databinding.FragmentGreenlineBinding
import org.thecosmicfrog.luasataglance.databinding.FragmentRedlineBinding

/**
 * Tests that [LineFragmentViewBindingAdapter] finds every view on both line layouts.
 */
@RunWith(RobolectricTestRunner::class)
class LineFragmentViewBindingAdapterTest {

    private lateinit var inflater: LayoutInflater

    @Before
    fun setUp() {
        /* The layouts use Material3 widgets, which will not inflate against a context with no theme. */
        inflater = LayoutInflater.from(
            ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.AppTheme)
        )
    }

    @Test
    fun `every view resolves on the Red Line layout`() {
        viewsOf(LineFragmentViewBindingAdapter(FragmentRedlineBinding.inflate(inflater), null)).forEach { (name, view) ->
            assertWithMessage("Red Line: %s", name).that(view).isNotNull()
        }
    }

    @Test
    fun `every view resolves on the Green Line layout`() {
        viewsOf(LineFragmentViewBindingAdapter(null, FragmentGreenlineBinding.inflate(inflater))).forEach { (name, view) ->
            assertWithMessage("Green Line: %s", name).that(view).isNotNull()
        }
    }

    @Test
    fun `the two layouts expose the same set of views`() {
        val redLine = viewsOf(LineFragmentViewBindingAdapter(FragmentRedlineBinding.inflate(inflater), null))
        val greenLine = viewsOf(LineFragmentViewBindingAdapter(null, FragmentGreenlineBinding.inflate(inflater)))

        assertThat(greenLine.filterValues { it != null }.keys).containsExactlyElementsIn(redLine.filterValues { it != null }.keys)
    }

    @Test
    fun `the Red Line adapter hands back the Red Line's own views`() {
        /* Guards the order of the two arguments, which nothing else would catch while both layouts still inflate. */
        val binding = FragmentRedlineBinding.inflate(inflater)
        val adapter = LineFragmentViewBindingAdapter(binding, null)

        assertThat(adapter.scrollview).isSameInstanceAs(binding.redlineScrollview)
        assertThat(adapter.swiperefreshlayout).isSameInstanceAs(binding.redlineSwiperefreshlayout)
    }

    @Test
    fun `the Green Line adapter hands back the Green Line's own views`() {
        val binding = FragmentGreenlineBinding.inflate(inflater)
        val adapter = LineFragmentViewBindingAdapter(null, binding)

        assertThat(adapter.scrollview).isSameInstanceAs(binding.greenlineScrollview)
        assertThat(adapter.swiperefreshlayout).isSameInstanceAs(binding.greenlineSwiperefreshlayout)
    }

    /**
     * Every view the adapter exposes, keyed by its property name so a failure says which one is missing.
     */
    private fun viewsOf(adapter: LineFragmentViewBindingAdapter) = mapOf(
        "stopForecastConstraintLayout" to adapter.stopForecastConstraintLayout,
        "textViewStopForecastInbound" to adapter.textViewStopForecastInbound,
        "textViewStopForecastOutbound" to adapter.textViewStopForecastOutbound,
        "recyclerViewStopForecastsInbound" to adapter.recyclerViewStopForecastsInbound,
        "recyclerViewStopForecastsOutbound" to adapter.recyclerViewStopForecastsOutbound,
        "progressbar" to adapter.progressbar,
        "scrollview" to adapter.scrollview,
        "spinnerCardView" to adapter.spinnerCardView,
        "statuscardview" to adapter.statuscardview,
        "tutorialcardviewSelectStop" to adapter.tutorialcardviewSelectStop,
        "swiperefreshlayout" to adapter.swiperefreshlayout
    )
}
