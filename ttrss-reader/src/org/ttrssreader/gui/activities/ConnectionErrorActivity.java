/*
 * Tiny Tiny RSS Reader for Android
 * 
 * Copyright (C) 2009 J. Devauchelle and contributors.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

package org.ttrssreader.gui.activities;

import org.ttrssreader.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ConnectionErrorActivity extends Activity {
	
	public static final String ERROR_MESSAGE = "ERROR_MESSAGE";
	
	private String mErrorMessage;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.connectionerror);
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mErrorMessage = extras.getString(ERROR_MESSAGE);
		} else if (savedInstanceState != null) {
			mErrorMessage = savedInstanceState.getString(ERROR_MESSAGE);
		} else {
			mErrorMessage = "";
		}
		
		TextView errorText = (TextView) this.findViewById(R.id.ConnectionErrorActivity_ErrorMessage);
		errorText.setText(mErrorMessage);
		
		Button prefBtn = (Button) this.findViewById(R.id.ConnectionErrorActivity_ShowPreferencesBtn);
		prefBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				finish();
				openPreferences();
			}
		});
		
		Button closeBtn = (Button) this.findViewById(R.id.ConnectionErrorActivity_CloseBtn);
		closeBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				finish();
			}
		});
	}
	
	private void openPreferences() {
		Intent preferencesActivity = new Intent(this, PreferencesActivity.class);
		startActivity(preferencesActivity);
	}
	
}
