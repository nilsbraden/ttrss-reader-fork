/*
 * Copyright (c) 2015, Nils Braden
 *
 * This file is part of ttrss-reader-fork. This program is free software; you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation;
 * either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program; If
 * not, see http://www.gnu.org/licenses/.
 */

package org.ttrssreader.model;

import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

class CategoryCursorHelper extends MainCursorHelper {

    @SuppressWarnings("unused")
    private static final String TAG = CategoryCursorHelper.class.getSimpleName();

    /*
     * This is quite a hack. Since partial-sorting of sql-results is not possible I wasn't able to sort virtual
     * categories by id, Labels by title, insert uncategorized feeds there and sort categories by title again.
     * No I insert these results one by one in a memory-table in the right order, add an auto-increment-column
     * ("sortId INTEGER PRIMARY KEY") and afterwards select everything from this memory-table sorted by sortId.
     * Works fine!
     */
    private static final String INSERT = "REPLACE INTO " + MemoryDBOpenHelper.TABLE_NAME
            + " (_id, title, unread, sortId) VALUES (?, ?, ?, null)";

    private static final Object LOCK = new Object();
    private SQLiteDatabase memoryDb;
    private SQLiteStatement insert;

    CategoryCursorHelper(SQLiteDatabase memoryDb) {
        super();
        this.memoryDb = memoryDb;
        this.insert = memoryDb.compileStatement(INSERT);
    }

    @Override
    public Cursor createCursor(SQLiteDatabase db, boolean overrideDisplayUnread, boolean buildSafeQuery) {
        boolean displayUnread = Controller.getInstance().onlyUnread();
        boolean invertSortFeedCats = Controller.getInstance().invertSortFeedscats();

        if (overrideDisplayUnread)
            displayUnread = false;

        Cursor cur;
        synchronized (LOCK) {
            memoryDb.delete(MemoryDBOpenHelper.TABLE_NAME, null, null);

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

            String[] columns = {"_id", "title", "unread"};
            cur = memoryDb.query(MemoryDBOpenHelper.TABLE_NAME, columns, null, null, null, null, null, "600");
        }
        return cur;
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

    static class MemoryDBOpenHelper extends SQLiteOpenHelper {
        private static final String TABLE_NAME = "categories_memory_db";

        MemoryDBOpenHelper(Context context) {
            super(context, null, null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_NAME
                    + " (_id INTEGER, title TEXT, unread INTEGER, sortId INTEGER PRIMARY KEY)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

}
