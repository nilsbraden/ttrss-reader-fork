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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package org.ttrssreader.gui.activities;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.preferences.PreferencesConstants;
import org.ttrssreader.utils.Utils;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class PreferencesActivity extends PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.preferences);
		Log.e(Utils.TAG, "PreferencesActivity.onCreate()...");
		
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				if ((key.equals(PreferencesConstants.CONNECTION_URL)) ||
						(key.equals(PreferencesConstants.CONNECTION_USERNAME)) ||
						(key.equals(PreferencesConstants.CONNECTION_PASSWORD)) ||
						(key.equals(PreferencesConstants.DISPLAY_SHOW_VIRTUAL_UNREAD)) ||
						(key.equals(PreferencesConstants.DISPLAY_ALWAYS_FULL_REFRESH)) ||
						(key.equals(PreferencesConstants.USAGE_AUTOMATIC_MARK_READ)) ||
						(key.equals(PreferencesConstants.USE_SWIPE))) {
					updatePreferences();
				}
			}			
		});
	}
	
	private void updatePreferences() {
		Controller.getInstance().initializeController(this);
	}

}
