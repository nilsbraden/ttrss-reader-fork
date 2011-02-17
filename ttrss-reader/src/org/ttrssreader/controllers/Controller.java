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

import org.ttrssreader.net.TTRSSJsonConnector;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.DonationHelper;
import org.ttrssreader.utils.ImageCache;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Not entirely sure why this is called the "Controller". Actually, in terms of MVC, it isn't the controller. There
 * isn't one in here but it's called like that and I don't have a better name so we stay with it.
 */
public class Controller {
    
    public final static String JSON_END_URL = "api/";
    
    private boolean initialized = false;
    private TTRSSJsonConnector ttrssConnector;
    private ImageCache imageCache;
    
    private static final Integer mutex = 0;
    private static Controller instance = null;
    private SharedPreferences prefs = null;
    
    private String url = "";
    private boolean trustAllSsl;
    private boolean useKeystore;
    private String keystorePassword;
    private boolean donator;
    private String donatorMail;
    
    private boolean automaticMarkRead;
    private boolean openUrlEmptyArticle;
    private boolean useVolumeKeys;
    private boolean vibrateOnLastArticle;
    private boolean workOffline;
    
    private boolean displayVirtuals;
    private boolean useSwipe;
    private boolean displayOnlyUnread;
    private int articleLimit;
    
    private int imageCacheSize;
    private boolean imageCacheUnread;
    private boolean articleCacheUnread;
    private boolean splitGetRequests;
    
    private long lastUpdateTime;
    private String lastVersionRun;
    private boolean newInstallation = false;
    private String freshArticleMaxAge;
    
    public static Controller getInstance() {
        if (instance == null) {
            synchronized (mutex) {
                if (instance == null) {
                    instance = new Controller();
                }
            }
        }
        return instance;
    }
    
    public synchronized void initializeController(Context context) {
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
        
        String httpUserName = "";
        String httpPassword = "";
        boolean useHttpAuth = prefs.getBoolean(Constants.USE_HTTP_AUTH, Constants.USE_HTTP_AUTH_DEFAULT);
        if (useHttpAuth) {
            httpUserName = prefs.getString(Constants.HTTP_USERNAME, Constants.EMPTY);
            httpPassword = prefs.getString(Constants.HTTP_PASSWORD, Constants.EMPTY);
        }
        
        trustAllSsl = prefs.getBoolean(Constants.TRUST_ALL_SSL, Constants.TRUST_ALL_SSL_DEFAULT);
        useKeystore = prefs.getBoolean(Constants.USE_KEYSTORE, Constants.USE_KEYSTORE_DEFAULT);
        keystorePassword = prefs.getString(Constants.KEYSTORE_PASSWORD, Constants.EMPTY);
        ttrssConnector = new TTRSSJsonConnector(url, userName, password, httpUserName, httpPassword);
        
        // Donator
        donator = prefs.getBoolean(Constants.DONATOR, Constants.DONATOR_DEFAULT);
        donatorMail = prefs.getString(Constants.DONATOR_MAIL, Constants.DONATOR_MAIL_DEFAULT);
        
        // Check donation-status
        if (!donator && !donatorMail.equals(Constants.DONATOR_MAIL_DEFAULT)) {
            if (DonationHelper.checkDonationStatus(context, donatorMail)) {
                setDonator(true);
            }
        }
        
        // Usage
        automaticMarkRead = prefs.getBoolean(Constants.AUTOMATIC_MARK_READ, Constants.AUTOMATIC_MARK_READ_DEFAULT);
        openUrlEmptyArticle = prefs.getBoolean(Constants.OPEN_URL_EMPTY_ARTICLE,
                Constants.OPEN_URL_EMPTY_ARTICLE_DEFAULT);
        useVolumeKeys = prefs.getBoolean(Constants.USE_VOLUME_KEYS, Constants.USE_VOLUME_KEYS_DEFAULT);
        vibrateOnLastArticle = prefs.getBoolean(Constants.VIBRATE_ON_LAST_ARTICLE,
                Constants.VIBRATE_ON_LAST_ARTICLE_DEFAULT);
        workOffline = prefs.getBoolean(Constants.WORK_OFFLINE, Constants.WORK_OFFLINE_DEFAULT);
        
        // Display
        displayVirtuals = prefs.getBoolean(Constants.SHOW_VIRTUAL, Constants.SHOW_VIRTUAL_DEFAULT);
        useSwipe = prefs.getBoolean(Constants.USE_SWIPE, Constants.USE_SWIPE_DEFAULT);
        displayOnlyUnread = prefs.getBoolean(Constants.ONLY_UNREAD, Constants.ONLY_UNREAD_DEFAULT);
        articleLimit = prefs.getInt(Constants.ARTICLE_LIMIT, Constants.ARTICLE_LIMIT_DEFAULT);
        
        // System
        imageCacheSize = prefs.getInt(Constants.IMAGE_CACHE_SIZE, Constants.IMAGE_CACHE_SIZE_DEFAULT);
        imageCacheUnread = prefs.getBoolean(Constants.IMAGE_CACHE_UNREAD, Constants.IMAGE_CACHE_UNREAD_DEFAULT);
        articleCacheUnread = prefs.getBoolean(Constants.ARTICLE_CACHE_UNREAD, Constants.ARTICLE_CACHE_UNREAD_DEFAULT);
        splitGetRequests = prefs.getBoolean(Constants.SPLIT_GET_REQUESTS, Constants.SPLIT_GET_REQUESTS_DEFAULT);
        
        lastUpdateTime = prefs.getLong(Constants.LAST_UPDATE_TIME, Constants.LAST_UPDATE_TIME_DEFAULT);
        lastVersionRun = prefs.getString(Constants.LAST_VERSION_RUN, Constants.LAST_VERSION_RUN_DEFAULT);
        
        // Initialize ImageCache
        getImageCache(context);
    }
    
    public synchronized void checkAndInitializeController(final Context context) {
        if (!initialized) {
            initializeController(context);
            initialized = true;
        }
    }
    
    // ******* USAGE-Options ****************************
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public TTRSSJsonConnector getConnector() {
        return ttrssConnector;
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
        return trustAllSsl;
    }
    
    public void setTrustAllSsl(boolean trustAllSsl) {
        put(Constants.TRUST_ALL_SSL, trustAllSsl);
        this.trustAllSsl = trustAllSsl;
    }
    
    public boolean useKeystore() {
        return useKeystore;
    }
    
    public void setUseKeystore(boolean useKeystore) {
        put(Constants.USE_KEYSTORE, useKeystore);
        this.useKeystore = useKeystore;
    }
    
    public String getKeystorePassword() {
        return keystorePassword;
    }
    
    public void setKeystorePassword(String keystorePassword) {
        put(Constants.KEYSTORE_PASSWORD, keystorePassword);
        this.keystorePassword = keystorePassword;
    }
    
    public boolean isDonator() {
        return donator;
    }
    
    public void setDonator(boolean donator) {
        put(Constants.DONATOR, donator);
        this.donator = donator;
    }
    
    public String getDonatorMail() {
        return donatorMail;
    }
    
    public void setDonatorMail(String donatorMail) {
        put(Constants.DONATOR_MAIL, donatorMail);
        this.donatorMail = donatorMail;
    }
    
    public boolean automaticMarkRead() {
        return automaticMarkRead;
    }
    
    public void setAutomaticMarkRead(boolean automaticMarkRead) {
        put(Constants.AUTOMATIC_MARK_READ, automaticMarkRead);
        this.automaticMarkRead = automaticMarkRead;
    }
    
    public boolean openUrlEmptyArticle() {
        return openUrlEmptyArticle;
    }
    
    public void setOpenUrlEmptyArticle(boolean openUrlEmptyArticle) {
        put(Constants.OPEN_URL_EMPTY_ARTICLE, openUrlEmptyArticle);
        this.openUrlEmptyArticle = openUrlEmptyArticle;
    }
    
    public boolean useVolumeKeys() {
        return useVolumeKeys;
    }
    
    public void setUseVolumeKeys(boolean useVolumeKeys) {
        put(Constants.USE_VOLUME_KEYS, useVolumeKeys);
        this.useVolumeKeys = useVolumeKeys;
    }
    
    public boolean vibrateOnLastArticle() {
        return vibrateOnLastArticle;
    }
    
    public void setVibrateOnLastArticle(boolean vibrateOnLastArticle) {
        put(Constants.VIBRATE_ON_LAST_ARTICLE, vibrateOnLastArticle);
        this.vibrateOnLastArticle = vibrateOnLastArticle;
    }
    
    public boolean workOffline() {
        return workOffline;
    }
    
    public void setWorkOffline(boolean workOffline) {
        put(Constants.WORK_OFFLINE, workOffline);
        this.workOffline = workOffline;
    }
    
    // ******* DISPLAY-Options ****************************
    
    public boolean displayVirtuals() {
        return displayVirtuals;
    }
    
    public void setDisplayVirtuals(boolean displayVirtuals) {
        put(Constants.SHOW_VIRTUAL, displayVirtuals);
        this.displayVirtuals = displayVirtuals;
    }
    
    public boolean useSwipe() {
        return useSwipe;
    }
    
    public void setUseSwipe(boolean useSwipe) {
        put(Constants.USE_SWIPE, useSwipe);
        this.useSwipe = useSwipe;
    }
    
    public boolean displayOnlyUnread() {
        return displayOnlyUnread;
    }
    
    public void setDisplayOnlyUnread(boolean displayOnlyUnread) {
        put(Constants.ONLY_UNREAD, displayOnlyUnread);
        this.displayOnlyUnread = displayOnlyUnread;
    }
    
    public int getArticleLimit() {
        return articleLimit;
    }
    
    public void setArticleLimit(int articleLimit) {
        put(Constants.ARTICLE_LIMIT, articleLimit);
        this.articleLimit = articleLimit;
    }
    
    public int getImageCacheSize() {
        return imageCacheSize;
    }
    
    public void setImageCacheSize(int imageCacheSize) {
        put(Constants.IMAGE_CACHE_SIZE, imageCacheSize);
        this.imageCacheSize = imageCacheSize;
    }
    
    public boolean isImageCacheUnread() {
        return imageCacheUnread;
    }
    
    public void setImageCacheUnread(boolean imageCacheUnread) {
        put(Constants.IMAGE_CACHE_UNREAD, imageCacheUnread);
        this.imageCacheUnread = imageCacheUnread;
    }
    
    public boolean isArticleCacheUnread() {
        return articleCacheUnread;
    }
    
    public void setArticleCacheUnread(boolean articleCacheUnread) {
        put(Constants.ARTICLE_CACHE_UNREAD, articleCacheUnread);
        this.articleCacheUnread = articleCacheUnread;
    }
    
    public boolean splitGetRequests() {
        return splitGetRequests;
    }
    
    public void setSplitGetRequests(boolean splitGetRequests) {
        put(Constants.SPLIT_GET_REQUESTS, splitGetRequests);
        this.splitGetRequests = splitGetRequests;
    }
    
    // ******* INTERNAL Data ****************************
    
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    public void setLastUpdateTime(long lastUpdateTime) {
        put(Constants.LAST_UPDATE_TIME, lastUpdateTime);
        this.lastUpdateTime = lastUpdateTime;
    }
    
    public String getLastVersionRun() {
        return lastVersionRun;
    }
    
    public void setLastVersionRun(String lastVersionRun) {
        put(Constants.LAST_VERSION_RUN, lastVersionRun);
        this.lastVersionRun = lastVersionRun;
    }
    
    public boolean newInstallation() {
        return newInstallation;
    }
    
    public long getFreshArticleMaxAge() {
        int ret = 24 * 60 * 60 * 1000;
        if (freshArticleMaxAge == null) {
            return ret;
        } else if (freshArticleMaxAge.equals("")) {
            freshArticleMaxAge = Data.getInstance().getPref("FRESH_ARTICLE_MAX_AGE");
        }
        
        try {
            ret = Integer.parseInt(freshArticleMaxAge) * 60 * 60 * 1000;
        } catch (Exception e) {
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
        editor.commit();
    }
    
}
