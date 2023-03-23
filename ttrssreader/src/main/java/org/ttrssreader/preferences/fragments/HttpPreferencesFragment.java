package org.ttrssreader.preferences.fragments;

import android.os.Bundle;
import android.text.InputType;

import org.ttrssreader.R;

import androidx.annotation.Keep;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

@Keep
public class HttpPreferencesFragment extends PreferenceFragmentCompat {

	//	private static final String TAG = HttpPreferencesFragment.class.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.prefs_http);
		WifiPreferencesFragment.tryInitializeWifiPrefs(getContext(), getArguments(), getPreferenceScreen());

		EditTextPreference prefHttpUsername = findPreference("ConnectionHttpUsernamePreference");
		if (prefHttpUsername != null)
			prefHttpUsername.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));

		EditTextPreference prefHttpPassword = findPreference("ConnectionHttpPasswordPreference");
		if (prefHttpPassword != null)
			prefHttpPassword.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
	}
}
