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

package org.ttrssreader.gui.activities;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DataController;
import org.ttrssreader.gui.IRefreshEndListener;
import org.ttrssreader.gui.IUpdateEndListener;
import org.ttrssreader.model.Refresher;
import org.ttrssreader.model.Updater;
import org.ttrssreader.model.article.ArticleReadStateUpdater;
import org.ttrssreader.model.feedheadline.FeedHeadlineListAdapter;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ListView;

public class FeedHeadlineListActivity extends ListActivity implements IRefreshEndListener, IUpdateEndListener {
	
	private static final int ACTIVITY_SHOW_FEED_ITEM = 0;
	
	private static final int MENU_REFRESH = Menu.FIRST;
	private static final int MENU_MARK_ALL_READ = Menu.FIRST + 1;
//	private static final int MENU_MARK_ALL_UNREAD = Menu.FIRST + 2;
	private static final int MENU_DISPLAY_ONLY_UNREAD = Menu.FIRST + 3;
	
	public static final String FEED_ID = "FEED_ID";
	public static final String FEED_TITLE = "FEED_TITLE";
	
	private String mFeedId;
	private String mFeedTitle;
	
	private ListView mFeedHeadlineListView;
	private FeedHeadlineListAdapter mAdapter = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.feedheadlinelist);

		setProgressBarIndeterminateVisibility(false);
		mFeedHeadlineListView = getListView();
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mFeedId = extras.getString(FEED_ID);
			mFeedTitle = extras.getString(FEED_TITLE);
		} else if (savedInstanceState != null) {
			mFeedId = savedInstanceState.getString(FEED_ID);
			mFeedTitle = savedInstanceState.getString(FEED_TITLE);
		} else {
			mFeedId = "-1";
			mFeedTitle = null;
		}
	}
	
	@Override
	protected void onResume() {
		doRefresh();
		super.onResume();
	}
	
	private void doRefresh() {
		setProgressBarIndeterminateVisibility(true);
		
		if (mAdapter == null) {
			mAdapter = new FeedHeadlineListAdapter(this, mFeedId);
			mFeedHeadlineListView.setAdapter(mAdapter);
		}
		new Refresher(this, mAdapter);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		Intent i = new Intent(this, ArticleActivity.class);
		i.putExtra(ArticleActivity.ARTICLE_ID, mAdapter.getFeedItemId(position));
		i.putExtra(ArticleActivity.FEED_ID, mFeedId);
		i.putStringArrayListExtra(ArticleActivity.ARTICLE_LIST, mAdapter.getFeedItemIds());
		
		startActivityForResult(i, ACTIVITY_SHOW_FEED_ITEM);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuItem item;
		
		item = menu.add(0, MENU_REFRESH, 0, R.string.Main_RefreshMenu);
		item.setIcon(R.drawable.refresh32);
		
		item = menu.add(0, MENU_MARK_ALL_READ, 0, R.string.FeedHeadlinesListActivity_MarkAllRead);
		
//		item = menu.add(0, MENU_MARK_ALL_UNREAD, 0, R.string.FeedHeadlinesListActivity_MarkAllUnread);
		
		item = menu.add(0, MENU_DISPLAY_ONLY_UNREAD, 0, R.string.Commons_DisplayOnlyUnread);
		
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
			case MENU_REFRESH:
				doForceRefresh();
				return true;
			case MENU_MARK_ALL_READ:
				setReadState();
				return true;
//			case MENU_MARK_ALL_UNREAD:
//				setUnreadState();
//				return true;
			case MENU_DISPLAY_ONLY_UNREAD:
				displayOnlyUnreadSwitch();
				return true;
		}
		
		return super.onMenuItemSelected(featureId, item);
	}
	
	private void doForceRefresh() {
		DataController.getInstance().forceFullRefresh();
		doRefresh();
	}
	
	private void setReadState() {
		setProgressBarIndeterminateVisibility(true);
		new Updater(this, new ArticleReadStateUpdater(null, mFeedId));
	}
	
//	private void setUnreadState() {
//		setProgressBarIndeterminateVisibility(true);
//		new Updater(this, new ArticleReadStateUpdater(null, mFeedId));
//	}
	
	private void displayOnlyUnreadSwitch() {
		boolean displayOnlyUnread = Controller.getInstance().isDisplayOnlyUnread();
		Controller.getInstance().setDisplayOnlyUnread(!displayOnlyUnread);
		doRefresh();
	}
	
	private void openConnectionErrorDialog(String errorMessage) {
		Intent i = new Intent(this, ConnectionErrorActivity.class);
		i.putExtra(ConnectionErrorActivity.ERROR_MESSAGE, errorMessage);
		startActivity(i);
	}
	
	@Override
	public void onRefreshEnd() {
		if (!Controller.getInstance().getTTRSSConnector().hasLastError()) {
			mAdapter.notifyDataSetChanged();
			
			if (mFeedTitle != null) {
				this.setTitle(this.getResources().getString(R.string.ApplicationName) + " - " + mFeedTitle + " ("
						+ mAdapter.getUnreadCount() + ")");
			} else {
				this.setTitle(this.getResources().getString(R.string.ApplicationName) + " ("
						+ mAdapter.getUnreadCount() + ")");
			}
		} else {
			openConnectionErrorDialog(Controller.getInstance().getTTRSSConnector().getLastError());
		}

		setProgressBarIndeterminateVisibility(false);
		
		if (Controller.getInstance().isDisplayOnlyUnread()) {
			// Close FeedHeadlineList if no unread article exists
			// AND: Not doing this with virtual feeds, starting with "-[0-9]"
			if (mAdapter.getArticleUnreadList().isEmpty() && !mFeedId.matches("-[0-9]")) {
				finish();
			}
			//			
			// // Directly open Article if only one unread Article exists
			// if (mAdapter.getArticleUnreadList().size() == 1) {
			// Intent i = new Intent(this, ArticleActivity.class);
			// i.putExtra(ArticleActivity.ARTICLE_ID, mAdapter.getFeedItemId(0));
			// i.putExtra(ArticleActivity.FEED_ID, mFeedId);
			// i.putStringArrayListExtra(ArticleActivity.ARTICLE_LIST, mAdapter.getFeedItemIds());
			//				
			// startActivityForResult(i, ACTIVITY_SHOW_FEED_ITEM);
			// }
		}
	}
	
	@Override
	public void onSubRefreshEnd() {
		if (!Controller.getInstance().getTTRSSConnector().hasLastError()) {
			mAdapter.notifyDataSetChanged();
		} else {
			openConnectionErrorDialog(Controller.getInstance().getTTRSSConnector().getLastError());
		}

		setProgressBarIndeterminateVisibility(false);
		DataController.getInstance().disableForceFullRefresh();
	}
	
	@Override
	public void onUpdateEnd() {
		setProgressBarIndeterminateVisibility(false);
		doRefresh();
	}
	
}
