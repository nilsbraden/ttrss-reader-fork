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

package org.ttrssreader.net;

import org.ttrssreader.model.pojos.Article;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * the instance of this class will be used for filtering out already cached articles, which was not updated while
 * parsing of JSON response from server
 *
 * @author igor
 */
public class IdUnreadArticleOmitter implements IArticleOmitter {

	/**
	 * map of article IDs to it's updated date
	 */
	private final Date lastUpdated;

	/**
	 * articles, that were skipped
	 */
	private final Set<Integer> omittedArticles = new HashSet<>();

	/**
	 * construct the object according to selection parameters
	 */
	public IdUnreadArticleOmitter(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	/**
	 * article should be omitted if it's ID already exist in DB and updated date is not after date, which is stored in
	 * DB
	 *
	 * @param field current article field added to article on this iteration
	 * @param a     article to test
	 * @return {@code true} if given article should be omitted, {@code false} otherwise
	 */
	public boolean omitArticle(Article.ArticleField field, Article a) {
		boolean ret = false;
		switch (field) {
			case unread:
				if (a.isUnread)
					ret = true;
				break;
			case updated:
				if (a.updated != null) {
					if (lastUpdated.compareTo(a.updated) >= 0) {
						omittedArticles.add(a.id);
						ret = true;
					}
				}
			default:
				break;
		}
		return ret;
	}

	@Override
	public Set<Integer> getOmittedArticles() {
		return omittedArticles;
	}

}
