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
import org.ttrssreader.preferences.PreferencesConstants;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Controller {
	
	private final static String JSON_END_URL = "api/";
	
	private boolean mIsControllerInitialized = false;
	private ITTRSSConnector mTTRSSConnector;
	
	private boolean mAlwaysFullRefresh = false;
	private boolean mAutomaticMarkRead = true;
	private boolean mUseSwipe = true;
	private boolean mDisplayOnlyUnread = false;
	private boolean mDisplayVirtuals = true;
	private int mArticleLimit = 100;
	
	private static Controller mInstance = null;
	private SharedPreferences prefs = null;
	
	private Controller() {}
	
	public static Controller getInstance() {
		if (mInstance == null) {
			mInstance = new Controller();
		}
		return mInstance;
	}
	
	public void initializeController(Context context) {
		
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		String url = PreferenceManager.getDefaultSharedPreferences(context).getString(
				PreferencesConstants.CONNECTION_URL, "http://localhost/");
		
		if (!url.endsWith(JSON_END_URL)) {
			if (!url.endsWith("/")) {
				url += "/";
			}
			url += JSON_END_URL;
		}
		
		String userName = PreferenceManager.getDefaultSharedPreferences(context).getString(
				PreferencesConstants.CONNECTION_USERNAME, "");
		String password = PreferenceManager.getDefaultSharedPreferences(context).getString(
				PreferencesConstants.CONNECTION_PASSWORD, "");
		
		boolean showUnreadInVirtualFeeds = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				PreferencesConstants.DISPLAY_SHOW_VIRTUAL_UNREAD, false);
		
		mAlwaysFullRefresh = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				PreferencesConstants.DISPLAY_ALWAYS_FULL_REFRESH, false);
		
		mAutomaticMarkRead = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				PreferencesConstants.USAGE_AUTOMATIC_MARK_READ, true);
		
		mUseSwipe = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				PreferencesConstants.USE_SWIPE, true);
		
		mDisplayOnlyUnread = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				PreferencesConstants.DISPLAY_ONLY_UNREAD, false);
		
		mDisplayVirtuals = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				PreferencesConstants.DISPLAY_SHOW_VIRTUAL, true);
		
		String artLimit = PreferenceManager.getDefaultSharedPreferences(context).getString(
				PreferencesConstants.ARTICLE_LIMIT, "100");
		mArticleLimit = new Integer(artLimit).intValue();
		
		mTTRSSConnector = new TTRSSJsonConnector(url, userName, password, showUnreadInVirtualFeeds);
	}
	
	public synchronized void checkAndInitializeController(final Context context) {
		if (!mIsControllerInitialized) {					

			initializeController(context);

			mIsControllerInitialized = true;
		}
	}
	
	public ITTRSSConnector getTTRSSConnector() {
		return mTTRSSConnector;		
	}
	
	public boolean isAlwaysPerformFullRefresh() {
		return mAlwaysFullRefresh;
	}
	
	public boolean isAutomaticMarkReadEnabled() {
		return mAutomaticMarkRead;
	}

	public boolean isUseSwipeEnabled() {
		return mUseSwipe;
	}

	public void setDisplayOnlyUnread(Context context, boolean displayOnlyUnread) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(PreferencesConstants.DISPLAY_ONLY_UNREAD, displayOnlyUnread);
		editor.commit();
		mDisplayOnlyUnread = displayOnlyUnread;
	}
	
	public boolean isDisplayOnlyUnreadEnabled() {
		return mDisplayOnlyUnread;
	}
	
	public boolean isDisplayVirtualsEnabled() {
		return mDisplayVirtuals;
	}
	
	public int getArticleLimit() {
		return mArticleLimit;
	}
	
}
