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
import org.ttrssreader.utils.Utils;
import android.util.Log;

public class StarredStateUpdater implements IUpdatable {
    
    private ArticleItem article;
    private int articleState;
    
    /**
     * Toggles the articles' Starred-Status.
     */
    public StarredStateUpdater(ArticleItem article) {
        this.article = article;
        this.articleState = -1;
    }
    
    /**
     * Sets the articles' Starred-Status according to articleState
     */
    public StarredStateUpdater(ArticleItem article, int articleState) {
        this.article = article;
        this.articleState = articleState;
    }
    
    @Override
    public void update() {
        Log.i(Utils.TAG, "Updating Article-Starred-Status...");
        
        if (articleState >= 0) {
            // mArticle.isStarred() ? 0 : 1
            Data.getInstance().setArticleStarred(article.getId(), articleState);
            DBHelper.getInstance().updateArticleStarred(article.getId(), articleState > 0 ? true : false);
            article.setStarred(articleState > 0 ? true : false);
        } else {
            // Does it make any sense to toggle the state on the server? Set newState to 2 for toggle.
            Data.getInstance().setArticleStarred(article.getId(), article.isStarred() ? 0 : 1);
            DBHelper.getInstance().updateArticleStarred(article.getId(), !article.isStarred());
            article.setStarred(!article.isStarred());
        }
    }
    
}
