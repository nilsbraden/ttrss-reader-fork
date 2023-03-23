package org.ttrssreader.preferences.fragments;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.preferences.FileBrowserHelper;

import java.io.File;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

@Keep
public class SystemPreferencesFragment extends PreferenceFragmentCompat {

	//	private static final String TAG = SystemPreferencesFragment.class.getSimpleName();
	private static final int ACTIVITY_CHOOSE_ATTACHMENT_FOLDER = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.prefs_system);
		initializePreference();
	}

	private void initializePreference() {
		final PreferenceFragmentCompat fragment = this;
		final Preference downloadPath = findPreference(Constants.SAVE_ATTACHMENT);

		if (downloadPath != null) {
			downloadPath.setSummary(Controller.getInstance().saveAttachmentPath());
			downloadPath.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(@NonNull Preference preference) {
					FileBrowserHelper.getInstance().showFileBrowserActivity(fragment, new File(Controller.getInstance().saveAttachmentPath()), ACTIVITY_CHOOSE_ATTACHMENT_FOLDER, callbackDownloadPath);
					return true;
				}

				// Fail-Safe Dialog for when there is no filebrowser installed:
				final FileBrowserHelper.FileBrowserFailOverCallback callbackDownloadPath = new FileBrowserHelper.FileBrowserFailOverCallback() {
					@Override
					public void onPathEntered(String path) {
						downloadPath.setSummary(path);
						Controller.getInstance().setSaveAttachmentGeneric(path);
					}

					@Override
					public void onCancel() {
					}
				};
			});
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK || requestCode != ACTIVITY_CHOOSE_ATTACHMENT_FOLDER || data == null) {
			super.onActivityResult(requestCode, resultCode, data);
			return;
		}

		// obtain the filename
		Uri fileUri = data.getData();
		if (fileUri != null) {
			Preference downloadPath = findPreference(Constants.SAVE_ATTACHMENT);
			if (downloadPath != null)
				downloadPath.setSummary(fileUri.getPath());

			// Use takePersistableUriPermission
			if (Build.VERSION.SDK_INT >= 100000) { //Build.VERSION_CODES.LOLLIPOP) { // TODO Use proper VERSION_CODE
				Context ctx = getContext();
				if (ctx != null) {
					ContentResolver resolver = getContext().getContentResolver();
					int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
					resolver.takePersistableUriPermission(fileUri, flags);
				}

				Controller.getInstance().setSaveAttachmentGeneric(fileUri.toString());
			} else {
				Controller.getInstance().setSaveAttachmentGeneric(fileUri.getPath());
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}
}
