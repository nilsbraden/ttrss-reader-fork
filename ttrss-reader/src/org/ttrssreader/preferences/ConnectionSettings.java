package org.ttrssreader.preferences;

import java.util.Map;
import org.ttrssreader.R;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

@SuppressWarnings("deprecation")
public class ConnectionSettings extends PreferenceActivity {
    
    protected static final String TAG = ConnectionSettings.class.getSimpleName();
    
    private static final String KEY_CONNECTION_CATEGORY = "connectionCategory";
    public static final String KEY_SSID = "SSID";
    
    private static final int MENU_DELETE = 1;
    
    private String sSID;
    private PreferenceCategory category;
    
    private SharedPreferences prefs = null;
    private Map<String, ?> prefsCache = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_main_top);
        
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        
        category = (PreferenceCategory) preferenceScreen.findPreference(KEY_CONNECTION_CATEGORY);
        
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefsCache = prefs.getAll();
        
        if (getIntent().getStringExtra(KEY_SSID) != null) {
            // WiFi-Based Settings
            sSID = getIntent().getStringExtra(KEY_SSID);
            createDynamicSettings(sSID);
        } else {
            // Default settings
            createDynamicSettings("");
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_DELETE, Menu.NONE, R.string.ConnectionWifiDeletePref);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item != null && item.getItemId() == MENU_DELETE) {
            Editor edit = prefs.edit();
            
            for (int i = 0; i < category.getPreferenceCount(); i++) {
                Preference pref = category.getPreference(i);
                
                if (prefs.contains(pref.getKey())) {
                    edit.remove(pref.getKey());
                    
                    // Remove and add again to reinitialize default values
                    category.removePreference(pref);
                    category.addPreference(pref);
                }
            }
            
            edit.commit();
            prefsCache = prefs.getAll();
        }
        return super.onMenuItemSelected(featureId, item);
    }
    
    private void createDynamicSettings(String keyPrefix) {
        
        category.setTitle(category.getTitle() + " ( Network: " + keyPrefix + " )");
        
        Log.d(TAG, "Adding WifiPreferences...");
        for (int i = 0; i < category.getPreferenceCount(); i++) {
            Preference pref = category.getPreference(i);
            
            String oldKey = pref.getKey();
            String newKey = keyPrefix + oldKey;
            
            pref.setKey(newKey);
            
            Object defaultValue = null;
            if (prefsCache.containsKey(newKey))
                defaultValue = prefsCache.get(newKey);
            
            pref.setDefaultValue(defaultValue);
            
            // Remove and add again to reinitialize default values
            category.removePreference(pref);
            category.addPreference(pref);
            Log.d(TAG, String.format("  oldKey: \"%s\" newKey: \"%s\"", oldKey, newKey));
        }
        
        onContentChanged();
    }
    
}
