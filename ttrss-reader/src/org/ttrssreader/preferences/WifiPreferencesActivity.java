/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 Nils Braden
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

package org.ttrssreader.preferences;

import java.util.List;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.model.HeaderAdapter;
import org.ttrssreader.utils.PostMortemReportExceptionHandler;
import android.app.backup.BackupManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

@SuppressWarnings("deprecation")
public class WifiPreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    
    protected static final String TAG = WifiPreferencesActivity.class.getSimpleName();
    
    protected PostMortemReportExceptionHandler mDamageReport = new PostMortemReportExceptionHandler(this);
    
    private static final String PREFS_MAIN_TOP = "prefs_main_top";
    private static final String PREFS_HTTP = "prefs_http";
    private static final String PREFS_SSL = "prefs_ssl";
    
    public static final int ACTIVITY_SHOW_PREFERENCES = 43;
    public static final String KEY_SSID = "SSID";
    private String m_SSID = null;
    
    private static List<Header> _headers;
    private boolean needResource = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Controller.getInstance().getTheme());
        super.onCreate(savedInstanceState); // IMPORTANT!
        mDamageReport.initialize();
        setResult(ACTIVITY_SHOW_PREFERENCES);
        
        if (needResource)
            addPreferencesFromResource(R.xml.prefs_main_top);
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
                if (header.fragmentArguments != null)
                    header.fragmentArguments.putString(KEY_SSID, m_SSID);
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
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(
                Controller.getInstance());
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(
                Controller.getInstance());
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
    
    public static class WifiPreferencesFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
            
            String ssid = getArguments().getString(KEY_SSID);
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
    
    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (WifiPreferencesFragment.class.getName().equals(fragmentName))
            return true;
        return false;
    }
    
    @Override
    public void switchToHeader(Header header) {
        if (header.fragment != null) {
            super.switchToHeader(header);
        }
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (m_SSID == null)
            return;
        
        if (key.equals(m_SSID + Constants.ENABLE_WIFI_BASED_SUFFIX)) {
            
        }
    }
    
}
