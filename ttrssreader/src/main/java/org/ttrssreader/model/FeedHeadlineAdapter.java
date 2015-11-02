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

import org.jetbrains.annotations.NotNull;
import org.ttrssreader.R;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.utils.DateUtils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Date;

public class FeedHeadlineAdapter extends MainAdapter {

	@SuppressWarnings("unused")
	private static final String TAG = FeedHeadlineAdapter.class.getSimpleName();

	private int feedId;
	private boolean selectArticlesForCategory;

	public FeedHeadlineAdapter(Context context, int feedId, boolean selectArticlesForCategory) {
		super(context);
		this.feedId = feedId;
		this.selectArticlesForCategory = selectArticlesForCategory;
	}

	@Override
	public Object getItem(int position) {
		Article ret = new Article();
		Cursor cur = getCursor();
		if (cur == null) return ret;

		if (cur.getCount() >= position) {
			if (cur.moveToPosition(position)) {
				return getArticle(cur);
			}
		}
		return ret;
	}

	@SuppressWarnings("deprecation")
	private static void setImage(ImageView icon, Article a) {
		if (a.isUnread) {
			icon.setBackgroundResource(R.drawable.articleunread48);
		} else {
			icon.setBackgroundResource(R.drawable.articleread48);
		}

		if (a.isStarred && a.isPublished) {
			icon.setImageResource(R.drawable.published_and_starred48);
		} else if (a.isStarred) {
			icon.setImageResource(R.drawable.star_yellow48);
		} else if (a.isPublished) {
			icon.setImageResource(R.drawable.published_blue48);
		} else {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
				icon.setBackgroundDrawable(null);
			} else {
				icon.setBackground(null);
			}
			if (a.isUnread) {
				icon.setImageResource(R.drawable.articleunread48);
			} else {
				icon.setImageResource(R.drawable.articleread48);
			}
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return View.inflate(context, R.layout.item_feedheadline, null);
	}

	@Override
	public void bindView(@NotNull View view, Context context, @NotNull Cursor cursor) {
		super.bindView(view, context, cursor);

		Article a = getArticle(cursor);
		ImageView icon = (ImageView) view.findViewById(R.id.icon);
		setImage(icon, a);

		TextView title = (TextView) view.findViewById(R.id.title);
		title.setText(a.title);
		if (a.isUnread) title.setTypeface(Typeface.DEFAULT_BOLD);
		else title.setTypeface(Typeface.DEFAULT);

		TextView updateDate = (TextView) view.findViewById(R.id.updateDate);
		String date = DateUtils.getDateTime(context, a.updated);
		updateDate.setText(date.length() > 0 ? "(" + date + ")" : "");

		TextView dataSource = (TextView) view.findViewById(R.id.dataSource);
		// Display Feed-Title in Virtual-Categories or when displaying all Articles in a Category
		if ((feedId < 0 && feedId >= -4) || (selectArticlesForCategory)) {
			dataSource.setText(a.feedTitle);
		}
	}

	private static Article getArticle(Cursor cur) {
		Article ret = new Article();
		ret.id = cur.getInt(0);
		ret.feedId = cur.getInt(1);
		ret.title = cur.getString(2);
		ret.isUnread = cur.getInt(3) != 0;
		ret.updated = new Date(cur.getLong(4));
		ret.isStarred = cur.getInt(5) != 0;
		ret.isPublished = cur.getInt(6) != 0;
		ret.feedTitle = cur.getString(7);
		return ret;
	}

}
