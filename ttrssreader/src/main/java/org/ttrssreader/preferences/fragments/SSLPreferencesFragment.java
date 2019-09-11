package org.ttrssreader.preferences.fragments;

import android.os.Bundle;

import org.ttrssreader.R;

import androidx.annotation.Keep;
import androidx.preference.PreferenceFragmentCompat;

@Keep
public class SSLPreferencesFragment extends PreferenceFragmentCompat {

	@SuppressWarnings("unused")
	private static final String TAG = SSLPreferencesFragment.class.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.prefs_ssl);
		WifiPreferencesFragment.tryInitializeWifiPrefs(getContext(), getArguments(), getPreferenceScreen());
	}
}
