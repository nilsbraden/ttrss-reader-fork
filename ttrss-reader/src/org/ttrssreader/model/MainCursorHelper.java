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

package org.ttrssreader.model;

import org.ttrssreader.controllers.Controller;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public abstract class MainCursorHelper {
    
    protected static final String TAG = MainCursorHelper.class.getSimpleName();
    
    protected Context context;
    
    protected int categoryId = Integer.MIN_VALUE;
    protected int feedId = Integer.MIN_VALUE;
    
    protected boolean selectArticlesForCategory;
    
    public MainCursorHelper(Context context) {
        this.context = context;
    }
    
    /**
     * Creates a new query
     * 
     * @param force
     *            forces the creation of a new query
     */
    public Cursor makeQuery(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            if (categoryId == 0 && (feedId == -1 || feedId == -2)) {
                
                // Starred/Published
                cursor = createCursor(db, true, false);
                
            } else {
                
                // normal query
                cursor = createCursor(db, false, false);
                
                // (categoryId == -2 || feedId >= 0): Normal feeds
                // (categoryId == 0 || feedId == Integer.MIN_VALUE): Uncategorized Feeds
                if ((categoryId == -2 || feedId >= 0) || (categoryId == 0 || feedId == Integer.MIN_VALUE)) {
                    if (Controller.getInstance().onlyUnread() && !checkUnread(cursor)) {
                        
                        // Override unread if query was empty
                        cursor = createCursor(db, true, false);
                        
                    }
                }
            }
            
        } catch (Exception e) {
            // Fail-safe-query
            cursor = createCursor(db, false, true);
        }
        return cursor;
    }
    
    /**
     * Tries to find out if the given cursor points to a dataset with unread articles in it, returns true if it does.
     * 
     * @param cursor
     *            the cursor.
     * @return true if there are unread articles in the dataset, else false.
     */
    private static final boolean checkUnread(Cursor cursor) {
        if (cursor == null || cursor.isClosed())
            return false; // Check null or closed
            
        if (!cursor.moveToFirst())
            return false; // Check empty
            
        do {
            if (cursor.getInt(cursor.getColumnIndex("unread")) > 0)
                return cursor.moveToFirst(); // One unread article found, move to first entry
        } while (cursor.moveToNext());
        
        cursor.moveToFirst();
        return false;
    }
    
    abstract Cursor createCursor(SQLiteDatabase db, boolean overrideDisplayUnread, boolean buildSafeQuery);
    
}
