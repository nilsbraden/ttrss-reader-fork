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

package org.ttrssreader.model;

import java.util.ArrayList;
import java.util.List;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.pojos.Category;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CategoryAdapter extends MainAdapter {
    
    public CategoryAdapter(Context context) {
        super(context);
    }
    
    @Override
    public Object getItem(int position) {
        if (cursor.isClosed()) {
            makeQuery();
        }
        
        if (cursor.getCount() >= position) {
            if (cursor.moveToPosition(position)) {
                Category ret = new Category();
                ret.id = cursor.getInt(0);
                ret.title = cursor.getString(1);
                ret.unread = cursor.getInt(2);
                return ret;
            }
        }
        return null;
    }
    
    public List<Category> getCategories() {
        if (cursor.isClosed()) {
            makeQuery();
        }
        
        List<Category> result = new ArrayList<Category>();
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                Category c = new Category();
                c.id = cursor.getInt(0);
                c.title = cursor.getString(1);
                c.unread = cursor.getInt(2);
                result.add(c);
                cursor.move(1);
            }
        }
        return result;
    }
    
    private int getImage(int id, boolean unread) {
        if (id == Data.VCAT_STAR) {
            return R.drawable.star48;
        } else if (id == Data.VCAT_PUB) {
            return R.drawable.published48;
        } else if (id == Data.VCAT_FRESH) {
            return R.drawable.fresh48;
        } else if (id == Data.VCAT_ALL) {
            return R.drawable.all48;
        } else if (id < -10) {
            if (unread) {
                return R.drawable.label;
            } else {
                return R.drawable.label_read;
            }
        } else {
            if (unread) {
                return R.drawable.categoryunread48;
            } else {
                return R.drawable.categoryread48;
            }
        }
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position >= getCount() || position < 0)
            return new View(context);
        
        Category c = (Category) getItem(position);
        
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout layout = null;
        if (convertView == null) {
            layout = (LinearLayout) inflater.inflate(R.layout.categoryitem, null);
        } else {
            if (convertView instanceof LinearLayout) {
                layout = (LinearLayout) convertView;
            }
        }
        
        ImageView icon = (ImageView) layout.findViewById(R.id.icon);
        icon.setImageResource(getImage(c.id, c.unread > 0));
        
        TextView title = (TextView) layout.findViewById(R.id.title);
        title.setText(formatTitle(c.title, c.unread));
        if (c.unread > 0) {
            title.setTypeface(Typeface.DEFAULT_BOLD, 1);
        } else {
            title.setTypeface(Typeface.DEFAULT, 0);
        }
        
        return layout;
    }
    
    protected synchronized Cursor executeQuery(boolean overrideDisplayUnread, boolean buildSafeQuery) {
        if (db != null)
            db.close();
        
        OpenHelper openHelper = new OpenHelper(context);
        db = openHelper.getWritableDatabase();
        insert = db.compileStatement(INSERT);
        
        boolean displayUnread = displayOnlyUnread;
        if (overrideDisplayUnread)
            displayUnread = false;
        
        StringBuilder query;
        
        // Virtual Feeds
        if (Controller.getInstance().showVirtual()) {
            query = new StringBuilder();
            query.append("SELECT id,title,unread FROM ");
            query.append(DBHelper.TABLE_CATEGORIES);
            query.append(" WHERE id>=-4 AND id<0 ORDER BY id");
            insertValues(DBHelper.getInstance().query(query.toString(), null));
        }
        
        // Labels
        query = new StringBuilder();
        query.append("SELECT id,title,unread FROM ");
        query.append(DBHelper.TABLE_FEEDS);
        query.append(" WHERE id<-10 ORDER BY UPPER(title) ASC");
        insertValues(DBHelper.getInstance().query(query.toString(), null));
        
        // "Uncategorized Feeds"
        query = new StringBuilder();
        query.append("SELECT id,title,unread FROM ");
        query.append(DBHelper.TABLE_CATEGORIES);
        query.append(" WHERE id=0");
        query.append(displayUnread ? " AND unread>0" : "");
        insertValues(DBHelper.getInstance().query(query.toString(), null));
        
        // Categories
        query = new StringBuilder();
        query.append("SELECT id,title,unread FROM ");
        query.append(DBHelper.TABLE_CATEGORIES);
        query.append(" WHERE id>0");
        query.append(displayUnread ? " AND unread>0" : "");
        query.append(" ORDER BY UPPER(title) ");
        query.append(invertSortFeedCats ? "DESC" : "ASC"); // TODO: Is that the right way round?
        insertValues(DBHelper.getInstance().query(query.toString(), null));
        
        closeCursor();
        String[] columns = { "id", "title", "unread" };
        Cursor c = db.query(TABLE_NAME, columns, null, null, null, null, "sortId");
        return c;
    }
    
    /*
     * This is quite a hack. Since partial-sorting of sql-results is not possible I wasn't able to sort virtual
     * categories by id, Labels by title, insert uncategorized feeds there and sort categories by title again.
     * No I insert these results one by one in a memory-table in the right order, add an auto-increment-column
     * ("sortId INTEGER PRIMARY KEY") and afterwards select everything from this memory-table sorted by sortId.
     * Works fine!
     */
    private static final String TABLE_NAME = "categories_memory_db";
    private static final String INSERT = "REPLACE INTO " + TABLE_NAME
            + "(id, title, unread, sortId) VALUES (?, ?, ?, null)";
    private SQLiteDatabase db;
    private SQLiteStatement insert;
    
    private static class OpenHelper extends SQLiteOpenHelper {
        OpenHelper(Context context) {
            super(context, null, null, 1);
        }
        
        /**
         * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_NAME
                    + " (id INTEGER, title TEXT, unread INTEGER, sortId INTEGER PRIMARY KEY)");
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }
    
    private void insertValues(Cursor c) {
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
    }
}
