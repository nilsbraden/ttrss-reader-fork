/*
 * Tiny Tiny RSS Reader for Android
 * 
 * Copyright (C) 2010 N. Braden and contributors.
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

package org.ttrssreader.model.feed;

import org.ttrssreader.utils.Utils;
import android.util.Log;

public class FeedItem {
    
    private int mId;
    private int mCategoryId;
    private String mTitle;
    private String mUrl;
    private int mUnread;
    
    public FeedItem() {
    }
    
    public FeedItem(int categoryId, int id, String title, String url, int unread) {
        setId(id);
        setCategoryId(categoryId);
        setTitle(title);
        setUrl(url);
        setUnread(unread);
    }
    
    /*
     * Feed-ID given as String, will be parsed in setId(String mId) or set to 0 if value is invalid.
     */
    public FeedItem(String categoryId, String id, String title, String url, int unread) {
        setId(id);
        setCategoryId(categoryId);
        setTitle(title);
        setUrl(url);
        setUnread(unread);
    }
    
    public void setDeltaUnreadCount(int value) {
        mUnread += value;
    }
    
    public int getId() {
        return mId;
    }
    
    public void setId(int id) {
        this.mId = id;
    }
    
    public void setId(String id) {
        // Check if mId is a number, else set to 0
        try {
            if (id == null) {
                this.mId = 0;
                id = "null"; // Set to (String) "null" for log-output..
            } else if (!id.matches("-*[0-9]+")) {
                this.mId = 0;
            } else {
                this.mId = Integer.parseInt(id);
            }
        } catch (NumberFormatException e) {
            Log.d(Utils.TAG, "Feed-ID has to be an integer-value but was " + id);
        }
    }
    
    public int getCategoryId() {
        return mCategoryId;
    }
    
    public void setCategoryId(int categoryId) {
        this.mCategoryId = categoryId;
    }
    
    public void setCategoryId(String categoryId) {
        // Check if mId is a number, else set to 0
        try {
            if (categoryId == null) {
                this.mCategoryId = 0;
                categoryId = "null"; // Set to (String) "null" for log-output..
            } else if (!categoryId.matches("-*[0-9]+")) {
                this.mCategoryId = 0;
            } else {
                this.mCategoryId = Integer.parseInt(categoryId);
            }
        } catch (NumberFormatException e) {
            Log.d(Utils.TAG, "Feed-ID has to be an integer-value but was " + mId);
        }
    }
    
    public String getTitle() {
        return mTitle;
    }
    
    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }
    
    public String getUrl() {
        return mUrl;
    }
    
    public void setUrl(String mUrl) {
        this.mUrl = mUrl;
    }
    
    public int getUnread() {
        return mUnread;
    }
    
    public void setUnread(int mUnread) {
        this.mUnread = mUnread;
    }
}
