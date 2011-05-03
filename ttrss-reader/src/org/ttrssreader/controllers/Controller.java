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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.ttrssreader.R;
import org.ttrssreader.net.Connector;
import org.ttrssreader.net.JSONConnector;
import org.ttrssreader.net.JSONPOSTConnector;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.ImageCache;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

/**
 * Not entirely sure why this is called the "Controller". Actually, in terms of MVC, it isn't the controller. There
 * isn't one in here but it's called like that and I don't have a better name so we stay with it.
 */
public class Controller {
    
    public final static String JSON_END_URL = "api/";
    
    private boolean initialized = false;
    private Context context;
    private Connector ttrssConnector;
    private Connector ttrssPostConnector;
    private ImageCache imageCache;
    
    private static Controller instance = null;
    private SharedPreferences prefs = null;
    
    private String url = null;
    private Boolean trustAllSsl = null;
    private Boolean useKeystore = null;
    private String keystorePassword = null;
    
    private Boolean automaticMarkRead = null;
    private Boolean openUrlEmptyArticle = null;
    private Boolean useVolumeKeys = null;
    private Boolean vibrateOnLastArticle = null;
    private Boolean workOffline = null;
    
    private Boolean displayVirtuals = null;
    private Boolean useSwipe = null;
    private Boolean displayOnlyUnread = null;
    private Integer articleLimit = null;
    private Boolean displayArticleHeader = null;
    private Boolean invertSortArticleList = null;
    private Boolean invertSortFeedsCats = null;
    
    private Integer imageCacheSize = null;
    private Boolean imageCacheUnread = null;
    private Boolean articleCacheUnread = null;
    private Boolean splitGetRequests = null;
    private Boolean isDeleteDBScheduled = null;
    private Boolean isDeleteDBOnStartup = null;
    
    private Long lastUpdateTime = null;
    private String lastVersionRun = null;
    private Boolean newInstallation = false;
    private String freshArticleMaxAge = "";
    private Integer serverVersion = null;
    private Long serverVersionLastUpdate = null;
    
    public volatile Integer lastOpenedFeed = null;
    public volatile Integer lastOpenedArticle = null;
    
    // Article-View-Stuff
    public static String htmlHeader = "";
    public static int absHeight = -1;
    public static int absWidth = -1;
    public static int swipeHeight = -1;
    public static int swipeWidth = -1;
    public static int padding = -1;
    
    // Singleton
    private Controller() {
    }
    
    public static Controller getInstance() {
        synchronized (Controller.class) {
            if (instance == null || instance.prefs == null) {
                instance = new Controller();
            }
        }
        return instance;
    }
    
    public synchronized void checkAndInitializeController(final Context context) {
        this.context = context;
        
        if (!initialized) {
            initializeController();
            initialized = true;
        }
    }
    
    public synchronized void checkAndInitializeController(final Context context, boolean force) {
        this.initialized = false;
        checkAndInitializeController(context);
    }
    
    private synchronized void initializeController() {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        // Check for new installation
        if (!prefs.contains(Constants.URL) && !prefs.contains(Constants.LAST_VERSION_RUN)) {
            newInstallation = true;
        }
        
        url = prefs.getString(Constants.URL, "http://localhost/");
        if (!url.endsWith(JSON_END_URL)) {
            if (!url.endsWith("/")) {
                url += "/";
            }
            url += JSON_END_URL;
        }
        String userName = prefs.getString(Constants.USERNAME, Constants.EMPTY);
        String password = prefs.getString(Constants.PASSWORD, Constants.EMPTY);
        
        String httpUserName = Constants.EMPTY;
        String httpPassword = Constants.EMPTY;
        boolean useHttpAuth = prefs.getBoolean(Constants.USE_HTTP_AUTH, Constants.USE_HTTP_AUTH_DEFAULT);
        if (useHttpAuth) {
            httpUserName = prefs.getString(Constants.HTTP_USERNAME, Constants.EMPTY);
            httpPassword = prefs.getString(Constants.HTTP_PASSWORD, Constants.EMPTY);
        }
        
        trustAllSsl = prefs.getBoolean(Constants.TRUST_ALL_SSL, Constants.TRUST_ALL_SSL_DEFAULT);
        useKeystore = prefs.getBoolean(Constants.USE_KEYSTORE, Constants.USE_KEYSTORE_DEFAULT);
        keystorePassword = prefs.getString(Constants.KEYSTORE_PASSWORD, Constants.EMPTY);
        
        int version = getServerVersion();
        if (version >= 153) {
            Log.d(Utils.TAG, "Server-version seems to be above 1.5.3, using new JSONPOSTConnector.");
            ttrssPostConnector = new JSONPOSTConnector(url, userName, password, httpUserName, httpPassword);
            ttrssConnector = null;
        } else if (version <= 0) {
            // Silently use old connector, version is fetched in the background
            ttrssPostConnector = null;
            ttrssConnector = new JSONConnector(url, userName, password, httpUserName, httpPassword);
        } else {
            Log.d(Utils.TAG, "Server-version seems to be lower then 1.5.3, using old JSONConnector.");
            ttrssPostConnector = null;
            ttrssConnector = new JSONConnector(url, userName, password, httpUserName, httpPassword);
        }
        
        // Article-Prefetch-Stuff from Raw-Ressources and System
        htmlHeader = context.getResources().getString(R.string.INJECT_HTML_HEAD);
        
        // Initialize ImageCache
        getImageCache(context);
    }
    
    public static void initializeArticleViewStuff(Display display, DisplayMetrics metrics) {
        if (absHeight <= 0) {
            display.getMetrics(metrics);
            absHeight = metrics.heightPixels;
            absWidth = metrics.widthPixels;
            swipeHeight = (int) (absHeight * 0.25); // 25% height
            swipeWidth = (int) (absWidth * 0.5); // 50% width
            padding = (int) ((swipeHeight / 2) - (16 * metrics.density));
        }
    }
    
    // ******* USAGE-Options ****************************
    
    public String getUrl() {
        // Initialized inside initializeController();
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public Connector getConnector() {
        // Initialized inside initializeController();
        if (ttrssPostConnector != null) {
            return ttrssPostConnector;
        } else {
            return ttrssConnector;
        }
    }
    
    public ImageCache getImageCache(Context context) {
        if (imageCache == null) {
            imageCache = new ImageCache(2000);
            if (context == null || !imageCache.enableDiskCache()) {
                imageCache = null;
            }
        }
        return imageCache;
    }
    
    public boolean trustAllSsl() {
        // Initialized inside initializeController();
        return trustAllSsl;
    }
    
    public void setTrustAllSsl(boolean trustAllSsl) {
        put(Constants.TRUST_ALL_SSL, trustAllSsl);
        this.trustAllSsl = trustAllSsl;
    }
    
    public boolean useKeystore() {
        // Initialized inside initializeController();
        return useKeystore;
    }
    
    public void setUseKeystore(boolean useKeystore) {
        put(Constants.USE_KEYSTORE, useKeystore);
        this.useKeystore = useKeystore;
    }
    
    public String getKeystorePassword() {
        // Initialized inside initializeController();
        return keystorePassword;
    }
    
    public void setKeystorePassword(String keystorePassword) {
        put(Constants.KEYSTORE_PASSWORD, keystorePassword);
        this.keystorePassword = keystorePassword;
    }
    
    // USAGE
    
    public boolean automaticMarkRead() {
        if (automaticMarkRead == null)
            automaticMarkRead = prefs.getBoolean(Constants.AUTOMATIC_MARK_READ, Constants.AUTOMATIC_MARK_READ_DEFAULT);
        return automaticMarkRead;
    }
    
    public void setAutomaticMarkRead(boolean automaticMarkRead) {
        put(Constants.AUTOMATIC_MARK_READ, automaticMarkRead);
        this.automaticMarkRead = automaticMarkRead;
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
    
    public boolean displayVirtuals() {
        if (displayVirtuals == null)
            displayVirtuals = prefs.getBoolean(Constants.SHOW_VIRTUAL, Constants.SHOW_VIRTUAL_DEFAULT);
        return displayVirtuals;
    }
    
    public void setDisplayVirtuals(boolean displayVirtuals) {
        put(Constants.SHOW_VIRTUAL, displayVirtuals);
        this.displayVirtuals = displayVirtuals;
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
    
    public boolean displayOnlyUnread() {
        if (displayOnlyUnread == null)
            displayOnlyUnread = prefs.getBoolean(Constants.ONLY_UNREAD, Constants.ONLY_UNREAD_DEFAULT);
        return displayOnlyUnread;
    }
    
    public void setDisplayOnlyUnread(boolean displayOnlyUnread) {
        put(Constants.ONLY_UNREAD, displayOnlyUnread);
        this.displayOnlyUnread = displayOnlyUnread;
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
    
    public boolean invertSortArticleList() {
        if (invertSortArticleList == null)
            invertSortArticleList = prefs.getBoolean(Constants.INVERT_SORT_ARTICLELIST,
                    Constants.INVERT_SORT_ARTICLELIST_DEFAULT);
        return invertSortArticleList;
    }
    
    public void setInvertSortArticleList(boolean invertSortArticleList) {
        put(Constants.INVERT_SORT_ARTICLELIST, invertSortArticleList);
        this.invertSortArticleList = invertSortArticleList;
    }
    
    public boolean invertSortFeedsCats() {
        if (invertSortFeedsCats == null)
            invertSortFeedsCats = prefs.getBoolean(Constants.INVERT_SORT_FEEDSCATS,
                    Constants.INVERT_SORT_FEEDSCATS_DEFAULT);
        return invertSortFeedsCats;
    }
    
    public void setInvertSortFeedsCats(boolean invertSortFeedsCats) {
        put(Constants.INVERT_SORT_FEEDSCATS, invertSortFeedsCats);
        this.invertSortFeedsCats = invertSortFeedsCats;
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
    
    public boolean isArticleCacheUnread() {
        if (articleCacheUnread == null)
            articleCacheUnread = prefs.getBoolean(Constants.ARTICLE_CACHE_UNREAD,
                    Constants.ARTICLE_CACHE_UNREAD_DEFAULT);
        return articleCacheUnread;
    }
    
    public void setArticleCacheUnread(boolean articleCacheUnread) {
        put(Constants.ARTICLE_CACHE_UNREAD, articleCacheUnread);
        this.articleCacheUnread = articleCacheUnread;
    }
    
    public boolean splitGetRequests() {
        if (splitGetRequests == null)
            splitGetRequests = prefs.getBoolean(Constants.SPLIT_GET_REQUESTS, Constants.SPLIT_GET_REQUESTS_DEFAULT);
        return splitGetRequests;
    }
    
    public void setSplitGetRequests(boolean splitGetRequests) {
        put(Constants.SPLIT_GET_REQUESTS, splitGetRequests);
        this.splitGetRequests = splitGetRequests;
    }
    
    public boolean isDeleteDBScheduled() {
        if (isDeleteDBScheduled == null)
            isDeleteDBScheduled = prefs.getBoolean(Constants.DELETE_DB_SCHEDULED, Constants.DELETE_DB_SCHEDULED_DEFAULT);
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
            isDeleteDBOnStartup = prefs.getBoolean(Constants.DELETE_DB_ON_STARTUP, Constants.DELETE_DB_ON_STARTUP_DEFAULT);
        return isDeleteDBOnStartup;
    }
    
    public void setDeleteDBOnStartup(boolean isDeleteDBOnStartup) {
        put(Constants.DELETE_DB_ON_STARTUP, isDeleteDBOnStartup);
        this.isDeleteDBOnStartup = isDeleteDBOnStartup;
        setDeleteDBScheduled(isDeleteDBOnStartup);
    }
    
    // ******* INTERNAL Data ****************************
    
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
    
    private AsyncTask<Void, Void, Void> task;
    
    public long getFreshArticleMaxAge() {
        int ret = 24 * 60 * 60 * 1000;
        
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
    
    public void resetServerVersion() {
        serverVersion = -1;
        serverVersionLastUpdate = new Long(-1);
    }
    
    public int getServerVersion() {
        // Refresh only once ever 24 hours or if no serverVersion is stored in preferences
        long oldTime = (System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        
        if (serverVersion == null)
            serverVersion = prefs.getInt(Constants.SERVER_VERSION, Constants.SERVER_VERSION_DEFAULT);
        
        if (serverVersionLastUpdate == null)
            serverVersionLastUpdate = prefs.getLong(Constants.SERVER_VERSION_LAST_UPDATE,
                    Constants.SERVER_VERSION_LAST_UPDATE_DEFAULT);
        
        if (serverVersion < 0 || serverVersionLastUpdate < oldTime) {
            
            if (ttrssConnector != null || ttrssPostConnector != null) {
                
                // Load server-version in background so we got it on next run
                AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        Log.d(Utils.TAG, "Retrieving Server-Version information...");
                        serverVersion = Data.getInstance().getVersion();
                        serverVersionLastUpdate = System.currentTimeMillis();
                        put(Constants.SERVER_VERSION, serverVersion);
                        put(Constants.SERVER_VERSION_LAST_UPDATE, serverVersionLastUpdate);
                        Log.d(Utils.TAG, "Server is running v" + serverVersion);
                        return null;
                    }
                };
                task.execute();
                
            }
            
        }
        
        return serverVersion;
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
    
}
