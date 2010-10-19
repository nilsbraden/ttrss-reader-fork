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

package org.ttrssreader.net;

import java.util.List;
import java.util.Map;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.model.category.CategoryItem;
import org.ttrssreader.model.feed.FeedItem;

public interface ITTRSSConnector {
    
    /**
     * Retrieves the number of unread articles.
     * 
     * @return the number of unread articles.
     */
    public int getTotalUnread();
    
    /**
     * Retrieves a map of feeds mapped to categories, each with only their ID and the unread-count set.
     * 
     * @return map of feeds mapped to categories. 
     */
    public Map<CategoryItem, List<FeedItem>> getCounters();
    
    /**
     * Retrieves all categories.
     * 
     * @return a list of categories.
     */
    public List<CategoryItem> getCategories();
    
    /**
     * Retrieves all feeds, mapped to their categories.
     * 
     * @return a map of all feeds for every category.
     */
    public Map<Integer, List<FeedItem>> getFeeds();
    
    /**
     * Retrieves the articles for the given feed or category, depending on boolean isCategory. Already read articles can be filtered.
     * 
     * @param id the id of the feed or category
     * @param displayOnlyUnread whether we are to filter out read articles
     * @param isCategory indicates if the id refers to a category or a feed
     * @return a list of articles
     */
    public List<ArticleItem> getArticles(int parentId, boolean displayOnlyUnread, boolean isCategory);
    
    /**
     * Retrieves the specified article.
     * 
     * @param articleId the id of the article.
     * @return the article.
     */
    public ArticleItem getArticle(int articleId);
    
    /**
     * Retrieves all articles for a given feed with their headlines to avoid too much traffic for the content.
     * 
     * @param feedId
     * @param limit
     * @param filter
     * @param viewMode
     * @return
     */
    @Deprecated
    public List<ArticleItem> getFeedHeadlines(int feedId, int limit, int filter, String viewMode);
    
    /**
     * Retrieves all new articles since the given time. ArticleState can be given to only retrieve unread articles. 
     * 
     * @param articleState int indicating whetether we are to fetch unread articles only (1) or all articles (0).
     * @param time the time in ms since 1970, specifying the time of the last update.
     * @return a map of List<ArticleItem> mapped to their feeds, mapped to their articles.
     */
    public Map<CategoryItem, Map<FeedItem, List<ArticleItem>>> getNewArticles(int articleState, long time /* ago */);
    
    /**
     * Marks the given list of article-Ids as read/unread depending on int articleState.
     * 
     * @param articlesIds the list of ids.
     * @param articleState the new state of the article (0 -> mark as read; 1 -> mark as unread).
     */
    public void setArticleRead(List<Integer> articlesIds, int articleState);
    
    /**
     * Marks a feed or a category with all its feeds as read.
     * 
     * @param id the feed-id/category-id.
     * @param isCategory indicates whether id refers to a feed or a category.
     */
    public void setRead(int id, boolean isCategory);
    
    /**
     * Returns true if there was an error.
     * 
     * @return true if there was an error.
     */
    public boolean hasLastError();
    
    /**
     * Returns the last error.
     * 
     * @return a string with the last error-message.
     */
    public String getLastError();
    
    /**
     * Returns the last error-message and resets the error-state of the connector.
     * 
     * @return a string with the last error-message.
     */
    public String pullLastError();
    
}
