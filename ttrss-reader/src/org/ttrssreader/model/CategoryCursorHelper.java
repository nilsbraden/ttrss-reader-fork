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
import org.ttrssreader.controllers.DBHelper;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

public class CategoryCursorHelper extends MainCursorHelper {
    
    /*
     * This is quite a hack. Since partial-sorting of sql-results is not possible I wasn't able to sort virtual
     * categories by id, Labels by title, insert uncategorized feeds there and sort categories by title again.
     * No I insert these results one by one in a memory-table in the right order, add an auto-increment-column
     * ("sortId INTEGER PRIMARY KEY") and afterwards select everything from this memory-table sorted by sortId.
     * Works fine!
     */
    private static final String TABLE_NAME = "categories_memory_db";
    private static final String INSERT = "REPLACE INTO " + TABLE_NAME
            + " (_id, title, unread, sortId) VALUES (?, ?, ?, null)";
    private SQLiteDatabase memoryDb;
    private SQLiteStatement insert;
    
    public CategoryCursorHelper(Context context) {
        super(context);
    }
    
    @Override
    public Cursor createCursor(SQLiteDatabase db, boolean overrideDisplayUnread, boolean buildSafeQuery) {
        boolean displayUnread = Controller.getInstance().onlyUnread();
        boolean invertSortFeedCats = Controller.getInstance().invertSortFeedscats();
        
        if (overrideDisplayUnread)
            displayUnread = false;
        
        if (memoryDb != null)
            memoryDb.close();
        
        OpenHelper openHelper = new OpenHelper(context, TABLE_NAME);
        memoryDb = openHelper.getWritableDatabase();
        insert = memoryDb.compileStatement(INSERT);
        
        StringBuilder query;
        // Virtual Feeds
        if (Controller.getInstance().showVirtual()) {
            query = new StringBuilder();
            query.append("SELECT _id,title,unread FROM ");
            query.append(DBHelper.TABLE_CATEGORIES);
            query.append(" WHERE _id>=-4 AND _id<0 ORDER BY _id");
            insertValues(db, query.toString());
        }
        
        // Labels
        query = new StringBuilder();
        query.append("SELECT _id,title,unread FROM ");
        query.append(DBHelper.TABLE_FEEDS);
        query.append(" WHERE _id<-10");
        query.append(displayUnread ? " AND unread>0" : "");
        query.append(" ORDER BY UPPER(title) ASC");
        query.append(" LIMIT 500 ");
        insertValues(db, query.toString());
        
        // "Uncategorized Feeds"
        query = new StringBuilder();
        query.append("SELECT _id,title,unread FROM ");
        query.append(DBHelper.TABLE_CATEGORIES);
        query.append(" WHERE _id=0");
        insertValues(db, query.toString());
        
        // Categories
        query = new StringBuilder();
        query.append("SELECT _id,title,unread FROM ");
        query.append(DBHelper.TABLE_CATEGORIES);
        query.append(" WHERE _id>0");
        query.append(displayUnread ? " AND unread>0" : "");
        query.append(" ORDER BY UPPER(title) ");
        query.append(invertSortFeedCats ? "DESC" : "ASC");
        query.append(" LIMIT 500 ");
        insertValues(db, query.toString());
        
        String[] columns = { "_id", "title", "unread" };
        return memoryDb.query(TABLE_NAME, columns, null, null, null, null, null, "600");
    }
    
    private void insertValues(SQLiteDatabase db, String query) {
        Cursor c = null;
        try {
            c = db.rawQuery(query, null);
            if (c == null)
                return;
            if (c.isBeforeFirst() && !c.moveToFirst())
                return;
            
            while (true) {
                insert.bindLong(1, c.getInt(0)); // id
                insert.bindString(2, c.getString(1)); // title
                insert.bindLong(3, c.getInt(2)); // unread
                insert.executeInsert();
                if (!c.moveToNext())
                    break;
            }
        } finally {
            if (c != null && !c.isClosed())
                c.close();
        }
    }
}
