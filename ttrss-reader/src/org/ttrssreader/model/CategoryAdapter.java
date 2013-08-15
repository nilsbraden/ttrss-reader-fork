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
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
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
        Category ret = new Category();
        Object[] o = content.get(position);
        ret.id = (Integer) o[POS_ID];
        ret.title = (String) o[POS_TITLE];
        ret.unread = (Integer) o[POS_UNREAD];
        return ret;
    }
    
    public List<Category> getCategories() {
        List<Category> result = new ArrayList<Category>(content.size());
        for (Object[] o : content) {
            Category ret = new Category();
            ret.id = (Integer) o[POS_ID];
            ret.title = (String) o[POS_TITLE];
            ret.unread = (Integer) o[POS_UNREAD];
            result.add(ret);
        }
        return result;
    }
    
    private int getImage(int id, boolean unread) {
        if (id == Data.VCAT_STAR) {
            return R.drawable.star48;
        } else if (id == Data.VCAT_PUB) {
            return R.drawable.published48;
        } else if (id == Data.VCAT_FRESH) {
            return R.drawable.fresh48;
        } else if (id == Data.VCAT_ALL) {
            return R.drawable.all48;
        } else if (id < -10) {
            if (unread) {
                return R.drawable.label;
            } else {
                return R.drawable.label_read;
            }
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
        
        if (layout == null)
            return new View(context);
        
        ImageView icon = (ImageView) layout.findViewById(R.id.cat_icon);
        icon.setImageResource(getImage(c.id, c.unread > 0));
        
        TextView title = (TextView) layout.findViewById(R.id.cat_title);
        title.setText(formatEntryTitle(c.title, c.unread));
        if (c.unread > 0) {
            title.setTypeface(Typeface.DEFAULT_BOLD, 1);
        } else {
            title.setTypeface(Typeface.DEFAULT, 0);
        }
        
        return layout;
    }
    
    protected ArrayList<Object[]> executeQuery(boolean overrideDisplayUnread, boolean buildSafeQuery) {
        boolean displayUnread = Controller.getInstance().onlyUnread();
        boolean invertSortFeedCats = Controller.getInstance().invertSortFeedscats();
        
        if (overrideDisplayUnread)
            displayUnread = false;
        
        ArrayList<Object[]> ret = new ArrayList<Object[]>();
        StringBuilder query;
        
        // Virtual Feeds
        if (Controller.getInstance().showVirtual()) {
            query = new StringBuilder();
            query.append("SELECT id,title,unread FROM ");
            query.append(DBHelper.TABLE_CATEGORIES);
            query.append(" WHERE id>=-4 AND id<0 ORDER BY id");
            DBHelper.getInstance().queryCategories(ret, query.toString());
        }
        
        // Labels
        query = new StringBuilder();
        query.append("SELECT id,title,unread FROM ");
        query.append(DBHelper.TABLE_FEEDS);
        query.append(" WHERE id<-10");
        query.append(displayUnread ? " AND unread>0" : "");
        query.append(" ORDER BY UPPER(title) ASC");
        query.append(" LIMIT 1000");
        DBHelper.getInstance().queryCategories(ret, query.toString());
        
        // "Uncategorized Feeds"
        query = new StringBuilder();
        query.append("SELECT id,title,unread FROM ");
        query.append(DBHelper.TABLE_CATEGORIES);
        query.append(" WHERE id=0");
        DBHelper.getInstance().queryCategories(ret, query.toString());
        
        // Categories
        query = new StringBuilder();
        query.append("SELECT id,title,unread FROM ");
        query.append(DBHelper.TABLE_CATEGORIES);
        query.append(" WHERE id>0");
        query.append(displayUnread ? " AND unread>0" : "");
        query.append(" ORDER BY UPPER(title) ");
        query.append(invertSortFeedCats ? "DESC" : "ASC");
        query.append(" LIMIT 1000");
        DBHelper.getInstance().queryCategories(ret, query.toString());
        
        return ret;
    }
    
}
