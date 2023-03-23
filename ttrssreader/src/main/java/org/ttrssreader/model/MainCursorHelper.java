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
import android.database.sqlite.SQLiteDatabase;

import org.ttrssreader.controllers.Controller;

abstract class MainCursorHelper {

	//	private static final String TAG = MainCursorHelper.class.getSimpleName();

	protected int categoryId = Integer.MIN_VALUE;
	protected int feedId = Integer.MIN_VALUE;

	protected boolean selectArticlesForCategory;

	/**
	 * Creates a new query
	 */
	Cursor makeQuery(SQLiteDatabase db) {
		Cursor cursor = null;
		try {
			if (categoryId == 0 && (feedId == -1 || feedId == -2)) {

				// Starred/Published
				cursor = createCursor(db, true, false);

			} else {

				// normal query
				cursor = createCursor(db, false, false);

				// (categoryId == -2 || feedId >= 0): Normal feeds
				// (categoryId == 0 || feedId == Integer.MIN_VALUE): Uncategorized Feeds
				if ((categoryId == -2 || feedId >= 0) || (categoryId == 0 || feedId == Integer.MIN_VALUE)) {
					if (Controller.getInstance().onlyUnread() && !checkUnread(cursor)) {

						// Close old cursor safely
						if (cursor != null && !cursor.isClosed())
							cursor.close();

						// Override unread if query was empty
						cursor = createCursor(db, true, false);

					}
				}
			}

		} catch (Exception e) {
			// Close old cursor safely
			if (cursor != null && !cursor.isClosed())
				cursor.close();
			// Fail-safe-query
			cursor = createCursor(db, false, true);
		}
		return cursor;
	}

	/**
	 * Tries to find out if the given cursor points to a dataset with unread articles in it, returns true if it does.
	 *
	 * @param cursor the cursor.
	 * @return true if there are unread articles in the dataset, else false.
	 */
	private static boolean checkUnread(Cursor cursor) {
		if (cursor == null || cursor.isClosed())
			return false; // Check null or closed

		if (!cursor.moveToFirst())
			return false; // Check empty

		int index = cursor.getColumnIndex("unread");
		if (index >= 0) {
			do {
				if (cursor.getInt(index) > 0)
					return cursor.moveToFirst(); // One unread article found, move to first entry
			} while (cursor.moveToNext());

			cursor.moveToFirst();
		}
		return false;
	}

	abstract Cursor createCursor(SQLiteDatabase db, boolean overrideDisplayUnread, boolean buildSafeQuery);

	abstract Cursor createDummyCursor();

}
