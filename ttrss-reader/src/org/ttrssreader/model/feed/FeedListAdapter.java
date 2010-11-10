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

package org.ttrssreader.model.feed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FeedListAdapter extends BaseAdapter implements IRefreshable, IUpdatable {
    
    private Context mContext;
    private int mCategoryId;
    
    private List<FeedItem> mFeeds;
    private Set<FeedItem> mFeedsTemp;
    
    public FeedListAdapter(Context context, int categoryId) {
        mContext = context;
        mCategoryId = categoryId;
        mFeeds = new ArrayList<FeedItem>();
    }
    
    public void setFeeds(List<FeedItem> feeds) {
        this.mFeeds = feeds;
    }
    
    @Override
    public int getCount() {
        return mFeeds.size();
    }
    
    @Override
    public Object getItem(int position) {
        return mFeeds.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    public int getFeedId(int position) {
        return mFeeds.get(position).getId();
    }
    
    public String getFeedTitle(int position) {
        return mFeeds.get(position).getTitle();
    }
    
    public int getUnread(int position) {
        return mFeeds.get(position).getUnread();
    }
    
    public List<FeedItem> getFeeds() {
        return mFeeds;
    }
    
    public ArrayList<Integer> getFeedIds() {
        ArrayList<Integer> ret = new ArrayList<Integer>();
        
        for (FeedItem f : mFeeds) {
            ret.add(f.getId());
        }
        return ret;
    }
    
    public ArrayList<String> getFeedNames() {
        ArrayList<String> ret = new ArrayList<String>();
        
        for (FeedItem f : mFeeds) {
            ret.add(f.getTitle());
        }
        return ret;
    }
    
    public int getTotalUnreadCount() {
        int result = 0;
        
        Iterator<FeedItem> iter = mFeeds.iterator();
        while (iter.hasNext()) {
            result += iter.next().getUnread();
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
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position >= mFeeds.size())
            return new View(mContext);
        
        FeedListView sv = null;
        FeedItem f = mFeeds.get(position);
        if (convertView == null) {
            sv = new FeedListView(mContext, f.getTitle(), f.getUnread());
        } else {
            if (convertView instanceof FeedListView) {
                sv = (FeedListView) convertView;
                sv.setIcon(f.getUnread() > 0);
                sv.setBoldTitleIfNecessary(f.getUnread() > 0);
                sv.setTitle(formatTitle(f.getTitle(), f.getUnread()));
            }
        }
        
        return sv;
    }
    
    private class FeedListView extends LinearLayout {
        
        public FeedListView(Context context, String title, int unread) {
            super(context);
            
            this.setOrientation(HORIZONTAL);
            
            // Here we build the child views in code. They could also have
            // been specified in an XML file.
            
            mIcon = new ImageView(context);
            setIcon(unread > 0);
            addView(mIcon, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));
            
            mTitle = new TextView(context);
            mTitle.setGravity(Gravity.CENTER_VERTICAL);
            setBoldTitleIfNecessary(unread > 0);
            mTitle.setText(formatTitle(title, unread));
            
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
        
        public void setIcon(boolean hasUnread) {
            if (hasUnread) {
                mIcon.setImageResource(R.drawable.feedheadlinesunread48);
            } else {
                mIcon.setImageResource(R.drawable.feedheadlinesread48);
            }
        }
        
        private ImageView mIcon;
        private TextView mTitle;
    }
    
    @Override
    public Set<?> refreshData() {
        Set<FeedItem> ret;
        
        if (mFeedsTemp != null && mFeedsTemp.size() > 0) {
            List<FeedItem> feeds = new ArrayList<FeedItem>(mFeedsTemp);
            Collections.sort(feeds);
            ret = new LinkedHashSet<FeedItem>(feeds);
            ret.addAll(mFeedsTemp);
            mFeedsTemp = null;
        } else {
            Log.d(Utils.TAG, "Fetching Feeds from DB...");
            ret = new LinkedHashSet<FeedItem>(Data.getInstance().getFeeds(mCategoryId));
        }
        
        if (ret != null) {
            if (Controller.getInstance().isDisplayOnlyUnread()) {
                Set<FeedItem> temp = new LinkedHashSet<FeedItem>();
                
                for (FeedItem fi : ret) {
                    if (fi.getUnread() > 0) {
                        temp.add(fi);
                    }
                }
                ret = temp;
            }
        }
        
        return ret;
    }
    
    @Override
    public void update() {
        if (!Controller.getInstance().isWorkOffline()) {
            Log.i(Utils.TAG, "updateFeeds(catId: " + mCategoryId + ")");
            mFeedsTemp = Data.getInstance().updateFeeds(mCategoryId);
        }
    }
    
}
