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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.ttrssreader.R;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.model.pojos.Label;
import org.ttrssreader.net.JSONConnector;
import org.ttrssreader.utils.Utils;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

@SuppressLint("UseSparseArrays")
public class Data {
    
    public static final int VCAT_UNCAT = 0;
    public static final int VCAT_STAR = -1;
    public static final int VCAT_PUB = -2;
    public static final int VCAT_FRESH = -3;
    public static final int VCAT_ALL = -4;
    
    public static final int TIME_CATEGORY = 1;
    public static final int TIME_FEED = 2;
    public static final int TIME_FEEDHEADLINE = 3;
    
    private static final String VIEW_ALL = "all_articles";
    private static final String VIEW_NEW = "unread";
    
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
            if (Controller.getInstance().getConnector().getCounters()) {
                countersChanged = System.currentTimeMillis();
                notifyListeners();
            }
        }
    }
    
    // *** ARTICLES *********************************************************************
    
    public void cacheArticles(boolean overrideOffline, boolean overrideDelay) {
        
        int limit = 400;
        if (Controller.getInstance().isLowMemory())
            limit = limit / 2;
        
        if (!overrideDelay && articlesCached > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return;
        } else if (Utils.isConnected(cm) || (overrideOffline && Utils.checkConnected(cm))) {
            
            int sinceId = Controller.getInstance().getSinceId();
            
            Set<Article> articles = new HashSet<Article>();
            Controller.getInstance().getConnector().getHeadlines(articles, -4, limit, VIEW_NEW, true);
            Controller.getInstance().getConnector().getHeadlines(articles, -4, limit, VIEW_ALL, true, sinceId);
            
            handleInsertArticles(articles, -4, true);
            
            // Only mark as updated if the calls were successful
            int count = articles.size();
            if (count > -1) {
                articlesCached = System.currentTimeMillis();
                
                notifyListeners();
                
                // Store all category-ids and ids of all feeds for this category in db
                articlesChanged.put(-4, articlesCached);
                for (Feed f : DBHelper.getInstance().getFeeds(-4)) {
                    articlesChanged.put(f.id, articlesCached);
                }
                for (Category c : DBHelper.getInstance().getCategoriesIncludingUncategorized()) {
                    feedsChanged.put(c.id, articlesCached);
                }
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
            int todo = calculateLimit(feedId, displayOnlyUnread, isCat);
            int limit;
            Set<Article> articles = new HashSet<Article>();
            
            while (true) {
                limit = todo;
                if (limit > 240)
                    limit = 240;
                
                todo = todo - limit;
                if (limit <= 0)
                    break;
                
                Log.d(Utils.TAG, "UPDATE limit: " + limit + " todo: " + todo);
                String viewMode = (displayOnlyUnread ? VIEW_NEW : VIEW_ALL);
                
                // If necessary and not displaying only unread articles: Refresh unread articles to get them too.
                if (needUnreadUpdate && !displayOnlyUnread)
                    Controller.getInstance().getConnector().getHeadlines(articles, feedId, limit, VIEW_NEW, isCat);
                
                Controller.getInstance().getConnector().getHeadlines(articles, feedId, limit, viewMode, isCat);
                
                handleInsertArticles(articles, feedId, isCat);
                
                // Only mark as updated if the first call was successful
                int count = articles.size();
                if (count != -1) {
                    long currentTime = System.currentTimeMillis();
                    // Store requested feed-/category-id and ids of all feeds in db for this category if a category was
                    // requested
                    articlesChanged.put(feedId, currentTime);
                    UpdateController.getInstance().notifyListeners();
                    
                    if (isCat) {
                        for (Feed f : DBHelper.getInstance().getFeeds(feedId)) {
                            articlesChanged.put(f.id, currentTime);
                        }
                    }
                }
                articles.clear();
            }
        }
    }
    
    public void searchArticles(int feedId, boolean isCat, boolean overrideOffline) {
        if (Utils.isConnected(cm) || (overrideOffline && Utils.checkConnected(cm))) {
            
            Set<Article> articles = new HashSet<Article>();
            Controller.getInstance().getConnector().getHeadlines(articles, feedId, 400, VIEW_ALL, isCat);
            
            // TODO: articles in DB packen oder direkt irgendwie zurück geben? Wenn ja, wohin?
            // Temporäre-DB erstellen und Adapter so umbauen, dass er Suchergebnisse automatisch daraus lädt?
            
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
        // else if (limit > 300)
        // limit = 300; // Lots of unread articles, fetch the first 300
        
        if (limit < 300) {
            if (isCat)
                limit = limit + 100; // Add some so we have a chance of getting not only the newest and possibly read
                                     // articles but also older ones.
            else
                limit = limit + 50; // Less on feed, more on category...
        }
        
        if (Controller.getInstance().isLowMemory())
            limit = limit / 2;
        
        return limit;
    }
    
    private void handleInsertArticles(Collection<Article> articles, int feedId, boolean isCategory) {
        int maxArticleId = Integer.MIN_VALUE;
        
        if (articles.size() > 0) {
            for (Article a : articles) {
                if (a.id > maxArticleId)
                    maxArticleId = a.id; // Store maximum id
            }
            
            DBHelper.getInstance().markFeedOnlyArticlesRead(feedId, isCategory, -1);
            DBHelper.getInstance().insertArticle(articles);
            DBHelper.getInstance().purgeArticlesNumber();
            Controller.getInstance().setSinceId(maxArticleId);
        }
    }
    
    // *** FEEDS ************************************************************************
    
    public Set<Feed> updateFeeds(int categoryId, boolean overrideOffline) {
        
        Long time = feedsChanged.get(categoryId);
        if (time == null)
            time = Long.valueOf(0);
        
        if (time > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return null;
        } else if (Utils.isConnected(cm) || (overrideOffline && Utils.checkConnected(cm))) {
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
                notifyListeners();
                for (Feed f : feeds) {
                    feedsChanged.put(f.categoryId, System.currentTimeMillis());
                }
            }
            
            return ret;
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
        notifyListeners();
        
        virtCategoriesChanged = System.currentTimeMillis();
        
        return vCats;
    }
    
    public Set<Category> updateCategories(boolean overrideOffline) {
        if (categoriesChanged > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return null;
        } else if (Utils.isConnected(cm) || overrideOffline) {
            Set<Category> categories = Controller.getInstance().getConnector().getCategories();
            
            if (!categories.isEmpty()) {
                DBHelper.getInstance().deleteCategories(false);
                DBHelper.getInstance().insertCategories(categories);
                
                categoriesChanged = System.currentTimeMillis();
                notifyListeners();
            }
            
            return categories;
        }
        return null;
    }
    
    // *** STATUS *******************************************************************
    
    public void setArticleRead(Set<Integer> ids, int articleState) {
        boolean erg = false;
        if (Utils.isConnected(cm))
            erg = Controller.getInstance().getConnector().setArticleRead(ids, articleState);
        
        if (!erg)
            DBHelper.getInstance().markUnsynchronizedStates(ids, DBHelper.MARK_READ, articleState);
    }
    
    public void setArticleStarred(int articleId, int articleState) {
        boolean erg = false;
        Set<Integer> ids = new HashSet<Integer>();
        ids.add(articleId);
        
        if (Utils.isConnected(cm))
            erg = Controller.getInstance().getConnector().setArticleStarred(ids, articleState);
        
        if (!erg)
            DBHelper.getInstance().markUnsynchronizedStates(ids, DBHelper.MARK_STAR, articleState);
    }
    
    public void setArticlePublished(int articleId, int articleState, String note) {
        boolean erg = false;
        Map<Integer, String> ids = new HashMap<Integer, String>();
        ids.put(articleId, note);
        
        if (Utils.isConnected(cm))
            erg = Controller.getInstance().getConnector().setArticlePublished(ids, articleState);
        
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
        
        notifyListeners();
        
        boolean erg = false;
        if (Utils.isConnected(cm)) {
            erg = Controller.getInstance().getConnector().setRead(id, isCategory);
        }
        
        if (isCategory || id < 0) {
            if (!erg)
                DBHelper.getInstance().markUnsynchronizedStatesCategory(id);
        } else {
            DBHelper.getInstance().markUnsynchronizedStatesFeed(id);
        }
        
    }
    
    public boolean shareToPublished(String title, String url, String content) {
        if (Utils.isConnected(cm))
            return Controller.getInstance().getConnector().shareToPublished(title, url, content);
        return false;
    }
    
    public JSONConnector.SubscriptionResponse feedSubscribe(String feed_url, int category_id) {
        if (Utils.isConnected(cm))
            return Controller.getInstance().getConnector().feedSubscribe(feed_url, category_id);
        return null;
    }
    
    public boolean feedUnsubscribe(int feed_id) {
        if (Utils.isConnected(cm))
            return Controller.getInstance().getConnector().feedUnsubscribe(feed_id);
        return false;
    }
    
    public String getPref(String pref) {
        if (Utils.isConnected(cm))
            return Controller.getInstance().getConnector().getPref(pref);
        return null;
    }
    
    public int getVersion() {
        if (Utils.isConnected(cm))
            return Controller.getInstance().getConnector().getVersion();
        return -1;
    }
    
    public int getApiLevel() {
        if (Utils.isConnected(cm))
            return Controller.getInstance().getConnector().getApiLevel();
        return -1;
    }
    
    public Set<Label> getLabels(int articleId) {
        Set<Label> ret = DBHelper.getInstance().getLabelsForArticle(articleId);
        return ret;
    }
    
    public boolean setLabel(Integer articleId, Label label) {
        Set<Integer> set = new HashSet<Integer>();
        set.add(articleId);
        return setLabel(set, label);
    }
    
    public boolean setLabel(Set<Integer> articleIds, Label label) {
        
        DBHelper.getInstance().insertLabels(articleIds, label.getInternalId(), label.checked);
        notifyListeners();
        
        boolean erg = false;
        if (Utils.isConnected(cm)) {
            Log.d(Utils.TAG, "Calling connector with Label: " + label + ") and ids.size() " + articleIds.size());
            erg = Controller.getInstance().getConnector().setArticleLabel(articleIds, label.getId(), label.checked);
        }
        return erg;
    }
    
    public void synchronizeStatus() {
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
            // TODO: Add synchronization of labels
        }
    }
    
    private void notifyListeners() {
        if (!Controller.getInstance().isHeadless())
            UpdateController.getInstance().notifyListeners();
    }
    
}
