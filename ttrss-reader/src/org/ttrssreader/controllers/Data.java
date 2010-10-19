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

package org.ttrssreader.controllers;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.model.category.CategoryItem;
import org.ttrssreader.model.feed.FeedItem;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.net.ConnectivityManager;

public class Data {
    
    private static final String mutex = "";
    private static Data mInstance = null;
    private static boolean mIsControllerInitialized = false;
    
    private long mCountersUpdated = 0;
    private Map<Integer, Long> mArticlesUpdated = new HashMap<Integer, Long>();
    private long mFeedsUpdated = 0;
    private long mVirtCategoriesUpdated = 0;
    private long mCategoriesUpdated = 0;
    private long mNewArticlesUpdated = 0;
    
    private Map<CategoryItem, Set<FeedItem>> mCounters;
    private Map<Integer, Set<ArticleItem>> mArticles;
    private Map<Integer, Set<FeedItem>> mFeeds;
    private Set<CategoryItem> mVirtCategories;
    private Set<CategoryItem> mCategories;
    
    private ConnectivityManager cm;
    
    public static Data getInstance() {
        synchronized (mutex) {
            if (mInstance == null)
                mInstance = new Data();
            return mInstance;
        }
    }
    
    public synchronized void initializeController(Context context) {
        if (context != null)
            cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        mCounters = DBHelper.getInstance().getCounters();
        mArticles = DBHelper.getInstance().getArticles(0, false);
        mFeeds = DBHelper.getInstance().getFeeds();
        mVirtCategories = DBHelper.getInstance().getVirtualCategories();
        mCategories = DBHelper.getInstance().getCategories(false);
        
        // Set new update-time if necessary
        if (mCountersUpdated < mNewArticlesUpdated)
            mCountersUpdated = mNewArticlesUpdated;
        
        for (int article : mArticlesUpdated.keySet()) {
            if (mArticlesUpdated.get(article) < mNewArticlesUpdated)
                mArticlesUpdated.put(article, mNewArticlesUpdated);
        }
        
        if (mFeedsUpdated < mNewArticlesUpdated)
            mFeedsUpdated = mNewArticlesUpdated;
        
        if (mVirtCategoriesUpdated < mNewArticlesUpdated)
            mVirtCategoriesUpdated = mNewArticlesUpdated;
        
        if (mCategoriesUpdated < mNewArticlesUpdated)
            mCategoriesUpdated = mNewArticlesUpdated;
    }
    
    public synchronized void checkAndInitializeController(final Context context) {
        if (!mIsControllerInitialized) {
            initializeController(context);
            mIsControllerInitialized = true;
        }
    }
    
    public void resetCounterTime() {
        mCountersUpdated = 0;
    }
    
    public void resetArticlesTime(int feedId) {
        mArticlesUpdated.put(feedId, new Long(0));
    }
    
    public void resetFeedTime() {
        mFeedsUpdated = 0;
    }
    
    public void resetCategoriesTime() {
        mVirtCategoriesUpdated = 0;
        mCategoriesUpdated = 0;
    }
    
    // *** COUNTERS *********************************************************************
    
    public int getCategoryUnreadCount(int catId) {
        updateCounters();
        
        for (CategoryItem c : mCounters.keySet()) {
            if (catId == c.getId()) {
                return c.getUnread();
            }
        }
        
        return 0;
    }
    
    public void updateCounters() {
        if (mCountersUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            if (mCounters != null) {
                return;
            }
        } else if (Utils.isOnline(cm)) {
            Map<CategoryItem, Set<FeedItem>> counters = Controller.getInstance().getConnector().getCounters();
            if (counters != null)
                mCounters = counters;
            
            mCountersUpdated = System.currentTimeMillis();
            DBHelper.getInstance().setCounters(mCounters);
        }
    }
    
    // *** ARTICLES *********************************************************************
    
    public Set<ArticleItem> getArticles(int feedId) {
        for (int i : mArticles.keySet()) {
            if (i == feedId) {
                return mArticles.get(i);
            }
        }
        return null;
    }
    
    public ArticleItem getArticle(int articleId) {
        for (int i : mArticles.keySet()) {
            for (ArticleItem ai : mArticles.get(i)) {
                if (ai.getId() == articleId) {
                    return ai;
                }
            }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public void updateArticles(int feedId, boolean displayOnlyUnread) {
        Long time = mArticlesUpdated.get(feedId);
        if (time == null)
            time = new Long(0);
        
        if (time > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            if (mArticles.get(feedId) != null) {
                return;
            }
        } else if (Utils.isOnline(cm)) {
            // TODO: Anzahl an Artikeln deutlich reduzieren, 1.5MB sind zu viel!
            Set<ArticleItem> articles = Controller.getInstance().getConnector()
                    .getArticles(feedId, displayOnlyUnread, false);
            if (articles == null)
                return;
            
            mArticles.put(feedId, articles);
            mArticlesUpdated.put(feedId, System.currentTimeMillis());
            
            DBInsertArticlesTask task = new DBInsertArticlesTask(Controller.getInstance().getArticleLimit());
            task.execute(articles);
        }
    }
    
    @SuppressWarnings({ "unchecked" })
    public void updateUnreadArticles() {
        mNewArticlesUpdated = Controller.getInstance().getLastUpdateTime();
        if (mNewArticlesUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return;
        }
        
        // Try to force-update counters
        resetCounterTime();
        getCategoryUnreadCount(0);
        
        Map<CategoryItem, Map<FeedItem, Set<ArticleItem>>> ret = null;
        if (Utils.isOnline(cm)) {
            // TODO: Verify behaviour: 1 == only unread articles are fetched?
            ret = Controller.getInstance().getConnector().getNewArticles(1, mNewArticlesUpdated);
        } else {
            return;
        }
        
        Controller.getInstance().setLastUpdateTime(System.currentTimeMillis());
        Set<ArticleItem> articles = new LinkedHashSet<ArticleItem>();
        
        for (CategoryItem c : ret.keySet()) {
            Map<FeedItem, Set<ArticleItem>> feeds = ret.get(c);
            
            for (FeedItem f : feeds.keySet()) {
                Set<ArticleItem> a = feeds.get(f);
                if (a != null)
                    articles.addAll(a);
            }
        }
        
        DBInsertArticlesTask task = new DBInsertArticlesTask(Controller.getInstance().getArticleLimit());
        task.execute(articles);
        
        // TODO
        // initializeController(null);
    }
    
    // *** FEEDS ************************************************************************
    
    public Set<FeedItem> getFeeds(int categoryId) {
        for (int i : mFeeds.keySet()) {
            if (i == categoryId) {
                return mFeeds.get(i);
            }
        }
        return null;
    }
    
    public FeedItem getFeed(int feedId) {
        for (int i : mFeeds.keySet()) {
            for (FeedItem fi : mFeeds.get(i)) {
                if (fi.getId() == feedId) {
                    return fi;
                }
            }
        }
        return null;
    }
    
    public void updateFeeds(int categoryId) {
        if (mFeedsUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            if (mFeeds != null) {
                return;
            }
        } else if (Utils.isOnline(cm)) {
            mFeeds = Controller.getInstance().getConnector().getFeeds();
            if (mFeeds == null)
                return;
            
            mFeedsUpdated = System.currentTimeMillis();
            
            DBHelper.getInstance().deleteFeeds();
            for (Integer s : mFeeds.keySet()) {
                DBHelper.getInstance().insertFeeds(mFeeds.get(s));
            }
        }
    }
    
    // *** CATEGORIES *******************************************************************
    
    public Set<CategoryItem> getCategories(boolean virtuals) {
        if (!virtuals) {
            return mCategories;
        }
        
        Set<CategoryItem> ret = new LinkedHashSet<CategoryItem>();
        ret.addAll(mVirtCategories);
        ret.addAll(mCategories);
        return ret;
    }
    
    public CategoryItem getCategory(int categoryId) {
        for (CategoryItem ci : mCategories) {
            if (ci.getId() == categoryId) {
                return ci;
            }
        }
        for (CategoryItem ci : mVirtCategories) {
            if (ci.getId() == categoryId) {
                return ci;
            }
        }
        return null;
    }
    
    public void updateVirtualCategories() {
        if (mVirtCategoriesUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            
        } else if (Utils.isOnline(cm)) {
            boolean displayCount = Controller.getInstance().isDisplayUnreadInVirtualFeeds();
            
            mVirtCategories = new LinkedHashSet<CategoryItem>();
            mVirtCategoriesUpdated = System.currentTimeMillis();
            
            // Refresh CategoryCounters
            // retrieveCategoryCounters(true);
            
            CategoryItem catItem;
            catItem = new CategoryItem(-4, "All articles", displayCount ? getCategoryUnreadCount(-4) : 0);
            mVirtCategories.add(catItem);
            catItem = new CategoryItem(-3, "Fresh articles", displayCount ? getCategoryUnreadCount(-3) : 0);
            mVirtCategories.add(catItem);
            catItem = new CategoryItem(-2, "Published articles", displayCount ? getCategoryUnreadCount(-2) : 0);
            mVirtCategories.add(catItem);
            catItem = new CategoryItem(-1, "Starred articles", displayCount ? getCategoryUnreadCount(-1) : 0);
            mVirtCategories.add(catItem);
            catItem = new CategoryItem(0, "Uncategorized Feeds", displayCount ? getCategoryUnreadCount(0) : 0);
            mVirtCategories.add(catItem);
            
            DBHelper.getInstance().insertCategories(mVirtCategories);
        }
        
        if (mVirtCategories == null)
            mVirtCategories = new LinkedHashSet<CategoryItem>();
    }
    
    public void updateCategories() {
        if (mCategoriesUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            if (mCategories != null) {
                return;
            }
        } else if (Utils.isOnline(cm)) {
            mCategories = Controller.getInstance().getConnector().getCategories();
            if (mCategories == null)
                return;
            
            mCategoriesUpdated = System.currentTimeMillis();
            
            DBHelper.getInstance().deleteCategories(false);
            DBHelper.getInstance().insertCategories(mCategories);
        }
    }
    
    // **********************************************************************************
    
}
