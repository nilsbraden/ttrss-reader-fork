package org.ttrssreader.preferences.fragments;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import org.ttrssreader.R;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

@Keep
public class ConnectionPreferencesFragment extends PreferenceFragmentCompat {

	@SuppressWarnings("unused")
	private static final String TAG = ConnectionPreferencesFragment.class.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.prefs_connection);
		WifiPreferencesFragment.tryInitializeWifiPrefs(getContext(), getArguments(), getPreferenceScreen());

		EditTextPreference prefUsername = findPreference("ConnectionUsernamePreference");
		if (prefUsername != null) {
			prefUsername.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
				@Override
				public void onBindEditText(@NonNull EditText editText) {
					editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
				}
			});
		}

		EditTextPreference prefPassword = findPreference("ConnectionPasswordPreference");
		if (prefPassword != null) {
			prefPassword.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
				@Override
				public void onBindEditText(@NonNull EditText editText) {
					editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
				}
			});
		}
	}
}
