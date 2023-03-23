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
import org.ttrssreader.model.pojos.Feed;

public class FeedAdapter extends MainAdapter {

	//	private static final String TAG = FeedAdapter.class.getSimpleName();

	public FeedAdapter(Context context) {
		super(context);
	}

	@Override
	public Object getItem(int position) {
		Feed ret = new Feed();
		Cursor cur = getCursor();
		if (cur == null)
			return ret;

		if (cur.getCount() >= position) {
			if (cur.moveToPosition(position)) {
				return getFeed(cur);
			}
		}
		return ret;
	}

	private int getImage(boolean unread) {
		if (unread) {
			return R.drawable.feedheadlinesunread48;
		} else {
			return R.drawable.feedheadlinesread48;
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return View.inflate(context, R.layout.item_feed, null);
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

		final Feed f = getFeed(cursor);

		holder.icon.setImageResource(getImage(f.unread > 0));
		holder.title.setText(f.title);
		holder.unread.setText(String.valueOf(f.unread));
		if (f.unread > 0) {
			holder.title.setTypeface(Typeface.DEFAULT_BOLD);
			holder.unread.setVisibility(View.VISIBLE);
		} else {
			holder.title.setTypeface(Typeface.DEFAULT);
			holder.unread.setVisibility(View.GONE);
		}
	}

	private static Feed getFeed(Cursor cur) {
		Feed ret = new Feed();
		ret.id = cur.getInt(0);
		ret.title = cur.getString(1);
		ret.unread = cur.getInt(2);
		if (!cur.isNull(3)) {
			ret.icon = cur.getBlob(3);
		}
		return ret;
	}

	private static class ViewHolder {
		TextView title;
		TextView unread;
		ImageView icon;
	}

}
