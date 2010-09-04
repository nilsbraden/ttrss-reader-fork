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

package org.ttrssreader.controllers;

import org.ttrssreader.net.ITTRSSConnector;
import org.ttrssreader.net.TTRSSJsonConnector;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

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
        
        // Connection
        url = prefs.getString(Constants.CONNECTION_URL, "http://localhost/");
        if (!url.endsWith(JSON_END_URL)) {
            if (!url.endsWith("/")) {
                url += "/";
            }
            url += JSON_END_URL;
        }
        String userName = prefs.getString(Constants.CONNECTION_USERNAME, "");
        String password = prefs.getString(Constants.CONNECTION_PASSWORD, "");
        mTrustAllSsl = prefs.getBoolean(Constants.CONNECTION_TRUST_ALL_SSL, false);
        mUseKeystore = prefs.getBoolean(Constants.CONNECTION_USE_KEYSTORE, false);
        mKeystorePassword = prefs.getString(Constants.CONNECTION_KEYSTORE_PASSWORD, "");
        mTTRSSConnector = new TTRSSJsonConnector(url, userName, password);
        
        // Usage
        mAutomaticMarkRead = prefs.getBoolean(Constants.USAGE_AUTOMATIC_MARK_READ, true);
        mOpenUrlEmptyArticle = prefs.getBoolean(Constants.USAGE_OPEN_URL_EMPTY_ARTICLE, false);
        mUpdateUnreadOnStartup = prefs.getBoolean(Constants.USAGE_UPDATE_UNREAD_ON_STARTUP, false);
        mRefreshSubData = prefs.getBoolean(Constants.USAGE_REFRESH_SUB_DATA, false);
        mUseVolumeKeys = prefs.getBoolean(Constants.USAGE_USE_VOLUME_KEYS, true);
        mVibrateOnLastArticle = prefs.getBoolean(Constants.USAGE_VIBRATE_ON_LAST_ARTICLE, true);
        mWorkOffline = prefs.getBoolean(Constants.USAGE_WORK_OFFLINE, false);
        
        // Display
        mDisplayVirtuals = prefs.getBoolean(Constants.DISPLAY_SHOW_VIRTUAL, true);
        mDisplayUnreadInVirtualFeeds = prefs.getBoolean(Constants.DISPLAY_SHOW_VIRTUAL_UNREAD, false);
        mAlwaysFullRefresh = prefs.getBoolean(Constants.DISPLAY_ALWAYS_FULL_REFRESH, false);
        mUseSwipe = prefs.getBoolean(Constants.DISPLAY_USE_SWIPE, true);
        mDisplayOnlyUnread = prefs.getBoolean(Constants.DISPLAY_ONLY_UNREAD, false);
        try {
            mArticleLimit = Integer.parseInt(prefs.getString(Constants.DISPLAY_ARTICLE_LIMIT, "100"));
        } catch (ClassCastException e) {
            setArticleLimit(100);
            Log.e(Utils.TAG, "DISPLAY_ARTICLE_LIMIT was not an integer value, using default value: " + mArticleLimit);
        }
        
        // Internal Data
        try {
            mDatabaseVersion = Integer.parseInt(prefs.getString(Constants.DATABASE_VERSION, "0"));
        } catch (ClassCastException e) {
            setDatabaseVersion(0);
            Log.e(Utils.TAG, "DATABASE_VERSION was not an integer value");
        }
        try {
            mLastUpdateTime = Long.parseLong(prefs.getString(Constants.LAST_UPDATE_TIME, "0"));
        } catch (ClassCastException e) {
            setLastUpdateTime(new Long(0));
            Log.e(Utils.TAG, "LAST_UPDATE_TIME was not a valid time value");
        }
        
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
    
    public ITTRSSConnector getTTRSSConnector() {
        return mTTRSSConnector;
    }
    
    public boolean isTrustAllSsl() {
        return mTrustAllSsl;
    }
    
    public void setTrustAllSsl(boolean trustAllSsl) {
        put(Constants.CONNECTION_TRUST_ALL_SSL, trustAllSsl);
        this.mTrustAllSsl = trustAllSsl;
    }
    
    public boolean isUseKeystore() {
        return mUseKeystore;
    }
    
    public void setUseKeystore(boolean useKeystore) {
        put(Constants.CONNECTION_USE_KEYSTORE, useKeystore);
        this.mUseKeystore = useKeystore;
    }
    
    public String getKeystorePassword() {
        return mKeystorePassword;
    }
    
    public void setKeystorePassword(String keystorePassword) {
        put(Constants.CONNECTION_KEYSTORE_PASSWORD, keystorePassword);
        this.mKeystorePassword = keystorePassword;
    }
    
    public boolean isAutomaticMarkRead() {
        return mAutomaticMarkRead;
    }
    
    public void setAutomaticMarkRead(boolean automaticMarkRead) {
        put(Constants.USAGE_AUTOMATIC_MARK_READ, automaticMarkRead);
        this.mAutomaticMarkRead = automaticMarkRead;
    }
    
    public boolean isOpenUrlEmptyArticle() {
        return mOpenUrlEmptyArticle;
    }
    
    public void setOpenUrlEmptyArticle(boolean openUrlEmptyArticle) {
        put(Constants.USAGE_OPEN_URL_EMPTY_ARTICLE, openUrlEmptyArticle);
        this.mOpenUrlEmptyArticle = openUrlEmptyArticle;
    }
    
    public boolean isUpdateUnreadOnStartup() {
        return mUpdateUnreadOnStartup;
    }
    
    public void setUpdateUnreadOnStartup(boolean mUpdateUnreadOnStartup) {
        put(Constants.USAGE_UPDATE_UNREAD_ON_STARTUP, mUpdateUnreadOnStartup);
        this.mUpdateUnreadOnStartup = mUpdateUnreadOnStartup;
    }
    
    public boolean isRefreshSubData() {
        return mRefreshSubData;
    }
    
    public void setRefreshSubData(boolean refreshSubData) {
        put(Constants.USAGE_REFRESH_SUB_DATA, refreshSubData);
        this.mRefreshSubData = refreshSubData;
    }
    
    public boolean isUseVolumeKeys() {
        return mUseVolumeKeys;
    }
    
    public void setUseVolumeKeys(boolean useVolumeKeys) {
        put(Constants.USAGE_USE_VOLUME_KEYS, useVolumeKeys);
        this.mUseVolumeKeys = useVolumeKeys;
    }
    
    public boolean isVibrateOnLastArticle() {
        return mVibrateOnLastArticle;
    }
    
    public void setVibrateOnLastArticle(boolean vibrateOnLastArticle) {
        put(Constants.USAGE_VIBRATE_ON_LAST_ARTICLE, vibrateOnLastArticle);
        this.mVibrateOnLastArticle = vibrateOnLastArticle;
    }
    
    public boolean isWorkOffline() {
        return mWorkOffline;
    }
    
    public void setWorkOffline(boolean workOffline) {
        put(Constants.USAGE_WORK_OFFLINE, workOffline);
        this.mWorkOffline = workOffline;
    }
    
    // ******* DISPLAY-Options ****************************
    
    public boolean isDisplayVirtuals() {
        return mDisplayVirtuals;
    }
    
    public void setDisplayVirtuals(boolean displayVirtuals) {
        put(Constants.DISPLAY_SHOW_VIRTUAL, displayVirtuals);
        this.mDisplayVirtuals = displayVirtuals;
    }
    
    public boolean isDisplayUnreadInVirtualFeeds() {
        return mDisplayUnreadInVirtualFeeds;
    }
    
    public void setDisplayUnreadInVirtualFeeds(boolean displayUnreadInVirtualFeeds) {
        put(Constants.DISPLAY_SHOW_VIRTUAL_UNREAD, displayUnreadInVirtualFeeds);
        this.mDisplayUnreadInVirtualFeeds = displayUnreadInVirtualFeeds;
    }
    
    public boolean isAlwaysFullRefresh() {
        return mAlwaysFullRefresh;
    }
    
    public void setAlwaysFullRefresh(boolean alwaysFullRefresh) {
        put(Constants.DISPLAY_ALWAYS_FULL_REFRESH, alwaysFullRefresh);
        this.mAlwaysFullRefresh = alwaysFullRefresh;
    }
    
    public boolean isUseSwipe() {
        return mUseSwipe;
    }
    
    public void setUseSwipe(boolean useSwipe) {
        put(Constants.DISPLAY_USE_SWIPE, useSwipe);
        this.mUseSwipe = useSwipe;
    }
    
    public boolean isDisplayOnlyUnread() {
        return mDisplayOnlyUnread;
    }
    
    public void setDisplayOnlyUnread(boolean displayOnlyUnread) {
        put(Constants.DISPLAY_ONLY_UNREAD, displayOnlyUnread);
        this.mDisplayOnlyUnread = displayOnlyUnread;
    }
    
    public int getArticleLimit() {
        return mArticleLimit;
    }
    
    public void setArticleLimit(int articleLimit) {
        put(Constants.DISPLAY_ARTICLE_LIMIT, articleLimit + "");
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
        } else if (o instanceof Boolean) {
            boolean bool = (Boolean) o;
            editor.putBoolean(constant, bool);
        }
        editor.commit();
    }
    
}
