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

import org.ttrssreader.controllers.DataController;
import org.ttrssreader.model.IRefreshable;

public class ArticleItemAdapter implements IRefreshable {

	private String mFeedId;
	private String mArticleId;
		
	private ArticleItem mArticle;
	
	public ArticleItemAdapter(String feedId, String articleId) {
		mFeedId = feedId;
		mArticleId = articleId;
	}
	
	public ArticleItem getArticle() {
		return mArticle;
	}
	
	@Override
	public void refreshData() {
		mArticle = DataController.getInstance().getSingleArticleWithFullContentLoaded(mFeedId, mArticleId);
	}

}
