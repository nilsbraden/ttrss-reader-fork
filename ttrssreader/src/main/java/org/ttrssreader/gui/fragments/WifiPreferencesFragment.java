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

package org.ttrssreader.gui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import org.ttrssreader.R;
import org.ttrssreader.gui.WifiPreferencesActivity;
import org.ttrssreader.preferences.Constants;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class WifiPreferencesFragment extends PreferenceFragmentCompat {

	private static final String TAG = WifiPreferencesFragment.class.getSimpleName();

	private static final String PREFS_MAIN_TOP = "prefs_main_top";
	private static final String PREFS_HTTP = "prefs_http";
	private static final String PREFS_SSL = "prefs_ssl";

	private String m_Ssid;
	private String m_CategoryName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		Bundle args = getArguments();
		if (args != null) {
			m_Ssid = getArguments().getString(WifiPreferencesActivity.KEY_SSID);
			m_CategoryName = getArguments().getString("cat");
		}
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		switch (m_CategoryName) {
			case PREFS_MAIN_TOP:
				addPreferencesFromResource(R.xml.prefs_main_top);
				break;
			case PREFS_HTTP:
				addPreferencesFromResource(R.xml.prefs_http);
				break;
			case PREFS_SSL:
				addPreferencesFromResource(R.xml.prefs_ssl);
				break;
		}

		initDynamicConnectionPrefs(m_Ssid, PREFS_MAIN_TOP.equals(m_CategoryName));
	}

	private void initDynamicConnectionPrefs(String ssid, boolean addEnableWifiPref) {
		if (getPreferenceScreen().getPreferenceCount() != 1)
			return;
		if (!(getPreferenceScreen().getPreference(0) instanceof PreferenceCategory))
			return;

		PreferenceCategory category = (PreferenceCategory) getPreferenceScreen().getPreference(0);

		SharedPreferences prefs = null;
		Context context = getActivity();
		if (context != null) {
			// If this fails we dont have default values:
			prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		}

		for (int i = 0; i < category.getPreferenceCount(); i++) {
			Preference pref = category.getPreference(i);

			String oldKey = pref.getKey();
			String newKey = ssid + oldKey;

			pref.setKey(newKey);

			Object defaultValue = null;
			if (prefs != null && prefs.getAll().containsKey(newKey))
				defaultValue = prefs.getAll().get(newKey);
			pref.setDefaultValue(defaultValue);

			// Key of dependency has probably been renamed. Beware: This might stop working if dependencies are
			// added in another order.
			if (pref.getDependency() != null)
				pref.setDependency(ssid + pref.getDependency());

			// Remove and add again to reinitialize default values
			category.removePreference(pref);
			category.addPreference(pref);
			Log.d(TAG, String.format("  oldKey: \"%s\" newKey: \"%s\"", oldKey, newKey));
		}

		if (addEnableWifiPref) {
			String key = ssid + Constants.ENABLE_WIFI_BASED_SUFFIX;
			CheckBoxPreference enableWifiPref = new CheckBoxPreference(getActivity());
			enableWifiPref.setKey(key);
			enableWifiPref.setTitle(R.string.ConnectionWifiPrefEnableTitle);
			enableWifiPref.setSummaryOn(R.string.ConnectionWifiPrefEnabledSummary);
			enableWifiPref.setSummaryOff(R.string.ConnectionWifiPrefDisbledSummary);

			Object defaultValue = null;
			if (prefs != null && prefs.getAll().containsKey(key))
				defaultValue = prefs.getAll().get(key);
			enableWifiPref.setDefaultValue(defaultValue);

			category.addPreference(enableWifiPref);
		}
	}
}
