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

import org.ttrssreader.R;

import android.content.Context;
import android.preference.PreferenceActivity.Header;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * <a href=
 * "http://stackoverflow.com/questions/15551673/android-headers-categories-in-preferenceactivity-with
 * -preferencefragment"
 * >stackoverflow.com</a>
 */
public class HeaderAdapter extends ArrayAdapter<Header> {

	@SuppressWarnings("unused")
	private static final String TAG = HeaderAdapter.class.getSimpleName();

	private static final int HEADER_TYPE_CATEGORY = 0;
	private static final int HEADER_TYPE_NORMAL = 1;
	private static final int HEADER_TYPE_COUNT = HEADER_TYPE_NORMAL + 1;

	private LayoutInflater mInflater;

	private static class HeaderViewHolder {
		ImageView icon;
		TextView title;
		TextView summary;
	}

	public HeaderAdapter(Context context, List<Header> objects) {

		super(context, 0, objects);

		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	private static int getHeaderType(Header header) {

		if (header.fragment == null && header.intent == null) return HEADER_TYPE_CATEGORY;
		else return HEADER_TYPE_NORMAL;
	}

	@Override
	public int getItemViewType(int position) {
		Header header = getItem(position);
		return getHeaderType(header);
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false; /* because of categories */
	}

	@Override
	public boolean isEnabled(int position) {
		return getItemViewType(position) != HEADER_TYPE_CATEGORY;
	}

	@Override
	public int getViewTypeCount() {
		return HEADER_TYPE_COUNT;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		HeaderViewHolder holder;
		Header header = getItem(position);
		int headerType = getHeaderType(header);
		View view = null;

		if (convertView == null) {

			holder = new HeaderViewHolder();

			switch (headerType) {

				case HEADER_TYPE_CATEGORY:

					view = new TextView(getContext(), null, android.R.attr.listSeparatorTextViewStyle);
					holder.title = (TextView) view;
					break;

				case HEADER_TYPE_NORMAL:

					view = mInflater.inflate(R.layout.item_preferenceheader, parent, false);
					holder.icon = (ImageView) view.findViewById(R.id.ph_icon);
					holder.title = (TextView) view.findViewById(R.id.ph_title);
					holder.summary = (TextView) view.findViewById(R.id.ph_summary);
					break;
			}

			if (view != null) view.setTag(holder);
		} else {

			view = convertView;
			holder = (HeaderViewHolder) view.getTag();
		}

		// All view fields must be updated every time, because the view may be recycled
		switch (headerType) {

			case HEADER_TYPE_CATEGORY:

				holder.title.setText(header.getTitle(getContext().getResources()));
				break;

			case HEADER_TYPE_NORMAL:

				holder.icon.setImageResource(header.iconRes);

				holder.title.setText(header.getTitle(getContext().getResources()));
				CharSequence summary = header.getSummary(getContext().getResources());

				if (!TextUtils.isEmpty(summary)) {

					holder.summary.setVisibility(View.VISIBLE);
					holder.summary.setText(summary);
				} else {
					holder.summary.setVisibility(View.GONE);
				}
				break;
		}

		return view;
	}
}
