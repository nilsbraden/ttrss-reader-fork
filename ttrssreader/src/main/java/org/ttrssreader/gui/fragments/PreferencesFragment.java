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

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.gui.WifiPreferencesActivity;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.preferences.FileBrowserHelper;
import org.ttrssreader.preferences.FileBrowserHelper.FileBrowserFailOverCallback;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import java.io.File;
import java.util.List;

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
		if (PREFS_DISPLAY.equals(cat)) addPreferencesFromResource(R.xml.prefs_display);
		if (PREFS_HEADERS.equals(cat)) addPreferencesFromResource(R.xml.prefs_headers);
		if (PREFS_HTTP.equals(cat)) addPreferencesFromResource(R.xml.prefs_http);
		if (PREFS_MAIN_TOP.equals(cat)) addPreferencesFromResource(R.xml.prefs_main_top);
		if (PREFS_SSL.equals(cat)) addPreferencesFromResource(R.xml.prefs_ssl);
		if (PREFS_SYSTEM.equals(cat)) {
			addPreferencesFromResource(R.xml.prefs_system);
			// Manually initialize Listeners for Download- and CachePath
			initializePreferences(this);
		}
		if (PREFS_USAGE.equals(cat)) addPreferencesFromResource(R.xml.prefs_usage);
		if (PREFS_WIFI.equals(cat)) {
			initWifibasedPreferences();
		}
	}

	public static void initializePreferences(final PreferencesFragment fragment) {
		final Preference downloadPath = fragment.findPreference(Constants.SAVE_ATTACHMENT);
		final Preference cachePath = fragment.findPreference(Constants.CACHE_FOLDER);

		if (downloadPath != null) {
			downloadPath.setSummary(Controller.getInstance().saveAttachmentPath());
			downloadPath.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					FileBrowserHelper.getInstance()
							.showFileBrowserActivity(fragment, new File(Controller.getInstance().saveAttachmentPath()),
									ACTIVITY_CHOOSE_ATTACHMENT_FOLDER, callbackDownloadPath);
					return true;
				}

				// Fail-Safe Dialog for when there is no filebrowser installed:
				FileBrowserFailOverCallback callbackDownloadPath = new FileBrowserFailOverCallback() {
					@Override
					public void onPathEntered(String path) {
						downloadPath.setSummary(path);
						Controller.getInstance().setSaveAttachmentPath(path);
					}

					@Override
					public void onCancel() {
					}
				};
			});
		}

		if (cachePath != null) {
			cachePath.setSummary(Controller.getInstance().cacheFolder());
			cachePath.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					FileBrowserHelper.getInstance()
							.showFileBrowserActivity(fragment, new File(Controller.getInstance().cacheFolder()),
									ACTIVITY_CHOOSE_CACHE_FOLDER, callbackCachePath);
					return true;
				}

				// Fail-Safe Dialog for when there is no filebrowser installed:
				FileBrowserFailOverCallback callbackCachePath = new FileBrowserFailOverCallback() {
					@Override
					public void onPathEntered(String path) {
						cachePath.setSummary(path);
						Controller.getInstance().setCacheFolder(path);
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
		WifiManager mWifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
		List<WifiConfiguration> mWifiList = mWifiManager.getConfiguredNetworks();

		if (mWifiList == null) return;

		for (WifiConfiguration wifi : mWifiList) {
			// Friendly SSID-Name
			String ssid = wifi.SSID.replaceAll("\"", "");
			// Add PreferenceScreen for each network

			PreferenceScreen pref = getPreferenceManager().createPreferenceScreen(getActivity());
			pref.setPersistent(false);
			pref.setKey("wifiNetwork" + ssid);
			pref.setTitle(ssid);

			Intent intent = new Intent(getActivity(), WifiPreferencesActivity.class);
			intent.putExtra(WifiPreferencesActivity.KEY_SSID, ssid); // TODO: ssid == null?
			pref.setIntent(intent);
			if (WifiConfiguration.Status.CURRENT == wifi.status)
				pref.setSummary(getResources().getString(R.string.ConnectionWifiConnected));
			else pref.setSummary(getResources().getString(R.string.ConnectionWifiNotInRange));
			mWifibasedCategory.addPreference(pref);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		String path = null;
		if (resultCode == Activity.RESULT_OK && data != null) {
			// obtain the filename
			Uri fileUri = data.getData();
			if (fileUri != null) path = fileUri.getPath();
		}
		if (path != null) {
			switch (requestCode) {
				case ACTIVITY_CHOOSE_ATTACHMENT_FOLDER:
					Preference downloadPath = findPreference(Constants.SAVE_ATTACHMENT);
					downloadPath.setSummary(path);
					Controller.getInstance().setSaveAttachmentPath(path);
					break;

				case ACTIVITY_CHOOSE_CACHE_FOLDER:
					Preference cachePath = findPreference(Constants.CACHE_FOLDER);
					cachePath.setSummary(path);
					Controller.getInstance().setCacheFolder(path);
					break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

}
