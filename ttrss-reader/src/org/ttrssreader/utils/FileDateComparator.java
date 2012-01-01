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
 *
 */
public class FileDateComparator implements Comparator<File> {
    
    @Override
    public int compare(File f1, File f2) {
        
        // Hopefully avoids crashes due to IllegalArgumentExceptions
        if (f1.equals(f2))
            return 0;
        
        long size1 = f1.lastModified();
        long size2 = f2.lastModified();
        
        if (size1 < size2) {
            return -1;
        } else if (size1 > size2) {
            return 1;
        }
        
        return 0; // equal
    }
    
}
