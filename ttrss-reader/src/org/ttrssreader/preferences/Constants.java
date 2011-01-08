/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
 * Copyright (C) 2009-2010 J. Devauchelle.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

package org.ttrssreader.preferences;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.ttrssreader.utils.Utils;
import android.content.SharedPreferences;
import android.util.Log;

public class Constants {
    
    public static String EMPTY = "";
    
    // Connection
    public static String URL = "ConnectionUrlPreference";
    public static String USERNAME = "ConnectionUsernamePreference";
    public static String PASSWORD = "ConnectionPasswordPreference";
    public static String USE_HTTP_AUTH = "ConnectionHttpPreference";
    public static String HTTP_USERNAME = "ConnectionHttpUsernamePreference";
    public static String HTTP_PASSWORD = "ConnectionHttpPasswordPreference";
    public static String TRUST_ALL_SSL = "ConnectionSSLPreference";
    public static String USE_KEYSTORE = "ConnectionUseKeystorePreference";
    public static String KEYSTORE_PASSWORD = "ConnectionKeystorePasswordPreference";
    // Connection Default Values
    public static String URL_DEFAULT = "http://localhost/";
    public static boolean USE_HTTP_AUTH_DEFAULT = false;
    public static boolean TRUST_ALL_SSL_DEFAULT = false;
    public static boolean USE_KEYSTORE_DEFAULT = false;
    
    // Usage
    public static String AUTOMATIC_MARK_READ = "UsageAutomaticMarkReadPreference";
    public static String OPEN_URL_EMPTY_ARTICLE = "UsageOpenUrlEmptyArticlePreference";
    public static String UPDATE_UNREAD_ON_STARTUP = "UsageUpdateArticlesOnStartupPreference";
    public static String USE_VOLUME_KEYS = "UsageUseVolumeKeysPreference";
    public static String VIBRATE_ON_LAST_ARTICLE = "UsageVibrateOnLastArticlePreference";
    public static String WORK_OFFLINE = "DisplayWorkOfflinePreference";
    // Usage Default Values
    public static boolean AUTOMATIC_MARK_READ_DEFAULT = true;
    public static boolean OPEN_URL_EMPTY_ARTICLE_DEFAULT = false;
    public static boolean UPDATE_UNREAD_ON_STARTUP_DEFAULT = false;
    public static boolean USE_VOLUME_KEYS_DEFAULT = false;
    public static boolean VIBRATE_ON_LAST_ARTICLE_DEFAULT = true;
    public static boolean WORK_OFFLINE_DEFAULT = false;
    
    // Display
    public static String SHOW_VIRTUAL = "DisplayShowVirtualPreference";
    public static String USE_SWIPE = "DisplayUseSwipePreference";
    public static String ONLY_UNREAD = "DisplayShowUnreadOnlyPreference";
    public static String ARTICLE_LIMIT = "DisplayArticleLimitPreference";
    public static String IMAGE_CACHE_SIZE = "StoreImageLimitPreference";
    public static String IMAGE_CACHE_AGE = "StoreImageAgeLimitPreference";
    // Display Default Values
    public static boolean SHOW_VIRTUAL_DEFAULT = true;
    public static boolean USE_SWIPE_DEFAULT = true;
    public static boolean ONLY_UNREAD_DEFAULT = false;
    public static int ARTICLE_LIMIT_DEFAULT = 1000;
    public static int IMAGE_CACHE_SIZE_DEFAULT = 400;
    public static int IMAGE_CACHE_AGE_DEFAULT = 14;
    
    // Internal
    public static String DATABASE_VERSION = "DatabaseVersion";
    public static String LAST_UPDATE_TIME = "LastUpdateTime";
    public static String LAST_VERSION_RUN = "LastVersionRun";
    // Internal Default Values
    public static int DATABASE_VERSION_DEFAULT = 1;
    public static long LAST_UPDATE_TIME_DEFAULT = 1;
    public static String LAST_VERSION_RUN_DEFAULT = "1";
    
    /*
     * Returns a list of the values of all constants in this class. Allows for easier watching the changes in the
     * preferences-activity.
     */
    public static List<String> getConstants() {
        List<String> ret = new ArrayList<String>();
        for (Field f : Constants.class.getFields()) {
            try {
                if (f.get(null) instanceof String) {
                    ret.add((String) f.get(null));
                }
            } catch (IllegalArgumentException e) {
                Log.e(Utils.TAG, "IllegalArgumentException");
            } catch (IllegalAccessException e) {
                Log.e(Utils.TAG, "IllegalAccessException");
            }
        }
        return ret;
    }
    
    public static void resetPreferences(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(URL, URL_DEFAULT);
        editor.putString(USERNAME, EMPTY);
        editor.putString(PASSWORD, EMPTY);
        editor.putBoolean(USE_HTTP_AUTH, USE_HTTP_AUTH_DEFAULT);
        editor.putString(HTTP_USERNAME, EMPTY);
        editor.putString(HTTP_PASSWORD, EMPTY);
        editor.putBoolean(TRUST_ALL_SSL, TRUST_ALL_SSL_DEFAULT);
        editor.putBoolean(USE_KEYSTORE, USE_KEYSTORE_DEFAULT);
        editor.putString(KEYSTORE_PASSWORD, EMPTY);
        
        editor.putBoolean(AUTOMATIC_MARK_READ, AUTOMATIC_MARK_READ_DEFAULT);
        editor.putBoolean(OPEN_URL_EMPTY_ARTICLE, OPEN_URL_EMPTY_ARTICLE_DEFAULT);
        editor.putBoolean(UPDATE_UNREAD_ON_STARTUP, UPDATE_UNREAD_ON_STARTUP_DEFAULT);
        editor.putBoolean(USE_VOLUME_KEYS, USE_VOLUME_KEYS_DEFAULT);
        editor.putBoolean(VIBRATE_ON_LAST_ARTICLE, VIBRATE_ON_LAST_ARTICLE_DEFAULT);
        editor.putBoolean(WORK_OFFLINE, WORK_OFFLINE_DEFAULT);
        
        editor.putBoolean(SHOW_VIRTUAL, SHOW_VIRTUAL_DEFAULT);
        editor.putBoolean(USE_SWIPE, USE_SWIPE_DEFAULT);
        editor.putBoolean(ONLY_UNREAD, ONLY_UNREAD_DEFAULT);
        editor.putInt(ARTICLE_LIMIT, ARTICLE_LIMIT_DEFAULT);
        editor.putInt(IMAGE_CACHE_SIZE, IMAGE_CACHE_SIZE_DEFAULT);
        editor.putInt(IMAGE_CACHE_AGE, IMAGE_CACHE_AGE_DEFAULT);
        
        editor.putInt(DATABASE_VERSION, DATABASE_VERSION_DEFAULT);
        editor.putLong(LAST_UPDATE_TIME, LAST_UPDATE_TIME_DEFAULT);
        editor.putString(LAST_VERSION_RUN, LAST_VERSION_RUN_DEFAULT);
        editor.commit();
    }
}
