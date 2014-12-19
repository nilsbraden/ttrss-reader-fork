/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 Nils Braden
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.pojos.Article;

public class ArticleReadStateUpdater implements IUpdatable {
    
    @SuppressWarnings("unused")
    private static final String TAG = ArticleReadStateUpdater.class.getSimpleName();
    
    private int state;
    private Collection<Article> articles = null;
    
    /* articleState: 0 = mark as read, 1 = mark as unread */
    public ArticleReadStateUpdater(Article article, int pid, int articleState) {
        articles = new ArrayList<Article>();
        articles.add(article);
        state = articleState;
        article.isUnread = (articleState > 0);
    }
    
    /* articleState: 0 = mark as read, 1 = mark as unread */
    public ArticleReadStateUpdater(Collection<Article> articlesList, int pid, int articleState) {
        articles = new ArrayList<Article>();
        articles.addAll(articlesList);
        state = articleState;
        for (Article article : articles) {
            article.isUnread = (articleState > 0);
        }
    }
    
    @Override
    public void update(Updater parent) {
        if (articles != null) {
            Set<Integer> ids = new HashSet<Integer>();
            for (Article article : articles) {
                ids.add(article.id);
                article.isUnread = (state > 0);
            }
            
            if (!ids.isEmpty()) {
                DBHelper.getInstance().markArticles(ids, "isUnread", state);
                Data.getInstance().calculateCounters();
                Data.getInstance().notifyListeners();
                Data.getInstance().setArticleRead(ids, state);
            }
        }
    }
    
}
