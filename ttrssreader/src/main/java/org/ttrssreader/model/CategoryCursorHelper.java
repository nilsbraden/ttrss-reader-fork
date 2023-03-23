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
import org.ttrssreader.model.pojos.Category;

import java.util.Collections;
import java.util.List;

class CategoryCursorHelper extends MainCursorHelper {

	//	private static final String TAG = CategoryCursorHelper.class.getSimpleName();

	@Override
	public Cursor createCursor(SQLiteDatabase db, boolean overrideDisplayUnread, boolean buildSafeQuery) {

		boolean includeRead = !Controller.getInstance().onlyUnread() && !overrideDisplayUnread;
		boolean inverted = Controller.getInstance().invertSortFeedscats();

		List<Category> list = DBHelper.getInstance().getCategories(Controller.getInstance().showVirtual(), includeRead);
		list.addAll(DBHelper.getInstance().getLabelsAsCategories(includeRead));

		MatrixCursor cursor = new MatrixCursor(DBHelper.CATEGORIES_COLUMNS, list.size());

		Collections.sort(list, new Category.CategoryComparator(inverted));

		for (Category category : list) {
			cursor.addRow(new Object[]{category.id, category.title, category.unread});
		}

		return cursor;

	}

	@Override
	Cursor createDummyCursor() {
		MatrixCursor cursor = new MatrixCursor(DBHelper.CATEGORIES_COLUMNS, 0);
		cursor.addRow(new Object[]{0, "error! check logcat.", 0});
		return cursor;
	}

}
