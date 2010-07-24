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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.model.category.CategoryItem;
import org.ttrssreader.model.category.CategoryItemComparator;
import org.ttrssreader.model.category.VirtualCategoryItemComparator;
import org.ttrssreader.model.feed.FeedItem;
import org.ttrssreader.utils.Utils;
import android.util.Log;

public class DataController {
	
	private static DataController mInstance = null;
	
	private boolean mForceFullRefresh = false;
	private boolean mForceFullReload= false;
	
	private Map<String, List<ArticleItem>> mFeedsHeadlines;
	private Map<String, List<FeedItem>> mSubscribedFeeds;
	private List<CategoryItem> mVirtualCategories;
	private List<CategoryItem> mCategories;
	
	private int articleLimit;
	
	private DataController() {

		articleLimit = Controller.getInstance().getArticleLimit();
		
		mFeedsHeadlines = DBHelper.getInstance().getArticles(articleLimit);
		mSubscribedFeeds = DBHelper.getInstance().getFeeds();
		mVirtualCategories = DBHelper.getInstance().getVirtualCategories();
		mCategories = DBHelper.getInstance().getCategories();
		
		if (mFeedsHeadlines.isEmpty()) mFeedsHeadlines = new HashMap<String, List<ArticleItem>>();
		if (mSubscribedFeeds.isEmpty()) mSubscribedFeeds = null;
		if (mVirtualCategories.isEmpty()) mVirtualCategories = null;
		if (mCategories.isEmpty()) mCategories = null;
		
		purgeArticlesNumber();
	}
	
	public static DataController getInstance() {
		if (mInstance == null) {
			mInstance = new DataController();
		}
		return mInstance;
	}
	
	public void forceFullRefresh() {
		mForceFullRefresh = true;
	}
	
	/*
	 * Changed forceRefresh-Behaviour because we need several runs with fullRefresh in Main-Activity (Categorys and
	 * FeedHeadLineList) but it was automatically reset inbetween. Now we set it by hand and reset it afterwards.
	 */
	public void disableFullRefresh() {
		mForceFullRefresh = false;
	}
	
	public void forceFullReload() {
		mForceFullReload = true;
	}
	
	private boolean needFullRefresh() {
		return mForceFullRefresh || Controller.getInstance().isAlwaysPerformFullRefresh();
	}
	
	private List<CategoryItem> internalGetVirtualCategories() {
		if (mVirtualCategories == null || needFullRefresh()) {
			Log.i(Utils.TAG, "Refreshing Data: internalGetVirtualCategories()");
			mVirtualCategories = Controller.getInstance().getTTRSSConnector().getVirtualFeeds();

			DBHelper.getInstance().insertCategories(mVirtualCategories);
			
			// mForceFullRefresh = false;
		}
		return mVirtualCategories;
	}
	
	private List<CategoryItem> internalGetCategories() {
		if ((mCategories == null) || (needFullRefresh())) {
			Log.i(Utils.TAG, "Refreshing Data: internalGetCategories()");
			mCategories = Controller.getInstance().getTTRSSConnector().getCategories();

			DBHelper.getInstance().insertCategories(mCategories);
			
			// mForceFullRefresh = false;
		}
		return mCategories;
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
	
	public CategoryItem getCategory(String categoryId) {
		CategoryItem result = null;
		
		CategoryItem item;
		
		Iterator<CategoryItem> iter = internalGetCategories().iterator();
		while ((iter.hasNext()) && (result == null)) {
			item = iter.next();
			
			if (item.getId().equals(categoryId)) {
				result = item;
			}
		}
		
		return result;
	}
	
	public List<CategoryItem> getCategories(boolean withVirtuals, boolean displayOnlyUnread) {

		List<CategoryItem> finalList = new ArrayList<CategoryItem>();
		
		if (withVirtuals) {
			List<CategoryItem> virtualList = internalGetVirtualCategories();
			
			if (virtualList != null) {
				finalList.addAll(virtualList);
				Collections.sort(finalList, new VirtualCategoryItemComparator());
			}
		}
		
		List<CategoryItem> tempList = new ArrayList<CategoryItem>();
		List<CategoryItem> categoryList = internalGetCategories();
		
		if (categoryList != null) {
			finalList.addAll(categoryList);
			Collections.sort(tempList, new CategoryItemComparator());
		}
		
		finalList.addAll(tempList);
		
		// If option "ShowUnreadOnly" is enabled filter out all categories without unread items
		if (displayOnlyUnread && finalList != null) {
			List<CategoryItem> catList = new ArrayList<CategoryItem>();
			
			for (CategoryItem ci : finalList) {
				
				// Dont filter for virtual Categories
				if (new Integer(ci.getId()).intValue() < 0) {
					catList.add(ci);
					continue;
				}
				
				List<FeedItem> temp = getSubscribedFeedsByCategory(ci.getId(), displayOnlyUnread);
				if (temp != null && temp.size() > 0) {
					catList.add(ci);
				}
			}
			
			// Overwrite old list with filtered one
			finalList = catList;
		}
		
		return finalList;
	}
	
	public Map<String, List<FeedItem>> getSubscribedFeeds() {
		if (mSubscribedFeeds == null || needFullRefresh()) {
			Log.i(Utils.TAG, "Refreshing Data: getSubscribedFeeds()");
			mSubscribedFeeds = Controller.getInstance().getTTRSSConnector().getSubsribedFeeds();

			for (String s : mSubscribedFeeds.keySet()) {
				DBHelper.getInstance().insertFeeds(mSubscribedFeeds.get(s));
			}
			// mForceFullRefresh = false;
		}
		return mSubscribedFeeds;
	}
	
	public List<FeedItem> getSubscribedFeedsByCategory(String categoryId, boolean displayOnlyUnread) {
		Map<String, List<FeedItem>> map = getSubscribedFeeds();
		List<FeedItem> result = new ArrayList<FeedItem>();
		if (map != null) {
			result = map.get(categoryId);
		}
		
		// If option "ShowUnreadOnly" is enabled filter out all Feeds without unread items
		if (displayOnlyUnread && result != null) {
			List<FeedItem> feedList = new ArrayList<FeedItem>();
			for (FeedItem fi : result) {
				if (fi.getUnreadCount() > 0) {
					feedList.add(fi);
				}
			}
			
			// Overwrite old list with filtered one
			result = feedList;
		}
		
		return result;
	}
	
	public FeedItem getFeed(String categoryId, String feedId, boolean displayOnlyUnread) {
		List<FeedItem> feedList = getSubscribedFeedsByCategory(categoryId, displayOnlyUnread);
		
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
		
		FeedItem result = null;
		
		Set<String> categories = feedsList.keySet();
		
		Iterator<String> categoryIter = categories.iterator();
		while ((categoryIter.hasNext()) && (result == null)) {
			result = getFeed(categoryIter.next(), feedId, displayOnlyUnread);
		}
		
		return result;
	}
	
	public List<ArticleItem> getArticlesForFeedsHeadlines(String feedId, boolean displayOnlyUnread) {
		List<ArticleItem> result = mFeedsHeadlines.get(feedId);
		
		if (result == null || needFullRefresh()) {
			Log.i(Utils.TAG, "Refreshing Data: getArticlesForFeedsHeadlines()");
			
			result = Controller.getInstance().getTTRSSConnector().getFeedHeadlines(new Integer(feedId).intValue(), articleLimit, 0);
			
			mFeedsHeadlines.put(feedId, result);
			
			DBHelper.getInstance().insertArticles(result, articleLimit);

			// mForceFullRefresh = false;
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
	
	public ArticleItem getSingleArticleForFeedsHeadlines(String feedId, String articleId) {
		List<ArticleItem> articlesList = getArticlesForFeedsHeadlines(feedId, false);
		
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
	public ArticleItem getSingleArticleWithFullContentLoaded(String feedId, String articleId) {
		
		ArticleItem result = getSingleArticleForFeedsHeadlines(feedId, articleId);
		
		if (result != null) {
			if (!result.isContentLoaded()) {
				Log.i(Utils.TAG, "Refreshing Data: getSingleArticleWithFullContentLoaded() -> doLoadContent()");
				result.doLoadContent();
				DBHelper.getInstance().updateArticleContent(result);
			}
		}
		
		return result;
	}
	
	public List<ArticleItem> getArticlesWithFullContentLoaded(String feedId) {
		
		List<ArticleItem> result = getArticlesForFeedsHeadlines(feedId, false);
		
		if (result != null) {
			for (ArticleItem a : result) {
				if (!a.isContentLoaded()) {
					Log.i(Utils.TAG, "Refreshing Data: getArticlesWithFullContentLoaded() -> doLoadContent()");
					a.doLoadContent();
					DBHelper.getInstance().updateArticleContent(a);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Iterates over all the given items and marks them and all sub-items as read. 
	 * 
	 * @param id the id of the item
	 * @param isCategory indicates whether the item is a category (or a feed if false)
	 */
	public void markAllRead(String id, boolean isCategory) {
		try {
			
			List<FeedItem> feeds = new ArrayList<FeedItem>();
			
			if (id.startsWith("-") || isCategory) {
				// Virtual Category or Category
				
				List<CategoryItem> iterate = getCategories(true, true);
				if (iterate != null) {
					
					for (CategoryItem categoryItem : iterate) {
						Log.i(Utils.TAG, "Marking category as read: " + categoryItem.getTitle());
						if (categoryItem.getId().equals(id)) {
							categoryItem.setUnreadCount(0);
							DBHelper.getInstance().updateCategoryUnreadCount(categoryItem.getId(), 0);
						}
						List<FeedItem> thisFeed = mSubscribedFeeds.get(categoryItem.getId());
						if (thisFeed != null) {
							feeds.addAll(thisFeed);
						}
					}

				}
			} else {
				// Feed
				FeedItem feedItem = getFeed(id, true);
				if (feedItem != null) {
					feeds.add(feedItem);
				}
			}
			
			// Set all items as read here
			for (FeedItem feedItem : feeds) {
				Log.i(Utils.TAG, "Marking feed as read: " + feedItem.getTitle());
				
				feedItem.setUnreadCount(0);
				DBHelper.getInstance().markFeedRead(feedItem);
			}
			
		} catch (NullPointerException npe) {
			npe.printStackTrace();
			Log.e(Utils.TAG, "Catched NPE in DataController.markAllRead(). " +
					"All articles should be marked read on server though, so " +
					"refreshing from menu should do the trick.");
		} finally {
			
			Controller.getInstance().getTTRSSConnector().setRead(id, isCategory);
			

		}
	}
	
	/**
	 * Purge articles that are older then [days].
	 * Only removes read articles, unread articles are not touched.
	 */
	public void purgeArticlesDays(int days) {
		long date = System.currentTimeMillis() - (days * 1000 * 60 * 60 * 24);
		DBHelper.getInstance().purgeArticlesDays(new Date(date));
	}
	
	/**
	 * Purge old articles so only the newest [number] articles are left.
	 * Only removes read articles, unread articles are not touched.
	 * 
	 * @param number
	 */
	public void purgeArticlesNumber() {
		Log.i(Utils.TAG, "Puring old articles... (Keeping " + articleLimit + " articles)");
		
		DBHelper.getInstance().purgeArticlesNumber(articleLimit);
		
		mFeedsHeadlines = DBHelper.getInstance().getArticles(articleLimit);
	}
	
	public void reloadEverything() {
		if (!mForceFullReload) {
			mForceFullReload = false;
			return;
		}
		
		DBHelper.getInstance().deleteAll();
		
		internalGetCategories();
		internalGetVirtualCategories();
		getSubscribedFeeds();
		
		for (String s : mSubscribedFeeds.keySet()) {
			List<FeedItem> list = mSubscribedFeeds.get(s);
			for (FeedItem f : list) {
				getArticlesForFeedsHeadlines(f.getId(), false);
//				getArticlesWithFullContentLoaded(f.getId());
			}
		}
	}
	
}
