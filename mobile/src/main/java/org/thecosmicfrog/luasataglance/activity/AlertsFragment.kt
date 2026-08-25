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

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.thecosmicfrog.luasataglance.R


class AlertsFragment : Fragment() {

    private val logTag = AlertsFragment::class.java.simpleName
    private var rootView: View? = null

    companion object {
        fun newInstance(): Fragment {
            val alertsFragment = AlertsFragment()
            val bundle = Bundle()

            alertsFragment.arguments = bundle

            return alertsFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        /* Inflate the layout for this Fragment. */
        rootView = inflater.inflate(R.layout.activity_news, container, false)

        return rootView
    }

    override fun onPause() {
        super.onPause()

        /* Flush cookies to disk to ensure luas.ie cookie banner preference is honoured. */
        CookieManager.getInstance().flush()
    }

    override fun onDestroyView() {
        rootView?.findViewById<WebView>(R.id.webview_news)?.let { webViewNews ->
            /* Detach before destroying, or Android logs a warning about destroying a WebView that is still in a hierarchy. */
            (webViewNews.parent as? ViewGroup)?.removeView(webViewNews)

            webViewNews.stopLoading()

            /* luas.ie runs JavaScript, and about:blank drops it rather than leaving it running into destroy(). */
            webViewNews.loadUrl("about:blank")

            webViewNews.removeAllViews()
            webViewNews.destroy()
        }

        rootView = null

        super.onDestroyView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        if (!isAdded || !isVisibleToUser) return

        val urlTravelUpdates = "https://luas.ie/travel-updates/"

        val progressBarNews = rootView?.findViewById<ProgressBar>(R.id.progressbar_news)
        val swipeRefreshLayoutNews =
            rootView?.findViewById<SwipeRefreshLayout>(R.id.swiperefreshlayout_news)

        /*
         * Create a new WebView and explicitly set the WebViewClient. Otherwise, an external
         * browser is liable to open.
         * Ensure the information is fresh by using no app or web browser cache.
         */
        val webViewNews = rootView?.findViewById<WebView>(R.id.webview_news)

        webViewNews?.settings?.cacheMode = WebSettings.LOAD_NO_CACHE

        /* Required for luas.ie. */
        webViewNews?.settings?.javaScriptEnabled = true
        webViewNews?.settings?.domStorageEnabled = true
        CookieManager.getInstance().setAcceptCookie(true)

        webViewNews?.webViewClient = object : WebViewClient() {
            override fun onPageCommitVisible(view: WebView, url: String) {
                super.onPageCommitVisible(view, url)

                progressBarNews?.visibility = View.INVISIBLE
            }
        }

        webViewNews?.loadUrl(urlTravelUpdates)

        swipeRefreshLayoutNews?.setOnRefreshListener {
            progressBarNews?.visibility = View.VISIBLE

            webViewNews?.clearCache(true)
            webViewNews?.reload()

            swipeRefreshLayoutNews.isRefreshing = false
        }
    }
}

