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
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

public class PreferencesActivity extends PreferenceActivity {
    
    public static final int ACTIVITY_SHOW_PREFERENCES = 43;
    private static final int MENU_RESET_PREFERENCES = Menu.FIRST;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(mListener);
        setResult(ACTIVITY_SHOW_PREFERENCES);
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
        item.setIcon(android.R.drawable.ic_menu_revert);
        
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
        Constants.resetPreferences(prefs);
        this.finish();
        ComponentName comp = new ComponentName(this.getPackageName(), getClass().getName());
        startActivity(new Intent().setComponent(comp));
    }
    
}
