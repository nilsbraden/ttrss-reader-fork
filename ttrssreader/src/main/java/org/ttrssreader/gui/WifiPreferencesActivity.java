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

package org.ttrssreader.gui;


import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.gui.fragments.WifiPreferencesFragment;
import org.ttrssreader.model.HeaderAdapter;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.PostMortemReportExceptionHandler;

import android.app.backup.BackupManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.ListAdapter;

import java.util.List;

@SuppressWarnings("deprecation")
public class WifiPreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	@SuppressWarnings("unused")
	private static final String TAG = WifiPreferencesActivity.class.getSimpleName();

	private PostMortemReportExceptionHandler mDamageReport = new PostMortemReportExceptionHandler(this);

	public static final String KEY_SSID = "SSID";
	private String m_SSID = null;

	private static List<Header> _headers;
	private boolean needResource = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(Controller.getInstance().getTheme());
		super.onCreate(savedInstanceState); // IMPORTANT!
		mDamageReport.initialize();
		setResult(Constants.ACTIVITY_SHOW_PREFERENCES);

		if (needResource) addPreferencesFromResource(R.xml.prefs_main_top);
	}

	@Override
	public void onBuildHeaders(List<Header> headers) {
		_headers = headers;
		if (onIsHidingHeaders()) {
			needResource = true;
		} else {
			m_SSID = getIntent().getStringExtra(KEY_SSID);
			setTitle(getString(R.string.ConnectionWifiPrefSelectionTitle, m_SSID));
			loadHeadersFromResource(R.xml.prefs_headers_wifibased, _headers);
			for (Header header : _headers) {
				if (header.fragmentArguments != null) header.fragmentArguments.putString(KEY_SSID, m_SSID);
			}
		}
	}

	@Override
	public void setListAdapter(ListAdapter adapter) {
		if (adapter == null) {
			super.setListAdapter(null);
		} else {
			super.setListAdapter(new HeaderAdapter(this, _headers));
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		PreferenceManager.getDefaultSharedPreferences(this)
				.unregisterOnSharedPreferenceChangeListener(Controller.getInstance());
	}

	@Override
	protected void onResume() {
		super.onResume();
		PreferenceManager.getDefaultSharedPreferences(this)
				.registerOnSharedPreferenceChangeListener(Controller.getInstance());
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (Controller.getInstance().isPreferencesChanged()) {
			new BackupManager(this).dataChanged();
			Controller.getInstance().setPreferencesChanged(false);
		}
	}

	@Override
	protected void onDestroy() {
		mDamageReport.restoreOriginalHandler();
		mDamageReport = null;
		super.onDestroy();
	}

	@Override
	protected boolean isValidFragment(String fragmentName) {
		return WifiPreferencesFragment.class.getName().equals(fragmentName);
	}

	@Override
	public void switchToHeader(Header header) {
		if (header.fragment != null) {
			super.switchToHeader(header);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (m_SSID == null) return;

		if (key.equals(m_SSID + Constants.ENABLE_WIFI_BASED_SUFFIX)) {
			// TODO: WTF?
		}
	}

}
