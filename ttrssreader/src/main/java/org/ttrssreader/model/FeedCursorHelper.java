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
import org.ttrssreader.utils.Utils;

class FeedCursorHelper extends MainCursorHelper {

	//	private static final String TAG = FeedCursorHelper.class.getSimpleName();
	public static final String[] FEED_COLUMNS = new String[]{"_id", "title", "unread", "icon"};

	FeedCursorHelper(int categoryId) {
		super();
		this.categoryId = categoryId;
	}

	@Override
	public Cursor createCursor(SQLiteDatabase db, boolean overrideDisplayUnread, boolean buildSafeQuery) {

		StringBuilder query = new StringBuilder();

		String lastOpenedFeedsList = Utils.separateItems(Controller.getInstance().lastOpenedFeeds, ",");

		boolean displayUnread = Controller.getInstance().onlyUnread();
		boolean invertSortFeedCats = Controller.getInstance().invertSortFeedscats();

		if (overrideDisplayUnread)
			displayUnread = false;

		if (lastOpenedFeedsList.length() > 0 && !buildSafeQuery) {
			query.append("SELECT _id,title,unread,icon FROM (");
		}

		query.append(" SELECT _id,title,unread,icon FROM ");
		query.append(DBHelper.TABLE_FEEDS);
		query.append(" WHERE categoryId=");
		query.append(categoryId);
		query.append(displayUnread ? " AND unread>0" : "");

		if (lastOpenedFeedsList.length() > 0 && !buildSafeQuery) {
			query.append(" UNION SELECT _id,title,unread,icon FROM ");
			query.append(DBHelper.TABLE_FEEDS);
			query.append(" WHERE _id IN (");
			query.append(lastOpenedFeedsList);
			query.append(" ))");
		}

		query.append(" ORDER BY UPPER(title) ");
		query.append(invertSortFeedCats ? "DESC" : "ASC");
		query.append(buildSafeQuery ? " LIMIT 400" : " LIMIT 1000");

		return db.rawQuery(query.toString(), null);
	}

	@Override
	Cursor createDummyCursor() {
		MatrixCursor cursor = new MatrixCursor(FEED_COLUMNS, 0);
		cursor.addRow(new Object[]{-1, "error! check logcat.", 0, new byte[]{}});
		return cursor;
	}

}
