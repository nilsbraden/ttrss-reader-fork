/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 Nils Braden
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

import java.io.Serializable;
import java.util.Comparator;
import org.ttrssreader.model.pojos.Label;

@SuppressWarnings("serial")
public class LabelTitleComparator implements Comparator<Label>, Serializable {
    
    public static final Comparator<Label> LABELTITLE_COMPARATOR = new LabelTitleComparator();
    
    public int compare(Label obj1, Label obj2) {
        if (obj1 == null || obj2 == null)
            throw new NullPointerException();
        return obj1.caption.compareTo(obj2.caption);
    }
    
}
