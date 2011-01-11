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

package org.ttrssreader.model.category;

import java.util.ArrayList;
import java.util.List;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.IUpdatable;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CategoryListAdapter extends BaseAdapter implements IUpdatable {
    
    private Context context;
    
    private int unreadCount;
    
    private volatile Cursor cursor;
    private boolean displayOnlyUnread;
    
    public CategoryListAdapter(Context context) {
        this.context = context;
        this.unreadCount = 0;
        makeQuery();
    }
    
    @Override
    public int getCount() {
        if (cursor.isClosed()) {
            Log.v(Utils.TAG, "CURSOR REQUERY");
            makeQuery();
        }
        
        return cursor.getCount();
    }
    
    @Override
    public Object getItem(int position) {
        if (cursor.isClosed()) {
            Log.v(Utils.TAG, "CURSOR REQUERY");
            makeQuery();
        }
        
        CategoryItem ret = null;
        if (cursor.getCount() >= position) {
            if (cursor.moveToPosition(position)) {
                ret = new CategoryItem();
                ret.setId(cursor.getInt(0));
                ret.setTitle(cursor.getString(1));
                ret.setUnread(cursor.getInt(2));
            }
        }
        return ret;
    }
    
    @Override
    public long getItemId(int position) {
        if (cursor.isClosed()) {
            Log.v(Utils.TAG, "CURSOR REQUERY");
            makeQuery();
        }
        
        if (cursor.getCount() >= position) {
            if (cursor.moveToPosition(position)) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }
    
    public int getCategoryId(int position) {
        if (cursor.isClosed()) {
            Log.v(Utils.TAG, "CURSOR REQUERY");
            makeQuery();
        }
        
        if (cursor.getCount() >= position) {
            if (cursor.moveToPosition(position)) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }
    
    public String getCategoryTitle(int position) {
        if (cursor.isClosed()) {
            Log.v(Utils.TAG, "CURSOR REQUERY");
            makeQuery();
        }
        
        if (cursor.getCount() >= position) {
            if (cursor.moveToPosition(position)) {
                return cursor.getString(1);
            }
        }
        return "";
    }
    
    public int getTotalUnread() {
        return unreadCount;
    }
    
    public List<CategoryItem> getCategories() {
        if (cursor.isClosed()) {
            Log.v(Utils.TAG, "CURSOR REQUERY");
            makeQuery();
        }
        
        List<CategoryItem> result = new ArrayList<CategoryItem>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            CategoryItem c = new CategoryItem();
            c.setId(cursor.getInt(0));
            c.setTitle(cursor.getString(1));
            c.setUnread(cursor.getInt(2));
            result.add(c);
            cursor.move(1);
        }
        return result;
    }
    
    private String formatTitle(String title, int unread) {
        if (unread > 0) {
            return title + " (" + unread + ")";
        } else {
            return title;
        }
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
        
        CategoryItem c = (CategoryItem) getItem(position);
        
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
        icon.setImageResource(getImage(c.getId(), c.getUnread() > 0));
        
        TextView title = (TextView) layout.findViewById(R.id.title);
        title.setText(formatTitle(c.getTitle(), c.getUnread()));
        if (c.getUnread() > 0) {
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
        
        query.append("SELECT id,title,unread FROM (SELECT id,title,unread FROM ");
        query.append(DBHelper.TABLE_CATEGORIES);
        query.append(" WHERE id<=0 ORDER BY id) AS a UNION SELECT id,title,unread FROM (SELECT id,title,unread FROM ");
        query.append(DBHelper.TABLE_CATEGORIES);
        query.append(" WHERE id>0");
        if (displayOnlyUnread) {
            query.append(" AND unread>0");
        }
        query.append(" ORDER BY UPPER(title) DESC) AS b");
        
        Log.d(Utils.TAG, query.toString());
        cursor = DBHelper.getInstance().query(query.toString(), null);
    }
    
    @Override
    public void update() {
        unreadCount = Data.getInstance().getCategoryUnreadCount(-4);
        Data.getInstance().updateCounters();
        Data.getInstance().updateCategories();
        Data.getInstance().updateVirtualCategories();
    }
    
}
