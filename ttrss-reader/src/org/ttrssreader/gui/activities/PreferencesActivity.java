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

package org.ttrssreader.gui.activities;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.Utils;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class PreferencesActivity extends PreferenceActivity {
    
    private static final int MENU_RESET_PREFERENCES = Menu.FIRST;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);
        Log.i(Utils.TAG, "PreferencesActivity.onCreate()...");
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(mListener);
    }
    
    private void updatePreferences() {
        Controller.getInstance().initializeController(this);
    }
    
    private OnSharedPreferenceChangeListener mListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (Constants.getConstants().contains(key)) {
                updatePreferences();
            }
        }
    };
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        MenuItem item;
        
        item = menu.add(0, MENU_RESET_PREFERENCES, 0, R.string.Preferences_Reset);
        item.setIcon(R.drawable.refresh32);
        
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET_PREFERENCES:
                resetPreferences();
                return true;
        }
        
        return super.onMenuItemSelected(featureId, item);
    }
    
    protected void resetPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.putString(Constants.URL, Constants.URL_DEFAULT);
        editor.putString(Constants.USERNAME, Constants.USERNAME_DEFAULT);
        editor.putString(Constants.PASSWORD, Constants.PASSWORD_DEFAULT);
        editor.putBoolean(Constants.TRUST_ALL_SSL, Constants.TRUST_ALL_SSL_DEFAULT);
        editor.putBoolean(Constants.USE_KEYSTORE, Constants.USE_KEYSTORE_DEFAULT);
        editor.putString(Constants.KEYSTORE_PASSWORD, Constants.KEYSTORE_PASSWORD_DEFAULT);
        
        editor.putBoolean(Constants.AUTOMATIC_MARK_READ, Constants.AUTOMATIC_MARK_READ_DEFAULT);
        editor.putBoolean(Constants.OPEN_URL_EMPTY_ARTICLE, Constants.OPEN_URL_EMPTY_ARTICLE_DEFAULT);
        editor.putBoolean(Constants.UPDATE_UNREAD_ON_STARTUP, Constants.UPDATE_UNREAD_ON_STARTUP_DEFAULT);
        editor.putBoolean(Constants.REFRESH_SUB_DATA, Constants.REFRESH_SUB_DATA_DEFAULT);
        editor.putBoolean(Constants.USE_VOLUME_KEYS, Constants.USE_VOLUME_KEYS_DEFAULT);
        editor.putBoolean(Constants.VIBRATE_ON_LAST_ARTICLE, Constants.VIBRATE_ON_LAST_ARTICLE_DEFAULT);
        editor.putBoolean(Constants.WORK_OFFLINE, Constants.WORK_OFFLINE_DEFAULT);
        
        editor.putBoolean(Constants.SHOW_VIRTUAL, Constants.SHOW_VIRTUAL_DEFAULT);
        editor.putBoolean(Constants.SHOW_VIRTUAL_UNREAD, Constants.SHOW_VIRTUAL_UNREAD_DEFAULT);
        editor.putBoolean(Constants.ALWAYS_FULL_REFRESH, Constants.ALWAYS_FULL_REFRESH_DEFAULT);
        editor.putBoolean(Constants.USE_SWIPE, Constants.USE_SWIPE_DEFAULT);
        editor.putBoolean(Constants.ONLY_UNREAD, Constants.ONLY_UNREAD_DEFAULT);
        editor.putInt(Constants.ARTICLE_LIMIT, Constants.ARTICLE_LIMIT_DEFAULT);
        
        editor.putInt(Constants.DATABASE_VERSION, Constants.DATABASE_VERSION_DEFAULT);
        editor.putLong(Constants.LAST_UPDATE_TIME, Constants.LAST_UPDATE_TIME_DEFAULT);
        editor.putString(Constants.LAST_VERSION_RUN, Constants.LAST_VERSION_RUN_DEFAULT);
        
        editor.commit();
        this.finish();
        
        ComponentName comp = new ComponentName(this.getPackageName(), getClass().getName());
        startActivity(new Intent().setComponent(comp));
    }
    
}
