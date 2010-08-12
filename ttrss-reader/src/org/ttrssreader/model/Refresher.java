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

package org.ttrssreader.model;

import org.ttrssreader.gui.IRefreshEndListener;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

public class Refresher extends AsyncTask<Void, Void, Void> {
	
	private IRefreshEndListener mParent;
	private IRefreshable mRefreshable;
	
	public Refresher(IRefreshEndListener parent, IRefreshable refreshable) {
		mParent = parent;
		mRefreshable = refreshable;
	}
	
	private Handler handler = new Handler() {
		
		public void handleMessage(Message msg) {
			mParent.onRefreshEnd();
		}
	};
	
	@Override
	protected Void doInBackground(Void... params) {
		// Refresh data oh this view
		mRefreshable.refreshData();
		handler.sendEmptyMessage(0);
		
		return null;
	}
	
}
