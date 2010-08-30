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

package org.ttrssreader.model.feed;

public class FeedItem {
    
    private String mId;
    private String mCategoryId;
    private String mTitle;
    private String mUrl;
    private int mUnread;
    
    public FeedItem() {
        
    }
    
    public FeedItem(String categoryId, String id, String title, String url, int unread) {
        mCategoryId = categoryId;
        mId = id;
        mTitle = title;
        mUrl = url;
        mUnread = unread;
    }
    
    public void setDeltaUnreadCount(int value) {
        mUnread += value;
    }
    
    public String getId() {
        return mId;
    }
    
    public void setId(String mId) {
        this.mId = mId;
    }
    
    public String getCategoryId() {
        return mCategoryId;
    }
    
    public void setCategoryId(String mCategoryId) {
        this.mCategoryId = mCategoryId;
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
    
    public FeedItem deepCopy() {
        FeedItem ret = new FeedItem();
        
        ret.setId(mId != null ? new String(mId) : null);
        ret.setCategoryId(mCategoryId != null ? new String(mCategoryId) : null);
        ret.setTitle(mTitle != null ? new String(mTitle) : null);
        ret.setUrl(mUrl != null ? new String(mUrl) : null);
        ret.setUnread(mUnread);
        
        return ret;
    }
}
