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

public class FeedHeadlineUpdater implements IUpdatable {
    
    boolean selectArticlesForCategory = false;
    int categoryId;
    int feedId;
    boolean isCategory;
    
    public int unreadCount = 0;
    
    public FeedHeadlineUpdater(boolean selectArticlesForCategory, int categoryId) {
        this.selectArticlesForCategory = selectArticlesForCategory;
        this.categoryId = categoryId;
        this.isCategory = true;
    }
    
    public FeedHeadlineUpdater(int feedId) {
        this.feedId = feedId;
        this.isCategory = false;
    }
    
    @Override
    public void update() {
        boolean displayUnread = Controller.getInstance().displayOnlyUnread();
        
        if (selectArticlesForCategory) {
            
            unreadCount = DBHelper.getInstance().getUnreadCount(categoryId, true);
            Data.getInstance().updateArticles(categoryId, displayUnread, isCategory);
            
        } else {
            
            unreadCount = DBHelper.getInstance().getUnreadCount(feedId, false);
            Data.getInstance().updateArticles(feedId, displayUnread, isCategory);
            
        }
    }
    
}
