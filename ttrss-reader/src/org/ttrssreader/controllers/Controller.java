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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.ttrssreader.R;
import org.ttrssreader.gui.MenuActivity;
import org.ttrssreader.imageCache.ImageCache;
import org.ttrssreader.net.Connector;
import org.ttrssreader.net.JSONConnector;
import org.ttrssreader.preferences.Constants;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Display;

/**
 * Not entirely sure why this is called the "Controller". Actually, in terms of MVC, it isn't the controller. There
 * isn't one in here but it's called like that and I don't have a better name so we stay with it.
 */
public class Controller implements OnSharedPreferenceChangeListener {
    
    public final static String JSON_END_URL = "api/";
    private static final String MARKER_ALIGN = "TEXT_ALIGN_MARKER";
    private static final String MARKER_LINK = "LINK_MARKER";
    private static final String MARKER_LINK_VISITED = "LINK_VISITED_MARKER";
    
    private boolean initialized = false;
    private Context context;
    private Connector ttrssConnector;
    private ImageCache imageCache;
    private String imageCacheLock = ""; // Use this to lock access to the cache as workaround for NPE on imageCache
    
    private static Controller instance = null;
    private SharedPreferences prefs = null;
    
    private String url = null;
    private String username = null;
    private String password = null;
    private String httpUsername = null;
    private String httpPassword = null;
    private Boolean useHttpAuth = null;
    private Boolean trustAllSsl = null;
    private Boolean useKeystore = null;
    private String keystorePassword = null;
    private Boolean lazyServer = null;
    
    private Boolean automaticMarkRead = null;
    private Boolean openUrlEmptyArticle = null;
    private Boolean useVolumeKeys = null;
    private Boolean vibrateOnLastArticle = null;
    private Boolean workOffline = null;
    
    private Boolean showVirtual = null;
    private Boolean useSwipe = null;
    private Boolean useButtons = null;
    private Boolean onlyUnread = null;
    private Integer articleLimit = null;
    private Boolean displayArticleHeader = null;
    private Boolean invertSortArticlelist = null;
    private Boolean invertSortFeedscats = null;
    private Boolean alignFlushLeft = null;
    private Boolean injectArticleLink = null;
    private Boolean dateTimeSystem = null;
    private String dateString = null;
    private String timeString = null;
    private Boolean darkBackground = null;
    
    private Integer imageCacheSize = null;
    private Boolean imageCacheUnread = null;
    private String saveAttachment = null;
    private String cacheFolder = null;
    private Boolean isVacuumDBScheduled = null;
    private Boolean isDeleteDBScheduled = null;
    private Boolean isDeleteDBOnStartup = null;
    private Boolean cacheImagesOnStartup = null;
    private Boolean cacheImagesOnlyWifi = null;
    private Boolean logSensitiveData = null;
    
    private Boolean isExecuteOnExecutorAvailable = null;
    private Long apiLevelUpdated = null;
    private Integer apiLevel = null;
    private Long appVersionCheckTime = null;
    private Integer appLatestVersion = null;
    private Long lastUpdateTime = null;
    private String lastVersionRun = null;
    private Boolean newInstallation = false;
    private String freshArticleMaxAge = "";
    private Long lastVacuumDate = null;
    
    public volatile Integer lastOpenedFeed = null;
    public volatile Integer lastOpenedArticle = null;
    
    // Article-View-Stuff
    public static String htmlHeader = "";
    public static String htmlHeaderZoom = "";
    public static int absHeight = -1;
    public static int absWidth = -1;
    public static int swipeAreaHeight = -1;
    public static int swipeWidth = -1;
    public static int swipeAreaWidth = -1;
    public static int swipeHeight = -1;
    public static int padding = -1;
    public static boolean landscape = false;
    
    // Singleton
    private Controller() {
    }
    
    public static Controller getInstance() {
        if (instance == null || instance.prefs == null) {
            synchronized (Controller.class) {
                if (instance == null || instance.prefs == null) {
                    instance = new Controller();
                }
            }
        }
        return instance;
    }
    
    public synchronized void checkAndInitializeController(final Context context, final Display display) {
        this.context = context;
        
        if (!initialized) {
            initializeController(display);
            initialized = true;
        }
    }
    
    public static synchronized void checkAndInitializeController(final Context context, boolean force_dummy_parameter) {
        Controller.instance = null;
        Controller.getInstance().checkAndInitializeController(context, null);
    }
    
    private synchronized void initializeController(final Display display) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        // Check for new installation
        if (!prefs.contains(Constants.URL) && !prefs.contains(Constants.LAST_VERSION_RUN)) {
            newInstallation = true;
        }
        
        ttrssConnector = new JSONConnector();
        
        // Attempt to initialize some stuff in a background-thread to reduce loading time
        // TODO: Check if it works..
        
        // Start a login-request separately because this takes some time
        new Thread(new Runnable() {
            public void run() {
                ttrssConnector.sessionAlive();
            }
        }).start();
        
        new Thread(new Runnable() {
            public void run() {
                
                // Only need once we are displaying the feed-list or an article...
                refreshDisplayMetrics(display);
                
                // This is only needed once an article is displayed
                synchronized (htmlHeader) {
                    // Article-Prefetch-Stuff from Raw-Ressources and System
                    htmlHeader = context.getResources().getString(R.string.INJECT_HTML_HEAD);
                    htmlHeaderZoom = context.getResources().getString(R.string.INJECT_HTML_HEAD_ZOOM);
                    
                    // Replace alignment-marker with the requested layout, align:left or justified
                    String replaceAlign = "";
                    if (alignFlushLeft()) {
                        replaceAlign = context.getResources().getString(R.string.ALIGN_LEFT);
                        htmlHeader = htmlHeader.replace(MARKER_ALIGN, replaceAlign);
                        htmlHeaderZoom = htmlHeaderZoom.replace(MARKER_ALIGN, replaceAlign);
                    } else {
                        replaceAlign = context.getResources().getString(R.string.ALIGN_JUSTIFY);
                        htmlHeader = htmlHeader.replace(MARKER_ALIGN, replaceAlign);
                        htmlHeaderZoom = htmlHeaderZoom.replace(MARKER_ALIGN, replaceAlign);
                    }
                    
                    // Replace color-markers with matching colors for the requested background
                    String replaceLink = "";
                    String replaceLinkVisited = "";
                    if (darkBackground()) {
                        replaceLink = context.getResources().getString(R.string.COLOR_LINK_DARK);
                        replaceLinkVisited = context.getResources().getString(R.string.COLOR_LINK_DARK_VISITED);
                        htmlHeader = htmlHeader.replace(MARKER_LINK, replaceLink);
                        htmlHeader = htmlHeader.replace(MARKER_LINK_VISITED, replaceLinkVisited);
                        htmlHeaderZoom = htmlHeaderZoom.replace(MARKER_LINK, replaceLink);
                        htmlHeaderZoom = htmlHeaderZoom.replace(MARKER_LINK_VISITED, replaceLinkVisited);
                    } else {
                        replaceLink = context.getResources().getString(R.string.COLOR_LINK_LIGHT);
                        replaceLinkVisited = context.getResources().getString(R.string.COLOR_LINK_LIGHT_VISITED);
                        htmlHeader = htmlHeader.replace(MARKER_LINK, replaceLink);
                        htmlHeader = htmlHeader.replace(MARKER_LINK_VISITED, replaceLinkVisited);
                        htmlHeaderZoom = htmlHeaderZoom.replace(MARKER_LINK, replaceLink);
                        htmlHeaderZoom = htmlHeaderZoom.replace(MARKER_LINK_VISITED, replaceLinkVisited);
                    }
                }
                
                // This will be accessed when displaying an article or starting the imageCache. When caching it is done
                // anyway so we can just do it in background and the ImageCache starts once it is done.
                getImageCache(context);
            }
        }).start();
        
    }
    
    public static void refreshDisplayMetrics(Display display) {
        if (display == null)
            return;
        
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        
        absHeight = metrics.heightPixels;
        absWidth = metrics.widthPixels;
        
        // Portrait-Mode
        swipeAreaHeight = (int) (absHeight * 0.25); // 25% of absolute height
        swipeWidth = (int) (absWidth * 0.5); // 50% of absolute width
        
        // Landscape-Mode
        swipeAreaWidth = (int) (absWidth * 0.20); // 20% of absolute width
        swipeHeight = (int) (absHeight * 0.5); // 50% of absolute height
        
        // Is this fine for orientation-recognition? display.getOrientation() is deprecated and
        // display.getRotation() doesn't give information about which layout is used.
        if (absWidth > absHeight) {
            landscape = true;
            padding = (int) ((swipeAreaWidth / 2) - (16 * metrics.density)); // TODO: Why 16???
        } else {
            landscape = false;
            padding = (int) ((swipeAreaHeight / 2) - (16 * metrics.density)); // TODO: Why 16???
        }
    }
    
    // ******* CONNECTION-Options ****************************
    
    public URI url() {
        if (url == null)
            url = prefs.getString(Constants.URL, Constants.URL_DEFAULT);
        
        if (!url.endsWith(JSON_END_URL)) {
            if (!url.endsWith("/")) {
                url += "/";
            }
            url += JSON_END_URL;
        }
        
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            // Return default URL if creating an URI from the configured URL fails...
            try {
                return new URI(Constants.URL_DEFAULT);
            } catch (URISyntaxException e1) {
                throw new RuntimeException(e1);
            }
        }
    }
    
    public String updateTriggerURI() {
        String url = prefs.getString(Constants.URL, Constants.URL_DEFAULT);
        
        if (!url.endsWith(JSON_END_URL)) {
            if (!url.endsWith("/")) {
                url += "/";
            }
        }
        
        final String updateSuffix = "backend.php?op=globalUpdateFeeds&daemon=1";
        return url + updateSuffix;
    }
    
    public String username() {
        if (username == null)
            username = prefs.getString(Constants.USERNAME, Constants.EMPTY);
        return username;
    }
    
    public String password() {
        if (password == null)
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
    
    public Connector getConnector() throws NotInitializedException {
        // Initialized inside initializeController();
        if (ttrssConnector != null) {
            return ttrssConnector;
        } else {
            throw new NotInitializedException(new NullPointerException());
        }
    }
    
    public ImageCache getImageCache(Context context) {
        synchronized (imageCacheLock) {
            if (imageCache == null) {
                imageCache = new ImageCache(2000);
                if (context == null || !imageCache.enableDiskCache()) {
                    imageCache = null;
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
    
    // ******* USAGE-Options ****************************
    
    public boolean automaticMarkRead() {
        if (automaticMarkRead == null)
            automaticMarkRead = prefs.getBoolean(Constants.AUTOMATIC_MARK_READ, Constants.AUTOMATIC_MARK_READ_DEFAULT);
        return automaticMarkRead;
    }
    
    public void setAutomaticMarkRead(boolean automaticMarkRead) {
        put(Constants.AUTOMATIC_MARK_READ, automaticMarkRead);
        this.automaticMarkRead = automaticMarkRead;
    }
    
    public boolean lazyServer() {
        if (lazyServer == null)
            lazyServer = prefs.getBoolean(Constants.USE_OF_A_LAZY_SERVER, Constants.USE_OF_A_LAZY_SERVER_DEFAULT);
        return lazyServer;
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
    
    public boolean vibrateOnLastArticle() {
        if (vibrateOnLastArticle == null)
            vibrateOnLastArticle = prefs.getBoolean(Constants.VIBRATE_ON_LAST_ARTICLE,
                    Constants.VIBRATE_ON_LAST_ARTICLE_DEFAULT);
        return vibrateOnLastArticle;
    }
    
    public void setVibrateOnLastArticle(boolean vibrateOnLastArticle) {
        put(Constants.VIBRATE_ON_LAST_ARTICLE, vibrateOnLastArticle);
        this.vibrateOnLastArticle = vibrateOnLastArticle;
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
    
    // ******* DISPLAY-Options ****************************
    
    public boolean showVirtual() {
        if (showVirtual == null)
            showVirtual = prefs.getBoolean(Constants.SHOW_VIRTUAL, Constants.SHOW_VIRTUAL_DEFAULT);
        return showVirtual;
    }
    
    public void setDisplayVirtuals(boolean displayVirtuals) {
        put(Constants.SHOW_VIRTUAL, displayVirtuals);
        this.showVirtual = displayVirtuals;
    }
    
    public boolean useSwipe() {
        if (useSwipe == null)
            useSwipe = prefs.getBoolean(Constants.USE_SWIPE, Constants.USE_SWIPE_DEFAULT);
        return useSwipe;
    }
    
    public void setUseSwipe(boolean useSwipe) {
        put(Constants.USE_SWIPE, useSwipe);
        this.useSwipe = useSwipe;
    }
    
    public boolean useButtons() {
        if (useButtons == null)
            useButtons = prefs.getBoolean(Constants.USE_BUTTONS, Constants.USE_BUTTONS_DEFAULT);
        return useButtons;
    }
    
    public void setUseButtons(boolean useButtons) {
        put(Constants.USE_BUTTONS, useButtons);
        this.useButtons = useButtons;
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
    
    public int getArticleLimit() {
        if (articleLimit == null)
            articleLimit = prefs.getInt(Constants.ARTICLE_LIMIT, Constants.ARTICLE_LIMIT_DEFAULT);
        return articleLimit;
    }
    
    public void setArticleLimit(int articleLimit) {
        put(Constants.ARTICLE_LIMIT, articleLimit);
        this.articleLimit = articleLimit;
    }
    
    public boolean displayArticleHeader() {
        if (displayArticleHeader == null)
            displayArticleHeader = prefs.getBoolean(Constants.DISPLAY_ARTICLE_HEADER,
                    Constants.DISPLAY_ARTICLE_HEADER_DEFAULT);
        return displayArticleHeader;
    }
    
    public void setDisplayArticleHeader(boolean displayArticleHeader) {
        put(Constants.DISPLAY_ARTICLE_HEADER, displayArticleHeader);
        this.displayArticleHeader = displayArticleHeader;
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
    
    public boolean injectArticleLink() {
        if (injectArticleLink == null)
            injectArticleLink = prefs.getBoolean(Constants.INJECT_ARTICLE_LINK, Constants.INJECT_ARTICLE_LINK_DEFAULT);
        return injectArticleLink;
    }
    
    public void setInjectArticleLink(boolean injectArticleLink) {
        put(Constants.INJECT_ARTICLE_LINK, injectArticleLink);
        this.injectArticleLink = injectArticleLink;
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
    
    public boolean darkBackground() {
        if (darkBackground == null)
            darkBackground = prefs.getBoolean(Constants.DARK_BACKGROUND, Constants.DARK_BACKGROUND_DEFAULT);
        return darkBackground;
    }
    
    public void setDarkBackground(boolean darkBackground) {
        put(Constants.DARK_BACKGROUND, darkBackground);
        this.darkBackground = darkBackground;
    }
    
    // SYSTEM
    
    public int getImageCacheSize() {
        if (imageCacheSize == null)
            imageCacheSize = prefs.getInt(Constants.IMAGE_CACHE_SIZE, Constants.IMAGE_CACHE_SIZE_DEFAULT);
        return imageCacheSize;
    }
    
    public void setImageCacheSize(int imageCacheSize) {
        put(Constants.IMAGE_CACHE_SIZE, imageCacheSize);
        this.imageCacheSize = imageCacheSize;
    }
    
    public boolean isImageCacheUnread() {
        if (imageCacheUnread == null)
            imageCacheUnread = prefs.getBoolean(Constants.IMAGE_CACHE_UNREAD, Constants.IMAGE_CACHE_UNREAD_DEFAULT);
        return imageCacheUnread;
    }
    
    public void setImageCacheUnread(boolean imageCacheUnread) {
        put(Constants.IMAGE_CACHE_UNREAD, imageCacheUnread);
        this.imageCacheUnread = imageCacheUnread;
    }
    
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
        return cacheFolder;
    }
    
    public void setCacheFolder(String cacheFolder) {
        put(Constants.CACHE_FOLDER, cacheFolder);
        this.cacheFolder = cacheFolder;
    }
    
    public boolean isVacuumDBScheduled() {
        long time = System.currentTimeMillis();
        long thirtyDays = 30 * 24 * 60 * 60 * 1000L; // Note "L" for explicit cast to long
        
        if (lastVacuumDate() < (time - thirtyDays))
            return true;
        
        if (isVacuumDBScheduled == null)
            isVacuumDBScheduled = prefs
                    .getBoolean(Constants.VACUUM_DB_SCHEDULED, Constants.VACUUM_DB_SCHEDULED_DEFAULT);
        return isVacuumDBScheduled;
    }
    
    public void setVacuumDBScheduled(boolean isVacuumDBScheduled) {
        put(Constants.VACUUM_DB_SCHEDULED, isVacuumDBScheduled);
        this.isVacuumDBScheduled = isVacuumDBScheduled;
    }
    
    public boolean isDeleteDBScheduled() {
        if (isDeleteDBScheduled == null)
            isDeleteDBScheduled = prefs
                    .getBoolean(Constants.DELETE_DB_SCHEDULED, Constants.DELETE_DB_SCHEDULED_DEFAULT);
        return isDeleteDBScheduled;
    }
    
    public void setDeleteDBScheduled(boolean isDeleteDBScheduled) {
        put(Constants.DELETE_DB_SCHEDULED, isDeleteDBScheduled);
        this.isDeleteDBScheduled = isDeleteDBScheduled;
    }
    
    // Reset to false if preference to delete on every start is not set
    public void resetDeleteDBScheduled() {
        if (!isDeleteDBOnStartup()) {
            put(Constants.DELETE_DB_SCHEDULED, Constants.DELETE_DB_SCHEDULED_DEFAULT);
            this.isDeleteDBScheduled = Constants.DELETE_DB_SCHEDULED_DEFAULT;
        }
    }
    
    public boolean isDeleteDBOnStartup() {
        if (isDeleteDBOnStartup == null)
            isDeleteDBOnStartup = prefs.getBoolean(Constants.DELETE_DB_ON_STARTUP,
                    Constants.DELETE_DB_ON_STARTUP_DEFAULT);
        return isDeleteDBOnStartup;
    }
    
    public void setDeleteDBOnStartup(boolean isDeleteDBOnStartup) {
        put(Constants.DELETE_DB_ON_STARTUP, isDeleteDBOnStartup);
        this.isDeleteDBOnStartup = isDeleteDBOnStartup;
        setDeleteDBScheduled(isDeleteDBOnStartup);
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
    
    public boolean logSensitiveData() {
        if (logSensitiveData == null)
            logSensitiveData = prefs.getBoolean(Constants.LOG_SENSITIVE_DATA, Constants.LOG_SENSITIVE_DATA_DEFAULT);
        return logSensitiveData;
    }
    
    public void setLogSensitiveData(boolean logSensitiveData) {
        put(Constants.LOG_SENSITIVE_DATA, logSensitiveData);
        this.logSensitiveData = logSensitiveData;
    }
    
    // ******* INTERNAL Data ****************************
    
    public boolean isExecuteOnExecutorAvailable() {
        if (isExecuteOnExecutorAvailable == null) {
            Class<?> cls = AsyncTask.class;
            for (Method m : cls.getMethods()) {
                if ("executeOnExecutor".equals(m.getName())) {
                    isExecuteOnExecutorAvailable = true;
                    return isExecuteOnExecutorAvailable;
                }
            }
            isExecuteOnExecutorAvailable = false;
        }
        return isExecuteOnExecutorAvailable;
    }
    
    public long apiLevelUpdated() {
        if (apiLevelUpdated == null)
            apiLevelUpdated = prefs.getLong(Constants.API_LEVEL_UPDATED, Constants.API_LEVEL_UPDATED_DEFAULT);
        return apiLevelUpdated;
    }
    
    private void setApiLevelUpdated(long apiLevelUpdated) {
        put(Constants.APP_VERSION_CHECK_TIME, apiLevelUpdated);
        this.apiLevelUpdated = apiLevelUpdated;
    }
    
    public int apiLevel() {
        if (apiLevel == null)
            apiLevel = prefs.getInt(Constants.API_LEVEL, Constants.API_LEVEL_DEFAULT);
        return apiLevel;
    }
    
    public void setApiLevel(int apiLevel) {
        put(Constants.API_LEVEL, apiLevel);
        this.apiLevel = apiLevel;
        setApiLevelUpdated(System.currentTimeMillis());
    }
    
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
    
    public long getLastUpdateTime() {
        if (lastUpdateTime == null)
            lastUpdateTime = prefs.getLong(Constants.LAST_UPDATE_TIME, Constants.LAST_UPDATE_TIME_DEFAULT);
        return lastUpdateTime;
    }
    
    public void setLastUpdateTime(long lastUpdateTime) {
        put(Constants.LAST_UPDATE_TIME, lastUpdateTime);
        this.lastUpdateTime = lastUpdateTime;
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
    
    public void setLastVacuumDate() {
        long time = System.currentTimeMillis();
        put(Constants.LAST_VACUUM_DATE, time);
        this.lastVacuumDate = time;
    }
    
    public long lastVacuumDate() {
        if (lastVacuumDate == null)
            lastVacuumDate = prefs.getLong(Constants.LAST_VACUUM_DATE, Constants.LAST_VACUUM_DATE_DEFAULT);
        return lastVacuumDate;
    }
    
    private AsyncTask<Void, Void, Void> task;
    
    public long getFreshArticleMaxAge() {
        int ret = 48 * 60 * 60 * 1000; // Refreshed every two days only
        
        if (freshArticleMaxAge == null) {
            return ret;
        } else if (freshArticleMaxAge.equals("")) {
            
            // Only start task if none existing yet
            if (task == null) {
                task = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        freshArticleMaxAge = Data.getInstance().getPref("FRESH_ARTICLE_MAX_AGE");
                        return null;
                    }
                };
                if (isExecuteOnExecutorAvailable())
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                else
                    task.execute();
            }
            
        }
        
        try {
            ret = Integer.parseInt(freshArticleMaxAge) * 60 * 60 * 1000;
        } catch (Exception e) {
            return ret;
        }
        
        return ret;
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
    
    // ------------------------------
    // Call all registered instances of MenuActivity when caching is done
    private List<MenuActivity> callbacks = new ArrayList<MenuActivity>();
    
    public void notifyActivities() {
        synchronized (callbacks) {
            for (MenuActivity m : callbacks) {
                m.onCacheEnd();
            }
            // Why??
            // callbacks = new ArrayList<MenuActivity>();
        }
    }
    
    public void registerActivity(MenuActivity activity) {
        synchronized (callbacks) {
            callbacks.add(activity);
            
            // Reduce size to maximum of 3 activities
            if (callbacks.size() > 2) {
                List<MenuActivity> temp = new ArrayList<MenuActivity>();
                temp.addAll(callbacks.subList(callbacks.size() - 2, callbacks.size()));
                callbacks = temp;
            }
        }
    }
    
    public void unregisterActivity(MenuActivity activity) {
        synchronized (callbacks) {
            callbacks.remove(activity);
        }
    }
    
    /**
     * If provided "key" resembles a setting as declared in Constants.java the corresponding variable in this class will
     * be reset to null. Variable-Name ist built from the name of the field in Contants.java which holds the value from
     * "key" which was changed lately.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        
        for (Field field : Constants.class.getDeclaredFields()) {
            
            // No default-values
            if (field.getName().endsWith(Constants.APPENDED_DEFAULT))
                continue;
            
            // Only use public static fields
            if (Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers())) {
                
                try {
                    Object f = field.get(this);
                    if (!(f instanceof String))
                        continue;
                    
                    if (key.equals((String) f)) {
                        // reset variable, it will be re-read on next access
                        String fieldName = Constants.constant2Var(field.getName());
                        Controller.class.getDeclaredField(fieldName).set(this, null); // "Declared" so also private
                                                                                      // fields are returned
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
            }
            
        }
    }
    
}
