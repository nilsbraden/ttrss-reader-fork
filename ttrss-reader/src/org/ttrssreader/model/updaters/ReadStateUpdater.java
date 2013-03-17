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
import org.ttrssreader.controllers.UpdateController;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.pojos.Feed;

public class ReadStateUpdater implements IUpdatable {
    
    private int pid = 0;
    private int articleState;
    
    private Collection<Category> categories = null;
    private Collection<Feed> feeds = null;
    private Collection<Article> articles = null;
    
    public ReadStateUpdater(Collection<Category> collection) {
        this.categories = new HashSet<Category>(collection);
    }
    
    public ReadStateUpdater(int categoryId) {
        this.categories = new HashSet<Category>();
        Category ci = DBHelper.getInstance().getCategory(categoryId);
        if (ci != null) {
            this.categories.add(ci);
        }
    }
    
    public ReadStateUpdater(int feedId, int dummy) {
        if (feedId <= 0 && feedId >= -4) { // Virtual Category...
            this.categories = new HashSet<Category>();
            Category ci = DBHelper.getInstance().getCategory(feedId);
            if (ci != null)
                this.categories.add(ci);
        } else {
            this.feeds = new HashSet<Feed>();
            Feed fi = DBHelper.getInstance().getFeed(feedId);
            if (fi != null)
                this.feeds.add(fi);
        }
    }
    
    /* articleState: 0 = mark as read, 1 = mark as unread */
    public ReadStateUpdater(Article article, int pid, int articleState) {
        this.articles = new ArrayList<Article>();
        this.articles.add(article);
        this.pid = pid;
        this.articleState = articleState;
    }
    
    /* articleState: 0 = mark as read, 1 = mark as unread */
    public ReadStateUpdater(Collection<Article> articles, int pid, int articleState) {
        this.articles = new ArrayList<Article>();
        this.articles.addAll(articles);
        this.pid = pid;
        this.articleState = articleState;
    }
    
    @Override
    public void update(Updater parent) {
        if (categories != null) {
            
            for (Category ci : categories) {
                // VirtualCats are actually Feeds (the server handles them as such) so we have to set isCat to false
                if (ci.id >= 0) {
                    Data.getInstance().setRead(ci.id, true);
                } else {
                    Data.getInstance().setRead(ci.id, false);
                }
            }
            
        } else if (feeds != null) {
            
            for (Feed fi : feeds) {
                Data.getInstance().setRead(fi.id, false);
            }
            
        } else if (articles != null) {
            
            boolean boolState = articleState == 1 ? true : false;
            int delta = articleState == 1 ? 1 : -1;
            
            Set<Integer> ids = new HashSet<Integer>();
            
            for (Article article : articles) {
                
                // Build a list of article ids to update.
                ids.add(article.id);
                
                // Set ArticleItem-State directly because the Activity uses this object
                article.isUnread = boolState;
                
                int feedId = article.feedId;
                Feed mFeed = DBHelper.getInstance().getFeed(feedId);
                int categoryId = mFeed.categoryId;
                
                DBHelper.getInstance().updateFeedDeltaUnreadCount(feedId, delta);
                DBHelper.getInstance().updateCategoryDeltaUnreadCount(categoryId, delta);
                
                // Check if is a fresh article and modify that count too
                long ms = System.currentTimeMillis() - Controller.getInstance().getFreshArticleMaxAge();
                Date maxAge = new Date(ms);
                if (article.updated.after(maxAge))
                    DBHelper.getInstance().updateCategoryDeltaUnreadCount(Data.VCAT_FRESH, delta);
                
                // Check if it is a starred article and modify that count too
                if (article.isStarred && pid != Data.VCAT_STAR)
                    DBHelper.getInstance().updateCategoryDeltaUnreadCount(Data.VCAT_STAR, delta);
                
                // Check if it is a published article and modify that count too
                if (article.isPublished && pid != Data.VCAT_PUB)
                    DBHelper.getInstance().updateCategoryDeltaUnreadCount(Data.VCAT_PUB, delta);
            }
            
            if (ids.size() > 0) {
                DBHelper.getInstance().markArticles(ids, "isUnread", articleState);
                
                int deltaUnread = articleState == 1 ? ids.size() : -ids.size();
                DBHelper.getInstance().updateCategoryDeltaUnreadCount(Data.VCAT_ALL, deltaUnread);
                
                if (pid < 0 && pid >= -3) {
                    // If on a virtual category also update article state in it.
                    DBHelper.getInstance().updateCategoryDeltaUnreadCount(pid, deltaUnread);
                } else if (pid < -10) {
                    // Article belongs to a label, modify that count too
                    DBHelper.getInstance().updateFeedDeltaUnreadCount(pid, deltaUnread);
                }
                
                // Notify about changes
                UpdateController.getInstance().notifyListeners();
                
                Data.getInstance().setArticleRead(ids, articleState);
            }
        }
    }
    
}
