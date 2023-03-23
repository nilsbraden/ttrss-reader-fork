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

import androidx.annotation.NonNull;

public class ImageCaptionDialog extends MyDialogFragment {

	public static final String DIALOG_CAPTION = "image_caption";

	private String caption;

	public static ImageCaptionDialog getInstance(String caption) {
		ImageCaptionDialog fragment = new ImageCaptionDialog();
		fragment.caption = caption;
		return fragment;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setIcon(android.R.drawable.ic_dialog_info);

		builder.setTitle(getResources().getString(R.string.Dialog_imageCaptionTitle));
		builder.setMessage(caption);

		builder.setNeutralButton(getResources().getString(R.string.Close), (d, which) -> d.dismiss());

		return builder.create();
	}

}
