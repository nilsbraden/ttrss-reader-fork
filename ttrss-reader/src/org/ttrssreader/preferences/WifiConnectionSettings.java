package org.ttrssreader.preferences;

import java.util.List;
import org.ttrssreader.R;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class WifiConnectionSettings extends SherlockPreferenceActivity {
    
    private PreferenceCategory mWifibasedCategory;
    
    private List<WifiConfiguration> mWifiList;
    private WifiManager mWifiManager;
    
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
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
