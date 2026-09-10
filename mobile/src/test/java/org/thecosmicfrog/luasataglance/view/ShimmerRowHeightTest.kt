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

package org.thecosmicfrog.luasataglance.view

import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.thecosmicfrog.luasataglance.R

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShimmerRowHeightTest {

    /* 0.85 is the smallest font size Android generally offers and 2.0 the largest. */
    private val fontScales = listOf(0.85f, 1.0f, 1.15f, 1.3f, 1.5f, 2.0f)

    private fun context(fontScale: Float): Context {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(base.resources.configuration)
        config.fontScale = fontScale

        return ContextThemeWrapper(base.createConfigurationContext(config), R.style.AppTheme)
    }

    private fun heightInParent(context: Context, view: View): Int {
        val parent = FrameLayout(context)
        parent.addView(view)
        parent.measure(
            View.MeasureSpec.makeMeasureSpec((360 * context.resources.displayMetrics.density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        return parent.measuredHeight
    }

    private fun inflate(context: Context, layout: Int): View =
        LayoutInflater.from(context).inflate(layout, FrameLayout(context), false)

    @Test
    fun `a shimmer row is the same height as the tram row that replaces it`() {
        fontScales.forEach { fontScale ->
            val context = context(fontScale)

            val shimmer = inflate(context, R.layout.cardview_stop_forecast_shimmer)

            val row = inflate(context, R.layout.cardview_stop_forecast)
            row.findViewById<TextView>(R.id.textview_stop_forecast_destination).text = "Tallaght"
            row.findViewById<TextView>(R.id.textview_stop_forecast_due_time_value).text = "8"
            row.findViewById<TextView>(R.id.textview_stop_forecast_due_time_min_mins).text = "mins"

            assertThat(heightInParent(context, shimmer)).isEqualTo(heightInParent(context, row))
        }
    }

    @Test
    fun `the status shimmer is the same height as a one line status message`() {
        fontScales.forEach { fontScale ->
            val context = context(fontScale)

            val shimmer = StatusCardView(context).apply { showShimmer() }
            val message = StatusCardView(context).apply { setStatus("Normal") }

            assertThat(heightInParent(context, shimmer)).isEqualTo(heightInParent(context, message))
        }
    }
}
