/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
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

package org.ttrssreader.controllers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.ttrssreader.R;
import org.ttrssreader.model.pojos.CategoryItem;
import org.ttrssreader.model.pojos.FeedItem;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.net.ConnectivityManager;

public class Data {
    
    private static final String mutex = "";
    private static Data instance = null;
    private static boolean initialized = false;
    private Context context;
    
    private long countersUpdated = 0;
    private Map<Integer, Long> articlesUpdated = new HashMap<Integer, Long>();
    private long feedsUpdated = 0;
    private long virtCategoriesUpdated = 0;
    private long categoriesUpdated = 0;
    private long newArticlesUpdated = 0;
    
    private ConnectivityManager cm;
    
    public static Data getInstance() {
        if (instance == null) {
            synchronized (mutex) {
                if (instance == null)
                    instance = new Data();
            }
        }
        return instance;
    }
    
    private synchronized void initializeData(Context context) {
        this.context = context;
        if (context != null)
            cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        // Set new update-time if necessary
        if (countersUpdated < newArticlesUpdated)
            countersUpdated = newArticlesUpdated;
        
        for (int article : articlesUpdated.keySet()) {
            if (articlesUpdated.get(article) < newArticlesUpdated)
                articlesUpdated.put(article, newArticlesUpdated);
        }
        
        if (feedsUpdated < newArticlesUpdated)
            feedsUpdated = newArticlesUpdated;
        
        if (virtCategoriesUpdated < newArticlesUpdated)
            virtCategoriesUpdated = newArticlesUpdated;
        
        if (categoriesUpdated < newArticlesUpdated)
            categoriesUpdated = newArticlesUpdated;
    }
    
    public synchronized void checkAndInitializeData(final Context context) {
        if (!initialized) {
            initializeData(context);
            initialized = true;
        }
    }
    
    // *** COUNTERS *********************************************************************
    
    public void resetTime(Object o) {
        if (o == null)
            return;
        
        if (o instanceof CategoryItem) {
            virtCategoriesUpdated = 0;
            categoriesUpdated = 0;
        } else if (o instanceof FeedItem) {
            feedsUpdated = 0;
        } else if (o instanceof Integer) {
            Integer i = (Integer) o;
            articlesUpdated.put(i, new Long(0));
        }
    }
    
    // takes about 2.5 seconds on wifi
    public void updateCounters(boolean overrideOffline) {
        if (Utils.isOnline(cm) || overrideOffline)
            Controller.getInstance().getConnector().getCounters();
    }
    
    // *** ARTICLES *********************************************************************
    
    public void updateArticles(int feedId, boolean displayOnlyUnread) {
        updateArticles(feedId, displayOnlyUnread, false);
    }
    
    public void updateArticles(int feedId, boolean displayOnlyUnread, boolean overrideOffline) {
        
        Long time = articlesUpdated.get(feedId);
        if (time == null)
            time = new Long(0);
        
        if (time > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return;
        } else if (Utils.isOnline(cm) || (overrideOffline && Utils.checkConnection(cm))) {
            int limit = 30;
            
            switch (feedId) {
                case -1:
                case -2:
                    limit = DBHelper.getInstance().getUnreadCount(feedId, false);
                    displayOnlyUnread = false;
                    break;
                
                case -3:
                    limit = DBHelper.getInstance().getUnreadCount(feedId, false);
                    break;
                
                case -4:
                    limit = DBHelper.getInstance().getUnreadCount(feedId, true);
                    break;
                
                default:
                    int l = DBHelper.getInstance().getUnreadCount(feedId, false);
                    limit = (l > limit ? l : 30);
            }
            
            String viewMode = (displayOnlyUnread ? "unread" : "all_articles");
            Set<Integer> ids = Controller.getInstance().getConnector()
                    .getHeadlinesToDatabase(feedId, limit, 0, viewMode, false); // false = Disable fetching of content
                                                                                // here
            if (ids != null)
                Controller.getInstance().getConnector().getArticle(ids);
            
            articlesUpdated.put(feedId, System.currentTimeMillis());
        }
    }
    
    // *** FEEDS ************************************************************************
    
    public Set<FeedItem> updateFeeds(int categoryId, boolean overrideOffline) {
        if (feedsUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return null;
        } else if (Utils.isOnline(cm) || (overrideOffline && Utils.checkConnection(cm))) {
            Set<FeedItem> feeds = Controller.getInstance().getConnector().getFeeds();
            feedsUpdated = System.currentTimeMillis();
            
            // Only delete feeds if we got new feeds...
            if (!feeds.isEmpty())
                DBHelper.getInstance().deleteFeeds();
            
            Set<FeedItem> ret = new LinkedHashSet<FeedItem>();
            for (FeedItem f : feeds) {
                if (categoryId == -4 || f.categoryId == categoryId)
                    ret.add(f);
            }
            DBHelper.getInstance().insertFeeds(feeds);
            
            return ret;
        }
        return null;
    }
    
    // *** CATEGORIES *******************************************************************
    
    public Set<CategoryItem> updateVirtualCategories() {
        if (virtCategoriesUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME)
            return null;
        
        String vCatAllArticles = "";
        String vCatFreshArticles = "";
        String vCatPublishedArticles = "";
        String vCatStarredArticles = "";
        String uncatFeeds = "";
        
        if (context != null) {
            vCatAllArticles = (String) context.getText(R.string.VCategory_AllArticles);
            vCatFreshArticles = (String) context.getText(R.string.VCategory_FreshArticles);
            vCatPublishedArticles = (String) context.getText(R.string.VCategory_PublishedArticles);
            vCatStarredArticles = (String) context.getText(R.string.VCategory_StarredArticles);
            uncatFeeds = (String) context.getText(R.string.Feed_UncategorizedFeeds);
        }
        
        Set<CategoryItem> vCats = new LinkedHashSet<CategoryItem>();
        vCats.add(new CategoryItem(-4, vCatAllArticles, DBHelper.getInstance().getUnreadCount(-4, true)));
        vCats.add(new CategoryItem(-3, vCatFreshArticles, DBHelper.getInstance().getUnreadCount(-3, true)));
        vCats.add(new CategoryItem(-2, vCatPublishedArticles, DBHelper.getInstance().getUnreadCount(-2, true)));
        vCats.add(new CategoryItem(-1, vCatStarredArticles, DBHelper.getInstance().getUnreadCount(-1, true)));
        vCats.add(new CategoryItem(0, uncatFeeds, DBHelper.getInstance().getUnreadCount(0, true)));
        
        DBHelper.getInstance().insertCategories(vCats);
        virtCategoriesUpdated = System.currentTimeMillis();
        
        return vCats;
    }
    
    public Set<CategoryItem> updateCategories(boolean overrideOffline) {
        if (categoriesUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return null;
        } else if (Utils.isOnline(cm) || overrideOffline) {
            Set<CategoryItem> categories = Controller.getInstance().getConnector().getCategories();
            categoriesUpdated = System.currentTimeMillis();
            
            DBHelper.getInstance().deleteCategories(false);
            DBHelper.getInstance().insertCategories(categories);
            return categories;
        }
        return null;
    }
    
    // *** STATUS *******************************************************************
    
    public void setArticleRead(Set<Integer> ids, int articleState) {
        boolean erg = false;
        if (Utils.isOnline(cm))
            erg = Controller.getInstance().getConnector().setArticleRead(ids, articleState);
        
        if (!erg)
            DBHelper.getInstance().markArticles(ids, DBHelper.MARK_READ, articleState);
    }
    
    public void setArticleStarred(int articleIds, int articleState) {
        boolean erg = false;
        Set<Integer> ids = new HashSet<Integer>();
        ids.add(articleIds);
        
        if (Utils.isOnline(cm))
            erg = Controller.getInstance().getConnector().setArticleStarred(ids, articleState);
        
        if (!erg)
            DBHelper.getInstance().markArticles(ids, DBHelper.MARK_STAR, articleState);
    }
    
    public void setArticlePublished(int articleIds, int articleState) {
        boolean erg = false;
        Set<Integer> ids = new HashSet<Integer>();
        ids.add(articleIds);
        
        if (Utils.isOnline(cm))
            erg = Controller.getInstance().getConnector().setArticlePublished(ids, articleState);
        
        if (!erg)
            DBHelper.getInstance().markArticles(ids, DBHelper.MARK_PUBLISH, articleState);
    }
    
    public void setRead(int id, boolean isCategory) {
        boolean erg = false;
        if (Utils.isOnline(cm))
            Controller.getInstance().getConnector().setRead(id, isCategory);
        
        if (!erg) {
            if (isCategory) {
                DBHelper.getInstance().markArticlesReadCategory(id);
            } else {
                if (id < 0) { // Filter Virtual Categories AGAIN. Why are we doing this weird stuff. Thanks to the guy
                              // who started developing the reader...
                    DBHelper.getInstance().markArticlesReadCategory(id);
                } else {
                    DBHelper.getInstance().markArticlesReadFeed(id);
                }
            }
        }
    }
    
    protected String getPref(String pref) {
        if (Utils.isOnline(cm))
            return Controller.getInstance().getConnector().getPref(pref);
        return null;
    }
    
    public void synchronizeStatus() {
        if (!Utils.isOnline(cm))
            return;
        
        String[] marks = new String[] { DBHelper.MARK_READ, DBHelper.MARK_STAR, DBHelper.MARK_PUBLISH };
        for (String mark : marks) {
            Set<Integer> idsMark = DBHelper.getInstance().getMarked(mark, 1);
            Set<Integer> idsUnmark = DBHelper.getInstance().getMarked(mark, 0);
            
            if (DBHelper.MARK_READ.equals(mark)) {
                if (Controller.getInstance().getConnector().setArticleRead(idsMark, 1))
                    DBHelper.getInstance().setMarked(idsMark, mark);
                
                if (Controller.getInstance().getConnector().setArticleRead(idsUnmark, 0))
                    DBHelper.getInstance().setMarked(idsUnmark, mark);
            }
            if (DBHelper.MARK_STAR.equals(mark)) {
                if (Controller.getInstance().getConnector().setArticleStarred(idsMark, 1))
                    DBHelper.getInstance().setMarked(idsMark, mark);
                
                if (Controller.getInstance().getConnector().setArticleStarred(idsUnmark, 0))
                    DBHelper.getInstance().setMarked(idsUnmark, mark);
            }
            if (DBHelper.MARK_PUBLISH.equals(mark)) {
                if (Controller.getInstance().getConnector().setArticlePublished(idsMark, 1))
                    DBHelper.getInstance().setMarked(idsMark, mark);
                
                if (Controller.getInstance().getConnector().setArticlePublished(idsUnmark, 0))
                    DBHelper.getInstance().setMarked(idsUnmark, mark);
            }
        }
    }
}
