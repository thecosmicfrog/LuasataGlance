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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.databinding.FragmentGreenlineBinding
import org.thecosmicfrog.luasataglance.databinding.FragmentRedlineBinding
import org.thecosmicfrog.luasataglance.model.LineViewModel
import org.thecosmicfrog.luasataglance.model.LineViewModelFactory
import org.thecosmicfrog.luasataglance.model.StopForecastAdapter
import org.thecosmicfrog.luasataglance.util.AppUtil
import org.thecosmicfrog.luasataglance.util.Constant
import org.thecosmicfrog.luasataglance.util.LineFragmentViewBindingAdapter
import org.thecosmicfrog.luasataglance.util.Preferences
import org.thecosmicfrog.luasataglance.util.StopForecastUtil
import org.thecosmicfrog.luasataglance.util.StopForecastUtil.showSnackbar
import org.thecosmicfrog.luasataglance.view.SpinnerCardView
import org.thecosmicfrog.luasataglance.view.StatusCardView
import kotlin.time.Duration.Companion.milliseconds

class LineFragment : Fragment() {

    private val logTag = LineFragment::class.java.simpleName
    private val viewModel: LineViewModel by viewModels {
        LineViewModelFactory.getInstance(requireContext())
    }

    private var viewBinding: LineFragmentViewBindingAdapter? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var act: FragmentActivity? = null
    private var ctx: Context? = null
    private var tabLayout: TabLayout? = null
    private var progressBar: ProgressBar? = null
    private var spinnerCardView: SpinnerCardView? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var scrollView: NestedScrollView? = null
    private var statusCardView: StatusCardView? = null
    private var isInitialised = false
    private var line: String? = null
    private var isVisibleToUser = false
    private var recyclerViewStopForecastsInbound: RecyclerView? = null
    private var recyclerViewStopForecastsOutbound: RecyclerView? = null
    private var linearLayoutManagerInbound: LinearLayoutManager? = null
    private var linearLayoutManagerOutbound: LinearLayoutManager? = null

    companion object {
        private var resArrayStopsRedLine = 0
        private var resArrayStopsGreenLine = 0

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
                }

                Constant.GREEN_LINE -> {
                    bundle.putString(Constant.LINE, Constant.GREEN_LINE)
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val bundle = arguments
        val line = bundle!!.getString(Constant.LINE)

        viewBinding = getBinding(line, container)

        return viewBinding?.stopForecastConstraintLayout!!.rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerViewStopForecastsInbound = viewBinding?.recyclerViewStopForecastsInbound
        recyclerViewStopForecastsOutbound = viewBinding?.recyclerViewStopForecastsOutbound
        linearLayoutManagerInbound = LinearLayoutManager(ctx)
        linearLayoutManagerOutbound = LinearLayoutManager(ctx)
        linearLayoutManagerInbound?.orientation = LinearLayoutManager.VERTICAL
        linearLayoutManagerOutbound?.orientation = LinearLayoutManager.VERTICAL
        recyclerViewStopForecastsInbound?.layoutManager = linearLayoutManagerInbound
        recyclerViewStopForecastsOutbound?.layoutManager = linearLayoutManagerOutbound

        initObservers()
    }

    override fun onDestroy() {
        super.onDestroy()

        viewModel.stopAutoReload()

        /* Unregister the BroadcastReceiver to prevent memory leaks. */
        broadcastReceiver?.let {
            LocalBroadcastManager.getInstance(ctx as Context).unregisterReceiver(it)
            broadcastReceiver = null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        viewBinding = null
    }

    override fun onPause() {
        super.onPause()

        /* Unregister the BroadcastReceiver to prevent memory leaks. */
        broadcastReceiver?.let {
            LocalBroadcastManager.getInstance(ctx as Context).unregisterReceiver(it)
            broadcastReceiver = null
        }
    }

    override fun onStop() {
        super.onStop()

        viewModel.stopAutoReload()

        /* Unregister the BroadcastReceiver to prevent memory leaks. */
        broadcastReceiver?.let {
            LocalBroadcastManager.getInstance(ctx as Context).unregisterReceiver(it)
            broadcastReceiver = null
        }
    }

    override fun onResume() {
        super.onResume()

        act = requireActivity()

        AppUtil.resetShouldNotAskAgainIfPermissionsChangedOutsideApp(context)

        if (!isAdded || viewBinding == null || line == null) return

        isInitialised = initFragment()

        broadcastReceiver?.let {
            LocalBroadcastManager.getInstance(ctx as Context).registerReceiver(
                it,
                IntentFilter(Constant.INTENT_ACTION_LOAD_STOP)
            )
        }

        val defaultStopName = Preferences.defaultStopName(ctx)
        val hasDefaultStop = defaultStopName != null && defaultStopName != getString(R.string.none)

        /*
         * If an Intent did not bring us to this Activity and there is a stop name saved in
         * shared preferences, load that stop.
         * This provides persistence to the app across shutdowns.
         *
         * Skipped when a default stop is set, since the block below loads that instead.
         */
        if (!hasDefaultStop && act?.intent?.hasExtra(Constant.STOP_NAME)?.not() == true) {
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
        } else if (hasDefaultStop) {
            setTabAndSpinner(defaultStopName)
        }

        /*
         * Reload stop forecast.
         * Induce 10 second delay if app is launching from cold start (timerTaskReload == null)
         * in order to prevent two HTTP requests in rapid succession.
         */
        autoReloadStopForecast(10000L)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        this.isVisibleToUser = isVisibleToUser

        if (isInitialised) {
            /* When this tab is visible to the user, load a stop forecast. */
            if (isVisibleToUser) {
                if (spinnerCardView?.spinnerStops?.selectedItem != null) {
                    /* Coroutine to assist with adding the delay below. */
                    viewLifecycleOwner.lifecycleScope.launch {
                        /* Add slight delay to prevent UI jank on tab change. */
                        delay(500L.milliseconds)

                        withStarted {
                            val stopName = spinnerCardView?.spinnerStops?.selectedItem.toString()

                            Preferences.saveSelectedStopName(ctx, Constant.NO_LINE, stopName)

                            viewModel.loadStopForecast(
                                stopName = stopName,
                                stopId = viewModel.getStopId(stopName)
                            )

                            autoReloadStopForecast(0L)
                        }
                    }
                } else {
                    Log.w(logTag, "Spinner selected item is null.")
                }
            } else {
                viewModel.stopAutoReload()

                viewLifecycleOwner.lifecycleScope.launch {
                    /*
                     * Clear the stop forecast of the "exiting tab" on tab change to avoid loading a stale
                     * stop forecast the next time the user opens it. Slight delay to prevent UI jank.
                     */
                    delay(500L.milliseconds)

                    withStarted {
                        viewModel.clearStopForecast()
                    }
                }
            }
        }
    }

    private fun getBinding(line: String?, viewGroup: ViewGroup?): LineFragmentViewBindingAdapter? {
        val inflater = LayoutInflater.from(context)

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
    }

    /**
     * Initialise Fragment and its views.
     */
    private fun initFragment(): Boolean {
        tabLayout = act?.findViewById(R.id.trams_tablayout)

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val favouriteStopName = intent?.getStringExtra(Constant.INTENT_EXTRA_STOP_NAME)
                spinnerCardView?.setSelection(favouriteStopName)
            }
        }

        StopForecastUtil.setStopForecastDirectionTitles(requireContext(), line, viewBinding)

        progressBar = viewBinding?.progressbar!!
        setIsLoading(false)

        /* Set up Spinner and onItemSelectedListener. */
        spinnerCardView = viewBinding?.spinnerCardView!!
        line?.let { spinnerCardView?.setLine(it) }
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
                    swipeRefreshLayout?.isEnabled = true

                    viewModel.clearStopForecast()

                    /*
                     * Get the stop name from the current position of the Spinner, save it to
                     * SharedPreferences, then load a stop forecast with it.
                     */
                    val selectedStopName =
                        spinnerCardView?.spinnerStops?.getItemAtPosition(position).toString()

                    viewModel.loadStopForecast(
                        stopName = selectedStopName,
                        stopId = viewModel.getStopId(selectedStopName)
                    )

                    if (isVisibleToUser) {
                        Preferences.saveSelectedStopName(ctx, line, selectedStopName)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        /* Set up Status CardView. */
        statusCardView = viewBinding?.statuscardview!!

        /* Spinner colours come from LaagSwipeRefreshLayout. */
        swipeRefreshLayout = viewBinding?.swiperefreshlayout!!
        swipeRefreshLayout?.setOnRefreshListener {
            viewModel.loadStopForecast(
                stopName = Preferences.selectedStopName(ctx, line),
                stopId = viewModel.getStopId(Preferences.selectedStopName(ctx, line)),
                isRefreshing = true,
                shouldShowSnackbar = true
            )
        }

        scrollView = viewBinding?.scrollview!!
        scrollView?.isNestedScrollingEnabled = false

        return true
    }

    /**
     * Make progress bar animate or not.
     * @param loading Whether or not progress bar should animate.
     */
    private fun setIsLoading(loading: Boolean) {
        if (!isAdded) return

        act?.runOnUiThread {
            if (loading) {
                progressBar?.visibility = View.VISIBLE
            } else {
                progressBar?.visibility = View.INVISIBLE
            }
        }
    }

    /**
     * Set the current tab and the position of the Spinner.
     *
     * @param stopName Stop name as displayed.
     * @return Whether the stop is on this line, and so whether the Spinner was set rather than the other tab selected.
     */
    private fun setTabAndSpinner(stopName: String?): Boolean {
        val arrayStopsRedLine = resources.getStringArray(resArrayStopsRedLine)
        val arrayStopGreenLine = resources.getStringArray(resArrayStopsGreenLine)

        val listStopsRedLine = arrayStopsRedLine.toList().sortedBy { it.lowercase() }
        val listStopsGreenLine = arrayStopGreenLine.toList().sortedBy { it.lowercase() }
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
    fun autoReloadStopForecast(delayTimeMillis: Long) {
        viewModel.stopAutoReload()

        if (isVisibleToUser) {
            viewModel.startAutoReload(
                stopName = Preferences.selectedStopName(ctx, line),
                stopNameId = viewModel.getStopId(Preferences.selectedStopName(ctx, line)),
                delayMillis = delayTimeMillis
            )
        } else {
            viewModel.stopAutoReload()
        }
    }

    /**
     * Initialise observers for the ViewModel.
     */
    private fun initObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.status.collect { status ->
                status?.let { (message, isError) ->
                    statusCardView?.setStatus(message)

                    if (isError) {
                        statusCardView?.setStatusColor(R.color.status_fill_error, R.color.status_text_error)
                    } else {
                        statusCardView?.setStatusColor(R.color.status_fill_success, R.color.status_text_success)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.stopForecastInfo.collect { pair ->
                /* Skips one value rather than the whole loop, so a forecast arriving while the tab is detached is dropped. */
                if (!isAdded) return@collect

                if (pair == null) {
                    /*
                     * Null covers a cold start, a stop change, tabbing away, and a failed load. LineViewModel.clearStopForecast()
                     * sets it, and the fragment never empties these RecyclerViews itself.
                     */
                    StopForecastUtil.clearStopForecast(recyclerViewStopForecastsInbound, recyclerViewStopForecastsOutbound)
                } else {
                    /* Replaced rather than diffed, which is what makes swapping the shimmer adapter in and out cheap. */
                    recyclerViewStopForecastsInbound ?.adapter = StopForecastAdapter(pair.first)
                    recyclerViewStopForecastsOutbound?.adapter = StopForecastAdapter(pair.second)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                setIsLoading(isLoading)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isRefreshing.collect { isRefreshing ->
                swipeRefreshLayout?.isRefreshing = isRefreshing
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Log.e(logTag, "Error loading stop forecast: $it")
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showSnackbarWithTime.collect { message ->
                message?.let {
                    act?.let { activity ->
                        showSnackbar(activity, it)
                    }
                }
            }
        }
    }
}
