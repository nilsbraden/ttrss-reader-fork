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

public class CategoryItem implements Comparable<CategoryItem> {
    
    public int id;
    public String title;
    public int unread;
    
    public CategoryItem() {
        this.id = 0;
        this.title = "";
        this.unread = 0;
    }
    
    public CategoryItem(int id, String title, int unread) {
        this.id = id;
        this.title = title;
        this.unread = unread;
    }
    
    @Override
    public int compareTo(CategoryItem ci) {
        // Sort by Id if Id is 0 or smaller, else sort by Title
        if (id <= 0 || ci.id <= 0) {
            Integer thisInt = id;
            Integer thatInt = ci.id;
            return thisInt.compareTo(thatInt);
        }
        return title.compareToIgnoreCase(ci.title);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof CategoryItem) {
            CategoryItem other = (CategoryItem) o;
            return (id == other.id);
        } else {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        return id + "".hashCode();
    }
    
}
