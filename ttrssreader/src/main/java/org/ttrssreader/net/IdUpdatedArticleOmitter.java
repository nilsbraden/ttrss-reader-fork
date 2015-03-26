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

import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.model.pojos.Article;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * the instance of this class will be used for filtering out already cached articles, which was not updated while
 * parsing of JSON response from server
 *
 * @author igor
 */
public class IdUpdatedArticleOmitter implements IArticleOmitter {

	/**
	 * map of article IDs to it's updated date
	 */
	private Map<Integer, Long> idUpdatedMap;

	/**
	 * articles, which was skipped
	 */
	private Set<Integer> omittedArticles;

	/**
	 * construct the object according to selection parameters
	 *
	 * @param selection     A filter declaring which rows to return, formatted as an SQL WHERE clause (excluding the
	 *                      WHERE
	 *                      itself). Passing null will return all rows.
	 * @param selectionArgs You may include ?s in selection, which will be replaced by the values from selectionArgs,
	 *                      in
	 *                      order
	 *                      that they appear in the selection. The values will be bound as Strings.
	 */
	public IdUpdatedArticleOmitter(String selection, String[] selectionArgs) {
		idUpdatedMap = DBHelper.getInstance().getArticleIdUpdatedMap(selection, selectionArgs);
		omittedArticles = new HashSet<>();
	}

	/**
	 * article should be omitted if it's ID already exist in DB and updated date is not after date, which is stored in
	 * DB
	 *
	 * @param field current article field added to article on this iteration
	 * @param a     article to test
	 * @return {@code true} if given article should be omitted, {@code false} otherwise
	 * @throws StopJsonParsingException if parsing process make no sense (anymore) and should be broken
	 */
	public boolean omitArticle(Article.ArticleField field, Article a) throws StopJsonParsingException {
		boolean skip = false;

		switch (field) {
			case id:
			case updated:
				if (a.id > 0 && a.updated != null) {
					Long updated = idUpdatedMap.get(a.id);
					if (updated != null && a.updated.getTime() <= updated) {
						skip = true;
						omittedArticles.add(a.id);
					}
				}
			default:
				break;
		}

		return skip;
	}

	/**
	 * map of article IDs to it's updated date
	 *
	 * @return the idUpdatedMap
	 */
	public Map<Integer, Long> getIdUpdatedMap() {
		return idUpdatedMap;
	}

	/**
	 * articles, which was skipped
	 *
	 * @return the omittedArticles
	 */
	public Set<Integer> getOmittedArticles() {
		return omittedArticles;
	}
}
