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
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

public class Data {
    
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
    
    // Singleton
    private Data() {
    }
    
    public static Data getInstance() {
        if (instance == null) {
            synchronized (Data.class) {
                if (instance == null)
                    instance = new Data();
            }
        }
        return instance;
    }
    
    public synchronized void checkAndInitializeData(final Context context) {
        this.context = context;
        
        if (!initialized) {
            initializeData();
            initialized = true;
        }
    }
    
    private synchronized void initializeData() {
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
    
    // *** COUNTERS *********************************************************************
    
    public void resetTime(Object o) {
        if (o == null)
            return;
        
        if (o instanceof Category) {
            virtCategoriesUpdated = 0;
            categoriesUpdated = 0;
            countersUpdated = 0;
        } else if (o instanceof Feed) {
            feedsUpdated = 0;
        } else if (o instanceof Integer) {
            Integer i = (Integer) o;
            articlesUpdated.put(i, new Long(0));
        }
    }
    
    // takes about 2.5 seconds on wifi
    // TODO: Why hasnt there been a check here for the last time data was fetched??
    public void updateCounters(boolean overrideOffline) {
        if (countersUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return;
        } else if (Utils.isConnected(cm) || overrideOffline) {
            try {
                Controller.getInstance().getConnector().getCounters();
            } catch (NotInitializedException e) {
            }
        }
    }
    
    // *** ARTICLES *********************************************************************
    
    public void updateArticles(int feedId, boolean displayOnlyUnread, boolean isCategory) {
        updateArticles(feedId, displayOnlyUnread, isCategory, false);
    }
    
    public void updateArticles(int feedId, boolean displayOnlyUnread, boolean isCategory, boolean overrideOffline) {
        
        Long time = articlesUpdated.get(feedId);
        if (time == null)
            time = new Long(0);
        
        if (time > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return;
        } else if (Utils.isConnected(cm) || (overrideOffline && Utils.checkConnected(cm))) {
            int limit = 50;
            
            switch (feedId) {
                case -1: // Starred
                case -2: // Published
                    limit = 300;
                    displayOnlyUnread = false;
                    break;
                
                case -3: // Fresh
                    limit = DBHelper.getInstance().getUnreadCount(feedId, true);
                    break;
                
                case -4: // All Articles
                    limit = DBHelper.getInstance().getUnreadCount(feedId, true);
                    break;
                
                default: // Normal categories
                    int l = DBHelper.getInstance().getUnreadCount(feedId, isCategory);
                    limit = (l > limit ? l : limit);
            }
            
            if (limit <= 0 && displayOnlyUnread)
                return; // No unread articles, do nothing
            if (limit <= 0)
                limit = 100; // No unread, fetch some to make sure we are at least a bit up-to-date
            if (limit > 300)
                limit = 300; // Lots of unread articles, fetch the first 300
                
            try {
                String viewMode = (displayOnlyUnread ? "unread" : "all_articles");
                Set<Integer> ids = Controller.getInstance().getConnector()
                        .getHeadlinesToDatabase(feedId, limit, viewMode, isCategory);
                
                // Check if there are new articles, then check if attachments are there, else fetch them separately
                if (ids != null) {
                    for (Integer i : ids) {
                        Article a = DBHelper.getInstance().getArticle(i);
                        if (a == null || a.attachments == null) {
                            Log.d(Utils.TAG,
                                    "WARNING: Had to call getArticle since getHeadline didn't fetch attachments. "
                                            + "Check if you are running latest server (1.5.3 or newer).");
                            Controller.getInstance().getConnector().getArticlesToDatabase(ids);
                            break;
                        }
                        break;
                    }
                }
            } catch (NotInitializedException e) {
                return;
            }
            
            articlesUpdated.put(feedId, System.currentTimeMillis());
        }
    }
    
    // *** FEEDS ************************************************************************
    
    public Set<Feed> updateFeeds(int categoryId, boolean overrideOffline) {
        if (feedsUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return null;
        } else if (Utils.isConnected(cm) || (overrideOffline && Utils.checkConnected(cm))) {
            try {
                Set<Feed> feeds = Controller.getInstance().getConnector().getFeeds();
                feedsUpdated = System.currentTimeMillis();
                
                // Only delete feeds if we got new feeds...
                if (!feeds.isEmpty())
                    DBHelper.getInstance().deleteFeeds();
                
                Set<Feed> ret = new LinkedHashSet<Feed>();
                for (Feed f : feeds) {
                    if (categoryId == -4 || f.categoryId == categoryId)
                        ret.add(f);
                }
                DBHelper.getInstance().insertFeeds(feeds);
                
                return ret;
            } catch (NotInitializedException e) {
            }
        }
        return null;
    }
    
    // *** CATEGORIES *******************************************************************
    
    public Set<Category> updateVirtualCategories() {
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
        
        Set<Category> vCats = new LinkedHashSet<Category>();
        vCats.add(new Category(-4, vCatAllArticles, DBHelper.getInstance().getUnreadCount(-4, true)));
        vCats.add(new Category(-3, vCatFreshArticles, DBHelper.getInstance().getUnreadCount(-3, true)));
        vCats.add(new Category(-2, vCatPublishedArticles, DBHelper.getInstance().getUnreadCount(-2, true)));
        vCats.add(new Category(-1, vCatStarredArticles, DBHelper.getInstance().getUnreadCount(-1, true)));
        vCats.add(new Category(0, uncatFeeds, DBHelper.getInstance().getUnreadCount(0, true)));
        
        DBHelper.getInstance().insertCategories(vCats);
        virtCategoriesUpdated = System.currentTimeMillis();
        
        return vCats;
    }
    
    public Set<Category> updateCategories(boolean overrideOffline) {
        if (categoriesUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return null;
        } else if (Utils.isConnected(cm) || overrideOffline) {
            try {
                Set<Category> categories = Controller.getInstance().getConnector().getCategories();
                
                categoriesUpdated = System.currentTimeMillis();
                
                DBHelper.getInstance().deleteCategories(false);
                DBHelper.getInstance().insertCategories(categories);
                return categories;
            } catch (NotInitializedException e) {
            }
        }
        return null;
    }
    
    // *** STATUS *******************************************************************
    
    public void setArticleRead(Set<Integer> ids, int articleState) {
        boolean erg = false;
        if (Utils.isConnected(cm))
            try {
                erg = Controller.getInstance().getConnector().setArticleRead(ids, articleState);
            } catch (NotInitializedException e) {
                return;
            }
        
        if (!erg)
            DBHelper.getInstance().markUnsynchronizedStates(ids, DBHelper.MARK_READ, articleState);
    }
    
    public void setArticleStarred(int articleIds, int articleState) {
        boolean erg = false;
        Set<Integer> ids = new HashSet<Integer>();
        ids.add(articleIds);
        
        if (Utils.isConnected(cm))
            try {
                erg = Controller.getInstance().getConnector().setArticleStarred(ids, articleState);
            } catch (NotInitializedException e) {
                return;
            }
        
        if (!erg)
            DBHelper.getInstance().markUnsynchronizedStates(ids, DBHelper.MARK_STAR, articleState);
    }
    
    public void setArticlePublished(int articleIds, int articleState) {
        boolean erg = false;
        Set<Integer> ids = new HashSet<Integer>();
        ids.add(articleIds);
        
        if (Utils.isConnected(cm))
            try {
                erg = Controller.getInstance().getConnector().setArticlePublished(ids, articleState);
            } catch (NotInitializedException e) {
                return;
            }
        
        if (!erg)
            DBHelper.getInstance().markUnsynchronizedStates(ids, DBHelper.MARK_PUBLISH, articleState);
    }
    
    public void setRead(int id, boolean isCategory) {
        boolean erg = false;
        if (Utils.isConnected(cm))
            try {
                Controller.getInstance().getConnector().setRead(id, isCategory);
            } catch (NotInitializedException e) {
                return;
            }
        
        if (!erg) {
            if (isCategory) {
                DBHelper.getInstance().markCategoryRead(id, true);
            } else {
                if (id < 0) { // Filter Virtual Categories AGAIN. Why are we doing this weird stuff. Thanks to the guy
                              // who started developing the reader...
                    DBHelper.getInstance().markCategoryRead(id, true);
                } else {
                    DBHelper.getInstance().markFeedRead(id, true);
                }
            }
        }
    }
    
    public String getPref(String pref) {
        if (Utils.isConnected(cm))
            try {
                return Controller.getInstance().getConnector().getPref(pref);
            } catch (NotInitializedException e) {
                return null;
            }
        return null;
    }
    
    public int getVersion() {
        if (Utils.isConnected(cm))
            try {
                return Controller.getInstance().getConnector().getVersion();
            } catch (NotInitializedException e) {
                return -1;
            }
        return -1;
    }
    
    public void synchronizeStatus() {
        if (!Utils.isConnected(cm))
            return;
        
        String[] marks = new String[] { DBHelper.MARK_READ, DBHelper.MARK_STAR, DBHelper.MARK_PUBLISH };
        for (String mark : marks) {
            Set<Integer> idsMark = DBHelper.getInstance().getMarked(mark, 1);
            Set<Integer> idsUnmark = DBHelper.getInstance().getMarked(mark, 0);
            
            try {
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
            } catch (NotInitializedException e) {
                return;
            }
        }
    }
}
