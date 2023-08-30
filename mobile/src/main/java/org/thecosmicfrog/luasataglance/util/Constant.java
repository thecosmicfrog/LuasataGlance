/**
 * @author Aaron Hastings
 *
 * Copyright 2015-2019 Aaron Hastings
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

package org.thecosmicfrog.luasataglance.util;

import org.thecosmicfrog.luasataglance.activity.FaresActivity;
import org.thecosmicfrog.luasataglance.activity.FavouritesActivity;
import org.thecosmicfrog.luasataglance.activity.MainActivity;
import org.thecosmicfrog.luasataglance.activity.MapsActivity;
import org.thecosmicfrog.luasataglance.activity.NewsActivity;
import org.thecosmicfrog.luasataglance.activity.SettingsActivity;

public final class Constant {

    /*
     * General Luas parlance.
     */
    public static final String LINE = "line";
    public static final String RED_LINE = "red_line";
    public static final String GREEN_LINE = "green_line";
    public static final String NO_LINE = "no_line";
    public static final String STOP_NAME = "stopName";
    public static final String SELECTED_STOP_NAME = "selectedStopName";
    public static final String NOTIFY_STOP_NAME = "notifyStopName";
    public static final String NOTIFY_TIME = "notifyTime";
    public static final String STOP_FORECAST = "stop_forecast";

    /*
     * Android Wear API paths.
     */
    public static final String PATH_FAVOURITES_OPEN_APP_MOBILE = "/favourites_open_app_mobile";
    public static final String PATH_FAVOURITES_FETCH_MOBILE = "/favourites_fetch_mobile";
    public static final String PATH_FAVOURITES_FETCH_WEAR = "/favourites_fetch_wear";
    public static final String PATH_STOPFORECAST_FETCH_MOBILE = "/stopforecast_fetch_mobile";
    public static final String PATH_STOPFORECAST_FETCH_WEAR = "/stopforecast_fetch_wear";

    /*
     * Classes.
     */
    public static final Class CLASS_FARES_ACTIVITY = FaresActivity.class;
    public static final Class CLASS_FAVOURITES_ACTIVITY = FavouritesActivity.class;
    public static final Class CLASS_MAIN_ACTIVITY = MainActivity.class;
    public static final Class CLASS_MAPS_ACTIVITY = MapsActivity.class;
    public static final Class CLASS_NEWS_ACTIVITY = NewsActivity.class;
    public static final Class CLASS_SETTINGS_ACTIVITY = SettingsActivity.class;

    /*
     * Firebase messaging.
     */
    public static final String NOTIFICATIONS = "notifications";

    /*
     * News.
     */
    public static final String NEWS_TYPE = "newsType";
    public static final String NEWS_TYPE_LUAS_NEWS = "luasNews";
    public static final String NEWS_TYPE_TRAVEL_UPDATES = "travelUpdates";

    /*
     * RemoteMessage keys and values.
     */
    public static final String REMOTEMESSAGE_KEY_ACTIVITY_TO_OPEN = "activityToOpen";
    public static final String REMOTEMESSAGE_VALUE_ACTIVITY_FARES = "fares";
    public static final String REMOTEMESSAGE_VALUE_ACTIVITY_FAVOURITES = "favourites";
    public static final String REMOTEMESSAGE_VALUE_ACTIVITY_MAIN = "main";
    public static final String REMOTEMESSAGE_VALUE_ACTIVITY_MAPS = "maps";
    public static final String REMOTEMESSAGE_VALUE_ACTIVITY_NEWS = "news";
    public static final String REMOTEMESSAGE_VALUE_ACTIVITY_SETTINGS = "settings";

    /*
     * Request codes for permissions.
     */
    public static final int REQUEST_CODE_NOTIFY_TIMES = 102;

    /*
     * Resources.
     */
    public static final String RES_LAYOUT_FRAGMENT_LINE = "resLayoutFragmentLine";
    public static final String RES_MENU_LINE = "resMenuLine";
    public static final String RES_PROGRESSBAR = "resProgressBar";
    public static final String RES_SPINNER_CARDVIEW = "resSpinnerCardView";
    public static final String RES_STATUS_CARDVIEW = "resStatusCardView";
    public static final String RES_SWIPEREFRESHLAYOUT = "resSwipeRefreshLayout";
    public static final String RES_SCROLLVIEW = "resScrollView";
    public static final String RES_INBOUND_STOPFORECASTCARDVIEW =
            "resInboundStopForecastCardView";
    public static final String RES_OUTBOUND_STOPFORECASTCARDVIEW =
            "resOutboundStopForecastCardView";
    public static final String RES_ARRAY_STOPS_RED_LINE = "resArrayStopsRedLine";
    public static final String RES_ARRAY_STOPS_GREEN_LINE = "resArrayStopsGreenLine";

    /* Tutorials. */
    public static final String TUTORIAL_SELECT_STOP = "select_stop";
    public static final String TUTORIAL_NOTIFICATIONS = "notifications";
    public static final String TUTORIAL_FAVOURITES = "favourites";
}
