/*
 * Copyright (c) 2015, Nils Braden
 *
 * This file is part of ttrss-reader-fork. This program is free software; you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation;
 * either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program; If
 * not, see http://www.gnu.org/licenses/.
 */

package org.ttrssreader.preferences;

import android.content.SharedPreferences;
import android.util.Log;

import org.ttrssreader.utils.Utils;

import java.lang.reflect.Field;
import java.util.Locale;

public class Constants {

	private static final String TAG = Constants.class.getSimpleName();

	public static final String EMPTY = "";
	public static final String APPENDED_DEFAULT = "_DEFAULT";

	// Connection
	public static final String URL = "ConnectionUrlPreference";
	public static final String URL_FEEDICONS = "ConnectionUrlFeedIconsPreference";
	//	public static final String SESSION_ID = "ConnectionSessionId";
	public static final String USERNAME = "ConnectionUsernamePreference";
	public static final String PASSWORD = "ConnectionPasswordPreference";
	public static final String USE_HTTP_AUTH = "ConnectionHttpPreference";
	public static final String HTTP_USERNAME = "ConnectionHttpUsernamePreference";
	public static final String HTTP_PASSWORD = "ConnectionHttpPasswordPreference";
	public static final String USE_PROVIDER_INSTALLER = "ConnectionProviderInstall";
	public static final String TRUST_ALL_SSL = "ConnectionSSLPreference";
	public static final String USE_CLIENT_CERT = "ConnectionUseClientCertificatePreference";
	public static final String TRUST_ALL_HOSTS = "ConnectionTrustHostsPreference";
	public static final String USE_KEYSTORE = "ConnectionUseKeystorePreference";
	public static final String KEYSTORE_PASSWORD = "ConnectionKeystorePasswordPreference";
	public static final String CLIENT_CERTIFICATE = "ConnectionClientCertificatePreference";
	public static final String USE_OF_A_LAZY_SERVER = "ConnectionLazyServerPreference";
	public static final String USE_PROXY = "ConnectionProxyPreference";
	public static final String PROXY_HOST = "ConnectionProxyHostPreference";
	public static final String PROXY_PORT = "ConnectionProxyPortPreference";
	public static final String IGNORE_UNSAFE_CONNECTION_ERROR = "IgnoreUnsafeConnectionError";

	// Connection Default Values
	public static final String URL_DEFAULT = "https://localhost/";
	public static final String URL_DEFAULT_FEEDICONS = "feed-icons";
	public static final boolean USE_HTTP_AUTH_DEFAULT = false;
	public static final boolean USE_PROVIDER_INSTALLER_DEFAULT = true;
	public static final boolean TRUST_ALL_SSL_DEFAULT = false;
	public static final boolean USE_CLIENT_CERT_DEFAULT = false;
	public static final boolean TRUST_ALL_HOSTS_DEFAULT = false;
	public static final boolean USE_KEYSTORE_DEFAULT = false;
	public static final boolean USE_OF_A_LAZY_SERVER_DEFAULT = false;
	public static final boolean IGNORE_UNSAFE_CONNECTION_ERROR_DEFAULT = false;
	public static final int PROXY_PORT_DEFAULT = 1080;

	// Usage
	public static final String OPEN_URL_EMPTY_ARTICLE = "UsageOpenUrlEmptyArticlePreference";
	public static final String USE_VOLUME_KEYS = "UsageUseVolumeKeysPreference";
	public static final String LOAD_IMAGES = "DisplayLoadImagesPreference";
	public static final String INVERT_BROWSING = "InvertBrowseArticlesPreference";
	public static final String WORK_OFFLINE = "UsageWorkOfflinePreference";
	public static final String GO_BACK_AFTER_MARK_ALL_READ = "GoBackAfterMarkAllReadPreference";
	public static final String HIDE_ACTIONBAR = "HideActionbarPreference";
	public static final String ALLOW_TABLET_LAYOUT = "AllowTabletLayoutPreference";
	public static final String HIDE_FEED_READ_BUTTONS = "HideFeedReadButtons";
	// Usage Default Values
	public static final boolean OPEN_URL_EMPTY_ARTICLE_DEFAULT = false;
	public static final boolean USE_VOLUME_KEYS_DEFAULT = false;
	public static final boolean LOAD_IMAGES_DEFAULT = true;
	public static final boolean INVERT_BROWSING_DEFAULT = false;
	public static final boolean WORK_OFFLINE_DEFAULT = false;
	public static final boolean GO_BACK_AFTER_MARK_ALL_READ_DEFAULT = false;
	public static final boolean HIDE_ACTIONBAR_DEFAULT = true;
	public static final boolean ALLOW_TABLET_LAYOUT_DEFAULT = true;
	public static final boolean HIDE_FEED_READ_BUTTONS_DEFAULT = false;

	// Display
	public static final String THEME = "DisplayThemePreferenceAuto";
	public static final String ANIMATIONS = "DisplayAnimationsPreference";
	public static final String TEXT_ZOOM = "TextZoomPreference";
	public static final String SUPPORT_ZOOM_CONTROLS = "SupportZoomControlsPreference";
	public static final String ALLOW_HYPHENATION = "AllowHyphenationPreference";
	public static final String HYPHENATION_LANGUAGE = "HyphenationLanguagePreference";
	public static final String SHOW_VIRTUAL = "DisplayShowVirtualPreference";
	public static final String SHOW_BUTTONS_MODE = "ShowButtonsModePreference";
	public static final String ONLY_UNREAD = "DisplayShowUnreadOnlyPreference";
	public static final String ONLY_CACHED_IMAGES = "DisplayShowCachedImagesPreference";
	public static final String INVERT_SORT_ARTICLELIST = "InvertSortArticlelistPreference";
	public static final String INVERT_SORT_FEEDSCATS = "InvertSortFeedscatsPreference";
	public static final String ALIGN_FLUSH_LEFT = "DisplayAlignFlushLeftPreference";
	public static final String PREFER_MARK_READ_OVER_PUBLISH = "PreferMarkReadOverPublish";
	public static final String DATE_TIME_SYSTEM = "DisplayDateTimeFormatSystemPreference";
	public static final String DATE_STRING = "DisplayDateFormatPreference";
	public static final String TIME_STRING = "DisplayTimeFormatPreference";
	public static final String DATE_TIME_STRING = "DisplayDateTimeFormatPreference";
	public static final String DISPLAY_FEED_ICONS = "DisplayFeedIconsPreference";
	// Display Default Values
	public static final int THEME_AUTO = 0;
	public static final int THEME_DARK = 1;
	public static final int THEME_LIGHT = 2;
	public static final int THEME_BLACK = 3;
	public static final int THEME_WHITE = 4;
	public static final String THEME_DEFAULT = "0"; // Needs to be a string
	public static final boolean ANIMATIONS_DEFAULT = true;
	public static final int TEXT_ZOOM_DEFAULT = 100;
	public static final boolean SUPPORT_ZOOM_CONTROLS_DEFAULT = true;
	public static final boolean ALLOW_HYPHENATION_DEFAULT = false;
	public static final String HYPHENATION_LANGUAGE_DEFAULT = "en-gb";
	public static final boolean SHOW_VIRTUAL_DEFAULT = true;
	public static final String SHOW_BUTTONS_MODE_DEFAULT = "0"; // Needs to be a string
	public static final int SHOW_BUTTONS_MODE_ALLWAYS = 1;
	public static final int SHOW_BUTTONS_MODE_HTML = 2;
	public static final boolean ONLY_UNREAD_DEFAULT = false;
	public static final boolean ONLY_CACHED_IMAGES_DEFAULT = false;
	public static final boolean INVERT_SORT_ARTICLELIST_DEFAULT = false;
	public static final boolean INVERT_SORT_FEEDSCATS_DEFAULT = false;
	public static final boolean ALIGN_FLUSH_LEFT_DEFAULT = false;
	public static final boolean DATE_TIME_SYSTEM_DEFAULT = true;
	public static final String DATE_STRING_DEFAULT = "dd.MM.yyyy";
	public static final String TIME_STRING_DEFAULT = "kk:mm";
	public static final String DATE_TIME_STRING_DEFAULT = "dd.MM.yyyy kk:mm";
	public static final boolean DISPLAY_FEED_ICONS_DEFAULT = true;

	// System
	public static final String SAVE_ATTACHMENT = "SaveAttachmentPreference";
	public static final String SAVE_ATTACHMENT_URI = "SaveAttachmentUriPreference";
	public static final String CACHE_FOLDER_MAX_SIZE = "CacheFolderMaxSizePreference";
	public static final String CACHE_IMAGE_MAX_SIZE = "CacheImageMaxSizePreference";
	public static final String CACHE_IMAGE_MIN_SIZE = "CacheImageMinSizePreference";
	public static final String DELETE_DB_SCHEDULED = "DeleteDBScheduledPreference";
	public static final String CACHE_IMAGES_ON_STARTUP = "CacheImagesOnStartupPreference";
	public static final String CACHE_IMAGES_ONLY_WIFI = "CacheImagesOnlyWifiPreference";
	public static final String ONLY_USE_WIFI = "OnlyUseWifiPreference";
	public static final String NO_CRASHREPORTS = "NoCrashreportsPreference";
	public static final String NO_CRASHREPORTS_UNTIL_UPDATE = "NoCrashreportsUntilUpdatePreference";
	public static final String IS_FIRST_RUN = "IsFirstRun";
	// System Default Values
	public static final int CACHE_FOLDER_MAX_SIZE_DEFAULT = 80;
	public static final int CACHE_IMAGE_MAX_SIZE_DEFAULT = 6 * (int) Utils.MB; // 6 MB
	public static final int CACHE_IMAGE_MIN_SIZE_DEFAULT = 32 * (int) Utils.KB; // 64 KB
	public static final boolean DELETE_DB_SCHEDULED_DEFAULT = false;
	public static final boolean CACHE_IMAGES_ON_STARTUP_DEFAULT = false;
	public static final boolean CACHE_IMAGES_ONLY_WIFI_DEFAULT = false;
	public static final boolean ONLY_USE_WIFI_DEFAULT = false;
	public static final boolean NO_CRASHREPORTS_DEFAULT = false;
	public static final boolean NO_CRASHREPORTS_UNTIL_UPDATE_DEFAULT = false;
	public static final boolean IS_FIRST_RUN_DEFAULT = true;

	// Internal
	public static final String APP_VERSION_CHECK_TIME = "appVersionCheckTime";
	public static final String APP_LATEST_VERSION = "appLatestVersion";
	public static final String LAST_VERSION_RUN = "LastVersionRun";
	public static final String FRESH_ARTICLE_MAX_AGE = "freshArticleMaxAge";
	public static final String FRESH_ARTICLE_MAX_AGE_DATE = "freshArticleMaxAgeDate";
	public static final String SINCE_ID = "sinceId";
	public static final String LAST_SYNC = "lastSync";
	public static final String LAST_CLEANUP = "lastCleanup";
	public static final String ENABLE_WIFI_BASED_SUFFIX = "_pref_enable_wifibased";
	// Internal Default Values
	public static final long APP_VERSION_CHECK_TIME_DEFAULT = 0;
	public static final int APP_LATEST_VERSION_DEFAULT = 0;
	public static final String LAST_VERSION_RUN_DEFAULT = "1"; // Needs to be a string
	public static final long FRESH_ARTICLE_MAX_AGE_DEFAULT = Utils.DAY;
	public static final long FRESH_ARTICLE_MAX_AGE_DATE_DEFAULT = 0;
	public static final int SINCE_ID_DEFAULT = 0;
	public static final long LAST_SYNC_DEFAULT = 0;
	public static final long LAST_CLEANUP_DEFAULT = 0;
	public static final int ACTIVITY_SHOW_PREFERENCES = 43;

	/*
	 * Resets all preferences to their default values. Only preferences which are mentioned in this class are reset,
	 * old
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
				// Get the default value, this throws NoSuchFieldException if no _DEFAULT field exists so we quietly ignore this
				Field fieldDefault = Constants.class.getDeclaredField(field.getName() + APPENDED_DEFAULT);
				String value = (String) field.get(new Constants());

				// Get the default type and store value for the specific type
				String type = fieldDefault.getType().getSimpleName();
				switch (type.toLowerCase()) {
					case "string": {
						String defaultValue = (String) fieldDefault.get(null);
						editor.putString(value, defaultValue);
						break;
					}
					case "boolean": {
						boolean defaultValue = fieldDefault.getBoolean(null);
						editor.putBoolean(value, defaultValue);
						break;
					}
					case "int":
					case "integer": {
						int defaultValue = fieldDefault.getInt(null);
						editor.putInt(value, defaultValue);
						break;
					}
					case "long": {
						long defaultValue = fieldDefault.getLong(null);
						editor.putLong(value, defaultValue);
						break;
					}
					default:
						Log.d(TAG, String.format("Field %s of type %s could not be reset.", field.getName(), field.getType()));
						break;
				}

			} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				// Ignore, occurrs if a search for field like EMPTY_DEFAULT is started,
				// this isn't there and shall never be there.
			}
		}

		// Commit when finished
		editor.apply();
	}

	public static int getDefaultValueInt(String key, int emergencyDefault) {
		// Iterate over all fields
		for (Field field : Constants.class.getDeclaredFields()) {
			// Continue on "_DEFAULT"-Fields, we have to find the Constant-Field first
			if (field.getName().endsWith(APPENDED_DEFAULT))
				continue;

			try {
				// Get the default value, this throws NoSuchFieldException if no _DEFAULT field exists so we quietly ignore this
				Field fieldDefault = Constants.class.getDeclaredField(field.getName() + APPENDED_DEFAULT);
				String value = (String) field.get(new Constants());
				if (key.equals(value))
					return fieldDefault.getInt(null);

			} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				// Ignore, occurrs if a search for field like EMPTY_DEFAULT is started,
				// this isn't there and shall never be there.
			}
		}

		return emergencyDefault;
	}

	public static String constant2Var(String s) {
		String[] parts = s.split("_");
		StringBuilder sb = new StringBuilder();
		for (String part : parts) {
			sb.append(toProperCase(part));
		}
		String camelCaseString = sb.toString();
		// We want the String to starrt with a lower-case letter...
		return camelCaseString.substring(0, 1).toLowerCase(Locale.getDefault()) + camelCaseString.substring(1);
	}

	private static String toProperCase(String s) {
		return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1).toLowerCase(Locale.getDefault());
	}

}
