/*
 * Tiny Tiny RSS Reader for Android
 * 
 * Copyright (C) 2009 J. Devauchelle and contributors.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

package org.ttrssreader.model.feed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DataController;
import org.ttrssreader.model.IRefreshable;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FeedListAdapter extends BaseAdapter implements IRefreshable {
	
	private Context mContext;
	private String mCategoryId;
	
	private List<FeedItem> mFeeds;
	
	public FeedListAdapter(Context context, String categoryId) {
		mContext = context;
		mCategoryId = categoryId;
		mFeeds = new ArrayList<FeedItem>();
	}
	
	@Override
	public int getCount() {
		return mFeeds.size();
	}
	
	@Override
	public Object getItem(int position) {
		return mFeeds.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public String getFeedId(int position) {
		return mFeeds.get(position).getId();
	}
	
	public String getFeedTitle(int position) {
		return mFeeds.get(position).getTitle();
	}
	
	public int getUnread(int position) {
		return mFeeds.get(position).getUnread();
	}
	
	public List<FeedItem> getFeeds() {
		return mFeeds;
	}
	
	public ArrayList<String> getFeedIds() {
		ArrayList<String> ret = new ArrayList<String>();
		
		for (FeedItem f : mFeeds) {
			ret.add(f.getId());
		}
		return ret;
	}
	
	public ArrayList<String> getFeedNames() {
		ArrayList<String> ret = new ArrayList<String>();
		
		for (FeedItem f : mFeeds) {
			ret.add(f.getTitle());
		}
		return ret;
	}
	
	public int getTotalUnreadCount() {
		int result = 0;
		
		Iterator<FeedItem> iter = mFeeds.iterator();
		while (iter.hasNext()) {
			result += iter.next().getUnread();
		}
		
		return result;
	}
	
	private String formatTitle(String title, int unread) {
		if (unread > 0) {
			return title + " (" + unread + ")";
		} else {
			return title;
		}
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (position >= mFeeds.size()) {
			return new View(mContext);
		}
		
		FeedListView sv = null;
		if (convertView == null) {
			sv = new FeedListView(mContext, mFeeds.get(position).getTitle(),
					mFeeds.get(position).getId(),
					mFeeds.get(position).getUnread());
		} else {
			if (convertView instanceof FeedListView) {
				sv = (FeedListView) convertView;
				sv.setIcon(mFeeds.get(position).getUnread() > 0);
				sv.setBoldTitleIfNecessary(mFeeds.get(position).getUnread() > 0);
				sv.setTitle(formatTitle(mFeeds.get(position).getTitle(), mFeeds.get(position).getUnread()));
			}
		}
		
		return sv;
	}
	
	private class FeedListView extends LinearLayout {
		
		public FeedListView(Context context, String title, String id, int unread) {
			super(context);
			
			this.setOrientation(HORIZONTAL);
			
			// Here we build the child views in code. They could also have
			// been specified in an XML file.
			
			mIcon = new ImageView(context);
			setIcon(unread > 0);
			addView(mIcon, new LinearLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));
			
			mTitle = new TextView(context);
			mTitle.setGravity(Gravity.CENTER_VERTICAL);
			setBoldTitleIfNecessary(unread > 0);
			mTitle.setText(formatTitle(title, unread));
			
			LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
			layoutParams.setMargins(10, 0, 0, 0);
			
			addView(mTitle, layoutParams);
		}
		
		/**
		 * Convenience method to set the title of a SpeechView
		 */
		public void setTitle(String title) {
			mTitle.setText(title);
		}
		
		public void setBoldTitleIfNecessary(boolean isUnread) {
			if (isUnread) {
				mTitle.setTypeface(Typeface.DEFAULT_BOLD, 1);
			} else {
				mTitle.setTypeface(Typeface.DEFAULT, 0);
			}
		}
		
		public void setIcon(boolean hasUnread) {
			if (hasUnread) {
				mIcon.setImageResource(R.drawable.feedheadlinesunread48);
			} else {
				mIcon.setImageResource(R.drawable.feedheadlinesread48);
			}
		}
		
		private ImageView mIcon;
		private TextView mTitle;
	}
	
	@Override
	public void refreshData() {
		boolean displayOnlyUnread = Controller.getInstance().isDisplayOnlyUnread();
		
		mFeeds = new ArrayList<FeedItem>(); 
		List<FeedItem> list = DataController.getInstance().getSubscribedFeeds(mCategoryId, displayOnlyUnread);
	
		if (list != null) {
			for (FeedItem f : list) {
				mFeeds.add(f.deepCopy());
			}
		
			Collections.sort(mFeeds, new FeedItemComparator());
		}
		DataController.getInstance().disableForceFullRefresh();
	}
		
}
