/*
 * Tiny Tiny RSS Reader for Android
 * 
 * Copyright (C) 2010 N. Braden and contributors.
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
import org.ttrssreader.model.category.VirtualCategoryItemComparator;
import org.ttrssreader.model.feed.FeedItem;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class DataController {
    
    private static final String mutex = "";
    private static DataController mInstance = null;
    private static boolean mIsControllerInitialized = false;
    
    private boolean mForceFullRefresh = false;
    
    private long mCountersUpdated = 0;
    private Map<Integer, Long> mArticlesUpdated = new HashMap<Integer, Long>();
    private long mFeedsUpdated = 0;
    private long mVirtCategoriesUpdated = 0;
    private long mCategoriesUpdated = 0;
    
    private Map<CategoryItem, List<FeedItem>> mCounters;
    private Map<Integer, List<ArticleItem>> mArticles;
    private Map<Integer, List<FeedItem>> mFeeds;
    private List<CategoryItem> mVirtCategories;
    private List<CategoryItem> mCategories;
    
    private ConnectivityManager cm;
    
    public static DataController getInstance() {
        synchronized (mutex) {
            if (mInstance == null) {
                mInstance = new DataController();
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
        
        if (mCounters.isEmpty())
            mCounters = null;
        if (mArticles.isEmpty())
            mArticles = new HashMap<Integer, List<ArticleItem>>();
        if (mFeeds.isEmpty())
            mFeeds = null;
        if (mVirtCategories.isEmpty())
            mVirtCategories = null;
        if (mCategories.isEmpty())
            mCategories = null;
    }
    
    public synchronized void checkAndInitializeController(final Context context) {
        if (!mIsControllerInitialized) {
            initializeController(context);
            mIsControllerInitialized = true;
        }
    }
    
    public void forceFullRefresh() {
        mForceFullRefresh = true;
    }
    
    public void disableForceFullRefresh() {
        mForceFullRefresh = false;
    }
    
    private boolean needFullRefresh() {
        if (Controller.getInstance().isWorkOffline()) {
            return false;
        } else {
            return mForceFullRefresh || Controller.getInstance().isAlwaysFullRefresh();
        }
    }
    
    private boolean isOnline() {
        if (Controller.getInstance().isWorkOffline()) {
//            Log.i(Utils.TAG, "isOnline: Config has isWorkOffline activated...");
            return false;
        }
        
        NetworkInfo info = cm.getActiveNetworkInfo();
        
        if (info == null) {
//            Log.i(Utils.TAG, "isOnline: No network available...");
            return false;
        }
        
        synchronized (this) {
            int wait = 0;
            while (info.isConnectedOrConnecting() && !info.isConnected()) {
                try {
//                    Log.d(Utils.TAG, "isOnline: Waiting for " + wait + " seconds...");
                    wait += 100;
                    wait(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                if (wait > 2000)
                    break;
            }
        }
        
//        Log.i(Utils.TAG, "isOnline: Network available, State: " + info.isConnected());
        return info.isConnected();
    }
    
    // ********** DATAACCESS **********
    
    private synchronized Map<CategoryItem, List<FeedItem>> retrieveCategoryCounters(boolean needFullRefresh) {
        // Only update counters once in 60 seconds
        if (mCountersUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            if (mCounters != null) {
                return mCounters;
            }
        } else if (mCounters == null || needFullRefresh() || needFullRefresh) {
            if (isOnline()) {
                
                mCounters = Controller.getInstance().getTTRSSConnector().getCounters();
                mCountersUpdated = System.currentTimeMillis();
                DBHelper.getInstance().setCounters(mCounters);
            }
        }
        return mCounters;
    }
    
    @SuppressWarnings("unchecked")
    public synchronized List<ArticleItem> retrieveHeadlines(int feedId, boolean displayOnlyUnread, boolean needFullRefresh) {
        List<ArticleItem> result = null;
        if (mArticles == null)
            mArticles = new HashMap<Integer, List<ArticleItem>>();
        
        if (feedId == -4) {
            result = new ArrayList<ArticleItem>();
            for (Integer s : mArticles.keySet()) {
                result.addAll(mArticles.get(s));
            }
        } else {
            result = mArticles.get(feedId);
        }
        
        if (isOnline()) {
            if (result == null || result.isEmpty() || needFullRefresh() || needFullRefresh) {
                
                // Check time of last update for this feedId
                Long time = mArticlesUpdated.get(feedId);
                if (time == null)
                    time = new Long(0);
                
                if (time < System.currentTimeMillis() - Utils.UPDATE_TIME) {
                    
                    String viewMode = (displayOnlyUnread ? "unread" : "all_articles");
                    int articleLimit = Controller.getInstance().getArticleLimit();
                    result = Controller.getInstance().getTTRSSConnector()
                            .getFeedHeadlines(feedId, articleLimit / 2, 0, viewMode);
                    // Divide by two to keep the list of "All Articles" a bit smaller
                    
                    if (result == null) {
                        return null;
                    }
                    
                    new DBInsertArticlesTask(articleLimit).execute(result);
                    
                    mArticles.put(feedId, result);
                    mArticlesUpdated.put(feedId, System.currentTimeMillis());
                }
            }
        }
        
        // If option "ShowUnreadOnly" is enabled filter out all read items
        if (displayOnlyUnread && result != null) {
            List<ArticleItem> artList = new ArrayList<ArticleItem>();
            for (ArticleItem ai : result) {
                if (ai.isUnread()) {
                    artList.add(ai);
                }
            }
            
            // Overwrite old list with filtered one
            result = artList;
        }
        
        return result;
    }
    
    private synchronized Map<Integer, List<FeedItem>> retrieveFeeds(boolean needFullRefresh) {
        // Only update counters once in 60 seconds
        if (mFeedsUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            if (mFeeds != null) {
                return mFeeds;
            }
        } else if (mFeeds == null || needFullRefresh() || needFullRefresh) {
            if (isOnline()) {
                
                mFeeds = Controller.getInstance().getTTRSSConnector().getSubsribedFeeds();
                mFeedsUpdated = System.currentTimeMillis();
                
                if (mFeeds == null)
                    return null;
                
                DBHelper.getInstance().deleteFeeds();
                for (Integer s : mFeeds.keySet()) {
                    DBHelper.getInstance().insertFeeds(mFeeds.get(s));
                }
            }
        }
        return mFeeds;
    }
    
    private synchronized List<CategoryItem> retrieveVirtualCategories(boolean needFullRefresh) {
        if (mVirtCategoriesUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            if (mVirtCategories != null) {
                return mVirtCategories;
            }
        } else if (mVirtCategories == null || needFullRefresh() || needFullRefresh) {
            
            if (isOnline()) {
                boolean showUnread = Controller.getInstance().isDisplayUnreadInVirtualFeeds();
                
                mVirtCategories = new ArrayList<CategoryItem>();
                mVirtCategoriesUpdated = System.currentTimeMillis();
                
                // Refresh CategoryCounters
                retrieveCategoryCounters(true);
                
                CategoryItem catItem;
                catItem = new CategoryItem(-1, "Starred articles", showUnread ? getCategoryUnreadCount(-1) : 0);
                mVirtCategories.add(catItem);
                catItem = new CategoryItem(-2, "Published articles", showUnread ? getCategoryUnreadCount(-2) : 0);
                mVirtCategories.add(catItem);
                catItem = new CategoryItem(-3, "Fresh articles", showUnread ? getCategoryUnreadCount(-3) : 0);
                mVirtCategories.add(catItem);
                catItem = new CategoryItem(-4, "All articles", showUnread ? getCategoryUnreadCount(-4) : 0);
                mVirtCategories.add(catItem);
                catItem = new CategoryItem(0, "Uncategorized Feeds", showUnread ? getCategoryUnreadCount(0) : 0);
                mVirtCategories.add(catItem);
                
                DBHelper.getInstance().insertCategories(mVirtCategories);
            }
        }
        return mVirtCategories;
    }
    
    private synchronized List<CategoryItem> retrieveCategories(boolean needFullRefresh) {
        // Only update counters once in 60 seconds
        if (mCategoriesUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            if (mCategories != null) {
                return mCategories;
            }
        } else if (mCategories == null || needFullRefresh() || needFullRefresh) {
            if (isOnline()) {
                mCategories = Controller.getInstance().getTTRSSConnector().getCategories();
                mCategoriesUpdated = System.currentTimeMillis();
                
                DBHelper.getInstance().deleteCategories(false);
                DBHelper.getInstance().insertCategories(mCategories);
            }
        }
        return mCategories;
    }
    
    // ********** META-FUNCTIONS **********
    
    public int getCategoryUnreadCount(int catId) {
        Map<CategoryItem, List<FeedItem>> map = retrieveCategoryCounters(false);
        
        if (map != null) {
            for (CategoryItem c : map.keySet()) {
                if (catId == c.getId()) {
                    return c.getUnreadCount();
                }
            }
        }
        
        return 0;
    }
    
    public ArticleItem getArticleHeadline(int feedId, int articleId) {
        List<ArticleItem> list = retrieveHeadlines(feedId, false, false);
        
        if (list == null)
            return null;
        
        for (ArticleItem a : list) {
            if (a.getId() == articleId) {
                return a;
            }
        }
        return null;
    }
    
    public ArticleItem getArticleWithContent(int articleId) {
        ArticleItem result = null;
        
        for (Integer s : mArticles.keySet()) {
            for (ArticleItem a : mArticles.get(s)) {
                if (a.getId() == articleId) {
                    result = a;
                }
            }
        }
        
        if (result == null) {
            if (!needFullRefresh()) {
                result = DBHelper.getInstance().getArticle(articleId);
            } else {
                result = Controller.getInstance().getTTRSSConnector().getArticle(articleId);
                
                int articleLimit = Controller.getInstance().getArticleLimit();
                DBHelper.getInstance().insertArticle(result, articleLimit);
            }
        }
        
        if (result != null && result.getContent() == null) {
            Log.i(Utils.TAG, "Loading Content for Article \"" + result.getTitle() + "\"");
            result = Controller.getInstance().getTTRSSConnector().getArticle(articleId);
            DBHelper.getInstance().updateArticleContent(result);
        }
        
        return result;
    }
    
    @SuppressWarnings("unchecked")
    public List<ArticleItem> getArticlesWithContent(int feedId, boolean displayOnlyUnread, boolean needFullRefresh) {
        if (feedId < 0) {
            feedId = -1;
        }
        FeedItem fi = new FeedItem();
        fi.setId(feedId);
        
        List<ArticleItem> result = DBHelper.getInstance().getArticles(fi, true);
        
        boolean needRefresh = false;
        for (ArticleItem a : result) {
            if (a.getContent() == null) {
                needRefresh = true;
                break;
            }
        }
        
        // Also do update if needFullRefresh given and not working offline
        if (needFullRefresh && !Controller.getInstance().isWorkOffline())
            needRefresh = true;
        
        if (result == null || needRefresh) {
            // Check time of last update for this feedId
            Long time = mArticlesUpdated.get(feedId);
            if (time == null)
                time = new Long(0);
            
            if (time < System.currentTimeMillis() - Utils.UPDATE_TIME) {
                
                result = Controller.getInstance().getTTRSSConnector()
                        .getArticles(feedId, displayOnlyUnread ? 1 : 0, false);
                
                if (result == null)
                    return null;
                
                int articleLimit = Controller.getInstance().getArticleLimit();
                new DBInsertArticlesTask(articleLimit).execute(result);
                
                mArticles.put(feedId, result);
                mArticlesUpdated.put(feedId, System.currentTimeMillis());
            }
        }
        
        // If option "ShowUnreadOnly" is enabled filter out all Feeds without unread items
        if (displayOnlyUnread && result != null) {
            List<ArticleItem> tempList = new ArrayList<ArticleItem>();
            for (ArticleItem a : result) {
                if (a.isUnread())
                    tempList.add(a);
            }
            
            result = tempList;
        }
        
        return result;
    }
    
    public FeedItem getFeed(int feedId, boolean displayOnlyUnread) {
        Map<Integer, List<FeedItem>> map = retrieveFeeds(false);
        
        if (map == null) {
            return null;
        }
        
        for (Integer s : map.keySet()) {
            for (FeedItem f : map.get(s)) {
                if (f.getId() == feedId && f.getUnread() > 0) {
                    return f;
                }
            }
        }
        return null;
    }
    
    public List<FeedItem> getFeeds(int categoryId, boolean displayOnlyUnread, boolean needFullRefresh) {
        Map<Integer, List<FeedItem>> map = retrieveFeeds(needFullRefresh);
        
        if (map == null) {
            return null;
        }
        
        List<FeedItem> result = map.get(categoryId);
        
        // If option "ShowUnreadOnly" is enabled filter out all Feeds without unread items
        if (displayOnlyUnread && result != null) {
            List<FeedItem> feedList = new ArrayList<FeedItem>();
            for (FeedItem fi : result) {
                if (fi.getUnread() > 0) {
                    feedList.add(fi);
                }
            }
            
            // Overwrite old list with filtered one
            result = feedList;
        }
        
        return result;
    }
    
    public CategoryItem getVirtualCategory(int categoryId) {
        for (CategoryItem c : retrieveVirtualCategories(false)) {
            if (c.getId() == categoryId) {
                return c;
            }
        }
        return null;
    }
    
    public CategoryItem getCategory(int categoryId, boolean withVirtuals) {
        List<CategoryItem> categories = new ArrayList<CategoryItem>();
        
        if (withVirtuals) {
            categories.addAll(retrieveVirtualCategories(false));
            Collections.sort(categories, new VirtualCategoryItemComparator());
        }
        
        List<CategoryItem> list = retrieveCategories(false);
        if (list == null) {
            return null;
        }
        categories.addAll(list);
        
        for (CategoryItem c : categories) {
            if (c.getId() == categoryId) {
                return c;
            }
        }
        return null;
    }
    
    public List<CategoryItem> getCategories(boolean withVirtuals, boolean displayOnlyUnread, boolean needFullRefresh) {
        List<CategoryItem> categories = new ArrayList<CategoryItem>();
        
        if (withVirtuals) {
            categories.addAll(retrieveVirtualCategories(needFullRefresh));
            Collections.sort(categories, new VirtualCategoryItemComparator());
        }
        
        List<CategoryItem> categoryList = retrieveCategories(needFullRefresh);
        if (categoryList != null) {
            Collections.sort(categoryList, new CategoryItemComparator());
            categories.addAll(categoryList);
        }
        
        // If option "ShowUnreadOnly" is enabled filter out all categories without unread items
        if (displayOnlyUnread && categories != null) {
            
            List<CategoryItem> catList = new ArrayList<CategoryItem>();
            for (CategoryItem ci : categories) {
                // Dont filter for virtual Categories
                if (ci.getId() < 0) {
                    catList.add(ci);
                    continue;
                }
                
                // Refresh CategoryCounters
                retrieveCategoryCounters(needFullRefresh);
                
                if (getCategoryUnreadCount(ci.getId()) > 0) {
                    ci.setUnreadCount(getCategoryUnreadCount(ci.getId()));
                    catList.add(ci);
                }
            }
            categories = catList;
        }
        
        return categories;
    }
    
    @SuppressWarnings("unchecked")
    public void getNewArticles() {
        
        // Only update once within UPDATE_TIME milliseconds
        long time = Controller.getInstance().getLastUpdateTime();
        if (time > System.currentTimeMillis() - Utils.UPDATE_TIME) {
            return;
        }
        
        // Force update counters
        mCounters = null;
        getCategoryUnreadCount(0);
        
        // Load new Articles
        Map<CategoryItem, Map<FeedItem, List<ArticleItem>>> ret = Controller.getInstance().getTTRSSConnector()
                .getNewArticles(1, time);
        
        if (ret != null && !ret.isEmpty()) {
            Controller.getInstance().setLastUpdateTime(System.currentTimeMillis());
            int articleLimit = Controller.getInstance().getArticleLimit();
            
            for (CategoryItem c : ret.keySet()) {
                Map<FeedItem, List<ArticleItem>> feeds = ret.get(c);
                
                for (FeedItem f : ret.get(c).keySet()) {
                    List<ArticleItem> articles = feeds.get(f);
                    new DBInsertArticlesTask(articleLimit).execute(articles);
                }
            }
        }
        
        initializeController(null);
        
    }
    
}
