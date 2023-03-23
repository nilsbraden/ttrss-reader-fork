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

import android.content.Context;
import android.database.Cursor;
import android.widget.SimpleCursorAdapter;

import org.ttrssreader.R;

import java.util.ArrayList;
import java.util.List;

public abstract class MainAdapter extends SimpleCursorAdapter {

	//	private static final String TAG = MainAdapter.class.getSimpleName();

	private static final int layout = R.layout.main;

	MainAdapter(Context context) {
		super(context, layout, null, new String[]{}, new int[]{}, 0);
	}

	@Override
	public final long getItemId(int position) {
		return position;
	}

	public final int getId(int position) {
		int ret = Integer.MIN_VALUE;
		Cursor cur = getCursor();
		if (cur == null)
			return ret;

		if (cur.getCount() >= position)
			if (cur.moveToPosition(position))
				ret = cur.getInt(0);
		return ret;
	}

	public final List<Integer> getIds() {
		List<Integer> ret = new ArrayList<>();
		Cursor cur = getCursor();
		if (cur == null)
			return ret;

		if (cur.moveToFirst()) {
			while (!cur.isAfterLast()) {
				ret.add(cur.getInt(0));
				cur.move(1);
			}
		}
		return ret;
	}

	protected static CharSequence formatItemTitle(String title, int unread) {
		if (unread > 0) {
			return title + " (" + unread + ")";
		} else {
			return title;
		}
	}

}
