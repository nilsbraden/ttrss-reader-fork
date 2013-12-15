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
import org.ttrssreader.R;
import android.content.Context;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;

public abstract class MainAdapter extends SimpleCursorAdapter {
    
    private static final String[] from = { "title", "updateDate" };
    private static final int[] to = { R.id.title, R.id.updateDate };
    private static final int layout = R.layout.categorylist;
    
    protected Context context;
    
    public String title = null; // TODO
    public int unreadCount = -1; // TODO
    
    public MainAdapter(Context context) {
        super(context, layout, null, from, to, 0);
        this.context = context;
    }
    
    // @Override
    // public final int getCount() {
    // synchronized (poorMansMutex) {
    // return getCursor().getCount();
    // }
    // }
    
    @Override
    public final long getItemId(int position) {
        return position;
    }
    
    public final int getId(int position) {
        int ret = Integer.MIN_VALUE;
        // if (getCursor().getCount() >= position)
        // if (getCursor().moveToPosition(position))
        // ret = getCursor().getInt(0);
        return ret;
    }
    
    public final List<Integer> getIds() {
        List<Integer> result = new ArrayList<Integer>();
        // if (getCursor().moveToFirst()) {
        // while (!getCursor().isAfterLast()) {
        // result.add(getCursor().getInt(0));
        // getCursor().move(1);
        // }
        // }
        return result;
    }
    
    public final String getTitle(int position) {
        String ret = "";
        // if (getCursor().getCount() >= position)
        // if (getCursor().moveToPosition(position))
        // ret = getCursor().getString(1);
        return ret;
    }
    
    public static final CharSequence formatEntryTitle(String title, int unread) {
        if (unread > 0) {
            return title + " (" + unread + ")";
        } else {
            return title;
        }
    }
    
    @Override
    public abstract Object getItem(int position);
    
    @Override
    public abstract View getView(int position, View convertView, ViewGroup parent);
    
}
