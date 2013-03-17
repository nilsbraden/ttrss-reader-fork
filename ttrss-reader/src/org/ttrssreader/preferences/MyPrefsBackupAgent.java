package org.ttrssreader.preferences;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

/**
 * Just implement a BackupAgent for the SharedPreferences, nothing else is of importance to us.
 * 
 * @author Nils Braden
 * 
 */
public class MyPrefsBackupAgent extends BackupAgentHelper {
    
    static final String PREFS = "org.ttrssreader_preferences";
    static final String PREFS_BACKUP_KEY = "prefs";
    
    @Override
    public void onCreate() {
        SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, PREFS);
        addHelper(PREFS_BACKUP_KEY, helper);
    }
    
}
