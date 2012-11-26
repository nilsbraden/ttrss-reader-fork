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
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.net.ConnectivityManager;

public class Data {
    
    public static final int VCAT_UNCAT = 0;
    public static final int VCAT_STAR = -1;
    public static final int VCAT_PUB = -2;
    public static final int VCAT_FRESH = -3;
    public static final int VCAT_ALL = -4;
    
    public static final int TIME_CATEGORY = 1;
    public static final int TIME_FEED = 2;
    public static final int TIME_FEEDHEADLINE = 3;
    
    private static Data instance = null;
    private Context context;
    
    private long articlesCached = 0;
    
    private Map<Integer, Long> articlesChanged = new HashMap<Integer, Long>();
    private Map<Integer, Long> feedsChanged = new HashMap<Integer, Long>();
    private long virtCategoriesChanged = 0;
    private long categoriesChanged = 0;
    private long countersChanged = 0;
    
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
        if (context != null)
            cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }
    
    // *** COUNTERS *********************************************************************
    
    public void updateCounters(boolean overrideOffline, boolean overrideDelay) {
        if (!overrideDelay && countersChanged > System.currentTimeMillis() - Utils.HALF_UPDATE_TIME) {
            return;
        } else if (Utils.isConnected(cm) || overrideOffline) {
            try {
                if (Controller.getInstance().getConnector().getCounters()) {
                    countersChanged = System.currentTimeMillis();
                    UpdateController.getInstance().notifyListeners();
                }
            } catch (NotInitializedException e) {
            }
        }
    }
    
    // *** ARTICLES *********************************************************************
    
    public void cacheArticles(boolean overrideOffline, boolean overrideDelay) {
        
        int limit = 1000;
        
        if (!overrideDelay && articlesCached > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return;
        } else if (Utils.isConnected(cm) || (overrideOffline && Utils.checkConnected(cm))) {
            
            try {
                int sinceId = Controller.getInstance().getSinceId();
                int count = Controller.getInstance().getConnector()
                        .getHeadlinesToDatabase(-4, limit, "all_articles", true, sinceId, 0);
                
                // Request lots of unread articles afterwards. Every call to getHeadlinesToDatabase() marks all existing
                // articles as unread so with these calls we should get a) lots of articles and b) all unread articles
                // except if there are more then 1000.
                count += Controller.getInstance().getConnector().getHeadlinesToDatabase(-4, limit, "unread", true);
                
                // Only mark as updated if the calls were successful
                if (count > -1) {
                    articlesCached = System.currentTimeMillis();
                    
                    UpdateController.getInstance().notifyListeners();
                    
                    // Store all category-ids and ids of all feeds for this category in db
                    articlesChanged.put(-4, articlesCached);
                    for (Feed f : DBHelper.getInstance().getFeeds(-4)) {
                        articlesChanged.put(f.id, articlesCached);
                    }
                    for (Category c : DBHelper.getInstance().getCategoriesIncludingUncategorized()) {
                        feedsChanged.put(c.id, articlesCached);
                    }
                }
            } catch (NotInitializedException e) {
            }
        }
    }
    
    public void updateArticles(int feedId, boolean displayOnlyUnread, boolean isCat, boolean overrideOffline, boolean overrideDelay) {
        // Check if unread-count and actual number of unread articles match, if not do a seperate call with
        // displayOnlyUnread=true
        boolean needUnreadUpdate = false;
        if (!isCat) {
            int unreadCount = DBHelper.getInstance().getUnreadCount(feedId, false);
            if (unreadCount > DBHelper.getInstance().getUnreadArticles(feedId).size()) {
                needUnreadUpdate = true;
            }
        }
        
        Long time = articlesChanged.get(feedId);
        
        if (isCat) // Category-Ids are in feedsChanged
            time = feedsChanged.get(feedId);
        
        if (time == null)
            time = Long.valueOf(0);
        
        if (!overrideDelay && !needUnreadUpdate && time > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return;
        } else if (Utils.isConnected(cm) || (overrideOffline && Utils.checkConnected(cm))) {
            
            if (feedId == VCAT_PUB || feedId == VCAT_STAR)
                displayOnlyUnread = false; // Display all articles for Starred/Published
                
            // Calculate an appropriate upper limit for the number of articles
            int limit = calculateLimit(feedId, displayOnlyUnread, isCat);
            
            try {
                String viewMode = (displayOnlyUnread ? "unread" : "all_articles");
                
                int count = Controller.getInstance().getConnector()
                        .getHeadlinesToDatabase(feedId, limit, viewMode, isCat);
                
                // If necessary and not displaying only unread articles: Refresh unread articles to get them too.
                if (needUnreadUpdate && !displayOnlyUnread)
                    Controller.getInstance().getConnector().getHeadlinesToDatabase(feedId, limit, "unread", isCat);
                
                // Only mark as updated if the first call was successful
                if (count != -1) {
                    long currentTime = System.currentTimeMillis();
                    // Store requested feed-/category-id and ids of all feeds in db for this category if a category was
                    // requested
                    articlesChanged.put(feedId, currentTime);
                    UpdateController.getInstance().notifyListeners();
                    
                    if (isCat) {
                        for (Feed f : DBHelper.getInstance().getFeeds(feedId)) {
                            articlesChanged.put(f.id, currentTime);
                            // UpdateController.getInstance().notifyListeners(UpdateController.TYPE_FEED, f.id,
                            // f.categoryId);
                        }
                    }
                }
            } catch (NotInitializedException e) {
            }
        }
    }
    
    /*
     * Calculate an appropriate upper limit for the number of articles
     */
    private int calculateLimit(int feedId, boolean displayOnlyUnread, boolean isCat) {
        int limit = 50;
        switch (feedId) {
            case VCAT_STAR: // Starred
            case VCAT_PUB: // Published
                limit = 300;
                break;
            case VCAT_FRESH: // Fresh
                limit = DBHelper.getInstance().getUnreadCount(feedId, true);
                break;
            case VCAT_ALL: // All Articles
                limit = DBHelper.getInstance().getUnreadCount(feedId, true);
                break;
            default: // Normal categories
                limit = DBHelper.getInstance().getUnreadCount(feedId, isCat);
        }
        
        if (limit <= 0 && displayOnlyUnread)
            limit = 50; // No unread articles, fetch some stuff
        else if (limit <= 0)
            limit = 100; // No unread, fetch some to make sure we are at least a bit up-to-date
        else if (limit > 300)
            limit = 300; // Lots of unread articles, fetch the first 300
            
        if (limit < 300) {
            if (isCat)
                limit = limit + 50; // Add some so we have a chance of getting not only the newest and possibly read
                                    // articles but also older ones.
            else
                limit = limit + 15; // Less on feed, more on category...
        }
        return limit;
    }
    
    // *** FEEDS ************************************************************************
    
    public Set<Feed> updateFeeds(int categoryId, boolean overrideOffline) {
        
        Long time = feedsChanged.get(categoryId);
        if (time == null)
            time = Long.valueOf(0);
        
        if (time > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return null;
        } else if (Utils.isConnected(cm) || (overrideOffline && Utils.checkConnected(cm))) {
            try {
                Set<Feed> ret = new LinkedHashSet<Feed>();
                Set<Feed> feeds = Controller.getInstance().getConnector().getFeeds();
                
                // Only delete feeds if we got new feeds...
                if (!feeds.isEmpty()) {
                    for (Feed f : feeds) {
                        if (categoryId == VCAT_ALL || f.categoryId == categoryId)
                            ret.add(f);
                    }
                    DBHelper.getInstance().deleteFeeds();
                    DBHelper.getInstance().insertFeeds(feeds);
                    
                    // Store requested category-id and ids of all received feeds
                    feedsChanged.put(categoryId, System.currentTimeMillis());
                    UpdateController.getInstance().notifyListeners();
                    for (Feed f : feeds) {
                        // UpdateController.getInstance().notifyListeners(UpdateController.TYPE_FEED, f.id, categoryId);
                        feedsChanged.put(f.categoryId, System.currentTimeMillis());
                    }
                }
                
                return ret;
            } catch (NotInitializedException e) {
            }
        }
        return null;
    }
    
    // *** CATEGORIES *******************************************************************
    
    public Set<Category> updateVirtualCategories() {
        if (virtCategoriesChanged > System.currentTimeMillis() - Utils.UPDATE_TIME)
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
        vCats.add(new Category(VCAT_ALL, vCatAllArticles, DBHelper.getInstance().getUnreadCount(VCAT_ALL, true)));
        vCats.add(new Category(VCAT_FRESH, vCatFreshArticles, DBHelper.getInstance().getUnreadCount(VCAT_FRESH, true)));
        vCats.add(new Category(VCAT_PUB, vCatPublishedArticles, DBHelper.getInstance().getUnreadCount(VCAT_PUB, true)));
        vCats.add(new Category(VCAT_STAR, vCatStarredArticles, DBHelper.getInstance().getUnreadCount(VCAT_STAR, true)));
        vCats.add(new Category(VCAT_UNCAT, uncatFeeds, DBHelper.getInstance().getUnreadCount(VCAT_UNCAT, true)));
        
        DBHelper.getInstance().insertCategories(vCats);
        UpdateController.getInstance().notifyListeners();
        
        virtCategoriesChanged = System.currentTimeMillis();
        
        return vCats;
    }
    
    public Set<Category> updateCategories(boolean overrideOffline) {
        if (categoriesChanged > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return null;
        } else if (Utils.isConnected(cm) || overrideOffline) {
            try {
                Set<Category> categories = Controller.getInstance().getConnector().getCategories();
                
                if (!categories.isEmpty()) {
                    DBHelper.getInstance().deleteCategories(false);
                    DBHelper.getInstance().insertCategories(categories);
                    
                    categoriesChanged = System.currentTimeMillis();
                    UpdateController.getInstance().notifyListeners();
                }
                
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
    
    public void setArticleStarred(int articleId, int articleState) {
        boolean erg = false;
        Set<Integer> ids = new HashSet<Integer>();
        ids.add(articleId);
        
        if (Utils.isConnected(cm))
            try {
                erg = Controller.getInstance().getConnector().setArticleStarred(ids, articleState);
            } catch (NotInitializedException e) {
                return;
            }
        
        if (!erg)
            DBHelper.getInstance().markUnsynchronizedStates(ids, DBHelper.MARK_STAR, articleState);
    }
    
    public void setArticlePublished(int articleId, int articleState, String note) {
        boolean erg = false;
        Map<Integer, String> ids = new HashMap<Integer, String>();
        ids.put(articleId, note);
        
        if (Utils.isConnected(cm))
            try {
                erg = Controller.getInstance().getConnector().setArticlePublished(ids, articleState);
            } catch (NotInitializedException e) {
                return;
            }
        
        // Write changes to cache if calling the server failed
        if (!erg) {
            DBHelper.getInstance().markUnsynchronizedStates(ids.keySet(), DBHelper.MARK_PUBLISH, articleState);
            DBHelper.getInstance().markUnsynchronizedNotes(ids, DBHelper.MARK_PUBLISH);
        }
    }
    
    public void setRead(int id, boolean isCategory) {
        
        if (isCategory || id < 0)
            DBHelper.getInstance().markCategoryRead(id);
        else
            DBHelper.getInstance().markFeedRead(id);
        
        UpdateController.getInstance().notifyListeners();
        
        boolean erg = false;
        if (Utils.isConnected(cm)) {
            try {
                erg = Controller.getInstance().getConnector().setRead(id, isCategory);
            } catch (NotInitializedException e) {
                return;
            }
        }
        
        if (isCategory || id < 0) {
            if (!erg)
                DBHelper.getInstance().markUnsynchronizedStatesCategory(id);
        } else {
            DBHelper.getInstance().markUnsynchronizedStatesFeed(id);
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
    
    public int getApiLevel() {
        if (Utils.isConnected(cm))
            try {
                return Controller.getInstance().getConnector().getApiLevel();
            } catch (NotInitializedException e) {
                return -1;
            }
        return -1;
    }
    
    public void synchronizeStatus() throws NotInitializedException {
        if (!Utils.isConnected(cm))
            return;
        
        String[] marks = new String[] { DBHelper.MARK_READ, DBHelper.MARK_STAR, DBHelper.MARK_PUBLISH,
                DBHelper.MARK_NOTE };
        for (String mark : marks) {
            Map<Integer, String> idsMark = DBHelper.getInstance().getMarked(mark, 1);
            Map<Integer, String> idsUnmark = DBHelper.getInstance().getMarked(mark, 0);
            
            if (DBHelper.MARK_READ.equals(mark)) {
                if (Controller.getInstance().getConnector().setArticleRead(idsMark.keySet(), 1))
                    DBHelper.getInstance().setMarked(idsMark, mark);
                
                if (Controller.getInstance().getConnector().setArticleRead(idsUnmark.keySet(), 0))
                    DBHelper.getInstance().setMarked(idsUnmark, mark);
            }
            if (DBHelper.MARK_STAR.equals(mark)) {
                if (Controller.getInstance().getConnector().setArticleStarred(idsMark.keySet(), 1))
                    DBHelper.getInstance().setMarked(idsMark, mark);
                
                if (Controller.getInstance().getConnector().setArticleStarred(idsUnmark.keySet(), 0))
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
