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

import java.util.Set;

/**
 * this interface is supposed to be used inside parseArticleArray of JSONConnector. The {@code omitArticle} method will
 * be called for each article field to determine if the article can be already omitted.
 *
 * @author igor
 */
public interface IArticleOmitter {
	/**
	 * this method should return {@code true} if given article should not be processed
	 *
	 * @param field current article field added to article on this iteration
	 * @param a     article to test
	 * @return {@code true} if given article should be omitted, {@code false} otherwise
	 */
	boolean omitArticle(Article.ArticleField field, Article a);

	/**
	 * Returns a list of articles that have been ignored in the last run.
	 *
	 * @return a list of article ids.
	 */
	Set<Integer> getOmittedArticles();
}
