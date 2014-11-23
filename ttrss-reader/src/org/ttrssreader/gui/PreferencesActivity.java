/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 Nils Braden
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

import java.util.List;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.gui.fragments.PreferencesFragment;
import org.ttrssreader.model.HeaderAdapter;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.PostMortemReportExceptionHandler;
import org.ttrssreader.utils.Utils;
import android.app.backup.BackupManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListAdapter;

public class PreferencesActivity extends PreferenceActivity {
    
    @SuppressWarnings("unused")
    private static final String TAG = PreferencesActivity.class.getSimpleName();
    
    private PostMortemReportExceptionHandler mDamageReport = new PostMortemReportExceptionHandler(this);
    
    private static AsyncTask<Void, Void, Void> init;
    private static List<Header> _headers;
    private boolean needResource = false;
    
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Controller.getInstance().getTheme());
        super.onCreate(savedInstanceState); // IMPORTANT!
        mDamageReport.initialize();
        setResult(Constants.ACTIVITY_SHOW_PREFERENCES);
        
        if (needResource) {
            addPreferencesFromResource(R.xml.prefs_main_top);
            addPreferencesFromResource(R.xml.prefs_http);
            addPreferencesFromResource(R.xml.prefs_ssl);
            addPreferencesFromResource(R.xml.prefs_wifi);
            addPreferencesFromResource(R.xml.prefs_usage);
            addPreferencesFromResource(R.xml.prefs_display);
            addPreferencesFromResource(R.xml.prefs_system);
            addPreferencesFromResource(R.xml.prefs_main_bottom);
        }
    }
    
    @Override
    public void onBuildHeaders(List<Header> headers) {
        _headers = headers;
        if (onIsHidingHeaders()) {
            needResource = true;
        } else {
            loadHeadersFromResource(R.xml.prefs_headers, headers);
        }
    }
    
    @Override
    public void setListAdapter(ListAdapter adapter) {
        if (adapter == null) {
            super.setListAdapter(null);
        } else {
            super.setListAdapter(new HeaderAdapter(this, _headers));
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(
                Controller.getInstance());
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(
                Controller.getInstance());
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        
        if (init != null) {
            init.cancel(true);
            init = null;
        }
        
        if (!Utils.checkIsConfigInvalid()) {
            init = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    Controller.getInstance().checkAndInitializeController(getApplicationContext(), null);
                    return null;
                }
            };
            init.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        if (Controller.getInstance().isPreferencesChanged()) {
            new BackupManager(this).dataChanged();
            Controller.getInstance().setPreferencesChanged(false);
        }
    }
    
    @Override
    protected void onDestroy() {
        mDamageReport.restoreOriginalHandler();
        mDamageReport = null;
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
        ComponentName comp = new ComponentName(this.getPackageName(), getClass().getName());
        switch (item.getItemId()) {
            case R.id.Preferences_Menu_Reset:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                Constants.resetPreferences(prefs);
                this.finish();
                startActivity(new Intent().setComponent(comp));
                return true;
            case R.id.Preferences_Menu_ResetDatabase:
                Controller.getInstance().setDeleteDBScheduled(true);
                DBHelper.getInstance().checkAndInitializeDB(this);
                this.finish();
                startActivity(new Intent().setComponent(comp));
                return true;
        }
        return false;
    }
    
    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (PreferencesFragment.class.getName().equals(fragmentName))
            return true;
        return false;
    }
    
    @Override
    public void switchToHeader(Header header) {
        if (header.fragment != null) {
            super.switchToHeader(header);
        }
    }
    
}
