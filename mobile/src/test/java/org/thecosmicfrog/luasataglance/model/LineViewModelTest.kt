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
package org.thecosmicfrog.luasataglance.model

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.api.ApiMethods
import org.thecosmicfrog.luasataglance.api.ApiTimes
import org.thecosmicfrog.luasataglance.util.MainDispatcherRule
import org.thecosmicfrog.luasataglance.util.ResourceProvider
import retrofit2.Response
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

/**
 * Tests for [LineViewModel], driven by a fake [ApiMethods] rather than the live API.
 *
 * Most of these cover what happens when the server does not answer properly: a 503, an empty body, a dropped connection. The
 * Luas API returns nothing useful once the trams stop for the night, so those paths run often and are awkward to reach by hand.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LineViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var resourceProvider: ResourceProvider

    @Before
    fun setUp() {
        resourceProvider = ResourceProvider(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `a successful response becomes a stop forecast`() = runTest {
        val viewModel = viewModel(FakeApiMethods { Response.success(apiTimes(tram("Tallaght", "Inbound", "3"))) })

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        assertThat(viewModel.stopForecast.value?.inboundTrams?.map { it.destination }).containsExactly("Tallaght")
        assertThat(viewModel.error.value).isNull()
    }

    @Test
    fun `the stop ID is sent to the API, not the stop name`() = runTest {
        val api = FakeApiMethods { Response.success(apiTimes()) }
        val viewModel = viewModel(api)

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        assertThat(api.lastStation).isEqualTo("TAL")
    }

    @Test
    fun `loading flags are cleared once the load finishes`() = runTest {
        val viewModel = viewModel(FakeApiMethods { Response.success(apiTimes()) })

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        assertThat(viewModel.isLoading.value).isFalse()
        assertThat(viewModel.isRefreshing.value).isFalse()
    }

    @Test
    fun `loading flags are cleared even when the request fails`() = runTest {
        val viewModel = viewModel(FakeApiMethods { throw IOException("no network") })

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        assertThat(viewModel.isLoading.value).isFalse()
        assertThat(viewModel.isRefreshing.value).isFalse()
    }

    @Test
    fun `a pull to refresh drives the refresh spinner rather than the shimmer`() = runTest {
        /* CompletableDeferred keeps the request unfinished, so the flags can be read before the finally block clears them. */
        val inFlight = CompletableDeferred<Response<ApiTimes>>()
        val viewModel = viewModel(FakeApiMethods { inFlight.await() })

        viewModel.loadStopForecast("Tallaght", "TAL", isRefreshing = true)
        runCurrent()

        assertThat(viewModel.isRefreshing.value).isTrue()
        assertThat(viewModel.isLoading.value).isFalse()

        inFlight.complete(Response.success(apiTimes()))
        advanceUntilIdle()

        assertThat(viewModel.isRefreshing.value).isFalse()
    }

    @Test
    fun `a first load drives the shimmer rather than the refresh spinner`() = runTest {
        val inFlight = CompletableDeferred<Response<ApiTimes>>()
        val viewModel = viewModel(FakeApiMethods { inFlight.await() })

        viewModel.loadStopForecast("Tallaght", "TAL")
        runCurrent()

        assertThat(viewModel.isLoading.value).isTrue()
        assertThat(viewModel.isRefreshing.value).isFalse()

        inFlight.complete(Response.success(apiTimes()))
        advanceUntilIdle()

        assertThat(viewModel.isLoading.value).isFalse()
    }

    @Test
    fun `a blank stop is not fetched at all`() = runTest {
        val api = FakeApiMethods { Response.success(apiTimes()) }
        val viewModel = viewModel(api)

        viewModel.loadStopForecast("", "TAL")
        viewModel.loadStopForecast("Tallaght", "")
        viewModel.loadStopForecast(null, null)
        advanceUntilIdle()

        assertThat(api.callCount).isEqualTo(0)
    }

    @Test
    fun `a network failure is reported rather than thrown`() = runTest {
        val viewModel = viewModel(FakeApiMethods { throw IOException("no network") })

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        assertThat(viewModel.error.value).isEqualTo("Network error")
    }

    @Test
    fun `an unsuccessful response is reported with its code`() = runTest {
        val viewModel = viewModel(
            FakeApiMethods { Response.error(503, "".toResponseBody("text/plain".toMediaType())) }
        )

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        assertThat(viewModel.error.value).isEqualTo("Error: 503")
    }

    @Test
    fun `an empty body is reported`() = runTest {
        val viewModel = viewModel(FakeApiMethods { Response.success(null) })

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        assertThat(viewModel.error.value).isEqualTo("No data received from server")
    }

    @Test
    fun `an earlier error is cleared by a later success`() = runTest {
        var shouldFail = true
        val viewModel = viewModel(
            FakeApiMethods {
                if (shouldFail) throw IOException("no network") else Response.success(apiTimes())
            }
        )

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()
        assertThat(viewModel.error.value).isNotNull()

        shouldFail = false
        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        assertThat(viewModel.error.value).isNull()
    }

    @Test
    fun `a direction with no trams says so instead of showing nothing`() = runTest {
        val viewModel = viewModel(FakeApiMethods { Response.success(apiTimes(tram("Tallaght", "Inbound", "3"))) })

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        val (inbound, outbound) = viewModel.stopForecastInfo.value!!

        assertThat(inbound.map { it.destination }).containsExactly("Tallaght")
        assertThat(outbound.map { it.destination }).containsExactly("No trams forecast")
    }

    @Test
    fun `each direction is shown the trams belonging to it`() = runTest {
        /* processStopForecastInfo sorts by direction again, separately from createStopForecast, so both need their own test. */
        val viewModel = viewModel(
            FakeApiMethods {
                Response.success(
                    apiTimes(
                        tram("Tallaght", "Inbound", "3"),
                        tram("Saggart", "Outbound", "6"),
                        tram("Connolly", "Inbound", "11")
                    )
                )
            }
        )

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        val (inbound, outbound) = viewModel.stopForecastInfo.value!!

        assertThat(inbound.map { it.destination }).containsExactly("Tallaght", "Connolly").inOrder()
        assertThat(outbound.map { it.destination }).containsExactly("Saggart")
    }

    @Test
    fun `a tram in an unrecognised direction is shown in neither`() = runTest {
        val viewModel = viewModel(
            FakeApiMethods { Response.success(apiTimes(tram("Tallaght", "Inbound", "3"), tram("Nowhere", "Sideways", "4"))) }
        )

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        val (inbound, outbound) = viewModel.stopForecastInfo.value!!

        assertThat(inbound.map { it.destination }).containsExactly("Tallaght")
        assertThat(outbound.map { it.destination }).containsExactly("No trams forecast")
    }

    @Test
    fun `a tram that is due shows the due label and no unit`() = runTest {
        val viewModel = viewModel(FakeApiMethods { Response.success(apiTimes(tram("Tallaght", "Inbound", "DUE"))) })

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        val inbound = viewModel.stopForecastInfo.value!!.first.single()

        assertThat(inbound.dueMinutes).isEqualTo("DUE")
        assertThat(inbound.showMinOrMins).isFalse()
        assertThat(inbound.minOrMins).isEmpty()
    }

    @Test
    fun `one minute is singular and more than one is plural`() = runTest {
        val viewModel = viewModel(
            FakeApiMethods {
                Response.success(apiTimes(tram("Tallaght", "Inbound", "1"), tram("Saggart", "Inbound", "9")))
            }
        )

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        val inbound = viewModel.stopForecastInfo.value!!.first

        assertThat(inbound[0].minOrMins).isEqualTo(" min")
        assertThat(inbound[1].minOrMins).isEqualTo(" mins")
    }

    @Test
    @Config(qualifiers = "ga")
    fun `destinations are translated under an Irish locale`() = runTest {
        /* The API says "Tallaght" whatever language the phone is set to, so LineViewModel translates it. */
        val viewModel = viewModel(FakeApiMethods { Response.success(apiTimes(tram("Tallaght", "Inbound", "3"))) })

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        assertThat(viewModel.stopForecastInfo.value!!.first.single().destination).isEqualTo("Tamhlacht")
    }

    @Test
    fun `destinations are left alone under any other locale`() = runTest {
        val viewModel = viewModel(FakeApiMethods { Response.success(apiTimes(tram("Tallaght", "Inbound", "3"))) })

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        assertThat(viewModel.stopForecastInfo.value!!.first.single().destination).isEqualTo("Tallaght")
    }

    @Test
    @Config(qualifiers = "ga")
    fun `a destination the app does not know is shown as the API sent it`() = runTest {
        val viewModel = viewModel(FakeApiMethods { Response.success(apiTimes(tram("Not A Stop", "Inbound", "3"))) })

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        assertThat(viewModel.stopForecastInfo.value!!.first.single().destination).isEqualTo("Not A Stop")
    }

    @Test
    fun `the snackbar time is only set when shouldShowSnackbar is true`() = runTest {
        val viewModel = viewModel(FakeApiMethods { Response.success(apiTimes()) })

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()
        assertThat(viewModel.showSnackbarWithTime.value).isNull()

        viewModel.loadStopForecast("Tallaght", "TAL", shouldShowSnackbar = true)
        advanceUntilIdle()
        assertThat(viewModel.showSnackbarWithTime.value).isNotNull()
    }

    @Test
    fun `an unparseable created time does not stop the forecast loading`() = runTest {
        val viewModel = viewModel(FakeApiMethods { Response.success(apiTimes(createdTime = "not a date")) })

        viewModel.loadStopForecast("Tallaght", "TAL", shouldShowSnackbar = true)
        advanceUntilIdle()

        assertThat(viewModel.error.value).isNull()
        assertThat(viewModel.showSnackbarWithTime.value).isNull()
    }

    @Test
    fun `each reload replaces the times on screen`() = runTest {
        /* The API returns 9, then 8, then 7 minutes. Checks the newest of those reaches the screen. */
        var dueMinutes = 9
        val api = FakeApiMethods { Response.success(apiTimes(tram("Tallaght", "Inbound", (dueMinutes--).toString()))) }
        val viewModel = viewModel(api)

        viewModel.startAutoReload("Tallaght", "TAL", intervalMillis = 1000L)
        advanceTimeBy(2500L.milliseconds)

        assertThat(viewModel.stopForecast.value?.inboundTrams?.single()?.dueMinutes).isEqualTo("7")
        assertThat(viewModel.stopForecastInfo.value?.first?.single()?.dueMinutes).isEqualTo("7")

        viewModel.stopAutoReload()
    }

    @Test
    fun `auto reload refreshes as soon as it starts, and then once per interval`() = runTest {
        val api = FakeApiMethods { Response.success(apiTimes()) }
        val viewModel = viewModel(api)

        viewModel.startAutoReload("Tallaght", "TAL", intervalMillis = 1000L)
        advanceTimeBy(3500L.milliseconds)

        /* Fetches at 0, 1000, 2000, and 3000 milliseconds. If it waited before the first fetch there would be three. */
        assertThat(api.callCount).isEqualTo(4)

        viewModel.stopAutoReload()
    }

    @Test
    fun `a starting delay holds off the first refresh`() = runTest {
        val api = FakeApiMethods { Response.success(apiTimes()) }
        val viewModel = viewModel(api)

        viewModel.startAutoReload("Tallaght", "TAL", delayMillis = 5000L, intervalMillis = 1000L)
        advanceTimeBy(4999L.milliseconds)

        assertThat(api.callCount).isEqualTo(0)

        advanceTimeBy(2L.milliseconds)

        assertThat(api.callCount).isEqualTo(1)

        viewModel.stopAutoReload()
    }

    @Test
    fun `stopping auto reload stops the polling`() = runTest {
        /*
         * LineFragment stops the loop in onPause(), onStop(), and onCleared(). If it did not, the app would keep hitting the API.
         */
        val api = FakeApiMethods { Response.success(apiTimes()) }
        val viewModel = viewModel(api)

        viewModel.startAutoReload("Tallaght", "TAL", intervalMillis = 1000L)
        advanceTimeBy(2500L.milliseconds)
        val callsWhileRunning = api.callCount

        viewModel.stopAutoReload()
        advanceTimeBy(60000L.milliseconds)

        assertThat(api.callCount).isEqualTo(callsWhileRunning)
    }

    @Test
    fun `restarting auto reload leaves only the new loop running`() = runTest {
        /* Picking a different stop restarts the loop. If the old one kept running, the app would fetch twice as often. */
        val api = FakeApiMethods { Response.success(apiTimes()) }
        val viewModel = viewModel(api)

        viewModel.startAutoReload("Tallaght", "TAL", intervalMillis = 1000L)
        advanceTimeBy(2500L.milliseconds)
        viewModel.startAutoReload("Saggart", "SAG", intervalMillis = 1000L)
        advanceTimeBy(2500L.milliseconds)

        /* Three from the Tallaght loop, then three from the Saggart one. A leaked first loop would make it eight. */
        assertThat(api.callCount).isEqualTo(6)
        assertThat(api.lastStation).isEqualTo("SAG")

        viewModel.stopAutoReload()
    }

    @Test
    fun `status is not an error when both directions are operating normally`() {
        val viewModel = viewModel(FakeApiMethods { Response.success(apiTimes()) })

        viewModel.updateStatus(stopForecast("Services operating normally", operatingNormally = true))

        assertThat(viewModel.status.value?.isError).isFalse()
        assertThat(viewModel.status.value?.message).isEqualTo("Services operating normally")
    }

    @Test
    fun `status is an error when a direction is not operating normally`() {
        val viewModel = viewModel(FakeApiMethods { Response.success(apiTimes()) })

        viewModel.updateStatus(stopForecast("Severe delays southbound", operatingNormally = false))

        assertThat(viewModel.status.value?.isError).isTrue()
    }

    @Test
    fun `a lift outage is not treated as a tram running error`() {
        /* A lift being broken is not a tram running problem, and most Luas statuses are about lifts. */
        val viewModel = viewModel(FakeApiMethods { Response.success(apiTimes()) })

        viewModel.updateStatus(stopForecast("The Lift at Connolly is out of service", operatingNormally = false))

        assertThat(viewModel.status.value?.isError).isFalse()
    }

    @Test
    fun `a stop ID can be looked up from the name shown to the user`() {
        val viewModel = viewModel(FakeApiMethods { Response.success(apiTimes()) })

        assertThat(viewModel.getStopId("Tallaght")).isEqualTo("TAL")
        assertThat(viewModel.getStopId("Not A Stop")).isNull()
    }

    @Test
    @Config(qualifiers = "ga")
    fun `a stop ID can be looked up from the Irish name`() {
        val viewModel = viewModel(FakeApiMethods { Response.success(apiTimes()) })

        assertThat(viewModel.getStopId("Tamhlacht")).isEqualTo("TAL")
    }

    @Test
    fun `a successful load sets the status without anything else asking it to`() = runTest {
        val viewModel = viewModel(FakeApiMethods { Response.success(apiTimes(status = normalStatus())) })

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        assertThat(viewModel.status.value).isEqualTo(Status("Message", false))
    }

    @Test
    fun `a failed load replaces the status with the error message`() = runTest {
        val viewModel = viewModel(FakeApiMethods { throw IOException("no network") })

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        assertThat(viewModel.status.value?.isError).isTrue()
        assertThat(viewModel.status.value?.message).isEqualTo(resourceProvider.getString(R.string.message_error))
    }

    @Test
    fun `an unsuccessful response also reaches the status card`() = runTest {
        val viewModel = viewModel(
            FakeApiMethods { Response.error(503, "".toResponseBody("text/plain".toMediaType())) }
        )

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        assertThat(viewModel.status.value?.isError).isTrue()
    }

    @Test
    fun `an empty body also reaches the status card`() = runTest {
        val viewModel = viewModel(FakeApiMethods { Response.success(null) })

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        assertThat(viewModel.status.value?.isError).isTrue()
    }

    @Test
    fun `a recovery is emitted even though the Luas status message never changed`() = runTest {
        var shouldFail = false
        val viewModel = viewModel(
            FakeApiMethods {
                if (shouldFail) throw IOException("no network") else Response.success(apiTimes(status = normalStatus()))
            }
        )

        /* UnconfinedTestDispatcher records each value as it is set. advanceUntilIdle() leaves a queued collector behind. */
        val seen = mutableListOf<Status?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.status.collect { seen.add(it) } }

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        shouldFail = true
        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        shouldFail = false
        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        assertThat(seen).containsExactly(
            null,
            Status("Message", false),
            Status(resourceProvider.getString(R.string.message_error), true),
            Status("Message", false)
        ).inOrder()
    }

    @Test
    fun `a response with no message says so rather than keeping the last status`() = runTest {
        var shouldFail = true
        val viewModel = viewModel(
            FakeApiMethods {
                if (shouldFail) throw IOException("no network") else Response.success(apiTimes(message = null))
            }
        )

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        shouldFail = false
        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        assertThat(viewModel.status.value?.message).isEqualTo(resourceProvider.getString(R.string.message_no_status))
    }

    @Test
    fun `a cleared forecast is drawn again even though the API repeated the same trams`() = runTest {
        val viewModel = viewModel(
            FakeApiMethods { Response.success(apiTimes(tram("The Point", "Inbound", "5"), status = normalStatus())) }
        )

        /* UnconfinedTestDispatcher records each value as it is set. advanceUntilIdle() leaves a queued collector behind. */
        val seen = mutableListOf<Pair<List<StopForecastInfo>, List<StopForecastInfo>>?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.stopForecastInfo.collect { seen.add(it) } }

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        viewModel.clearStopForecast()

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        /* The null between the two forecasts is the point. Without it StateFlow drops the second one and the shimmer stays. */
        assertThat(seen.map { it?.first?.size }).containsExactly(null, 1, null, 1).inOrder()
    }

    @Test
    fun `clearing the forecast empties both the forecast and its display rows`() = runTest {
        val viewModel = viewModel(
            FakeApiMethods { Response.success(apiTimes(tram("The Point", "Inbound", "5"), status = normalStatus())) }
        )

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        viewModel.clearStopForecast()

        assertThat(viewModel.stopForecast.value).isNull()
        assertThat(viewModel.stopForecastInfo.value).isNull()
    }

    @Test
    fun `a failed load clears the forecast rather than leaving stale trams on screen`() = runTest {
        var shouldFail = false
        val viewModel = viewModel(
            FakeApiMethods {
                if (shouldFail) throw IOException("no network")
                else Response.success(apiTimes(tram("The Point", "Inbound", "5"), status = normalStatus()))
            }
        )

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()
        assertThat(viewModel.stopForecastInfo.value).isNotNull()

        shouldFail = true
        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceUntilIdle()

        assertThat(viewModel.stopForecastInfo.value).isNull()
    }

    @Test
    fun `a reply for the previous stop is dropped rather than drawn under the new one`() = runTest {
        val tallaght = CompletableDeferred<Response<ApiTimes>>()
        val api = FakeApiMethods { station ->
            if (station == "TAL") tallaght.await()
            else Response.success(apiTimes(tram("Connolly", "Inbound", "3"), status = normalStatus()))
        }
        val viewModel = viewModel(api)

        viewModel.loadStopForecast("Tallaght", "TAL")
        runCurrent()

        /* Without this the test would pass green even if the first request never went out. */
        assertThat(api.callCount).isEqualTo(1)

        /* Switch stop while the first is still unanswered. */
        viewModel.loadStopForecast("Jervis", "JER")
        advanceUntilIdle()

        val jervis = viewModel.stopForecastInfo.value

        tallaght.complete(Response.success(apiTimes(tram("The Point", "Inbound", "9"), status = normalStatus())))
        advanceUntilIdle()

        assertThat(viewModel.stopForecastInfo.value).isEqualTo(jervis)
    }

    @Test
    fun `a failure for the previous stop does not replace the new one with an error`() = runTest {
        val tallaght = CompletableDeferred<Response<ApiTimes>>()
        val api = FakeApiMethods { station ->
            if (station == "TAL") tallaght.await()
            else Response.success(apiTimes(tram("Connolly", "Inbound", "3"), status = normalStatus()))
        }
        val viewModel = viewModel(api)

        viewModel.loadStopForecast("Tallaght", "TAL")
        runCurrent()

        /* Without this the test would pass green even if the first request never went out. */
        assertThat(api.callCount).isEqualTo(1)

        viewModel.loadStopForecast("Jervis", "JER")
        advanceUntilIdle()

        tallaght.completeExceptionally(IOException("no network"))
        advanceUntilIdle()

        assertThat(viewModel.stopForecastInfo.value).isNotNull()
        assertThat(viewModel.status.value?.isError).isFalse()
    }

    @Test
    fun `auto reload waits for the reply instead of firing the next request on top of it`() = runTest {
        val inFlight = CompletableDeferred<Response<ApiTimes>>()
        val api = FakeApiMethods { inFlight.await() }
        val viewModel = viewModel(api)

        viewModel.startAutoReload("Tallaght", "TAL", intervalMillis = 1000L)

        /* Four intervals pass with the first request still unanswered. */
        advanceTimeBy(4500.milliseconds)
        runCurrent()

        assertThat(api.callCount).isEqualTo(1)

        inFlight.complete(Response.success(apiTimes(tram("Connolly", "Inbound", "3"), status = normalStatus())))
        advanceTimeBy(1100.milliseconds)
        runCurrent()

        assertThat(api.callCount).isEqualTo(2)

        viewModel.stopAutoReload()
    }

    @Test
    fun `stopping auto reload cancels the request already in flight`() = runTest {
        val inFlight = CompletableDeferred<Response<ApiTimes>>()
        val api = FakeApiMethods { inFlight.await() }
        val viewModel = viewModel(api)

        viewModel.startAutoReload("Tallaght", "TAL", intervalMillis = 1000L)
        runCurrent()
        assertThat(api.callCount).isEqualTo(1)

        viewModel.stopAutoReload()
        advanceUntilIdle()

        /* The reply lands after the fragment has gone. Nothing should be published, and no error shown. */
        inFlight.complete(Response.success(apiTimes(tram("Connolly", "Inbound", "3"), status = normalStatus())))
        advanceUntilIdle()

        assertThat(viewModel.stopForecastInfo.value).isNull()
        assertThat(viewModel.status.value).isNull()
    }

    @Test
    fun `an older reply for the same stop cannot overwrite a newer one`() = runTest {
        val first = CompletableDeferred<Response<ApiTimes>>()
        val second = CompletableDeferred<Response<ApiTimes>>()
        var call = 0
        val api = FakeApiMethods {
            call++
            if (call == 1) first.await() else second.await()
        }
        val viewModel = viewModel(api)

        /* A long interval so the loop fires once and the test drives the ordering itself. */
        viewModel.startAutoReload("Tallaght", "TAL", intervalMillis = 600000L)
        runCurrent()
        assertThat(api.callCount).isEqualTo(1)

        /* A pull to refresh on the same stop, issued while the loop's request is still out. */
        viewModel.loadStopForecast("Tallaght", "TAL", isRefreshing = true)
        runCurrent()
        assertThat(api.callCount).isEqualTo(2)

        second.complete(Response.success(apiTimes(tram("Connolly", "Inbound", "3"), status = normalStatus())))
        runCurrent()

        /* The loop's older reply lands last, and says the tram is further away than the newer one did. */
        first.complete(Response.success(apiTimes(tram("Connolly", "Inbound", "4"), status = normalStatus())))
        runCurrent()

        assertThat(viewModel.stopForecast.value?.inboundTrams?.map { it.dueMinutes }).containsExactly("3")

        viewModel.stopAutoReload()
    }

    @Test
    fun `a request slower than the fetch timeout is reported rather than waited on`() = runTest {
        /* Slower than the 8000ms fetch timeout, so the timeout wins and the late reply is never published. */
        val api = FakeApiMethods {
            delay(9000.milliseconds)
            Response.success(apiTimes(tram("Connolly", "Inbound", "3"), status = normalStatus()))
        }
        val viewModel = viewModel(api)

        viewModel.loadStopForecast("Tallaght", "TAL")
        advanceTimeBy(8500.milliseconds)
        runCurrent()

        assertThat(viewModel.error.value).isEqualTo("Network error")

        advanceUntilIdle()

        assertThat(viewModel.stopForecast.value).isNull()
    }

    private fun viewModel(apiMethods: ApiMethods) = LineViewModel(resourceProvider, apiMethods)

    private fun tram(destination: String, direction: String, dueMinutes: String) =
        Tram(destination = destination, direction = direction, dueMinutes = dueMinutes)

    private fun apiTimes(
        vararg trams: Tram,
        createdTime: String? = "2026-08-19T18:00:00",
        message: String? = "Message",
        status: StopForecastStatus = StopForecastStatus()
    ) =
        ApiTimes(
            createdTime = createdTime,
            message = message,
            stopForecastStatus = status,
            trams = trams.toList()
        )

    /* StopForecastStatus defaults operatingNormally to false, which updateStatus() reads as an error. */
    private fun normalStatus() =
        StopForecastStatus(
            StopForecastStatusDirection("Message", true, true),
            StopForecastStatusDirection("Message", true, true)
        )

    private fun stopForecast(message: String, operatingNormally: Boolean) =
        StopForecast().apply {
            this.message = message
            stopForecastStatusDirectionInbound = StopForecastStatusDirection(message, true, operatingNormally)
            stopForecastStatusDirectionOutbound = StopForecastStatusDirection(message, true, operatingNormally)
        }

    /**
     * Stands in for the Retrofit-backed API.
     *
     * @param response Supplies the response for each call, so a test can change it between calls or throw from it.
     */
    private class FakeApiMethods(private val response: suspend (station: String?) -> Response<ApiTimes>) : ApiMethods {

        var callCount = 0
        var lastStation: String? = null

        override suspend fun getStopForecast(action: String?, ver: String?, station: String?): Response<ApiTimes> {
            callCount++
            lastStation = station

            return response(station)
        }
    }
}
