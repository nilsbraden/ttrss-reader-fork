/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
 * Copyright (C) 2009-2010 J. Devauchelle.
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

package org.ttrssreader.gui;

import java.io.File;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.preferences.FileBrowserHelper;
import org.ttrssreader.preferences.FileBrowserHelper.FileBrowserFailOverCallback;
import org.ttrssreader.utils.Utils;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class PreferencesActivity extends PreferenceActivity {
    
    public static final int ACTIVITY_SHOW_PREFERENCES = 43;
    public static final int ACTIVITY_CHOOSE_ATTACHMENT_FOLDER = 1;
    public static final int ACTIVITY_CHOOSE_CACHE_FOLDER = 2;
    
    private static AsyncTask<Void, Void, Void> init;
    
    private Context context;
    private Preference downloadPath;
    private Preference cachePath;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        addPreferencesFromResource(R.layout.preferences);
        setResult(ACTIVITY_SHOW_PREFERENCES);
        
        downloadPath = findPreference(Constants.SAVE_ATTACHMENT);
        downloadPath.setSummary(Controller.getInstance().saveAttachmentPath());
        downloadPath.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FileBrowserHelper.getInstance().showFileBrowserActivity(PreferencesActivity.this,
                        new File(Controller.getInstance().saveAttachmentPath()), ACTIVITY_CHOOSE_ATTACHMENT_FOLDER,
                        callbackDownloadPath);
                return true;
            }
            
            FileBrowserFailOverCallback callbackDownloadPath = new FileBrowserFailOverCallback() {
                
                @Override
                public void onPathEntered(String path) {
                    downloadPath.setSummary(path);
                    Controller.getInstance().setSaveAttachmentPath(path);
                }
                
                @Override
                public void onCancel() {
                    // canceled, do nothing
                }
            };
        });
        
        cachePath = findPreference(Constants.CACHE_FOLDER);
        cachePath.setSummary(Controller.getInstance().cacheFolder());
        cachePath.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FileBrowserHelper.getInstance().showFileBrowserActivity(PreferencesActivity.this,
                        new File(Controller.getInstance().cacheFolder()), ACTIVITY_CHOOSE_CACHE_FOLDER,
                        callbackCachePath);
                return true;
            }
            
            FileBrowserFailOverCallback callbackCachePath = new FileBrowserFailOverCallback() {
                
                @Override
                public void onPathEntered(String path) {
                    cachePath.setSummary(path);
                    Controller.getInstance().setCacheFolder(path);
                }
                
                @Override
                public void onCancel() {
                    // canceled, do nothing
                }
            };
        });
        
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(Controller.getInstance());
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                Controller.getInstance());
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(Controller.getInstance());
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        
        if (init != null) {
            init.cancel(true);
            init = null;
        }
        
        if (Utils.checkConfig()) {
            init = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    Controller.checkAndInitializeController(context, (0 != 1));
                    return null;
                }
            };
            if (Controller.getInstance().isExecuteOnExecutorAvailable())
                init.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                init.execute();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.preferences, menu);
        return true;
    }
    
    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.Preferences_Menu_Reset:
                resetPreferences();
                return true;
            case R.id.Preferences_Menu_ResetDatabase:
                resetDB();
                return true;
        }
        return false;
    }
    
    private void resetPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Constants.resetPreferences(prefs);
        this.finish();
        ComponentName comp = new ComponentName(this.getPackageName(), getClass().getName());
        startActivity(new Intent().setComponent(comp));
    }
    
    private void resetDB() {
        Controller.getInstance().setDeleteDBScheduled(true);
        DBHelper.getInstance().checkAndInitializeDB(this);
        
        this.finish();
        ComponentName comp = new ComponentName(this.getPackageName(), getClass().getName());
        startActivity(new Intent().setComponent(comp));
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        String path = null;
        if (resultCode == RESULT_OK && data != null) {
            // obtain the filename
            Uri fileUri = data.getData();
            if (fileUri != null)
                path = fileUri.getPath();
        }
        
        if (path != null) {
            switch (requestCode) {
                case ACTIVITY_CHOOSE_ATTACHMENT_FOLDER:
                    downloadPath.setSummary(path);
                    Controller.getInstance().setSaveAttachmentPath(path);
                    break;
                
                case ACTIVITY_CHOOSE_CACHE_FOLDER:
                    cachePath.setSummary(path);
                    Controller.getInstance().setCacheFolder(path);
                    break;
            }
        }
        
        super.onActivityResult(requestCode, resultCode, data);
    }
}
