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
import org.ttrssreader.net.Connector;
import org.ttrssreader.net.JSONConnector;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.ImageCache;
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
    
    private boolean initialized = false;
    private Context context;
    private Connector ttrssConnector;
    private ImageCache imageCache;
    
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
    
    private Boolean automaticMarkRead = null;
    private Boolean openUrlEmptyArticle = null;
    private Boolean useVolumeKeys = null;
    private Boolean vibrateOnLastArticle = null;
    private Boolean workOffline = null;
    
    private Boolean showVirtual = null;
    private Boolean useSwipe = null;
    private Boolean onlyUnread = null;
    private Integer articleLimit = null;
    private Boolean displayArticleHeader = null;
    private Boolean invertSortArticlelist = null;
    private Boolean invertSortFeedscats = null;
    private Boolean alignFlushLeft = null;
    private Boolean dateTimeSystem = null;
    private String dateString = null;
    private String timeString = null;
    
    private Integer imageCacheSize = null;
    private Boolean imageCacheUnread = null;
    private Boolean articleCacheUnread = null;
    private Boolean splitGetRequests = null;
    private Boolean isVacuumDBScheduled = null;
    private Boolean isDeleteDBScheduled = null;
    private Boolean isDeleteDBOnStartup = null;
    private Boolean cacheOnStartup = null;
    private Boolean cacheImagesOnStartup = null;
    private Boolean logSensitiveData = null;
    
    private Long lastUpdateTime = null;
    private String lastVersionRun = null;
    private Boolean newInstallation = false;
    private String freshArticleMaxAge = "";
    private Long lastVacuumDate = null;
    
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
    
    public static synchronized void checkAndInitializeController(final Context context, boolean force_dummy_parameter) {
        Controller.instance = null;
        Controller.getInstance().checkAndInitializeController(context);
    }
    
    private synchronized void initializeController() {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        // Check for new installation
        if (!prefs.contains(Constants.URL) && !prefs.contains(Constants.LAST_VERSION_RUN)) {
            newInstallation = true;
        }
        
        ttrssConnector = new JSONConnector();
        
        // Article-Prefetch-Stuff from Raw-Ressources and System
        htmlHeader = context.getResources().getString(R.string.INJECT_HTML_HEAD);
        
        // Replace marker with the requested layout, align:left or justified
        String alignMarker = "TEXT_ALIGN_MARKER";
        String replacement = "";
        if (alignFlushLeft()) {
            replacement = context.getResources().getString(R.string.ALIGN_LEFT);
            htmlHeader = htmlHeader.replace(alignMarker, replacement);
        } else {
            replacement = context.getResources().getString(R.string.ALIGN_JUSTIFY);
            htmlHeader = htmlHeader.replace(alignMarker, replacement);
        }
        
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
            e.printStackTrace();
        }
        return null;
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
        if (imageCache == null) {
            imageCache = new ImageCache(2000);
            if (context == null || !imageCache.enableDiskCache()) {
                imageCache = null;
            }
        }
        return imageCache;
    }
    
    public String getKeystorePassword() {
        // Initialized inside initializeController();
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
    
    public boolean cacheOnStartup() {
        if (cacheOnStartup == null)
            cacheOnStartup = prefs.getBoolean(Constants.CACHE_ON_STARTUP, Constants.CACHE_ON_STARTUP_DEFAULT);
        return cacheOnStartup;
    }
    
    public void setCacheOnStartup(boolean cacheOnStartup) {
        put(Constants.CACHE_ON_STARTUP, cacheOnStartup);
        this.cacheOnStartup = cacheOnStartup;
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
    
    // logSensitiveData
    
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
            callbacks = new ArrayList<MenuActivity>();
        }
    }
    
    public void registerActivity(MenuActivity activity) {
        synchronized (callbacks) {
            callbacks.add(activity);
            
            // Reduce size to maximum of 3 activities
            if (callbacks.size() > 3) {
                List<MenuActivity> temp = new ArrayList<MenuActivity>();
                temp.addAll(callbacks.subList(callbacks.size() - 3, callbacks.size()));
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
