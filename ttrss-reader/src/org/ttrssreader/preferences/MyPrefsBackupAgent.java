package org.ttrssreader.preferences;

import org.ttrssreader.utils.Utils;
import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.util.Log;

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
        Log.e(Utils.TAG, "== DEBUG: MyPrefsBackupAgent started...");
        SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, PREFS);
        addHelper(PREFS_BACKUP_KEY, helper);
    }
    
}
