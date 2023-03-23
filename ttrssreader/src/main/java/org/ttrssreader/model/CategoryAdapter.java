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
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.pojos.Category;

public class CategoryAdapter extends MainAdapter {

	//	private static final String TAG = CategoryAdapter.class.getSimpleName();

	public CategoryAdapter(Context context) {
		super(context);
	}

	@Override
	public Object getItem(int position) {
		Category ret = new Category();
		Cursor cur = getCursor();
		if (cur == null)
			return ret;

		if (cur.getCount() >= position) {
			if (cur.moveToPosition(position)) {
				return getCategory(cur);
			}
		}
		return ret;
	}

	private static int getImage(int id, boolean unread) {
		if (id == Data.VCAT_STAR) {
			return R.drawable.star48;
		} else if (id == Data.VCAT_PUB) {
			return R.drawable.published48;
		} else if (id == Data.VCAT_FRESH) {
			return R.drawable.fresh48;
		} else if (id == Data.VCAT_ALL) {
			return R.drawable.all48;
		} else if (id < -10) {
			if (unread) {
				return R.drawable.label;
			} else {
				return R.drawable.label_read;
			}
		} else {
			if (unread) {
				return R.drawable.categoryunread48;
			} else {
				return R.drawable.categoryread48;
			}
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return View.inflate(context, R.layout.item_category, null);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		super.bindView(view, context, cursor);

		ViewHolder holder = (ViewHolder) view.getTag();
		if (holder == null) {
			holder = new ViewHolder();
			holder.icon = (ImageView) view.findViewById(R.id.icon);
			holder.title = (TextView) view.findViewById(R.id.title);
			holder.unread = (TextView) view.findViewById(R.id.item_unread);
			view.setTag(holder);
		}

		final Category c = getCategory(cursor);

		holder.icon.setImageResource(getImage(c.id, c.unread > 0));
		holder.title.setText(c.title);
		holder.unread.setText(String.valueOf(c.unread));
		if (c.unread > 0) {
			holder.title.setTypeface(Typeface.DEFAULT_BOLD);
			holder.unread.setVisibility(View.VISIBLE);
		} else {
			holder.title.setTypeface(Typeface.DEFAULT);
			holder.unread.setVisibility(View.GONE);
		}
	}

	private Category getCategory(Cursor cur) {
		Category ret = new Category();
		ret.id = cur.getInt(0);
		ret.title = cur.getString(1);
		ret.unread = cur.getInt(2);
		return ret;
	}

	private static class ViewHolder {
		TextView title;
		TextView unread;
		ImageView icon;
	}

}
