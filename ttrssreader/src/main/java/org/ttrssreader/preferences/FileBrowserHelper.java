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

package org.ttrssreader.preferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.InputType;
import android.widget.EditText;

import org.ttrssreader.R;

import java.io.File;

import androidx.fragment.app.Fragment;

/**
 * Copied from https://code.google.com/p/k9mail/
 */
public class FileBrowserHelper {

	private static FileBrowserHelper instance;

	/**
	 * callbackDownloadPath class to provide the result of the fallback textedit path dialog
	 */
	public interface FileBrowserFailOverCallback {
		/**
		 * the user has entered a path
		 *
		 * @param path the path as String
		 */
		void onPathEntered(String path);

		/**
		 * the user has cancel the inputtext dialog
		 */
		void onCancel();
	}

	// Singleton
	private FileBrowserHelper() {
	}

	public synchronized static FileBrowserHelper getInstance() {
		if (instance == null) {
			instance = new FileBrowserHelper();
		}
		return instance;
	}

	/**
	 * tries to open known filebrowsers.
	 * If no filebrowser is found and fallback textdialog is shown
	 *
	 * @param fragment    the context as activity
	 * @param startPath   : the default value, where the filebrowser will start.
	 *                    if startPath = null => the default path is used
	 * @param requestcode : the int you will get as requestcode in onActivityResult
	 *                    (only used if there is a filebrowser installed)
	 * @param callback    : the callbackDownloadPath (only used when no filebrowser is installed.
	 *                    if a filebrowser is installed => override the onActivtyResult Method
	 *                    false: a fallback textinput has been shown. The Result will be sent with the callbackDownloadPath method
	 */
	public void showFileBrowserActivity(Fragment fragment, File startPath, int requestcode, FileBrowserFailOverCallback callback) {
		boolean success = false;

		Intent intent;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
			intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
			intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

			// Reset startPath to avoid people trying to change Uri:
			startPath = Environment.getStorageDirectory();
		} else {
			intent = new Intent(Intent.ACTION_PICK);
			intent.setData(Uri.parse("folder://" + startPath.getPath()));
		}

		try {
			fragment.startActivityForResult(intent, requestcode);
			success = true;
		} catch (Exception e) {
			// Empty, try next intent
		}

		if (!success) {
			// No Filebrowser is installed => show a fallback textdialog
			showPathTextInput(fragment.getActivity(), startPath, callback);
		}
	}

	private void showPathTextInput(final Activity c, final File startPath, final FileBrowserFailOverCallback callback) {
		AlertDialog.Builder alert = new AlertDialog.Builder(c);

		alert.setTitle(c.getString(R.string.Utils_FileSaveTitle));
		alert.setMessage(c.getString(R.string.Utils_FileSaveMessage));
		final EditText input = new EditText(c);
		input.setInputType(InputType.TYPE_CLASS_TEXT);
		if (startPath != null)
			input.setText(startPath.toString());
		alert.setView(input);

		alert.setPositiveButton(c.getString(R.string.Utils_OkayAction), (dialog, whichButton) -> {
			String path = input.getText().toString();
			callback.onPathEntered(path);
		});

		alert.setNegativeButton(c.getString(R.string.Utils_CancelAction), (dialog, whichButton) -> callback.onCancel());

		alert.show();
	}
}
