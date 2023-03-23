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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.preferences.PreferencesActivity;
import org.ttrssreader.utils.PostMortemReportExceptionHandler;

public class ErrorActivity extends Activity {

	//	private static final String TAG = ErrorActivity.class.getSimpleName();

	private PostMortemReportExceptionHandler mDamageReport = new PostMortemReportExceptionHandler(this);

	public static final int ACTIVITY_SHOW_ERROR = 42;
	static final int ACTIVITY_EXIT = 40;
	public static final String ERROR_MESSAGE = "ERROR_MESSAGE";

	private String message;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(Controller.getInstance().getThemeResource());
		Controller.getInstance().initializeThemeMode();
		mDamageReport.initialize();

		setContentView(R.layout.error);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			message = extras.getString(ERROR_MESSAGE);
		} else if (savedInstanceState != null) {
			message = savedInstanceState.getString(ERROR_MESSAGE);
		}

		TextView errorText = (TextView) this.findViewById(R.id.ErrorActivity_ErrorMessage);
		errorText.setText(message);

		Button prefBtn = (Button) this.findViewById(R.id.Preferences_Btn);
		prefBtn.setOnClickListener(view -> {
			finish();
			openPreferences();
		});

		Button exitBtn = (Button) this.findViewById(R.id.ErrorActivity_ExitBtn);
		exitBtn.setOnClickListener(view -> exitButtonPressed());

		Button closeBtn = (Button) this.findViewById(R.id.ErrorActivity_CloseBtn);
		closeBtn.setOnClickListener(view -> closeButtonPressed());
	}

	@Override
	protected void onDestroy() {
		mDamageReport.restoreOriginalHandler();
		mDamageReport = null;
		super.onDestroy();
	}

	private void exitButtonPressed() {
		setResult(ACTIVITY_EXIT);
		finish();
	}

	private void closeButtonPressed() {
		setResult(ACTIVITY_SHOW_ERROR);
		finish();
	}

	private void openPreferences() {
		startActivity(new Intent(this, PreferencesActivity.class));
	}

}
