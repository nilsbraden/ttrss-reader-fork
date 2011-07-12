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

public class CategoryUpdater implements IUpdatable {

    public int unreadCount = 0;
    
    @Override
    public void update() {
        
        Data.getInstance().updateCounters(false);
        Data.getInstance().updateCategories(false);
        Data.getInstance().updateVirtualCategories();
        Data.getInstance().updateFeeds(-4, true);
        
        // Refresh articles for all categories on startup
        for (Category c : DBHelper.getInstance().getCategoriesIncludingUncategorized()) {
            if (c.unread == 0)
                continue;
            
            boolean onlyUnreadArticles = Controller.getInstance().onlyUnread();
            Data.getInstance().updateArticles(c.id, onlyUnreadArticles, true); // Don't force update, offline *means* offline!
        }
        
        unreadCount = DBHelper.getInstance().getUnreadCount(-4, true);
    }
    
}
