/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 Nils Braden
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

public class Feed implements Comparable<Feed> {
    
    public int id;
    public int categoryId;
    public String title;
    public String url;
    public int unread;
    
    public Feed() {
        this.id = 0;
        this.categoryId = 0;
        this.title = "";
        this.url = "";
        this.unread = 0;
    }
    
    public Feed(int id, int categoryId, String title, String url, int unread) {
        this.id = id;
        this.categoryId = categoryId;
        this.title = title;
        this.url = url;
        this.unread = unread;
    }
    
    @Override
    public int compareTo(Feed fi) {
        return title.compareToIgnoreCase(fi.title);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof Feed) {
            Feed other = (Feed) o;
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
