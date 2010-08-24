/*
 * Tiny Tiny RSS Reader for Android
 * 
 * Copyright (C) 2009 J. Devauchelle and contributors.
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
import android.util.Log;

public class Constants {
	
	// Connection
	public static String CONNECTION_URL = "ConnectionUrlPreference";
	public static String CONNECTION_USERNAME = "ConnectionUsernamePreference";
	public static String CONNECTION_PASSWORD = "ConnectionPasswordPreference";
	public static String CONNECTION_TRUST_ALL_SSL = "ConnectionSSLPreference";
	public static String CONNECTION_USE_KEYSTORE = "ConnectionUseKeystorePreference";
	public static String CONNECTION_KEYSTORE_PASSWORD = "ConnectionKeystorePasswordPreference";
	
	// Usage
	public static String USAGE_AUTOMATIC_MARK_READ = "UsageAutomaticMarkReadPreference";
	public static String USAGE_OPEN_URL_EMPTY_ARTICLE = "UsageOpenUrlEmptyArticlePreference";
	public static String USAGE_UPDATE_UNREAD_ON_STARTUP = "UsageUpdateArticlesOnStartupPreference";
	public static String USAGE_REFRESH_SUB_DATA = "UsageRefreshSubDataPreference";
	public static String USAGE_USE_VOLUME_KEYS = "UsageUseVolumeKeysPreference";
	public static String USAGE_VIBRATE_ON_LAST_ARTICLE = "UsageVibrateOnLastArticlePreference";
	public static String USAGE_WORK_OFFLINE = "DisplayWorkOfflinePreference";
	
	// Display
	public static String DISPLAY_SHOW_VIRTUAL = "DisplayShowVirtualPreference";
	public static String DISPLAY_SHOW_VIRTUAL_UNREAD = "DisplayShowVirtualUnreadPreference";
	public static String DISPLAY_ALWAYS_FULL_REFRESH = "DisplayAlwaysFullRefreshPreference";
	public static String DISPLAY_USE_SWIPE = "DisplayUseSwipePreference";
	public static String DISPLAY_ONLY_UNREAD = "DisplayShowUnreadOnlyPreference";
	public static String DISPLAY_ARTICLE_LIMIT = "DisplayArticleLimitPreference";
	
	// Internal
	public static String DATABASE_VERSION = "DatabaseVersion";
	public static String LAST_UPDATE_TIME = "LastUpdateTime";
	
	/*
	 * Returns a list of the values of all constants in this class. Allows for easier watching the changes in the
	 * preferences-activity.
	 */
	public static List<String> getConstants() {
		List<String> ret = new ArrayList<String>();
		for (Field f : Constants.class.getFields()) {
			try {
				ret.add((String) f.get(null));
			} catch (IllegalArgumentException e) {
				Log.e(Utils.TAG, "IllegalArgumentException");
			} catch (IllegalAccessException e) {
				Log.e(Utils.TAG, "IllegalAccessException");
			}
		}
		return ret;
	}
}
