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

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.gui.dialogs.StringDialog;
import org.ttrssreader.gui.dialogs.StringResultListener;
import org.ttrssreader.preferences.PreferencesActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

@Keep
public class WifiListPreferencesFragment extends PreferenceFragmentCompat implements StringResultListener, EasyPermissions.PermissionCallbacks {

	private static final String TAG = WifiListPreferencesFragment.class.getSimpleName();

	private static final String KEY_ADD_MANUAL = "WifibasedAddManual";
	private static final int RC_LOCATION = 12;

	private boolean dontAskAgain = false;
	private final Map<String, Boolean> mNewlyConfiguredWifiNetworks = new HashMap<>();
	private Map<String, Boolean> mConfiguredWifiNetworks = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.prefs_wifibased);
	}

	@Override
	public void onResume() {
		// Add all existing configurations:
		addConfiguredNetworks();

		// If available use the last wifi scan results to make it easier to configure networks:
		addScanResults();

		super.onResume();
	}

	private void addConfiguredNetworks() {
		Activity activity = getActivity();
		if (activity == null)
			return;

		PreferenceCategory mWifibasedConfigured = findPreference("wifibasedCategoryConfigured");
		if (mWifibasedConfigured == null)
			return;

		mConfiguredWifiNetworks = Controller.getInstance().getConfiguredWifiNetworks();

		// Reset displayed list:
		mWifibasedConfigured.removeAll();
		// Add all configured networks again:
		addPreferences(activity, mNewlyConfiguredWifiNetworks, mWifibasedConfigured);
		addPreferences(activity, mConfiguredWifiNetworks, mWifibasedConfigured);
	}

	@AfterPermissionGranted(RC_LOCATION)
	public void addScanResults() {
		Activity activity = getActivity();
		if (activity == null)
			return;

		String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION};
		if (!EasyPermissions.hasPermissions(activity, perms)) {
			// Do not have permissions
			if (dontAskAgain)
				return;

			// Otherwise request permissions now
			EasyPermissions.requestPermissions(this, getString(R.string.ConnectionWifiRequestPermissions), RC_LOCATION, perms);
			return;
		}

		PreferenceCategory mWifibasedScanned = findPreference("wifibasedCategoryScanned");
		if (mWifibasedScanned == null)
			return;

		WifiManager mWifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		if (mWifiManager == null)
			return;

		Map<String, Boolean> networks = new HashMap<>();
		if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			List<ScanResult> results = mWifiManager.getScanResults();
			for (ScanResult result : results) {
				String ssid = cleanupSsid(result.SSID);
				if (mConfiguredWifiNetworks != null && mConfiguredWifiNetworks.containsKey(ssid))
					continue;

				networks.put(ssid, false);
			}
		}

		// Reset displayed list:
		mWifibasedScanned.removeAll();
		// Add all configured networks again:
		addPreferences(activity, networks, mWifibasedScanned);
	}

	private void addPreferences(@NonNull Activity activity, Map<String, Boolean> networks, PreferenceCategory cat) {
		for (String key : networks.keySet()) {
			// Friendly SSID-Name
			String ssid = cleanupSsid(key);

			// Add PreferenceScreen for each network
			PreferenceScreen pref = getPreferenceManager().createPreferenceScreen(activity);
			pref.setPersistent(false);
			pref.setKey("wifiNetwork" + ssid);
			pref.setTitle(ssid);

			pref.setFragment("org.ttrssreader.preferences.fragments.WifiPreferencesFragment");
			pref.getExtras().putString(PreferencesActivity.KEY_SSID, ssid);

			Boolean status = networks.get(key);
			if (status != null && status)
				pref.setSummary(getResources().getString(R.string.ConnectionWifiEnabled));
			else
				pref.setSummary(getResources().getString(R.string.ConnectionWifiDisabled));
			cat.addPreference(pref);
		}
	}

	private static String cleanupSsid(String ssid) {
		return ssid.replaceAll("\"", "");
	}

	@Override
	public boolean onPreferenceTreeClick(Preference preference) {
		String key = preference.getKey();
		if (KEY_ADD_MANUAL.equals(key)) {
			addManualPref(key);
		}
		return super.onPreferenceTreeClick(preference);
	}

	private void addManualPref(String key) {
		if (getActivity() == null)
			return;

		final FragmentManager fm = getActivity().getSupportFragmentManager();
		int titleRes = R.string.ConnectionWifiAddManuallyTitle;
		int msgRes = R.string.ConnectionWifiAddManuallyMessage;
		StringDialog dialog = StringDialog.getInstance(this, titleRes, msgRes);
		dialog.show(fm, key);
	}

	@Override
	public void onClick(int which, String data) {
		if (getActivity() == null)
			return;

		if (which == Dialog.BUTTON_POSITIVE) {
			mNewlyConfiguredWifiNetworks.put(data, false);
			// Refresh displayed list:
			addConfiguredNetworks();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		// Forward results to EasyPermissions
		EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
	}


	@Override
	public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

	}

	@Override
	public void onPermissionsDenied(int requestCode, List<String> perms) {
		Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

		// (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
		// This will display a dialog directing them to enable the permission in app settings.
		if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
			dontAskAgain = true;
		}
	}

}
