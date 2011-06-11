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
import org.ttrssreader.model.pojos.Category;

public class FeedUpdater implements IUpdatable {
    
    private int categoryId;
    public int unreadCount = 0;
    
    public FeedUpdater(int categoryId) {
        this.categoryId = categoryId;
    }
    
    @Override
    public void update() {
        
        Data.getInstance().updateFeeds(categoryId, false);
        unreadCount = DBHelper.getInstance().getUnreadCount(categoryId, true);
        
        Category c = DBHelper.getInstance().getCategory(categoryId);
        if (c.unread == 0)
            return;
        
        // Update articles for current category
        boolean onlyUnreadArticles = Controller.getInstance().displayOnlyUnread();
        Data.getInstance().updateArticles(c.id, onlyUnreadArticles, true, true);
        
    }
    
}
