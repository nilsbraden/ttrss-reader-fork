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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package org.ttrssreader.gui.activities;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DataController;
import org.ttrssreader.gui.IRefreshEndListener;
import org.ttrssreader.model.Refresher;
import org.ttrssreader.model.feed.FeedListAdapter;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

public class FeedListActivity extends ListActivity implements IRefreshEndListener {
	
	private static final int ACTIVITY_SHOW_FEED_HEADLINE = 0;
	
	private static final int MENU_REFRESH = Menu.FIRST;
	private static final int MENU_DISPLAY_ONLY_UNREAD = Menu.FIRST + 1;
	private static final int MENU_MARK_ALL_READ = Menu.FIRST + 2;
	
	public static final String CATEGORY_ID = "CATEGORY_ID";
	public static final String CATEGORY_TITLE = "CATEGORY_TITLE";
	
	private String mCategoryId;
	private String mCategoryTitle;
	
	private ListView mFeedListView;
	private FeedListAdapter mAdapter = null;
	
	private ProgressDialog mProgressDialog;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feedlist);
		
//		Controller.getInstance().checkAndInitializeController(this);

		mFeedListView = getListView();

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mCategoryId = extras.getString(CATEGORY_ID);
			mCategoryTitle = extras.getString(CATEGORY_TITLE);
		} else if (savedInstanceState != null) {
			mCategoryId = savedInstanceState.getString(CATEGORY_ID);
			mCategoryTitle = savedInstanceState.getString(CATEGORY_TITLE);
		} else {
			mCategoryId = "-1";
			mCategoryTitle = null;
		}		
	}
	
	@Override
	protected void onResume() {	
		doRefresh();
		super.onResume();
	}
	
	@Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CATEGORY_ID, mCategoryId);
        outState.putString(CATEGORY_TITLE, mCategoryTitle);
    }

	private void doRefresh() {
		mProgressDialog = ProgressDialog.show(this, "Refreshing", this.getResources().getString(R.string.Commons_PleaseWait));

		if (mAdapter == null) {
			mAdapter = new FeedListAdapter(this, mCategoryId);
			mFeedListView.setAdapter(mAdapter);
		}
		new Refresher(this, mAdapter);		
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Intent i = new Intent(this, FeedHeadlineListActivity.class);
		i.putExtra(FeedHeadlineListActivity.FEED_ID, mAdapter.getFeedId(position));
		i.putExtra(FeedHeadlineListActivity.FEED_TITLE, mAdapter.getFeedTitle(position));

		startActivityForResult(i, ACTIVITY_SHOW_FEED_HEADLINE);
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	
    	MenuItem item;
    	
    	item = menu.add(0, MENU_REFRESH, 0, R.string.Main_RefreshMenu);
        item.setIcon(R.drawable.refresh32);
        
		item = menu.add(0, MENU_DISPLAY_ONLY_UNREAD, 0, R.string.Commons_DisplayOnlyUnread);
		
		item = menu.add(0, MENU_MARK_ALL_READ, 0, R.string.Commons_MarkAllRead);
        
    	return true;
    }
	
	@Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	switch(item.getItemId()) {
    	case MENU_REFRESH:
    		doForceRefresh();
            return true;
    	case MENU_DISPLAY_ONLY_UNREAD:
    		displayOnlyUnreadSwitch();
    		return true;
		case MENU_MARK_ALL_READ:
			markAllRead();
			return true;
    	}
    	
    	return super.onMenuItemSelected(featureId, item);
    }
	
	private void doForceRefresh() {
		DataController.getInstance().forceFullRefresh();
		doRefresh();
	}
	
	private void displayOnlyUnreadSwitch() {
		boolean displayOnlyUnread = Controller.getInstance().isDisplayOnlyUnreadEnabled();
		Controller.getInstance().setDisplayOnlyUnread(this, !displayOnlyUnread);
		doRefresh();
	}
	
	private void markAllRead() {
		mAdapter.markAllRead();
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

			if (mCategoryTitle != null) {
				this.setTitle(this.getResources().getString(R.string.ApplicationName) + " - " + mCategoryTitle + " (" + mAdapter.getTotalUnreadCount() + ")");
			} else {
				this.setTitle(this.getResources().getString(R.string.ApplicationName) + " (" + mAdapter.getTotalUnreadCount() + ")");
			}
		} else {
			openConnectionErrorDialog(Controller.getInstance().getTTRSSConnector().getLastError());
		}
		
		mProgressDialog.dismiss();
	}

}
