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

package org.ttrssreader.model.category;

public class CategoryItem {
	
	private String mId;
	private String mTitle;
	private int mUnreadCount;
	
	public CategoryItem(String id, String title, int unreadCount) {
		mId = id;
		mTitle = title;
		mUnreadCount = unreadCount;
	}
	
	public String getId() {
		return mId;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public int getUnreadCount() {
		return mUnreadCount;
	}
	
	public boolean isUnreadManaged() {
		return mUnreadCount >= 0;
	}
	
	public void setDeltaUnreadCount(int value) {
		if (isUnreadManaged()) {
			mUnreadCount += value;
		}
	}

}
