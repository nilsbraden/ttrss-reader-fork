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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
import org.ttrssreader.utils.DateUtils;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FeedHeadlineListAdapter extends BaseAdapter implements IRefreshable, IUpdatable {
    
    private Context mContext;
    
    private int mFeedId;
    
    // Renamed from mFeeds to mArticles because its an Article-List
    private List<ArticleItem> mArticles = null;
    private Set<ArticleItem> mArticlesTemp = null;
    
    public FeedHeadlineListAdapter(Context context, int feedId) {
        mContext = context;
        mFeedId = feedId;
        mArticles = new ArrayList<ArticleItem>();
    }
    
    public void setArticles(List<ArticleItem> articles) {
        this.mArticles = articles;
    }
    
    @Override
    public int getCount() {
        return mArticles.size();
    }
    
    @Override
    public Object getItem(int position) {
        return mArticles.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    public int getFeedItemId(int position) {
        return mArticles.get(position).getId();
    }
    
    public ArrayList<Integer> getFeedItemIds() {
        ArrayList<Integer> result = new ArrayList<Integer>();
        
        for (ArticleItem ai : mArticles) {
            result.add(ai.getId());
        }
        
        return result;
    }
    
    public int getUnreadCount() {
        int result = 0;
        
        Iterator<ArticleItem> iter = mArticles.iterator();
        while (iter.hasNext()) {
            if (iter.next().isUnread()) {
                result++;
            }
        }
        
        return result;
    }
    
    public List<ArticleItem> getArticles() {
        return mArticles;
    }
    
    public List<ArticleItem> getArticleReadList() {
        List<ArticleItem> result = new ArrayList<ArticleItem>();
        
        ArticleItem item;
        Iterator<ArticleItem> iter = mArticles.iterator();
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
        Iterator<ArticleItem> iter = mArticles.iterator();
        while (iter.hasNext()) {
            item = iter.next();
            if (item.isUnread()) {
                result.add(item);
            }
        }
        
        return result;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position >= mArticles.size())
            return new View(mContext);
        
        FeedHeadlineListView sv;
        ArticleItem a = mArticles.get(position);
        if (convertView == null) {
            sv = new FeedHeadlineListView(mContext, a.getTitle(), a.isUnread(), a.getUpdateDate());
        } else {
            sv = (FeedHeadlineListView) convertView;
            sv.setIcon(a.isUnread());
            sv.setBoldTitleIfNecessary(a.isUnread());
            sv.setTitle(a.getTitle());
            sv.setUpdateDate(mContext, a.getUpdateDate());
        }
        
        return sv;
    }
    
    private class FeedHeadlineListView extends LinearLayout {
        
        public FeedHeadlineListView(Context context, String title, boolean isUnread, Date updatedDate) {
            super(context);
            
            this.setOrientation(HORIZONTAL);
            
            // Here we build the child views in code. They could also have
            // been specified in an XML file.
            
            mIcon = new ImageView(context);
            setIcon(isUnread);
            addView(mIcon, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));
            
            LinearLayout textLayout = new LinearLayout(context);
            textLayout.setOrientation(VERTICAL);
            textLayout.setGravity(Gravity.CENTER_VERTICAL);
            
            LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
            layoutParams.setMargins(10, 0, 0, 0);
            
            addView(textLayout, layoutParams);
            
            mTitle = new TextView(context);
            setBoldTitleIfNecessary(isUnread);
            mTitle.setText(title);
            textLayout.addView(mTitle, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            
            mUpdateDate = new TextView(context);
            mUpdateDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            setUpdateDate(context, updatedDate);
            textLayout.addView(mUpdateDate, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
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
        
        public void setIcon(boolean isUnread) {
            if (isUnread) {
                mIcon.setImageResource(R.drawable.articleunread48);
            } else {
                mIcon.setImageResource(R.drawable.articleread48);
            }
        }
        
        public void setUpdateDate(Context context, Date updatedDate) {
            mUpdateDate.setText(DateUtils.getDisplayDate(context, updatedDate));
        }
        
        private ImageView mIcon;
        private TextView mTitle;
        private TextView mUpdateDate;
    }
    
    @Override
    public Set<?> refreshData() {
        Set<ArticleItem> ret;
        
        if (mArticlesTemp != null && mArticlesTemp.size() > 0) {
            List<ArticleItem> articles = new ArrayList<ArticleItem>(mArticlesTemp);
            Collections.sort(articles);
            ret = new LinkedHashSet<ArticleItem>(articles);
            ret.addAll(mArticlesTemp);
            mArticlesTemp = null;
        } else {
            Log.d(Utils.TAG, "Fetching Articles from DB...");
            ret = new LinkedHashSet<ArticleItem>(Data.getInstance().getArticles(mFeedId));
        }

        if (ret != null) {
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
            Log.i(Utils.TAG, "updateArticles(feedId: " + mFeedId + ")");
            mArticlesTemp = Data.getInstance().updateArticles(mFeedId, displayOnlyUnread);
        }
    }
    
}
