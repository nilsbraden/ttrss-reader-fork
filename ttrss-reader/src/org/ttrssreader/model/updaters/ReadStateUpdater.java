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
    
    private int mPid = 0;
    private int mArticleState;
    
    private Collection<CategoryItem> categories = null;
    private Collection<FeedItem> feeds = null;
    private Collection<ArticleItem> articles = null;
    
    public ReadStateUpdater(Collection<CategoryItem> collection) {
        categories = new HashSet<CategoryItem>(collection);
    }
    
    public ReadStateUpdater(int categoryId) {
        categories = new HashSet<CategoryItem>();
        CategoryItem ci = Data.getInstance().getCategory(categoryId);
        if (ci != null) {
            categories.add(ci);
        }
    }
    
    public ReadStateUpdater(int feedId, int dummy) {
        
        if (feedId <= 0) { // Virtual Category...
            categories = new HashSet<CategoryItem>();
            CategoryItem ci = Data.getInstance().getCategory(feedId);
            if (ci != null) {
                categories.add(ci);
            }
        }
        
        feeds = new HashSet<FeedItem>();
        FeedItem fi = Data.getInstance().getFeed(feedId);
        if (fi != null) {
            feeds.add(fi);
        }
    }
    
    /* articleState: 0 = mark as read, 1 = mark as unread */
    public ReadStateUpdater(ArticleItem article, int pid, int articleState) {
        articles = new ArrayList<ArticleItem>();
        articles.add(article);
        mPid = pid;
        mArticleState = articleState;
    }
    
    @Override
    public void update() {
        if (categories != null) {
            
            for (CategoryItem ci : categories) {
                Controller.getInstance().getConnector().setRead(ci.getId(), true);
                DBHelper.getInstance().markCategoryRead(ci, true);
            }
            Data.getInstance().updateCounters();
            
        } else if (feeds != null) {
            
            for (FeedItem fi : feeds) {
                Controller.getInstance().getConnector().setRead(fi.getId(), false);
                DBHelper.getInstance().markFeedRead(fi, true);
            }
            Data.getInstance().updateCounters();
            
        } else if (articles != null) {
            
            Log.i(Utils.TAG, "Updating Article-Read-Status...");
            
            boolean boolState = mArticleState == 1 ? true : false;
            int delta = mArticleState == 1 ? 1 : -1;
            int deltaUnread = mArticleState == 1 ? articles.size() : -articles.size();
            
            Set<Integer> ids = new HashSet<Integer>();
            
            for (ArticleItem article : articles) {
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
                int categoryId = mFeed.getCategoryId();
                
                DBHelper.getInstance().updateFeedDeltaUnreadCount(feedId, delta);
                DBHelper.getInstance().updateCategoryDeltaUnreadCount(categoryId, delta);
                
                // Check if is a fresh article and modify that count too
                long ms = System.currentTimeMillis() - Controller.getInstance().getFreshArticleMaxAge();
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
    
}
