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

package org.thecosmicfrog.luasataglance.util;

import org.thecosmicfrog.luasataglance.activity.FaresActivity;
import org.thecosmicfrog.luasataglance.activity.FavouritesActivity;
import org.thecosmicfrog.luasataglance.activity.MainActivity;
import org.thecosmicfrog.luasataglance.activity.MapsActivity;
import org.thecosmicfrog.luasataglance.activity.NewsActivity;
import org.thecosmicfrog.luasataglance.activity.SettingsActivity;

public final class Constant {

    public static final Class CLASS_FARES_ACTIVITY = FaresActivity.class;
    public static final Class CLASS_FAVOURITES_ACTIVITY = FavouritesActivity.class;
    public static final Class CLASS_MAIN_ACTIVITY = MainActivity.class;
    public static final Class CLASS_MAPS_ACTIVITY = MapsActivity.class;
    public static final Class CLASS_NEWS_ACTIVITY = NewsActivity.class;
    public static final Class CLASS_SETTINGS_ACTIVITY = SettingsActivity.class;
    public static final String REMOTEMESSAGE_KEY_ACTIVITY_TO_OPEN = "activityToOpen";
    public static final String REMOTEMESSAGE_VALUE_ACTIVITY_FARES = "fares";
    public static final String REMOTEMESSAGE_VALUE_ACTIVITY_FAVOURITES = "favourites";
    public static final String REMOTEMESSAGE_VALUE_ACTIVITY_MAIN = "main";
    public static final String REMOTEMESSAGE_VALUE_ACTIVITY_MAPS = "maps";
    public static final String REMOTEMESSAGE_VALUE_ACTIVITY_NEWS = "news";
    public static final String REMOTEMESSAGE_VALUE_ACTIVITY_SETTINGS = "settings";
}
