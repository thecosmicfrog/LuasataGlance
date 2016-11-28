/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2016 Aaron Hastings
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

package org.thecosmicfrog.luasataglance.activity;

import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.thecosmicfrog.luasataglance.R;
import org.thecosmicfrog.luasataglance.util.Constant;

public class NewsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String URL_NEWS = "http://m.luas.ie/news/";
        final String URL_TRAVEL_UPDATES = "http://m.luas.ie/travel-updates.html";

        setContentView(R.layout.activity_news);

        /*
         * Set ActionBar colour.
         */
        getSupportActionBar().setBackgroundDrawable(
                new ColorDrawable(
                        ContextCompat.getColor(getApplication(), R.color.luas_purple)
                )
        );

        /*
         * Set status bar colour.
         */
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(
                    ContextCompat.getColor(getApplicationContext(),
                            R.color.luas_purple_statusbar)
            );
        }

        /*
         * Create a new WebView and explicitly set the WebViewClient. Otherwise, an external
         * browser is liable to open.
         */
        WebView webViewNews = (WebView) findViewById(R.id.webview_news);
        webViewNews.setWebViewClient(new WebViewClient());

        /*
         * Load either the "Travel Updates" or "News" section of the Luas mobile website.
         */
        if (getIntent().hasExtra(Constant.NEWS_TYPE)) {
            if (getIntent().getStringExtra(Constant.NEWS_TYPE)
                    .equals(Constant.NEWS_TYPE_TRAVEL_UPDATES)) {
                setTitle(getString(R.string.title_activity_news_travel_updates));

                webViewNews.loadUrl(URL_TRAVEL_UPDATES);
            } else {
                setTitle(getString(R.string.title_activity_news_luas_news));

                webViewNews.loadUrl(URL_NEWS);
            }
        }
    }
}
