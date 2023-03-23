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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import org.ttrssreader.R;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.preferences.PreferencesActivity;

import java.util.HashMap;

import androidx.annotation.Keep;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

@Keep
public class WifiPreferencesFragment extends PreferenceFragmentCompat {

	private static final String TAG = WifiPreferencesFragment.class.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	private static String getSsidFromExtras(Bundle extras) {
		if (extras == null)
			return null;
		return extras.getString(PreferencesActivity.KEY_SSID);
	}

	/**
	 * This method changes the Keys of all the preferences in the given screen to a key consisting of
	 * <SSID + oldKey> which allows us to store different sets of preferences for specific Wifi
	 * Networks. All preferences are removed and added again to initialize them with the stored
	 * settings for the Wifi network and the dependencies are reset to point to the new keys too.
	 *
	 * @param context     the application context to access preferences
	 * @param extras      the given extras containing the selected network
	 * @param prefsScreen the PreferenceScreen that is to be edited
	 */
	static void tryInitializeWifiPrefs(Context context, Bundle extras, PreferenceScreen prefsScreen) {
		String ssid = getSsidFromExtras(extras);
		if (ssid == null)
			return;

		HashMap<String, String> dependencies = new HashMap<>();

		SharedPreferences prefs;
		if (context != null) {
			// If this fails we dont have default values:
			prefs = PreferenceManager.getDefaultSharedPreferences(context);
			prefsScreen.setOrderingAsAdded(false);

			if (prefsScreen.getPreferenceCount() != 1) {
				return;
			}
			if (!(prefsScreen.getPreference(0) instanceof PreferenceCategory)) {
				return;
			}

			PreferenceCategory category = (PreferenceCategory) prefsScreen.getPreference(0);
			category.setOrder(PreferenceCategory.DEFAULT_ORDER);
			CharSequence title = category.getTitle();
			category.setTitle(title + " (Wifi: \"" + ssid + "\")");

			// Store and then remove all dependencies:
			for (int i = 0; i < category.getPreferenceCount(); i++) {
				Preference pref = category.getPreference(i);
				String newKey = ssid + pref.getKey();

				if (pref.getDependency() != null) {
					dependencies.put(newKey, ssid + pref.getDependency());
					pref.setDependency(null);
				}
			}

			// Change key and add ssid, init with existing values for this ssid
			for (int i = 0; i < category.getPreferenceCount(); i++) {
				Preference pref = category.getPreference(i);
				pref.getExtras().putString(PreferencesActivity.KEY_SSID, ssid);

				// Remove to avoid duplicate keys
				category.removePreference(pref);

				String oldKey = pref.getKey();
				String newKey = ssid + oldKey;
				pref.setKey(newKey);

				Object defaultValue = null;
				if (prefs != null && prefs.getAll().containsKey(newKey)) {
					defaultValue = prefs.getAll().get(newKey);
				}
				pref.setDefaultValue(defaultValue);

				// Add again to reinitialize default values
				category.addPreference(pref);
				Log.d(TAG, String.format("  oldKey: \"%s\" newKey: \"%s\"", oldKey, newKey));
			}

			// Set dependencies later because we rename the keys and obviously the new keys
			// don't work until the dependant preferences are also renamed properly...
			for (String key : dependencies.keySet()) {
				Preference pref = category.findPreference(key);
				if (pref != null)
					pref.setDependency(dependencies.get(key));
			}
		}
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		String ssid = getSsidFromExtras(getArguments());
		if (ssid != null) {
			addPreferencesFromResource(R.xml.prefs_wifibased_main);

			if (getPreferenceScreen().getPreferenceCount() != 1)
				return;
			if (!(getPreferenceScreen().getPreference(0) instanceof PreferenceCategory))
				return;

			PreferenceCategory category = (PreferenceCategory) getPreferenceScreen().getPreference(0);
			category.setTitle(getString(R.string.ConnectionWifiMainPrefScreenTitle, ssid));

			SharedPreferences prefs;
			Context context = getContext();
			if (context != null) {
				// If this fails we dont have default values:
				prefs = PreferenceManager.getDefaultSharedPreferences(context);

				// Add checkbox for using this network with wifi based prefs:
				String key = ssid + Constants.ENABLE_WIFI_BASED_SUFFIX;
				CheckBoxPreference enableWifiPref = new CheckBoxPreference(context);
				enableWifiPref.setKey(key);
				enableWifiPref.setTitle(R.string.ConnectionWifiPrefEnableTitle);
				enableWifiPref.setSummaryOn(R.string.ConnectionWifiPrefEnabledSummary);
				enableWifiPref.setSummaryOff(R.string.ConnectionWifiPrefDisbledSummary);
				enableWifiPref.setOrder(-1);

				Object defaultValue = null;
				if (prefs != null && prefs.getAll().containsKey(key))
					defaultValue = prefs.getAll().get(key);
				enableWifiPref.setDefaultValue(defaultValue);

				category.addPreference(enableWifiPref);
			}

			for (int i = 0; i < category.getPreferenceCount(); i++) {
				Preference pref = category.getPreference(i);
				pref.getExtras().putString(PreferencesActivity.KEY_SSID, ssid);
			}
		}
	}
}
