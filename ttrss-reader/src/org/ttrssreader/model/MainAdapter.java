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
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class MainAdapter extends BaseAdapter implements IUpdatable {
    
    protected Context context;
    public Cursor cursor;
    
    protected boolean displayOnlyUnread;
    protected boolean invertSortFeedCats;
    protected boolean invertSortArticles;
    protected int unreadCount = 0;
    protected int categoryId;
    protected int feedId;
    
    public MainAdapter(Context context) {
        this.context = context;
        this.displayOnlyUnread = Controller.getInstance().displayOnlyUnread();
        this.invertSortFeedCats = Controller.getInstance().invertSortFeedsCats();
        this.invertSortArticles = Controller.getInstance().invertSortArticleList();
        makeQuery();
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
    
    protected void closeCursor() {
        if (cursor != null) {
            cursor.close();
        }
    }
    
    public synchronized void makeQuery() {
        // Check if display-settings have changed
        if (displayOnlyUnread != Controller.getInstance().displayOnlyUnread()) {
            displayOnlyUnread = !displayOnlyUnread;
            closeCursor();
        } else if (invertSortFeedCats != Controller.getInstance().invertSortFeedsCats()) {
            invertSortFeedCats = !invertSortFeedCats;
            closeCursor();
        } else if (invertSortArticles != Controller.getInstance().invertSortArticleList()) {
            invertSortArticles = !invertSortArticles;
            closeCursor();
        }
        
        // Log.v(Utils.TAG, query.toString());
        if (cursor != null)
            cursor.close();
        cursor = DBHelper.getInstance().query(buildQuery(), null);
    }
    
    protected String buildQuery() {
        return "";
    }
    
}
