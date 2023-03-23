package org.ttrssreader.preferences.fragments;

import android.os.Bundle;
import android.text.InputType;

import org.ttrssreader.R;

import androidx.annotation.Keep;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

@Keep
public class SSLPreferencesFragment extends PreferenceFragmentCompat {

	//	private static final String TAG = SSLPreferencesFragment.class.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.prefs_ssl);
		WifiPreferencesFragment.tryInitializeWifiPrefs(getContext(), getArguments(), getPreferenceScreen());

		EditTextPreference prefSslPassword = findPreference("ConnectionKeystorePasswordPreference");
		if (prefSslPassword != null)
			prefSslPassword.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
	}
}
