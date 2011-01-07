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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FeedListAdapter extends BaseAdapter implements IRefreshable, IUpdatable {
    
    private Context context;
    
    private int categoryId;
    
    private List<FeedItem> feeds;
    private Set<FeedItem> feedsTemp;
    
    public FeedListAdapter(Context context, int categoryId) {
        this.context = context;
        this.categoryId = categoryId;
        this.feeds = new ArrayList<FeedItem>();
    }
    
    public void setFeeds(List<FeedItem> feeds) {
        this.feeds = feeds;
    }
    
    @Override
    public int getCount() {
        return feeds.size();
    }
    
    @Override
    public Object getItem(int position) {
        return feeds.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    public int getFeedId(int position) {
        return feeds.get(position).getId();
    }
    
    public String getFeedTitle(int position) {
        return feeds.get(position).getTitle();
    }
    
    public int getUnread(int position) {
        return feeds.get(position).getUnread();
    }
    
    public List<FeedItem> getFeeds() {
        return feeds;
    }
    
    public ArrayList<Integer> getFeedIds() {
        ArrayList<Integer> ret = new ArrayList<Integer>();
        
        for (FeedItem f : feeds) {
            ret.add(f.getId());
        }
        return ret;
    }
    
    public ArrayList<String> getFeedNames() {
        ArrayList<String> ret = new ArrayList<String>();
        
        for (FeedItem f : feeds) {
            ret.add(f.getTitle());
        }
        return ret;
    }
    
    public int getTotalUnreadCount() {
        int result = 0;
        
        Iterator<FeedItem> iter = feeds.iterator();
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
    
    private int getImage(boolean unread) {
        if (unread) {
            return R.drawable.feedheadlinesunread48;
        } else {
            return R.drawable.feedheadlinesread48;
        }
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position >= feeds.size())
            return new View(context);
        
        FeedItem f = feeds.get(position);
        
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
    
    @Override
    public Set<?> refreshData() {
        Set<FeedItem> ret;
        
        if (feedsTemp != null && feedsTemp.size() > 0) {
            List<FeedItem> feeds = new ArrayList<FeedItem>(feedsTemp);
            Collections.sort(feeds);
            ret = new LinkedHashSet<FeedItem>(feeds);
            ret.addAll(feedsTemp);
            feedsTemp = null;
        } else {
            Log.d(Utils.TAG, "Fetching Feeds from DB...");
            ret = new LinkedHashSet<FeedItem>(Data.getInstance().getFeeds(categoryId));
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
            Log.i(Utils.TAG, "updateFeeds(catId: " + categoryId + ")");
            feedsTemp = Data.getInstance().updateFeeds(categoryId);
        }
    }
    
}
