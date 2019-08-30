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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.gui.WifiPreferencesActivity;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.preferences.FileBrowserHelper;
import org.ttrssreader.preferences.FileBrowserHelper.FileBrowserFailOverCallback;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class PreferencesFragment extends PreferenceFragment {

	private static final int ACTIVITY_CHOOSE_ATTACHMENT_FOLDER = 1;
	private static final int ACTIVITY_CHOOSE_CACHE_FOLDER = 2;

	private static final String PREFS_DISPLAY = "prefs_display";
	private static final String PREFS_HEADERS = "prefs_headers";
	private static final String PREFS_HTTP = "prefs_http";
	private static final String PREFS_MAIN_TOP = "prefs_main_top";
	private static final String PREFS_SSL = "prefs_ssl";
	private static final String PREFS_SYSTEM = "prefs_system";
	private static final String PREFS_USAGE = "prefs_usage";
	private static final String PREFS_WIFI = "prefs_wifibased";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String cat = getArguments().getString("cat");
		if (PREFS_DISPLAY.equals(cat))
			addPreferencesFromResource(R.xml.prefs_display);
		if (PREFS_HEADERS.equals(cat))
			addPreferencesFromResource(R.xml.prefs_headers);
		if (PREFS_HTTP.equals(cat))
			addPreferencesFromResource(R.xml.prefs_http);
		if (PREFS_MAIN_TOP.equals(cat))
			addPreferencesFromResource(R.xml.prefs_main_top);
		if (PREFS_SSL.equals(cat))
			addPreferencesFromResource(R.xml.prefs_ssl);
		if (PREFS_SYSTEM.equals(cat)) {
			addPreferencesFromResource(R.xml.prefs_system);
			// Manually initialize Listeners for Download- and CachePath
			initializePreferences(this);
		}
		if (PREFS_USAGE.equals(cat))
			addPreferencesFromResource(R.xml.prefs_usage);
		if (PREFS_WIFI.equals(cat)) {
			initWifibasedPreferences();
		}
	}

	public static void initializePreferences(final PreferencesFragment fragment) {
		final Preference downloadPath = fragment.findPreference(Constants.SAVE_ATTACHMENT);

		if (downloadPath != null) {
			downloadPath.setSummary(Controller.getInstance().saveAttachmentPath());
			downloadPath.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					FileBrowserHelper.getInstance().showFileBrowserActivity(fragment, new File(Controller.getInstance().saveAttachmentPath()), ACTIVITY_CHOOSE_ATTACHMENT_FOLDER, callbackDownloadPath);
					return true;
				}

				// Fail-Safe Dialog for when there is no filebrowser installed:
				FileBrowserFailOverCallback callbackDownloadPath = new FileBrowserFailOverCallback() {
					@Override
					public void onPathEntered(String path) {
						downloadPath.setSummary(path);
						Controller.getInstance().setSaveAttachmentGeneric(path);
					}

					@Override
					public void onCancel() {
					}
				};
			});
		}
	}

	private void initWifibasedPreferences() {
		addPreferencesFromResource(R.xml.prefs_wifibased);
		PreferenceCategory mWifibasedCategory = (PreferenceCategory) findPreference("wifibasedCategory");
		WifiManager mWifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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

			Intent intent = new Intent(getActivity(), WifiPreferencesActivity.class);
			intent.putExtra(WifiPreferencesActivity.KEY_SSID, ssid);
			pref.setIntent(intent);
			if (WifiConfiguration.Status.CURRENT == wifi.status)
				pref.setSummary(getResources().getString(R.string.ConnectionWifiConnected));
			else
				pref.setSummary(getResources().getString(R.string.ConnectionWifiNotInRange));
			mWifibasedCategory.addPreference(pref);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK || requestCode != ACTIVITY_CHOOSE_ATTACHMENT_FOLDER || data == null) {
			super.onActivityResult(requestCode, resultCode, data);
			return;
		}

		// obtain the filename
		Uri fileUri = data.getData();
		if (fileUri != null) {
			Preference downloadPath = findPreference(Constants.SAVE_ATTACHMENT);
			downloadPath.setSummary(fileUri.getPath());

			// Use takePersistableUriPermission
			if (Build.VERSION.SDK_INT >= 100000) { //Build.VERSION_CODES.LOLLIPOP) { // TODO Use proper VERSION_CODE
				ContentResolver resolver = getContext().getContentResolver();
				int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
				resolver.takePersistableUriPermission(fileUri, flags);

				Controller.getInstance().setSaveAttachmentGeneric(fileUri.toString());
			} else {
				Controller.getInstance().setSaveAttachmentGeneric(fileUri.getPath());
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

}
