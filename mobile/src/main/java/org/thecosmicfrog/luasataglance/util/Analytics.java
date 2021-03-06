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

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.thecosmicfrog.luasataglance.activity.LineFragment;

public final class Analytics {

    private static final String EVENT_API_CREATED_PARSE_ERROR = "api_created_parse_error";
    private static final String EVENT_DISABLE_WIDGET = "disable_widget";
    private static final String EVENT_ENABLE_WIDGET = "enable_widget";
    private static final String EVENT_HTTP_ERROR = "http_error";
    private static final String EVENT_HTTP_ERROR_WEAR = "http_error_wear";
    private static final String EVENT_HTTP_ERROR_WIDGET = "http_error_widget";
    private static final String EVENT_PERMISSION_LOCATION_DENIED = "permission_location_denied";
    private static final String EVENT_PERMISSION_LOCATION_GRANTED = "permission_location_granted";
    private static final String EVENT_PERMISSION_RATIONALE_LOCATION_DENIED =
            "permission_rationale_location_denied";
    private static final String EVENT_PERMISSION_RATIONALE_LOCATION_ACCEPTED =
            "permission_rationale_location_accepted";
    private static final String EVENT_NULL_APITIMES = "null_apitimes";
    private static final String LOG_TAG = Analytics.class.getSimpleName();

    private static FirebaseAnalytics firebaseAnalytics;

    public static void apiCreatedParseError(Context context, String contentType, String itemId) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error gathering analytics for " + itemId + ".");
            Log.e(LOG_TAG, Log.getStackTraceString(e));
        }

        if (firebaseAnalytics != null) {
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, contentType);
            params.putString(FirebaseAnalytics.Param.ITEM_ID, itemId);
            firebaseAnalytics.logEvent(EVENT_API_CREATED_PARSE_ERROR, params);
        }
    }

    public static void disableWidget(Context context, String contentType, String itemId) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error gathering analytics for " + itemId + ".");
            Log.e(LOG_TAG, Log.getStackTraceString(e));
        }

        if (firebaseAnalytics != null) {
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, contentType);
            params.putString(FirebaseAnalytics.Param.ITEM_ID, itemId);
            firebaseAnalytics.logEvent(EVENT_DISABLE_WIDGET, params);
        }
    }

    public static void enableWidget(Context context, String contentType, String itemId) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error gathering analytics for " + itemId + ".");
            Log.e(LOG_TAG, Log.getStackTraceString(e));
        }

        if (firebaseAnalytics != null) {
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, contentType);
            params.putString(FirebaseAnalytics.Param.ITEM_ID, itemId);
            firebaseAnalytics.logEvent(EVENT_ENABLE_WIDGET, params);
        }
    }

    public static void httpError(Context context, String contentType, String itemId) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error gathering analytics for " + itemId + ".");
            Log.e(LOG_TAG, Log.getStackTraceString(e));
        }

        if (firebaseAnalytics != null) {
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, contentType);
            params.putString(FirebaseAnalytics.Param.ITEM_ID, itemId);
            firebaseAnalytics.logEvent(EVENT_HTTP_ERROR, params);
        }
    }

    public static void httpErrorWear(Context context, String contentType, String itemId) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error gathering analytics for " + itemId + ".");
            Log.e(LOG_TAG, Log.getStackTraceString(e));
        }

        if (firebaseAnalytics != null) {
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, contentType);
            params.putString(FirebaseAnalytics.Param.ITEM_ID, itemId);
            firebaseAnalytics.logEvent(EVENT_HTTP_ERROR_WEAR, params);
        }
    }

    public static void httpErrorWidget(Context context, String contentType, String itemId) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error gathering analytics for " + itemId + ".");
            Log.e(LOG_TAG, Log.getStackTraceString(e));
        }

        if (firebaseAnalytics != null) {
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, contentType);
            params.putString(FirebaseAnalytics.Param.ITEM_ID, itemId);
            firebaseAnalytics.logEvent(EVENT_HTTP_ERROR_WIDGET, params);
        }
    }

    public static void nullApitimes(Context context, String contentType, String itemId) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error gathering analytics for " + itemId + ".");
            Log.e(LOG_TAG, Log.getStackTraceString(e));
        }

        if (firebaseAnalytics != null) {
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, contentType);
            params.putString(FirebaseAnalytics.Param.ITEM_ID, itemId);
            firebaseAnalytics.logEvent(EVENT_NULL_APITIMES, params);
        }
    }

    public static void permissionLocationDenied(
            Context context, String contentType, String itemId) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error gathering analytics for " + itemId + ".");
            Log.e(LOG_TAG, Log.getStackTraceString(e));
        }

        if (firebaseAnalytics != null) {
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, contentType);
            params.putString(FirebaseAnalytics.Param.ITEM_ID, itemId);
            firebaseAnalytics.logEvent(EVENT_PERMISSION_LOCATION_DENIED, params);
        }
    }

    public static void permissionLocationGranted(
            Context context, String contentType, String itemId) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error gathering analytics for " + itemId + ".");
            Log.e(LOG_TAG, Log.getStackTraceString(e));
        }

        if (firebaseAnalytics != null) {
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, contentType);
            params.putString(FirebaseAnalytics.Param.ITEM_ID, itemId);
            firebaseAnalytics.logEvent(EVENT_PERMISSION_LOCATION_GRANTED, params);
        }
    }

    public static void permissionRationaleLocationDenied(
            Context context, String contentType, String itemId) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error gathering analytics for " + itemId + ".");
            Log.e(LOG_TAG, Log.getStackTraceString(e));
        }

        if (firebaseAnalytics != null) {
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, contentType);
            params.putString(FirebaseAnalytics.Param.ITEM_ID, itemId);
            firebaseAnalytics.logEvent(EVENT_PERMISSION_RATIONALE_LOCATION_DENIED, params);
        }
    }

    public static void permissionRationaleLocationAccepted(
            Context context, String contentType, String itemId) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error gathering analytics for " + itemId + ".");
            Log.e(LOG_TAG, Log.getStackTraceString(e));
        }

        if (firebaseAnalytics != null) {
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, contentType);
            params.putString(FirebaseAnalytics.Param.ITEM_ID, itemId);
            firebaseAnalytics.logEvent(EVENT_PERMISSION_RATIONALE_LOCATION_ACCEPTED, params);
        }
    }

    public static void selectContent(Context context, String contentType, String itemId) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error gathering analytics for " + itemId + ".");
            Log.e(LOG_TAG, Log.getStackTraceString(e));
        }

        if (firebaseAnalytics != null) {
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, contentType);
            params.putString(FirebaseAnalytics.Param.ITEM_ID, itemId);
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, params);
        }
    }

    public static void tutorialBegin(Context context, String contentType, String itemId) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error gathering analytics for " + itemId + ".");
            Log.e(LOG_TAG, Log.getStackTraceString(e));
        }

        if (firebaseAnalytics != null) {
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, contentType);
            params.putString(FirebaseAnalytics.Param.ITEM_ID, itemId);
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_BEGIN, params);
        }
    }
}
