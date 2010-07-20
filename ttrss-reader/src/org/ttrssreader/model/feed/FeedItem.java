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

package org.ttrssreader.model.feed;

public class FeedItem {
	
	private String mId;
	private String mCategoryId;
	private String mTitle;
	private String mUrl;
	private int mUnread;
	
	public FeedItem(String categoryId, String id, String title, String url, int unread) {
		mCategoryId = categoryId;
		mId = id;
		mTitle = title;
		mUrl = url;
		mUnread = unread;		
	}
	
	public String getId() {
		return mId;
	}
	
	public String getCategoryId() {
		return mCategoryId;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getUrl() {
		return mUrl;
	}
	
	public int getUnreadCount() {
		return mUnread;
	}
	
	public void setDeltaUnreadCount(int value) {
		mUnread += value;
	}

}
