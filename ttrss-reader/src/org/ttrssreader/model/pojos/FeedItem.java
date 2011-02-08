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

package org.ttrssreader.model.pojos;

public class FeedItem implements Comparable<FeedItem> {
    
    private int mId;
    private int mCategoryId;
    private String mTitle;
    private String mUrl;
    private int mUnread;
    
    public FeedItem() {
        mId = 0;
        mCategoryId = 0;
        mTitle = "";
        mUrl = "";
        mUnread = 0;
    }
    
    public FeedItem(int id, int categoryId, String title, String url, int unread) {
        mId = id;
        mCategoryId = categoryId;
        mTitle = title;
        mUrl = url;
        mUnread = unread;
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
        // Check if id is a number, else set to 0
        try {
            if (id == null) {
                this.mId = 0;
            } else if (!id.matches("-*[0-9]+")) {
                this.mId = 0;
            } else {
                this.mId = Integer.parseInt(id);
            }
        } catch (NumberFormatException e) {
        }
    }
    
    public int getCategoryId() {
        return mCategoryId;
    }
    
    public void setCategoryId(int categoryId) {
        this.mCategoryId = categoryId;
    }
    
    public void setCategoryId(String categoryId) {
        // Check if categoryId is a number, else set to 0
        try {
            if (categoryId == null) {
                this.mCategoryId = 0;
            } else if (!categoryId.matches("-*[0-9]+")) {
                this.mCategoryId = 0;
            } else {
                this.mCategoryId = Integer.parseInt(categoryId);
            }
        } catch (NumberFormatException e) {
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
    
    @Override
    public int compareTo(FeedItem fi) {
        return this.mTitle.compareToIgnoreCase(fi.getTitle());
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof FeedItem) {
            FeedItem other = (FeedItem) o;
            return (this.getId() == other.getId());
        } else {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        return mId + "".hashCode();
    }
    
}
