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
	private String url = "";

	private static final String mutex = "";
	private static Controller mInstance = null;
	private SharedPreferences prefs = null;
	
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
	
	private Controller() {
	}
	
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
		
		// Login
		url = prefs.getString(Constants.CONNECTION_URL, "http://localhost/");
		if (!url.endsWith(JSON_END_URL)) {
			if (!url.endsWith("/")) {
				url += "/";
			}
			url += JSON_END_URL;
		}
		String userName = prefs.getString(Constants.CONNECTION_USERNAME, "");
		String password = prefs.getString(Constants.CONNECTION_PASSWORD, "");
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
	
	public boolean isAutomaticMarkRead() {
		return mAutomaticMarkRead;
	}
	
	public void setAutomaticMarkRead(boolean automaticMarkRead) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(Constants.USAGE_AUTOMATIC_MARK_READ, automaticMarkRead);
		editor.commit();
		this.mAutomaticMarkRead = automaticMarkRead;
	}
	
	public boolean isOpenUrlEmptyArticle() {
		return mOpenUrlEmptyArticle;
	}
	
	public void setOpenUrlEmptyArticle(boolean openUrlEmptyArticle) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(Constants.USAGE_OPEN_URL_EMPTY_ARTICLE, openUrlEmptyArticle);
		editor.commit();
		this.mOpenUrlEmptyArticle = openUrlEmptyArticle;
	}
	
	public boolean isUpdateUnreadOnStartup() {
		return mUpdateUnreadOnStartup;
	}
	
	public void setUpdateUnreadOnStartup(boolean mUpdateUnreadOnStartup) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(Constants.USAGE_UPDATE_UNREAD_ON_STARTUP, mUpdateUnreadOnStartup);
		editor.commit();
		this.mUpdateUnreadOnStartup = mUpdateUnreadOnStartup;
	}
	
	public boolean isRefreshSubData() {
		return mRefreshSubData;
	}
	
	public void setRefreshSubData(boolean refreshSubData) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(Constants.USAGE_REFRESH_SUB_DATA, refreshSubData);
		editor.commit();
		this.mRefreshSubData = refreshSubData;
	}
	
	public boolean isUseVolumeKeys() {
		return mUseVolumeKeys;
	}
	
	public void setUseVolumeKeys(boolean useVolumeKeys) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(Constants.USAGE_USE_VOLUME_KEYS, useVolumeKeys);
		editor.commit();
		this.mUseVolumeKeys = useVolumeKeys;
	}
	
	public boolean isVibrateOnLastArticle() {
		return mVibrateOnLastArticle;
	}
	
	public void setVibrateOnLastArticle(boolean vibrateOnLastArticle) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(Constants.USAGE_VIBRATE_ON_LAST_ARTICLE, vibrateOnLastArticle);
		editor.commit();
		this.mVibrateOnLastArticle = vibrateOnLastArticle;
	}
	
	public boolean isWorkOffline() {
		return mWorkOffline;
	}
	
	public void setWorkOffline(boolean workOffline) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(Constants.USAGE_WORK_OFFLINE, workOffline);
		editor.commit();
		this.mWorkOffline = workOffline;
	}
	
	// ******* DISPLAY-Options ****************************
	
	public boolean isDisplayVirtuals() {
		return mDisplayVirtuals;
	}
	
	public void setDisplayVirtuals(boolean displayVirtuals) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(Constants.DISPLAY_SHOW_VIRTUAL, displayVirtuals);
		editor.commit();
		this.mDisplayVirtuals = displayVirtuals;
	}
	
	public boolean isDisplayUnreadInVirtualFeeds() {
		return mDisplayUnreadInVirtualFeeds;
	}
	
	public void setDisplayUnreadInVirtualFeeds(boolean displayUnreadInVirtualFeeds) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(Constants.DISPLAY_SHOW_VIRTUAL_UNREAD, displayUnreadInVirtualFeeds);
		editor.commit();
		this.mDisplayUnreadInVirtualFeeds = displayUnreadInVirtualFeeds;
	}
	
	public boolean isAlwaysFullRefresh() {
		return mAlwaysFullRefresh;
	}
	
	public void setAlwaysFullRefresh(boolean alwaysFullRefresh) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(Constants.DISPLAY_ALWAYS_FULL_REFRESH, alwaysFullRefresh);
		editor.commit();
		this.mAlwaysFullRefresh = alwaysFullRefresh;
	}
	
	public boolean isUseSwipe() {
		return mUseSwipe;
	}
	
	public void setUseSwipe(boolean useSwipe) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(Constants.DISPLAY_USE_SWIPE, useSwipe);
		editor.commit();
		this.mUseSwipe = useSwipe;
	}
	
	public boolean isDisplayOnlyUnread() {
		return mDisplayOnlyUnread;
	}
	
	public void setDisplayOnlyUnread(boolean displayOnlyUnread) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(Constants.DISPLAY_ONLY_UNREAD, displayOnlyUnread);
		editor.commit();
		this.mDisplayOnlyUnread = displayOnlyUnread;
	}
	
	public int getArticleLimit() {
		return mArticleLimit;
	}
	
	public void setArticleLimit(int articleLimit) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(Constants.DISPLAY_ARTICLE_LIMIT, articleLimit+"");
		editor.commit();
		this.mArticleLimit = articleLimit;
	}
	
	// ******* Internal Data ****************************
	
	public long getLastUpdateTime() {
		return mLastUpdateTime;
	}

	public void setLastUpdateTime(long lastUpdateTime) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(Constants.LAST_UPDATE_TIME, lastUpdateTime+"");
		editor.commit();
		this.mLastUpdateTime = lastUpdateTime;
	}
	
	public int getDatabaseVersion() {
		return mDatabaseVersion;
	}

	public void setDatabaseVersion(int databaseVersion) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(Constants.DATABASE_VERSION, databaseVersion+"");
		editor.commit();
		this.mDatabaseVersion = databaseVersion;
	}
	
}
