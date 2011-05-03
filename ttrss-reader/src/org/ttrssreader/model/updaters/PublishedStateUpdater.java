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
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.utils.Utils;
import android.util.Log;

public class PublishedStateUpdater implements IUpdatable {
    
    private Article article;
    private int articleState;
    
    /**
     * Sets the articles' Published-Status according to articleState
     */
    public PublishedStateUpdater(Article article, int articleState) {
        this.article = article;
        this.articleState = articleState;
    }
    
    @Override
    public void update() {
        Log.i(Utils.TAG, "Updating Article-Published-Status...");
        
        if (articleState >= 0) {
            // article.isPublished() ? 0 : 1
            Data.getInstance().setArticlePublished(article.id, articleState);
            DBHelper.getInstance().markUnsynchronizedState(article.id, "isPublished", articleState);
            
            article.isPublished = articleState > 0 ? true : false;
        } else {
            // Does it make any sense to toggle the state on the server? Set newState to 2 for toggle.
            Data.getInstance().setArticlePublished(article.id, article.isPublished ? 0 : 1);
            DBHelper.getInstance().markUnsynchronizedState(article.id, "isPublished", article.isPublished ? 0 : 1);
            
            article.isPublished = !article.isPublished;
        }
    }
    
}
