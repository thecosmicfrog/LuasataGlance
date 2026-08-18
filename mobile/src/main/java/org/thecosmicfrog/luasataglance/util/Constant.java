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

package org.thecosmicfrog.luasataglance.util;

public final class Constant {

    /*
     * General Luas parlance.
     */
    public static final String LINE = "line";
    public static final String RED_LINE = "red_line";
    public static final String GREEN_LINE = "green_line";
    public static final String NO_LINE = "no_line";
    public static final String STOP_NAME = "stopName";
    public static final String NOTIFY_STOP_NAME = "notifyStopName";
    public static final String NOTIFY_TIME = "notifyTime";

    /*
     * Bottom Navigation View.
     */
    public static final int BOTTOMNAV_MENU_ITEM_INDEX_TRAMS = 0;
    public static final int BOTTOMNAV_MENU_ITEM_INDEX_FAVOURITES = 1;
    public static final int BOTTOMNAV_MENU_ITEM_INDEX_MAP = 2;
    public static final int BOTTOMNAV_MENU_ITEM_INDEX_ALERTS = 3;

    /*
     * Broadcast actions.
     */
    public static final String INTENT_ACTION_LOAD_STOP = "load_stop";

    /*
     * Intent extras.
     */
    public static final String INTENT_EXTRA_STOP_NAME = "extra_stop_name";

    /*
     * Request codes for permissions.
     */
    public static final int REQUEST_CODE_NOTIFY_TIMES = 102;

    /*
     * Resources.
     */
    public static final String RES_ARRAY_STOPS_RED_LINE = "resArrayStopsRedLine";
    public static final String RES_ARRAY_STOPS_GREEN_LINE = "resArrayStopsGreenLine";

    /*
     * Tutorials.
     */
    public static final String TUTORIAL_SELECT_STOP = "select_stop";
}
