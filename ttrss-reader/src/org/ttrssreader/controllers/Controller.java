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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
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
	
	private final static String JSON_END_URL = "api/";
	
	private boolean mIsControllerInitialized = false;
	private ITTRSSConnector mTTRSSConnector;
	
	private static Controller mInstance = null;
	private SharedPreferences prefs = null;
	
	private boolean mAutomaticMarkRead = true;
	private boolean mOpenUrlEmptyArticle = false;
	private boolean mUpdateUnreadOnStartup = false;
	
	private boolean mDisplayVirtuals = true;
	private boolean mDisplayUnreadInVirtualFeeds = false;
	private boolean mAlwaysFullRefresh = false;
	private boolean mUseSwipe = true;
	private boolean mDisplayOnlyUnread = false;
	private int mArticleLimit = 100;
	
	
	private Controller() {}
	
	public static Controller getInstance() {
		if (mInstance == null) {
			mInstance = new Controller();
		}
		return mInstance;
	}
	
	public void initializeController(Context context) {
		
		prefs = PreferenceManager.getDefaultSharedPreferences(context);

		// Login
		String url = prefs.getString(Constants.CONNECTION_URL, "http://localhost/");
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
		setUpdateUnreadOnStartup(prefs.getBoolean(Constants.USAGE_UPDATE_UNREAD_ON_STARTUP, false));
		
		// Display
		mDisplayVirtuals = prefs.getBoolean(Constants.DISPLAY_SHOW_VIRTUAL, true);
		mDisplayUnreadInVirtualFeeds = prefs.getBoolean(Constants.DISPLAY_SHOW_VIRTUAL_UNREAD, false);
		mAlwaysFullRefresh = prefs.getBoolean(Constants.DISPLAY_ALWAYS_FULL_REFRESH, false);
		mUseSwipe = prefs.getBoolean(Constants.DISPLAY_USE_SWIPE, true);
		mDisplayOnlyUnread = prefs.getBoolean(Constants.DISPLAY_ONLY_UNREAD, false);
		try {
			mArticleLimit = Integer.parseInt(prefs.getString(Constants.DISPLAY_ARTICLE_LIMIT, "100"));
		} catch (ClassCastException e) {
			mArticleLimit = 100;
			Log.e(Utils.TAG, "DISPLAY_ARTICLE_LIMIT was not an integer value, using default value: " + mArticleLimit);
		}
	}
	
	public synchronized void checkAndInitializeController(final Context context) {
		if (!mIsControllerInitialized) {					

			initializeController(context);

			mIsControllerInitialized = true;
		}
	}
	
	
	
	// **** Getter / Setter **********
	
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
		this.mUpdateUnreadOnStartup = mUpdateUnreadOnStartup;
	}

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
		editor.putInt(Constants.DISPLAY_ARTICLE_LIMIT, articleLimit);
		editor.commit();
		this.mArticleLimit = articleLimit;
	}
	
}
