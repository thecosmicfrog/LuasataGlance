/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2023 Aaron Hastings
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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.api.ApiMethods
import org.thecosmicfrog.luasataglance.api.ApiTimes
import org.thecosmicfrog.luasataglance.databinding.FragmentGreenlineBinding
import org.thecosmicfrog.luasataglance.databinding.FragmentRedlineBinding
import org.thecosmicfrog.luasataglance.model.EnglishGaeilgeMap
import org.thecosmicfrog.luasataglance.model.NotifyTimesMap
import org.thecosmicfrog.luasataglance.model.StopForecast
import org.thecosmicfrog.luasataglance.model.StopNameIdMap
import org.thecosmicfrog.luasataglance.util.AppUtil
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.LineFragmentViewBindingAdapter
import org.thecosmicfrog.luasataglance.util.Preferences
import org.thecosmicfrog.luasataglance.util.Settings
import org.thecosmicfrog.luasataglance.util.StopForecastUtil.createStopForecast
import org.thecosmicfrog.luasataglance.util.StopForecastUtil.displayTutorial
import org.thecosmicfrog.luasataglance.util.StopForecastUtil.showSnackbar
import org.thecosmicfrog.luasataglance.view.SpinnerCardView
import org.thecosmicfrog.luasataglance.view.StatusCardView
import org.thecosmicfrog.luasataglance.view.StopForecastCardView
import retrofit.Callback
import retrofit.RestAdapter
import retrofit.RetrofitError
import retrofit.client.Response
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

class LineFragment : Fragment() {
    private val logTag = LineFragment::class.java.simpleName

    private var viewBinding: LineFragmentViewBindingAdapter? = null
    private var isInitialised = false
    private var shouldAutoReload = false
    private var isVisibleToUser = false
    private var line: String? = null

    private lateinit var activity: FragmentActivity
    private lateinit var context: Context
    private lateinit var tabLayout: TabLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var spinnerCardView: SpinnerCardView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var scrollView: ScrollView
    private lateinit var statusCardView: StatusCardView
    private lateinit var inboundStopForecastCardView: StopForecastCardView
    private lateinit var outboundStopForecastCardView: StopForecastCardView
    private lateinit var imageViewBottomNavAlerts: ImageView
    private lateinit var textViewBottomNavAlerts: TextView
    private lateinit var timerTaskReload: TimerTask
//    private lateinit var line: String

    override fun onAttach(c: Context) {
        super.onAttach(c)

        context = c
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initFragmentVars()
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val bundle = arguments
        val line = bundle!!.getString(Constant.LINE)

        viewBinding = getBinding(line, container)

        /* Initialise correct locale. */localeDefault = Locale.getDefault().toString()

        /* Instantiate a new StopNameIdMap. */mapStopNameId = StopNameIdMap(localeDefault!!)
        return viewBinding?.linearlayoutFragment!!.rootView
    }

    override fun onPause() {
        super.onPause()

        /* Stop the auto-reload TimerTask. */timerTaskReload.cancel()
    }

    override fun onResume() {
        super.onResume()

        activity = requireActivity()

        AppUtil.resetShouldNotAskAgainIfPermissionsChangedOutsideApp(context)

        /* Remove Favourites tutorial if it has been completed once already. */
        if (viewBinding != null && line == Constant.RED_LINE && Preferences.hasRunOnce(context, Constant.TUTORIAL_FAVOURITES)) {
            displayTutorial(viewBinding!!, Constant.RED_LINE, Constant.TUTORIAL_FAVOURITES, false)
        }

        if (isAdded && viewBinding != null && line != null) {
            isInitialised = initFragment()

            /*
             * If an Intent did not bring us to this Activity and there is a stop name saved in
             * shared preferences, load that stop.
             * This provides persistence to the app across shutdowns.
             */
            if (!requireActivity().intent.hasExtra(Constant.STOP_NAME)) {
                if (Preferences.selectedStopName(context, Constant.NO_LINE) != null) {
                    val stopName = Preferences.selectedStopName(context, Constant.NO_LINE)
                    setTabAndSpinner(stopName)
                }
            }
            imageViewBottomNavAlerts = requireActivity().findViewById(R.id.imageview_bottomnav_alerts)
            textViewBottomNavAlerts = requireActivity().findViewById(R.id.textview_bottomnav_alerts)

            /*
             * If a Favourite stop brought us to this Activity, load that stop's forecast.
             * If a tapped notification brought us to this Activity, load the forecast for the stop
             * sent with that Intent.
             * If the previous cases are not matched, and the user has selected a default stop, load
             * the forecast for that.
             */
            if (requireActivity().intent.hasExtra(Constant.STOP_NAME)) {
                val stopName = requireActivity().intent.getStringExtra(Constant.STOP_NAME)

                /*
                 * Track whether or not the tab and spinner has been set. If it has, clear the Extra
                 * so it doesn't break the Default Stop setting.
                 */
                val hasSetTabAndSpinner = setTabAndSpinner(stopName)
                if (hasSetTabAndSpinner) {
                    requireActivity().intent.removeExtra(Constant.STOP_NAME)
                }
            } else if (requireActivity().intent.hasExtra(Constant.NOTIFY_STOP_NAME)) {
                /*
                 * Track whether or not the tab and spinner has been set. If it has, clear the Extra
                 * so it doesn't break the Default Stop setting.
                 */
                val hasSetTabAndSpinner = setTabAndSpinner(
                    requireActivity().intent.getStringExtra(Constant.NOTIFY_STOP_NAME)
                )
                if (hasSetTabAndSpinner) {
                    requireActivity().intent.removeExtra(Constant.NOTIFY_STOP_NAME)
                }
            } else if (Preferences.defaultStopName(context) != getString(R.string.none) && Preferences.defaultStopName(context) != null) {
                setTabAndSpinner(Preferences.defaultStopName(context))
            }

            /* Display tutorial for selecting a stop, if required. */
            displayTutorial(viewBinding!!, line!!, Constant.TUTORIAL_SELECT_STOP, true)

            /*
             * Reload stop forecast.
             * Induce 10 second delay if app is launching from cold start (timerTaskReload == null)
             * in order to prevent two HTTP requests in rapid succession.
             */
            autoReloadStopForecast(0)
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        this.isVisibleToUser = isVisibleToUser

        if (isInitialised) {
            /* If the Spinner's selected item is "Select a stop...", get out of here. */
            if (spinnerCardView.spinnerStops.selectedItemPosition == 0) {
                Log.i(logTag, "Spinner selected item is \"Select a stop...\"")
                return
            }

            /* When this tab is visible to the user, load a stop forecast. */
            if (isVisibleToUser) {
                if (spinnerCardView.spinnerStops.selectedItem != null) {
                    val stopName = spinnerCardView.spinnerStops.selectedItem.toString()
                    Preferences.saveSelectedStopName(context, Constant.NO_LINE, stopName)

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        /* Inflate the menu; this adds items to the action bar if it is present. */
        inflater.inflate(resMenuLine, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Settings.getSettings(context, item)
        return super.onOptionsItemSelected(item)
    }

    private fun getBinding(line: String?, viewGroup: ViewGroup?): LineFragmentViewBindingAdapter? {
        val inflater = LayoutInflater.from(getContext())
        when (line) {
            Constant.RED_LINE -> {
                val fragmentRedlineBinding = FragmentRedlineBinding.inflate(inflater, viewGroup, false)
                return LineFragmentViewBindingAdapter(fragmentRedlineBinding, null)
            }

            Constant.GREEN_LINE -> {
                val fragmentGreenlineBinding = FragmentGreenlineBinding.inflate(inflater, viewGroup, false)
                return LineFragmentViewBindingAdapter(null, fragmentGreenlineBinding)
            }

            else -> Log.wtf(logTag, "Invalid line specified.")
        }

        return null
    }

    /**
     * Initialise local variables for this Fragment instance.
     */
    private fun initFragmentVars() {
        resArrayStopsRedLine = requireArguments().getInt(Constant.RES_ARRAY_STOPS_RED_LINE)
        resArrayStopsGreenLine = requireArguments().getInt(Constant.RES_ARRAY_STOPS_GREEN_LINE)
        line = requireArguments().getString(Constant.LINE)
        resMenuLine = requireArguments().getInt(Constant.RES_MENU_LINE)
    }

    /**
     * Initialise Fragment and its views.
     */
    private fun initFragment(): Boolean {
        tabLayout = requireActivity().findViewById(R.id.tablayout)
        progressBar = viewBinding?.progressbar!!

        setIsLoading(false)

        /* Set up Spinner and onItemSelectedListener. */
        spinnerCardView = viewBinding?.spinnerCardView!!
        spinnerCardView.setLine(line)
        spinnerCardView.spinnerStops.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
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
                        swipeRefreshLayout.isEnabled = false

                        clearStopForecast()

                        return
                    } else {
                        swipeRefreshLayout.isEnabled = true
                    }

                    shouldAutoReload = true

                    /* Hide the select stop tutorial, if it is visible. */
                    displayTutorial(viewBinding!!, line!!, Constant.TUTORIAL_SELECT_STOP, false)

                    /* Show the notifications tutorial. */
                    displayTutorial(viewBinding!!, line!!, Constant.TUTORIAL_NOTIFICATIONS, true)

                    /*
                     * Get the stop name from the current position of the Spinner, save it to
                     * SharedPreferences, then load a stop forecast with it.
                     */
                    val selectedStopName = spinnerCardView.spinnerStops?.getItemAtPosition(position).toString()
                    loadStopForecast(selectedStopName, false)

                    if (isVisibleToUser) {
                        Preferences.saveSelectedStopName(
                            context,
                            line,
                            selectedStopName
                        )
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        /* Set up Status CardView. */
        statusCardView = viewBinding?.statuscardview!!

        /* Set up SwipeRefreshLayout. */swipeRefreshLayout = viewBinding?.swiperefreshlayout!!
        swipeRefreshLayout.setOnRefreshListener {

            /* Start by clearing the currently-displayed stop forecast. */
            clearStopForecast()

            /* Start the refresh animation. */
            swipeRefreshLayout.isRefreshing = true
            loadStopForecast(
                Preferences.selectedStopName(context, line),
                true
            )
        }
        scrollView = viewBinding?.scrollview!!

        /* Set up stop forecast CardViews. */inboundStopForecastCardView = viewBinding?.inboundStopforecastcardview!!
        inboundStopForecastCardView.setStopForecastDirection(getString(R.string.inbound))
        outboundStopForecastCardView = viewBinding?.outboundStopforecastcardview!!
        outboundStopForecastCardView.setStopForecastDirection(getString(R.string.outbound))

        /* Set up onClickListeners for stop forecasts in both tabs. */
        initStopForecastOnClickListeners()

        return true
    }

    /**
     * Initialise OnClickListeners for a stop forecast.
     */
    private fun initStopForecastOnClickListeners() {
        val tableRowInboundStops = inboundStopForecastCardView.tableRowStops!!
        val tableRowOutboundStops = outboundStopForecastCardView.tableRowStops!!
        for (i in 0..5) {
            tableRowInboundStops[i].setOnClickListener {
                showNotifyTimeDialog(
                    spinnerCardView.spinnerStops.selectedItem.toString(),
                    inboundStopForecastCardView.textViewStopTimes,
                    i
                )
            }
            tableRowOutboundStops[i].setOnClickListener {
                showNotifyTimeDialog(
                    spinnerCardView.spinnerStops.selectedItem.toString(),
                    outboundStopForecastCardView.textViewStopTimes,
                    i
                )
            }
        }
    }

    /**
     * Show dialog for choosing notification times.
     * @param stopName          Stop name to notify for.
     * @param textViewStopTimes Array of TextViews for times in a stop forecast.
     * @param index             Index representing which specific tram to notify for.
     */
    private fun showNotifyTimeDialog(stopName: String, textViewStopTimes: Array<TextView>, index: Int) {
        val localeDefault = Locale.getDefault().toString()
        val notifyStopTimeStr = textViewStopTimes[index].text.toString()
        val mapNotifyTimes = NotifyTimesMap(localeDefault, Constant.STOP_FORECAST)

        if (notifyStopTimeStr == "") return

        if (notifyStopTimeStr.matches((getString(R.string.due) + "|" + "1 .*|2 .*").toRegex())) {
            Toast.makeText(
                context,
                getString(R.string.cannot_schedule_notification),
                Toast.LENGTH_LONG
            ).show()

            return
        }

        /*
         * When the user opens the notification dialog as part of the tutorial, scroll back up to
         * the top so that the next tutorial is definitely visible. This should only ever run once.
         */
        if (!Preferences.hasRunOnce(context, Constant.TUTORIAL_NOTIFICATIONS)) {
            scrollView.scrollY = 0
        }
        Preferences.saveHasRunOnce(context, Constant.TUTORIAL_NOTIFICATIONS, true)

        /* We're done with the notifications tutorial. Hide it. */
        displayTutorial(viewBinding!!, line!!, Constant.TUTORIAL_NOTIFICATIONS, false)

        /* Then, display the final tutorial. */
        displayTutorial(viewBinding!!, line!!, Constant.TUTORIAL_FAVOURITES, true)

        Preferences.saveNotifyStopName(context, stopName)
        Preferences.saveNotifyStopTimeExpected(context, mapNotifyTimes[notifyStopTimeStr]!!)

        requireContext().startActivity(Intent(context, NotifyTimeActivity::class.java))
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
            requireActivity().runOnUiThread {
                if (loading) {
                    progressBar.visibility = View.VISIBLE
                } else {
                    progressBar.visibility = View.INVISIBLE
                }
            }
        }
    }

    /**
     * Set the current tab and the position of the Spinner.
     */
    private fun setTabAndSpinner(stopName: String?): Boolean {
        val arrayStopsRedLine = resources.getStringArray(
            resArrayStopsRedLine
        )
        val arrayStopsGreenLine = resources.getStringArray(
            resArrayStopsGreenLine
        )
        val listStopsRedLine = listOf(*arrayStopsRedLine)
        val listStopsGreenLine = listOf(*arrayStopsGreenLine)
        var listStopsThisLine: List<String?>? = null
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

            else -> Log.wtf(logTag, "Invalid line specified.")
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
            spinnerCardView.setSelection(stopName)

            true
        } else {
            val tab = tabLayout.getTabAt(indexOtherLine)
            tab?.select()
            spinnerCardView.setSelection(Preferences.selectedStopName(context, line))

            false
        }
    }

    /**
     * Clear stop forecast.
     */
    private fun clearStopForecast() {
        inboundStopForecastCardView.clearStopForecast()
        outboundStopForecastCardView.clearStopForecast()
    }

    /**
     * Automatically reload the stop forecast after a defined period.
     * @param delayTimeMillis The delay (ms) before starting the timer.
     */
    private fun autoReloadStopForecast(delayTimeMillis: Int) {
        val reloadTimeMillis = 10000

        timerTaskReload = object : TimerTask() {
            override fun run() {
                /* Check Fragment is attached to Activity to avoid NullPointerExceptions. */
                if (isAdded) {
                    activity.runOnUiThread {
                        if (shouldAutoReload) {
                            loadStopForecast(
                                Preferences.selectedStopName(
                                    activity.applicationContext,
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
        val restAdapter = RestAdapter.Builder()
            .setEndpoint(apiUrl)
            .build()
        val methods = restAdapter.create(ApiMethods::class.java)

        val callback: Callback<ApiTimes?> = object : Callback<ApiTimes?> {
            override fun success(apiTimes: ApiTimes?, response: Response) {
                /* Check Fragment is attached to Activity to avoid NullPointerExceptions. */
                if (isAdded) {
                    /* If the server returned times. */
                    if (apiTimes != null) {
                        /* Then create a stop forecast with this data. */
                        val stopForecast = createStopForecast(apiTimes)
                        clearStopForecast()

                        /* Update the stop forecast. */
                        updateStopForecast(stopForecast)

                        /* Stop the refresh animations. */
                        setIsLoading(false)
                        swipeRefreshLayout.isRefreshing = false
                        if (shouldShowSnackbar) {
                            val apiCreatedTime = getApiCreatedTime(apiTimes)
                            if (apiCreatedTime != null) {
                                showSnackbar(
                                    activity,
                                    "Times updated at $apiCreatedTime"
                                )
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
                    Log.e(logTag, "Status: " + retrofitError.response.status)
                    if (retrofitError.response.headers != null) {
                        Log.e(
                            logTag, "Headers: " +
                                    retrofitError.response.headers.toString()
                        )
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
        methods.getStopForecast(
            apiAction,
            apiVer,
            mapStopNameId!![stopName],
            callback
        )
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
            Log.e(
                logTag,
                "Failed to find content view during Snackbar creation."
            )
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
        val localeGaeilge = "ga"
        val due = "DUE"
        val mapEnglishGaeilge = EnglishGaeilgeMap()
        val min = " " + getString(R.string.min)
        val mins = " " + getString(R.string.mins)
        var minOrMins: String

        /* If a valid stop forecast exists... */
        if (stopForecast != null) {
            var operatingNormally = false

            if (stopForecast.stopForecastStatusDirectionInbound.operatingNormally != null &&
                stopForecast.stopForecastStatusDirectionOutbound.operatingNormally != null) {
                if (stopForecast.stopForecastStatusDirectionInbound.operatingNormally!! &&
                    stopForecast.stopForecastStatusDirectionOutbound.operatingNormally!!) {
                    operatingNormally = true
                }
            }

            val status: String? = if (localeDefault!!.startsWith(localeGaeilge)) {
                getString(R.string.message_success)
            } else {
                stopForecast.message
            }
            if (status != null) {
                /* A lot of Luas status messages relate to lifts being out of service. Ignore these. */
                if (operatingNormally || status.lowercase(Locale.getDefault()).contains("lift")) {
                    /*
                     * No error message on server. Change the message title TextView to
                     * green and set a default success message.
                     */
                    statusCardView.setStatus(status)
                    statusCardView.setStatusColor(R.color.message_success)

                    /* Change the alerts image to the default white image. */
                    imageViewBottomNavAlerts.setImageResource(
                        R.drawable.ic_error_alerts
                    )

                    /* Change the color of the Alerts TextView to white (default). */
                    textViewBottomNavAlerts.setTextColor(
                        ContextCompat.getColor(requireContext(), android.R.color.white)
                    )
                } else {
                    if (status == "") {
                        /*
                         * If the server returns no status message, the Luas RTPI system is
                         * probably down.
                         */
                        statusCardView.setStatus(
                            getString(R.string.message_no_status)
                        )
                    } else {
                        /* Set the error message from the server. */
                        statusCardView.setStatus(status)
                    }

                    /* Change the color of the message title TextView to red. */
                    statusCardView.setStatusColor(R.color.message_error)

                    /* Change the Alerts image to the red version. */
                    imageViewBottomNavAlerts.setImageResource(
                        R.drawable.ic_error_alerts_red
                    )

                    /* Change the color of the Alerts TextView to red. */
                    textViewBottomNavAlerts.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.message_error)
                    )
                }
            }

            /*
             * Pull in all trams from the StopForecast, but only display up to five
             * inbound and outbound trams.
             */
            if (stopForecast.inboundTrams.size == 0) {
                inboundStopForecastCardView.setNoTramsForecast()
            } else {
                var inboundTram: String?

                for (i in stopForecast.inboundTrams.indices) {
                    var dueMinutes = stopForecast.inboundTrams[i].dueMinutes

                    if (i < 6) {
                        inboundTram = if (localeDefault!!.startsWith(localeGaeilge)) {
                            mapEnglishGaeilge[stopForecast.inboundTrams[i].destination]
                        } else {
                            stopForecast.inboundTrams[i].destination
                        }

                        if (dueMinutes != null) {
                            if (dueMinutes.equals(due, ignoreCase = true)) {
                                if (localeDefault!!.startsWith(localeGaeilge)) {
                                    dueMinutes = mapEnglishGaeilge[dueMinutes]
                                }
                                minOrMins = ""
                            } else if (dueMinutes.toInt() > 1) {
                                minOrMins = mins
                            } else {
                                minOrMins = min
                            }

                            inboundStopForecastCardView.setStopNames(i, inboundTram)
                            inboundStopForecastCardView.setStopTimes(i, dueMinutes + minOrMins)
                        }
                    }
                }
            }

            if (stopForecast.outboundTrams.size == 0) {
                outboundStopForecastCardView.setNoTramsForecast()
            } else {
                var outboundTram: String?

                for (i in stopForecast.outboundTrams.indices) {
                    var dueMinutes = stopForecast.outboundTrams[i].dueMinutes
                    if (i < 6) {
                        outboundTram = if (localeDefault!!.startsWith(localeGaeilge)) {
                            mapEnglishGaeilge[stopForecast.outboundTrams[i].destination]
                        } else {
                            stopForecast.outboundTrams[i].destination
                        }

                        if (dueMinutes != null) {
                            if (dueMinutes.equals(due, ignoreCase = true)) {
                                if (localeDefault!!.startsWith(localeGaeilge)) {
                                    dueMinutes = mapEnglishGaeilge[dueMinutes]
                                }
                                minOrMins = ""
                            } else if (dueMinutes.toInt() > 1) {
                                minOrMins = mins
                            } else {
                                minOrMins = min
                            }

                            outboundStopForecastCardView.setStopNames(i, outboundTram)
                            outboundStopForecastCardView.setStopTimes(i, dueMinutes + minOrMins)
                        }
                    }
                }
            }
        } else {
            /*
             * If no stop forecast can be retrieved, set a generic error message and
             * change the color of the message title box red.
             */
            statusCardView.setStatus(getString(R.string.message_error))
            statusCardView.setStatusColor(R.color.message_error)
        }
    }

    companion object {
        private var resMenuLine = 0
        private var resArrayStopsRedLine = 0
        private var resArrayStopsGreenLine = 0
        private var mapStopNameId: StopNameIdMap? = null
        private var localeDefault: String? = null

        fun newInstance(line: String?): LineFragment {
            val lineFragment = LineFragment()
            val bundle = Bundle()

            bundle.putInt(Constant.RES_ARRAY_STOPS_RED_LINE, R.array.array_stops_redline)
            bundle.putInt(Constant.RES_ARRAY_STOPS_GREEN_LINE, R.array.array_stops_greenline)

            when (line) {
                Constant.RED_LINE -> {
                    bundle.putString(Constant.LINE, Constant.RED_LINE)
                    bundle.putInt(Constant.RES_MENU_LINE, R.menu.menu_red_line)
                }

                Constant.GREEN_LINE -> {
                    bundle.putString(Constant.LINE, Constant.GREEN_LINE)
                    bundle.putInt(Constant.RES_MENU_LINE, R.menu.menu_green_line)
                }

                Constant.NO_LINE -> Log.e(LineFragment::class.java.simpleName, "No line specified.")

                else -> Log.wtf(LineFragment::class.java.simpleName, "Invalid line specified.")
            }

            lineFragment.arguments = bundle

            return lineFragment
        }
    }
}
