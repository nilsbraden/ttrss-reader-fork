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

package org.ttrssreader.model;

import java.util.ArrayList;
import java.util.List;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.DataController;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.model.category.CategoryItem;
import org.ttrssreader.model.feed.FeedItem;

public class ReadStateUpdater implements IUpdatable {
	
	List<ArticleItem> mList = null;
	private String mPid = "";
	private int mArticleState;
	
	public ReadStateUpdater(List<CategoryItem> list, int articleState) {
		mList = new ArrayList<ArticleItem>();
		for (CategoryItem c : list) {
			mList.addAll(DataController.getInstance().getArticlesHeadlines(c.getId(), false));
		}
		mArticleState = articleState;
	}
	
	public ReadStateUpdater(CategoryItem c, String pid, int articleState) {
		mList = new ArrayList<ArticleItem>();
		mList.addAll(DataController.getInstance().getArticlesHeadlines(c.getId(), false));
		mPid = pid;
		mArticleState = articleState;
	}
	
	public ReadStateUpdater(List<FeedItem> list, String pid, int articleState, boolean isFeedList) {
		mList = new ArrayList<ArticleItem>();
		for (FeedItem f : list) {
			mList.addAll(DataController.getInstance().getArticlesHeadlines(f.getId(), false));
		}
		mPid = pid;
		mArticleState = articleState;
	}
	
	public ReadStateUpdater(FeedItem f, String pid, int articleState) {
		mList = new ArrayList<ArticleItem>();
		mList.addAll(DataController.getInstance().getArticlesHeadlines(f.getId(), false));
		mPid = pid;
		mArticleState = articleState;
	}
	
	public ReadStateUpdater(List<ArticleItem> list, String pid, int articleState) {
		mList = list;
		mPid = pid;
		mArticleState = articleState;
	}
	
	public ReadStateUpdater(ArticleItem article, String pid, int articleState) {
		mList = new ArrayList<ArticleItem>();
		mList.add(article);
		mPid = pid;
		mArticleState = articleState;
	}
	
	@Override
	public void update() {
		
		boolean bState = mArticleState == 1 ? true : false;
		int iState = mArticleState == 1 ? 1 : -1;
		
		List<String> idList = new ArrayList<String>();
		mList = filterList();
		
		for (ArticleItem article : mList) {
			// Build a list of article ids to update.
			idList.add(article.getId());
			
			String feedId = article.getFeedId();
			String articleId = article.getId();
			FeedItem mFeed = DataController.getInstance().getFeed(feedId, false);
			
			if (mFeed == null) {
				continue;
			}
			
			String categoryId = mFeed.getCategoryId();
			
			ArticleItem articleTemp = DataController.getInstance().getArticleHeadline(feedId, articleId);
			if (articleTemp != null) {
				articleTemp.setUnread(bState);
			}
			
			FeedItem feed = DataController.getInstance().getFeed(feedId, false);
			if (feed != null) {
				feed.setDeltaUnreadCount(iState);
				DBHelper.getInstance().updateFeedUnreadCount(feedId, categoryId, iState);
			}
			
			CategoryItem category = DataController.getInstance().getCategory(categoryId, true);
			if (category != null) {
				category.setDeltaUnreadCount(iState);
				DBHelper.getInstance().updateCategoryUnreadCount(categoryId, iState);
			}
			
			// If on a virtual feeds, also update article state in it.
			if (("-1".equals(mPid)) ||
					("-2".equals(mPid)) ||
					("-3".equals(mPid)) ||
					("-4".equals(mPid))) {
				
				DataController.getInstance()
						.getArticleHeadline(mPid, articleId).setUnread(bState);
				
				DataController.getInstance()
						.getVirtualCategory(mPid).setDeltaUnreadCount(iState);
			}
		}
		
		if (idList.size() > 0) {
			Controller.getInstance().getTTRSSConnector().setArticleRead(idList, mArticleState);
			DBHelper.getInstance().markArticlesRead(idList, mArticleState);
			
			int deltaUnread = mArticleState == 1 ? mList.size() : -mList.size();
			DataController.getInstance().getVirtualCategory("-4").setDeltaUnreadCount(deltaUnread);
		}
	}
	
	public List<ArticleItem> filterList() {
		List<ArticleItem> ret = new ArrayList<ArticleItem>();
		
		boolean state = mArticleState == 0 ? true : false;
		
		for (ArticleItem a : mList) {
			if (state && a.isUnread()) {
				ret.add(a);
			} else if (!state && !a.isUnread()) {
				ret.add(a);
			}
		}
		
		return ret;
	}
	
}
