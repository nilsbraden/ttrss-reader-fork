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

package org.ttrssreader.preferences;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.util.Log;

/**
 * Just implement a BackupAgent for the SharedPreferences, nothing else is of importance to us.
 *
 * @author Nils Braden
 */
public class MyPrefsBackupAgent extends BackupAgentHelper {

	private static final String TAG = MyPrefsBackupAgent.class.getSimpleName();

	static final String PREFS = "org.ttrssreader_preferences";
	static final String PREFS_BACKUP_KEY = "prefs";

	@Override
	public void onCreate() {
		Log.e(TAG, "== DEBUG: MyPrefsBackupAgent started...");
		SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, PREFS);
		addHelper(PREFS_BACKUP_KEY, helper);
	}

}
