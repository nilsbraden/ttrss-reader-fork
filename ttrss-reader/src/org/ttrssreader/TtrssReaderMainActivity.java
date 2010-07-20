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

package org.ttrssreader;

import org.ttrssreader.controllers.Controller;
import org.ttrssreader.gui.activities.ConnectionErrorActivity;
import org.ttrssreader.gui.activities.FeedListActivity;
import org.ttrssreader.gui.activities.OverviewActivity;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;

public class TtrssReaderMainActivity extends TabActivity {
	
	private TabHost tabHost;
	
	private ProgressDialog mProgressDialog;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		Controller.getInstance().checkAndInitializeController(this);
		
		// Create Tab-Layout...
		tabHost = getTabHost();
		TabHost.TabSpec spec;
		Intent i;
		
		// TAB 1: Overview, Virtual Categories, other Categories
		i = new Intent().setClass(this, OverviewActivity.class);
		spec = tabHost.newTabSpec("TAB_1").setIndicator("Overview").setContent(i);
		tabHost.addTab(spec);
		
		// TAB 2: All uncategorized Feeds
		i = new Intent().setClass(this, FeedListActivity.class);
		i.putExtra(FeedListActivity.CATEGORY_ID, "0");
		i.putExtra(FeedListActivity.CATEGORY_TITLE, "Uncategorized Feeds");
		spec = tabHost.newTabSpec("TAB_2").setIndicator("Uncategorized Feeds").setContent(i);
		tabHost.addTab(spec);
		
		tabHost.getTabWidget().getChildAt(0).getLayoutParams().height = 60;
		tabHost.getTabWidget().getChildAt(1).getLayoutParams().height = 60;
	}
	
	@Override
	protected void onResume() {
		doRefresh();
		super.onResume();
	}
	
	private void doRefresh() {
		Controller.getInstance().setRefreshNeeded(false);
		
		mProgressDialog = ProgressDialog.show(this, "Refreshing", this.getResources().getString(
				R.string.Commons_PleaseWait));
		
		int totalUnread = Controller.getInstance().getTTRSSConnector().getTotalUnread();
		
		if (totalUnread > 0) {
			this.setTitle(this.getResources().getString(R.string.ApplicationName) + " (" + totalUnread + ")");
		} else {
			this.setTitle(this.getResources().getString(R.string.ApplicationName));
		}
		
		if (!Controller.getInstance().getTTRSSConnector().hasLastError()) {
		} else {
			openConnectionErrorDialog(Controller.getInstance().getTTRSSConnector().getLastError());
		}
		mProgressDialog.dismiss();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		return super.onMenuItemSelected(featureId, item);
	}
	
	private void openConnectionErrorDialog(String errorMessage) {
		Intent i = new Intent(this, ConnectionErrorActivity.class);
		i.putExtra(ConnectionErrorActivity.ERROR_MESSAGE, errorMessage);
		startActivity(i);
	}
	
}
