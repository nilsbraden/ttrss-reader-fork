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

package org.ttrssreader.model.updaters;

import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.ArticleItem;
import org.ttrssreader.model.IUpdatable;
import org.ttrssreader.utils.Utils;
import android.util.Log;

public class PublishedStateUpdater implements IUpdatable {
    
    private ArticleItem mArticle;
    
    /**
     * Toggles the articles' Published-Status.
     */
    public PublishedStateUpdater(ArticleItem article) {
        mArticle = article;
    }
    
    @Override
    public void update() {
        Log.i(Utils.TAG, "Updating Article-Published-Status...");
        
        Data.getInstance().setArticlePublished(mArticle.getId(), mArticle.isPublished() ? 0 : 1);
        
        DBHelper.getInstance().updateArticlePublished(mArticle.getId(), !mArticle.isPublished());
        // Does it make any sense to toggle the state on the server? Set newState to 2 for toggle.
        
        mArticle.setPublished(!mArticle.isPublished());
    }
    
}
