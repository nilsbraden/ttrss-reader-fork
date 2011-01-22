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
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.pojos.FeedItem;
import org.ttrssreader.model.updaters.IUpdatable;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FeedListAdapter extends BaseAdapter implements IUpdatable {
    
    private Context context;
    public Cursor cursor;
    
    private int categoryId;
    private boolean displayOnlyUnread;
    int unreadCount = 0;
    
    public FeedListAdapter(Context context, int categoryId) {
        this.context = context;
        this.categoryId = categoryId;
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
        if (cursor.isClosed()) {
            return null;
        }
        
        FeedItem ret = null;
        if (cursor.getCount() >= position) {
            if (cursor.moveToPosition(position)) {
                ret = new FeedItem();
                ret.setId(cursor.getInt(0));
                ret.setTitle(cursor.getString(1));
                ret.setUnread(cursor.getInt(2));
            }
        }
        return ret;
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    public int getFeedId(int position) {
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
    
    public String getFeedTitle(int position) {
        if (cursor.isClosed()) {
            return null;
        }
        
        if (cursor.getCount() >= position) {
            if (cursor.moveToPosition(position)) {
                return cursor.getString(1);
            }
        }
        return "";
    }
    
    public ArrayList<Integer> getFeedIds() {
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
    
    public ArrayList<String> getFeedNames() {
        if (cursor.isClosed()) {
            return null;
        }
        
        ArrayList<String> result = new ArrayList<String>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            result.add(cursor.getString(1));
            cursor.move(1);
        }
        return result;
    }
    
    public int getUnread() {
        return unreadCount;
    }
    
    private String formatTitle(String title, int unread) {
        if (unread > 0) {
            return title + " (" + unread + ")";
        } else {
            return title;
        }
    }
    
    private int getImage(boolean unread) {
        if (unread) {
            return R.drawable.feedheadlinesunread48;
        } else {
            return R.drawable.feedheadlinesread48;
        }
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position >= getCount() || position < 0)
            return new View(context);
        
        FeedItem f = (FeedItem) getItem(position);
        
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout layout = null;
        if (convertView == null) {
            layout = (LinearLayout) inflater.inflate(R.layout.feeditem, null);
        } else {
            if (convertView instanceof LinearLayout) {
                layout = (LinearLayout) convertView;
            }
        }
        
        ImageView icon = (ImageView) layout.findViewById(R.id.icon);
        icon.setImageResource(getImage(f.getUnread() > 0));
        
        TextView title = (TextView) layout.findViewById(R.id.title);
        title.setText(formatTitle(f.getTitle(), f.getUnread()));
        if (f.getUnread() > 0) {
            title.setTypeface(Typeface.DEFAULT_BOLD, 1);
        } else {
            title.setTypeface(Typeface.DEFAULT, 0);
        }
        
        return layout;
    }
    
    public void closeCursor() {
        if (cursor != null) {
            cursor.close();
        }
    }
    
    public synchronized void makeQuery() {
        if (displayOnlyUnread != Controller.getInstance().isDisplayOnlyUnread()) {
            displayOnlyUnread = Controller.getInstance().isDisplayOnlyUnread();
            closeCursor();
        } else if (cursor != null && !cursor.isClosed()) {
            cursor.requery();
        }
        StringBuffer query = new StringBuffer();
        
        query.append("SELECT id,title,unread FROM ");
        query.append(DBHelper.TABLE_FEEDS);
        query.append(" WHERE categoryId=");
        query.append(categoryId);
        
        if (displayOnlyUnread) {
            query.append(" AND unread>0");
        }
        
        query.append(" ORDER BY UPPER(title) ASC");
        
        // Log.v(Utils.TAG, query.toString());
        if (cursor != null)
            cursor.close();
        cursor = DBHelper.getInstance().query(query.toString(), null);
    }
    
    @Override
    public void update() {
        Data.getInstance().updateFeeds(categoryId);
        unreadCount = DBHelper.getInstance().getUnreadCount(categoryId, true);
    }
    
}
