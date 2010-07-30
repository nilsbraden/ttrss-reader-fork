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
import org.ttrssreader.utils.Utils;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Refresher implements Runnable {
	
	private IRefreshEndListener mParent;
	private IRefreshable mRefreshable;
	
	
	public Refresher(IRefreshEndListener parent, IRefreshable refreshable) {
		mParent = parent;
		mRefreshable = refreshable;
		
		Thread mThread = new Thread(this);
		mThread.start();
	}
	
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.arg1 == 0) {
				Log.d(Utils.TAG, "Calling onRefreshEnd");
				mParent.onRefreshEnd();
			} else if (msg.arg1 == 1) {
				Log.d(Utils.TAG, "Calling onSUBRefreshEnd");
				mParent.onSubRefreshEnd();
			}
		}
	};
	
	@Override
	public void run() {
		// Refresh data oh this view
		mRefreshable.refreshData();
		Message m = new Message();
		m.arg1 = 0;
		handler.sendMessage(m);
		
		// Refresh data of next view below the current one, e.g. Feeds->Headlines.
		mRefreshable.refreshSubData();
		handler.sendEmptyMessage(1);
		
		m = new Message();
		m.arg1 = 1;
		handler.sendMessage(m);
	}
	
}
