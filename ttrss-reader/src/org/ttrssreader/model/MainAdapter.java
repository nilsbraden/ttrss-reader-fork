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
import org.ttrssreader.utils.WeakReferenceHandler;
import android.content.Context;
import android.database.Cursor;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class MainAdapter extends BaseAdapter {
    
    protected ArrayList<Object[]> content;
    public static final int POS_ID = 0;
    public static final int POS_TITLE = 1;
    public static final int POS_UNREAD = 2;
    
    protected Context context;
    
    protected int categoryId = Integer.MIN_VALUE;
    protected int feedId = Integer.MIN_VALUE;
    
    protected boolean selectArticlesForCategory;
    
    public MainAdapter(Context context) {
        this(context, Integer.MIN_VALUE);
    }
    
    public MainAdapter(Context context, int categoryId) {
        this(context, Integer.MIN_VALUE, categoryId, false);
    }
    
    public MainAdapter(Context context, int feedId, int categoryId, boolean selectArticlesForCategory) {
        this.context = context;
        this.feedId = feedId;
        this.categoryId = categoryId;
        this.selectArticlesForCategory = selectArticlesForCategory;
        this.handler = new MsgHandler(this);
        makeQuery(false);
    }
    
    public final void close() {
        // closeCursor(cursor);
    }
    
    public final void closeCursor(Cursor c) {
        if (c == null || c.isClosed())
            return;
        
        c.close();
    }
    
    @Override
    public final int getCount() {
        if (content == null)
            return 0;
        return content.size();
    }
    
    @Override
    public final long getItemId(int position) {
        return position;
    }
    
    public final int getId(int position) {
        if (position >= 0 && content != null)
            return (Integer) (content.get(position)[POS_ID]);
        else
            return -1;
    }
    
    public final List<Integer> getIds() {
        if (content == null)
            return new ArrayList<Integer>();
        
        List<Integer> result = new ArrayList<Integer>(content.size());
        for (Object[] o : content) {
            result.add((Integer) (o[POS_ID]));
        }
        return result;
    }
    
    public final String getTitle(int position) {
        if (position >= 0 && content != null)
            return (String) (content.get(position)[POS_TITLE]);
        else
            return "";
    }
    
    public static final CharSequence formatEntryTitle(String title, int unread) {
        if (unread > 0) {
            return title + " (" + unread + ")";
        } else {
            return title;
        }
    }
    
    /**
     * Discards the old cursor and fetches new data in the background.
     */
    public final void refreshQuery() {
        new Thread(new Runnable() {
            public void run() {
                makeQuery(true);
            }
        }).start();
    }
    
    /**
     * Creates a new query if necessary
     */
    public void makeQuery(boolean force) {
        makeQuery(force, false);
    }
    
    /**
     * Creates a new query if necessary or called with force = true.
     * 
     * @param force
     *            forces the creation of a new query
     */
    public void makeQuery(boolean force, boolean overrideUnreadCheck) {
        if (!force) {
            if (content != null && !content.isEmpty())
                return;
        }
        
        synchronized (this) {
            
            // Check again to reduce the number of unnecessary new cursors
            if (!force) {
                if (content != null && !content.isEmpty())
                    return;
            }
            
            ArrayList<Object[]> contentTest = null;
            try {
                if (categoryId == 0 && (feedId == -1 || feedId == -2)) {
                    
                    contentTest = executeQuery(true, false); // Starred/Published
                    
                } else {
                    
                    contentTest = executeQuery(false, false); // normal query
                    
                    // (categoryId == -2 || feedId >= 0): Normal feeds
                    // (categoryId == 0 || feedId == Integer.MIN_VALUE): Uncategorized Feeds
                    if ((categoryId == -2 || feedId >= 0) || (categoryId == 0 || feedId == Integer.MIN_VALUE)) {
                        if (Controller.getInstance().onlyUnread() && !checkUnread(contentTest)) {
                            
                            contentTest = executeQuery(true, false); // Override unread if query was empty
                            
                        }
                    }
                }
                
            } catch (Exception e) {
                
                contentTest = executeQuery(false, true); // Fail-safe-query
                
            }
            
            // Try to almost atomically switch the old for the new cursor and close the old one afterwards
            if (contentTest != null && !contentTest.isEmpty()) {
                content = contentTest;
                handler.sendEmptyMessage(0);
            }
        }
    }
    
    /**
     * Tries to find out if the given cursor points to a dataset with unread articles in it, returns true if it does.
     * 
     * @param cursor
     *            the cursor.
     * @return true if there are unread articles in the dataset, else false.
     */
    private final boolean checkUnread(ArrayList<Object[]> contentTest) {
        if (contentTest == null || contentTest.isEmpty())
            return false;
        
        for (Object[] o : contentTest) {
            if (((Integer) o[POS_UNREAD]) > 0) {
                return true;
            }
        }
        
        return false;
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
     * @param buildSafeQuery
     *            indicates that the query should modified to also display unread content even though displayUnread is
     *            disabled, this is used to get a new query when the current query is empty.
     * @param forceRefresh
     *            this indicates that a refresh of the cursor should be forced.
     * @return a valid SQL-Query string for this adapter.
     */
    protected abstract ArrayList<Object[]> executeQuery(boolean overrideDisplayUnread, boolean buildSafeQuery);
    
    // Use handler with weak reference on parent object
    private static class MsgHandler extends WeakReferenceHandler<BaseAdapter> {
        public MsgHandler(BaseAdapter parent) {
            super(parent);
        }
        
        @Override
        public void handleMessage(BaseAdapter parent, Message msg) {
            parent.notifyDataSetChanged();
        }
    }
    
    private MsgHandler handler;
    
}
