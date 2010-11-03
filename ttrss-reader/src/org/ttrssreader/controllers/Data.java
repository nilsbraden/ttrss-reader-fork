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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.model.category.CategoryItem;
import org.ttrssreader.model.feed.FeedItem;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

public class Data {
    
    private static final String mutex = "";
    private static Data mInstance = null;
    private static boolean mIsDataInitialized = false;
    
    private long mCountersUpdated = 0;
    private Map<Integer, Long> mArticlesUpdated = new HashMap<Integer, Long>();
    private long mFeedsUpdated = 0;
    private long mVirtCategoriesUpdated = 0;
    private long mCategoriesUpdated = 0;
    private long mNewArticlesUpdated = 0;
    
    private ConnectivityManager cm;
    
    public static Data getInstance() {
        if (mInstance == null) {
            synchronized (mutex) {
                if (mInstance == null) {
                    mInstance = new Data();
                }
            }
        }
        return mInstance;
    }
    
    public synchronized void initializeData(Context context) {
        if (context != null)
            cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
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
    
    public synchronized void checkAndInitializeData(final Context context) {
        if (!mIsDataInitialized) {
            initializeData(context);
            mIsDataInitialized = true;
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
    
    public Set<CategoryItem> getCategoryCounters() {
        return DBHelper.getInstance().getCategoryCounters();
    }
    
    public int getCategoryUnreadCount(int catId) {
        for (CategoryItem c : getCategoryCounters()) {
            if (catId == c.getId()) {
                return c.getUnread();
            }
        }
        return -1;
    }
    
    public void updateCounters() {
        if (mCountersUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return;
        } else if (Utils.isOnline(cm)) {
            Map<CategoryItem, Set<FeedItem>> counters = Controller.getInstance().getConnector().getCounters();
            mCountersUpdated = System.currentTimeMillis();
            DBHelper.getInstance().setCounters(counters);
        }
    }
    
    // *** ARTICLES *********************************************************************
    
    public Set<ArticleItem> getArticles(int feedId) {
        return DBHelper.getInstance().getArticles(feedId, true);
    }
    
    public ArticleItem getArticle(int articleId) {
        return DBHelper.getInstance().getArticle(articleId);
    }
    
    public ArticleItem updateArticle(int articleId) {
        // TODO: Hopefully someday we don't need to fetch the content seperately and can remove this method.
        // Don't check last update time here
        if (Utils.isOnline(cm)) {
            Set<Integer> set = new LinkedHashSet<Integer>();
            set.add(articleId);
            
            for (ArticleItem a : Controller.getInstance().getConnector().getArticle(set)) {
                if (a.getId() == articleId) {
                    Log.d(Utils.TAG, "Found article: " + articleId);
                    return a;
                }
            }
        }
        Log.d(Utils.TAG, "Couldn't find article: " + articleId);
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public Set<ArticleItem> updateArticles(int feedId, boolean displayOnlyUnread) {
        Long time = mArticlesUpdated.get(feedId);
        if (time == null)
            time = new Long(0);
        
        if (time > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return null;
        } else if (Utils.isOnline(cm)) {
            FeedItem f = getFeed(feedId);
            int limit = 30;
            if (f != null) {
                int l = getFeed(feedId).getUnread();
                limit = (l > limit ? l : 30);
            }
            
            boolean isCategory = false;
            if (feedId < 0 && feedId > -10) {
                isCategory = true;
                limit = getCategoryUnreadCount(feedId);
            }
            
            Set<ArticleItem> articles = Controller.getInstance().getConnector()
                    .getArticles(feedId, displayOnlyUnread, isCategory, limit);
            
            if (articles.isEmpty()) {
                // getArticles not working, fetch headlines and articles manually
                String viewMode = (displayOnlyUnread ? "unread" : "all_articles");
                articles = Controller.getInstance().getConnector().getFeedHeadlines(feedId, limit, 0, viewMode);
                
                Set<Integer> set = new LinkedHashSet<Integer>();
                for (ArticleItem a : articles) {
                    set.add(a.getId());
                }
                
                Set<ArticleItem> temp = Controller.getInstance().getConnector().getArticle(set);
                
                if (temp.size() == articles.size()) {
                    articles = temp;
                }
            }
            
            mArticlesUpdated.put(feedId, System.currentTimeMillis());
            
            DBInsertArticlesTask task = new DBInsertArticlesTask(Controller.getInstance().getArticleLimit());
            task.execute(articles);
            
            // Utils.waitForTask(task);
            return articles;
        }
        return null;
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
                if (a != null) {
                    articles.addAll(a);
                }
            }
        }
        
        DBInsertArticlesTask task = new DBInsertArticlesTask(Controller.getInstance().getArticleLimit());
        task.execute(articles);
        
        Utils.waitForTask(task);
    }
    
    // *** FEEDS ************************************************************************
    
    public Set<FeedItem> getFeeds(int categoryId) {
        return DBHelper.getInstance().getFeeds(categoryId);
    }
    
    public FeedItem getFeed(int feedId) {
        return DBHelper.getInstance().getFeed(feedId);
    }
    
    public Set<FeedItem> updateFeeds(int categoryId) {
        if (mFeedsUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return null;
        } else if (Utils.isOnline(cm)) {
            Map<Integer, Set<FeedItem>> feeds = Controller.getInstance().getConnector().getFeeds();
            mFeedsUpdated = System.currentTimeMillis();
            
            if (!feeds.isEmpty()) {
                // Only delete feeds if we got new feeds...
                DBHelper.getInstance().deleteFeeds();
            }
            
            Set<FeedItem> ret = new LinkedHashSet<FeedItem>();
            for (Integer s : feeds.keySet()) {
                if (s.equals(categoryId)) {
                    ret.addAll(feeds.get(s));
                }
                DBHelper.getInstance().insertFeeds(feeds.get(s));
            }
            
            return ret;
        }
        return null;
    }
    
    // *** CATEGORIES *******************************************************************
    
    public Set<CategoryItem> getCategories(boolean virtuals) {
        Set<CategoryItem> ret = new LinkedHashSet<CategoryItem>();
        
        if (virtuals) {
            ret.addAll(DBHelper.getInstance().getVirtualCategories());
        }
        ret.addAll(DBHelper.getInstance().getCategories());
        
        return ret;
    }
    
    public CategoryItem getCategory(int categoryId) {
        return DBHelper.getInstance().getCategory(categoryId);
    }
    
    public Set<CategoryItem> updateVirtualCategories() {
        if (mVirtCategoriesUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return null;
        } else if (Utils.isOnline(cm)) {
            mVirtCategoriesUpdated = System.currentTimeMillis();
            
            Set<CategoryItem> virtCategories = new LinkedHashSet<CategoryItem>();
            for (CategoryItem c : getCategoryCounters()) {
                if (c.getId() < 1) {
                    virtCategories.add(c);
                }
            }
            
            boolean needUpdate = false;
            for (CategoryItem c : virtCategories) {
                if (c.getUnread() == -1) {
                    needUpdate = true;
                    break;
                }
            }
            
            if (needUpdate || virtCategories.isEmpty()) {

                virtCategories = new LinkedHashSet<CategoryItem>();
                virtCategories.add(new CategoryItem(-4, "All articles", 0));
                virtCategories.add(new CategoryItem(-3, "Fresh articles", 0));
                virtCategories.add(new CategoryItem(-2, "Published articles", 0));
                virtCategories.add(new CategoryItem(-1, "Starred articles", 0));
                virtCategories.add(new CategoryItem(0, "Uncategorized Feeds", 0));
                DBHelper.getInstance().insertCategories(virtCategories);
                
                resetCounterTime();
                updateCounters();
                
                return(DBHelper.getInstance().getVirtualCategories());
            }

            DBHelper.getInstance().insertCategories(virtCategories);
            return virtCategories;
        }
        return null;
    }
    
    public Set<CategoryItem> updateCategories() {
        if (mCategoriesUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return null;
        } else if (Utils.isOnline(cm)) {
            Set<CategoryItem> categories = Controller.getInstance().getConnector().getCategories();
            mCategoriesUpdated = System.currentTimeMillis();
            
            if (!categories.isEmpty()) {
                // Only delete categories if we got new categories...
                DBHelper.getInstance().deleteCategories(false);
            }
            
            DBHelper.getInstance().insertCategories(categories);
            return categories;
        }
        return null;
    }
    
    // **********************************************************************************
    
}
