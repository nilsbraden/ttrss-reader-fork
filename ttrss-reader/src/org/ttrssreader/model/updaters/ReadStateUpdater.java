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
import org.ttrssreader.model.pojos.ArticleItem;
import org.ttrssreader.model.pojos.CategoryItem;
import org.ttrssreader.model.pojos.FeedItem;

public class ReadStateUpdater implements IUpdatable {
    
    private int pid = 0;
    private int articleState;
    
    private Collection<CategoryItem> categories = null;
    private Collection<FeedItem> feeds = null;
    private Collection<ArticleItem> articles = null;
    
    public ReadStateUpdater(Collection<CategoryItem> collection) {
        this.categories = new HashSet<CategoryItem>(collection);
    }
    
    public ReadStateUpdater(int categoryId) {
        this.categories = new HashSet<CategoryItem>();
        CategoryItem ci = DBHelper.getInstance().getCategory(categoryId);
        if (ci != null) {
            this.categories.add(ci);
        }
    }
    
    public ReadStateUpdater(int feedId, int dummy) {
        if (feedId <= 0) { // Virtual Category...
            this.categories = new HashSet<CategoryItem>();
            CategoryItem ci = DBHelper.getInstance().getCategory(feedId);
            if (ci != null)
                this.categories.add(ci);
        } else {
            this.feeds = new HashSet<FeedItem>();
            FeedItem fi = DBHelper.getInstance().getFeed(feedId);
            if (fi != null)
                this.feeds.add(fi);
        }
    }
    
    /* articleState: 0 = mark as read, 1 = mark as unread */
    public ReadStateUpdater(ArticleItem article, int pid, int articleState) {
        this.articles = new ArrayList<ArticleItem>();
        this.articles.add(article);
        this.pid = pid;
        this.articleState = articleState;
    }
    
    @Override
    public void update() {
        if (categories != null) {
            
            for (CategoryItem ci : categories) {
                if (ci.id >= 0) {
                    Data.getInstance().setRead(ci.id, true);
                } else {
                    // Virtual Categories are actually Feeds (the server handles them as such) so we have to set isCat
                    // to false here
                    Data.getInstance().setRead(ci.id, false);
                }
                DBHelper.getInstance().markCategoryRead(ci, true);
            }
            Data.getInstance().updateCounters(false);
            
        } else if (feeds != null) {
            
            for (FeedItem fi : feeds) {
                Data.getInstance().setRead(fi.id, false);
                DBHelper.getInstance().markFeedRead(fi, true);
            }
            Data.getInstance().updateCounters(false);
            
        } else if (articles != null) {
            
            boolean boolState = articleState == 1 ? true : false;
            int delta = articleState == 1 ? 1 : -1;
            int deltaUnread = articleState == 1 ? articles.size() : -articles.size();
            
            Set<Integer> ids = new HashSet<Integer>();
            
            for (ArticleItem article : articles) {
                if (articleState != 0 && article.isUnread) {
                    continue;
                } else if (articleState == 0 && !article.isUnread) {
                    continue;
                }
                
                // Build a list of article ids to update.
                ids.add(article.id);
                
                // Set ArticleItem-State directly because the Activity uses this object
                article.isUnread = boolState;
                
                int feedId = article.feedId;
                FeedItem mFeed = DBHelper.getInstance().getFeed(feedId);
                int categoryId = mFeed.categoryId;
                
                DBHelper.getInstance().updateFeedDeltaUnreadCount(feedId, delta);
                DBHelper.getInstance().updateCategoryDeltaUnreadCount(categoryId, delta);
                
                // Check if is a fresh article and modify that count too
                long ms = System.currentTimeMillis() - Controller.getInstance().getFreshArticleMaxAge();
                Date d = new Date(ms);
                if (article.updated.after(d)) {
                    DBHelper.getInstance().updateCategoryDeltaUnreadCount(-3, deltaUnread);
                }
            }
            
            if (ids.size() > 0) {
                Data.getInstance().setArticleRead(ids, articleState);
                DBHelper.getInstance().markArticlesRead(ids, articleState);
                
                // If on a virtual category also update article state in it.
                if (pid < 0 && pid > -4) {
                    DBHelper.getInstance().updateCategoryDeltaUnreadCount(pid, deltaUnread);
                }
                DBHelper.getInstance().updateCategoryDeltaUnreadCount(-4, deltaUnread);
            }
            
        }
    }
    
}
