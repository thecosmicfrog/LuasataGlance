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
package org.thecosmicfrog.luasataglance.widget

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.Application
import android.app.ForegroundServiceStartNotAllowedException
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowApplication
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.api.ApiMethods
import org.thecosmicfrog.luasataglance.api.ApiTimes
import org.thecosmicfrog.luasataglance.model.Tram
import org.thecosmicfrog.luasataglance.util.Preferences
import retrofit2.Response

/**
 * Tests for what [StopForecastWidget] does with a tap, up to handing the fetch to [WidgetFetchService].
 */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [PartialUpdateShadowAppWidgetManager::class])
class StopForecastWidgetTapTest {

    private lateinit var context: Context
    private lateinit var application: ShadowApplication
    private lateinit var widgets: PartialUpdateShadowAppWidgetManager
    private var appWidgetId = 0

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        application = shadowOf(context as Application)
        widgets = Shadow.extract(AppWidgetManager.getInstance(context))
        appWidgetId = widgets.createWidget(StopForecastWidget::class.java, R.layout.stop_forecast_widget)

        WidgetStopStore.save(context, appWidgetId, listOf("Abbey Street", "Busáras", "Connolly"))
        Preferences.saveWidgetSelectedStopName(context, appWidgetId, "Abbey Street")
    }

    @Test
    fun `a refresh tap hands the widget's stop to WidgetFetchService`() {
        tap(StopForecastWidget.ACTION_REFRESH)

        val started = application.nextStartedService

        assertThat(started.component).isEqualTo(ComponentName(context, WidgetFetchService::class.java))
        assertThat(started.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)).isEqualTo(appWidgetId)
        assertThat(started.getStringExtra(WidgetFetchService.EXTRA_STOP_NAME)).isEqualTo("Abbey Street")
        assertThat(started.getLongExtra(WidgetFetchService.EXTRA_DELAY_MS, -1L)).isEqualTo(0L)
    }

    @Test
    fun `the next stop arrow hands over the next stop, with the debounce`() {
        tap(StopForecastWidget.ACTION_NEXT_STOP)

        val started = application.nextStartedService

        assertThat(started.getStringExtra(WidgetFetchService.EXTRA_STOP_NAME)).isEqualTo("Busáras")
        assertThat(started.getLongExtra(WidgetFetchService.EXTRA_DELAY_MS, -1L))
            .isEqualTo(StopForecastWidget.STOP_CHANGE_DEBOUNCE_MS)
    }

    @Test
    fun `the previous stop arrow wraps round to the last stop`() {
        tap(StopForecastWidget.ACTION_PREV_STOP)

        assertThat(application.nextStartedService.getStringExtra(WidgetFetchService.EXTRA_STOP_NAME)).isEqualTo("Connolly")
    }

    @Test
    fun `a tap shows placeholder rows straight away`() {
        tap(StopForecastWidget.ACTION_REFRESH)

        assertThat(widgets.holdingScreenText(appWidgetId)).isNull()
        assertThat(widgets.placeholderRowCount(appWidgetId)).isEqualTo(StopForecastWidget.MIN_TOTAL_TRAMS)
    }

    @Test
    fun `a tap sets the revert alarm before the forecast arrives`() {
        /* If Android kills the app before the service draws anything, this alarm is all that takes the placeholder rows away. */
        tap(StopForecastWidget.ACTION_NEXT_STOP)

        assertThat(shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms).hasSize(1)
    }

    @Test
    fun `the forecast the service fetches replaces the placeholder rows`() {
        tap(StopForecastWidget.ACTION_REFRESH)

        val service = Robolectric.buildService(WidgetFetchService::class.java, application.nextStartedService).create()
        service.get().apiMethods = object : ApiMethods {
            override suspend fun getStopForecast(action: String?, ver: String?, station: String?): Response<ApiTimes> =
                Response.success(ApiTimes("2026-10-04T18:00:00", "Message", null, listOf(Tram("Connolly", "Inbound", "3"))))
        }
        service.startCommand(0, 1)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(widgets.placeholderRowCount(appWidgetId)).isEqualTo(0)
        assertThat(widgets.destinations(appWidgetId)).contains("Connolly")
    }

    @Test
    fun `a refused service start shows the error row instead of placeholder rows`() {
        val refusing = object : ContextWrapper(context) {
            override fun startForegroundService(service: Intent) = throw ForegroundServiceStartNotAllowedException("refused")
        }

        StopForecastWidget().onReceive(refusing, tapIntent(StopForecastWidget.ACTION_REFRESH))

        assertThat(widgets.placeholderRowCount(appWidgetId)).isEqualTo(0)
        assertThat(widgets.destinations(appWidgetId)).contains(context.getText(R.string.widget_message_error).toString())
    }

    @Test
    fun `a start refused because battery use is Restricted says so, and a tap opens the app's settings`() {
        /* Android refuses without throwing, and holds the revert alarm too, so placeholder rows would stay until the app opened. */
        shadowOf(context.getSystemService(ActivityManager::class.java)).setBackgroundRestricted(true)

        StopForecastWidget().onReceive(startReturning(null), tapIntent(StopForecastWidget.ACTION_REFRESH))

        assertThat(widgets.holdingScreenText(appWidgetId)).isEqualTo(context.getText(R.string.widget_message_restricted).toString())

        tapHoldingScreen()

        val opened = application.nextStartedActivity
        assertThat(opened.action).isEqualTo(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        assertThat(opened.data.toString()).isEqualTo("package:${context.packageName}")
    }

    @Test
    fun `once the revert alarm fires after the Restricted message, a tap loads times again`() {
        shadowOf(context.getSystemService(ActivityManager::class.java)).setBackgroundRestricted(true)
        StopForecastWidget().onReceive(startReturning(null), tapIntent(StopForecastWidget.ACTION_REFRESH))

        StopForecastWidget().onReceive(context, tapIntent(StopForecastWidget.ACTION_TIMEOUT))
        assertThat(widgets.holdingScreenText(appWidgetId)).isEqualTo(context.getText(R.string.tap_to_load_times).toString())

        tapHoldingScreen()

        assertThat(application.nextStartedActivity).isNull()
        assertThat(application.nextStartedService.component).isEqualTo(ComponentName(context, WidgetFetchService::class.java))
    }

    @Test
    fun `a start refused for any other reason shows the error row`() {
        StopForecastWidget().onReceive(startReturning(null), tapIntent(StopForecastWidget.ACTION_REFRESH))

        assertThat(widgets.placeholderRowCount(appWidgetId)).isEqualTo(0)
        assertThat(widgets.destinations(appWidgetId)).contains(context.getText(R.string.widget_message_error).toString())
    }

    @Test
    fun `a start refused by the app op shows the error row rather than crashing`() {
        val refusing = object : ContextWrapper(context) {
            override fun startForegroundService(service: Intent) = throw SecurityException("app op denied")
        }

        StopForecastWidget().onReceive(refusing, tapIntent(StopForecastWidget.ACTION_REFRESH))

        assertThat(widgets.destinations(appWidgetId)).contains(context.getText(R.string.widget_message_error).toString())
    }

    @Test
    fun `a tap on a widget with no stops starts nothing`() {
        WidgetStopStore.delete(context, appWidgetId)
        Preferences.removeWidgetSelectedStopName(context, appWidgetId)

        tap(StopForecastWidget.ACTION_REFRESH)

        assertThat(application.nextStartedService).isNull()
        assertThat(widgets.holdingScreenText(appWidgetId)).isEqualTo(context.getText(R.string.widget_message_configure).toString())
    }

    @Test
    fun `updates from the system draw the holding screen and fetch nothing`() {
        /* The launcher sends APPWIDGET_UPDATE_OPTIONS every time it restarts. Nobody asked for tram times then. */
        val widget = StopForecastWidget()

        widget.onUpdate(context, AppWidgetManager.getInstance(context), intArrayOf(appWidgetId))
        widget.onAppWidgetOptionsChanged(context, AppWidgetManager.getInstance(context), appWidgetId, Bundle())

        assertThat(application.nextStartedService).isNull()
        assertThat(widgets.holdingScreenText(appWidgetId)).isEqualTo(context.getText(R.string.tap_to_load_times).toString())
    }

    private fun tap(action: String) = StopForecastWidget().onReceive(context, tapIntent(action))

    /**
     * Taps the holding screen as the launcher would, by clicking the view its click handler was applied to.
     */
    private fun tapHoldingScreen() {
        widgets.getViewFor(appWidgetId).findViewById<View>(R.id.holding_screen).performClick()
        shadowOf(Looper.getMainLooper()).idle()
    }

    /**
     * Wraps the app's context so that starting a foreground service returns [result] instead of starting anything.
     *
     * @param result What startForegroundService returns. Null is Android refusing without throwing.
     * @return The wrapped context.
     */
    private fun startReturning(result: ComponentName?) = object : ContextWrapper(context) {
        override fun startForegroundService(service: Intent): ComponentName? = result
    }

    private fun tapIntent(action: String) =
        Intent(context, StopForecastWidget::class.java).setAction(action).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}
