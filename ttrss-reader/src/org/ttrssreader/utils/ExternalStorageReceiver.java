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

package org.ttrssreader.utils;

import org.ttrssreader.controllers.DBHelper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.util.Log;


public class ExternalStorageReceiver extends BroadcastReceiver {

	private boolean mExternalStorageAvailable = false;
	private boolean mExternalStorageWriteable = false;
	public IntentFilter filter;
	
	public ExternalStorageReceiver() {	
		filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_REMOVED);
		updateExternalStorageState();
	}
	
	public IntentFilter getFilter() {
		return filter;
	}
	
	private void updateExternalStorageState() {
		String state = Environment.getExternalStorageState();
		
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		
		handleExternalStorageState(mExternalStorageAvailable, mExternalStorageWriteable);
	}
	
	private void handleExternalStorageState(boolean storageAvailable, boolean storageWriteable) {
		DBHelper.getInstance().setExternalDB(storageWriteable);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("test", "Storage: " + intent.getData());
		updateExternalStorageState();
	}
	
}
