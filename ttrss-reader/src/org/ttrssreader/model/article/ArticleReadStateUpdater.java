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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package org.ttrssreader.model.article;

import java.util.ArrayList;
import java.util.List;

import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DataController;
import org.ttrssreader.model.IUpdatable;
import org.ttrssreader.model.category.CategoryItem;
import org.ttrssreader.model.feed.FeedItem;
import android.os.AsyncTask;

public class ArticleReadStateUpdater extends AsyncTask<Object, Object, Object> implements IUpdatable {
	
	private List<ArticleItem> mArticleList;
	private String mFeedId;
	private int mArticleState;
	
	public ArticleReadStateUpdater(String feedId, List<ArticleItem> articleList, int articleState) {
		mFeedId = feedId;
		mArticleList = articleList;
		mArticleState = articleState;
	}
	
	public ArticleReadStateUpdater(String feedId, ArticleItem article, int articleState) {
		mFeedId = feedId;
		mArticleList = new ArrayList<ArticleItem>();
		mArticleList.add(article);
		mArticleState = articleState;
	}

	@Override
	protected Object doInBackground(Object... params) {
		update();
		return null;
	}
	
	@Override
	public void update() {
		boolean displayOnlyUnread = Controller.getInstance().isDisplayOnlyUnreadEnabled();
		
		String idList = "";

		for (ArticleItem article : mArticleList) {
			// Build a list of articles id to update.
			if (idList.length() > 0) {
				idList += ",";
			}
			
			idList += article.getId();
			
			String feedId = article.getFeedId();
			String articleId = article.getId();
			FeedItem mFeed = DataController.getInstance().getFeed(feedId, displayOnlyUnread);
			if (mFeed == null) {
				// Avoid NPE if "displayOnlyUnread" and getFeed returns null because feed got no unread items
				continue;
			}
			String categoryId = mFeed.getCategoryId();
			
			ArticleItem articleTemp = DataController.getInstance().getArticleHeadline(feedId, articleId);
			if (articleTemp != null) {
				articleTemp.setIsUnread(mArticleState == 1 ? true : false);
			}
			
			FeedItem feed = DataController.getInstance().getFeed(feedId, displayOnlyUnread);
			if (feed != null) {
				feed.setDeltaUnreadCount(mArticleState == 1 ? 1 : -1);
			}
			
			CategoryItem category = DataController.getInstance().getCategory(categoryId);
			if (category != null) {
				category.setDeltaUnreadCount(mArticleState == 1 ? 1 : -1);
			}
			
			// If on a virtual feeds, also update article state in it.
			if ((mFeedId.equals("-1")) ||
					(mFeedId.equals("-2")) ||
					(mFeedId.equals("-3")) ||
					(mFeedId.equals("-4"))) {
				
				DataController.getInstance()
					.getArticleHeadline(mFeedId, articleId).setIsUnread(
							mArticleState == 1 ? true : false);
				
				DataController.getInstance()
					.getVirtualCategory(mFeedId).setDeltaUnreadCount(
							mArticleState == 1 ? 1 : -1);
			}
		}
		
		Controller.getInstance().getTTRSSConnector().setArticleRead(idList, mArticleState);
				
		int deltaUnread = mArticleState == 1 ? mArticleList.size() : - mArticleList.size();
		DataController.getInstance().getVirtualCategory("-4").setDeltaUnreadCount(deltaUnread);
	}

}
