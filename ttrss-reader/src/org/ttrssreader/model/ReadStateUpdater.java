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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.model.category.CategoryItem;
import org.ttrssreader.model.feed.FeedItem;
import org.ttrssreader.utils.Utils;
import android.util.Log;

public class ReadStateUpdater implements IUpdatable {
    
    List<ArticleItem> mList = null;
    private int mPid = 0;
    private int mArticleState;
    
    public ReadStateUpdater(List<CategoryItem> list, int articleState) {
        mList = new ArrayList<ArticleItem>();
        for (CategoryItem ci : list) {
            for (FeedItem fi : Data.getInstance().getFeeds(ci.getId())) {
                mList.addAll(Data.getInstance().getArticles(fi.getId()));
            }
        }
        mArticleState = articleState;
    }
    
    public ReadStateUpdater(List<FeedItem> list, int pid, int articleState, boolean isFeedList) {
        mList = new ArrayList<ArticleItem>();
        for (FeedItem fi : list) {
            mList.addAll(Data.getInstance().getArticles(fi.getId()));
        }
        mPid = pid;
        mArticleState = articleState;
    }
    
    public ReadStateUpdater(List<ArticleItem> list, int pid, int articleState) {
        mList = list;
        mPid = pid;
        mArticleState = articleState;
    }
    
    public ReadStateUpdater(ArticleItem article, int pid, int articleState) {
        mList = new ArrayList<ArticleItem>();
        mList.add(article);
        mPid = pid;
        mArticleState = articleState;
    }
    
    @Override
    public void update() {
        Log.i(Utils.TAG, "Updating Article-Read-Status...");
        
        boolean boolState = mArticleState == 1 ? true : false;
        int delta = mArticleState == 1 ? 1 : -1;
        int deltaUnread = mArticleState == 1 ? mList.size() : -mList.size();
        
        Set<Integer> ids = new HashSet<Integer>();
        mList = filterList();
        
        for (ArticleItem article : mList) {
            // Build a list of article ids to update.
            ids.add(article.getId());
            
            // Set ArticleItem-State directly because the Activity uses this object
            article.setUnread(boolState);
            
            int feedId = article.getFeedId();
            FeedItem mFeed = Data.getInstance().getFeed(feedId);
            if (mFeed == null)
                continue;
            
            int categoryId = mFeed.getCategoryId();
            
            DBHelper.getInstance().updateFeedDeltaUnreadCount(feedId, delta);
            DBHelper.getInstance().updateCategoryDeltaUnreadCount(categoryId, delta);
            
            // Check if its a fresh article to modify that count too
            long ms = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
            Date d = new Date(ms);
            if (article.getUpdateDate().after(d)) {
                DBHelper.getInstance().updateCategoryDeltaUnreadCount(-3, deltaUnread);
            }
        }
        
        if (ids.size() > 0) {
            Controller.getInstance().getConnector().setArticleRead(ids, mArticleState);
            DBHelper.getInstance().markArticlesRead(ids, mArticleState);
            
            // If on a virtual category also update article state in it.
            if (mPid < 0 && mPid > -4) {
                Log.d(Utils.TAG, "Delta-Unread: " + deltaUnread + " Virtual-Category: " + mPid);
                DBHelper.getInstance().updateCategoryDeltaUnreadCount(mPid, deltaUnread);
            }
            Log.d(Utils.TAG, "Delta-Unread: " + deltaUnread + " mPid: " + mPid);
            DBHelper.getInstance().updateCategoryDeltaUnreadCount(-4, deltaUnread);
        }
    }
    
    public List<ArticleItem> filterList() {
        List<ArticleItem> ret = new ArrayList<ArticleItem>();
        
        for (ArticleItem a : mList) {
            if (mArticleState == 0 && a.isUnread()) {
                ret.add(a);
            } else if (mArticleState != 0 && !a.isUnread()) {
                ret.add(a);
            }
        }
        
        return ret;
    }
    
}
