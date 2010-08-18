/*
 * Tiny Tiny RSS Reader for Android
 * 
 * Copyright (C) 2009 J. Devauchelle and contributors.
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
	private Map<String, Long> mArticlesUpdated = new HashMap<String, Long>();
	private long mFeedsUpdated = 0;
	private long mVirtCategoriesUpdated = 0;
	private long mCategoriesUpdated = 0;
	
	private Map<CategoryItem, List<FeedItem>> mCounters;
	private Map<String, List<ArticleItem>> mArticles;
	private Map<String, List<FeedItem>> mFeeds;
	private List<CategoryItem> mVirtCategories;
	private List<CategoryItem> mCategories;
	
	private NetworkInfo info;
	
	public static DataController getInstance() {
		synchronized (mutex) {
			if (mInstance == null) {
				mInstance = new DataController();
			}
			return mInstance;
		}
	}
	
	public synchronized void initializeController(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		info = cm.getActiveNetworkInfo();
		
		mCounters = DBHelper.getInstance().getCounters();
		mArticles = DBHelper.getInstance().getArticles(0, false);
		mFeeds = DBHelper.getInstance().getFeeds();
		mVirtCategories = DBHelper.getInstance().getVirtualCategories();
		mCategories = DBHelper.getInstance().getCategories(false);
		
		if (mCounters.isEmpty()) mCounters = null;
		if (mArticles.isEmpty()) mArticles = new HashMap<String, List<ArticleItem>>();
		if (mFeeds.isEmpty()) mFeeds = null;
		if (mVirtCategories.isEmpty()) mVirtCategories = null;
		if (mCategories.isEmpty()) mCategories = null;
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
	
	public boolean isOnline() {
		if (Controller.getInstance().isWorkOffline()) {
			return false;
		}
		return true;
//		return info.isConnected();
	}
	
	// ********** DATAACCESS **********
	
	public synchronized Map<CategoryItem, List<FeedItem>> getCategoryCounters(boolean needFullRefresh) {
		if (isOnline()) {
			if (mCounters == null || needFullRefresh() || needFullRefresh) {
				
				// Only update counters once in 60 seconds
				if (mCountersUpdated < System.currentTimeMillis() - Utils.UPDATE_TIME) {
					
					mCounters = Controller.getInstance().getTTRSSConnector().getCounters();
					mCountersUpdated = System.currentTimeMillis();
					DBHelper.getInstance().setCounters(mCounters);
				}
			}
		}
		return mCounters;
	}
	
	@SuppressWarnings("unchecked")
	public synchronized List<ArticleItem> getArticlesHeadlines(String feedId, boolean displayOnlyUnread, boolean needFullRefresh) {
		int fId = new Integer(feedId);
		List<ArticleItem> result = null;
		
		if (fId == -4) {
			result = new ArrayList<ArticleItem>();
			for (String s : mArticles.keySet()) {
				result.addAll(mArticles.get(s));
			}
		} else {
			result = mArticles.get(feedId);
		}
		
		if (isOnline()) {
			if (result == null || result.isEmpty() || needFullRefresh() || needFullRefresh) {
				
				// Check time of last update for this feedId
				Long time = mArticlesUpdated.get(feedId);
				if (time == null) time = new Long(0);
				
				if (time < System.currentTimeMillis() - Utils.UPDATE_TIME) {
					
					String viewMode = (displayOnlyUnread ? "unread" : "all_articles");
					int articleLimit = Controller.getInstance().getArticleLimit();
					result = Controller.getInstance().getTTRSSConnector()
							.getFeedHeadlines(fId, articleLimit/2, 0, viewMode);
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
	
	public synchronized Map<String, List<FeedItem>> getFeeds(boolean needFullRefresh) {
		if (isOnline()) {
			if (mFeeds == null || needFullRefresh() || needFullRefresh) {
				
				// Only update counters once in 60 seconds
				if (mFeedsUpdated < System.currentTimeMillis() - Utils.UPDATE_TIME) {
					
					mFeeds = Controller.getInstance().getTTRSSConnector().getSubsribedFeeds();
					mFeedsUpdated = System.currentTimeMillis();
					
					DBHelper.getInstance().deleteFeeds();
					for (String s : mFeeds.keySet()) {
						DBHelper.getInstance().insertFeeds(mFeeds.get(s));
					}
				}
			}
		}
		return mFeeds;
	}
	
	private synchronized List<CategoryItem> internalGetVirtualCategories(boolean needFullRefresh) {
		if (mVirtCategoriesUpdated > System.currentTimeMillis() - Utils.UPDATE_TIME) {
			return mVirtCategories;
		}
		
		if (isOnline()) {
			if (mVirtCategories == null || needFullRefresh() || needFullRefresh) {
				
				// Only update counters once in 60 seconds
				if (mVirtCategoriesUpdated < System.currentTimeMillis() - Utils.UPDATE_TIME) {
					
					boolean showUnread = Controller.getInstance().isDisplayUnreadInVirtualFeeds();
					
					mVirtCategories = new ArrayList<CategoryItem>();
					mVirtCategoriesUpdated = System.currentTimeMillis();
					
					// Refresh CategoryCounters
					getCategoryCounters(true);
					
					CategoryItem catItem;
					catItem = new CategoryItem("-1", "Starred articles", showUnread ? getCategoryUnreadCount("-1") : 0);
					mVirtCategories.add(catItem);
					catItem = new CategoryItem("-2", "Published articles", showUnread ? getCategoryUnreadCount("-2") : 0);
					mVirtCategories.add(catItem);
					catItem = new CategoryItem("-3", "Fresh articles", showUnread ? getCategoryUnreadCount("-3") : 0);
					mVirtCategories.add(catItem);
					catItem = new CategoryItem("-4", "All articles", showUnread ? getCategoryUnreadCount("-4") : 0);
					mVirtCategories.add(catItem);
					catItem = new CategoryItem("0", "Uncategorized Feeds", showUnread ? getCategoryUnreadCount("0") : 0);
					mVirtCategories.add(catItem);
					
					DBHelper.getInstance().insertCategories(mVirtCategories);
				}
				
			}
		}
		return mVirtCategories;
	}
	
	private synchronized List<CategoryItem> internalGetCategories(boolean needFullRefresh) {
		if (isOnline()) {
			if (mCategories == null || needFullRefresh() || needFullRefresh) {
				
				// Only update counters once in 60 seconds
				if (mCategoriesUpdated < System.currentTimeMillis() - Utils.UPDATE_TIME) {
					
					mCategories = Controller.getInstance().getTTRSSConnector().getCategories();
					mCategoriesUpdated = System.currentTimeMillis();
					
					DBHelper.getInstance().deleteCategories(false);
					DBHelper.getInstance().insertCategories(mCategories);
				}
			}
		}
		return mCategories;
	}
	
	
	// ********** META-FUNCTIONS **********
	
	public int getCategoryUnreadCount(String catId) {
		Map<CategoryItem, List<FeedItem>> map = getCategoryCounters(false);
		
		if (map != null) {
			for (CategoryItem c : map.keySet()) {
				if (catId.equals(c.getId())) {
					return c.getUnreadCount();
				}
			}
		}
		
		return 0;
	}
	
	public ArticleItem getArticleHeadline(String feedId, String articleId) {
		for (ArticleItem a : getArticlesHeadlines(feedId, false, false)) {
			if (a.getId().equals(articleId)) {
				return a;
			}
		}
		return null;
	}
	
	public ArticleItem getArticleWithContent(String articleId) {
		ArticleItem result = null;
		if (!needFullRefresh()) {
			result = DBHelper.getInstance().getArticle(articleId);
		} else {
			result = Controller.getInstance().getTTRSSConnector().getArticle(Integer.parseInt(articleId));
			
			int articleLimit = Controller.getInstance().getArticleLimit();
			DBHelper.getInstance().insertArticle(result, articleLimit);
		}
		
		if (result != null && !result.isContentLoaded()) {
			Log.i(Utils.TAG, "Loading Content for Article \"" + result.getTitle() + "\"");
			
			result = Controller.getInstance().getTTRSSConnector().getArticle(Integer.parseInt(articleId));
			
			DBHelper.getInstance().updateArticleContent(result);
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public List<ArticleItem> getArticlesWithContent(String feedId, boolean displayOnlyUnread, boolean needFullRefresh) {
		if (feedId.startsWith("-")) {
			feedId = "-1";
		}
		FeedItem fi = new FeedItem();
		fi.setId(feedId);
		
		List<ArticleItem> result = DBHelper.getInstance().getArticles(fi, true);
		
		boolean needRefresh = false;
		for (ArticleItem a : result) {
			if (!a.isContentLoaded()) {
				needRefresh = true;
				break;
			}
		}
		
		// Also do update if needFullRefresh given and not working offline
		if (needFullRefresh && !Controller.getInstance().isWorkOffline()) needRefresh = true;
		
		if (result == null || needRefresh) {
			// Check time of last update for this feedId
			Long time = mArticlesUpdated.get(feedId);
			if (time == null) time = new Long(0);
			
			if (time < System.currentTimeMillis() - Utils.UPDATE_TIME) {
				
				result = Controller.getInstance().getTTRSSConnector()
						.getFeedArticles(Integer.parseInt(feedId), displayOnlyUnread ? 1 : 0, false);
				
				if (result == null) {
					return null;
				}
				
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
				if (a.isUnread()) tempList.add(a);
			}
			
			result = tempList;
		}
		
		return result;
	}
	
	public FeedItem getFeed(String feedId, boolean displayOnlyUnread) {
		Map<String, List<FeedItem>> map = getFeeds(false);
		
		if (map == null) {
			return null;
		}
		
		for (String s : map.keySet()) {
			for (FeedItem f : map.get(s)) {
				if (f.getId().equals(feedId) && f.getUnread() > 0) {
					return f;
				}
			}
		}
		return null;
	}
	
	public List<FeedItem> getFeeds(String categoryId, boolean displayOnlyUnread, boolean needFullRefresh) {
		Map<String, List<FeedItem>> map = getFeeds(needFullRefresh);
		
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
	
	public CategoryItem getVirtualCategory(String categoryId) {
		for (CategoryItem c : internalGetVirtualCategories(false)) {
			if (c.getId().equals(categoryId)) {
				return c;
			}
		}
		return null;
	}
	
	public CategoryItem getCategory(String categoryId, boolean withVirtuals) {
		List<CategoryItem> categories = new ArrayList<CategoryItem>();
		
		if (withVirtuals) {
			categories.addAll(internalGetVirtualCategories(false));
			Collections.sort(categories, new VirtualCategoryItemComparator());
		}
		
		List<CategoryItem> list = internalGetCategories(false);
		if (list == null) {
			return null;
		}
		categories.addAll(list);
		
		for (CategoryItem c : categories) {
			if (c.getId().equals(categoryId)) {
				return c;
			}
		}
		return null;
	}
	
	public List<CategoryItem> getCategories(boolean withVirtuals, boolean displayOnlyUnread, boolean needFullRefresh) {
		List<CategoryItem> categories = new ArrayList<CategoryItem>();
		
		if (withVirtuals) {
			categories.addAll(internalGetVirtualCategories(needFullRefresh));
			Collections.sort(categories, new VirtualCategoryItemComparator());
		}
		
		List<CategoryItem> categoryList = internalGetCategories(needFullRefresh);
		if (categoryList != null) {
			Collections.sort(categoryList, new CategoryItemComparator());
			categories.addAll(categoryList);
		}
		
		// If option "ShowUnreadOnly" is enabled filter out all categories without unread items
		if (displayOnlyUnread && categories != null) {
			
			List<CategoryItem> catList = new ArrayList<CategoryItem>();
			for (CategoryItem ci : categories) {
				// Dont filter for virtual Categories, only 0 (uncategorized feeds) are filtered
				if (ci.getId().startsWith("-")) {
					catList.add(ci);
					continue;
				}
				
				// Refresh CategoryCounters
				getCategoryCounters(needFullRefresh);
				
				if (getCategoryUnreadCount(ci.getId()) > 0) {
					ci.setUnreadCount(getCategoryUnreadCount(ci.getId()));
					catList.add(ci);
				}
			}
			categories = catList;
		}
		
		return categories;
	}
	
	public void getNewArticles() {
		// TODO: Auf die aktuelle API-funktion anpassen..
		
		// Force update counters
		// mCounters = null;
		// getCategoryUnreadCount("0");
		//
		// long time = Controller.getInstance().getLastUpdateTime();
		// Controller.getInstance().setLastUpdateTime(System.currentTimeMillis());
		// List<ArticleItem> list = Controller.getInstance().getTTRSSConnector().getNewArticles(1, time);
		//
		// if (list != null && !list.isEmpty()) {
		//
		// int articleLimit = Controller.getInstance().getArticleLimit();
		// DBHelper.getInstance().insertArticles(list, articleLimit);
		// -> new DBInsertArticlesTask(articleLimit).execute(result);
		//
		// for (ArticleItem a : list) {
		//
		// List<ArticleItem> temp = mFeedsHeadlines.get(a.getFeedId());
		//
		// if (temp == null) temp = new ArrayList<ArticleItem>();
		// temp.add(a);
		//
		// }
		// }
	}
	
}
