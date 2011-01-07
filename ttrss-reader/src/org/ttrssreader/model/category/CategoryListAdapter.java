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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.IRefreshable;
import org.ttrssreader.model.IUpdatable;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CategoryListAdapter extends BaseAdapter implements IRefreshable, IUpdatable {
    
    private Context context;
    
    private int unreadCount;
    
    private List<CategoryItem> categories;
    private Set<CategoryItem> categoriesTemp;
    
    public CategoryListAdapter(Context context) {
        this.context = context;
        this.categories = new ArrayList<CategoryItem>();
        this.unreadCount = 0;
    }
    
    @Override
    public int getCount() {
        return categories.size();
    }
    
    @Override
    public Object getItem(int position) {
        return categories.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    public int getCategoryId(int position) {
        return categories.get(position).getId();
    }
    
    public String getCategoryTitle(int position) {
        return categories.get(position).getTitle();
    }
    
    public int getUnreadCount(int position) {
        return categories.get(position).getUnread();
    }
    
    public int getTotalUnread() {
        return unreadCount;
    }
    
    public List<CategoryItem> getCategories() {
        return categories;
    }
    
    public void setCategories(List<CategoryItem> categories) {
        this.categories = categories;
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
        if (position >= categories.size())
            return new View(context);
        
        CategoryItem c = categories.get(position);
        
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
    
    @Override
    public Set<?> refreshData() {
        Set<CategoryItem> ret;
        
        if (categoriesTemp != null) {
            List<CategoryItem> cats = new ArrayList<CategoryItem>(categoriesTemp);
            Collections.sort(cats);
            ret = new LinkedHashSet<CategoryItem>(cats);
            categoriesTemp = null;
        } else {
            ret = Data.getInstance().getCategories(Controller.getInstance().isDisplayVirtuals());
        }
        
        if (Controller.getInstance().isDisplayOnlyUnread()) {
            Set<CategoryItem> temp = new LinkedHashSet<CategoryItem>();
            
            for (CategoryItem ci : ret) {
                if (ci.getId() < 0) {
                    temp.add(ci); // Virtual Category
                } else if (ci.getUnread() > 0) {
                    temp.add(ci);
                }
            }
            
            ret = temp;
        }
        
        // Fetch new overall Unread-Count
        unreadCount = Data.getInstance().getCategoryUnreadCount(-4);
        
        return ret;
    }
    
    @Override
    public void update() {
        if (Controller.getInstance().isWorkOffline())
            return;
        
        // Update counters
        Data.getInstance().updateCounters();
        Log.i(Utils.TAG, "CategoryListAdapter - updateCounters()");
        
        if (Controller.getInstance().isUpdateUnreadOnStartup()) {
            Log.i(Utils.TAG, "CategoryListAdapter - updateUnreadArticles()");
            Data.getInstance().updateUnreadArticles();
        }
        
        if (categoriesTemp == null) {
            categoriesTemp = new LinkedHashSet<CategoryItem>();
            
            Log.i(Utils.TAG, "CategoryListAdapter - updateCategories()");
            Set<CategoryItem> cats = Data.getInstance().updateCategories();
            if (cats != null && cats.size() > 0 && categoriesTemp != null)
                categoriesTemp.addAll(cats);
            
            Log.i(Utils.TAG, "CategoryListAdapter - updateVirtualCategories()");
            Set<CategoryItem> vCats = Data.getInstance().updateVirtualCategories();
            if (vCats != null && vCats.size() > 0 && categoriesTemp != null)
                categoriesTemp.addAll(vCats);
            
            if (categoriesTemp != null && categoriesTemp.isEmpty())
                categoriesTemp = null;
        }
    }
    
}
