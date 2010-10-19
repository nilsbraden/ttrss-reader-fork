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

package org.ttrssreader.model.category;

import org.ttrssreader.utils.Utils;
import android.util.Log;

public class CategoryItem {
    
    private int mId;
    private String mTitle;
    private int mUnread;
    
    public CategoryItem() {
    }
    
    public CategoryItem(int id, String title, int unread) {
        setId(id);
        setTitle(title);
        setUnread(unread);
    }
    
    public CategoryItem(String id, String title, int unread) {
        setId(id);
        setTitle(title);
        setUnread(unread);
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
            Log.d(Utils.TAG, "Category-ID has to be an integer-value but was " + id);
        }
    }
    
    public String getTitle() {
        return mTitle;
    }
    
    public void setTitle(String title) {
        this.mTitle = title;
    }
    
    public int getUnread() {
        return mUnread;
    }
    
    public void setUnread(int unreadCount) {
        this.mUnread = unreadCount;
    }
    
    public void setDeltaUnreadCount(int value) {
        if (isUnreadManaged()) {
            mUnread += value;
        }
    }
    
    public boolean isUnreadManaged() {
        return mUnread >= 0;
    }
}
