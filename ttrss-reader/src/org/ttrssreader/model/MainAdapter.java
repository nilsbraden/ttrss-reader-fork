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
import java.util.List;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class MainAdapter extends BaseAdapter {
    
    protected Context context;
    protected Cursor cursor;
    
    protected boolean displayOnlyUnread;
    protected boolean invertSortFeedCats;
    protected boolean invertSortArticles;
    protected int categoryId;
    protected int feedId;
    
    protected boolean selectArticlesForCategory;
    
    public MainAdapter(Context context) {
        this.context = context;
        
        this.displayOnlyUnread = Controller.getInstance().displayOnlyUnread();
        this.invertSortFeedCats = Controller.getInstance().invertSortFeedsCats();
        this.invertSortArticles = Controller.getInstance().invertSortArticleList();
        
        makeQuery();
    }
    
    public MainAdapter(Context context, int feedId, int categoryId, boolean selectArticlesForCategory) {
        this.context = context;
        this.selectArticlesForCategory = selectArticlesForCategory;
        this.feedId = feedId;
        this.categoryId = categoryId;
        
        this.displayOnlyUnread = Controller.getInstance().displayOnlyUnread();
        this.invertSortFeedCats = Controller.getInstance().invertSortFeedsCats();
        this.invertSortArticles = Controller.getInstance().invertSortArticleList();
        
        makeQuery();
    }
    
    public final void closeCursor() {
        synchronized (cursor) {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }
    
    @Override
    public final int getCount() {
        synchronized (cursor) {
            if (cursor.isClosed()) {
                makeQuery();
            }
            
            return cursor.getCount();
        }
    }
    
    @Override
    public final long getItemId(int position) {
        return position;
    }
    
    public final int getId(int position) {
        int ret = 0;
        synchronized (cursor) {
            if (cursor.isClosed()) {
                makeQuery();
            }
            
            if (cursor.getCount() >= position) {
                if (cursor.moveToPosition(position)) {
                    ret = cursor.getInt(0);
                }
            }
        }
        return ret;
    }
    
    public final List<Integer> getIds() {
        List<Integer> result = new ArrayList<Integer>();
        synchronized (cursor) {
            if (cursor.isClosed()) {
                makeQuery();
            }
            
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    result.add(cursor.getInt(0));
                    cursor.move(1);
                }
            }
        }
        return result;
    }
    
    public final String getTitle(int position) {
        String ret = "";
        synchronized (cursor) {
            if (cursor.isClosed()) {
                makeQuery();
            }
            
            if (cursor.getCount() >= position) {
                if (cursor.moveToPosition(position)) {
                    ret = cursor.getString(1);
                }
            }
        }
        return ret;
    }
    
    public static final String formatTitle(String title, int unread) {
        if (unread > 0) {
            return title + " (" + unread + ")";
        } else {
            return title;
        }
    }
    
    public final synchronized void makeQuery() {
        makeQuery(false);
    }
    
    /**
     * Only refresh if forceRefresh is true (called from constructor) or one of the display-attributes changed.
     * 
     * @param forceRefresh
     *            Discards the current cursor and forces a refresh, including a newly built SQL-Query.
     */
    public final synchronized void makeQuery(boolean forceRefresh) {
        boolean refresh = false;
        // Check if display-settings have changed
        if (displayOnlyUnread != Controller.getInstance().displayOnlyUnread()) {
            refresh = true;
            displayOnlyUnread = !displayOnlyUnread;
        }
        if (invertSortFeedCats != Controller.getInstance().invertSortFeedsCats()) {
            refresh = true;
            invertSortFeedCats = !invertSortFeedCats;
        }
        if (invertSortArticles != Controller.getInstance().invertSortArticleList()) {
            refresh = true;
            invertSortArticles = !invertSortArticles;
        }
        
        // if: sort-order or display-settings changed
        // if: forced by explicit call with forceRefresh
        // if: cursor is closed or null
        
        if (refresh || forceRefresh || (cursor != null && cursor.isClosed()) || cursor == null) {
            if (cursor != null)
                closeCursor();
            
            String query = buildQuery(false);
            Log.v(Utils.TAG, query);
            cursor = DBHelper.getInstance().query(query, null);
            
            // Call again with override enabled if cursor doesn't contain any data
            if (!checkUnread(cursor)) {
                
                query = buildQuery(true);
                Log.v(Utils.TAG, query);
                cursor = DBHelper.getInstance().query(query, null);
                
            }
        } else if (cursor != null) {
            cursor.requery();
        }
        
    }
    
    private boolean checkUnread(Cursor c) {
        if (c == null || c.isClosed())
            return false;
        
        boolean gotUnread = false;
        if (c.moveToFirst()) {
            int col = c.getColumnIndex("unread");
            if (col > -1) {
                while (!c.isAfterLast()) {
                    int unread = c.getInt(col);
                    if (unread > 0) {
                        gotUnread = true;
                        break;
                    }
                    c.move(1);
                }
            }
            
        }
        
        c.moveToFirst();
        return gotUnread;
    }
    
    @Override
    public abstract Object getItem(int position);
    
    @Override
    public abstract View getView(int position, View convertView, ViewGroup parent);
    
    /**
     * Builds the query for this adapter as a string and returns it to be invoked on a database object.
     * 
     * @param overrideDisplayUnread
     *            if true unread articles/feeds/anything won't be filtered as specified by the setting but will be
     *            included in the result.
     * @return a valid SQL-Query string for this adapter.
     */
    protected abstract String buildQuery(boolean overrideDisplayUnread);
    
}
