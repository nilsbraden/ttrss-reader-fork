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

package org.ttrssreader.gui;

import android.app.AlertDialog;
import android.content.Context;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;

import org.ttrssreader.R;
import org.ttrssreader.gui.interfaces.TextInputAlertCallback;
import org.ttrssreader.model.pojos.Article;

public class TextInputAlert {

	//	private static final String TAG = TextInputAlert.class.getSimpleName();

	private final Article article;
	private final TextInputAlertCallback callback;

	public TextInputAlert(TextInputAlertCallback callback, Article article) {
		this.callback = callback;
		this.article = article;
	}

	public void show(Context context) {
		AlertDialog.Builder alert = new AlertDialog.Builder(context);

		alert.setTitle(context.getString(R.string.Commons_MarkNote));

		final EditText input = new EditText(context);
		input.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		input.setMinLines(3);
		input.setMaxLines(10);
		input.setText(article.note);
		alert.setView(input);

		alert.setPositiveButton(context.getString(R.string.Utils_OkayAction), (dialog, whichButton) -> {
			String value = input.getText().toString();
			callback.onAddNoteResult(article, value);
		});

		alert.setNegativeButton(context.getString(R.string.Utils_CancelAction), (dialog, whichButton) -> dialog.dismiss());

		alert.show();
	}
}
