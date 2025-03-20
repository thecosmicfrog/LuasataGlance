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
 * along with Luas at a Glance.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package org.thecosmicfrog.luasataglance.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ProgressBar
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.api.ApiMethods
import org.thecosmicfrog.luasataglance.api.ApiTimes
import org.thecosmicfrog.luasataglance.model.*
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.Preferences
import org.thecosmicfrog.luasataglance.util.StopForecastUtil.createStopForecast
import org.thecosmicfrog.luasataglance.util.StopForecastUtil.displayTutorial
import org.thecosmicfrog.luasataglance.util.StopForecastUtil.showSnackbar
import org.thecosmicfrog.luasataglance.view.SpinnerCardView
import org.thecosmicfrog.luasataglance.view.StatusCardView
import retrofit.Callback
import retrofit.RestAdapter
import retrofit.RetrofitError
import retrofit.client.Response
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class LineFragment : Fragment() {

    private val logTag = LineFragment::class.java.simpleName
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val favouriteStopName = intent?.getStringExtra(Constant.INTENT_EXTRA_STOP_NAME)

            spinnerCardView?.setSelection(favouriteStopName)
        }
    }

    private var act: FragmentActivity? = null
    private var ctx: Context? = null
    private var rootView: View? = null
    private var tabLayout: TabLayout? = null
    private var progressBar: ProgressBar? = null
    private var spinnerCardView: SpinnerCardView? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var scrollView: NestedScrollView? = null
    private var statusCardView: StatusCardView? = null
    private var isInitialised = false
    private var timerTaskReload: TimerTask? = null
    private var shouldAutoReload = false
    private var line: String? = null
    private var isVisibleToUser = false
    private var recyclerViewStopForecastsInbound: RecyclerView? = null
    private var recyclerViewStopForecastsOutbound: RecyclerView? = null
    private var linearLayoutManagerInbound: LinearLayoutManager? = null
    private var linearLayoutManagerOutbound: LinearLayoutManager? = null
    private var mapStopIdLine: StopIdLineMap? = null

    companion object {
        private var resLayoutFragmentLine: Int? = 0
        private var resProgressBar: Int? = 0
        private var resSpinnerCardView: Int? = 0
        private var resStatusCardView: Int? = 0
        private var resSwipeRefreshLayout: Int? = 0
        private var resScrollView: Int? = 0
        private var resArrayStopsRedLine: Int? = 0
        private var resArrayStopsGreenLine: Int? = 0
        private var mapStopNameId: StopNameIdMap? = null
        private var localeDefault: String? = null

        fun newInstance(line: String?): LineFragment {
            val lineFragment = LineFragment()

            val bundle = Bundle()
            bundle.putInt(
                Constant.RES_ARRAY_STOPS_RED_LINE,
                R.array.array_stops_redline
            )
            bundle.putInt(
                Constant.RES_ARRAY_STOPS_GREEN_LINE,
                R.array.array_stops_greenline
            )

            when (line) {
                Constant.RED_LINE -> {
                    bundle.putString(Constant.LINE, Constant.RED_LINE)
                    bundle.putInt(Constant.RES_LAYOUT_FRAGMENT_LINE, R.layout.fragment_redline)
                    bundle.putInt(Constant.RES_PROGRESSBAR, R.id.redline_progressbar)
                    bundle.putInt(Constant.RES_SPINNER_CARDVIEW, R.id.redline_spinner_card_view)
                    bundle.putInt(Constant.RES_STATUS_CARDVIEW, R.id.redline_statuscardview)
                    bundle.putInt(
                        Constant.RES_SWIPEREFRESHLAYOUT,
                        R.id.redline_swiperefreshlayout
                    )
                    bundle.putInt(Constant.RES_SCROLLVIEW, R.id.redline_scrollview)
                    bundle.putInt(
                        Constant.RES_STOPFORECASTCONSTRAINTLAYOUT,
                        R.id.redline_stopforecastconstraintlayout
                    )
                }

                Constant.GREEN_LINE -> {
                    bundle.putString(Constant.LINE, Constant.GREEN_LINE)
                    bundle.putInt(Constant.RES_LAYOUT_FRAGMENT_LINE, R.layout.fragment_greenline)
                    bundle.putInt(Constant.RES_PROGRESSBAR, R.id.greenline_progressbar)
                    bundle.putInt(Constant.RES_SPINNER_CARDVIEW, R.id.greenline_spinner_card_view)
                    bundle.putInt(Constant.RES_STATUS_CARDVIEW, R.id.greenline_statuscardview)
                    bundle.putInt(
                        Constant.RES_SWIPEREFRESHLAYOUT,
                        R.id.greenline_swiperefreshlayout)
                    bundle.putInt(Constant.RES_SCROLLVIEW, R.id.greenline_scrollview)
                    bundle.putInt(
                        Constant.RES_STOPFORECASTCONSTRAINTLAYOUT,
                        R.id.greenline_stopforecastconstraintlayout
                    )
                }

                Constant.NO_LINE -> Log.e(
                    LineFragment::class.java.simpleName, "No line specified."
                )

                else ->
                    /* If for some reason the line doesn't make sense. */
                    Log.wtf(LineFragment::class.java.simpleName, "Invalid line specified.")
            }

            lineFragment.arguments = bundle

            return lineFragment
        }
    }

    override fun onAttach(c: Context) {
        super.onAttach(c)
        ctx = c
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initFragmentVars()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        /* Inflate the layout for this fragment. */
        rootView = resLayoutFragmentLine?.let { inflater.inflate(it, container, false) }

        /* Initialise correct locale. */
        localeDefault = Locale.getDefault().toString()

        /* Instantiate a new StopNameIdMap. */
        mapStopNameId = StopNameIdMap(localeDefault)

        mapStopIdLine = StopIdLineMap()

        return rootView
    }

    override fun onPause() {
        super.onPause()

        /* Stop the auto-reload TimerTask. */
        timerTaskReload?.cancel()
    }

    override fun onResume() {
        val intentExtraActivityToOpen = "activityToOpen"

        super.onResume()

        act = activity

        /* Remove Favourites tutorial if it has been completed once already. */
        if (line == Constant.RED_LINE && Preferences.hasRunOnce(ctx, Constant.TUTORIAL_FAVOURITES))
        {
            rootView?.let {
                displayTutorial(it, Constant.RED_LINE, Constant.TUTORIAL_FAVOURITES, false)
            }
        }

        if (isAdded) {
            isInitialised = initFragment()

            ctx?.let {
                LocalBroadcastManager.getInstance(ctx as Context).registerReceiver(
                    broadcastReceiver,
                    IntentFilter(Constant.INTENT_ACTION_LOAD_STOP)
                )
            }

            /*
             * If an Intent did not bring us to this Activity and there is a stop name saved in
             * shared preferences, load that stop.
             * This provides persistence to the app across shutdowns.
             */
            if (act?.intent?.hasExtra(Constant.STOP_NAME)?.not() == true) {
                if (Preferences.selectedStopName(ctx, Constant.NO_LINE) != null) {
                    val stopName = Preferences.selectedStopName(ctx, Constant.NO_LINE)

                    setTabAndSpinner(stopName)
                }
            }

            /*
             * If a Favourite stop brought us to this Activity, load that stop's forecast.
             * If a tapped notification brought us to this Activity, load the forecast for the stop
             * sent with that Intent.
             * If the previous cases are not matched, and the user has selected a default stop, load
             * the forecast for that.
             */
            if (act?.intent?.hasExtra(Constant.STOP_NAME) == true) {
                val stopName = act?.intent?.getStringExtra(Constant.STOP_NAME)

                /*
                 * Track whether or not the tab and spinner has been set. If it has, clear the Extra
                 * so it doesn't break the Default Stop setting.
                 */
                val hasSetTabAndSpinner = setTabAndSpinner(stopName)
                if (hasSetTabAndSpinner) {
                    act?.intent?.removeExtra(Constant.STOP_NAME)
                }
            } else if (act?.intent?.hasExtra(Constant.NOTIFY_STOP_NAME) == true) {
                /*
                 * Track whether or not the tab and spinner has been set. If it has, clear the Extra
                 * so it doesn't break the Default Stop setting.
                 */
                val hasSetTabAndSpinner = setTabAndSpinner(
                    act?.intent?.getStringExtra(Constant.NOTIFY_STOP_NAME)
                )
                if (hasSetTabAndSpinner) {
                    act?.intent?.removeExtra(Constant.NOTIFY_STOP_NAME)
                }
            } else if (act?.intent?.hasExtra(intentExtraActivityToOpen) == true) {
                act?.intent?.getStringExtra(intentExtraActivityToOpen)?.let {
                    activityRouter(it)
                }

                /* Clear the Extra to avoid opening the same Activity on every start. */
                act?.intent?.removeExtra(intentExtraActivityToOpen)
            } else if (Preferences.defaultStopName(ctx) != getString(R.string.none)
                && Preferences.defaultStopName(ctx) != null) {
                setTabAndSpinner(Preferences.defaultStopName(ctx))
            }

            /* Display tutorial for selecting a stop, if required. */
            displayTutorial(rootView, line, Constant.TUTORIAL_SELECT_STOP, true)

            /*
             * Reload stop forecast.
             * Induce 10 second delay if app is launching from cold start (timerTaskReload == null)
             * in order to prevent two HTTP requests in rapid succession.
             */
            if (timerTaskReload == null) {
                autoReloadStopForecast(10000)
            } else {
                autoReloadStopForecast(0)
            }

            recyclerViewStopForecastsInbound =
                rootView?.findViewById(R.id.recyclerview_stop_forecasts_inbound)
            recyclerViewStopForecastsOutbound =
                rootView?.findViewById(R.id.recyclerview_stop_forecasts_outbound)
            linearLayoutManagerInbound = LinearLayoutManager(ctx)
            linearLayoutManagerOutbound = LinearLayoutManager(ctx)
            linearLayoutManagerInbound?.orientation = LinearLayoutManager.VERTICAL
            linearLayoutManagerOutbound?.orientation = LinearLayoutManager.VERTICAL
            recyclerViewStopForecastsInbound?.layoutManager = linearLayoutManagerInbound
            recyclerViewStopForecastsOutbound?.layoutManager = linearLayoutManagerOutbound
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        this.isVisibleToUser = isVisibleToUser

        if (isInitialised) {
            /* If the Spinner's selected item is "Select a stop...", get out of here. */
            if (spinnerCardView?.spinnerStops?.selectedItemPosition == 0) {
                Log.i(logTag, "Spinner selected item is \"Select a stop...\"")
                return
            }

            /* When this tab is visible to the user, load a stop forecast. */
            if (isVisibleToUser) {
                if (spinnerCardView?.spinnerStops?.selectedItem != null) {
                    val stopName = spinnerCardView?.spinnerStops?.selectedItem.toString()

                    Preferences.saveSelectedStopName(ctx, Constant.NO_LINE, stopName)

                    loadStopForecast(stopName, false)

                    shouldAutoReload = true
                } else {
                    Log.w(logTag, "Spinner selected item is null.")
                }
            } else {
                shouldAutoReload = false
            }
        }
    }

    /**
     * Initialise local variables for this Fragment instance.
     */
    private fun initFragmentVars() {
        resArrayStopsRedLine = arguments?.getInt(Constant.RES_ARRAY_STOPS_RED_LINE)
        resArrayStopsGreenLine = arguments?.getInt(Constant.RES_ARRAY_STOPS_GREEN_LINE)
        line = arguments?.getString(Constant.LINE)
        resLayoutFragmentLine = arguments?.getInt(Constant.RES_LAYOUT_FRAGMENT_LINE)
        resProgressBar = arguments?.getInt(Constant.RES_PROGRESSBAR)
        resSpinnerCardView = arguments?.getInt(Constant.RES_SPINNER_CARDVIEW)
        resStatusCardView = arguments?.getInt(Constant.RES_STATUS_CARDVIEW)
        resSwipeRefreshLayout = arguments?.getInt(Constant.RES_SWIPEREFRESHLAYOUT)
        resScrollView = arguments?.getInt(Constant.RES_SCROLLVIEW)
    }

    /**
     * Initialise Fragment and its views.
     */
    private fun initFragment(): Boolean {
        tabLayout = act?.findViewById(R.id.trams_tablayout)
        progressBar = resProgressBar?.let { rootView?.findViewById(it) }
        setIsLoading(false)

        /* Set up Spinner and onItemSelectedListener. */
        spinnerCardView = resSpinnerCardView?.let { rootView?.findViewById(it) }
        spinnerCardView?.setLine(line)
        spinnerCardView?.spinnerStops?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int,
                id: Long
            ) {
                /*
                 * onItemSelected() is triggered on creation of the tab. Prevent this by
                 * only triggering when the tab is visible to user. This is to prevent the
                 * Alerts button changing colour out of sync with the currently-visible tab.
                 */
                if (isVisibleToUser) {
                    /*
                     * If the Spinner's selected item is "Select a stop...", we don't need
                     * to do anything. Just clear the stop forecast and get out of here.
                     */
                    if (position == 0) {
                        shouldAutoReload = false
                        swipeRefreshLayout?.isEnabled = false

                        return
                    } else {
                        swipeRefreshLayout?.isEnabled = true
                    }

                    shouldAutoReload = true

                    /* Hide the select stop tutorial, if it is visible. */
                    displayTutorial(rootView, line, Constant.TUTORIAL_SELECT_STOP, false)

                    /* Show the notifications tutorial. */
                    displayTutorial(rootView, line, Constant.TUTORIAL_NOTIFICATIONS, true)

                    /*
                     * Get the stop name from the current position of the Spinner, save it to
                     * SharedPreferences, then load a stop forecast with it.
                     */
                    val selectedStopName =
                        spinnerCardView?.spinnerStops?.getItemAtPosition(position).toString()

                    loadStopForecast(selectedStopName, false)

                    if (isVisibleToUser) {
                        Preferences.saveSelectedStopName(ctx, line, selectedStopName)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        /* Set up Status CardView. */
        statusCardView = resStatusCardView?.let { rootView?.findViewById(it) }

        /* Set up SwipeRefreshLayout. */
        swipeRefreshLayout = resSwipeRefreshLayout?.let { rootView?.findViewById(it) }
        swipeRefreshLayout?.setOnRefreshListener{
            /* Start the refresh animation. */
            swipeRefreshLayout?.isRefreshing = true

            loadStopForecast(Preferences.selectedStopName(ctx, line), true)
        }

        scrollView = resScrollView?.let { rootView?.findViewById(it) }
        scrollView?.isNestedScrollingEnabled = false

        return true
    }

    /**
     * Utility method to open an Activity based on a passed tag value.
     * @param activityToOpen Value of Activity to open.
     */
    private fun activityRouter(activityToOpen: String) {
        Log.i(logTag, "Intent received to open Activity.")

        when (activityToOpen) {
            Constant.REMOTEMESSAGE_VALUE_ACTIVITY_FARES -> {
                Log.i(logTag, "Routing to Activity: " + Constant.CLASS_FARES_ACTIVITY)

                startActivity(
                    Intent(ctx, Constant.CLASS_FARES_ACTIVITY)
                )
            }

            Constant.REMOTEMESSAGE_VALUE_ACTIVITY_MAIN ->
                /* We're already in MainActivity. Nothing to do here. */
                Log.i(logTag, "Already on MainActivity. Not routing anywhere.")

            Constant.REMOTEMESSAGE_VALUE_ACTIVITY_NEWS -> {
                Log.i(logTag, "Routing to Activity: " + Constant.CLASS_NEWS_ACTIVITY)

                startActivity(
                    Intent(ctx, Constant.CLASS_NEWS_ACTIVITY)
                )
            }

            Constant.REMOTEMESSAGE_VALUE_ACTIVITY_SETTINGS -> {
                Log.i(logTag, "Routing to Activity: " + Constant.CLASS_SETTINGS_ACTIVITY)

                startActivity(
                    Intent(ctx, Constant.CLASS_SETTINGS_ACTIVITY)
                )
            }

            else ->
                /*
                 * We should have never gotten to this point, as NotificationUtil should
                 * pass MainActivity as its default case.
                 */
                Log.wtf(logTag, "activityToOpen key does not correspond to any known value.")
        }
    }

    /**
     * Make progress bar animate or not.
     * @param loading Whether or not progress bar should animate.
     */
    private fun setIsLoading(loading: Boolean) {
        if (isAdded) {
            /*
             * Only run if Fragment is attached to Activity. Without this check, the app is liable
             * to crash when the screen is rotated many times in a given period of time.
             */
            act?.runOnUiThread {
                if (loading) {
                    progressBar?.visibility = View.VISIBLE
                } else {
                    progressBar?.visibility = View.INVISIBLE
                }
            }
        }
    }

    /**
     * Set the current tab and the position of the Spinner.
     */
    private fun setTabAndSpinner(stopName: String?): Boolean {
        lateinit var listStopsRedLine: List<String>
        lateinit var listStopsGreenLine: List<String>

        resArrayStopsRedLine?.let {
            val arrayStopsRedLine = resources.getStringArray(it)
            listStopsRedLine = listOf(*arrayStopsRedLine)
        }
        resArrayStopsGreenLine?.let {
            val arrayStopsGreenLine = resources.getStringArray(it)
            listStopsGreenLine = listOf(*arrayStopsGreenLine)
        }

        var listStopsThisLine: List<String>? = null
        var indexOtherLine = -1

        when (line) {
            Constant.RED_LINE -> {
                listStopsThisLine = listStopsRedLine
                indexOtherLine = 1
            }

            Constant.GREEN_LINE -> {
                listStopsThisLine = listStopsGreenLine
                indexOtherLine = 0
            }

            else ->
                /* If for some reason the line doesn't make sense. */
                Log.wtf(logTag, "Invalid line specified.")
        }

        /* Safety check. */
        if (listStopsThisLine == null) {
            Log.e(logTag, "List of stops for this line is null.")

            return false
        }

        /*
         * If the List of stops representing this Fragment contains the requested stop name, set the
         * Spinner to that stop.
         * Otherwise, switch to the other tab and load the last-loaded stop in the previous tab.
         */
        return if (listStopsThisLine.contains(stopName)) {
            spinnerCardView?.setSelection(stopName)

            true
        } else {
            val tab = tabLayout?.getTabAt(indexOtherLine)
            tab?.select()

            spinnerCardView?.setSelection(Preferences.selectedStopName(ctx, line))

            false
        }
    }

    /**
     * Automatically reload the stop forecast after a defined period.
     * @param delayTimeMillis The delay (ms) before starting the timer.
     */
    fun autoReloadStopForecast(delayTimeMillis: Int) {
        val reloadTimeMillis = 10000

        timerTaskReload = object : TimerTask() {
            override fun run() {
                /* Check Fragment is attached to Activity to avoid NullPointerExceptions. */
                if (isAdded) {
                    act?.runOnUiThread {
                        if (shouldAutoReload) {
                            loadStopForecast(
                                Preferences.selectedStopName(
                                    act?.applicationContext,
                                    line
                                ),
                                false
                            )
                        }
                    }
                }
            }
        }

        /* Schedule the auto-reload task to run. */
        Timer().schedule(timerTaskReload, delayTimeMillis.toLong(), reloadTimeMillis.toLong())
    }

    /**
     * Load the stop forecast for a particular stop.
     * @param stopName The stop for which to load a stop forecast.
     * @param shouldShowSnackbar Whether or not we should show a Snackbar to the user with the API
     * created time.
     */
    private fun loadStopForecast(stopName: String, shouldShowSnackbar: Boolean) {
        val apiUrl = "https://api.thecosmicfrog.org/cgi-bin"
        val apiAction = "times"
        val apiVer = "3"

        setIsLoading(true)

        /*
         * Prepare Retrofit API call.
         */
        val restAdapter = RestAdapter.Builder().setEndpoint(apiUrl).build()
        val methods = restAdapter.create(ApiMethods::class.java)

        val callback: Callback<ApiTimes?> = object : Callback<ApiTimes?> {
            override fun success(apiTimes: ApiTimes?, response: Response) {
                /* Check Fragment is attached to Activity to avoid NullPointerExceptions. */
                if (isAdded) {
                    /* If the server returned times. */
                    if (apiTimes != null) {
                        /* Then create a stop forecast with this data. */
                        val stopForecast = createStopForecast(apiTimes)

                        /* Update the stop forecast. */
                        updateStopForecast(stopForecast)

                        /* Stop the refresh animations. */
                        setIsLoading(false)
                        swipeRefreshLayout?.isRefreshing = false

                        if (shouldShowSnackbar) {
                            val apiCreatedTime = getApiCreatedTime(apiTimes)
                            if (apiCreatedTime != null) {
                                act?.let {
                                    showSnackbar(it, "Times updated at $apiCreatedTime")
                                }
                            }
                        }
                    }
                }
            }

            override fun failure(retrofitError: RetrofitError) {
                Log.e(logTag, "Failure during call to server.")

                /*
                 * If we get a message or a response from the server, there's likely an issue with
                 * the client request or the server's response itself.
                 */
                if (retrofitError.message != null) {
                    Log.e(logTag, "Message: " + retrofitError.message)
                }

                if (retrofitError.response != null) {
                    if (retrofitError.response.url != null) {
                        Log.e(logTag, "Response: " + retrofitError.response.url)
                    }

                    Log.e(logTag, "Status: " + retrofitError.response.status.toString())

                    if (retrofitError.response.headers != null) {
                        Log.e(logTag, "Headers: " + retrofitError.response.headers.toString())
                    }

                    if (retrofitError.response.body != null) {
                        Log.e(logTag, "Body: " + retrofitError.response.body.toString())
                    }

                    if (retrofitError.response.reason != null) {
                        Log.e(logTag, "Reason: " + retrofitError.response.reason)
                    }
                }

                /*
                 * If we don't receive a message or response, we can still get an idea of what's
                 * going on by getting the "kind" of error.
                 */
                if (retrofitError.kind != null) {
                    Log.e(logTag, "Kind: " + retrofitError.kind.toString())
                }
            }
        }

        /* Call API and get stop forecast from server. */
        methods.getStopForecast(apiAction, apiVer, mapStopNameId?.get(stopName), callback)
    }

    /**
     * Get the "created" time from the API response and format it so that only the time (and not
     * date) is returned.
     * @param apiTimes ApiTimes model.
     * @return String representing the 24hr time (HH:mm:ss) of the API's "created" time.
     */
    private fun getApiCreatedTime(apiTimes: ApiTimes): String? {
        try {
            if (apiTimes.createdTime != null) {
                val currentTime = SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss",
                    Locale.getDefault()
                ).parse(apiTimes.createdTime)

                val dateFormat: DateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

                if (currentTime != null) {
                    return dateFormat.format(currentTime)
                }
            }
        } catch (e: NullPointerException) {
            Log.e(logTag, "Failed to find content view during Snackbar creation.")
        } catch (e: ParseException) {
            Log.e(logTag, "Failed to parse created time from API.")
        }

        return null
    }

    /**
     * Draw stop forecast to screen.
     * @param stopForecast StopForecast model containing data for requested stop.
     */
    private fun updateStopForecast(stopForecast: StopForecast?) {
        val gaeilge = "ga"
        val due = "DUE"
        val mapEnglishGaeilge = EnglishGaeilgeMap()
        val min = " ${getString(R.string.min)}"
        val mins = " ${getString(R.string.mins)}"
        var minOrMins: String

        /* If a valid stop forecast exists... */
        if (stopForecast != null) {
            var operatingNormally = false

            if (stopForecast.stopForecastStatusDirectionInbound.operatingNormally != null
                && stopForecast.stopForecastStatusDirectionOutbound.operatingNormally != null) {
                if (stopForecast.stopForecastStatusDirectionInbound.operatingNormally == true
                    && stopForecast.stopForecastStatusDirectionOutbound.operatingNormally == true) {
                    operatingNormally = true
                }
            }

            val status: String? = if (localeDefault?.startsWith(gaeilge) == true) {
                getString(R.string.message_success)
            } else {
                stopForecast.message
            }

            if (status != null) {
                /* A lot of Luas statuses relate to lifts being out of service. Ignore these. */
                if (operatingNormally || status.toLowerCase().contains("lift")) {
                    /*
                     * No error message on server. Change the message title TextView to
                     * green and set a default success message.
                     */
                    statusCardView?.setStatus(status)
                    statusCardView?.setStatusColor(R.color.message_success)
                } else {
                    if (status.isBlank()) {
                        /*
                         * If server returns no status message, the Luas RTPI system is likely down.
                         */
                        statusCardView?.setStatus(getString(R.string.message_no_status))
                    } else {
                        /* Set the error message from the server. */
                        statusCardView?.setStatus(status)
                    }

                    /* Change the color of the message title TextView to red. */
                    statusCardView?.setStatusColor(R.color.message_error)
                }
            }

            var destination: String?
            val listStopForecastInfoInbound: MutableList<StopForecastInfo> = ArrayList()
            val listStopForecastInfoOutbound: MutableList<StopForecastInfo> = ArrayList()

            listStopForecastInfoInbound.clear()
            listStopForecastInfoOutbound.clear()

            val listAllTrams: MutableList<Tram> = ArrayList()
            listAllTrams.addAll(stopForecast.inboundTrams)
            listAllTrams.addAll(stopForecast.outboundTrams)

            if (stopForecast.inboundTrams.size <= 0) {
                listStopForecastInfoInbound.add(
                    StopForecastInfo(getString(R.string.no_trams_forecast), "", "")
                )
            }

            if (stopForecast.outboundTrams.size <= 0) {
                listStopForecastInfoOutbound.add(
                    StopForecastInfo(getString(R.string.no_trams_forecast), "", "")
                )
            }

            for (tram in listAllTrams) {
                var dueMinutes = tram.dueMinutes

                destination = if (localeDefault?.startsWith(gaeilge) == true) {
                    mapEnglishGaeilge[tram.destination]
                } else {
                    tram.destination
                }

                if (dueMinutes != null) {
                    when {
                        dueMinutes.equals(due, ignoreCase = true) -> {
                            if (localeDefault?.startsWith(gaeilge) == true) {
                                dueMinutes = mapEnglishGaeilge[dueMinutes]
                            }
                            minOrMins = ""
                        }

                        dueMinutes.toInt() > 1 -> minOrMins = mins

                        else -> minOrMins = min
                    }

                    if (tram.direction != null) {
                        when (tram.direction) {
                            Constant.INBOUND -> listStopForecastInfoInbound.add(
                                StopForecastInfo(destination, dueMinutes, minOrMins)
                            )
                            Constant.OUTBOUND -> listStopForecastInfoOutbound.add(
                                StopForecastInfo(destination, dueMinutes, minOrMins)
                            )
                            else -> Log.wtf(logTag, "Tram direction makes no sense.")
                        }
                    }
                }
            }

            val stopForecastAdapterInbound = StopForecastAdapter(listStopForecastInfoInbound)
            val stopForecastAdapterOutbound = StopForecastAdapter(listStopForecastInfoOutbound)

            stopForecastAdapterInbound.notifyDataSetChanged()
            stopForecastAdapterOutbound.notifyDataSetChanged()

            recyclerViewStopForecastsInbound?.adapter = stopForecastAdapterInbound
            recyclerViewStopForecastsOutbound?.adapter = stopForecastAdapterOutbound
        } else {
            /*
             * If no stop forecast can be retrieved, set a generic error message and
             * change the color of the message title box red.
             */
            statusCardView?.setStatus(getString(R.string.message_error))
            statusCardView?.setStatusColor(R.color.message_error)
        }
    }
}

