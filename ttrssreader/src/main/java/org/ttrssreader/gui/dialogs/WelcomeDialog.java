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

package org.ttrssreader.gui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import org.ttrssreader.R;
import org.ttrssreader.preferences.PreferencesActivity;

import androidx.annotation.NonNull;

public class WelcomeDialog extends MyDialogFragment {

	public static WelcomeDialog getInstance() {
		return new WelcomeDialog();
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setIcon(android.R.drawable.ic_dialog_info);

		builder.setTitle(getResources().getString(R.string.Welcome_Title));
		builder.setMessage(getResources().getString(R.string.Welcome_Message));
		builder.setNeutralButton(getText(R.string.Preferences_Btn), (d, which) -> {
			Intent i = new Intent(getActivity(), PreferencesActivity.class);
			startActivity(i);
			d.dismiss();
		});

		return builder.create();
	}

}
