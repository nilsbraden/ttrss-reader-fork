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
    private static Data mInstance = null;
    private static boolean mIsDataInitialized = false;
    
    private String vCategoryAllArticles;
    private String vCategoryFreshArticles;
    private String vCategoryPublishedArticles;
    private String vCategoryStarredArticles;
    private String feedUncategorizedFeeds;
    
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
        if (context != null) {
            cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        
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
        
        vCategoryAllArticles = (String) context.getText(R.string.VCategory_AllArticles);
        vCategoryFreshArticles = (String) context.getText(R.string.VCategory_FreshArticles);
        vCategoryPublishedArticles = (String) context.getText(R.string.VCategory_PublishedArticles);
        vCategoryStarredArticles = (String) context.getText(R.string.VCategory_StarredArticles);
        feedUncategorizedFeeds = (String) context.getText(R.string.Feed_UncategorizedFeeds);
    }
    
    public synchronized void checkAndInitializeData(final Context context) {
        if (!mIsDataInitialized) {
            initializeData(context);
            mIsDataInitialized = true;
        }
    }
    
    // *** COUNTERS *********************************************************************
    
    public void resetTime(Object o) {
        if (o == null) {
            return;
        }
        
        if (o instanceof CategoryItem) {
            mVirtCategoriesUpdated = 0;
            mCategoriesUpdated = 0;
        } else if (o instanceof FeedItem) {
            mFeedsUpdated = 0;
        } else if (o instanceof Integer) {
            Integer i = (Integer) o;
            mArticlesUpdated.put(i, new Long(0));
        }
    }
    
    // takes about 2.5 seconds on wifi
    public void updateCounters() {
        if (Utils.isOnline(cm)) {
            Controller.getInstance().getConnector().getCounters();
        }
    }
    
    // *** ARTICLES *********************************************************************
    
    public void updateArticles(int feedId, boolean displayOnlyUnread) {
        updateArticles(feedId, displayOnlyUnread, false);
    }
    
    public void updateArticles(int feedId, boolean displayOnlyUnread, boolean overrideOffline) {
        
        Long time = mArticlesUpdated.get(feedId);
        if (time == null) {
            time = new Long(0);
        }
        
        if (time > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return;
        } else if (Utils.isOnline(cm) || (overrideOffline && Utils.checkConnection(cm))) {
            int limit = 30;
            
            if (feedId < 0 && feedId >= -3) {
                // We want all articles for starred (-1) and published (-2) and fresh (-3)
                limit = DBHelper.getInstance().getUnreadCount(feedId, false);
                displayOnlyUnread = false;
            } else {
                int l = DBHelper.getInstance().getUnreadCount(feedId, false);
                limit = (l > limit ? l : 30);
            }
            
            String viewMode = (displayOnlyUnread ? "unread" : "all_articles");
            Set<Integer> ids = Controller.getInstance().getConnector()
                    .getHeadlinesToDatabase(feedId, limit, 0, viewMode, true);
            if (ids != null) {
                Controller.getInstance().getConnector().getArticle(ids);
            }
            
            mArticlesUpdated.put(feedId, System.currentTimeMillis());
        }
    }
    
    // *** FEEDS ************************************************************************
    
    public Set<FeedItem> updateFeeds(int categoryId) {
        if (mFeedsUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return null;
        } else if (Utils.isOnline(cm)) {
            Set<FeedItem> feeds = Controller.getInstance().getConnector().getFeeds();
            mFeedsUpdated = System.currentTimeMillis();
            
            if (!feeds.isEmpty()) {
                // Only delete feeds if we got new feeds...
                DBHelper.getInstance().deleteFeeds();
            }
            
            Set<FeedItem> ret = new LinkedHashSet<FeedItem>();
            for (FeedItem f : feeds) {
                if (categoryId == -4 || f.getCategoryId() == categoryId) {
                    ret.add(f);
                }
            }
            DBHelper.getInstance().insertFeeds(feeds);
            
            return ret;
        }
        return null;
    }
    
    // *** CATEGORIES *******************************************************************
    
    public Set<CategoryItem> updateVirtualCategories() {
        if (mVirtCategoriesUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return null;
        }
        
        Set<CategoryItem> virtCategories = new LinkedHashSet<CategoryItem>();
        virtCategories.add(new CategoryItem(-4, vCategoryAllArticles, DBHelper.getInstance().getUnreadCount(-4, true)));
        virtCategories
                .add(new CategoryItem(-3, vCategoryFreshArticles, DBHelper.getInstance().getUnreadCount(-3, true)));
        virtCategories.add(new CategoryItem(-2, vCategoryPublishedArticles, DBHelper.getInstance().getUnreadCount(-2,
                true)));
        virtCategories.add(new CategoryItem(-1, vCategoryStarredArticles, DBHelper.getInstance().getUnreadCount(-1,
                true)));
        virtCategories.add(new CategoryItem(0, feedUncategorizedFeeds, DBHelper.getInstance().getUnreadCount(0, true)));
        
        DBHelper.getInstance().insertCategories(virtCategories);
        mVirtCategoriesUpdated = System.currentTimeMillis();
        
        return virtCategories;
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
    
    // *** STATUS *******************************************************************
    
    public void setArticleRead(Set<Integer> articlesIds, int articleState) {
        if (Utils.isOnline(cm)) {
            Controller.getInstance().getConnector().setArticleRead(articlesIds, articleState);
        }
    }
    
    public void setArticleStarred(int articlesId, int articleState) {
        if (Utils.isOnline(cm)) {
            Set<Integer> ids = new HashSet<Integer>();
            ids.add(articlesId);
            Controller.getInstance().getConnector().setArticleStarred(ids, articleState);
        }
    }
    
    public void setArticlePublished(int articlesId, int articleState) {
        if (Utils.isOnline(cm)) {
            Set<Integer> ids = new HashSet<Integer>();
            ids.add(articlesId);
            Controller.getInstance().getConnector().setArticlePublished(ids, articleState);
        }
    }
    
    public void setRead(int id, boolean isCategory) {
        if (Utils.isOnline(cm)) {
            Controller.getInstance().getConnector().setRead(id, isCategory);
        }
    }
    
    public String getPref(String pref) {
        if (Utils.isOnline(cm)) {
            return Controller.getInstance().getConnector().getPref(pref);
        }
        return null;
    }
    
    public void synchronizeStatus() {
        if (!Utils.isOnline(cm))
            return;
        
        String[] marks = new String[] { "isUnread", "isStarred", "isPublished" };
        for (String mark : marks) {
            Set<Integer> idsMark = DBHelper.getInstance().getMarked(mark, 1);
            Set<Integer> idsUnmark = DBHelper.getInstance().getMarked(mark, 0);
            
            if ("isUnread".equals(mark)) {
                if (Controller.getInstance().getConnector().setArticleRead(idsMark, 1))
                    DBHelper.getInstance().setMarked(idsMark, mark);
                
                if (Controller.getInstance().getConnector().setArticleRead(idsUnmark, 0))
                    DBHelper.getInstance().setMarked(idsUnmark, mark);
            }
            if ("isStarred".equals(mark)) {
                if (Controller.getInstance().getConnector().setArticleStarred(idsMark, 1))
                    DBHelper.getInstance().setMarked(idsMark, mark);
                
                if (Controller.getInstance().getConnector().setArticleStarred(idsUnmark, 0))
                    DBHelper.getInstance().setMarked(idsUnmark, mark);
            }
            if ("isPublished".equals(mark)) {
                if (Controller.getInstance().getConnector().setArticlePublished(idsMark, 1))
                    DBHelper.getInstance().setMarked(idsMark, mark);
                
                if (Controller.getInstance().getConnector().setArticlePublished(idsUnmark, 0))
                    DBHelper.getInstance().setMarked(idsUnmark, mark);
            }
        }
    }
}
