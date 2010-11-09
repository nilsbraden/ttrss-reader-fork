/*
 * ttrss-reader-fork for Android
 * Copyright (C) 2010 N. Braden.
 * Copyright (C) 2009-2010 J. Devauchelle.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
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

public class ErrorActivity extends Activity {

    public static final int ACTIVITY_SHOW_ERROR = 42;
    public static final String ERROR_MESSAGE = "ERROR_MESSAGE";
    
    private String mErrorMessage;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.error);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mErrorMessage = extras.getString(ERROR_MESSAGE);
        } else if (savedInstanceState != null) {
            mErrorMessage = savedInstanceState.getString(ERROR_MESSAGE);
        }
        
        TextView errorText = (TextView) this.findViewById(R.id.ErrorActivity_ErrorMessage);
        errorText.setText(mErrorMessage);
        
        Button prefBtn = (Button) this.findViewById(R.id.Preferences_Btn);
        prefBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                finish();
                openPreferences();
            }
        });
        
        Button closeBtn = (Button) this.findViewById(R.id.ErrorActivity_CloseBtn);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                closeButtonPressed();
            }
        });
    }
    
    private void closeButtonPressed() {
        setResult(ACTIVITY_SHOW_ERROR);
        finish();
    }
    
    private void openPreferences() {
        startActivity(new Intent(this, PreferencesActivity.class));
    }
    
}
