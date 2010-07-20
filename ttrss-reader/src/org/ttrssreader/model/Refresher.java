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
import android.os.Handler;
import android.os.Message;

public class Refresher implements Runnable {
	
	private IRefreshEndListener mParent;
	private IRefreshable mRefreshable;
	
	public Refresher(IRefreshEndListener parent, IRefreshable refreshable) {
		mParent = parent;
		mRefreshable = refreshable;
		
		/* Disable threading, if "displayOnlyUnread" is active we get the following Exception: /*
		/*
		 * java.lang.IllegalStateException: The content of the adapter has changed but ListView did not receive a
		 * notification. Make sure the content of your adapter is not modified from a background thread, but only from
		 * the UI thread. [in ListView(16908298, class android.widget.ListView) with Adapter(class
		 * org.ttrssreader.model.feedheadline.FeedHeadlineListAdapter)]
		 */
		
		// Thread mThread = new Thread(this);
		// mThread.start();
		run();
	}
	
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			mParent.onRefreshEnd();
		}
	};
	
	@Override
	public void run() {
		mRefreshable.refreshData();
		handler.sendEmptyMessage(0);
	}
	
}
