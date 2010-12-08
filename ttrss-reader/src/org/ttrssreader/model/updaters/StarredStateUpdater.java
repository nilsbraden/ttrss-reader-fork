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

import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.model.IUpdatable;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.utils.Utils;
import android.util.Log;

public class StarredStateUpdater implements IUpdatable {
    
    private ArticleItem mArticle;
    
    /**
     * Toggles the articles' Starred-Status.
     */
    public StarredStateUpdater(ArticleItem article) {
        mArticle = article;
    }
    
    @Override
    public void update() {
        Log.i(Utils.TAG, "Updating Article-Starred-Status...");
        
        // Does it make any sense to toggle the state on the server? Set newState to 2 for toggle.
        Controller.getInstance().getConnector().setArticleStarred(mArticle.getId(), mArticle.isStarred() ? 0 : 1);
        
        DBHelper.getInstance().updateArticleStarred(mArticle.getId(), !mArticle.isStarred());
        
        mArticle.setStarred(!mArticle.isStarred());
    }
    
}
