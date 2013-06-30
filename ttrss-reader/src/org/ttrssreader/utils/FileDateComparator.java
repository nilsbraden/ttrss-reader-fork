/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
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
package org.ttrssreader.utils;

import java.io.File;
import java.util.Comparator;

/**
 * Compares two files by their last-modified-date.
 * 
 * @author Nils Braden
 */
public class FileDateComparator implements Comparator<File> {
    
    @Override
    public int compare(File f1, File f2) {
        // As suggested here:
        // http://stackoverflow.com/questions/203030/best-way-to-list-files-in-java-sorted-by-date-modified
        // return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
        
        // Last solution somehow also produced errors ("Comparison method violates its general contract!"), this one is
        // copied from LastModifiedFileComparator.java (Apache Commons IO):
        long result = f1.lastModified() - f2.lastModified();
        if (result < 0) {
            return -1;
        } else if (result > 0) {
            return 1;
        } else {
            return 0;
        }
    }
    
}
