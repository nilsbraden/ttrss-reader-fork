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
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.model.pojos.Category;
import android.content.Context;
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
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Category c = new Category();
            c.id = cursor.getInt(0);
            c.title = cursor.getString(1);
            c.unread = cursor.getInt(2);
            result.add(c);
            cursor.move(1);
        }
        return result;
    }
    
    private int getImage(int id, boolean unread) {
        if (id == -1) {
            return R.drawable.star48;
        } else if (id == -2) {
            return R.drawable.published48;
        } else if (id == -3) {
            return R.drawable.fresh48;
        } else if (id == -4) {
            return R.drawable.all48;
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
    
    protected String buildQuery(boolean overrideDisplayUnread) {
        StringBuilder query = new StringBuilder();

        boolean displayUnread = displayOnlyUnread;
        if (overrideDisplayUnread)
            displayUnread = false;
        
        // Virtual Feeds
        query.append("SELECT id,title,unread FROM (SELECT id,title,unread FROM ");
        query.append(DBHelper.TABLE_CATEGORIES);
        query.append(" WHERE id<0 ORDER BY id) AS a ");
        query.append(" UNION ");
        
        // "Uncetegorized Feeds"
        query.append("SELECT id,title,unread FROM (SELECT id,title,unread FROM ");
        query.append(DBHelper.TABLE_CATEGORIES);
        query.append(" WHERE id=0 ");
        query.append(displayUnread ? " AND unread>0 " : "");
        query.append(" ) AS b ");
        query.append(" UNION ");
        
        // Categories
        query.append(" SELECT id,title,unread FROM (SELECT id,title,unread FROM ");
        query.append(DBHelper.TABLE_CATEGORIES);
        query.append(" WHERE id>0 ");
        query.append(displayUnread ? " AND unread>0 " : "");
        query.append(" ORDER BY UPPER(title) ");
        query.append(invertSortFeedCats ? "ASC" : "DESC");
        query.append(") AS c");
        
        return query.toString();
    }
    
}
