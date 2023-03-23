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
import android.os.Bundle;

import org.ttrssreader.R;
import org.ttrssreader.model.updaters.IUpdatable;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.utils.AsyncTask;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

public class ReadStateDialog extends MyDialogFragment {

	public static final String DIALOG = "yesnodialog";

	private IUpdatable updater;
	private int titleRes;
	private int msgRes;
	private boolean backAfterUpdate;

	public static ReadStateDialog getInstance(IUpdatable updater) {
		return getInstance(updater, false);
	}

	public static ReadStateDialog getInstance(IUpdatable updater, boolean backAfterUpdate) {
		ReadStateDialog fragment = new ReadStateDialog();
		fragment.updater = updater;
		fragment.titleRes = R.string.Dialog_Title;
		fragment.msgRes = R.string.Dialog_MarkAllRead;
		fragment.backAfterUpdate = backAfterUpdate;
		return fragment;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setIcon(android.R.drawable.ic_dialog_info);

		builder.setTitle(getResources().getString(titleRes));
		builder.setMessage(getResources().getString(msgRes));
		builder.setPositiveButton(getResources().getString(R.string.Yes), (d, which) -> {
			new Updater(getActivity(), updater, backAfterUpdate).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			d.dismiss();
		});
		builder.setNegativeButton(getResources().getString(R.string.No), (d, which) -> d.dismiss());

		return builder.create();
	}

	public void show(FragmentManager manager) {
		super.show(manager, ReadStateDialog.DIALOG);
	}

}
