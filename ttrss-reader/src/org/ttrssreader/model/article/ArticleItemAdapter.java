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

public class ArticleItemAdapter implements IRefreshable, IUpdatable {
    
    private int mArticleId;
    private ArticleItem mArticle;
    
    public ArticleItemAdapter(int articleId) {
        mArticleId = articleId;
    }
    
    public ArticleItem getArticle() {
        return mArticle;
    }
    
    @Override
    public Set<Object> refreshData() {
        mArticle = Data.getInstance().getArticle(mArticleId);
        
        // BUGFIX: Fetch article-content if not done yet.
        if (mArticle.getContent() == null) {
            Set<Integer> articleIds = new LinkedHashSet<Integer>();
            articleIds.add(mArticleId);
            Set<ArticleItem> temp = Controller.getInstance().getConnector().getArticle(articleIds);
            for (ArticleItem a : temp) {
                if (a.getId() == mArticleId) {
                    mArticle = a;
                    break;
                }
            }
        }
        
        return null;
    }
    
    @Override
    public void update() {
    }
    
}
