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

import java.util.ArrayList;
import java.util.List;
import org.ttrssreader.R;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

public abstract class MainAdapter extends SimpleCursorAdapter {
    
    protected static final String TAG = MainAdapter.class.getSimpleName();
    
    private static final int layout = R.layout.main;
    
    protected Context context;
    
    public MainAdapter(Context context) {
        super(context, layout, null, new String[] {}, new int[] {}, 0);
        this.context = context;
    }
    
    @Override
    public final long getItemId(int position) {
        return position;
    }
    
    public final int getId(int position) {
        int ret = Integer.MIN_VALUE;
        Cursor cur = getCursor();
        if (cur == null)
            return ret;
        
        if (cur.getCount() >= position)
            if (cur.moveToPosition(position))
                ret = cur.getInt(0);
        return ret;
    }
    
    public final List<Integer> getIds() {
        List<Integer> ret = new ArrayList<Integer>();
        Cursor cur = getCursor();
        if (cur == null)
            return ret;
        
        if (cur.moveToFirst()) {
            while (!cur.isAfterLast()) {
                ret.add(cur.getInt(0));
                cur.move(1);
            }
        }
        return ret;
    }
    
    protected static final CharSequence formatItemTitle(String title, int unread) {
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
