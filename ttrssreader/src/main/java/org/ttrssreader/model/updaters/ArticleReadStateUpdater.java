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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ArticleReadStateUpdater implements IUpdatable {

	//	private static final String TAG = ArticleReadStateUpdater.class.getSimpleName();

	private final int state;
	private final Collection<Article> articles;

	/* articleState: 0 = mark as read, 1 = mark as unread */
	public ArticleReadStateUpdater(Article article, int articleState) {
		articles = new ArrayList<>();
		articles.add(article);
		state = articleState;
		article.isUnread = (articleState > 0);
	}

	/* articleState: 0 = mark as read, 1 = mark as unread */
	public ArticleReadStateUpdater(Collection<Article> articlesList, int articleState) {
		articles = new ArrayList<>();
		articles.addAll(articlesList);
		state = articleState;
		for (Article article : articles) {
			article.isUnread = (articleState > 0);
		}
	}

	@Override
	public void update() {
		if (articles != null) {
			Set<Integer> ids = new HashSet<>();
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
