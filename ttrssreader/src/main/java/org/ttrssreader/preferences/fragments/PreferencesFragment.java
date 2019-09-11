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

package org.ttrssreader.preferences.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.preferences.PreferencesActivity;
import org.ttrssreader.utils.AsyncTask;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class PreferencesFragment extends PreferenceFragmentCompat {

	@SuppressWarnings("unused")
	private static final String TAG = PreferencesFragment.class.getSimpleName();

	private static final String KEY_RESET_PREFERENCES = "ResetPreferences";
	private static final String KEY_RESET_DATABASE = "ResetDatabase";
	private static final String KEY_RESET_CACHE = "ResetCache";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.prefs_main);
	}

	@Override
	public boolean onPreferenceTreeClick(Preference preference) {
		// Only start reset-task if one of the three reset-items was clicked
		String key = preference.getKey();
		if (key != null) {
			switch (key) {
				case KEY_RESET_PREFERENCES:
				case KEY_RESET_DATABASE:
				case KEY_RESET_CACHE:
					new ResetTask(getContext()).execute(key);
					break;
				default:
					break;
			}
		}
		return super.onPreferenceTreeClick(preference);
	}

	/**
	 * All Reset-Operations should be done from Background.
	 */
	private class ResetTask extends AsyncTask<String, Void, Void> {

		private final Context context;
		private int textResource;
		boolean resetDone = false;

		ResetTask(Context context) {
			this.context = context;
		}

		@Override
		protected Void doInBackground(String... params) {
			switch (params[0]) {
				case KEY_RESET_PREFERENCES:
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
					Constants.resetPreferences(prefs);
					textResource = R.string.Preferences_Reset_Done;
					resetDone = true;
					break;
				case KEY_RESET_DATABASE:
					Controller.getInstance().setDeleteDBScheduled(true);
					Controller.getInstance().setSinceId(0);
					DBHelper.getInstance().initialize(context);
					Data.getInstance().initTimers();
					textResource = R.string.Preferences_ResetDatabase_Done;
					resetDone = true;
					break;
				case KEY_RESET_CACHE:
					Data.getInstance().deleteAllRemoteFiles();
					DBHelper.getInstance().initialize(context);
					textResource = R.string.Preferences_ResetCache_Done;
					resetDone = true;
					break;
				default:
					resetDone = false;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			// Only apply if we actually did reset anything
			if (resetDone) {
				Toast.makeText(context, textResource, Toast.LENGTH_SHORT).show();
				Activity activity = getActivity();
				if (activity != null) {
					activity.setResult(PreferencesActivity.ACTIVITY_RELOAD);
					activity.finish();
				}
			}
			super.onPostExecute(aVoid);
		}
	}
}
