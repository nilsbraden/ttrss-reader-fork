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

import java.util.ArrayList;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.model.updaters.IUpdatable;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class MainAdapter extends BaseAdapter implements IUpdatable {
    
    protected Context context;
    protected Cursor cursor;
    protected SQLiteDatabase db;
    
    protected boolean displayOnlyUnread;
    protected boolean invertSortFeedCats;
    protected boolean invertSortArticles;
    protected int unreadCount = 0;
    protected int categoryId;
    protected int feedId;
    
    public MainAdapter(Context context) {
        this.context = context;
        openDB();
        
        this.displayOnlyUnread = Controller.getInstance().displayOnlyUnread();
        this.invertSortFeedCats = Controller.getInstance().invertSortFeedsCats();
        this.invertSortArticles = Controller.getInstance().invertSortArticleList();

        makeQuery();
    }
    
    public void openDB() {
        closeDB();
        
        OpenHelper openHelper = new OpenHelper(context);
        db = openHelper.getWritableDatabase();
        db.setLockingEnabled(false);
    }
    
    public void closeDB() {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        if (db != null && db.isOpen()) {
            db.close();
        }
    }
    
    public boolean isDBOpen() {
        return db.isOpen();
    }
    
    private static class OpenHelper extends SQLiteOpenHelper {
        OpenHelper(Context context) {
            super(context, DBHelper.DATABASE_NAME, null, DBHelper.DATABASE_VERSION);
        }
        
        // @formatter:off
        @Override public void onCreate(SQLiteDatabase db) { }
        @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
        // @formatter:on
    }
    
    @Override
    public int getCount() {
        if (cursor.isClosed()) {
            return -1;
        }
        
        return cursor.getCount();
    }
    
    @Override
    public Object getItem(int position) {
        return null;
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }
    
    @Override
    public void update() {
    }
    
    public int getId(int position) {
        if (cursor.isClosed()) {
            return -1;
        }
        
        if (cursor.getCount() >= position) {
            if (cursor.moveToPosition(position)) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }
    
    public ArrayList<Integer> getIds() {
        if (cursor.isClosed()) {
            return null;
        }
        
        ArrayList<Integer> result = new ArrayList<Integer>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            result.add(cursor.getInt(0));
            cursor.move(1);
        }
        return result;
    }
    
    public String getTitle(int position) {
        if (!cursor.isClosed() && cursor.getCount() >= position) {
            if (cursor.moveToPosition(position)) {
                return cursor.getString(1);
            }
        }
        return "";
    }
    
    public int getUnread() {
        return unreadCount;
    }
    
    protected String formatTitle(String title, int unread) {
        if (unread > 0) {
            return title + " (" + unread + ")";
        } else {
            return title;
        }
    }
    
    public synchronized void makeQuery() {
        // Check if display-settings have changed
        if (displayOnlyUnread != Controller.getInstance().displayOnlyUnread()) {
            displayOnlyUnread = !displayOnlyUnread;
            cursor.close();
        } else if (invertSortFeedCats != Controller.getInstance().invertSortFeedsCats()) {
            invertSortFeedCats = !invertSortFeedCats;
            cursor.close();
        } else if (invertSortArticles != Controller.getInstance().invertSortArticleList()) {
            invertSortArticles = !invertSortArticles;
            cursor.close();
        }
        
        // Log.v(Utils.TAG, query.toString());
        if (cursor != null)
            cursor.close();
        if (db == null || !db.isOpen())
            openDB();
        
        cursor = db.rawQuery(buildQuery(), null);
    }
    
    protected String buildQuery() {
        return "";
    }
    
}
