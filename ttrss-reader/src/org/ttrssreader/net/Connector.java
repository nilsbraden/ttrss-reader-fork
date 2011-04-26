/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
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

import java.util.Set;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.pojos.Feed;

public interface Connector {
    
    /**
     * Retrieves a Set of Maps which map Strings to the information, e.g. "id" -> 42, containing the counters for every
     * category and feed.
     * 
     * @return set of Name-Value-Pairs stored in maps
     */
    public abstract void getCounters();
    
    /**
     * Retrieves all categories.
     * 
     * @return a list of categories.
     */
    public abstract Set<Category> getCategories();
    
    /**
     * Retrieves all feeds, mapped to their categories.
     * 
     * @return a map of all feeds for every category.
     */
    public abstract Set<Feed> getFeeds();
    
    /**
     * Retrieves the specified articles and inserts them into the Database
     * 
     * @param articleIds
     *            the ids of the articles.
     */
    public abstract void getArticle(Set<Integer> ids);
    
    /**
     * Retrieves the specified articles and directly stores them in the database.
     * 
     * @param articleIds
     *            the ids of the articles.
     */
    public abstract Set<Integer> getHeadlinesToDatabase(Integer feedId, int limit, int filter, String viewMode);
    
    /**
     * Marks the given list of article-Ids as read/unread depending on int articleState.
     * 
     * @param articlesIds
     *            the list of ids.
     * @param articleState
     *            the new state of the article (0 -> mark as read; 1 -> mark as unread).
     */
    public abstract boolean setArticleRead(Set<Integer> ids, int articleState);
    
    /**
     * Marks the given Article as "starred"/"not starred" depending on int articleState.
     * 
     * @param articlesId
     *            the article.
     * @param articleState
     *            the new state of the article (0 -> not starred; 1 -> starred; 2 -> toggle).
     */
    public abstract boolean setArticleStarred(Set<Integer> ids, int articleState);
    
    /**
     * Marks the given Article as "published"/"not published" depending on int articleState.
     * 
     * @param articlesId
     *            the article.
     * @param articleState
     *            the new state of the article (0 -> not published; 1 -> published; 2 -> toggle).
     */
    public abstract boolean setArticlePublished(Set<Integer> ids, int articleState);
    
    /**
     * Marks a feed or a category with all its feeds as read.
     * 
     * @param id
     *            the feed-id/category-id.
     * @param isCategory
     *            indicates whether id refers to a feed or a category.
     */
    public abstract boolean setRead(int id, boolean isCategory);
    
    /**
     * Returns the value for the given preference-name as a string.
     * 
     * @param pref
     *            the preferences name
     * @return the value of the preference or null if it ist not set or unknown
     */
    public abstract String getPref(String pref);
    
    /**
     * Returns the version of the server-installation as integer (version without dots)
     * 
     * @return the version
     */
    public abstract int getVersion();
    
}
