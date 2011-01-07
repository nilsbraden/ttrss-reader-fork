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

package org.ttrssreader.model.feedheadline;

import java.text.DateFormat;
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
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.model.feed.FeedItem;
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

public class FeedHeadlineListAdapter extends BaseAdapter implements IRefreshable, IUpdatable {
    
    private Context context;
    
    private int feedId;
    
    // Renamed from mFeeds to mArticles because its an Article-List
    private List<ArticleItem> articles = null;
    private Set<ArticleItem> articlesTemp = null;
    
    public FeedHeadlineListAdapter(Context context, int feedId) {
        this.context = context;
        this.feedId = feedId;
        this.articles = new ArrayList<ArticleItem>();
    }
    
    public void setArticles(List<ArticleItem> articles) {
        this.articles = articles;
    }
    
    @Override
    public int getCount() {
        return articles.size();
    }
    
    @Override
    public Object getItem(int position) {
        return articles.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    public int getFeedItemId(int position) {
        return articles.get(position).getId();
    }
    
    public ArrayList<Integer> getFeedItemIds() {
        ArrayList<Integer> result = new ArrayList<Integer>();
        
        for (ArticleItem ai : articles) {
            result.add(ai.getId());
        }
        
        return result;
    }
    
    public int getUnreadCount() {
        int result = 0;
        
        Iterator<ArticleItem> iter = articles.iterator();
        while (iter.hasNext()) {
            if (iter.next().isUnread()) {
                result++;
            }
        }
        
        return result;
    }
    
    public List<ArticleItem> getArticles() {
        return articles;
    }
    
    public List<ArticleItem> getArticleReadList() {
        List<ArticleItem> result = new ArrayList<ArticleItem>();
        
        ArticleItem item;
        Iterator<ArticleItem> iter = articles.iterator();
        while (iter.hasNext()) {
            item = iter.next();
            if (!item.isUnread()) {
                result.add(item);
            }
        }
        
        return result;
    }
    
    public List<ArticleItem> getArticleUnreadList() {
        List<ArticleItem> result = new ArrayList<ArticleItem>();
        
        ArticleItem item;
        Iterator<ArticleItem> iter = articles.iterator();
        while (iter.hasNext()) {
            item = iter.next();
            if (item.isUnread()) {
                result.add(item);
            }
        }
        
        return result;
    }
    
    private void getImage(ImageView icon, ArticleItem a) {
        if (a.isUnread()) {
            icon.setBackgroundResource(R.drawable.articleunread48);
        } else {
            icon.setBackgroundResource(R.drawable.articleread48);
        }
        
        if (a.isStarred() && a.isPublished()) {
            icon.setImageResource(R.drawable.published_and_starred48);
        } else if (a.isStarred()) {
            icon.setImageResource(R.drawable.star_yellow48);
        } else if (a.isPublished()) {
            icon.setImageResource(R.drawable.published_blue48);
        } else {
            icon.setBackgroundDrawable(null);
            if (a.isUnread()) {
                icon.setImageResource(R.drawable.articleunread48);
            } else {
                icon.setImageResource(R.drawable.articleread48);
            }
        }
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position >= articles.size())
            return new View(context);
        
        ArticleItem a = articles.get(position);
        
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout layout = null;
        if (convertView == null) {
            layout = (LinearLayout) inflater.inflate(R.layout.feedheadlineitem, null);
        } else {
            if (convertView instanceof LinearLayout) {
                layout = (LinearLayout) convertView;
            }
        }
        
        // TODO: Find a way to overlay more than 2 images
        ImageView icon = (ImageView) layout.findViewById(R.id.icon);
        getImage(icon, a);
        
        TextView title = (TextView) layout.findViewById(R.id.title);
        title.setText(a.getTitle());
        if (a.isUnread()) {
            title.setTypeface(Typeface.DEFAULT_BOLD, 1);
        } else {
            title.setTypeface(Typeface.DEFAULT, 0);
        }
        
        TextView updateDate = (TextView) layout.findViewById(R.id.updateDate);
        String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(a.getUpdateDate());
        updateDate.setText(date);
        
        TextView dataSource = (TextView) layout.findViewById(R.id.dataSource);
        if (feedId < 0 && feedId >= -4) {
            FeedItem f = Data.getInstance().getFeed(a.getFeedId());
            if (f != null) {
                dataSource.setText(f.getTitle());
            }
        }
        
        return layout;
    }
    
    @Override
    public Set<?> refreshData() {
        Set<ArticleItem> ret;
        
        if (articlesTemp != null && articlesTemp.size() > 0) {
            List<ArticleItem> articles = new ArrayList<ArticleItem>(articlesTemp);
            Collections.sort(articles);
            ret = new LinkedHashSet<ArticleItem>(articles);
            ret.addAll(articlesTemp);
            articlesTemp = null;
        } else {
            Log.d(Utils.TAG, "Fetching Articles from DB...");
            ret = new LinkedHashSet<ArticleItem>(Data.getInstance().getArticles(feedId));
        }
        
        if (ret != null && (feedId < 0 && feedId >= -3)) {
            // We want all articles for starred (-1) and published (-2) and fresh (-3)
            if (Controller.getInstance().isDisplayOnlyUnread()) {
                Set<ArticleItem> temp = new LinkedHashSet<ArticleItem>();
                
                for (ArticleItem ai : ret) {
                    if (ai.isUnread()) {
                        temp.add(ai);
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
            boolean displayOnlyUnread = Controller.getInstance().isDisplayOnlyUnread();
            Log.i(Utils.TAG, "updateArticles(feedId: " + feedId + ")");
            articlesTemp = Data.getInstance().updateArticles(feedId, displayOnlyUnread);
        }
    }
    
}
