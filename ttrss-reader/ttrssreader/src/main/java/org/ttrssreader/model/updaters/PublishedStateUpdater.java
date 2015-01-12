/*
 * Copyright (c) 2015, Nils Braden
 *
 * This file is part of ttrss-reader-fork. This program is free software; you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation;
 * either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program; If
 * not, see http://www.gnu.org/licenses/.
 */

package org.ttrssreader.model.updaters;

import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.pojos.Article;

public class PublishedStateUpdater implements IUpdatable {

    @SuppressWarnings("unused")
    private static final String TAG = PublishedStateUpdater.class.getSimpleName();

    private Article article;
    private int articleState;
    private String note;

    /**
     * Sets the articles' Published-Status according to articleState
     */
    public PublishedStateUpdater(Article article, int articleState) {
        this.article = article;
        this.articleState = articleState;
        this.note = null;
    }

    /**
     * Sets the articles' Published-Status according to articleState and adds the given note to the article.
     */
    public PublishedStateUpdater(Article article, int articleState, String note) {
        this.article = article;
        this.articleState = articleState;
        this.note = note;
    }

    @Override
    public void update(Updater parent) {
        if (articleState >= 0) {
            article.isPublished = articleState > 0 ? true : false;
            DBHelper.getInstance().markArticle(article.id, "isPublished", articleState);
            Data.getInstance().calculateCounters();
            Data.getInstance().notifyListeners();
            Data.getInstance().setArticlePublished(article.id, articleState, note);
        }
    }

}
