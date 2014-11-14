/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 Nils Braden
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
