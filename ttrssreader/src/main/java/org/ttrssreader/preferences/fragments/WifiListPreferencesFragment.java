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
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import org.ttrssreader.R;
import org.ttrssreader.preferences.PreferencesActivity;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

public class WifiListPreferencesFragment extends PreferenceFragmentCompat {

	@SuppressWarnings("unused")
	private static final String TAG = WifiListPreferencesFragment.class.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.prefs_wifibased);

		PreferenceCategory mWifibasedCategory = findPreference("wifibasedCategory");
		if (mWifibasedCategory == null)
			return;

		Activity activity = getActivity();
		if (activity == null)
			return;

		WifiManager mWifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		if (mWifiManager == null)
			return;

		List<WifiConfiguration> mWifiList = mWifiManager.getConfiguredNetworks();
		if (mWifiList == null)
			return;

		// Sort list by name (SSID) of the network, currently connected network is displayed on top
		Collections.sort(mWifiList, new Comparator<WifiConfiguration>() {
			@Override
			public int compare(WifiConfiguration lhs, WifiConfiguration rhs) {
				if (Objects.equals(lhs, rhs))
					return 0;
				if (lhs == null)
					return -1;
				if (rhs == null)
					return 1;
				if (WifiConfiguration.Status.CURRENT == lhs.status)
					return -1;
				if (WifiConfiguration.Status.CURRENT == rhs.status)
					return 1;
				return lhs.SSID.compareToIgnoreCase(rhs.SSID);
			}
		});

		for (WifiConfiguration wifi : mWifiList) {
			// Friendly SSID-Name
			String ssid = wifi.SSID.replaceAll("\"", "");

			// Add PreferenceScreen for each network
			PreferenceScreen pref = getPreferenceManager().createPreferenceScreen(getActivity());
			pref.setPersistent(false);
			pref.setKey("wifiNetwork" + ssid);
			pref.setTitle(ssid);

			pref.setFragment("org.ttrssreader.preferences.fragments.WifiPreferencesFragment");
			pref.getExtras().putString(PreferencesActivity.KEY_SSID, ssid);

			if (WifiConfiguration.Status.CURRENT == wifi.status)
				pref.setSummary(getResources().getString(R.string.ConnectionWifiConnected));
			else
				pref.setSummary(getResources().getString(R.string.ConnectionWifiNotInRange));
			mWifibasedCategory.addPreference(pref);
		}
	}
}
