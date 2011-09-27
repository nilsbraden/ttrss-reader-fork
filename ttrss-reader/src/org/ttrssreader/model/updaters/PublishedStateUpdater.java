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
    public void update(Updater parent) {
        if (articleState >= 0) {
            article.isPublished = articleState > 0 ? true : false;

            Data.getInstance().setArticlePublished(article.id, articleState);
            parent.progress();
            DBHelper.getInstance().markArticle(article.id, "isPublished", articleState);
            
        } else {
            // Does it make any sense to toggle the state on the server? Set newState to 2 for toggle.
            int pub = article.isPublished ? 0 : 1;
            article.isPublished = !article.isPublished;
                    
            Data.getInstance().setArticlePublished(article.id, pub);
            parent.progress();
            DBHelper.getInstance().markArticle(article.id, "isPublished", pub);
            
        }
    }
    
}
