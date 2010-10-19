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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.model.category.CategoryItem;
import org.ttrssreader.model.category.CategoryItemComparator;
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
    
    private Map<CategoryItem, List<FeedItem>> mCounters;
    private Map<Integer, List<ArticleItem>> mArticles;
    private Map<Integer, List<FeedItem>> mFeeds;
    private List<CategoryItem> mVirtCategories;
    private List<CategoryItem> mCategories;
    
    private ConnectivityManager cm;
    
    public static Data getInstance() {
        synchronized (mutex) {
            if (mInstance == null) {
                mInstance = new Data();
            }
            return mInstance;
        }
    }
    
    public synchronized void initializeController(Context context) {
        if (context != null) {
            cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        
        mCounters = DBHelper.getInstance().getCounters();
        mArticles = DBHelper.getInstance().getArticles(0, false);
        mFeeds = DBHelper.getInstance().getFeeds();
        mVirtCategories = DBHelper.getInstance().getVirtualCategories();
        mCategories = DBHelper.getInstance().getCategories(false);
        
        // Set new update-time if necessary
        if (mCountersUpdated < mNewArticlesUpdated) {
            mCountersUpdated = mNewArticlesUpdated;
        }
        
        for (int article : mArticlesUpdated.keySet()) {
            if (mArticlesUpdated.get(article) < mNewArticlesUpdated) {
                mArticlesUpdated.put(article, mNewArticlesUpdated);
            }
        }
        
        if (mFeedsUpdated < mNewArticlesUpdated) {
            mFeedsUpdated = mNewArticlesUpdated;
        }
        
        if (mVirtCategoriesUpdated < mNewArticlesUpdated) {
            mVirtCategoriesUpdated = mNewArticlesUpdated;
        }
        
        if (mCategoriesUpdated < mNewArticlesUpdated) {
            mCategoriesUpdated = mNewArticlesUpdated;
        }
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
            Map<CategoryItem, List<FeedItem>> counters = Controller.getInstance().getTTRSSConnector().getCounters();
            if (counters != null) {
                mCounters = counters;
            }
            
            mCountersUpdated = System.currentTimeMillis();
            DBHelper.getInstance().setCounters(mCounters);
        }
    }
    
    // *** ARTICLES *********************************************************************
    
    public List<ArticleItem> getArticles(int feedId) {
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
            List<ArticleItem> articles = Controller.getInstance().getTTRSSConnector()
                    .getArticles(feedId, displayOnlyUnread, false);
            if (articles == null)
                return;
            
            mArticles.put(feedId, articles);
            mArticlesUpdated.put(feedId, System.currentTimeMillis());
            
            int articleLimit = Controller.getInstance().getArticleLimit();
            new DBInsertArticlesTask(articleLimit).execute(articles);
            
            mArticles.put(feedId, articles);
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
        
        Map<CategoryItem, Map<FeedItem, List<ArticleItem>>> ret = null;
        if (Utils.isOnline(cm)) {
            ret = Controller.getInstance().getTTRSSConnector().getNewArticles(1, mNewArticlesUpdated);
            // TODO: Verify behaviour: 1 == only unread articles are fetched?
        }
        
        if (ret == null) {
            return;
        }
        
        Controller.getInstance().setLastUpdateTime(System.currentTimeMillis());
        int articleLimit = Controller.getInstance().getArticleLimit();
        List<ArticleItem> articleList = new ArrayList<ArticleItem>();
        
        for (CategoryItem c : ret.keySet()) {
            Map<FeedItem, List<ArticleItem>> feeds = ret.get(c);
            
            for (FeedItem f : feeds.keySet()) {
                List<ArticleItem> articles = feeds.get(f);
                if (articles == null) {
                    continue;
                }
                articleList.addAll(articles);
            }
        }
        
        new DBInsertArticlesTask(articleLimit).execute(articleList);
        initializeController(null);
    }
    
    // *** FEEDS ************************************************************************
    
    public List<FeedItem> getFeeds(int categoryId) {
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
            mFeeds = Controller.getInstance().getTTRSSConnector().getFeeds();
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
    
    public List<CategoryItem> getCategories(boolean virtuals) {
        if (!virtuals) {
            return mCategories;
        }
        
        List<CategoryItem> ret = new ArrayList<CategoryItem>();
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
            
            mVirtCategories = new ArrayList<CategoryItem>();
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
            mVirtCategories = new ArrayList<CategoryItem>();
    }
    
    public void updateCategories() {
        if (mCategoriesUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            if (mCategories != null) {
                return;
            }
        } else if (Utils.isOnline(cm)) {
            mCategories = Controller.getInstance().getTTRSSConnector().getCategories();
            if (mCategories == null)
                return;
            
            Collections.sort(mCategories, new CategoryItemComparator());
            mCategoriesUpdated = System.currentTimeMillis();
            
            DBHelper.getInstance().deleteCategories(false);
            DBHelper.getInstance().insertCategories(mCategories);
        }
    }
    
    // **********************************************************************************
    
}
