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
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

public class WifiConnectionSettings extends PreferenceActivity {
    
    private PreferenceCategory mWifibasedCategory;
    
    private List<WifiConfiguration> mWifiList;
    private WifiManager mWifiManager;
    
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Controller.getInstance().getTheme());
        super.onCreate(savedInstanceState); // IMPORTANT!
        
        addPreferencesFromResource(R.xml.prefs_wifibased);
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        mWifibasedCategory = (PreferenceCategory) preferenceScreen.findPreference("wifibasedCategory");
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        
        mWifiList = mWifiManager.getConfiguredNetworks();
        if (mWifiList == null)
            return;
        
        for (WifiConfiguration wifi : mWifiList) {
            // Friendly SSID-Name
            String ssid = wifi.SSID.replaceAll("\"", "");
            // Add PreferenceScreen for each network
            PreferenceScreen pref = getPreferenceManager().createPreferenceScreen(this);
            pref.setPersistent(false);
            pref.setKey("wifiNetwork" + ssid);
            pref.setTitle(ssid);
            
            Intent intent = new Intent(this, ConnectionSettings.class);
            intent.putExtra(ConnectionSettings.KEY_SSID, ssid);
            pref.setIntent(intent);
            if (WifiConfiguration.Status.CURRENT == wifi.status)
                pref.setSummary(getResources().getString(R.string.ConnectionWifiConnected));
            else
                pref.setSummary(getResources().getString(R.string.ConnectionWifiNotInRange));
            mWifibasedCategory.addPreference(pref);
        }
    }
    
}
