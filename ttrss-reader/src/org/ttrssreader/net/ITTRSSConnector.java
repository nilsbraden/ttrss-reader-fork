/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
 * Copyright (C) 2009-2010 J. Devauchelle.
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

import java.util.Map;
import java.util.Set;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.model.category.CategoryItem;
import org.ttrssreader.model.feed.FeedItem;

public interface ITTRSSConnector {
    
    /**
     * Retrieves the number of unread articles.
     * 
     * @return the number of unread articles.
     */
    // public int getTotalUnread();
    
    /**
     * Retrieves a Set of Maps which map Strings to the information, e.g. "id" -> 42, containing the counters for every
     * category and feed.
     * 
     * @return set of Name-Value-Pairs stored in maps
     */
    public Set<Map<String, Object>> getCounters();
    
    /**
     * Retrieves all categories.
     * 
     * @return a list of categories.
     */
    public Set<CategoryItem> getCategories();
    
    /**
     * Retrieves all feeds, mapped to their categories.
     * 
     * @return a map of all feeds for every category.
     */
    public Map<Integer, Set<FeedItem>> getFeeds();
    
    /**
     * Retrieves the articles for the given feed or category, depending on boolean isCategory. Already read articles can
     * be filtered.
     * 
     * @param id
     *            the id of the feed or category.
     * @param displayOnlyUnread
     *            whether we are to filter out read articles.
     * @param isCategory
     *            indicates if the id refers to a category or a feed.
     * @param limit
     *            sets a limit of articles that are read.
     * @return a list of articles or null on error.
     */
    // public Set<ArticleItem> getArticles(int parentId, boolean displayOnlyUnread, boolean isCategory, int limit);
    
    /**
     * Retrieves the specified articles.
     * 
     * @param articleIds
     *            the ids of the articles.
     * @return the articles.
     */
    public Set<ArticleItem> getArticle(Set<Integer> articleIds);
    
    /**
     * Retrieves all articles for a given feed with their headlines to avoid too much traffic for the content.
     * 
     * @param feedId
     * @param limit
     * @param filter
     * @param viewMode
     * @return
     */
    public Set<ArticleItem> getFeedHeadlines(int feedId, int limit, int filter, String viewMode);
    
    /**
     * Retrieves all new articles since the given time. ArticleState can be given to only retrieve unread articles.
     * 
     * @param articleState
     *            int indicating whetether we are to fetch unread articles only (1) or all articles (0).
     * @param time
     *            the time in ms since 1970, specifying the time of the last update.
     * @return a map of List<ArticleItem> mapped to their feeds, mapped to their articles.
     */
    public Map<CategoryItem, Map<FeedItem, Set<ArticleItem>>> getNewArticles(int articleState, long time /* ago */);
    
    /**
     * Marks the given list of article-Ids as read/unread depending on int articleState.
     * 
     * @param articlesIds
     *            the list of ids.
     * @param articleState
     *            the new state of the article (0 -> mark as read; 1 -> mark as unread).
     */
    public void setArticleRead(Set<Integer> articlesIds, int articleState);
    
    /**
     * Marks the given Article as "starred"/"not starred" depending on int articleState.
     * 
     * @param articlesId
     *            the article.
     * @param articleState
     *            the new state of the article (0 -> not starred; 1 -> starred; 2 -> toggle).
     */
    public void setArticleStarred(int articlesId, int articleState);
    
    /**
     * Marks the given Article as "published"/"not published" depending on int articleState.
     * 
     * @param articlesId
     *            the article.
     * @param articleState
     *            the new state of the article (0 -> not published; 1 -> published; 2 -> toggle).
     */
    public void setArticlePublished(int articlesId, int articleState);
    
    /**
     * Marks a feed or a category with all its feeds as read.
     * 
     * @param id
     *            the feed-id/category-id.
     * @param isCategory
     *            indicates whether id refers to a feed or a category.
     */
    public void setRead(int id, boolean isCategory);
    
    /**
     * Returns the value for the given preference-name as a string.
     * 
     * @param pref
     *            the preferences name
     * @return the value of the preference or null if it ist not set or unknown
     */
    public String getPref(String pref);
    
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
