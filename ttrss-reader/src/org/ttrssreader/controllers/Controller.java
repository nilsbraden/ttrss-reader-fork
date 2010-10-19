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

import org.ttrssreader.net.ITTRSSConnector;
import org.ttrssreader.net.TTRSSJsonConnector;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * 
 * Not entirely sure why this is called the "Controller", actually, in terms of MVC it isn't the controller. There isn't
 * one in here. But it's called like that and I don't have a better name so we stay with it.
 * 
 */
public class Controller {
    
    public final static String JSON_END_URL = "api/";
    
    private boolean mIsControllerInitialized = false;
    private ITTRSSConnector mTTRSSConnector;
    
    private static final String mutex = "";
    private static Controller mInstance = null;
    private SharedPreferences prefs = null;
    
    private String url = "";
    private boolean mTrustAllSsl;
    private boolean mUseKeystore;
    private String mKeystorePassword;
    
    private boolean mAutomaticMarkRead;
    private boolean mOpenUrlEmptyArticle;
    private boolean mUpdateUnreadOnStartup;
    private boolean mRefreshSubData;
    private boolean mUseVolumeKeys;
    private boolean mVibrateOnLastArticle;
    private boolean mWorkOffline;
    
    private boolean mDisplayVirtuals;
    private boolean mDisplayUnreadInVirtualFeeds;
    private boolean mAlwaysFullRefresh;
    private boolean mUseSwipe;
    private boolean mDisplayOnlyUnread;
    private int mArticleLimit;
    
    private int mDatabaseVersion;
    private long mLastUpdateTime;
    
    public static Controller getInstance() {
        synchronized (mutex) {
            if (mInstance == null) {
                mInstance = new Controller();
            }
            return mInstance;
        }
    }
    
    public synchronized void initializeController(Context context) {
        
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        // Connection @formatter:off
        url = prefs.getString(Constants.URL, "http://localhost/");
        if (!url.endsWith(JSON_END_URL)) {
            if (!url.endsWith("/")) {
                url += "/";
            }
            url += JSON_END_URL;
        }
        String userName = prefs.getString(Constants.USERNAME, Constants.USERNAME_DEFAULT);
        String password = prefs.getString(Constants.PASSWORD, Constants.PASSWORD_DEFAULT);
        mTrustAllSsl = prefs.getBoolean(Constants.TRUST_ALL_SSL, Constants.TRUST_ALL_SSL_DEFAULT);
        mUseKeystore = prefs.getBoolean(Constants.USE_KEYSTORE, Constants.USE_KEYSTORE_DEFAULT);
        mKeystorePassword = prefs.getString(Constants.KEYSTORE_PASSWORD, Constants.KEYSTORE_PASSWORD_DEFAULT);
        mTTRSSConnector = new TTRSSJsonConnector(url, userName, password);
        
        // Usage
        mAutomaticMarkRead = prefs.getBoolean(Constants.AUTOMATIC_MARK_READ, Constants.AUTOMATIC_MARK_READ_DEFAULT);
        mOpenUrlEmptyArticle = prefs.getBoolean(Constants.OPEN_URL_EMPTY_ARTICLE, Constants.OPEN_URL_EMPTY_ARTICLE_DEFAULT);
        mUpdateUnreadOnStartup = prefs.getBoolean(Constants.UPDATE_UNREAD_ON_STARTUP, Constants.UPDATE_UNREAD_ON_STARTUP_DEFAULT);
        mRefreshSubData = prefs.getBoolean(Constants.REFRESH_SUB_DATA, Constants.REFRESH_SUB_DATA_DEFAULT);
        mUseVolumeKeys = prefs.getBoolean(Constants.USE_VOLUME_KEYS, Constants.USE_VOLUME_KEYS_DEFAULT);
        mVibrateOnLastArticle = prefs.getBoolean(Constants.VIBRATE_ON_LAST_ARTICLE, Constants.VIBRATE_ON_LAST_ARTICLE_DEFAULT);
        mWorkOffline = prefs.getBoolean(Constants.WORK_OFFLINE, Constants.WORK_OFFLINE_DEFAULT);
        
        // Display
        mDisplayVirtuals = prefs.getBoolean(Constants.SHOW_VIRTUAL, Constants.SHOW_VIRTUAL_DEFAULT);
        mDisplayUnreadInVirtualFeeds = prefs.getBoolean(Constants.SHOW_VIRTUAL_UNREAD, Constants.SHOW_VIRTUAL_UNREAD_DEFAULT);
        mAlwaysFullRefresh = prefs.getBoolean(Constants.ALWAYS_FULL_REFRESH, Constants.ALWAYS_FULL_REFRESH_DEFAULT);
        mUseSwipe = prefs.getBoolean(Constants.USE_SWIPE, Constants.USE_SWIPE_DEFAULT);
        mDisplayOnlyUnread = prefs.getBoolean(Constants.ONLY_UNREAD, Constants.ONLY_UNREAD_DEFAULT);
        try {
            mArticleLimit = Integer.parseInt(prefs.getString(Constants.ARTICLE_LIMIT, Constants.ARTICLE_LIMIT_DEFAULT));
        } catch (ClassCastException e) {
            setArticleLimit(100);
            Log.e(Utils.TAG, "DISPLAY_ARTICLE_LIMIT was not an integer value, using default value: " + mArticleLimit);
        }
        
        // Internal Data
        try {
            mDatabaseVersion = Integer.parseInt(prefs.getString(Constants.DATABASE_VERSION, Constants.DATABASE_VERSION_DEFAULT));
        } catch (ClassCastException e) {
            setDatabaseVersion(0);
            Log.e(Utils.TAG, "DATABASE_VERSION was not an integer value");
        }
        try {
            mLastUpdateTime = Long.parseLong(prefs.getString(Constants.LAST_UPDATE_TIME, Constants.LAST_UPDATE_TIME_DEFAULT));
        } catch (ClassCastException e) {
            setLastUpdateTime(new Long(0));
            Log.e(Utils.TAG, "LAST_UPDATE_TIME was not a valid time value");
        }
        // @formatter:on
        
    }
    
    public synchronized void checkAndInitializeController(final Context context) {
        if (!mIsControllerInitialized) {
            initializeController(context);
            mIsControllerInitialized = true;
        }
    }
    
    // **** Getter / Setter **********
    
    // ******* USAGE-Options ****************************
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public ITTRSSConnector getConnector() {
        return mTTRSSConnector;
    }
    
    public boolean isTrustAllSsl() {
        return mTrustAllSsl;
    }
    
    public void setTrustAllSsl(boolean trustAllSsl) {
        put(Constants.TRUST_ALL_SSL, trustAllSsl);
        this.mTrustAllSsl = trustAllSsl;
    }
    
    public boolean isUseKeystore() {
        return mUseKeystore;
    }
    
    public void setUseKeystore(boolean useKeystore) {
        put(Constants.USE_KEYSTORE, useKeystore);
        this.mUseKeystore = useKeystore;
    }
    
    public String getKeystorePassword() {
        return mKeystorePassword;
    }
    
    public void setKeystorePassword(String keystorePassword) {
        put(Constants.KEYSTORE_PASSWORD, keystorePassword);
        this.mKeystorePassword = keystorePassword;
    }
    
    public boolean isAutomaticMarkRead() {
        return mAutomaticMarkRead;
    }
    
    public void setAutomaticMarkRead(boolean automaticMarkRead) {
        put(Constants.AUTOMATIC_MARK_READ, automaticMarkRead);
        this.mAutomaticMarkRead = automaticMarkRead;
    }
    
    public boolean isOpenUrlEmptyArticle() {
        return mOpenUrlEmptyArticle;
    }
    
    public void setOpenUrlEmptyArticle(boolean openUrlEmptyArticle) {
        put(Constants.OPEN_URL_EMPTY_ARTICLE, openUrlEmptyArticle);
        this.mOpenUrlEmptyArticle = openUrlEmptyArticle;
    }
    
    public boolean isUpdateUnreadOnStartup() {
        return mUpdateUnreadOnStartup;
    }
    
    public void setUpdateUnreadOnStartup(boolean mUpdateUnreadOnStartup) {
        put(Constants.UPDATE_UNREAD_ON_STARTUP, mUpdateUnreadOnStartup);
        this.mUpdateUnreadOnStartup = mUpdateUnreadOnStartup;
    }
    
    public boolean isRefreshSubData() {
        return mRefreshSubData;
    }
    
    public void setRefreshSubData(boolean refreshSubData) {
        put(Constants.REFRESH_SUB_DATA, refreshSubData);
        this.mRefreshSubData = refreshSubData;
    }
    
    public boolean isUseVolumeKeys() {
        return mUseVolumeKeys;
    }
    
    public void setUseVolumeKeys(boolean useVolumeKeys) {
        put(Constants.USE_VOLUME_KEYS, useVolumeKeys);
        this.mUseVolumeKeys = useVolumeKeys;
    }
    
    public boolean isVibrateOnLastArticle() {
        return mVibrateOnLastArticle;
    }
    
    public void setVibrateOnLastArticle(boolean vibrateOnLastArticle) {
        put(Constants.VIBRATE_ON_LAST_ARTICLE, vibrateOnLastArticle);
        this.mVibrateOnLastArticle = vibrateOnLastArticle;
    }
    
    public boolean isWorkOffline() {
        return mWorkOffline;
    }
    
    public void setWorkOffline(boolean workOffline) {
        put(Constants.WORK_OFFLINE, workOffline);
        this.mWorkOffline = workOffline;
    }
    
    // ******* DISPLAY-Options ****************************
    
    public boolean isDisplayVirtuals() {
        return mDisplayVirtuals;
    }
    
    public void setDisplayVirtuals(boolean displayVirtuals) {
        put(Constants.SHOW_VIRTUAL, displayVirtuals);
        this.mDisplayVirtuals = displayVirtuals;
    }
    
    public boolean isDisplayUnreadInVirtualFeeds() {
        return mDisplayUnreadInVirtualFeeds;
    }
    
    public void setDisplayUnreadInVirtualFeeds(boolean displayUnreadInVirtualFeeds) {
        put(Constants.SHOW_VIRTUAL_UNREAD, displayUnreadInVirtualFeeds);
        this.mDisplayUnreadInVirtualFeeds = displayUnreadInVirtualFeeds;
    }
    
    public boolean isAlwaysFullRefresh() {
        return mAlwaysFullRefresh;
    }
    
    public void setAlwaysFullRefresh(boolean alwaysFullRefresh) {
        put(Constants.ALWAYS_FULL_REFRESH, alwaysFullRefresh);
        this.mAlwaysFullRefresh = alwaysFullRefresh;
    }
    
    public boolean isUseSwipe() {
        return mUseSwipe;
    }
    
    public void setUseSwipe(boolean useSwipe) {
        put(Constants.USE_SWIPE, useSwipe);
        this.mUseSwipe = useSwipe;
    }
    
    public boolean isDisplayOnlyUnread() {
        return mDisplayOnlyUnread;
    }
    
    public void setDisplayOnlyUnread(boolean displayOnlyUnread) {
        put(Constants.ONLY_UNREAD, displayOnlyUnread);
        this.mDisplayOnlyUnread = displayOnlyUnread;
    }
    
    public int getArticleLimit() {
        return mArticleLimit;
    }
    
    public void setArticleLimit(int articleLimit) {
        put(Constants.ARTICLE_LIMIT, articleLimit + "");
        this.mArticleLimit = articleLimit;
    }
    
    // ******* Internal Data ****************************
    
    public long getLastUpdateTime() {
        return mLastUpdateTime;
    }
    
    public void setLastUpdateTime(long lastUpdateTime) {
        put(Constants.LAST_UPDATE_TIME, lastUpdateTime + "");
        this.mLastUpdateTime = lastUpdateTime;
    }
    
    public int getDatabaseVersion() {
        return mDatabaseVersion;
    }
    
    public void setDatabaseVersion(int databaseVersion) {
        put(Constants.DATABASE_VERSION, databaseVersion);
        this.mDatabaseVersion = databaseVersion;
    }
    
    /*
     * Generic method to insert values into the preferences store
     */
    public void put(String constant, Object o) {
        SharedPreferences.Editor editor = prefs.edit();
        if (o instanceof String) {
            String string = (String) o;
            editor.putString(constant, string);
        } else if (o instanceof Integer) {
            int integer = (Integer) o;
            editor.putString(constant, integer + "");
        } else if (o instanceof Boolean) {
            boolean bool = (Boolean) o;
            editor.putBoolean(constant, bool);
        }
        editor.commit();
    }
    
}
