/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 Nils Braden
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
import java.util.Locale;
import org.ttrssreader.utils.FileUtils;
import org.ttrssreader.utils.Utils;
import android.content.SharedPreferences;
import android.os.Environment;

public class Constants {
    
    public static final String EMPTY = "";
    public static final String APPENDED_DEFAULT = "_DEFAULT";
    
    static {
        StringBuilder sbAttachments = new StringBuilder();
        sbAttachments.append(Environment.getExternalStorageDirectory()).append(File.separator)
                .append(FileUtils.SDCARD_PATH_FILES);
        SAVE_ATTACHMENT_DEFAULT = sbAttachments.toString();
        
        StringBuilder sbCache = new StringBuilder();
        sbCache.append(Environment.getExternalStorageDirectory()).append(File.separator)
                .append(FileUtils.SDCARD_PATH_CACHE);
        CACHE_FOLDER_DEFAULT = sbCache.toString();
    }
    
    // Connection
    public static final String URL = "ConnectionUrlPreference";
    public static final String USERNAME = "ConnectionUsernamePreference";
    public static final String PASSWORD = "ConnectionPasswordPreference";
    public static final String USE_HTTP_AUTH = "ConnectionHttpPreference";
    public static final String HTTP_USERNAME = "ConnectionHttpUsernamePreference";
    public static final String HTTP_PASSWORD = "ConnectionHttpPasswordPreference";
    public static final String TRUST_ALL_SSL = "ConnectionSSLPreference";
    public static final String TRUST_ALL_HOSTS = "ConnectionTrustHostsPreference";
    public static final String USE_OLD_CONNECTOR = "ConnectionUseOldConnector";
    public static final String USE_KEYSTORE = "ConnectionUseKeystorePreference";
    public static final String KEYSTORE_PASSWORD = "ConnectionKeystorePasswordPreference";
    public static final String USE_OF_A_LAZY_SERVER = "ConnectionLazyServerPreference";
    // Connection Default Values
    public static final String URL_DEFAULT = "http://localhost/";
    public static final boolean USE_HTTP_AUTH_DEFAULT = false;
    public static final boolean TRUST_ALL_SSL_DEFAULT = false;
    public static final boolean TRUST_ALL_HOSTS_DEFAULT = false;
    public static final boolean USE_OLD_CONNECTOR_DEFAULT = false;
    public static final boolean USE_KEYSTORE_DEFAULT = false;
    public static final boolean USE_OF_A_LAZY_SERVER_DEFAULT = false;
    
    // Usage
    public static final String OPEN_URL_EMPTY_ARTICLE = "UsageOpenUrlEmptyArticlePreference";
    public static final String USE_VOLUME_KEYS = "UsageUseVolumeKeysPreference";
    public static final String LOAD_IMAGES = "DisplayLoadImagesPreference";
    public static final String INVERT_BROWSING = "InvertBrowseArticlesPreference";
    public static final String WORK_OFFLINE = "UsageWorkOfflinePreference";
    public static final String GO_BACK_AFTER_MARK_ALL_READ = "GoBackAfterMarkAllReadPreference";
    public static final String HIDE_ACTIONBAR = "HideActionbarPreference";
    public static final String ALLOW_TABLET_LAYOUT = "AllowTabletLayoutPreference";
    // Usage Default Values
    public static final boolean OPEN_URL_EMPTY_ARTICLE_DEFAULT = false;
    public static final boolean USE_VOLUME_KEYS_DEFAULT = false;
    public static final boolean LOAD_IMAGES_DEFAULT = true;
    public static final boolean INVERT_BROWSING_DEFAULT = false;
    public static final boolean WORK_OFFLINE_DEFAULT = false;
    public static final boolean GO_BACK_AFTER_MARK_ALL_READ_DEFAULT = false;
    public static final boolean HIDE_ACTIONBAR_DEFAULT = true;
    public static final boolean ALLOW_TABLET_LAYOUT_DEFAULT = true;
    
    // Display
    public static final String HEADLINE_SIZE = "HeadlineSizePreference";
    public static final String TEXT_ZOOM = "TextZoomPreference";
    public static final String SUPPORT_ZOOM_CONTROLS = "SupportZoomControlsPreference";
    public static final String ALLOW_HYPHENATION = "AllowHyphenationPreference";
    public static final String HYPHENATION_LANGUAGE = "HyphenationLanguagePreference";
    public static final String SHOW_VIRTUAL = "DisplayShowVirtualPreference";
    public static final String SHOW_BUTTONS_MODE = "ShowButtonsModePreference";
    public static final String ONLY_UNREAD = "DisplayShowUnreadOnlyPreference";
    public static final String INVERT_SORT_ARTICLELIST = "InvertSortArticlelistPreference";
    public static final String INVERT_SORT_FEEDSCATS = "InvertSortFeedscatsPreference";
    public static final String ALIGN_FLUSH_LEFT = "DisplayAlignFlushLeftPreference";
    public static final String DATE_TIME_SYSTEM = "DisplayDateTimeFormatSystemPreference";
    public static final String DATE_STRING = "DisplayDateFormatPreference";
    public static final String TIME_STRING = "DisplayTimeFormatPreference";
    public static final String DATE_TIME_STRING = "DisplayDateTimeFormatPreference";
    public static final String THEME = "DisplayThemePreference";
    // Display Default Values
    public static final int HEADLINE_SIZE_DEFAULT = 16;
    public static final int TEXT_ZOOM_DEFAULT = 100;
    public static final boolean SUPPORT_ZOOM_CONTROLS_DEFAULT = true;
    public static final boolean ALLOW_HYPHENATION_DEFAULT = false;
    public static final String HYPHENATION_LANGUAGE_DEFAULT = "en-gb";
    public static final boolean SHOW_VIRTUAL_DEFAULT = true;
    public static final String SHOW_BUTTONS_MODE_DEFAULT = "0";
    public static final int SHOW_BUTTONS_MODE_NONE = 0;
    public static final int SHOW_BUTTONS_MODE_ALLWAYS = 1;
    public static final int SHOW_BUTTONS_MODE_HTML = 2;
    public static final boolean ONLY_UNREAD_DEFAULT = false;
    public static final boolean INVERT_SORT_ARTICLELIST_DEFAULT = false;
    public static final boolean INVERT_SORT_FEEDSCATS_DEFAULT = false;
    public static final boolean ALIGN_FLUSH_LEFT_DEFAULT = false;
    public static final boolean DATE_TIME_SYSTEM_DEFAULT = true;
    public static final String DATE_STRING_DEFAULT = "dd.MM.yyyy";
    public static final String TIME_STRING_DEFAULT = "kk:mm";
    public static final String DATE_TIME_STRING_DEFAULT = "dd.MM.yyyy kk:mm";
    public static final String THEME_DEFAULT = "1";
    
    // System
    public static final String SAVE_ATTACHMENT = "SaveAttachmentPreference";
    public static final String CACHE_FOLDER = "CacheFolderPreference";
    public static final String CACHE_FOLDER_MAX_SIZE = "CacheFolderMaxSizePreference";
    public static final String CACHE_IMAGE_MAX_SIZE = "CacheImageMaxSizePreference";
    public static final String CACHE_IMAGE_MIN_SIZE = "CacheImageMinSizePreference";
    public static final String DELETE_DB_SCHEDULED = "DeleteDBScheduledPreference";
    public static final String CACHE_IMAGES_ON_STARTUP = "CacheImagesOnStartupPreference";
    public static final String CACHE_IMAGES_ONLY_WIFI = "CacheImagesOnlyWifiPreference";
    public static final String ONLY_USE_WIFI = "OnlyUseWifiPreference";
    public static final String NO_CRASHREPORTS = "NoCrashreportsPreference";
    public static final String NO_CRASHREPORTS_UNTIL_UPDATE = "NoCrashreportsUntilUpdatePreference";
    // System Default Values
    public static final String SAVE_ATTACHMENT_DEFAULT;
    public static final String CACHE_FOLDER_DEFAULT;
    public static final Integer CACHE_FOLDER_MAX_SIZE_DEFAULT = 80;
    public static final Integer CACHE_IMAGE_MAX_SIZE_DEFAULT = 6 * (int) Utils.MB; // 6 MB
    public static final Integer CACHE_IMAGE_MIN_SIZE_DEFAULT = 32 * (int) Utils.KB; // 64 KB
    public static final boolean DELETE_DB_SCHEDULED_DEFAULT = false;
    public static final boolean CACHE_IMAGES_ON_STARTUP_DEFAULT = false;
    public static final boolean CACHE_IMAGES_ONLY_WIFI_DEFAULT = false;
    public static final boolean ONLY_USE_WIFI_DEFAULT = false;
    public static final boolean NO_CRASHREPORTS_DEFAULT = false;
    public static final boolean NO_CRASHREPORTS_UNTIL_UPDATE_DEFAULT = false;
    
    // Internal
    public static final String API_LEVEL_UPDATED = "apiLevelUpdated";
    public static final String API_LEVEL = "apiLevel";
    public static final String APP_VERSION_CHECK_TIME = "appVersionCheckTime";
    public static final String APP_LATEST_VERSION = "appLatestVersion";
    public static final String LAST_UPDATE_TIME = "LastUpdateTime";
    public static final String LAST_VERSION_RUN = "LastVersionRun";
    public static final String FRESH_ARTICLE_MAX_AGE = "freshArticleMaxAge";
    public static final String FRESH_ARTICLE_MAX_AGE_DATE = "freshArticleMaxAgeDate";
    public static final String SINCE_ID = "sinceId";
    public static final String LAST_SYNC = "lastSync";
    public static final String LAST_CLEANUP = "lastCleanup";
    // Internal Default Values
    public static final long API_LEVEL_UPDATED_DEFAULT = -1;
    public static final int API_LEVEL_DEFAULT = -1;
    public static final long APP_VERSION_CHECK_TIME_DEFAULT = 0;
    public static final int APP_LATEST_VERSION_DEFAULT = 0;
    public static final long LAST_UPDATE_TIME_DEFAULT = 1;
    public static final String LAST_VERSION_RUN_DEFAULT = "1";
    public static final long FRESH_ARTICLE_MAX_AGE_DEFAULT = Utils.DAY;
    public static final long FRESH_ARTICLE_MAX_AGE_DATE_DEFAULT = 0;
    public static final int SINCE_ID_DEFAULT = 0;
    public static final long LAST_SYNC_DEFAULT = 0l;
    public static final long LAST_CLEANUP_DEFAULT = 0l;
    
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
        return camelCaseString.substring(0, 1).toLowerCase(Locale.getDefault()) + camelCaseString.substring(1);
    }
    
    static String toProperCase(String s) {
        return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1).toLowerCase(Locale.getDefault());
    }
    
}
