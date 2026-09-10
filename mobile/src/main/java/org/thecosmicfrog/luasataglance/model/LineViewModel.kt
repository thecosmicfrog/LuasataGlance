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

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okio.IOException
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.api.ApiMethods
import org.thecosmicfrog.luasataglance.api.ApiProvider
import org.thecosmicfrog.luasataglance.api.ApiTimes
import org.thecosmicfrog.luasataglance.util.ResourceProvider
import org.thecosmicfrog.luasataglance.util.StopForecastUtil.createStopForecast
import retrofit2.HttpException
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel backing a single Luas line.
 *
 * @param resourceProvider Resolves strings without the ViewModel holding a Context.
 * @param apiMethods       Source of stop forecasts. Defaults to the flavour's own provider, and is only ever passed explicitly by
 *                         tests, which hand in a fake rather than reaching the live API. Resolved once here rather than per
 *                         request, so the prod flavour builds one Retrofit client instead of one for every reload.
 */
class LineViewModel(
    private val resourceProvider: ResourceProvider,
    private val apiMethods: ApiMethods = ApiProvider.getApiMethods()
) : ViewModel() {

    private val logTag = LineViewModel::class.java.simpleName

    private val _status = MutableStateFlow<Status?>(null)
    val status: StateFlow<Status?> = _status.asStateFlow()

    private val _stopForecast = MutableStateFlow<StopForecast?>(null)
    val stopForecast: StateFlow<StopForecast?> = _stopForecast.asStateFlow()

    private val _stopForecastInfo = MutableStateFlow<Pair<List<StopForecastInfo>, List<StopForecastInfo>>?>(null)
    val stopForecastInfo: StateFlow<Pair<List<StopForecastInfo>, List<StopForecastInfo>>?> = _stopForecastInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showSnackbarWithTime = MutableStateFlow<String?>(null)
    val showSnackbarWithTime: StateFlow<String?> = _showSnackbarWithTime.asStateFlow()

    private var reloadJob: Job? = null
    private var loadJob: Job? = null
    private var currentStopId: String? = null

    /* Bumped per request. Only the newest may publish, so a slow reply cannot overwrite a later one. */
    private var latestRequest = 0

    override fun onCleared() {
        super.onCleared()
        stopAutoReload()
    }

    /**
     * Load the stop forecast for the given stop name and ID.
     *
     * @param stopName The name of the stop.
     * @param stopId The ID of the stop.
     * @param isRefreshing Whether or not the SwipeRefreshLayout is being refreshed.
     * @param shouldShowSnackbar Whether or not we should show a Snackbar to the user with the API created time.
     */
    fun loadStopForecast(stopName: String?, stopId: String?, isRefreshing: Boolean = false, shouldShowSnackbar: Boolean = false) {
        if (stopName.isNullOrBlank() || stopId.isNullOrBlank()) return

        currentStopId = stopId

        /* Prevent a superseded request continuing to run. */
        loadJob?.cancel()
        loadJob = viewModelScope.launch { fetchStopForecast(stopId, isRefreshing, shouldShowSnackbar) }
    }

    /**
     * Fetch one stop forecast and publish it.
     *
     * Suspends until the request finishes, so [startAutoReload] can wait for it rather than firing the next one on top.
     *
     * @param stopId The ID of the stop.
     * @param isRefreshing Whether or not the SwipeRefreshLayout is being refreshed.
     * @param shouldShowSnackbar Whether or not we should show a Snackbar to the user with the API created time.
     */
    private suspend fun fetchStopForecast(stopId: String, isRefreshing: Boolean, shouldShowSnackbar: Boolean) {
        val request = ++latestRequest
        val job = currentCoroutineContext()[Job]

        try {
            if (isRefreshing) {
                _isRefreshing.value = true
            } else {
                _isLoading.value = true
            }
            _error.value = null
            _showSnackbarWithTime.value = null

            val response = withTimeoutOrNull(FETCH_TIMEOUT_MS.milliseconds) {
                apiMethods.getStopForecast(
                    action = "times",
                    ver = "3",
                    station = stopId
                )
            }

            /* Prevent a superseded reply overwriting a newer one. */
            if (request != latestRequest) return

            if (response == null) {
                setError("Network error")

                return
            }

            if (response.isSuccessful) {
                val apiTimes = response.body()

                if (apiTimes != null) {
                    val stopForecast = createStopForecast(apiTimes)
                    _stopForecast.value = stopForecast

                    updateStatus(stopForecast)

                    _stopForecastInfo.value = processStopForecastInfo(stopForecast = stopForecast)

                    if (shouldShowSnackbar) {
                        getApiCreatedTime(apiTimes)?.let { time ->
                            _showSnackbarWithTime.value = "Times updated at $time"
                        }
                    }
                } else {
                    setError("No data received from server")
                }
            } else {
                setError("Error: ${response.code()}")
            }

        } catch (e: Exception) {
            /* Prevent LineFragment stopping the reload from being reported as a failed request. */
            if (e is CancellationException && job?.isActive != true) throw e

            Log.e(logTag, "Error loading stop forecast", e)

            /* Prevent a superseded failure erroring a newer request. */
            if (request != latestRequest) return

            when (e) {
                is IOException -> setError("Network error")
                is HttpException -> setError("Server error: ${e.code()}")
                else -> setError("Unexpected error: ${e.message}")
            }
        } finally {
            /* Prevent a superseded reply hiding the progress bar for the request still running. */
            if (request == latestRequest) {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Start auto-reloading the stop forecast at regular intervals.
     *
     * @param stopName The name of the stop.
     * @param stopNameId The ID of the stop.
     * @param delayMillis Delay before starting the auto-reload.
     * @param intervalMillis Gap between one reply and the next request, rather than between requests.
     */
    fun startAutoReload(stopName: String?, stopNameId: String?, delayMillis: Long = 0L, intervalMillis: Long = 10000L) {
        currentStopId = stopNameId

        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            delay(delayMillis.milliseconds)

            while (isActive) {
                /* Awaited. Prevent a request slower than intervalMillis having the next one fired on top of it. */
                currentStopId?.takeIf { it.isNotBlank() }
                    ?.let { fetchStopForecast(it, isRefreshing = false, shouldShowSnackbar = false) }

                delay(intervalMillis.milliseconds)
            }
        }
    }

    /**
     * Stop auto-reloading the stop forecast.
     */
    fun stopAutoReload() {
        reloadJob?.cancel()
        reloadJob = null
    }

    /**
     * Record a failed load, and put the failure on the status card.
     *
     * @param message The error to report.
     */
    private fun setError(message: String) {
        _error.value = message

        clearStopForecast(status = Status(resourceProvider.getString(R.string.message_error), true))
    }

    /**
     * Clear the stop forecast, so LineFragment draws its shimmer rows in place of the trams.
     *
     * @param status The status to show, or null to shimmer the status card too.
     */
    fun clearStopForecast(status: Status? = null) {
        _stopForecast.value = null
        _stopForecastInfo.value = null
        _status.value = status
    }

    /**
     * Update the status of the stop forecast.
     *
     * @param stopForecast The stop forecast to update.
     */
    fun updateStatus(stopForecast: StopForecast) {
        val operatingNormally = stopForecast.stopForecastStatusDirectionInbound.operatingNormally == true &&
                stopForecast.stopForecastStatusDirectionOutbound.operatingNormally == true

        val message = stopForecast.message

        _status.value = if (message.isNullOrBlank()) {
            Status(resourceProvider.getString(R.string.message_no_status), true)
        } else {
            /* A lot of Luas statuses relate to lifts being out of service. Ignore these. */
            Status(message, !(operatingNormally || message.lowercase().contains("lift")))
        }
    }

    /**
     * Process the stop forecast information and return a pair of lists for inbound and outbound trams.
     *
     * @param stopForecast The stop forecast to process.
     * @return A pair of lists containing the processed stop forecast information.
     */
    private fun processStopForecastInfo(stopForecast: StopForecast): Pair<List<StopForecastInfo>, List<StopForecastInfo>> {
        val listStopForecastInfoInbound = mutableListOf<StopForecastInfo>()
        val listStopForecastInfoOutbound = mutableListOf<StopForecastInfo>()

        if (stopForecast.inboundTrams.isEmpty()) {
            listStopForecastInfoInbound.add(
                StopForecastInfo(resourceProvider.getString(R.string.no_trams_forecast), "", "")
            )
        }

        if (stopForecast.outboundTrams.isEmpty()) {
            listStopForecastInfoOutbound.add(
                StopForecastInfo(resourceProvider.getString(R.string.no_trams_forecast), "", "")
            )
        }

        (stopForecast.inboundTrams + stopForecast.outboundTrams).forEach { tram ->
            tram.dueMinutes?.let { dueMinutes ->
                /*
                 * The API answers in English whatever language the phone is set to, so "Tallaght" becomes "Tamhlacht" here
                 * on an Irish device.
                 */
                val destination = resourceProvider.localiseApiName(tram.destination)

                val isDue = dueMinutes.equals("DUE", ignoreCase = true)

                val minOrMins = when {
                    isDue -> ""
                    dueMinutes.toIntOrNull()?.let { it > 1 } == true -> " ${resourceProvider.getString(R.string.mins)}"
                    else -> " ${resourceProvider.getString(R.string.min)}"
                }

                val stopForecastInfo = StopForecastInfo(
                    destination = destination,
                    dueMinutes = if (isDue) resourceProvider.getString(R.string.due) else dueMinutes,
                    minOrMins = minOrMins,
                    showMinOrMins = !isDue
                )

                when (tram.direction) {
                    "Inbound" -> listStopForecastInfoInbound.add(stopForecastInfo)
                    "Outbound" -> listStopForecastInfoOutbound.add(stopForecastInfo)
                }
            }
        }

        return Pair(listStopForecastInfoInbound, listStopForecastInfoOutbound)
    }

    /**
     * Get the API created time from the ApiTimes object.
     *
     * @param apiTimes The ApiTimes object containing the created time.
     * @return The formatted created time as a string.
     */
    private fun getApiCreatedTime(apiTimes: ApiTimes): String? {
        return try {
            apiTimes.createdTime?.let {
                val currentTime = SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss",
                    Locale.getDefault()
                ).parse(it)

                val dateFormat: DateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                currentTime?.let { time -> dateFormat.format(time) }
            }
        } catch (e: ParseException) {
            Log.e(logTag, "Failed to parse created time from API.")
            Log.e(logTag, e.message.toString())
            null
        }
    }

    /**
     * Get the stop ID for the given stop name.
     *
     * @param stopName The name of the stop, as displayed to the user.
     * @return The ID of the stop, or null if no stop in this language has that name.
     */
    fun getStopId(stopName: String?): String? = resourceProvider.stopId(stopName)

    companion object {
        /* Under startAutoReload's 10000L default, so a slow request cannot hold the loop past its next tick. */
        private const val FETCH_TIMEOUT_MS = 8000L
    }
}
