/**
 * @author Aaron Hastings
 *
 * Copyright 2015 Aaron Hastings
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
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.webkit.WebView;

import org.thecosmicfrog.luasataglance.R;

public class NewsActivity extends ActionBarActivity {

    private final String URL_NEWS = "http://m.luas.ie/travel-updates.html";
    private WebView webViewNews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable((getResources().getColor(R.color.luas_purple))));

        // Load the "Travel Updates" section of the Luas mobile website.
        webViewNews = (WebView) findViewById(R.id.webview_news);
        webViewNews.loadUrl(URL_NEWS);
    }
}
