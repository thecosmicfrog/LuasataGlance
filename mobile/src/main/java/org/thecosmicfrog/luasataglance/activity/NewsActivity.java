package org.thecosmicfrog.luasataglance.activity;

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

        // Load the "Travel Updates" section of the Luas mobile website.
        webViewNews = (WebView) findViewById(R.id.webview_news);
        webViewNews.loadUrl(URL_NEWS);
    }
}
