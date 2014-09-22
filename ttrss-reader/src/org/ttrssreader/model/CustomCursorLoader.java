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

package org.ttrssreader.model;

import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.gui.interfaces.IItemSelectedListener.TYPE;
import org.ttrssreader.model.CategoryCursorHelper.MemoryDBOpenHelper;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CustomCursorLoader extends CursorLoader {
    
    protected static final String TAG = CustomCursorLoader.class.getSimpleName();
    
    TYPE type;
    
    int categoryId;
    int feedId;
    boolean selectArticlesForCategory;
    
    public CustomCursorLoader(Context context, TYPE type, int categoryId, int feedId, boolean selectArticlesForCategory) {
        super(context);
        this.type = type;
        this.categoryId = categoryId;
        this.feedId = feedId;
        this.selectArticlesForCategory = selectArticlesForCategory;
    }
    
    private final ForceLoadContentObserver mObserver = new ForceLoadContentObserver();
    
    @Override
    public Cursor loadInBackground() {
        MainCursorHelper cursorHelper = null;
        
        SQLiteOpenHelper openHelper = new SQLiteOpenHelper(getContext(), DBHelper.DATABASE_NAME, null,
                DBHelper.DATABASE_VERSION) {
            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                throw new RuntimeException("Upgrade not implemented here!");
            }
            
            @Override
            public void onCreate(SQLiteDatabase db) {
                throw new RuntimeException("Create not implemented here!");
            }
        };
        SQLiteDatabase db = openHelper.getReadableDatabase();
        
        switch (type) {
            case CATEGORY: {
                MemoryDBOpenHelper memoryDbOpenHelper = new MemoryDBOpenHelper(getContext());
                SQLiteDatabase memoryDb = memoryDbOpenHelper.getWritableDatabase();
                cursorHelper = new CategoryCursorHelper(getContext(), memoryDb);
                break;
            }
            case FEED:
                cursorHelper = new FeedCursorHelper(getContext(), categoryId);
                break;
            case FEEDHEADLINE:
                cursorHelper = new FeedHeadlineCursorHelper(getContext(), feedId, categoryId, selectArticlesForCategory);
                break;
            default:
                return null;
        }
        
        Cursor cursor = cursorHelper.makeQuery(db);
        if (cursor != null) {
            // Ensure the cursor window is filled
            cursor.getCount();
            cursor.registerContentObserver(mObserver);
        }
        
        return cursor;
    }
    
};
