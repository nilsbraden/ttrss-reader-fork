/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 Nils Braden
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
import org.ttrssreader.controllers.UpdateController;
import org.ttrssreader.model.pojos.Article;

public class StarredStateUpdater implements IUpdatable {

    @SuppressWarnings("unused")
    private static final String TAG = StarredStateUpdater.class.getSimpleName();
    
    private Article article;
    private int articleState;
    
    /**
     * Sets the articles' Starred-Status according to articleState
     */
    public StarredStateUpdater(Article article, int articleState) {
        this.article = article;
        this.articleState = articleState;
    }
    
    @Override
    public void update(Updater parent) {
        if (articleState >= 0) {
            article.isStarred = articleState > 0 ? true : false;
            DBHelper.getInstance().markArticle(article.id, "isStarred", articleState);
            UpdateController.getInstance().notifyListeners();
            Data.getInstance().setArticleStarred(article.id, articleState);
        }
    }
    
}
