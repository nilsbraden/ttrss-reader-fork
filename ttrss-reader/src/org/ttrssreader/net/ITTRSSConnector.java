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

package org.ttrssreader.net;

import java.util.List;
import java.util.Map;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.model.category.CategoryItem;
import org.ttrssreader.model.feed.FeedItem;

public interface ITTRSSConnector {
	
	public int getTotalUnread();
	
	public List<CategoryItem> getCategories();
	
	public Map<String, List<FeedItem>> getSubsribedFeeds();
	
	public ArticleItem getArticle(int articleId);
	
	public List<ArticleItem> getFeedHeadlines(int feedId, int limit, int filter, String viewMode);
	
	public List<ArticleItem> getFeedArticles(int id, int articleState, boolean isCategory);
	
	public Map<CategoryItem,Map<FeedItem, List<ArticleItem>>> getNewArticles(int articleState, long time);
	
	public void setArticleRead(List<String> articlesIds, int articleState);
	
	public void setRead(String id, boolean isCategory);
	
	public Map<CategoryItem, List<FeedItem>> getCounters();
	
	public boolean hasLastError();
	
	public String getLastError();
	
}
