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

package org.ttrssreader.controllers;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import javax.net.ssl.SSLSocketFactory;
import org.stringtemplate.v4.ST;
import org.ttrssreader.R;
import org.ttrssreader.gui.CategoryActivity;
import org.ttrssreader.gui.FeedHeadlineActivity;
import org.ttrssreader.gui.MenuActivity;
import org.ttrssreader.imageCache.ImageCache;
import org.ttrssreader.net.ApacheJSONConnector;
import org.ttrssreader.net.JSONConnector;
import org.ttrssreader.net.JavaJSONConnector;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.SSLUtils;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

/**
 * Not entirely sure why this is called the "Controller". Actually, in terms of MVC, it isn't the controller. There
 * isn't one in here but it's called like that and I don't have a better name so we stay with it.
 */
public class Controller implements OnSharedPreferenceChangeListener {
    
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
    
    public static final int THEME_DARK = 1;
    public static final int THEME_LIGHT = 2;
    public static final int THEME_BLACK = 3;
    public static final int THEME_WHITE = 4;
    
    private Context context;
    private WifiManager wifiManager;
    
    private JSONConnector ttrssConnector;
    private ImageCache imageCache = null;
    
    private boolean isHeadless = false;
    private String imageCacheLock = "lock";
    
    private static Boolean initialized = false;
    private SharedPreferences prefs = null;
    private static boolean preferencesChanged = false;
    
    private String url = null;
    private String username = null;
    private String password = null;
    private String httpUsername = null;
    private String httpPassword = null;
    private Boolean useHttpAuth = null;
    private Boolean trustAllSsl = null;
    private Boolean trustAllHosts = null;
    private Boolean useOldConnector;
    private Boolean useKeystore = null;
    private String keystorePassword = null;
    private Boolean useOfALazyServer = null;
    
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
    private Boolean markReadInMenu = null;
    private Boolean showVirtual = null;
    private Integer showButtonsMode = null;
    private Boolean onlyUnread = null;
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
    private Boolean deleteDbScheduled = null;
    private Boolean cacheImagesOnStartup = null;
    private Boolean cacheImagesOnlyWifi = null;
    private Boolean onlyUseWifi = null;
    
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
    
    public volatile Set<Integer> lastOpenedFeeds = new HashSet<Integer>();
    public volatile Set<Integer> lastOpenedArticles = new HashSet<Integer>();
    
    // Article-View-Stuff
    public static String htmlTemplate = "";
    public static int relSwipeMinDistance;
    public static int relSwipeMaxOffPath;
    public static int relSwipteThresholdVelocity;
    public static int displayHeight;
    public static int displayWidth;
    
    public static boolean isTablet = false;
    private boolean scheduledRestart = false;
    
    // SocketFactory for SSL-Connections, doesn't need to be accessed but indicates it is initialized if != null.
    private SSLSocketFactory sslSocketFactory = null;
    
    // Singleton (see http://stackoverflow.com/a/11165926)
    private Controller() {
    }
    
    private static class InstanceHolder {
        private static final Controller instance = new Controller();
    }
    
    public static Controller getInstance() {
        return InstanceHolder.instance;
    }
    
    public void checkAndInitializeController(final Context context, final Display display) {
        synchronized (initialized) {
            this.context = context;
            this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            
            if (!initialized) {
                initializeController(display);
                initialized = true;
            }
        }
    }
    
    private void initializeController(final Display display) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        // Initially read absolutely necessary preferences:
        sizeVerticalCategory = prefs.getInt(SIZE_VERTICAL_CATEGORY, -1);
        sizeHorizontalCategory = prefs.getInt(SIZE_HORIZONTAL_CATEGORY, -1);
        sizeVerticalHeadline = prefs.getInt(SIZE_VERTICAL_HEADLINE, -1);
        sizeHorizontalHeadline = prefs.getInt(SIZE_HORIZONTAL_HEADLINE, -1);
        
        // Check for new installation
        if (!prefs.contains(Constants.URL) && !prefs.contains(Constants.LAST_VERSION_RUN)) {
            newInstallation = true;
        }
        
        // Attempt to initialize some stuff in a background-thread to reduce loading time. Start a login-request
        // separately because this takes some time. Also initialize SSL-Stuff since the login needs this.
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                try {
                    SSLUtils.trustAllCertOrHost(Controller.getInstance().trustAllSsl(), Controller.getInstance()
                            .trustAllHosts());
                    
                    if (sslSocketFactory == null && Controller.getInstance().useKeystore()) {
                        sslSocketFactory = SSLUtils.initializePrivateKeystore(Controller.getInstance()
                                .getKeystorePassword());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                // This will be accessed when displaying an article or starting the imageCache. When caching it is done
                // anyway so we can just do it in background and the ImageCache starts once it is done.
                getImageCache();
                
                // Only need once we are displaying the feed-list or an article...
                refreshDisplayMetrics(display);
                
                // Loads all article and webview related resources
                reloadTheme();
                
                enableHttpResponseCache(context);
                
                return null;
            }
        }.execute();
    }
    
    private void reloadTheme() {
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
        synchronized (htmlTemplate) {
            htmlTemplate = htmlTmpl.render();
        }
    }
    
    /**
     * Enables HTTP response caching on devices that support it, see
     * http://android-developers.blogspot.de/2011/09/androids-http-clients.html
     * 
     * @param context
     */
    private void enableHttpResponseCache(Context context) {
        try {
            long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
            File httpCacheDir = new File(context.getCacheDir(), "http");
            Class.forName("android.net.http.HttpResponseCache").getMethod("install", File.class, long.class)
                    .invoke(null, httpCacheDir, httpCacheSize);
        } catch (Exception httpResponseCacheNotAvailable) {
        }
    }
    
    private synchronized void initializeConnector() {
        if (ttrssConnector != null)
            return;
        
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO || useOldConnector()) {
            ttrssConnector = new ApacheJSONConnector(context);
        } else {
            ttrssConnector = new JavaJSONConnector(context);
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
    
    public URI uri() throws URISyntaxException {
        return new URI(hostname());
    }
    
    public URL url() throws MalformedURLException {
        return new URL(hostname());
    }
    
    public String hostname() {
        // Load from Wifi-Preferences:
        String key = getStringWithSSID(Constants.URL, getCurrentSSID(wifiManager));
        
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
        String key = getStringWithSSID(Constants.USERNAME, getCurrentSSID(wifiManager));
        
        if (prefs.contains(key))
            username = prefs.getString(key, Constants.EMPTY);
        else
            username = prefs.getString(Constants.USERNAME, Constants.EMPTY);
        
        return username;
    }
    
    public String password() {
        // Load from Wifi-Preferences:
        String key = getStringWithSSID(Constants.PASSWORD, getCurrentSSID(wifiManager));
        
        if (prefs.contains(key))
            password = prefs.getString(key, Constants.EMPTY);
        else
            password = prefs.getString(Constants.PASSWORD, Constants.EMPTY);
        
        return password;
    }
    
    public boolean useHttpAuth() {
        if (useHttpAuth == null)
            useHttpAuth = prefs.getBoolean(Constants.USE_HTTP_AUTH, Constants.USE_HTTP_AUTH_DEFAULT);
        return useHttpAuth;
    }
    
    public String httpUsername() {
        if (httpUsername == null)
            httpUsername = prefs.getString(Constants.HTTP_USERNAME, Constants.EMPTY);
        return httpUsername;
    }
    
    public String httpPassword() {
        if (httpPassword == null)
            httpPassword = prefs.getString(Constants.HTTP_PASSWORD, Constants.EMPTY);
        return httpPassword;
    }
    
    public boolean useKeystore() {
        if (useKeystore == null)
            useKeystore = prefs.getBoolean(Constants.USE_KEYSTORE, Constants.USE_KEYSTORE_DEFAULT);
        return useKeystore;
    }
    
    public boolean trustAllSsl() {
        if (trustAllSsl == null)
            trustAllSsl = prefs.getBoolean(Constants.TRUST_ALL_SSL, Constants.TRUST_ALL_SSL_DEFAULT);
        return trustAllSsl;
    }
    
    public boolean trustAllHosts() {
        if (trustAllHosts == null)
            trustAllHosts = prefs.getBoolean(Constants.TRUST_ALL_HOSTS, Constants.TRUST_ALL_HOSTS_DEFAULT);
        return trustAllSsl;
    }
    
    private boolean useOldConnector() {
        if (useOldConnector == null)
            useOldConnector = prefs.getBoolean(Constants.USE_OLD_CONNECTOR, Constants.USE_OLD_CONNECTOR_DEFAULT);
        return useOldConnector;
    }
    
    public JSONConnector getConnector() {
        // Initialized inside initializeController();
        if (ttrssConnector != null) {
            return ttrssConnector;
        } else {
            initializeConnector();
            if (ttrssConnector != null)
                return ttrssConnector;
            else
                throw new RuntimeException("Connector could not be initialized.");
        }
    }
    
    public ImageCache getImageCache() {
        return getImageCache(true);
    }
    
    public ImageCache getImageCache(boolean wait) {
        if (imageCache == null && wait) {
            synchronized (imageCacheLock) {
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
    
    public String getKeystorePassword() {
        if (keystorePassword == null)
            keystorePassword = prefs.getString(Constants.KEYSTORE_PASSWORD, Constants.EMPTY);
        return keystorePassword;
    }
    
    public boolean isHeadless() {
        return isHeadless;
    }
    
    public void setHeadless(boolean isHeadless) {
        this.isHeadless = isHeadless;
    }
    
    // ******* USAGE-Options ****************************
    
    public boolean lazyServer() {
        // Load from Wifi-Preferences:
        String key = getStringWithSSID(Constants.USE_OF_A_LAZY_SERVER, getCurrentSSID(wifiManager));
        
        if (prefs.contains(key))
            useOfALazyServer = prefs.getBoolean(key, Constants.USE_OF_A_LAZY_SERVER_DEFAULT);
        else
            useOfALazyServer = prefs.getBoolean(Constants.USE_OF_A_LAZY_SERVER, Constants.USE_OF_A_LAZY_SERVER_DEFAULT);
        
        return useOfALazyServer;
    }
    
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
    
    public boolean loadImages() {
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
    
    public boolean allowHyphenation() {
        if (allowHyphenation == null)
            allowHyphenation = prefs.getBoolean(Constants.ALLOW_HYPHENATION, Constants.ALLOW_HYPHENATION_DEFAULT);
        return allowHyphenation;
    }
    
    public void setAllowHyphenation(boolean allowHyphenation) {
        put(Constants.ALLOW_HYPHENATION, allowHyphenation);
        this.allowHyphenation = allowHyphenation;
    }
    
    public String hyphenationLanguage() {
        if (hyphenationLanguage == null)
            hyphenationLanguage = prefs.getString(Constants.HYPHENATION_LANGUAGE,
                    Constants.HYPHENATION_LANGUAGE_DEFAULT);
        return hyphenationLanguage;
    }
    
    public void setHyphenationLanguage(String hyphenationLanguage) {
        put(Constants.HYPHENATION_LANGUAGE, hyphenationLanguage);
        this.hyphenationLanguage = hyphenationLanguage;
    }
    
    public boolean markReadInMenu() {
        if (markReadInMenu == null)
            markReadInMenu = prefs.getBoolean(Constants.MARK_READ_IN_MENU, Constants.MARK_READ_IN_MENU_DEFAULT);
        return markReadInMenu;
    }
    
    public void setMarkReadInMenu(boolean markReadInMenu) {
        put(Constants.MARK_READ_IN_MENU, markReadInMenu);
        this.markReadInMenu = markReadInMenu;
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
    
    public boolean alignFlushLeft() {
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
        put(Constants.THEME, theme + "");
        this.theme = theme;
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
            Log.w(Utils.TAG, "lowMemory-Situation detected, trying to reduce memory footprint...");
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
                            Log.d(Utils.TAG, "Pref \"FRESH_ARTICLE_MAX_AGE\" could not be fetched from server: " + s);
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
        
        /*
         * The following Code is extracted from
         * https://code.google.com/p/zippy-android/source/browse/trunk/examples/SharedPreferencesCompat.java
         * 
         * Copyright (C) 2010 The Android Open Source Project
         * 
         * Licensed under the Apache License, Version 2.0 (the "License");
         * you may not use this file except in compliance with the License.
         * You may obtain a copy of the License at
         * 
         * http://www.apache.org/licenses/LICENSE-2.0
         * 
         * Unless required by applicable law or agreed to in writing, software
         * distributed under the License is distributed on an "AS IS" BASIS,
         * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
         * See the License for the specific language governing permissions and
         * limitations under the License.
         */
        // Uses faster apply() instead of commit() if method is available
        if (sApplyMethod != null) {
            try {
                sApplyMethod.invoke(editor);
                return;
            } catch (InvocationTargetException unused) {
                // fall through
            } catch (IllegalAccessException unused) {
                // fall through
            }
        }
        editor.commit();
    }
    
    private static final Method sApplyMethod = findApplyMethod();
    
    private static Method findApplyMethod() {
        try {
            Class<?> cls = SharedPreferences.Editor.class;
            return cls.getMethod("apply");
        } catch (NoSuchMethodException unused) {
            // fall through
        }
        return null;
    }
    
    /**
     * If provided "key" resembles a setting as declared in Constants.java the corresponding variable in this class will
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
                
                if (!key.equals((String) f))
                    continue;
                
                // reset variable, it will be re-read on next access
                fieldName = Constants.constant2Var(field.getName());
                Controller.class.getDeclaredField(fieldName).set(this, null); // "Declared" so also private
                setPreferencesChanged(true);
                
            } catch (Exception e) {
                Log.e(Utils.TAG, "Field not found: " + fieldName);
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
    
    private static String getStringWithSSID(String param, String wifiSSID) {
        if (wifiSSID == null)
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
    private Integer sizeVerticalHeadline;
    private Integer sizeHorizontalHeadline;
    
    public int getViewSize(MenuActivity activity, boolean isVertical) {
        if (activity instanceof CategoryActivity) {
            if (isVertical) {
                return sizeVerticalCategory;
            } else {
                return sizeHorizontalCategory;
            }
        } else if (activity instanceof FeedHeadlineActivity) {
            if (isVertical) {
                return sizeVerticalHeadline;
            } else {
                return sizeHorizontalHeadline;
            }
        }
        return -1;
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
            if (isVertical) {
                sizeVerticalHeadline = size;
                put(SIZE_VERTICAL_HEADLINE, size);
            } else {
                sizeHorizontalHeadline = size;
                put(SIZE_HORIZONTAL_HEADLINE, size);
            }
        }
    }
    
}
