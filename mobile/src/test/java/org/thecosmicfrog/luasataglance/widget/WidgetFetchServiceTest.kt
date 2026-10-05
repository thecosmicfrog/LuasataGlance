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

import android.app.AlarmManager
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.api.ApiMethods
import org.thecosmicfrog.luasataglance.api.ApiTimes
import org.thecosmicfrog.luasataglance.model.Tram
import retrofit2.Response
import java.io.IOException
import java.time.Duration

/**
 * Tests for [WidgetFetchService], driven by a fake [ApiMethods]. Starts call onStartCommand directly, because
 * ServiceController.startCommand runs the main looper, and some tests need two starts before either fetch runs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [PartialUpdateShadowAppWidgetManager::class])
class WidgetFetchServiceTest {

    private lateinit var context: Context
    private lateinit var widgets: PartialUpdateShadowAppWidgetManager
    private lateinit var controller: ServiceController<WidgetFetchService>
    private lateinit var api: FakeApiMethods
    private var appWidgetId = 0
    private var nextStartId = 1

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        widgets = Shadow.extract(AppWidgetManager.getInstance(context))
        appWidgetId = widgets.createWidget(StopForecastWidget::class.java, R.layout.stop_forecast_widget)

        api = FakeApiMethods { Response.success(apiTimes()) }
        controller = Robolectric.buildService(WidgetFetchService::class.java).create()
        controller.get().apiMethods = api
    }

    @Test
    fun `a tap fetches the widget's stop straight away`() {
        start("Abbey Street")
        idle()

        assertThat(api.stations).containsExactly("ABB")
    }

    @Test
    fun `the forecast reaches the widget`() {
        controller.get().apiMethods =
            FakeApiMethods { Response.success(apiTimes(Tram("Connolly", "Inbound", "3"), Tram("Saggart", "Outbound", "5"))) }

        start("Abbey Street")
        idle()

        assertThat(widgets.destinations(appWidgetId)).containsAtLeast("Connolly", "Saggart").inOrder()
    }

    @Test
    fun `a stop change waits out the debounce before fetching`() {
        start("Busáras", StopForecastWidget.STOP_CHANGE_DEBOUNCE_MS)

        advance(StopForecastWidget.STOP_CHANGE_DEBOUNCE_MS - 1)
        assertThat(api.stations).isEmpty()

        advance(1)
        assertThat(api.stations).containsExactly("BUS")
    }

    @Test
    fun `stepping on before the debounce ends fetches only the stop it lands on`() {
        /* Stepping from Abbey Street to Connolly passes Busáras, which is never fetched. */
        start("Busáras", StopForecastWidget.STOP_CHANGE_DEBOUNCE_MS)
        advance(300)
        start("Connolly", StopForecastWidget.STOP_CHANGE_DEBOUNCE_MS)
        advance(2000)

        assertThat(api.stations).containsExactly("CON")
    }

    @Test
    fun `a tap on one widget does not cancel another widget's fetch`() {
        val otherWidgetId = widgets.createWidget(StopForecastWidget::class.java, R.layout.stop_forecast_widget)

        start("Busáras", StopForecastWidget.STOP_CHANGE_DEBOUNCE_MS)
        start("Ranelagh", StopForecastWidget.STOP_CHANGE_DEBOUNCE_MS, otherWidgetId)
        advance(2000)

        assertThat(api.stations).containsExactly("BUS", "RAN")
    }

    @Test
    fun `every start goes into the foreground`() {
        start("Abbey Street")

        assertThat(shadowOf(controller.get()).lastForegroundNotification).isNotNull()
    }

    @Test
    fun `a start with no stop still goes into the foreground, and then stops`() {
        val intent = WidgetFetchService.intent(context, appWidgetId, "Abbey Street", 0L).apply {
            removeExtra(WidgetFetchService.EXTRA_STOP_NAME)
        }

        controller.get().onStartCommand(intent, 0, nextStartId++)

        assertThat(shadowOf(controller.get()).lastForegroundNotification).isNotNull()
        assertThat(shadowOf(controller.get()).isStoppedBySelf).isTrue()
    }

    @Test
    fun `the notification is on a silent channel`() {
        start("Abbey Street")

        val notification = shadowOf(controller.get()).lastForegroundNotification
        val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(notification.channelId)

        assertThat(channel.importance).isAtMost(NotificationManager.IMPORTANCE_LOW)
        assertThat(channel.name.toString()).isEqualTo(context.getString(R.string.widget_notification_channel))
    }

    @Test
    fun `the service stops once the fetch is done`() {
        start("Abbey Street")
        idle()

        assertThat(shadowOf(controller.get()).isStoppedBySelf).isTrue()
    }

    @Test
    fun `the service keeps running while a fetch is in flight`() {
        val response = CompletableDeferred<Response<ApiTimes>>()
        controller.get().apiMethods = FakeApiMethods { response.await() }

        start("Abbey Street")
        idle()
        assertThat(shadowOf(controller.get()).isStoppedBySelf).isFalse()

        response.complete(Response.success(apiTimes()))
        idle()
        assertThat(shadowOf(controller.get()).isStoppedBySelf).isTrue()
    }

    @Test
    fun `the service keeps running while another widget's fetch is still waiting`() {
        val otherWidgetId = widgets.createWidget(StopForecastWidget::class.java, R.layout.stop_forecast_widget)

        start("Abbey Street")
        start("Ranelagh", StopForecastWidget.STOP_CHANGE_DEBOUNCE_MS, otherWidgetId)
        idle()

        assertThat(api.stations).containsExactly("ABB")
        assertThat(shadowOf(controller.get()).isStoppedBySelf).isFalse()
    }

    @Test
    fun `a replaced fetch does not stop the service under the one replacing it`() {
        start("Busáras", StopForecastWidget.STOP_CHANGE_DEBOUNCE_MS)
        start("Connolly", StopForecastWidget.STOP_CHANGE_DEBOUNCE_MS)

        /* The cancelled Busáras fetch finishes here. */
        idle()
        assertThat(shadowOf(controller.get()).isStoppedBySelf).isFalse()

        advance(StopForecastWidget.STOP_CHANGE_DEBOUNCE_MS)
        assertThat(api.stations).containsExactly("CON")
    }

    @Test
    fun `the service stops with the latest start, so a tap that arrives as it stops is not dropped`() {
        /* Busáras starts first and finishes last, so stopping with its own start ID would give 1. */
        start("Busáras", StopForecastWidget.STOP_CHANGE_DEBOUNCE_MS)
        start("Ranelagh", 0L, widgets.createWidget(StopForecastWidget::class.java, R.layout.stop_forecast_widget))
        advance(StopForecastWidget.STOP_CHANGE_DEBOUNCE_MS)

        assertThat(api.stations).containsExactly("RAN", "BUS").inOrder()
        assertThat(shadowOf(controller.get()).stopSelfId).isEqualTo(2)
    }

    @Test
    fun `a request cancelled by a newer tap does not show the error row`() {
        /* An error row would cover the newer tap's placeholder rows. */
        controller.get().apiMethods = FakeApiMethods { awaitCancellation() }

        start("Abbey Street")
        idle()
        start("Busáras", StopForecastWidget.STOP_CHANGE_DEBOUNCE_MS)
        idle()

        assertThat(widgets.destinations(appWidgetId)).doesNotContain(context.getText(R.string.widget_message_error).toString())
    }

    @Test
    fun `a request that fails shows the error row`() {
        controller.get().apiMethods = FakeApiMethods { throw IOException("no network") }

        start("Abbey Street")
        idle()

        assertThat(widgets.destinations(appWidgetId)).contains(context.getText(R.string.widget_message_error).toString())
    }

    @Test
    fun `a server error shows the error row`() {
        controller.get().apiMethods = FakeApiMethods { Response.error(503, "".toResponseBody("text/plain".toMediaType())) }

        start("Abbey Street")
        idle()

        assertThat(widgets.destinations(appWidgetId)).contains(context.getText(R.string.widget_message_error).toString())
    }

    @Test
    fun `a request that never answers ends at the error row and the service stops`() {
        controller.get().apiMethods = FakeApiMethods { awaitCancellation() }

        start("Abbey Street")
        advance(WidgetFetchService.FETCH_TIMEOUT_MS - 1)
        assertThat(widgets.destinations(appWidgetId)).doesNotContain(context.getText(R.string.widget_message_error).toString())

        advance(1)
        assertThat(widgets.destinations(appWidgetId)).contains(context.getText(R.string.widget_message_error).toString())
        assertThat(shadowOf(controller.get()).isStoppedBySelf).isTrue()
    }

    @Test
    fun `a forecast sets the revert alarm`() {
        start("Abbey Street")
        idle()

        assertThat(shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms).hasSize(1)
    }

    @Test
    fun `an error sets the revert alarm too`() {
        controller.get().apiMethods = FakeApiMethods { throw IOException("no network") }

        start("Abbey Street")
        idle()

        assertThat(shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms).hasSize(1)
    }

    @Test
    fun `the 3 minute timeout stops the service and abandons a fetch still waiting`() {
        start("Busáras", StopForecastWidget.STOP_CHANGE_DEBOUNCE_MS)

        controller.get().onTimeout(nextStartId - 1)
        advance(2000)

        assertThat(shadowOf(controller.get()).isStoppedBySelf).isTrue()
        assertThat(api.stations).isEmpty()
    }

    /**
     * Delivers a start to the service, as [WidgetFetchService.start] would.
     *
     * @param stopName    Stop to fetch.
     * @param delayMs     Debounce before the fetch.
     * @param widgetId    Widget the fetch is for.
     */
    private fun start(stopName: String, delayMs: Long = 0L, widgetId: Int = appWidgetId) {
        controller.get().onStartCommand(WidgetFetchService.intent(context, widgetId, stopName, delayMs), 0, nextStartId++)
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun advance(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    private fun apiTimes(vararg trams: Tram) =
        ApiTimes(createdTime = "2026-10-04T18:00:00", message = "Message", stopForecastStatus = null, trams = trams.toList())

    private class FakeApiMethods(private val response: suspend (station: String?) -> Response<ApiTimes>) : ApiMethods {

        val stations = mutableListOf<String?>()

        override suspend fun getStopForecast(action: String?, ver: String?, station: String?): Response<ApiTimes> {
            stations.add(station)

            return response(station)
        }
    }
}
