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

package org.ttrssreader.model;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;

import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.utils.Utils;

import java.util.Date;

class FeedHeadlineCursorHelper extends MainCursorHelper {

	//	private static final String TAG = FeedHeadlineCursorHelper.class.getSimpleName();
	public static final String[] FEEDHEADLINE_COLUMNS = new String[]{"_id", "feedId", "title", "unread", "updateDate", "isStarred", "isPublished", "note", "feedTitle"};

	FeedHeadlineCursorHelper(int feedId, int categoryId, boolean selectArticlesForCategory) {
		this.feedId = feedId;
		this.categoryId = categoryId;
		this.selectArticlesForCategory = selectArticlesForCategory;
	}

	@Override
	public Cursor createCursor(SQLiteDatabase db, boolean overrideDisplayUnread, boolean buildSafeQuery) {

		String query;
		if (feedId > -10)
			query = buildFeedQuery(overrideDisplayUnread, buildSafeQuery);
		else
			query = buildLabelQuery(overrideDisplayUnread, buildSafeQuery);

		return db.rawQuery(query, null);
	}

	private String buildFeedQuery(boolean overrideDisplayUnread, boolean buildSafeQuery) {
		String lastOpenedArticlesList = Utils.separateItems(Controller.getInstance().lastOpenedArticles, ",");

		boolean displayUnread = Controller.getInstance().onlyUnread();
		boolean displayCachedImages = Controller.getInstance().onlyDisplayCachedImages();
		boolean invertSortArticles = Controller.getInstance().invertSortArticlelist();

		if (overrideDisplayUnread)
			displayUnread = false;

		StringBuilder query = new StringBuilder();
		query.append(" SELECT ");
		query.append(" a._id AS _id, a.feedId, a.title, a.isUnread AS unread, a.updateDate, ");
		query.append(" a.isStarred, a.isPublished, a.note, f.title AS feedTitle ");
		query.append(" FROM ");
		query.append(DBHelper.TABLE_ARTICLES).append(" a, ");
		query.append(DBHelper.TABLE_FEEDS).append(" f ");
		query.append("WHERE a.feedId=f._id ");

		switch (feedId) {
			case Data.VCAT_STAR:
				query.append(" AND a.isStarred=1");
				break;

			case Data.VCAT_PUB:
				query.append(" AND a.isPublished=1");
				break;

			case Data.VCAT_FRESH:
				long max = System.currentTimeMillis() - Controller.getInstance().getFreshArticleMaxAge();
				query.append(" AND a.updateDate>").append(max);
				query.append(" AND a.isUnread>0");
				query.append(" AND (a.score is null or a.score>=0)");
				break;

			case Data.VCAT_ALL:
				query.append(displayUnread ? " AND a.isUnread>0 " : "");
				break;

			default:
				// User selected to display all articles of a category directly
				query.append(displayUnread ? " AND a.isUnread>0 " : " ");
				if (selectArticlesForCategory) {
					query.append(" AND f.categoryId=").append(categoryId);
				} else {
					query.append(" AND a.feedId=").append(feedId);
				}
				if (displayCachedImages) {
					query.append(" AND 0 < (SELECT SUM(r.cached) FROM ");
					query.append(DBHelper.TABLE_REMOTEFILE2ARTICLE).append(" r2a, ");
					query.append(DBHelper.TABLE_REMOTEFILES).append(" r ");
					query.append("WHERE a._id=r2a.articleId and r2a.remotefileId=r.id) ");
				}
		}

		if (lastOpenedArticlesList.length() > 0 && !buildSafeQuery) {
			query.append(" UNION SELECT ");
			query.append(" c._id AS _id, c.feedId, c.title, c.isUnread AS unread, c.updateDate, ");
			query.append(" c.isStarred, c.isPublished, c.note, d.title AS feedTitle ");
			query.append(" FROM ");
			query.append(DBHelper.TABLE_ARTICLES).append(" c, ");
			query.append(DBHelper.TABLE_FEEDS).append(" d ");
			query.append("WHERE c.feedId=d._id AND c._id IN (").append(lastOpenedArticlesList).append(" ) ");
		}

		query.append(" ORDER BY a.updateDate ");
		query.append(invertSortArticles ? "ASC" : "DESC");
		query.append(" LIMIT 1000 ");
		return query.toString();
	}

	private String buildLabelQuery(boolean overrideDisplayUnread, boolean buildSafeQuery) {
		String lastOpenedArticlesList = Utils.separateItems(Controller.getInstance().lastOpenedArticles, ",");

		boolean displayUnread = Controller.getInstance().onlyUnread();
		boolean invertSortArticles = Controller.getInstance().invertSortArticlelist();

		if (overrideDisplayUnread)
			displayUnread = false;

		StringBuilder query = new StringBuilder();
		query.append("SELECT ");
		query.append(" a._id AS _id, a.feedId, a.title, a.isUnread AS unread, a.updateDate, ");
		query.append(" a.isStarred, a.isPublished, a.note, f.title AS feedTitle ");
		query.append(" FROM ");
		query.append(DBHelper.TABLE_FEEDS).append(" f, ");
		query.append(DBHelper.TABLE_ARTICLES).append(" a, ");
		query.append(DBHelper.TABLE_ARTICLES2LABELS).append(" a2l, ");
		query.append(DBHelper.TABLE_FEEDS).append(" l ");
		query.append("WHERE f._id=a.feedId AND a._id=a2l.articleId AND a2l.labelId=l._id");
		query.append(" AND a2l.labelId=").append(feedId);
		query.append(displayUnread ? " AND isUnread>0" : "");

		if (lastOpenedArticlesList.length() > 0 && !buildSafeQuery) {
			query.append(" UNION SELECT ");
			query.append(" b._id AS _id, b.feedId, b.title, b.isUnread AS unread, b.updateDate, ");
			query.append(" b.isStarred, b.isPublished, b.note, f.title AS feedTitle ");
			query.append(" FROM ");
			query.append(DBHelper.TABLE_FEEDS).append(" f, ");
			query.append(DBHelper.TABLE_ARTICLES).append(" b, ");
			query.append(DBHelper.TABLE_ARTICLES2LABELS).append(" b2m, ");
			query.append(DBHelper.TABLE_FEEDS).append(" m ");
			query.append("WHERE f._id=a.feedId AND b2m.labelId=m._id AND b2m.articleId=b._id");
			query.append(" AND b._id IN (").append(lastOpenedArticlesList).append(" )");
		}

		query.append(" ORDER BY updateDate ");
		query.append(invertSortArticles ? "ASC" : "DESC");
		query.append(" LIMIT 1000 ");
		return query.toString();
	}

	@Override
	Cursor createDummyCursor() {
		MatrixCursor cursor = new MatrixCursor(FEEDHEADLINE_COLUMNS, 0);
		cursor.addRow(new Object[]{-1, -1, "error! check logcat.", 0, new Date().getTime(), 0, 0, "dummy note", "dummy title"});
		return cursor;
	}

}
