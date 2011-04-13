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

package org.ttrssreader.model.updaters;

import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.pojos.Feed;

public class FeedHeadlineUpdater implements IUpdatable {
    
    boolean selectArticlesForCategory;
    int categoryId;
    int feedId;
    
    public int unreadCount = 0;
    
    public FeedHeadlineUpdater(boolean selectArticlesForCategory, int categoryId) {
        this.selectArticlesForCategory = selectArticlesForCategory;
        this.categoryId = categoryId;
    }
    
    public FeedHeadlineUpdater(int feedId) {
        this.feedId = feedId;
    }
    
    @Override
    public void update() {
        if (selectArticlesForCategory) {
            unreadCount = DBHelper.getInstance().getUnreadCount(categoryId, true);
            for (Feed f : DBHelper.getInstance().getFeeds(categoryId)) {
                Data.getInstance().updateArticles(f.id, Controller.getInstance().displayOnlyUnread());
            }
        } else {
            unreadCount = DBHelper.getInstance().getUnreadCount(feedId, false);
            Data.getInstance().updateArticles(feedId, Controller.getInstance().displayOnlyUnread());
        }
    }
    
}
