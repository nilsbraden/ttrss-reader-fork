/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 Nils Braden
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
import java.util.HashSet;
import java.util.Set;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.controllers.UpdateController;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.pojos.Feed;

public class ReadStateUpdater implements IUpdatable {
    
    @SuppressWarnings("unused")
    private static final String TAG = ReadStateUpdater.class.getSimpleName();
    
    private int state;
    
    TYPE type;
    int id;
    
    private Collection<Category> categories = null;
    private Collection<Feed> feeds = null;
    private Collection<Article> articles = null;
    
    public static enum TYPE {
        ALL_CATEGORIES, ALL_FEEDS, CATEGORY, FEED
    }
    
    public ReadStateUpdater(TYPE type) {
        this(type, -1);
    }
    
    private ReadStateUpdater(TYPE type, int id) {
        this.type = type;
        this.id = id;
    }
    
    public ReadStateUpdater(int categoryId) {
        type = TYPE.CATEGORY;
        id = categoryId;
    }
    
    public ReadStateUpdater(int feedId, int dummy) {
        if (feedId <= 0 && feedId >= -4) { // Virtual Category...
            type = TYPE.CATEGORY;
            id = feedId;
        } else {
            type = TYPE.FEED;
            id = feedId;
        }
    }
    
    /* articleState: 0 = mark as read, 1 = mark as unread */
    public ReadStateUpdater(Article article, int pid, int articleState) {
        articles = new ArrayList<Article>();
        articles.add(article);
        state = articleState;
        article.isUnread = (articleState > 0);
    }
    
    /* articleState: 0 = mark as read, 1 = mark as unread */
    public ReadStateUpdater(Collection<Article> articlesList, int pid, int articleState) {
        articles = new ArrayList<Article>();
        articles.addAll(articlesList);
        state = articleState;
        for (Article article : articles) {
            article.isUnread = (articleState > 0);
        }
    }
    
    @Override
    public void update(Updater parent) {
        // Read appropriate data from the DB
        switch (type) {
            case ALL_CATEGORIES:
                categories = DBHelper.getInstance().getAllCategories();
                break;
            case CATEGORY:
                categories = new HashSet<Category>();
                Category c = DBHelper.getInstance().getCategory(id);
                if (c != null)
                    categories.add(c);
                break;
            case ALL_FEEDS:
                feeds = DBHelper.getInstance().getFeeds(id);
                break;
            case FEED:
                feeds = new HashSet<Feed>();
                Feed f = DBHelper.getInstance().getFeed(id);
                if (f != null)
                    feeds.add(f);
                break;
            default:
                break;
        }
        
        if (categories != null) {
            for (Category ci : categories) {
                // VirtualCats are actually Feeds (the server handles them as such) so we have to set isCat to false
                if (ci.id >= 0) {
                    Data.getInstance().setRead(ci.id, true);
                } else {
                    Data.getInstance().setRead(ci.id, false);
                }
                UpdateController.getInstance().notifyListeners();
            }
        } else if (feeds != null) {
            for (Feed fi : feeds) {
                Data.getInstance().setRead(fi.id, false);
                UpdateController.getInstance().notifyListeners();
            }
        } else if (articles != null) {
            Set<Integer> ids = new HashSet<Integer>();
            for (Article article : articles) {
                ids.add(article.id);
                article.isUnread = (state > 0);
            }
            
            if (!ids.isEmpty()) {
                DBHelper.getInstance().markArticles(ids, "isUnread", state);
                UpdateController.getInstance().notifyListeners();
                Data.getInstance().setArticleRead(ids, state);
            }
        }
    }
    
}
