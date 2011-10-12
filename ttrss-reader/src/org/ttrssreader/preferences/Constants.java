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

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.ttrssreader.utils.Utils;
import android.content.SharedPreferences;
import android.os.Environment;

public class Constants {
    
    public static String EMPTY = "";
    public static String APPENDED_DEFAULT = "_DEFAULT";
    
    static {
        StringBuilder sbAttachments = new StringBuilder();
        sbAttachments.append(Environment.getExternalStorageDirectory()).append(File.separator).append(Utils.SDCARD_PATH_FILES);
        SAVE_ATTACHMENT_DEFAULT = sbAttachments.toString();
        
        StringBuilder sbCache = new StringBuilder();
        sbCache.append(Environment.getExternalStorageDirectory()).append(File.separator).append(Utils.SDCARD_PATH_CACHE);
        CACHE_FOLDER_DEFAULT = sbCache.toString();
    }
    
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
    public static String USE_VOLUME_KEYS = "UsageUseVolumeKeysPreference";
    public static String VIBRATE_ON_LAST_ARTICLE = "UsageVibrateOnLastArticlePreference";
    public static String WORK_OFFLINE = "UsageWorkOfflinePreference";
    // Usage Default Values
    public static boolean AUTOMATIC_MARK_READ_DEFAULT = true;
    public static boolean OPEN_URL_EMPTY_ARTICLE_DEFAULT = false;
    public static boolean USE_VOLUME_KEYS_DEFAULT = false;
    public static boolean VIBRATE_ON_LAST_ARTICLE_DEFAULT = true;
    public static boolean WORK_OFFLINE_DEFAULT = false;
    
    // Display
    public static String SHOW_VIRTUAL = "DisplayShowVirtualPreference";
    public static String USE_SWIPE = "DisplayUseSwipePreference";
    public static String USE_BUTTONS = "DisplayUseButtonsPreference";
    public static String LEFT_HANDED = "DisplayLeftHandedPreference";
    public static String ONLY_UNREAD = "DisplayShowUnreadOnlyPreference";
    public static String ARTICLE_LIMIT = "DisplayArticleLimitPreference";
    public static String DISPLAY_ARTICLE_HEADER = "DisplayArticleHeaderPreference";
    public static String INVERT_SORT_ARTICLELIST = "InvertSortArticlelistPreference";
    public static String INVERT_SORT_FEEDSCATS = "InvertSortFeedscatsPreference";
    public static String ALIGN_FLUSH_LEFT = "DisplayAlignFlushLeftPreference";
    public static String DATE_TIME_SYSTEM = "DisplayDateTimeFormatSystemPreference";
    public static String DATE_STRING = "DisplayDateFormatPreference";
    public static String TIME_STRING = "DisplayTimeFormatPreference";
    public static String DARK_BACKGROUND = "DisplayDarkBackgroundPreference";
    // Display Default Values
    public static boolean SHOW_VIRTUAL_DEFAULT = true;
    public static boolean USE_SWIPE_DEFAULT = true;
    public static boolean USE_BUTTONS_DEFAULT = false;
    public static boolean LEFT_HANDED_DEFAULT = false;
    public static boolean ONLY_UNREAD_DEFAULT = false;
    public static int ARTICLE_LIMIT_DEFAULT = 1000;
    public static boolean DISPLAY_ARTICLE_HEADER_DEFAULT = true;
    public static boolean INVERT_SORT_ARTICLELIST_DEFAULT = false;
    public static boolean INVERT_SORT_FEEDSCATS_DEFAULT = false;
    public static boolean ALIGN_FLUSH_LEFT_DEFAULT = false;
    public static boolean DATE_TIME_SYSTEM_DEFAULT = true;
    public static String DATE_STRING_DEFAULT = "dd.MM.yyyy";
    public static String TIME_STRING_DEFAULT = "kk:mm";
    public static boolean DARK_BACKGROUND_DEFAULT = false;
    
    // System
    public static String IMAGE_CACHE_SIZE = "StoreImageLimitPreference";
    public static String IMAGE_CACHE_UNREAD = "CacheImagesUnreadArticlesPreference";
    public static String SAVE_ATTACHMENT = "SaveAttachmentPreference";
    public static String CACHE_FOLDER = "CacheFolderPreference";
    public static String VACUUM_DB_SCHEDULED = "VacuumDBScheduledPreference";
    public static String DELETE_DB_SCHEDULED = "DeleteDBScheduledPreference";
    public static String DELETE_DB_ON_STARTUP = "DeleteDBOnStartupPreference";
    public static String SERVER_VERSION = "ServerVersion";
    public static String SERVER_VERSION_LAST_UPDATE = "ServerVersionLastUpdate";
    public static String CACHE_ON_STARTUP = "CacheOnStartupPreference";
    public static String CACHE_IMAGES_ON_STARTUP = "CacheImagesOnStartupPreference";
    public static String CACHE_IMAGES_ONLY_WIFI = "CacheImagesOnlyWifiPreference";
    public static String LOG_SENSITIVE_DATA = "LogSensitiveDataPreference";
    // System Default Values
    public static int IMAGE_CACHE_SIZE_DEFAULT = 50;
    public static boolean IMAGE_CACHE_UNREAD_DEFAULT = true;
    public static String SAVE_ATTACHMENT_DEFAULT;
    public static String CACHE_FOLDER_DEFAULT;
    public static boolean VACUUM_DB_SCHEDULED_DEFAULT = false;
    public static boolean DELETE_DB_SCHEDULED_DEFAULT = false;
    public static boolean DELETE_DB_ON_STARTUP_DEFAULT = false;
    public static int SERVER_VERSION_DEFAULT = -1;
    public static long SERVER_VERSION_LAST_UPDATE_DEFAULT = -1;
    public static boolean CACHE_ON_STARTUP_DEFAULT = false;
    public static boolean CACHE_IMAGES_ON_STARTUP_DEFAULT = false;
    public static boolean CACHE_IMAGES_ONLY_WIFI_DEFAULT = false;
    public static boolean LOG_SENSITIVE_DATA_DEFAULT = false;
    
    // Internal
    public static String DATABASE_VERSION = "DatabaseVersion";
    public static String LAST_UPDATE_TIME = "LastUpdateTime";
    public static String LAST_VERSION_RUN = "LastVersionRun";
    public static String LAST_VACUUM_DATE = "lastVacuumDate";
    // Internal Default Values
    public static int DATABASE_VERSION_DEFAULT = 1;
    public static long LAST_UPDATE_TIME_DEFAULT = 1;
    public static String LAST_VERSION_RUN_DEFAULT = "1";
    public static long LAST_VACUUM_DATE_DEFAULT = 0;
    
    /*
     * Returns a list of the values of all constants in this class which represent preferences. Allows for easier
     * watching the changes in the preferences-activity.
     */
    public static List<String> getConstants() {
        List<String> ret = new ArrayList<String>();
        
        // Iterate over all fields
        for (Field field : Constants.class.getDeclaredFields()) {
            // Continue on "_DEFAULT"-Fields, these hold only the default values for a preference
            if (field.getName().endsWith(APPENDED_DEFAULT))
                continue;
            
            try {
                // Return all String-Fields, these hold the preference-name
                if (field.get(null) instanceof String) {
                    ret.add((String) field.get(null));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }
    
    /*
     * Resets all preferences to their default values. Only preferences which are mentioned in this class are reset, old
     * or unsused values don't get reset.
     */
    public static void resetPreferences(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        
        // Iterate over all fields
        for (Field field : Constants.class.getDeclaredFields()) {
            
            // Continue on "_DEFAULT"-Fields, these hold only the default values for a preference
            if (field.getName().endsWith(APPENDED_DEFAULT))
                continue;
            
            try {
                // Get the default value
                Field fieldDefault = Constants.class.getDeclaredField(field.getName() + APPENDED_DEFAULT);
                String value = (String) field.get(new Constants());
                
                // Get the default type and store value for the specific type
                String type = fieldDefault.getType().getSimpleName();
                if (type.equals("String")) {
                    
                    String defaultValue = (String) fieldDefault.get(null);
                    editor.putString(value, defaultValue);
                    
                } else if (type.equals("boolean")) {
                    
                    boolean defaultValue = fieldDefault.getBoolean(null);
                    editor.putBoolean(value, defaultValue);
                    
                } else if (type.equals("int")) {
                    
                    int defaultValue = fieldDefault.getInt(null);
                    editor.putInt(value, defaultValue);
                    
                } else if (type.equals("long")) {
                    
                    long defaultValue = fieldDefault.getLong(null);
                    editor.putLong(value, defaultValue);
                    
                }
                
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                // Ignore, occurrs if a search for field like EMPTY_DEFAULT is started, this isn't there and shall never
                // be there.
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        
        // Commit when finished
        editor.commit();
    }
    
    public static String constant2Var(String s) {
        String[] parts = s.split("_");
        String camelCaseString = "";
        for (String part : parts) {
            camelCaseString = camelCaseString + toProperCase(part);
        }
        // We want the String to starrt with a lower-case letter...
        return camelCaseString.substring(0, 1).toLowerCase() + camelCaseString.substring(1);
    }
    
    static String toProperCase(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
    
}
