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
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.DataController;
import org.ttrssreader.gui.IRefreshEndListener;
import org.ttrssreader.gui.IUpdateEndListener;
import org.ttrssreader.model.DataUpdater;
import org.ttrssreader.model.Refresher;
import org.ttrssreader.model.Updater;
import org.ttrssreader.model.category.CategoryListAdapter;
import org.ttrssreader.utils.Utils;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ListView;

public class CategoryActivity extends ListActivity implements IRefreshEndListener, IUpdateEndListener {
	
	private static final int ACTIVITY_SHOW_FEEDS = 0;
	
	private static final int MENU_REFRESH = Menu.FIRST;
	private static final int MENU_UPDATE = Menu.FIRST + 1;
	private static final int MENU_SHOW_PREFERENCES = Menu.FIRST +2;
	private static final int MENU_SHOW_ABOUT = Menu.FIRST + 3;
	private static final int MENU_DISPLAY_ONLY_UNREAD = Menu.FIRST + 4;
	private static final int MENU_MARK_ALL_READ = Menu.FIRST + 5;
	
	private ListView mCategoryListView;
	private CategoryListAdapter mAdapter = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.category);

		Controller.getInstance().checkAndInitializeController(this);
		DBHelper.getInstance().checkAndInitializeController(this);

		setProgressBarIndeterminateVisibility(false);

		mCategoryListView = getListView();
	}
	
	@Override
	protected void onResume() {
		Log.i(Utils.TAG, "onResume() wird also auch beim starten aufgerufen...");
		super.onResume();
		doRefresh();
	}

	private void doRefresh() {
		setProgressBarIndeterminateVisibility(true);
		
		if (mAdapter == null) {
			mAdapter = new CategoryListAdapter(this);
			mCategoryListView.setAdapter(mAdapter);
		} 
		this.setTitle(this.getResources().getString(R.string.ApplicationName) + " (" + mAdapter.getTotalUnread() + ")");
		new Refresher(this, mAdapter);
	}

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        
        String categoryId = mAdapter.getCategoryId(position); 
        Intent i;
        
        if ((categoryId.equals("-1")) ||
        		(categoryId.equals("-2")) ||
        		(categoryId.equals("-3")) ||
        		(categoryId.equals("-4"))) {
        	// Virtual feeds
        	i = new Intent(this, FeedHeadlineListActivity.class);
        	i.putExtra(FeedHeadlineListActivity.FEED_ID, categoryId);
        	i.putExtra(FeedHeadlineListActivity.FEED_TITLE, mAdapter.getCategoryTitle(position));
        } else {
        	// Categories
        	i = new Intent(this, FeedListActivity.class);
        	i.putExtra(FeedListActivity.CATEGORY_ID, categoryId);
        	i.putExtra(FeedListActivity.CATEGORY_TITLE, mAdapter.getCategoryTitle(position));        	
        }                
    	
    	startActivityForResult(i, ACTIVITY_SHOW_FEEDS);
    }
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuItem item;
		
		item = menu.add(0, MENU_REFRESH, 0, R.string.Main_RefreshMenu);
		item.setIcon(R.drawable.refresh32);
		
		item = menu.add(0, MENU_UPDATE, 0, R.string.Main_UpdateMenu);
		
		item = menu.add(0, MENU_DISPLAY_ONLY_UNREAD, 0, R.string.Commons_DisplayOnlyUnread);
		
		item = menu.add(0, MENU_MARK_ALL_READ, 0, R.string.Commons_MarkAllRead);
		
		item = menu.add(0, MENU_SHOW_PREFERENCES, 0, R.string.Main_ShowPreferencesMenu);
		item.setIcon(R.drawable.preferences32);
		
		item = menu.add(0, MENU_SHOW_ABOUT, 0, R.string.Main_ShowAboutMenu);
		item.setIcon(R.drawable.about32);
		
		return true;
    }
	
	@Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
			case MENU_REFRESH:
				doForceRefresh();
				return true;
			case MENU_UPDATE:
				doUpdateEverything();
				return true;
			case MENU_DISPLAY_ONLY_UNREAD:
				displayOnlyUnreadSwitch();
				return true;
			case MENU_MARK_ALL_READ:
				markAllRead();
				return true;
			case MENU_SHOW_PREFERENCES:
				openPreferences();
				return true;
			case MENU_SHOW_ABOUT:
				openAboutDialog();
				return true;
		}
		
		return super.onMenuItemSelected(featureId, item);
    }
	
	private void doForceRefresh() {
		DataController.getInstance().forceFullRefresh();
		doRefresh();
	}
	
	private void doUpdateEverything() {
		setProgressBarIndeterminateVisibility(true);
		new Updater(this, new DataUpdater());
	}
	
	private void openPreferences() {
		Intent i = new Intent(this, PreferencesActivity.class);
		Log.e(Utils.TAG, "Starting PreferencesActivity");
		startActivity(i);
	}
	
	private void openAboutDialog() {
		Intent i = new Intent(this, AboutActivity.class);
		startActivity(i);
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
		} else {
			openConnectionErrorDialog(Controller.getInstance().getTTRSSConnector().getLastError());
		}
		setProgressBarIndeterminateVisibility(false);
	}

	@Override
	public void onUpdateEnd() {
		if (!Controller.getInstance().getTTRSSConnector().hasLastError()) {
			mAdapter.notifyDataSetChanged();
		} else {
			openConnectionErrorDialog(Controller.getInstance().getTTRSSConnector().getLastError());
		}

		setProgressBarIndeterminateVisibility(false);
		doRefresh();
	}

}
