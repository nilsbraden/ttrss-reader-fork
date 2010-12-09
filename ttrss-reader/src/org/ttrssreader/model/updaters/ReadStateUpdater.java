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

package org.ttrssreader.model.updaters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.IUpdatable;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.model.category.CategoryItem;
import org.ttrssreader.model.feed.FeedItem;
import org.ttrssreader.utils.Utils;
import android.util.Log;

public class ReadStateUpdater implements IUpdatable {
    
    Collection<ArticleItem> mArticles = null;
    private int mPid = 0;
    private int mArticleState;
    
    /**
     * Marks everything as read.
     */
    public ReadStateUpdater() {
        mArticles = new ArrayList<ArticleItem>();
        Map<Integer, Set<ArticleItem>> map = DBHelper.getInstance().getArticles(false);
        for (Integer i : map.keySet()) {
            mArticles.addAll(map.get(i));
        }
        mArticleState = 0;
    }
    
    /**
     * Marks all articles in the given Categories depending on articleState (0 = mark as read, 1 = mark as unread).
     * 
     * @param list
     * @param articleState
     */
    public ReadStateUpdater(Collection<CategoryItem> collection, int articleState) {
        mArticles = new ArrayList<ArticleItem>();
        for (CategoryItem ci : collection) {
            for (FeedItem fi : Data.getInstance().getFeeds(ci.getId())) {
                mArticles.addAll(Data.getInstance().getArticles(fi.getId()));
            }
        }
        mArticleState = articleState;
    }
    
    /**
     * Marks all articles in this category depending on articleState (0 = mark as read, 1 = mark as unread).
     * 
     * @param category
     * @param pid
     * @param articleState
     */
    public ReadStateUpdater(int categoryId, int articleState) {
        mArticles = new ArrayList<ArticleItem>();
        for (FeedItem fi : DBHelper.getInstance().getFeeds(categoryId)) {
            mArticles.addAll(Data.getInstance().getArticles(fi.getId()));
        }
        mPid = categoryId;
        mArticleState = articleState;
    }
    
    /**
     * Marks the given list of articles depending on articleState (0 = mark as read, 1 = mark as unread).
     * 
     * @param list
     * @param pid
     * @param articleState
     */
    public ReadStateUpdater(Collection<ArticleItem> collection, int pid, int articleState) {
        mArticles = collection;
        mPid = pid;
        mArticleState = articleState;
    }
    
    /**
     * Marks the given articles depending on articleState (0 = mark as read, 1 = mark as unread).
     * 
     * @param article
     * @param pid
     * @param articleState
     */
    public ReadStateUpdater(ArticleItem article, int pid, int articleState) {
        mArticles = new ArrayList<ArticleItem>();
        mArticles.add(article);
        mPid = pid;
        mArticleState = articleState;
    }
    
    @Override
    public void update() {
        Log.i(Utils.TAG, "Updating Article-Read-Status...");
        
        boolean boolState = mArticleState == 1 ? true : false;
        int delta = mArticleState == 1 ? 1 : -1;
        int deltaUnread = mArticleState == 1 ? mArticles.size() : -mArticles.size();
        
        Set<Integer> ids = new HashSet<Integer>();
        
        for (ArticleItem article : mArticles) {
            if (mArticleState != 0 && article.isUnread()) {
                continue;
            } else if (mArticleState == 0 && !article.isUnread()) {
                continue;
            }
            
            // Build a list of article ids to update.
            ids.add(article.getId());
            
            // Set ArticleItem-State directly because the Activity uses this object
            article.setUnread(boolState);
            
            int feedId = article.getFeedId();
            FeedItem mFeed = Data.getInstance().getFeed(feedId);
            if (mFeed == null) {
                Log.d(Utils.TAG, "Should never happen but this feed could not be found: " + feedId);
                continue;
            }
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
    
}
