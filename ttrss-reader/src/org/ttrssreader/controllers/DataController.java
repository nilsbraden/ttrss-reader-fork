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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
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
	
	private Map<CategoryItem, List<FeedItem>> mCounters;
	private long mCountersUpdated = 0;
	
	private Map<String, List<ArticleItem>> mFeedsHeadlines;
	private Map<String, List<FeedItem>> mSubscribedFeeds;
	private List<CategoryItem> mVirtualCategories;
	private List<CategoryItem> mCategories;

	private NetworkInfo info;
	
	private DataController() {
		
		mCounters = DBHelper.getInstance().getCounters();
		mFeedsHeadlines = DBHelper.getInstance().getArticles(0, false);
		mSubscribedFeeds = DBHelper.getInstance().getFeeds();
		mVirtualCategories = DBHelper.getInstance().getVirtualCategories();
		mCategories = DBHelper.getInstance().getCategories(false);
		
		if (mCounters.isEmpty()) mCounters = new HashMap<CategoryItem, List<FeedItem>>();
		if (mFeedsHeadlines.isEmpty()) mFeedsHeadlines = new HashMap<String, List<ArticleItem>>();
		if (mSubscribedFeeds.isEmpty()) mSubscribedFeeds = null;
		if (mVirtualCategories.isEmpty()) mVirtualCategories = null;
		if (mCategories.isEmpty()) mCategories = null;
	}
	
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
		if (isOnline()) {
			return mForceFullRefresh || Controller.getInstance().isAlwaysFullRefresh();
		} else return false;
	}
	
	public boolean isOnline() {
		if (info != null) {
			return info.isConnected();
		}
		return false;
	}
	
	private List<CategoryItem> internalGetVirtualCategories() {
		if (mVirtualCategories == null || needFullRefresh()) {
			synchronized (mVirtualCategories = new ArrayList<CategoryItem>()) {

				// Check again to make sure it has not been updated while we were waiting
				if (mVirtualCategories.isEmpty() || needFullRefresh()) {
					boolean showUnread = Controller.getInstance().isDisplayUnreadInVirtualFeeds();
					
					CategoryItem categoryItem;
					categoryItem = new CategoryItem("-1", "Starred articles", showUnread ? getCategoryUnreadCount("-1") : 0);
					mVirtualCategories.add(categoryItem);
					categoryItem = new CategoryItem("-2", "Published articles", showUnread ? getCategoryUnreadCount("-2") : 0);
					mVirtualCategories.add(categoryItem);
					categoryItem = new CategoryItem("-3", "Fresh articles", showUnread ? getCategoryUnreadCount("-3") : 0);
					mVirtualCategories.add(categoryItem);
					categoryItem = new CategoryItem("-4", "All articles", showUnread ? getCategoryUnreadCount("-4") : 0);
					mVirtualCategories.add(categoryItem);
					categoryItem = new CategoryItem("0", "Uncategorized Feeds", showUnread ? getCategoryUnreadCount("0") : 0);
					mVirtualCategories.add(categoryItem);
					
					DBHelper.getInstance().insertCategories(mVirtualCategories);
				}
				
			}
		}
		return mVirtualCategories;
	}
	
	private List<CategoryItem> internalGetCategories() {
		if (mCategories == null || needFullRefresh()) {
			synchronized (mCategories = new ArrayList<CategoryItem>()) {
				
				// Check again to make sure it has not been updated while we were waiting
				if (mCategories.isEmpty() || needFullRefresh()) {
					mCategories = Controller.getInstance().getTTRSSConnector().getCategories();
					
					DBHelper.getInstance().deleteCategories();
					DBHelper.getInstance().insertCategories(mCategories);
				}

			}
		}
		return mCategories;
	}
	
	public int getCategoryUnreadCount(String catId) {
		if (mCounters == null || needFullRefresh()) {
			
			// Only update counters once in 60 seconds
			if (mCountersUpdated < System.currentTimeMillis() - 60000) {
				mCounters = Controller.getInstance().getTTRSSConnector().getCounters();
				DBHelper.getInstance().setCounters(mCounters);
				mCountersUpdated = System.currentTimeMillis();
			}
		}
		
		if (mCounters != null) {
			for (CategoryItem c : mCounters.keySet()) {
				if (catId.equals(c.getId())) {
					return c.getUnreadCount();
				}
			}
		}
			
		return 0;
	}
	
	public CategoryItem getVirtualCategory(String categoryId) {
		CategoryItem result = null;
		
		CategoryItem item;
		
		Iterator<CategoryItem> iter = internalGetVirtualCategories().iterator();
		while ((iter.hasNext()) && (result == null)) {
			item = iter.next();
			
			if (item.getId().equals(categoryId)) {
				result = item;
			}
		}
		
		return result;
	}
	
	public CategoryItem getCategory(String categoryId, boolean withVirtuals) {
		CategoryItem result = null;
		CategoryItem item;
		List<CategoryItem> finalList = new ArrayList<CategoryItem>();
		
		if (withVirtuals) {
			finalList.addAll(internalGetVirtualCategories());
			Collections.sort(finalList, new VirtualCategoryItemComparator());
		}
		
		List<CategoryItem> list = internalGetCategories();
		if (list == null) {
			return null;
		}
		finalList.addAll(list);
		
		Iterator<CategoryItem> iter = finalList.iterator();
		while ((iter.hasNext()) && (result == null)) {
			item = iter.next();
			
			if (item.getId().equals(categoryId)) {
				result = item;
			}
		}
		
		return result;
	}
	
	public synchronized List<CategoryItem> getCategories(boolean withVirtuals, boolean displayOnlyUnread) {
		
		List<CategoryItem> finalList = new ArrayList<CategoryItem>();
		
		if (withVirtuals) {
			finalList.addAll(internalGetVirtualCategories());
			Collections.sort(finalList, new VirtualCategoryItemComparator());
		}
		
		List<CategoryItem> categoryList = internalGetCategories();
		if (categoryList != null) {
			Collections.sort(categoryList, new CategoryItemComparator());
			finalList.addAll(categoryList);
		} else {
			return null;
		}
		
		// If option "ShowUnreadOnly" is enabled filter out all categories without unread items
		if (displayOnlyUnread && finalList != null) {
			
			List<CategoryItem> catList = new ArrayList<CategoryItem>();
			
			for (CategoryItem ci : finalList) {

				// Dont filter for virtual Categories, only 0 (uncategorized feeds) are filtered
				if (ci.getId().startsWith("-")) {
					catList.add(ci);
					continue;
				}
				
				if (getCategoryUnreadCount(ci.getId()) > 0) {
					ci.setUnreadCount(getCategoryUnreadCount(ci.getId()));
					catList.add(ci);
				}
				
			}
			finalList = catList;
		}
		
		return finalList;
	}
	
	public Map<String, List<FeedItem>> getSubscribedFeeds() {
		if (mSubscribedFeeds == null || needFullRefresh()) {
			synchronized (mSubscribedFeeds = new HashMap<String, List<FeedItem>>()) {
				
				// Check again to make sure it has not been updated while we were waiting
				if (mSubscribedFeeds.isEmpty() || needFullRefresh()) {
					mSubscribedFeeds = Controller.getInstance().getTTRSSConnector().getSubsribedFeeds();
					
					DBHelper.getInstance().deleteFeeds();
					for (String s : mSubscribedFeeds.keySet()) {
						DBHelper.getInstance().insertFeeds(mSubscribedFeeds.get(s));
					}
				}
				
			}
		}
		return mSubscribedFeeds;
	}
	
	public List<FeedItem> getSubscribedFeeds(String categoryId, boolean displayOnlyUnread) {
		Map<String, List<FeedItem>> map = getSubscribedFeeds();
		
		List<FeedItem> result = new ArrayList<FeedItem>();
		if (map != null) {
			result = map.get(categoryId);
		} else {
			return null;
		}
		
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
	
	public FeedItem getFeed(String categoryId, String feedId, boolean displayOnlyUnread) {
		List<FeedItem> feedList = getSubscribedFeeds(categoryId, displayOnlyUnread);
		
		FeedItem result = null;
		
		FeedItem tempFeed;
		Iterator<FeedItem> iter = feedList.iterator();
		while ((result == null) && (iter.hasNext())) {
			
			tempFeed = iter.next();
			
			if (tempFeed.getId().equals(feedId)) {
				result = tempFeed;
			}
		}
		
		return result;
	}
	
	/**
	 * Same as getFeed(String categoryId, String feedId), but slower as it as to look through categories.
	 * 
	 * @param feedId
	 * @return
	 */
	public FeedItem getFeed(String feedId, boolean displayOnlyUnread) {
		Map<String, List<FeedItem>> feedsList = getSubscribedFeeds();
		
		if (feedsList == null) {
			return null;
		}
		
		FeedItem result = null;
		
		Set<String> categories = feedsList.keySet();
		
		Iterator<String> categoryIter = categories.iterator();
		while ((categoryIter.hasNext()) && (result == null)) {
			result = getFeed(categoryIter.next(), feedId, displayOnlyUnread);
		}
		
		return result;
	}
	
	public List<ArticleItem> getArticlesHeadlines(String feedId, boolean displayOnlyUnread) {
		int fId = new Integer(feedId);
		List<ArticleItem> result = null;
		
		// TODO: Add check for fresh articles here?
		if (fId == -4) {
			result = new ArrayList<ArticleItem>();
			for (Entry<String, List<ArticleItem>> e : mFeedsHeadlines.entrySet()) {
				result.addAll(e.getValue());
			}
		} else {
			result = mFeedsHeadlines.get(feedId);
		}
		
		if (result == null || needFullRefresh()) {
			synchronized (mFeedsHeadlines) {
				
				// Check again to make sure it has not been updated while we were waiting
				if (result == null || needFullRefresh()) {
					
					String viewMode = (displayOnlyUnread ? "unread" : "all_articles");
					int articleLimit = Controller.getInstance().getArticleLimit();
					result = Controller.getInstance().getTTRSSConnector()
							.getFeedHeadlines(new Integer(feedId), articleLimit, 0, viewMode);
					
					if (result == null) {
						return null;
					}
					
					long start = System.currentTimeMillis();
					DBHelper.getInstance().insertArticles(result, articleLimit);
					Log.i(Utils.TAG, "Inserting took " + (System.currentTimeMillis()-start) + "ms.");
					
					// Refresh Headlines from DB so they get reduced to articleLimit too.
					mFeedsHeadlines = DBHelper.getInstance().getArticles(0, false);
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
	
	public ArticleItem getArticleHeadline(String feedId, String articleId) {
		List<ArticleItem> articlesList = getArticlesHeadlines(feedId, false);
		
		ArticleItem result = null;
		
		ArticleItem tempArticle;
		Iterator<ArticleItem> iter = articlesList.iterator();
		while ((result == null) && (iter.hasNext())) {
			
			tempArticle = iter.next();
			
			if (tempArticle.getId().equals(articleId)) {
				result = tempArticle;
			}
		}
		
		return result;
	}
	
	/**
	 * Same has getSingleArticleForFeedsHeadlines(), except that it load full article content if necessary.
	 * 
	 * @param feedId
	 * @param articleId
	 * @return
	 */
	public ArticleItem getArticleWithContent(String articleId) {
		
		ArticleItem result = null;
		if (!needFullRefresh()) {
			result = DBHelper.getInstance().getArticle(articleId);
		}
		
		if (result == null || needFullRefresh()) {
			
			result = Controller.getInstance().getTTRSSConnector().getArticle(Integer.parseInt(articleId));
			
			int articleLimit = Controller.getInstance().getArticleLimit();
			DBHelper.getInstance().insertArticle(result, articleLimit);
		}
		
		if (result != null) {
			if (!result.isContentLoaded()) {
				Log.i(Utils.TAG, "Loading Content for Article \"" + result.getTitle() + "\"");
				
				result = Controller.getInstance().getTTRSSConnector().getArticle(Integer.parseInt(articleId));
				
				DBHelper.getInstance().updateArticleContent(result);
			}
		}
		
		return result;
	}
	
	public List<ArticleItem> getArticlesWithContent(String feedId) {
		FeedItem fi = new FeedItem();
		if (feedId.startsWith("-")) {
			feedId = "-1";
		}
		fi.setId(feedId);
		
		List<ArticleItem> result = null;
		
		result = DBHelper.getInstance().getArticles(fi, true);
		
		boolean needRefresh = false;
		for (ArticleItem a : result) {
			if (!a.isContentLoaded()) {
				needRefresh = true;
				break;
			}
		}
		
		if (result == null || needRefresh) {
			synchronized (mFeedsHeadlines) {
				
				// Check again to make sure it has not been updated while we were waiting
				if (result == null || needRefresh) {
					
					List<ArticleItem> temp = Controller.getInstance().getTTRSSConnector()
							.getFeedArticles(Integer.parseInt(feedId), 1, false);
					if (temp == null) {
						return null;
					}
					
					result.addAll(temp);
					
					int articleLimit = Controller.getInstance().getArticleLimit();
					DBHelper.getInstance().insertArticles(temp, articleLimit);
					
					// Refresh Headlines from DB so they get reduced to articleLimit too.
					mFeedsHeadlines = DBHelper.getInstance().getArticles(0, false);
				
				}
				
			}
		}
		return result;
	}
	
}
