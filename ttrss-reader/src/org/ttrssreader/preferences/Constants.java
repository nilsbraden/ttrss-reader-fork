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
    public static final String AUTOMATIC_MARK_READ = "UsageAutomaticMarkReadPreference";
    public static final String OPEN_URL_EMPTY_ARTICLE = "UsageOpenUrlEmptyArticlePreference";
    public static final String USE_VOLUME_KEYS = "UsageUseVolumeKeysPreference";
    public static final String VIBRATE_ON_LAST_ARTICLE = "UsageVibrateOnLastArticlePreference";
    public static final String LOAD_IMAGES = "DisplayLoadImagesPreference";
    public static final String INVERT_BROWSING = "InvertBrowseArticlesPreference";
    public static final String WORK_OFFLINE = "UsageWorkOfflinePreference";
    // Usage Default Values
    public static final boolean AUTOMATIC_MARK_READ_DEFAULT = true;
    public static final boolean OPEN_URL_EMPTY_ARTICLE_DEFAULT = false;
    public static final boolean USE_VOLUME_KEYS_DEFAULT = false;
    public static final boolean VIBRATE_ON_LAST_ARTICLE_DEFAULT = true;
    public static final boolean LOAD_IMAGES_DEFAULT = true;
    public static final boolean INVERT_BROWSING_DEFAULT = false;
    public static final boolean WORK_OFFLINE_DEFAULT = false;

    // Display
    public static final String HEADLINE_SIZE = "HeadlineSizePreference";
    public static final String TEXT_ZOOM = "TextZoomPreference";
    public static final String SUPPORT_ZOOM_CONTROLS = "SupportZoomControlsPreference";
    public static final String ALLOW_HYPHENATION = "AllowHyphenationPreference";
    public static final String HYPHENATION_LANGUAGE = "HyphenationLanguagePreference";
    public static final String MARK_READ_IN_MENU = "MarkReadInMenuPreference";
    public static final String SHOW_VIRTUAL = "DisplayShowVirtualPreference";
    public static final String USE_SWIPE = "DisplayUseSwipePreference";
    public static final String USE_BUTTONS = "DisplayUseButtonsPreference";
    public static final String ONLY_UNREAD = "DisplayShowUnreadOnlyPreference";
    public static final String ARTICLE_LIMIT = "DisplayArticleLimitPreference";
    public static final String INVERT_SORT_ARTICLELIST = "InvertSortArticlelistPreference";
    public static final String INVERT_SORT_FEEDSCATS = "InvertSortFeedscatsPreference";
    public static final String ALIGN_FLUSH_LEFT = "DisplayAlignFlushLeftPreference";
    public static final String INJECT_ARTICLE_LINK = "DisplayArticleLinkPreference";
    public static final String DATE_TIME_SYSTEM = "DisplayDateTimeFormatSystemPreference";
    public static final String DATE_STRING = "DisplayDateFormatPreference";
    public static final String TIME_STRING = "DisplayTimeFormatPreference";
    public static final String DARK_BACKGROUND = "DisplayDarkBackgroundPreference";
    // Display Default Values
    public static final int HEADLINE_SIZE_DEFAULT = 16;
    public static final int TEXT_ZOOM_DEFAULT = 100;
    public static final boolean SUPPORT_ZOOM_CONTROLS_DEFAULT = true;
    public static final boolean ALLOW_HYPHENATION_DEFAULT = true;
    public static final String HYPHENATION_LANGUAGE_DEFAULT = "en-ru";
    public static final boolean MARK_READ_IN_MENU_DEFAULT = true;
    public static final boolean SHOW_VIRTUAL_DEFAULT = true;
    public static final boolean USE_SWIPE_DEFAULT = true;
    public static final boolean USE_BUTTONS_DEFAULT = false;
    public static final boolean ONLY_UNREAD_DEFAULT = false;
    public static final int ARTICLE_LIMIT_DEFAULT = 1000;
    public static final boolean INVERT_SORT_ARTICLELIST_DEFAULT = false;
    public static final boolean INVERT_SORT_FEEDSCATS_DEFAULT = false;
    public static final boolean ALIGN_FLUSH_LEFT_DEFAULT = false;
    public static final boolean INJECT_ARTICLE_LINK_DEFAULT = true;
    public static final boolean DATE_TIME_SYSTEM_DEFAULT = true;
    public static final String DATE_STRING_DEFAULT = "dd.MM.yyyy";
    public static final String TIME_STRING_DEFAULT = "kk:mm";
    public static final boolean DARK_BACKGROUND_DEFAULT = false;

    // System
    public static final String IMAGE_CACHE_SIZE = "StoreImageLimitPreference";
    public static final String IMAGE_CACHE_UNREAD = "CacheImagesUnreadArticlesPreference";
    public static final String SAVE_ATTACHMENT = "SaveAttachmentPreference";
    public static final String CACHE_FOLDER = "CacheFolderPreference";
    public static final String VACUUM_DB_SCHEDULED = "VacuumDBScheduledPreference";
    public static final String DELETE_DB_SCHEDULED = "DeleteDBScheduledPreference";
    public static final String DELETE_DB_ON_STARTUP = "DeleteDBOnStartupPreference";
    public static final String SERVER_VERSION = "ServerVersion";
    public static final String SERVER_VERSION_LAST_UPDATE = "ServerVersionLastUpdate";
    public static final String CACHE_ON_STARTUP = "CacheOnStartupPreference";
    public static final String CACHE_IMAGES_ON_STARTUP = "CacheImagesOnStartupPreference";
    public static final String CACHE_IMAGES_ONLY_WIFI = "CacheImagesOnlyWifiPreference";
    public static final String LOG_SENSITIVE_DATA = "LogSensitiveDataPreference";
    // System Default Values
    public static final int IMAGE_CACHE_SIZE_DEFAULT = 50;
    public static final boolean IMAGE_CACHE_UNREAD_DEFAULT = true;
    public static final String SAVE_ATTACHMENT_DEFAULT;
    public static final String CACHE_FOLDER_DEFAULT;
    public static final boolean VACUUM_DB_SCHEDULED_DEFAULT = false;
    public static final boolean DELETE_DB_SCHEDULED_DEFAULT = false;
    public static final boolean DELETE_DB_ON_STARTUP_DEFAULT = false;
    public static final int SERVER_VERSION_DEFAULT = -1;
    public static final long SERVER_VERSION_LAST_UPDATE_DEFAULT = -1;
    public static final boolean CACHE_ON_STARTUP_DEFAULT = false;
    public static final boolean CACHE_IMAGES_ON_STARTUP_DEFAULT = false;
    public static final boolean CACHE_IMAGES_ONLY_WIFI_DEFAULT = false;
    public static final boolean LOG_SENSITIVE_DATA_DEFAULT = false;

    // Internal
    public static final String API_LEVEL_UPDATED = "apiLevelUpdated";
    public static final String API_LEVEL = "apiLevel";
    public static final String APP_VERSION_CHECK_TIME = "appVersionCheckTime";
    public static final String APP_LATEST_VERSION = "appLatestVersion";
    public static final String DATABASE_VERSION = "DatabaseVersion";
    public static final String LAST_UPDATE_TIME = "LastUpdateTime";
    public static final String LAST_VERSION_RUN = "LastVersionRun";
    public static final String LAST_VACUUM_DATE = "lastVacuumDate";
    public static final String FRESH_ARTICLE_MAX_AGE = "freshArticleMaxAge";
    public static final String FRESH_ARTICLE_MAX_AGE_DATE = "freshArticleMaxAgeDate";
    public static final String SINCE_ID = "sinceId";
    // Internal Default Values
    public static final long API_LEVEL_UPDATED_DEFAULT = -1;
    public static final int API_LEVEL_DEFAULT = -1;
    public static final long APP_VERSION_CHECK_TIME_DEFAULT = 0;
    public static final int APP_LATEST_VERSION_DEFAULT = 0;
    public static final int DATABASE_VERSION_DEFAULT = 1;
    public static final long LAST_UPDATE_TIME_DEFAULT = 1;
    public static final String LAST_VERSION_RUN_DEFAULT = "1";
    public static final long LAST_VACUUM_DATE_DEFAULT = 0;
    public static final long FRESH_ARTICLE_MAX_AGE_DEFAULT = Utils.DAY;
    public static final long FRESH_ARTICLE_MAX_AGE_DATE_DEFAULT = 0;
    public static final int SINCE_ID_DEFAULT = 0;

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
