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
        
        editor.putString(Constants.CONNECTION_URL, "http://localhost/");
        editor.putString(Constants.CONNECTION_USERNAME, "");
        editor.putString(Constants.CONNECTION_PASSWORD, "");
        editor.putBoolean(Constants.CONNECTION_TRUST_ALL_SSL, false);
        editor.putBoolean(Constants.CONNECTION_USE_KEYSTORE, false);
        editor.putString(Constants.CONNECTION_KEYSTORE_PASSWORD, "");
        
        editor.putBoolean(Constants.USAGE_AUTOMATIC_MARK_READ, true);
        editor.putBoolean(Constants.USAGE_OPEN_URL_EMPTY_ARTICLE, false);
        editor.putBoolean(Constants.USAGE_UPDATE_UNREAD_ON_STARTUP, false);
        editor.putBoolean(Constants.USAGE_REFRESH_SUB_DATA, false);
        editor.putBoolean(Constants.USAGE_USE_VOLUME_KEYS, true);
        editor.putBoolean(Constants.USAGE_VIBRATE_ON_LAST_ARTICLE, true);
        editor.putBoolean(Constants.USAGE_WORK_OFFLINE, false);
        
        editor.putBoolean(Constants.DISPLAY_SHOW_VIRTUAL, true);
        editor.putBoolean(Constants.DISPLAY_SHOW_VIRTUAL_UNREAD, false);
        editor.putBoolean(Constants.DISPLAY_ALWAYS_FULL_REFRESH, false);
        editor.putBoolean(Constants.DISPLAY_USE_SWIPE, true);
        editor.putBoolean(Constants.DISPLAY_ONLY_UNREAD, false);
        editor.putString(Constants.DISPLAY_ARTICLE_LIMIT, "100");
        
        editor.putString(Constants.DATABASE_VERSION, "1");
        editor.putString(Constants.LAST_UPDATE_TIME, "0");
        
        editor.commit();
        super.finish();
        this.finish();
        ComponentName comp = new ComponentName(this.getPackageName(), getClass().getName());
        startActivity(new Intent().setComponent(comp));
    }
    
}
