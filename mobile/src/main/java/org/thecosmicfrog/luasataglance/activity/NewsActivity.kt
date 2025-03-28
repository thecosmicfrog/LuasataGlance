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
 * along with Luas at a Glance.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thecosmicfrog.luasataglance.activity

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.thecosmicfrog.luasataglance.R
import org.thecosmicfrog.luasataglance.databinding.ActivityNewsBinding
import org.thecosmicfrog.luasataglance.util.Constant

class NewsActivity : AppCompatActivity() {

    private lateinit var viewBinding : ActivityNewsBinding
    private lateinit var webViewNews : WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityNewsBinding.inflate(layoutInflater)
        val view = viewBinding.root

        val urlNews = "https://luas.ie/news/"
        val urlTravelUpdates = "https://luas.ie/travel-updates/"

        setContentView(view)

        /*
         * Set ActionBar colour.
         */
        supportActionBar!!.setBackgroundDrawable(
            ColorDrawable(ContextCompat.getColor(applicationContext, R.color.luas_purple))
        )

        /*
         * Set status bar colour.
         */
        val window = window

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = ContextCompat.getColor(
            applicationContext,
            R.color.luas_purple_statusbar
        )

        /*
         * Create a new WebView and explicitly set the WebViewClient. Otherwise, an external
         * browser is liable to open.
         * Ensure the information is fresh by using no app or web browser cache.
         */
        webViewNews = viewBinding.webviewNews
        webViewNews.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        webViewNews.webViewClient = WebViewClient()

        /*
         * Load either the "Travel Updates" or "News" section of the Luas mobile website.
         */
        if (intent.hasExtra(Constant.NEWS_TYPE)) {
            if (intent.getStringExtra(Constant.NEWS_TYPE) == Constant.NEWS_TYPE_TRAVEL_UPDATES) {
                title = getString(R.string.title_activity_news_travel_updates)
                webViewNews.loadUrl(urlTravelUpdates)
            } else {
                title = getString(R.string.title_activity_news_luas_news)
                webViewNews.loadUrl(urlNews)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        /* Destroy WebView to prevent memory leaks. */
        webViewNews.removeAllViews()
        webViewNews.destroy()
    }
}

