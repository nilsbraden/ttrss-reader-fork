/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2013 I. Lubimov.
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

import java.util.Date;

/**
 * this class represents remote file (image, attachment, etc)
 * belonging to article(s), which may be locally stored (cached)
 * 
 * @author igor
 */
public class RemoteFile implements Comparable<RemoteFile> {
    /** numerical ID */
    public int id;
    
    /** remote file URL */
    public String url;
    
    /** file size */
    public int length;
    
    /** last change date */
    private Date updated;
    
    /** boolean flag determining if the file is locally stored */
    public volatile boolean cached;
    
    /**
     * default constructor
     */
    public RemoteFile() {
    }
    
    /**
     * constructor with parameters
     * 
     * @param id
     *            numerical ID
     * @param url
     *            remote file URL
     * @param length
     *            file size
     * @param ext
     *            extension - some kind of additional info
     * @param updated
     *            last change date
     * @param cached
     *            boolean flag determining if the file is locally stored
     */
    public RemoteFile(int id, String url, int length, Date updated, boolean cached) {
        this.id = id;
        this.url = url;
        this.length = length;
        this.updated = updated;
        this.cached = cached;
    }
    
    @Override
    public int compareTo(RemoteFile rf) {
        return rf.updated.compareTo(this.updated);
    }
    
    @Override
    public boolean equals(Object o) {
        boolean isEqual = false;
        if (o != null && o instanceof RemoteFile) {
            RemoteFile rf = (RemoteFile) o;
            isEqual = (id == rf.id);
        }
        return isEqual;
    }
    
    @Override
    public int hashCode() {
        return id;
    }
    
}
