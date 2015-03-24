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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
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
public class Controller implements OnSharedPreferenceChangeListener {

    private static final String TAG = Controller.class.getSimpleName();

    public final static String JSON_END_URL = "api/index.php";

    private final static char TEMPLATE_DELIMITER_START = '$';
    private final static char TEMPLATE_DELIMITER_END = '$';

    private static final String MARKER_ALIGN = "TEXT_ALIGN_MARKER";
    private static final String MARKER_CACHE_DIR = "CACHE_DIR_MARKER";
    private static final String MARKER_CACHED_IMAGES = "CACHED_IMAGES_MARKER";
    private static final String MARKER_JS = "JS_MARKER";
    private static final String MARKER_THEME = "THEME_MARKER";
    private static final String MARKER_LANG = "LANG_MARKER";
    private static final String MARKER_TOP_NAV = "TOP_NAVIGATION_MARKER";
    private static final String MARKER_CONTENT = "CONTENT_MARKER";
    private static final String MARKER_BOTTOM_NAV = "BOTTOM_NAVIGATION_MARKER";

    private static final int THEME_DARK = 1;
    private static final int THEME_LIGHT = 2;
    private static final int THEME_BLACK = 3;
    private static final int THEME_WHITE = 4;

    private WeakReference<Context> contextRef;
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
    public static String htmlTemplate = "";
    private static final Object lockHtmlTemplate = new Object();

    public static int relSwipeMinDistance;
    public static int relSwipeMaxOffPath;
    public static int relSwipteThresholdVelocity;
    public static int displayHeight;
    public static int displayWidth;

    public static boolean isTablet = false;
    private boolean scheduledRestart = false;

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
        this.contextRef = new WeakReference<>(context);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);

        synchronized (lockInitialize) {

            if (initialized)
                return;

            // Attempt to initialize some stuff in a background-thread to reduce loading time. Start a login-request
            // separately because this takes some time. Also initialize SSL-Stuff since the login needs this.
            new AsyncTask<Void, Void, Void>() {
                protected Void doInBackground(Void... params) {
                    wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

                    // Initially read absolutely necessary preferences:
                    sizeVerticalCategory = prefs.getInt(SIZE_VERTICAL_CATEGORY, -1);
                    sizeHorizontalCategory = prefs.getInt(SIZE_HORIZONTAL_CATEGORY, -1);

                    // Check for new installation
                    if (!prefs.contains(Constants.URL) && !prefs.contains(Constants.LAST_VERSION_RUN)) {
                        newInstallation = true;
                    }

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

                    // This will be accessed when displaying an article or starting the imageCache. When caching it
                    // is done
                    // anyway so we can just do it in background and the ImageCache starts once it is done.
                    getImageCache();

                    // Only need once we are displaying the feed-list or an article...
                    Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                            .getDefaultDisplay();
                    refreshDisplayMetrics(display);

                    // Loads all article and webview related resources
                    reloadTheme();

                    enableHttpResponseCache(context);

                    return null;
                }
            }.execute();

            initialized = true;
        }
    }

    private void reloadTheme() {
        Context context = contextRef.get();
        if (context == null)
            return;

        // Article-Prefetch-Stuff from Raw-Ressources and System
        ST htmlTmpl = new ST(context.getResources().getString(R.string.HTML_TEMPLATE), TEMPLATE_DELIMITER_START,
                TEMPLATE_DELIMITER_END);

        // Replace alignment-marker with the requested layout, align:left or justified
        String replaceAlign;
        if (alignFlushLeft()) {
            replaceAlign = context.getResources().getString(R.string.ALIGN_LEFT);
        } else {
            replaceAlign = context.getResources().getString(R.string.ALIGN_JUSTIFY);
        }

        String javascript = "";
        String lang = "";
        if (allowHyphenation()) {
            ST javascriptST = new ST(context.getResources().getString(R.string.JAVASCRIPT_HYPHENATION_TEMPLATE),
                    TEMPLATE_DELIMITER_START, TEMPLATE_DELIMITER_END);
            lang = hyphenationLanguage();
            javascriptST.add(MARKER_LANG, lang);
            javascript = javascriptST.render();
        }

        String buttons = "";
        if (showButtonsMode() == Constants.SHOW_BUTTONS_MODE_HTML)
            buttons = context.getResources().getString(R.string.BOTTOM_NAVIGATION_TEMPLATE);

        htmlTmpl.add(MARKER_ALIGN, replaceAlign);
        htmlTmpl.add(MARKER_THEME, context.getResources().getString(getThemeHTML()));
        htmlTmpl.add(MARKER_CACHE_DIR, cacheFolder());
        htmlTmpl.add(MARKER_CACHED_IMAGES, context.getResources().getString(R.string.CACHED_IMAGES_TEMPLATE));
        htmlTmpl.add(MARKER_JS, javascript);
        htmlTmpl.add(MARKER_LANG, lang);
        htmlTmpl.add(MARKER_TOP_NAV, context.getResources().getString(R.string.TOP_NAVIGATION_TEMPLATE));
        htmlTmpl.add(MARKER_CONTENT, context.getResources().getString(R.string.CONTENT_TEMPLATE));
        htmlTmpl.add(MARKER_BOTTOM_NAV, buttons);

        // This is only needed once an article is displayed
        synchronized (lockHtmlTemplate) {
            htmlTemplate = htmlTmpl.render();
        }
    }

    /**
     * Enables HTTP response caching on devices that support it, see
     * http://android-developers.blogspot.de/2011/09/androids-http-clients.html
     */
    private void enableHttpResponseCache(Context context) {
        try {
            long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
            File httpCacheDir = new File(context.getCacheDir(), "http");
            Class.forName("android.net.http.HttpResponseCache").getMethod("install", File.class, long.class)
                    .invoke(null, httpCacheDir, httpCacheSize);
        } catch (Exception httpResponseCacheNotAvailable) {
            // Empty!
        }
    }

    public static void refreshDisplayMetrics(Display display) {
        if (display == null)
            return;

        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);

        int SWIPE_MIN_DISTANCE = 120;
        int SWIPE_MAX_OFF_PATH = 250;
        int SWIPE_THRESHOLD_VELOCITY = 200;

        relSwipeMinDistance = (int) (SWIPE_MIN_DISTANCE * dm.densityDpi / 160.0f);
        relSwipeMaxOffPath = (int) (SWIPE_MAX_OFF_PATH * dm.densityDpi / 160.0f);
        relSwipteThresholdVelocity = (int) (SWIPE_THRESHOLD_VELOCITY * dm.densityDpi / 160.0f);
        displayHeight = dm.heightPixels;
        displayWidth = dm.widthPixels;
    }

    // ******* CONNECTION-Options ****************************

    private boolean wifibasedPrefsEnabled() {
        // Load from Wifi-Preferences:
        String key = getStringWithSSID(Constants.ENABLE_WIFI_BASED_SUFFIX, getCurrentSSID(wifiManager), true);
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
        String key = getStringWithSSID(Constants.URL, getCurrentSSID(wifiManager), wifibasedPrefsEnabled());

        String url;
        if (prefs.contains(key))
            url = prefs.getString(key, Constants.URL_DEFAULT);
        else
            url = prefs.getString(Constants.URL, Constants.URL_DEFAULT);

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
        String key = getStringWithSSID(Constants.USERNAME, getCurrentSSID(wifiManager), wifibasedPrefsEnabled());

        if (prefs.contains(key))
            return prefs.getString(key, Constants.EMPTY);
        else
            return prefs.getString(Constants.USERNAME, Constants.EMPTY);
    }

    public String password() {
        // Load from Wifi-Preferences:
        String key = getStringWithSSID(Constants.PASSWORD, getCurrentSSID(wifiManager), wifibasedPrefsEnabled());

        if (prefs.contains(key))
            return prefs.getString(key, Constants.EMPTY);
        else
            return prefs.getString(Constants.PASSWORD, Constants.EMPTY);
    }

    public boolean lazyServer() {
        // Load from Wifi-Preferences:
        String key = getStringWithSSID(Constants.USE_OF_A_LAZY_SERVER, getCurrentSSID(wifiManager),
                wifibasedPrefsEnabled());

        boolean useOfALazyServer;
        if (prefs.contains(key))
            useOfALazyServer = prefs.getBoolean(key, Constants.USE_OF_A_LAZY_SERVER_DEFAULT);
        else
            useOfALazyServer = prefs.getBoolean(Constants.USE_OF_A_LAZY_SERVER, Constants.USE_OF_A_LAZY_SERVER_DEFAULT);

        return useOfALazyServer;
    }

    public boolean useHttpAuth() {
        // Load from Wifi-Preferences:
        String key = getStringWithSSID(Constants.USE_HTTP_AUTH, getCurrentSSID(wifiManager), wifibasedPrefsEnabled());

        if (prefs.contains(key))
            return prefs.getBoolean(key, Constants.USE_HTTP_AUTH_DEFAULT);
        else
            return prefs.getBoolean(Constants.USE_HTTP_AUTH, Constants.USE_HTTP_AUTH_DEFAULT);
    }

    public String httpUsername() {
        // Load from Wifi-Preferences:
        String key = getStringWithSSID(Constants.HTTP_USERNAME, getCurrentSSID(wifiManager), wifibasedPrefsEnabled());

        if (prefs.contains(key))
            return prefs.getString(key, Constants.EMPTY);
        else
            return prefs.getString(Constants.HTTP_USERNAME, Constants.EMPTY);
    }

    public String httpPassword() {
        // Load from Wifi-Preferences:
        String key = getStringWithSSID(Constants.HTTP_PASSWORD, getCurrentSSID(wifiManager), wifibasedPrefsEnabled());

        if (prefs.contains(key))
            return prefs.getString(key, Constants.EMPTY);
        else
            return prefs.getString(Constants.HTTP_PASSWORD, Constants.EMPTY);
    }

    public boolean useKeystore() {
        // Load from Wifi-Preferences:
        String key = getStringWithSSID(Constants.USE_KEYSTORE, getCurrentSSID(wifiManager), wifibasedPrefsEnabled());

        if (prefs.contains(key))
            return prefs.getBoolean(key, Constants.USE_KEYSTORE_DEFAULT);
        else
            return prefs.getBoolean(Constants.USE_KEYSTORE, Constants.USE_KEYSTORE_DEFAULT);
    }

    public boolean trustAllSsl() {
        // Load from Wifi-Preferences:
        String key = getStringWithSSID(Constants.TRUST_ALL_SSL, getCurrentSSID(wifiManager), wifibasedPrefsEnabled());

        if (prefs.contains(key))
            return prefs.getBoolean(key, Constants.TRUST_ALL_SSL_DEFAULT);
        else
            return prefs.getBoolean(Constants.TRUST_ALL_SSL, Constants.TRUST_ALL_SSL_DEFAULT);
    }

    private boolean trustAllHosts() {
        // Load from Wifi-Preferences:
        String key = getStringWithSSID(Constants.TRUST_ALL_HOSTS, getCurrentSSID(wifiManager), wifibasedPrefsEnabled());

        if (prefs.contains(key))
            return prefs.getBoolean(key, Constants.TRUST_ALL_HOSTS_DEFAULT);
        else
            return prefs.getBoolean(Constants.TRUST_ALL_HOSTS, Constants.TRUST_ALL_HOSTS_DEFAULT);
    }

    private boolean useOldConnector() {
        // Load from Wifi-Preferences:
        String key = getStringWithSSID(Constants.USE_OLD_CONNECTOR, getCurrentSSID(wifiManager),
                wifibasedPrefsEnabled());

        if (prefs.contains(key))
            return prefs.getBoolean(key, Constants.USE_OLD_CONNECTOR_DEFAULT);
        else
            return prefs.getBoolean(Constants.USE_OLD_CONNECTOR, Constants.USE_OLD_CONNECTOR_DEFAULT);
    }

    public String getKeystorePassword() {
        // Load from Wifi-Preferences:
        String key = getStringWithSSID(Constants.KEYSTORE_PASSWORD, getCurrentSSID(wifiManager),
                wifibasedPrefsEnabled());

        if (prefs.contains(key))
            return prefs.getString(key, Constants.EMPTY);
        else
            return prefs.getString(Constants.KEYSTORE_PASSWORD, Constants.EMPTY);
    }

    public JSONConnector getConnector() {
        // Check if connector needs to be reinitialized because of per-wifi-settings:
        boolean useOldConnector = useOldConnector();
        if (useOldConnector && ttrssConnector instanceof JavaJSONConnector)
            ttrssConnector = null;
        if (!useOldConnector && ttrssConnector instanceof ApacheJSONConnector)
            ttrssConnector = null;

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
            } else
                throw new RuntimeException("Connector could not be initialized.");
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
            openUrlEmptyArticle = prefs.getBoolean(Constants.OPEN_URL_EMPTY_ARTICLE,
                    Constants.OPEN_URL_EMPTY_ARTICLE_DEFAULT);
        return openUrlEmptyArticle;
    }

    public void setOpenUrlEmptyArticle(boolean openUrlEmptyArticle) {
        put(Constants.OPEN_URL_EMPTY_ARTICLE, openUrlEmptyArticle);
        this.openUrlEmptyArticle = openUrlEmptyArticle;
    }

    public boolean useVolumeKeys() {
        if (useVolumeKeys == null)
            useVolumeKeys = prefs.getBoolean(Constants.USE_VOLUME_KEYS, Constants.USE_VOLUME_KEYS_DEFAULT);
        return useVolumeKeys;
    }

    public void setUseVolumeKeys(boolean useVolumeKeys) {
        put(Constants.USE_VOLUME_KEYS, useVolumeKeys);
        this.useVolumeKeys = useVolumeKeys;
    }

    public boolean loadMedia() {
        if (loadImages == null)
            loadImages = prefs.getBoolean(Constants.LOAD_IMAGES, Constants.LOAD_IMAGES_DEFAULT);
        return loadImages;
    }

    public void setLoadImages(boolean loadImages) {
        put(Constants.LOAD_IMAGES, loadImages);
        this.loadImages = loadImages;
    }

    public boolean invertBrowsing() {
        if (invertBrowsing == null)
            invertBrowsing = prefs.getBoolean(Constants.INVERT_BROWSING, Constants.INVERT_BROWSING_DEFAULT);
        return invertBrowsing;
    }

    public void setInvertBrowsing(boolean invertBrowsing) {
        put(Constants.INVERT_BROWSING, invertBrowsing);
        this.invertBrowsing = invertBrowsing;
    }

    public boolean workOffline() {
        if (workOffline == null)
            workOffline = prefs.getBoolean(Constants.WORK_OFFLINE, Constants.WORK_OFFLINE_DEFAULT);
        return workOffline;
    }

    public void setWorkOffline(boolean workOffline) {
        put(Constants.WORK_OFFLINE, workOffline);
        this.workOffline = workOffline;
    }

    public boolean goBackAfterMarkAllRead() {
        if (goBackAfterMarkAllRead == null)
            goBackAfterMarkAllRead = prefs.getBoolean(Constants.GO_BACK_AFTER_MARK_ALL_READ,
                    Constants.GO_BACK_AFTER_MARK_ALL_READ_DEFAULT);
        return goBackAfterMarkAllRead;
    }

    public void setGoBackAfterMarkAllRead(boolean goBackAfterMarkAllRead) {
        put(Constants.GO_BACK_AFTER_MARK_ALL_READ, goBackAfterMarkAllRead);
        this.goBackAfterMarkAllRead = goBackAfterMarkAllRead;
    }

    public boolean hideActionbar() {
        if (hideActionbar == null)
            hideActionbar = prefs.getBoolean(Constants.HIDE_ACTIONBAR, Constants.HIDE_ACTIONBAR_DEFAULT);
        return hideActionbar;
    }

    public void setHideActionbar(boolean hideActionbar) {
        put(Constants.HIDE_ACTIONBAR, hideActionbar);
        this.hideActionbar = hideActionbar;
    }

    public boolean allowTabletLayout() {
        if (allowTabletLayout == null)
            allowTabletLayout = prefs.getBoolean(Constants.ALLOW_TABLET_LAYOUT, Constants.ALLOW_TABLET_LAYOUT_DEFAULT);
        return allowTabletLayout;
    }

    public void setAllowTabletLayout(boolean allowTabletLayout) {
        put(Constants.ALLOW_TABLET_LAYOUT, allowTabletLayout);
        this.allowTabletLayout = allowTabletLayout;
    }

    // ******* DISPLAY-Options ****************************

    public int textZoom() {
        if (textZoom == null)
            textZoom = prefs.getInt(Constants.TEXT_ZOOM, Constants.TEXT_ZOOM_DEFAULT);
        return textZoom;
    }

    public void setTextZoom(int textZoom) {
        put(Constants.TEXT_ZOOM, textZoom);
        this.textZoom = textZoom;
    }

    public boolean supportZoomControls() {
        if (supportZoomControls == null)
            supportZoomControls = prefs.getBoolean(Constants.SUPPORT_ZOOM_CONTROLS,
                    Constants.SUPPORT_ZOOM_CONTROLS_DEFAULT);
        return supportZoomControls;
    }

    public void setSupportZoomControls(boolean supportZoomControls) {
        put(Constants.SUPPORT_ZOOM_CONTROLS, supportZoomControls);
        this.supportZoomControls = supportZoomControls;
    }

    private boolean allowHyphenation() {
        if (allowHyphenation == null)
            allowHyphenation = prefs.getBoolean(Constants.ALLOW_HYPHENATION, Constants.ALLOW_HYPHENATION_DEFAULT);
        return allowHyphenation;
    }

    public void setAllowHyphenation(boolean allowHyphenation) {
        put(Constants.ALLOW_HYPHENATION, allowHyphenation);
        this.allowHyphenation = allowHyphenation;
    }

    private String hyphenationLanguage() {
        if (hyphenationLanguage == null)
            hyphenationLanguage = prefs.getString(Constants.HYPHENATION_LANGUAGE,
                    Constants.HYPHENATION_LANGUAGE_DEFAULT);
        return hyphenationLanguage;
    }

    public void setHyphenationLanguage(String hyphenationLanguage) {
        put(Constants.HYPHENATION_LANGUAGE, hyphenationLanguage);
        this.hyphenationLanguage = hyphenationLanguage;
    }

    public boolean showVirtual() {
        if (showVirtual == null)
            showVirtual = prefs.getBoolean(Constants.SHOW_VIRTUAL, Constants.SHOW_VIRTUAL_DEFAULT);
        return showVirtual;
    }

    public void setDisplayVirtuals(boolean displayVirtuals) {
        put(Constants.SHOW_VIRTUAL, displayVirtuals);
        this.showVirtual = displayVirtuals;
    }

    public Integer showButtonsMode() {
        if (showButtonsMode == null)
            showButtonsMode = Integer.parseInt(prefs.getString(Constants.SHOW_BUTTONS_MODE,
                    Constants.SHOW_BUTTONS_MODE_DEFAULT));
        return showButtonsMode;
    }

    public void setShowButtonsMode(Integer showButtonsMode) {
        put(Constants.SHOW_BUTTONS_MODE, showButtonsMode);
        this.showButtonsMode = showButtonsMode;
    }

    public boolean onlyUnread() {
        if (onlyUnread == null)
            onlyUnread = prefs.getBoolean(Constants.ONLY_UNREAD, Constants.ONLY_UNREAD_DEFAULT);
        return onlyUnread;
    }

    public void setDisplayOnlyUnread(boolean displayOnlyUnread) {
        put(Constants.ONLY_UNREAD, displayOnlyUnread);
        this.onlyUnread = displayOnlyUnread;
    }

    public boolean onlyDisplayCachedImages() {
        if (onlyDisplayCachedImages == null)
            onlyDisplayCachedImages = prefs.getBoolean(Constants.ONLY_CACHED_IMAGES,
                    Constants.ONLY_CACHED_IMAGES_DEFAULT);
        return onlyDisplayCachedImages;
    }

    public void setDisplayCachedImages(boolean onlyDisplayCachedImages) {
        put(Constants.ONLY_CACHED_IMAGES, onlyDisplayCachedImages);
        this.onlyDisplayCachedImages = onlyDisplayCachedImages;
    }

    public boolean invertSortArticlelist() {
        if (invertSortArticlelist == null)
            invertSortArticlelist = prefs.getBoolean(Constants.INVERT_SORT_ARTICLELIST,
                    Constants.INVERT_SORT_ARTICLELIST_DEFAULT);
        return invertSortArticlelist;
    }

    public void setInvertSortArticleList(boolean invertSortArticleList) {
        put(Constants.INVERT_SORT_ARTICLELIST, invertSortArticleList);
        this.invertSortArticlelist = invertSortArticleList;
    }

    public boolean invertSortFeedscats() {
        if (invertSortFeedscats == null)
            invertSortFeedscats = prefs.getBoolean(Constants.INVERT_SORT_FEEDSCATS,
                    Constants.INVERT_SORT_FEEDSCATS_DEFAULT);
        return invertSortFeedscats;
    }

    public void setInvertSortFeedsCats(boolean invertSortFeedsCats) {
        put(Constants.INVERT_SORT_FEEDSCATS, invertSortFeedsCats);
        this.invertSortFeedscats = invertSortFeedsCats;
    }

    private boolean alignFlushLeft() {
        if (alignFlushLeft == null)
            alignFlushLeft = prefs.getBoolean(Constants.ALIGN_FLUSH_LEFT, Constants.ALIGN_FLUSH_LEFT_DEFAULT);
        return alignFlushLeft;
    }

    public void setAlignFlushLeft(boolean alignFlushLeft) {
        put(Constants.ALIGN_FLUSH_LEFT, alignFlushLeft);
        this.alignFlushLeft = alignFlushLeft;
    }

    public boolean dateTimeSystem() {
        if (dateTimeSystem == null)
            dateTimeSystem = prefs.getBoolean(Constants.DATE_TIME_SYSTEM, Constants.DATE_TIME_SYSTEM_DEFAULT);
        return dateTimeSystem;
    }

    public void setDateTimeSystem(boolean dateTimeSystem) {
        put(Constants.DATE_TIME_SYSTEM, dateTimeSystem);
        this.dateTimeSystem = dateTimeSystem;
    }

    public String dateString() {
        if (dateString == null)
            dateString = prefs.getString(Constants.DATE_STRING, Constants.DATE_STRING_DEFAULT);
        return dateString;
    }

    public void setDateString(String dateString) {
        put(Constants.DATE_STRING, dateString);
        this.dateString = dateString;
    }

    public String timeString() {
        if (timeString == null)
            timeString = prefs.getString(Constants.TIME_STRING, Constants.TIME_STRING_DEFAULT);
        return timeString;
    }

    public void setTimeString(String timeString) {
        put(Constants.TIME_STRING, timeString);
        this.timeString = timeString;
    }

    public String dateTimeString() {
        if (dateTimeString == null)
            dateTimeString = prefs.getString(Constants.DATE_TIME_STRING, Constants.DATE_TIME_STRING_DEFAULT);
        return dateTimeString;
    }

    public void setDateTimeString(String dateTimeString) {
        put(Constants.DATE_TIME_STRING, dateTimeString);
        this.dateTimeString = dateTimeString;
    }

    public int getTheme() {
        switch (getThemeInternal()) {
            case THEME_LIGHT:
                return R.style.Theme_Light;
            case THEME_BLACK:
                return R.style.Theme_Black;
            case THEME_WHITE:
                return R.style.Theme_White;
            case THEME_DARK:
            default:
                return R.style.Theme_Dark;
        }
    }

    public int getThemeInternal() {
        if (theme == null)
            theme = Integer.parseInt(prefs.getString(Constants.THEME, Constants.THEME_DEFAULT));
        return theme;
    }

    public void setTheme(int theme) {
        this.theme = theme;
        put(Constants.THEME, theme + "");
    }

    public int getThemeBackground() {
        switch (getThemeInternal()) {
            case THEME_LIGHT:
                return R.color.background_light;
            case THEME_BLACK:
                return R.color.background_black;
            case THEME_WHITE:
                return R.color.background_white;
            case THEME_DARK:
            default:
                return R.color.background_dark;
        }
    }

    public int getThemeFont() {
        switch (getThemeInternal()) {
            case THEME_LIGHT:
                return R.color.font_color_light;
            case THEME_BLACK:
                return R.color.font_color_black;
            case THEME_WHITE:
                return R.color.font_color_white;
            case THEME_DARK:
            default:
                return R.color.font_color_dark;
        }
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
        if (saveAttachment == null)
            saveAttachment = prefs.getString(Constants.SAVE_ATTACHMENT, Constants.SAVE_ATTACHMENT_DEFAULT);
        return saveAttachment;
    }

    public void setSaveAttachmentPath(String saveAttachment) {
        put(Constants.SAVE_ATTACHMENT, saveAttachment);
        this.saveAttachment = saveAttachment;
    }

    public String cacheFolder() {
        if (cacheFolder == null)
            cacheFolder = prefs.getString(Constants.CACHE_FOLDER, Constants.CACHE_FOLDER_DEFAULT);
        if (!cacheFolder.endsWith("/"))
            setCacheFolder(cacheFolder + "/");
        return cacheFolder;
    }

    public void setCacheFolder(String cacheFolder) {
        put(Constants.CACHE_FOLDER, cacheFolder);
        this.cacheFolder = cacheFolder;
    }

    public Integer cacheFolderMaxSize() {
        if (cacheFolderMaxSize == null)
            cacheFolderMaxSize = prefs.getInt(Constants.CACHE_FOLDER_MAX_SIZE, Constants.CACHE_FOLDER_MAX_SIZE_DEFAULT);
        return cacheFolderMaxSize;
    }

    public void setCacheFolderMaxSize(Integer cacheFolderMaxSize) {
        put(Constants.CACHE_FOLDER_MAX_SIZE, cacheFolderMaxSize);
        this.cacheFolderMaxSize = cacheFolderMaxSize;
    }

    public Integer cacheImageMaxSize() {
        if (cacheImageMaxSize == null)
            cacheImageMaxSize = prefs.getInt(Constants.CACHE_IMAGE_MAX_SIZE, Constants.CACHE_IMAGE_MAX_SIZE_DEFAULT);
        return cacheImageMaxSize;
    }

    public void setCacheImageMaxSize(Integer cacheImageMaxSize) {
        put(Constants.CACHE_IMAGE_MAX_SIZE, cacheImageMaxSize);
        this.cacheImageMaxSize = cacheImageMaxSize;
    }

    public Integer cacheImageMinSize() {
        if (cacheImageMinSize == null)
            cacheImageMinSize = prefs.getInt(Constants.CACHE_IMAGE_MIN_SIZE, Constants.CACHE_IMAGE_MIN_SIZE_DEFAULT);
        return cacheImageMinSize;
    }

    public void setCacheImageMinSize(Integer cacheImageMinSize) {
        put(Constants.CACHE_IMAGE_MIN_SIZE, cacheImageMinSize);
        this.cacheImageMinSize = cacheImageMinSize;
    }

    public boolean isDeleteDBScheduled() {
        if (deleteDbScheduled == null)
            deleteDbScheduled = prefs.getBoolean(Constants.DELETE_DB_SCHEDULED, Constants.DELETE_DB_SCHEDULED_DEFAULT);
        return deleteDbScheduled;
    }

    public void setDeleteDBScheduled(boolean isDeleteDBScheduled) {
        put(Constants.DELETE_DB_SCHEDULED, isDeleteDBScheduled);
        this.deleteDbScheduled = isDeleteDBScheduled;
    }

    public boolean cacheImagesOnStartup() {
        if (cacheImagesOnStartup == null)
            cacheImagesOnStartup = prefs.getBoolean(Constants.CACHE_IMAGES_ON_STARTUP,
                    Constants.CACHE_IMAGES_ON_STARTUP_DEFAULT);
        return cacheImagesOnStartup;
    }

    public void setCacheImagesOnStartup(boolean cacheImagesOnStartup) {
        put(Constants.CACHE_IMAGES_ON_STARTUP, cacheImagesOnStartup);
        this.cacheImagesOnStartup = cacheImagesOnStartup;
    }

    public boolean cacheImagesOnlyWifi() {
        if (cacheImagesOnlyWifi == null)
            cacheImagesOnlyWifi = prefs.getBoolean(Constants.CACHE_IMAGES_ONLY_WIFI,
                    Constants.CACHE_IMAGES_ONLY_WIFI_DEFAULT);
        return cacheImagesOnlyWifi;
    }

    public void setCacheImagesOnlyWifi(boolean cacheImagesOnlyWifi) {
        put(Constants.CACHE_IMAGES_ONLY_WIFI, cacheImagesOnlyWifi);
        this.cacheImagesOnlyWifi = cacheImagesOnlyWifi;
    }

    public boolean onlyUseWifi() {
        if (onlyUseWifi == null)
            onlyUseWifi = prefs.getBoolean(Constants.ONLY_USE_WIFI, Constants.ONLY_USE_WIFI_DEFAULT);
        return onlyUseWifi;
    }

    public void setOnlyUseWifi(boolean onlyUseWifi) {
        put(Constants.ONLY_USE_WIFI, onlyUseWifi);
        this.onlyUseWifi = onlyUseWifi;
    }

    // Returns true if noCrashreports OR noCrashreportsUntilUpdate is true.
    public boolean isNoCrashreports() {
        if (noCrashreports == null)
            noCrashreports = prefs.getBoolean(Constants.NO_CRASHREPORTS, Constants.NO_CRASHREPORTS_DEFAULT);
        if (noCrashreportsUntilUpdate == null)
            noCrashreportsUntilUpdate = prefs.getBoolean(Constants.NO_CRASHREPORTS_UNTIL_UPDATE,
                    Constants.NO_CRASHREPORTS_UNTIL_UPDATE_DEFAULT);
        return noCrashreports || noCrashreportsUntilUpdate;
    }

    public void setNoCrashreports(boolean noCrashreports) {
        put(Constants.NO_CRASHREPORTS, noCrashreports);
        this.noCrashreports = noCrashreports;
    }

    public void setNoCrashreportsUntilUpdate(boolean noCrashreportsUntilUpdate) {
        put(Constants.NO_CRASHREPORTS_UNTIL_UPDATE, noCrashreportsUntilUpdate);
        this.noCrashreportsUntilUpdate = noCrashreportsUntilUpdate;
    }

    // ******* INTERNAL Data ****************************

    public long appVersionCheckTime() {
        if (appVersionCheckTime == null)
            appVersionCheckTime = prefs.getLong(Constants.APP_VERSION_CHECK_TIME,
                    Constants.APP_VERSION_CHECK_TIME_DEFAULT);
        return appVersionCheckTime;
    }

    private void setAppVersionCheckTime(long appVersionCheckTime) {
        put(Constants.APP_VERSION_CHECK_TIME, appVersionCheckTime);
        this.appVersionCheckTime = appVersionCheckTime;
    }

    public int appLatestVersion() {
        if (appLatestVersion == null)
            appLatestVersion = prefs.getInt(Constants.APP_LATEST_VERSION, Constants.APP_LATEST_VERSION_DEFAULT);
        return appLatestVersion;
    }

    public void setAppLatestVersion(int appLatestVersion) {
        put(Constants.APP_LATEST_VERSION, appLatestVersion);
        this.appLatestVersion = appLatestVersion;
        setAppVersionCheckTime(System.currentTimeMillis());
        // Set current time, this only changes when it has been fetched from the server
    }

    public String getLastVersionRun() {
        if (lastVersionRun == null)
            lastVersionRun = prefs.getString(Constants.LAST_VERSION_RUN, Constants.LAST_VERSION_RUN_DEFAULT);
        return lastVersionRun;
    }

    public void setLastVersionRun(String lastVersionRun) {
        put(Constants.LAST_VERSION_RUN, lastVersionRun);
        this.lastVersionRun = lastVersionRun;
    }

    public boolean newInstallation() {
        // Initialized inside initializeController();
        return newInstallation;
    }

    public void setSinceId(int sinceId) {
        put(Constants.SINCE_ID, sinceId);
        this.sinceId = sinceId;
    }

    public int getSinceId() {
        if (sinceId == null)
            sinceId = prefs.getInt(Constants.SINCE_ID, Constants.SINCE_ID_DEFAULT);
        return sinceId;
    }

    public void setLastSync(long lastSync) {
        put(Constants.LAST_SYNC, lastSync);
        this.lastSync = lastSync;
    }

    public long getLastSync() {
        if (lastSync == null)
            lastSync = prefs.getLong(Constants.LAST_SYNC, Constants.LAST_SYNC_DEFAULT);
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
        put(Constants.LAST_CLEANUP, lastCleanup);
        this.lastCleanup = lastSync;
    }

    public long getLastCleanup() {
        if (lastCleanup == null)
            lastCleanup = prefs.getLong(Constants.LAST_CLEANUP, Constants.LAST_CLEANUP_DEFAULT);
        return lastCleanup;
    }

    private AsyncTask<Void, Void, Void> refreshPrefTask;

    public long getFreshArticleMaxAge() {
        if (freshArticleMaxAge == null)
            freshArticleMaxAge = prefs
                    .getLong(Constants.FRESH_ARTICLE_MAX_AGE, Constants.FRESH_ARTICLE_MAX_AGE_DEFAULT);

        if (freshArticleMaxAgeDate == null)
            freshArticleMaxAgeDate = prefs.getLong(Constants.FRESH_ARTICLE_MAX_AGE_DATE,
                    Constants.FRESH_ARTICLE_MAX_AGE_DATE_DEFAULT);

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
                            put(Constants.FRESH_ARTICLE_MAX_AGE, freshArticleMaxAge);

                            freshArticleMaxAgeDate = System.currentTimeMillis();
                            put(Constants.FRESH_ARTICLE_MAX_AGE_DATE, freshArticleMaxAgeDate);
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

    /**
     * If provided "key" resembles a setting as declared in Constants.java the corresponding variable in this class
     * will
     * be reset to null. Variable-Name ist built from the name of the field in Contants.java which holds the value from
     * "key" which was changed lately.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        // Indicate Restart of App is necessary if Theme-Pref is changed and value differs from old value:
        if (key.equals(Constants.THEME)) {
            int newTheme = Integer.parseInt(prefs.getString(key, Constants.THEME_DEFAULT));
            if (newTheme != getThemeInternal()) {
                setTheme(newTheme);
                reloadTheme();
                scheduledRestart = true;
            }
        }

        for (Field field : Constants.class.getDeclaredFields()) {

            // No default-values
            if (field.getName().endsWith(Constants.APPENDED_DEFAULT))
                continue;

            // Only use public static fields
            if (!Modifier.isStatic(field.getModifiers()) || !Modifier.isPublic(field.getModifiers()))
                continue;

            String fieldName = "";
            try {
                Object f = field.get(this);
                if (!(f instanceof String))
                    continue;

                if (!key.equals(f))
                    continue;

                // reset variable, it will be re-read on next access
                fieldName = Constants.constant2Var(field.getName());
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
        if (wifiSSID == null || !wifibasedPrefsEnabled)
            return param;
        else
            return wifiSSID + param;
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

        if (ret < min || ret > max)
            ret = (min + max) / 2;

        return ret;
    }

    public void setViewSize(MenuActivity activity, boolean isVertical, int size) {
        if (size <= 0)
            return;
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
