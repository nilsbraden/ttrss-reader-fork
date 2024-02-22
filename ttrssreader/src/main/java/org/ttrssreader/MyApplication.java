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

package org.ttrssreader;

import android.content.Context;

import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.controllers.ProgressBarManager;
import org.ttrssreader.utils.PRNGFixes;

import androidx.multidex.MultiDexApplication;
import androidx.work.Configuration;
import androidx.work.WorkManager;

public class MyApplication extends MultiDexApplication {

	private static MyApplication instance;

	//	private static final String TAG = MyApplication.class.getSimpleName();

	public void onCreate() {
		super.onCreate();
		instance = this;
		PRNGFixes.apply();
		initSingletons();
		Data.getInstance().notifyListeners(); // Notify once to make sure the handler is initialized
		WorkManager.initialize(this, new Configuration.Builder().build());

		//AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
	}

	protected void initSingletons() {
		ProgressBarManager.getInstance();
		Controller.getInstance().initialize(this);
		DBHelper.getInstance().initialize(this);
		Data.getInstance().initialize(this);
	}

	@Override
	public void onLowMemory() {
		Controller.getInstance().lowMemory(true);
		super.onLowMemory();
	}

	public static Context context() {
		return instance.getApplicationContext();
	}

}
