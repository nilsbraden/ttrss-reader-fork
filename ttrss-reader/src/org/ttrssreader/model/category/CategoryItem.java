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


public class CategoryItem implements Comparable<CategoryItem> {
    
    private int mId;
    private String mTitle;
    private int mUnread;
    
    public CategoryItem() {
        mId = 0;
        mTitle = "";
        mUnread = 0;
    }
    
    public CategoryItem(int id, String title, int unread) {
        mId = id;
        mTitle = title;
        mUnread = unread;
    }
    
    public CategoryItem(String id, String title, int unread) {
        setId(id);
        mTitle = title;
        mUnread = unread;
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
    
    @Override
    public int compareTo(CategoryItem ci) {
        // Sort by Id if Id is 0 or smaller, else sort by Title
        if (this.getId() <= 0 || ci.getId() <= 0) {
            Integer thisInt = this.getId();
            Integer thatInt = ci.getId();
            return thisInt.compareTo(thatInt);
        }
        return this.getTitle().compareToIgnoreCase(ci.getTitle());
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof CategoryItem) {
            CategoryItem other = (CategoryItem) o;
            return (this.getId() == other.getId());
        } else {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        return this.getId() + "".hashCode();
    }
    
}
