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

package org.ttrssreader.controllers;

import org.stringtemplate.v4.ST;
import org.ttrssreader.R;
import org.ttrssreader.gui.CategoryActivity;
import org.ttrssreader.gui.FeedHeadlineActivity;
import org.ttrssreader.gui.MenuActivity;
import org.ttrssreader.imageCache.ImageCache;
import org.ttrssreader.net.JSONConnector;
import org.ttrssreader.net.JavaJSONConnector;
import org.ttrssreader.net.deprecated.ApacheJSONConnector;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.SSLUtils;
import org.ttrssreader.utils.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.http.HttpResponseCache;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Set;

/**
 * Not entirely sure why this is called the "Controller". Actually, in terms of MVC, it isn't the controller. There
 * isn't one in here but it's called like that and I don't have a better name so we stay with it.
 */
@SuppressWarnings("UnusedDeclaration")
public class Controller extends Constants implements OnSharedPreferenceChangeListener {

	private static final String TAG = Controller.class.getSimpleName();

	public final static String JSON_END_URL = "api/index.php";

	private final static char TEMPLATE_DELIMITER_START = '$';
	private final static char TEMPLATE_DELIMITER_END = '$';

	private static final int THEME_DARK = 1;
	private static final int THEME_LIGHT = 2;
	private static final int THEME_BLACK = 3;
	private static final int THEME_WHITE = 4;

	private WifiManager wifiManager;

	private JSONConnector ttrssConnector;
	private static final Object lockConnector = new Object();

	private ImageCache imageCache = null;

	private boolean isHeadless = false;
	private static final Object lockImageCache = new Object();

	private static Boolean initialized = false;
	private static final Object lockInitialize = new Object();

	private SharedPreferences prefs = null;
	private static boolean preferencesChanged = false;

	private Boolean openUrlEmptyArticle = null;
	private Boolean useVolumeKeys = null;
	private Boolean loadImages = null;
	private Boolean invertBrowsing = null;
	private Boolean goBackAfterMarkAllRead = null;
	private Boolean hideActionbar = null;
	private Boolean workOffline = null;
	private Boolean allowTabletLayout = null;
	private Boolean hideFeedReadButtons = null;

	private Boolean animations = null;
	private Integer textZoom = null;
	private Boolean supportZoomControls = null;
	private Boolean allowHyphenation = null;
	private String hyphenationLanguage = null;
	private Boolean showVirtual = null;
	private Integer showButtonsMode = null;
	private Boolean onlyUnread = null;
	private Boolean onlyDisplayCachedImages = null;
	private Boolean invertSortArticlelist = null;
	private Boolean invertSortFeedscats = null;
	private Boolean alignFlushLeft = null;
	private Boolean dateTimeSystem = null;
	private String dateString = null;
	private String timeString = null;
	private String dateTimeString = null;
	private Integer theme = null;

	private String saveAttachment = null;
	private String cacheFolder = null;
	private Integer cacheFolderMaxSize = null;
	private Integer cacheImageMaxSize = null;
	private Integer cacheImageMinSize = null;
	private Boolean deleteDbScheduled = null;
	private Boolean cacheImagesOnStartup = null;
	private Boolean cacheImagesOnlyWifi = null;
	private Boolean onlyUseWifi = null;
	private Boolean noCrashreports = null;
	private Boolean noCrashreportsUntilUpdate = null;

	private Long appVersionCheckTime = null;
	private Integer appLatestVersion = null;
	private String lastVersionRun = null;
	private Boolean newInstallation = false;
	private Long freshArticleMaxAge = null;
	private Long freshArticleMaxAgeDate = null;
	private Integer sinceId = null;
	private Long lastSync = null;
	private Long lastCleanup = null;
	private Boolean lowMemory = false;

	public volatile Set<Integer> lastOpenedFeeds = new HashSet<>();
	public volatile Set<Integer> lastOpenedArticles = new HashSet<>();

	// Article-View-Stuff
	public static ST htmlTemplate;
	private static final Object lockHtmlTemplate = new Object();

	public static int relSwipeMinDistance;
	public static int relSwipeMaxOffPath;
	public static int relSwipeThresholdVelocity;
	public static int displayHeight;
	public static int displayWidth;

	public static boolean isTablet = false;
	private boolean scheduledRestart = false;
	public static int sFragmentAnimationDirection = 0;

	// Singleton (see http://stackoverflow.com/a/11165926)
	private Controller() {
	}

	private static class InstanceHolder {
		private static final Controller instance = new Controller();
	}

	public static Controller getInstance() {
		return InstanceHolder.instance;
	}

	public void initialize(final Context context) {
		this.prefs = PreferenceManager.getDefaultSharedPreferences(context);

		// Check for new installation
		if (!prefs.contains(URL) && !prefs.contains(LAST_VERSION_RUN)) {
			newInstallation = true;
		}

		// Initially read absolutely necessary preferences:
		sizeVerticalCategory = prefs.getInt(SIZE_VERTICAL_CATEGORY, -1);
		sizeHorizontalCategory = prefs.getInt(SIZE_HORIZONTAL_CATEGORY, -1);

		synchronized (lockInitialize) {

			if (initialized) return;

			/* Attempt to initialize some stuff in a background-thread to reduce loading time. Start a login-request
			separately because this takes some time. Also initialize SSL-Stuff since the login needs this. */
			new AsyncTask<Void, Void, Void>() {
				protected Void doInBackground(Void... params) {
					wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

					if (Controller.getInstance().useKeystore()) {
						try {
							// Trust certificates from keystore:
							SSLUtils.initPrivateKeystore(Controller.getInstance().getKeystorePassword());
						} catch (GeneralSecurityException e) {
							String msg = context.getString(R.string.Error_SSL_Keystore);
							Log.e(TAG, msg, e);
							Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
						}
					} else if (Controller.getInstance().trustAllSsl()) {
						try {
							// Trust all certificates:
							SSLUtils.trustAllCert();
						} catch (GeneralSecurityException e) {
							String msg = context.getString(R.string.Error_SSL_TrustAllHosts);
							Log.e(TAG, msg, e);
							Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
						}
					} else {
						try {
							// Normal certificate-checks:
							SSLUtils.initSslSocketFactory(null, null);
						} catch (GeneralSecurityException e) {
							String msg = context.getString(R.string.Error_SSL_SocketFactory);
							Log.e(TAG, msg, e);
							Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
						}
					}

					if (Controller.getInstance().trustAllHosts()) {
						// Ignore if Certificate matches host:
						SSLUtils.trustAllHost();
					}

					/* This will be accessed when displaying an article or starting the imageCache. When caching it
					 is done anyway so we can just do it in background and the ImageCache starts once it is done. */
					getImageCache();

					// Only need once we are displaying the feed-list or an article...
					Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
							.getDefaultDisplay();
					refreshDisplayMetrics(display);

					enableHttpResponseCache(context.getCacheDir());

					return null;
				}
			}.execute();

			initialized = true;
		}
	}

	/**
	 * Enables HTTP response caching, see http://android-developers.blogspot.de/2011/09/androids-http-clients.html
	 */
	private void enableHttpResponseCache(final File cacheDir) {
		long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
		File httpCacheDir = new File(cacheDir, "http");
		try {
			HttpResponseCache.install(httpCacheDir, httpCacheSize);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void refreshDisplayMetrics(Display display) {
		if (display == null) return;

		DisplayMetrics dm = new DisplayMetrics();
		display.getMetrics(dm);

		int SWIPE_MIN_DISTANCE = 120;
		int SWIPE_MAX_OFF_PATH = 250;
		int SWIPE_THRESHOLD_VELOCITY = 220;

		relSwipeMinDistance = (int) (SWIPE_MIN_DISTANCE * dm.densityDpi / 160.0f);
		relSwipeMaxOffPath = (int) (SWIPE_MAX_OFF_PATH * dm.densityDpi / 160.0f);
		relSwipeThresholdVelocity = (int) (SWIPE_THRESHOLD_VELOCITY * dm.densityDpi / 160.0f);
		displayHeight = dm.heightPixels;
		displayWidth = dm.widthPixels;
	}

	// ******* CONNECTION-Options ****************************

	private boolean wifibasedPrefsEnabled() {
		// Load from Wifi-Preferences:
		String key = getStringWithSSID(ENABLE_WIFI_BASED_SUFFIX, getCurrentSSID(wifiManager), true);
		return prefs.getBoolean(key, false);
	}

	public URI uri() throws URISyntaxException {
		return new URI(hostname());
	}

	public URL url() throws MalformedURLException {
		return new URL(hostname());
	}

	public String hostname() {
		// Load from Wifi-Preferences:
		String key = getStringWithSSID(URL, getCurrentSSID(wifiManager), wifibasedPrefsEnabled());

		String url;
		if (prefs.contains(key)) url = prefs.getString(key, URL_DEFAULT);
		else url = prefs.getString(URL, URL_DEFAULT);

		if (url == null) return null;

		if (!url.endsWith(JSON_END_URL)) {
			if (!url.endsWith("/")) {
				url += "/";
			}
			url += JSON_END_URL;
		}

		return url;
	}

	public String username() {
		// Load from Wifi-Preferences:
		String key = getStringWithSSID(USERNAME, getCurrentSSID(wifiManager), wifibasedPrefsEnabled());

		if (prefs.contains(key)) return prefs.getString(key, EMPTY);
		else return prefs.getString(USERNAME, EMPTY);
	}

	public String password() {
		// Load from Wifi-Preferences:
		String key = getStringWithSSID(PASSWORD, getCurrentSSID(wifiManager), wifibasedPrefsEnabled());

		if (prefs.contains(key)) return prefs.getString(key, EMPTY);
		else return prefs.getString(PASSWORD, EMPTY);
	}

	public boolean lazyServer() {
		// Load from Wifi-Preferences:
		String key = getStringWithSSID(USE_OF_A_LAZY_SERVER, getCurrentSSID(wifiManager), wifibasedPrefsEnabled());

		boolean useOfALazyServer;
		if (prefs.contains(key)) useOfALazyServer = prefs.getBoolean(key, USE_OF_A_LAZY_SERVER_DEFAULT);
		else useOfALazyServer = prefs.getBoolean(USE_OF_A_LAZY_SERVER, Constants.USE_OF_A_LAZY_SERVER_DEFAULT);

		return useOfALazyServer;
	}

	public boolean useHttpAuth() {
		// Load from Wifi-Preferences:
		String key = getStringWithSSID(USE_HTTP_AUTH, getCurrentSSID(wifiManager), wifibasedPrefsEnabled());

		if (prefs.contains(key)) return prefs.getBoolean(key, USE_HTTP_AUTH_DEFAULT);
		else return prefs.getBoolean(USE_HTTP_AUTH, USE_HTTP_AUTH_DEFAULT);
	}

	public String httpUsername() {
		// Load from Wifi-Preferences:
		String key = getStringWithSSID(HTTP_USERNAME, getCurrentSSID(wifiManager), wifibasedPrefsEnabled());

		if (prefs.contains(key)) return prefs.getString(key, EMPTY);
		else return prefs.getString(HTTP_USERNAME, EMPTY);
	}

	public String httpPassword() {
		// Load from Wifi-Preferences:
		String key = getStringWithSSID(HTTP_PASSWORD, getCurrentSSID(wifiManager), wifibasedPrefsEnabled());

		if (prefs.contains(key)) return prefs.getString(key, EMPTY);
		else return prefs.getString(HTTP_PASSWORD, EMPTY);
	}

	public boolean useKeystore() {
		// Load from Wifi-Preferences:
		String key = getStringWithSSID(USE_KEYSTORE, getCurrentSSID(wifiManager), wifibasedPrefsEnabled());

		if (prefs.contains(key)) return prefs.getBoolean(key, USE_KEYSTORE_DEFAULT);
		else return prefs.getBoolean(USE_KEYSTORE, USE_KEYSTORE_DEFAULT);
	}

	public boolean trustAllSsl() {
		// Load from Wifi-Preferences:
		String key = getStringWithSSID(TRUST_ALL_SSL, getCurrentSSID(wifiManager), wifibasedPrefsEnabled());

		if (prefs.contains(key)) return prefs.getBoolean(key, TRUST_ALL_SSL_DEFAULT);
		else return prefs.getBoolean(TRUST_ALL_SSL, TRUST_ALL_SSL_DEFAULT);
	}

	private boolean trustAllHosts() {
		// Load from Wifi-Preferences:
		String key = getStringWithSSID(TRUST_ALL_HOSTS, getCurrentSSID(wifiManager), wifibasedPrefsEnabled());

		if (prefs.contains(key)) return prefs.getBoolean(key, TRUST_ALL_HOSTS_DEFAULT);
		else return prefs.getBoolean(TRUST_ALL_HOSTS, TRUST_ALL_HOSTS_DEFAULT);
	}

	private boolean useOldConnector() {
		// Load from Wifi-Preferences:
		String key = getStringWithSSID(USE_OLD_CONNECTOR, getCurrentSSID(wifiManager), wifibasedPrefsEnabled());

		if (prefs.contains(key)) return prefs.getBoolean(key, USE_OLD_CONNECTOR_DEFAULT);
		else return prefs.getBoolean(USE_OLD_CONNECTOR, USE_OLD_CONNECTOR_DEFAULT);
	}

	public String getKeystorePassword() {
		// Load from Wifi-Preferences:
		String key = getStringWithSSID(KEYSTORE_PASSWORD, getCurrentSSID(wifiManager), wifibasedPrefsEnabled());

		if (prefs.contains(key)) return prefs.getString(key, EMPTY);
		else return prefs.getString(KEYSTORE_PASSWORD, EMPTY);
	}

	@SuppressWarnings("deprecation")
	public JSONConnector getConnector() {
		// Check if connector needs to be reinitialized because of per-wifi-settings:
		boolean useOldConnector = useOldConnector();
		if (useOldConnector && ttrssConnector instanceof JavaJSONConnector) ttrssConnector = null;
		if (!useOldConnector && ttrssConnector instanceof ApacheJSONConnector) ttrssConnector = null;

		// Initialized inside initializeController();
		if (ttrssConnector != null) {
			return ttrssConnector;
		} else {
			synchronized (lockConnector) {
				if (ttrssConnector == null) {
					if (useOldConnector) {
						ttrssConnector = new ApacheJSONConnector();
					} else {
						ttrssConnector = new JavaJSONConnector();
					}
				}
			}
			if (ttrssConnector != null) {
				ttrssConnector.init();
				return ttrssConnector;
			} else throw new RuntimeException("Connector could not be initialized.");
		}
	}

	public ImageCache getImageCache() {
		return getImageCache(true);
	}

	private ImageCache getImageCache(boolean wait) {
		if (imageCache == null && wait) {
			synchronized (lockImageCache) {
				if (imageCache == null) {
					imageCache = new ImageCache(1000, cacheFolder());
					if (!imageCache.enableDiskCache()) {
						imageCache = null;
					}
				}
			}
		}
		return imageCache;
	}

	public boolean isHeadless() {
		return isHeadless;
	}

	public void setHeadless(boolean isHeadless) {
		this.isHeadless = isHeadless;
	}

	// ******* USAGE-Options ****************************

	public boolean openUrlEmptyArticle() {
		if (openUrlEmptyArticle == null)
			openUrlEmptyArticle = prefs.getBoolean(OPEN_URL_EMPTY_ARTICLE, OPEN_URL_EMPTY_ARTICLE_DEFAULT);
		return openUrlEmptyArticle;
	}

	public void setOpenUrlEmptyArticle(boolean openUrlEmptyArticle) {
		put(OPEN_URL_EMPTY_ARTICLE, openUrlEmptyArticle);
		this.openUrlEmptyArticle = openUrlEmptyArticle;
	}

	public boolean useVolumeKeys() {
		if (useVolumeKeys == null) useVolumeKeys = prefs.getBoolean(USE_VOLUME_KEYS, USE_VOLUME_KEYS_DEFAULT);
		return useVolumeKeys;
	}

	public void setUseVolumeKeys(boolean useVolumeKeys) {
		put(USE_VOLUME_KEYS, useVolumeKeys);
		this.useVolumeKeys = useVolumeKeys;
	}

	public boolean loadMedia() {
		if (loadImages == null) loadImages = prefs.getBoolean(LOAD_IMAGES, LOAD_IMAGES_DEFAULT);
		return loadImages;
	}

	public void setLoadImages(boolean loadImages) {
		put(LOAD_IMAGES, loadImages);
		this.loadImages = loadImages;
	}

	public boolean invertBrowsing() {
		if (invertBrowsing == null) invertBrowsing = prefs.getBoolean(INVERT_BROWSING, INVERT_BROWSING_DEFAULT);
		return invertBrowsing;
	}

	public void setInvertBrowsing(boolean invertBrowsing) {
		put(INVERT_BROWSING, invertBrowsing);
		this.invertBrowsing = invertBrowsing;
	}

	public boolean workOffline() {
		if (workOffline == null) workOffline = prefs.getBoolean(WORK_OFFLINE, Constants.WORK_OFFLINE_DEFAULT);
		return workOffline;
	}

	public void setWorkOffline(boolean workOffline) {
		put(WORK_OFFLINE, workOffline);
		this.workOffline = workOffline;
	}

	public boolean goBackAfterMarkAllRead() {
		if (goBackAfterMarkAllRead == null)
			goBackAfterMarkAllRead = prefs.getBoolean(GO_BACK_AFTER_MARK_ALL_READ,
					GO_BACK_AFTER_MARK_ALL_READ_DEFAULT);
		return goBackAfterMarkAllRead;
	}

	public void setGoBackAfterMarkAllRead(boolean goBackAfterMarkAllRead) {
		put(GO_BACK_AFTER_MARK_ALL_READ, goBackAfterMarkAllRead);
		this.goBackAfterMarkAllRead = goBackAfterMarkAllRead;
	}

	public boolean hideActionbar() {
		if (hideActionbar == null) hideActionbar = prefs.getBoolean(HIDE_ACTIONBAR, HIDE_ACTIONBAR_DEFAULT);
		return hideActionbar;
	}

	public void setHideActionbar(boolean hideActionbar) {
		put(HIDE_ACTIONBAR, hideActionbar);
		this.hideActionbar = hideActionbar;
	}

	public boolean allowTabletLayout() {
		if (allowTabletLayout == null)
			allowTabletLayout = prefs.getBoolean(ALLOW_TABLET_LAYOUT, ALLOW_TABLET_LAYOUT_DEFAULT);
		return allowTabletLayout;
	}

	public void setAllowTabletLayout(boolean allowTabletLayout) {
		put(ALLOW_TABLET_LAYOUT, allowTabletLayout);
		this.allowTabletLayout = allowTabletLayout;
	}

	public void setHideFeedReadButtons(boolean hideFeedReadButtons) {
		put(HIDE_FEED_READ_BUTTONS, hideFeedReadButtons);
		this.hideFeedReadButtons = hideFeedReadButtons;
	}

	public boolean hideFeedReadButtons() {
		if (hideFeedReadButtons == null)
			hideFeedReadButtons = prefs.getBoolean(HIDE_FEED_READ_BUTTONS, HIDE_FEED_READ_BUTTONS_DEFAULT);

		return hideFeedReadButtons;
	}

	// ******* DISPLAY-Options ****************************

	public boolean animations() {
		if (animations == null) animations = prefs.getBoolean(ANIMATIONS, ANIMATIONS_DEFAULT);
		return animations;
	}

	public void setAnimations(boolean animations) {
		put(ANIMATIONS, animations);
		this.animations = animations;
	}

	public int textZoom() {
		if (textZoom == null) textZoom = prefs.getInt(TEXT_ZOOM, TEXT_ZOOM_DEFAULT);
		return textZoom;
	}

	public void setTextZoom(int textZoom) {
		put(TEXT_ZOOM, textZoom);
		this.textZoom = textZoom;
	}

	public boolean supportZoomControls() {
		if (supportZoomControls == null)
			supportZoomControls = prefs.getBoolean(SUPPORT_ZOOM_CONTROLS, SUPPORT_ZOOM_CONTROLS_DEFAULT);
		return supportZoomControls;
	}

	public void setSupportZoomControls(boolean supportZoomControls) {
		put(SUPPORT_ZOOM_CONTROLS, supportZoomControls);
		this.supportZoomControls = supportZoomControls;
	}

	public boolean allowHyphenation() {
		if (allowHyphenation == null) allowHyphenation = prefs.getBoolean(ALLOW_HYPHENATION,
				ALLOW_HYPHENATION_DEFAULT);
		return allowHyphenation;
	}

	public void setAllowHyphenation(boolean allowHyphenation) {
		put(ALLOW_HYPHENATION, allowHyphenation);
		this.allowHyphenation = allowHyphenation;
	}

	public String hyphenationLanguage() {
		if (hyphenationLanguage == null)
			hyphenationLanguage = prefs.getString(HYPHENATION_LANGUAGE, HYPHENATION_LANGUAGE_DEFAULT);
		return hyphenationLanguage;
	}

	public void setHyphenationLanguage(String hyphenationLanguage) {
		put(HYPHENATION_LANGUAGE, hyphenationLanguage);
		this.hyphenationLanguage = hyphenationLanguage;
	}

	public boolean showVirtual() {
		if (showVirtual == null) showVirtual = prefs.getBoolean(SHOW_VIRTUAL, Constants.SHOW_VIRTUAL_DEFAULT);
		return showVirtual;
	}

	public void setDisplayVirtuals(boolean displayVirtuals) {
		put(SHOW_VIRTUAL, displayVirtuals);
		this.showVirtual = displayVirtuals;
	}

	public Integer showButtonsMode() {
		if (showButtonsMode == null)
			showButtonsMode = Integer.parseInt(prefs.getString(SHOW_BUTTONS_MODE, SHOW_BUTTONS_MODE_DEFAULT));
		return showButtonsMode;
	}

	public void setShowButtonsMode(Integer showButtonsMode) {
		put(SHOW_BUTTONS_MODE, showButtonsMode);
		this.showButtonsMode = showButtonsMode;
	}

	public boolean onlyUnread() {
		if (onlyUnread == null) onlyUnread = prefs.getBoolean(ONLY_UNREAD, ONLY_UNREAD_DEFAULT);
		return onlyUnread;
	}

	public void setDisplayOnlyUnread(boolean displayOnlyUnread) {
		put(ONLY_UNREAD, displayOnlyUnread);
		this.onlyUnread = displayOnlyUnread;
	}

	public boolean onlyDisplayCachedImages() {
		if (onlyDisplayCachedImages == null)
			onlyDisplayCachedImages = prefs.getBoolean(ONLY_CACHED_IMAGES, ONLY_CACHED_IMAGES_DEFAULT);
		return onlyDisplayCachedImages;
	}

	public void setDisplayCachedImages(boolean onlyDisplayCachedImages) {
		put(ONLY_CACHED_IMAGES, onlyDisplayCachedImages);
		this.onlyDisplayCachedImages = onlyDisplayCachedImages;
	}

	public boolean invertSortArticlelist() {
		if (invertSortArticlelist == null)
			invertSortArticlelist = prefs.getBoolean(INVERT_SORT_ARTICLELIST, INVERT_SORT_ARTICLELIST_DEFAULT);
		return invertSortArticlelist;
	}

	public void setInvertSortArticleList(boolean invertSortArticleList) {
		put(INVERT_SORT_ARTICLELIST, invertSortArticleList);
		this.invertSortArticlelist = invertSortArticleList;
	}

	public boolean invertSortFeedscats() {
		if (invertSortFeedscats == null)
			invertSortFeedscats = prefs.getBoolean(INVERT_SORT_FEEDSCATS, INVERT_SORT_FEEDSCATS_DEFAULT);
		return invertSortFeedscats;
	}

	public void setInvertSortFeedsCats(boolean invertSortFeedsCats) {
		put(INVERT_SORT_FEEDSCATS, invertSortFeedsCats);
		this.invertSortFeedscats = invertSortFeedsCats;
	}

	public boolean alignFlushLeft() {
		if (alignFlushLeft == null) alignFlushLeft = prefs.getBoolean(ALIGN_FLUSH_LEFT, ALIGN_FLUSH_LEFT_DEFAULT);
		return alignFlushLeft;
	}

	public void setAlignFlushLeft(boolean alignFlushLeft) {
		put(ALIGN_FLUSH_LEFT, alignFlushLeft);
		this.alignFlushLeft = alignFlushLeft;
	}

	public boolean dateTimeSystem() {
		if (dateTimeSystem == null) dateTimeSystem = prefs.getBoolean(DATE_TIME_SYSTEM, DATE_TIME_SYSTEM_DEFAULT);
		return dateTimeSystem;
	}

	public void setDateTimeSystem(boolean dateTimeSystem) {
		put(DATE_TIME_SYSTEM, dateTimeSystem);
		this.dateTimeSystem = dateTimeSystem;
	}

	public String dateString() {
		if (dateString == null) dateString = prefs.getString(DATE_STRING, DATE_STRING_DEFAULT);
		return dateString;
	}

	public void setDateString(String dateString) {
		put(DATE_STRING, dateString);
		this.dateString = dateString;
	}

	public String timeString() {
		if (timeString == null) timeString = prefs.getString(TIME_STRING, TIME_STRING_DEFAULT);
		return timeString;
	}

	public void setTimeString(String timeString) {
		put(TIME_STRING, timeString);
		this.timeString = timeString;
	}

	public String dateTimeString() {
		if (dateTimeString == null) dateTimeString = prefs.getString(DATE_TIME_STRING, DATE_TIME_STRING_DEFAULT);
		return dateTimeString;
	}

	public void setDateTimeString(String dateTimeString) {
		put(DATE_TIME_STRING, dateTimeString);
		this.dateTimeString = dateTimeString;
	}

	public int getTheme() {
		switch (getThemeInternal()) {
			case THEME_LIGHT:
				return R.style.TTRSS_Light;
			case THEME_BLACK:
				return R.style.TTRSS_Black;
			case THEME_WHITE:
				return R.style.TTRSS_White;
			case THEME_DARK:
			default:
				return R.style.TTRSS_Dark;
		}
	}

	private int getThemeInternal() {
		if (theme == null) theme = Integer.parseInt(prefs.getString(THEME, THEME_DEFAULT));
		return theme;
	}

	public void setTheme(int theme) {
		this.theme = theme;
		put(THEME, theme + "");
	}

	public int getThemeHTML() {
		switch (getThemeInternal()) {
			case THEME_LIGHT:
				return R.string.HTML_THEME_LIGHT;
			case THEME_BLACK:
				return R.string.HTML_THEME_BLACK;
			case THEME_WHITE:
				return R.string.HTML_THEME_WHITE;
			case THEME_DARK:
			default:
				return R.string.HTML_THEME_DARK;
		}
	}

	public boolean isScheduledRestart() {
		return scheduledRestart;
	}

	public void setScheduledRestart(boolean scheduledRestart) {
		this.scheduledRestart = scheduledRestart;
	}

	// SYSTEM

	public String saveAttachmentPath() {
		if (saveAttachment == null) saveAttachment = prefs.getString(SAVE_ATTACHMENT, SAVE_ATTACHMENT_DEFAULT);
		return saveAttachment;
	}

	public void setSaveAttachmentPath(String saveAttachment) {
		put(SAVE_ATTACHMENT, saveAttachment);
		this.saveAttachment = saveAttachment;
	}

	public String cacheFolder() {
		if (cacheFolder == null) cacheFolder = prefs.getString(CACHE_FOLDER, CACHE_FOLDER_DEFAULT);
		if (cacheFolder != null && !cacheFolder.endsWith("/")) setCacheFolder(cacheFolder + "/");
		return cacheFolder;
	}

	public void setCacheFolder(String cacheFolder) {
		put(CACHE_FOLDER, cacheFolder);
		this.cacheFolder = cacheFolder;
	}

	public Integer cacheFolderMaxSize() {
		if (cacheFolderMaxSize == null)
			cacheFolderMaxSize = prefs.getInt(CACHE_FOLDER_MAX_SIZE, Constants.CACHE_FOLDER_MAX_SIZE_DEFAULT);
		return cacheFolderMaxSize;
	}

	public void setCacheFolderMaxSize(Integer cacheFolderMaxSize) {
		put(CACHE_FOLDER_MAX_SIZE, cacheFolderMaxSize);
		this.cacheFolderMaxSize = cacheFolderMaxSize;
	}

	public Integer cacheImageMaxSize() {
		if (cacheImageMaxSize == null)
			cacheImageMaxSize = prefs.getInt(CACHE_IMAGE_MAX_SIZE, CACHE_IMAGE_MAX_SIZE_DEFAULT);
		return cacheImageMaxSize;
	}

	public void setCacheImageMaxSize(Integer cacheImageMaxSize) {
		put(CACHE_IMAGE_MAX_SIZE, cacheImageMaxSize);
		this.cacheImageMaxSize = cacheImageMaxSize;
	}

	public Integer cacheImageMinSize() {
		if (cacheImageMinSize == null)
			cacheImageMinSize = prefs.getInt(CACHE_IMAGE_MIN_SIZE, CACHE_IMAGE_MIN_SIZE_DEFAULT);
		return cacheImageMinSize;
	}

	public void setCacheImageMinSize(Integer cacheImageMinSize) {
		put(CACHE_IMAGE_MIN_SIZE, cacheImageMinSize);
		this.cacheImageMinSize = cacheImageMinSize;
	}

	public boolean isDeleteDBScheduled() {
		if (deleteDbScheduled == null)
			deleteDbScheduled = prefs.getBoolean(DELETE_DB_SCHEDULED, DELETE_DB_SCHEDULED_DEFAULT);
		return deleteDbScheduled;
	}

	public void setDeleteDBScheduled(boolean isDeleteDBScheduled) {
		put(DELETE_DB_SCHEDULED, isDeleteDBScheduled);
		this.deleteDbScheduled = isDeleteDBScheduled;
	}

	public boolean cacheImagesOnStartup() {
		if (cacheImagesOnStartup == null)
			cacheImagesOnStartup = prefs.getBoolean(CACHE_IMAGES_ON_STARTUP, CACHE_IMAGES_ON_STARTUP_DEFAULT);
		return cacheImagesOnStartup;
	}

	public void setCacheImagesOnStartup(boolean cacheImagesOnStartup) {
		put(CACHE_IMAGES_ON_STARTUP, cacheImagesOnStartup);
		this.cacheImagesOnStartup = cacheImagesOnStartup;
	}

	public boolean cacheImagesOnlyWifi() {
		if (cacheImagesOnlyWifi == null)
			cacheImagesOnlyWifi = prefs.getBoolean(CACHE_IMAGES_ONLY_WIFI, CACHE_IMAGES_ONLY_WIFI_DEFAULT);
		return cacheImagesOnlyWifi;
	}

	public void setCacheImagesOnlyWifi(boolean cacheImagesOnlyWifi) {
		put(CACHE_IMAGES_ONLY_WIFI, cacheImagesOnlyWifi);
		this.cacheImagesOnlyWifi = cacheImagesOnlyWifi;
	}

	public boolean onlyUseWifi() {
		if (onlyUseWifi == null) onlyUseWifi = prefs.getBoolean(ONLY_USE_WIFI, ONLY_USE_WIFI_DEFAULT);
		return onlyUseWifi;
	}

	public void setOnlyUseWifi(boolean onlyUseWifi) {
		put(ONLY_USE_WIFI, onlyUseWifi);
		this.onlyUseWifi = onlyUseWifi;
	}

	// Returns true if noCrashreports OR noCrashreportsUntilUpdate is true.
	public boolean isNoCrashreports() {
		if (noCrashreports == null) noCrashreports = prefs.getBoolean(NO_CRASHREPORTS, NO_CRASHREPORTS_DEFAULT);
		if (noCrashreportsUntilUpdate == null) noCrashreportsUntilUpdate = prefs
				.getBoolean(NO_CRASHREPORTS_UNTIL_UPDATE, NO_CRASHREPORTS_UNTIL_UPDATE_DEFAULT);
		return noCrashreports || noCrashreportsUntilUpdate;
	}

	public void setNoCrashreports(boolean noCrashreports) {
		put(NO_CRASHREPORTS, noCrashreports);
		this.noCrashreports = noCrashreports;
	}

	public void setNoCrashreportsUntilUpdate(boolean noCrashreportsUntilUpdate) {
		put(NO_CRASHREPORTS_UNTIL_UPDATE, noCrashreportsUntilUpdate);
		this.noCrashreportsUntilUpdate = noCrashreportsUntilUpdate;
	}

	// ******* INTERNAL Data ****************************

	public long appVersionCheckTime() {
		if (appVersionCheckTime == null)
			appVersionCheckTime = prefs.getLong(APP_VERSION_CHECK_TIME, APP_VERSION_CHECK_TIME_DEFAULT);
		return appVersionCheckTime;
	}

	private void setAppVersionCheckTime(long appVersionCheckTime) {
		put(APP_VERSION_CHECK_TIME, appVersionCheckTime);
		this.appVersionCheckTime = appVersionCheckTime;
	}

	public int appLatestVersion() {
		if (appLatestVersion == null) try {
			appLatestVersion = prefs.getInt(APP_LATEST_VERSION, APP_LATEST_VERSION_DEFAULT);
		} catch (ClassCastException e) {
			/* For some users this returns a Long, don't know why and can't really fix it, so just reset the value. */
			setAppLatestVersion(APP_LATEST_VERSION_DEFAULT);
		}
		return appLatestVersion;
	}

	public void setAppLatestVersion(int appLatestVersion) {
		put(APP_LATEST_VERSION, appLatestVersion);
		this.appLatestVersion = appLatestVersion;
		setAppVersionCheckTime(System.currentTimeMillis());
		// Set current time, this only changes when it has been fetched from the server
	}

	public String getLastVersionRun() {
		if (lastVersionRun == null) lastVersionRun = prefs.getString(LAST_VERSION_RUN, LAST_VERSION_RUN_DEFAULT);
		return lastVersionRun;
	}

	public void setLastVersionRun(String lastVersionRun) {
		put(LAST_VERSION_RUN, lastVersionRun);
		this.lastVersionRun = lastVersionRun;
	}

	public boolean newInstallation() {
		// Initialized inside initializeController();
		return newInstallation;
	}

	public void setSinceId(int sinceId) {
		put(SINCE_ID, sinceId);
		this.sinceId = sinceId;
	}

	public int getSinceId() {
		if (sinceId == null) sinceId = prefs.getInt(SINCE_ID, SINCE_ID_DEFAULT);
		return sinceId;
	}

	public void setLastSync(long lastSync) {
		put(LAST_SYNC, lastSync);
		this.lastSync = lastSync;
	}

	public long getLastSync() {
		if (lastSync == null) lastSync = prefs.getLong(LAST_SYNC, LAST_SYNC_DEFAULT);
		return lastSync;
	}

	public void lowMemory(boolean lowMemory) {
		if (lowMemory && !this.lowMemory)
			Log.w(TAG, "lowMemory-Situation detected, trying to reduce memory footprint...");
		this.lowMemory = lowMemory;
	}

	public boolean isLowMemory() {
		return lowMemory;
	}

	public void setLastCleanup(long lastCleanup) {
		put(LAST_CLEANUP, lastCleanup);
		this.lastCleanup = lastSync;
	}

	public long getLastCleanup() {
		if (lastCleanup == null) lastCleanup = prefs.getLong(LAST_CLEANUP, LAST_CLEANUP_DEFAULT);
		return lastCleanup;
	}

	private AsyncTask<Void, Void, Void> refreshPrefTask;

	public long getFreshArticleMaxAge() {
		if (freshArticleMaxAge == null)
			freshArticleMaxAge = prefs.getLong(FRESH_ARTICLE_MAX_AGE, FRESH_ARTICLE_MAX_AGE_DEFAULT);

		if (freshArticleMaxAgeDate == null)
			freshArticleMaxAgeDate = prefs.getLong(FRESH_ARTICLE_MAX_AGE_DATE, FRESH_ARTICLE_MAX_AGE_DATE_DEFAULT);

		if (freshArticleMaxAgeDate < System.currentTimeMillis() - (Utils.DAY * 2)) {

			// Only start task if none existing yet
			if (refreshPrefTask == null) {
				refreshPrefTask = new AsyncTask<Void, Void, Void>() {
					@Override
					protected Void doInBackground(Void... params) {
						String s = "";
						try {
							s = Data.getInstance().getPref("FRESH_ARTICLE_MAX_AGE");

							freshArticleMaxAge = Long.parseLong(s) * Utils.HOUR;
							put(FRESH_ARTICLE_MAX_AGE, freshArticleMaxAge);

							freshArticleMaxAgeDate = System.currentTimeMillis();
							put(FRESH_ARTICLE_MAX_AGE_DATE, freshArticleMaxAgeDate);
						} catch (Exception e) {
							Log.d(TAG, "Pref \"FRESH_ARTICLE_MAX_AGE\" could not be fetched from server: " + s);
						}
						return null;
					}
				};

				refreshPrefTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		}

		return freshArticleMaxAge;
	}

	/*
	 * Generic method to insert values into the preferences store
	 */
	private void put(String constant, Object o) {
		SharedPreferences.Editor editor = prefs.edit();
		if (o instanceof String) {
			editor.putString(constant, (String) o);
		} else if (o instanceof Integer) {
			editor.putInt(constant, (Integer) o);
		} else if (o instanceof Long) {
			editor.putLong(constant, (Long) o);
		} else if (o instanceof Boolean) {
			editor.putBoolean(constant, (Boolean) o);
		}
		editor.apply();
	}

	/** If provided "key" resembles a setting as declared in java the corresponding variable in this class
	 * will
	 * be reset to null. Variable-Name ist built from the name of the field in Contants.java which holds the value from
	 * "key" which was changed lately.
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

		// Indicate Restart of App is necessary if Theme-Pref is changed and value differs from old value:
		if (key.equals(THEME)) {
			int newTheme = Integer.parseInt(prefs.getString(key, THEME_DEFAULT));
			if (newTheme != getThemeInternal()) {
				setTheme(newTheme);
				scheduledRestart = true;
			}
		}

		for (Field field : Constants.class.getDeclaredFields()) {

			// No default-values
			if (field.getName().endsWith(APPENDED_DEFAULT)) continue;

			// Only use public static fields
			if (!Modifier.isStatic(field.getModifiers()) || !Modifier.isPublic(field.getModifiers())) continue;

			String fieldName = "";
			try {
				Object f = field.get(this);
				if (!(f instanceof String)) continue;

				if (!key.equals(f)) continue;

				// reset variable, it will be re-read on next access
				fieldName = constant2Var(field.getName());
				Controller.class.getDeclaredField(fieldName).set(this, null); // "Declared" so also private
				setPreferencesChanged(true);

			} catch (Exception e) {
				Log.e(TAG, "Field not found: " + fieldName);
			}

		}
	}

	public boolean isPreferencesChanged() {
		return preferencesChanged;
	}

	public void setPreferencesChanged(boolean preferencesChanged) {
		Controller.preferencesChanged = preferencesChanged;
	}

	private static String getCurrentSSID(WifiManager wifiManager) {
		if (wifiManager != null && wifiManager.isWifiEnabled()) {
			WifiInfo info = wifiManager.getConnectionInfo();
			final String ssid = info.getSSID();
			return ssid == null ? "" : ssid.replace("\"", "");
		} else {
			return null;
		}
	}

	private static String getStringWithSSID(String param, String wifiSSID, boolean wifibasedPrefsEnabled) {
		if (wifiSSID == null || !wifibasedPrefsEnabled) return param;
		else return wifiSSID + param;
	}

	private static final String SIZE_VERTICAL_CATEGORY = "sizeVerticalCategory";
	private static final String SIZE_HORIZONTAL_CATEGORY = "sizeHorizontalCategory";
	private static final String SIZE_VERTICAL_HEADLINE = "sizeVerticalHeadline";
	private static final String SIZE_HORIZONTAL_HEADLINE = "sizeHorizontalHeadline";
	private Integer sizeVerticalCategory;
	private Integer sizeHorizontalCategory;

	public int getMainFrameSize(MenuActivity activity, boolean isVertical, int min, int max) {
		int ret = -1;
		if (activity instanceof CategoryActivity) {
			if (isVertical) {
				ret = sizeVerticalCategory;
			} else {
				ret = sizeHorizontalCategory;
			}
		} else if (activity instanceof FeedHeadlineActivity) {
			String key = getKeyCategoryFeedId((FeedHeadlineActivity) activity);
			if (isVertical) {
				ret = prefs.getInt(SIZE_VERTICAL_HEADLINE + key, -1);
			} else {
				ret = prefs.getInt(SIZE_HORIZONTAL_HEADLINE + key, -1);
			}
		}

		if (ret < min || ret > max) ret = (min + max) / 2;

		return ret;
	}

	public void setViewSize(MenuActivity activity, boolean isVertical, int size) {
		if (size <= 0) return;
		if (activity instanceof CategoryActivity) {
			if (isVertical) {
				sizeVerticalCategory = size;
				put(SIZE_VERTICAL_CATEGORY, size);
			} else {
				sizeHorizontalCategory = size;
				put(SIZE_HORIZONTAL_CATEGORY, size);
			}
		} else if (activity instanceof FeedHeadlineActivity) {
			String key = getKeyCategoryFeedId((FeedHeadlineActivity) activity);
			if (isVertical) {
				put(SIZE_VERTICAL_HEADLINE + key, size);
			} else {
				put(SIZE_HORIZONTAL_HEADLINE + key, size);
			}
		}
	}

	private static String getKeyCategoryFeedId(FeedHeadlineActivity activity) {
		return "_" + activity.getFeedId() + " " + activity.getCategoryId();
	}

}
