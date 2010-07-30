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
import java.util.Map.Entry;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.model.category.CategoryItem;
import org.ttrssreader.model.category.CategoryItemComparator;
import org.ttrssreader.model.category.VirtualCategoryItemComparator;
import org.ttrssreader.model.feed.FeedItem;
import org.ttrssreader.utils.Utils;
import android.util.Log;

public class DataController {
	
	private static String mutex = "";
	private static DataController mInstance = null;
	
	private boolean mForceFullRefresh = false;
	
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
		
		if (mFeedsHeadlines.isEmpty())
			mFeedsHeadlines = new HashMap<String, List<ArticleItem>>();
		if (mSubscribedFeeds.isEmpty())
			mSubscribedFeeds = null;
		if (mVirtualCategories.isEmpty())
			mVirtualCategories = null;
		if (mCategories.isEmpty())
			mCategories = null;
		
		purgeArticles();
	}
	
	public static DataController getInstance() {
		synchronized (mutex) {
			if (mInstance == null) {
				mInstance = new DataController();
			}
		}
		return mInstance;
	}
	
	public void forceFullRefresh() {
		mForceFullRefresh = true;
	}
	
	public void disableForceFullRefresh() {
		mForceFullRefresh = false;
	}
	
	private boolean needFullRefresh() {
		return mForceFullRefresh || Controller.getInstance().isAlwaysPerformFullRefresh();
	}
	
	private List<CategoryItem> internalGetVirtualCategories() {
		if (mVirtualCategories == null || needFullRefresh()) {
			mVirtualCategories = new ArrayList<CategoryItem>();
			boolean showUnread = Controller.getInstance().isShowUnreadInVirtualFeeds();
			
			CategoryItem categoryItem;
			categoryItem = new CategoryItem("-1", "Starred articles", showUnread ? getUnreadCount(-1) : 0);
			mVirtualCategories.add(categoryItem);
			categoryItem = new CategoryItem("-2", "Published articles", showUnread ? getUnreadCount(-2) : 0);
			mVirtualCategories.add(categoryItem);
			categoryItem = new CategoryItem("-3", "Fresh articles", showUnread ? getUnreadCount(-3) : 0);
			mVirtualCategories.add(categoryItem);
			categoryItem = new CategoryItem("-4", "All articles", showUnread ? getUnreadCount(-4) : 0);
			mVirtualCategories.add(categoryItem);
			categoryItem = new CategoryItem("0", "Uncategorized Feeds", showUnread ? getUnreadCount(0) : 0);
			mVirtualCategories.add(categoryItem);
			
			DBHelper.getInstance().insertCategories(mVirtualCategories);
		}
		return mVirtualCategories;
	}
	
	private List<CategoryItem> internalGetCategories() {
		if ((mCategories == null) || (needFullRefresh())) {
			mCategories = Controller.getInstance().getTTRSSConnector().getCategories();
			DBHelper.getInstance().insertCategories(mCategories);
		}
		return mCategories;
	}

	private int getUnreadCount(int feedId) {
		if (feedId == 0) {
			int ret = 0;
			for (FeedItem f : getSubscribedFeeds("0", true)) {
				ret += f.getUnread();
			}
			return ret;
		} else {
			List<ArticleItem> feedHeadlines = getArticlesHeadlines(feedId+"", true);
			return feedHeadlines.size();
		}
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
			finalList.addAll(internalGetVirtualCategories());
			Collections.sort(finalList, new VirtualCategoryItemComparator());
		}
		
		List<CategoryItem> categoryList = internalGetCategories();
		Collections.sort(categoryList, new CategoryItemComparator());
		if (categoryList != null) {
			finalList.addAll(categoryList);
		}
		
		// If option "ShowUnreadOnly" is enabled filter out all categories without unread items
		if (displayOnlyUnread && finalList != null) {
			List<CategoryItem> catList = new ArrayList<CategoryItem>();
			
			// Workaround for too much needFullRefresh-Confusion..
			getSubscribedFeeds();
			disableForceFullRefresh();
			
			for (CategoryItem ci : finalList) {
				
				// Dont filter for virtual Categories, only 0 (uncategorized feeds) are filtered
				if (new Integer(ci.getId()).intValue() < 0) {
					catList.add(ci);
					continue;
				}
				
				List<FeedItem> temp = getSubscribedFeeds(ci.getId(), displayOnlyUnread);
				if (temp != null && temp.size() > 0) {
					catList.add(ci);
					continue;
				}
			}
			finalList = catList;
			
			// Workaround for too much needFullRefresh-Confusion..
			forceFullRefresh();
		}
		
		return finalList;
	}
	
	public Map<String, List<FeedItem>> getSubscribedFeeds() {
		if (mSubscribedFeeds == null || needFullRefresh()) {
			mSubscribedFeeds = Controller.getInstance().getTTRSSConnector().getSubsribedFeeds();
			for (String s : mSubscribedFeeds.keySet()) {
				DBHelper.getInstance().insertFeeds(mSubscribedFeeds.get(s));
			}
		}
		return mSubscribedFeeds;
	}
	
	public List<FeedItem> getSubscribedFeeds(String categoryId, boolean displayOnlyUnread) {
		Map<String, List<FeedItem>> map = getSubscribedFeeds();
		List<FeedItem> result = new ArrayList<FeedItem>();
		if (map != null) {
			result = map.get(categoryId);
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
		
		if (fId == -4) {
			result = new ArrayList<ArticleItem>();
			for (Entry<String, List<ArticleItem>> e : mFeedsHeadlines.entrySet()) {
				result.addAll(e.getValue());
			}
		} else {
			result = mFeedsHeadlines.get(feedId);
		}
		
		if (result == null || needFullRefresh()) {
			String viewMode = (displayOnlyUnread ? "unread" : "all_articles");
			result = Controller.getInstance().getTTRSSConnector().
				getFeedHeadlines(new Integer(feedId), articleLimit, 0, viewMode);
			
			mFeedsHeadlines.put(feedId, result);
			
			DBHelper.getInstance().insertArticles(result);
			purgeArticles();
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
			
			DBHelper.getInstance().insertArticle(result);
			purgeArticles();
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
		fi.setId(feedId);
		
		List<ArticleItem> result = null;
		if (!needFullRefresh()) {
			result = DBHelper.getInstance().getArticles(fi);
		}
		
		if (result == null || needFullRefresh()) {
			
			result = Controller.getInstance().getTTRSSConnector().getFeedArticles(Integer.parseInt(feedId), 0, 0);
			
			DBHelper.getInstance().insertArticles(result);
			purgeArticles();
		}
		return result;
	}
	
	/**
	 * Iterates over all the given items and marks them and all sub-items as read.
	 * 
	 * @param id
	 *            the id of the item, beeing a feed or a caetgory
	 * @param isCategory
	 *            indicates whether the item is a category (or a feed if false)
	 */
	public void markAllRead(String id, boolean isCategory) {
		try {
			List<FeedItem> feeds = new ArrayList<FeedItem>();
			
			if (id.startsWith("-") || isCategory) { // Virtual Category or Category
			
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
				
				feedItem.setUnread(0);
				DBHelper.getInstance().markFeedRead(feedItem);
			}
			
		} catch (NullPointerException npe) {
			npe.printStackTrace();
			Log.e(Utils.TAG, "Catched NPE in DataController.markAllRead(). "
					+ "All articles should be marked read on server though, so "
					+ "refreshing from menu should do the trick.");
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
	public void purgeArticles() {
		DBHelper.getInstance().purgeArticlesNumber(articleLimit);
		mFeedsHeadlines = DBHelper.getInstance().getArticles(articleLimit);
	}
	
	public void updateUnread() {
		// Mark eveything as read
		for (CategoryItem c : mCategories) {
			DBHelper.getInstance().markCategoryRead(c);
		}
		
		forceFullRefresh(); // TODO: Fix this...
		internalGetCategories();
		internalGetVirtualCategories();
		getSubscribedFeeds();
		disableForceFullRefresh();
		
		// Leave article-content for now, its getting too slow.
		// for (String s : mSubscribedFeeds.keySet()) {
		// List<FeedItem> list = mSubscribedFeeds.get(s);
		// for (FeedItem f : list) {
		// getArticlesWithContent(f.getId());
		// }
		// }
	}
	
}
