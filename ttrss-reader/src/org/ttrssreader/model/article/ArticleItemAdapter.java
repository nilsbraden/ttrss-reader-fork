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

package org.ttrssreader.model.article;

import java.util.LinkedHashSet;
import java.util.Set;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.IRefreshable;
import org.ttrssreader.model.IUpdatable;
import org.ttrssreader.utils.Utils;
import android.util.Log;

public class ArticleItemAdapter implements IRefreshable, IUpdatable {
    
    private int mArticleId;
    private ArticleItem mArticle;
    private ArticleItem mArticleTemp;
    
    public ArticleItemAdapter(int articleId) {
        mArticleId = articleId;
    }
    
    public ArticleItem getArticle() {
        return mArticle;
    }
    
    @Override
    public Set<Object> refreshData() {
        if (mArticleTemp != null) {
            Log.d(Utils.TAG, "Using Article from Update...");
            mArticle = mArticleTemp;
            mArticleTemp = null;
        } else {
            Log.d(Utils.TAG, "Fetching Article from DB...");
            mArticle = Data.getInstance().getArticle(mArticleId);
        }
        
        return null;
    }
    
    @Override
    public void update() {

        if (!Controller.getInstance().isWorkOffline()) {
            mArticleTemp = Data.getInstance().getArticle(mArticleId);
            
            // BUGFIX: Fetch article-content if not done yet.
            if (mArticleTemp.getContent() == null) {
                Log.i(Utils.TAG, "getArticle() for content");
                Set<Integer> articleIds = new LinkedHashSet<Integer>();
                articleIds.add(mArticleId);
                Set<ArticleItem> temp = Controller.getInstance().getConnector().getArticle(articleIds);
                for (ArticleItem a : temp) {
                    if (a.getId() == mArticleId) {
                        mArticleTemp = a;
                        break;
                    }
                }
            }
        }
        
    }
    
}
