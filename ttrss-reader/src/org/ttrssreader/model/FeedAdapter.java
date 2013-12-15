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

import org.ttrssreader.R;
import org.ttrssreader.model.pojos.Feed;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FeedAdapter extends MainAdapter {
    
    public FeedAdapter(Context context) {
        super(context);
    }
    
    @Override
    public Object getItem(int position) {
        Feed ret = new Feed();
        if (getCursor().getCount() >= position) {
            if (getCursor().moveToPosition(position)) {
                ret.id = getCursor().getInt(0);
                ret.title = getCursor().getString(1);
                ret.unread = getCursor().getInt(2);
            }
        }
        return ret;
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
        
        Feed f = (Feed) getItem(position);
        
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout layout = null;
        if (convertView == null) {
            layout = (LinearLayout) inflater.inflate(R.layout.feeditem, null);
        } else {
            if (convertView instanceof LinearLayout) {
                layout = (LinearLayout) convertView;
            }
        }
        
        if (layout == null)
            return new View(context);
        
        ImageView icon = (ImageView) layout.findViewById(R.id.icon);
        icon.setImageResource(getImage(f.unread > 0));
        
        TextView title = (TextView) layout.findViewById(R.id.title);
        title.setText(formatEntryTitle(f.title, f.unread));
        if (f.unread > 0)
            title.setTypeface(Typeface.DEFAULT_BOLD);
        
        return layout;
    }
    
    // @Override
    // protected void fetchOtherData() {
    // Category category = DBHelper.getInstance().getCategory(categoryId);
    // if (category != null)
    // title = category.title;
    // unreadCount = DBHelper.getInstance().getUnreadCount(categoryId, true);
    // }
    
}
