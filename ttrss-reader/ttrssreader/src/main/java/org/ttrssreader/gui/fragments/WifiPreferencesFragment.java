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
import org.ttrssreader.gui.WifiPreferencesActivity;
import org.ttrssreader.preferences.Constants;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class WifiPreferencesFragment extends PreferenceFragment {

    private static final String TAG = WifiPreferencesFragment.class.getSimpleName();

    private static final String PREFS_MAIN_TOP = "prefs_main_top";
    private static final String PREFS_HTTP = "prefs_http";
    private static final String PREFS_SSL = "prefs_ssl";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        String ssid = getArguments().getString(WifiPreferencesActivity.KEY_SSID);
        String cat = getArguments().getString("cat");

        if (PREFS_MAIN_TOP.equals(cat))
            addPreferencesFromResource(R.xml.prefs_main_top);
        if (PREFS_HTTP.equals(cat))
            addPreferencesFromResource(R.xml.prefs_http);
        if (PREFS_SSL.equals(cat))
            addPreferencesFromResource(R.xml.prefs_ssl);

        initDynamicConnectionPrefs(ssid, PREFS_MAIN_TOP.equals(cat));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private void initDynamicConnectionPrefs(String ssid, boolean addEnableWifiPref) {
        if (getPreferenceScreen().getPreferenceCount() != 1)
            return;
        if (!(getPreferenceScreen().getPreference(0) instanceof PreferenceCategory))
            return;

        PreferenceCategory category = (PreferenceCategory) getPreferenceScreen().getPreference(0);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        for (int i = 0; i < category.getPreferenceCount(); i++) {
            Preference pref = category.getPreference(i);

            String oldKey = pref.getKey();
            String newKey = ssid + oldKey;

            pref.setKey(newKey);

            Object defaultValue = null;
            if (prefs.getAll().containsKey(newKey))
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
            // TODO
            String key = ssid + Constants.ENABLE_WIFI_BASED_SUFFIX;
            CheckBoxPreference enableWifiPref = new CheckBoxPreference(getActivity());
            enableWifiPref.setKey(key);
            enableWifiPref.setTitle(R.string.ConnectionWifiPrefEnableTitle);
            enableWifiPref.setSummaryOn(R.string.ConnectionWifiPrefEnabledSummary);
            enableWifiPref.setSummaryOff(R.string.ConnectionWifiPrefDisbledSummary);

            Object defaultValue = null;
            if (prefs.getAll().containsKey(key))
                defaultValue = prefs.getAll().get(key);
            enableWifiPref.setDefaultValue(defaultValue);

            category.addPreference(enableWifiPref);
        }
    }
}
