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
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.IRefreshable;
import org.ttrssreader.model.IUpdatable;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CategoryListAdapter extends BaseAdapter implements IRefreshable, IUpdatable {
    
    private Context mContext;
    
    private List<CategoryItem> mCategories;
    private int mUnreadCount;
    
    public CategoryListAdapter(Context context) {
        mContext = context;
        mCategories = new ArrayList<CategoryItem>();
        mUnreadCount = 0;
    }
    
    @Override
    public int getCount() {
        return mCategories.size();
    }
    
    @Override
    public Object getItem(int position) {
        return mCategories.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    public int getCategoryId(int position) {
        return mCategories.get(position).getId();
    }
    
    public String getCategoryTitle(int position) {
        return mCategories.get(position).getTitle();
    }
    
    public int getUnreadCount(int position) {
        return mCategories.get(position).getUnread();
    }
    
    public int getTotalUnread() {
        return mUnreadCount;
    }
    
    public List<CategoryItem> getCategories() {
        return mCategories;
    }
    
    public void setCategories(List<CategoryItem> categories) {
        this.mCategories = categories;
    }
    
    private String formatTitle(String title, int unread) {
        if (unread > 0) {
            return title + " (" + unread + ")";
        } else {
            return title;
        }
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position >= mCategories.size())
            return new View(mContext);
        
        CategoryListView sv = null;
        CategoryItem c = mCategories.get(position);
        if (convertView == null) {
            sv = new CategoryListView(mContext, c.getTitle(), c.getId(), c.getUnread());
        } else {
            sv = (CategoryListView) convertView;
            sv.setIcon(c.getId(), c.getUnread() > 0);
            sv.setBoldTitleIfNecessary(c.getUnread() > 0);
            sv.setTitle(formatTitle(c.getTitle(), c.getUnread()));
        }
        
        return sv;
    }
    
    private class CategoryListView extends LinearLayout {
        
        public CategoryListView(Context context, String title, int id, int unreadCount) {
            super(context);
            
            this.setOrientation(HORIZONTAL);
            
            // Here we build the child views in code. They could also have
            // been specified in an XML file.
            
            mIcon = new ImageView(context);
            setIcon(id, unreadCount > 0);
            addView(mIcon, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));
            
            mTitle = new TextView(context);
            mTitle.setGravity(Gravity.CENTER_VERTICAL);
            setBoldTitleIfNecessary(unreadCount > 0);
            mTitle.setText(formatTitle(title, unreadCount));
            
            LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
            layoutParams.setMargins(10, 0, 0, 0);
            
            addView(mTitle, layoutParams);
            
        }
        
        public void setTitle(String title) {
            mTitle.setText(title);
        }
        
        public void setBoldTitleIfNecessary(boolean isUnread) {
            if (isUnread) {
                mTitle.setTypeface(Typeface.DEFAULT_BOLD, 1);
            } else {
                mTitle.setTypeface(Typeface.DEFAULT, 0);
            }
        }
        
        public void setIcon(int id, boolean hasUnread) {
            if (id == -1) {
                mIcon.setImageResource(R.drawable.star48);
            } else if (id == -2) {
                mIcon.setImageResource(R.drawable.published48);
            } else if (id == -3) {
                mIcon.setImageResource(R.drawable.fresh48);
            } else if (id == -4) {
                mIcon.setImageResource(R.drawable.all48);
            } else {
                if (hasUnread) {
                    mIcon.setImageResource(R.drawable.categoryunread48);
                } else {
                    mIcon.setImageResource(R.drawable.categoryread48);
                }
            }
        }
        
        private ImageView mIcon;
        private TextView mTitle;
    }
    
    @Override
    public List<?> refreshData() {
        Log.i(Utils.TAG, "CategoryListAdapter     - getCategories()");
        List<CategoryItem> ret = Data.getInstance().getCategories(Controller.getInstance().isDisplayVirtuals());
        
        if (Controller.getInstance().isDisplayOnlyUnread()) {
            List<CategoryItem> temp = new ArrayList<CategoryItem>();
            
            for (CategoryItem ci : ret) {
                if (ci.getId() < 0) {
                    continue; // Virtual Category
                } else if (ci.getUnread() > 0) {
                    temp.add(ci);
                }
            }
            
            ret = temp;
        }
        
        // Update Unread Count
        mUnreadCount = Data.getInstance().getCategoryUnreadCount(-4);
        
        return ret;
    }
    
    @Override
    public void update() {
        Log.i(Utils.TAG, "CategoryListAdapter     - updateCategories()");
        
        if (Controller.getInstance().isUpdateUnreadOnStartup()) {
            Data.getInstance().updateUnreadArticles();
        }
        
        Data.getInstance().updateCategories();
        Data.getInstance().updateVirtualCategories();
    }
    
}
