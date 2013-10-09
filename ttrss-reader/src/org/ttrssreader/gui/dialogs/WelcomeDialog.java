/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
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

package org.ttrssreader.gui.dialogs;

import org.ttrssreader.R;
import org.ttrssreader.gui.PreferencesActivity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class WelcomeDialog extends DialogFragment {
    
    public static WelcomeDialog getInstance() {
        return new WelcomeDialog();
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(android.R.drawable.ic_dialog_info);
        
        builder.setTitle(getResources().getString(R.string.Welcome_Title));
        builder.setMessage(getResources().getString(R.string.Welcome_Message));
        builder.setNeutralButton((String) getText(R.string.Preferences_Btn), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface d, final int which) {
                Intent i = new Intent(getActivity(), PreferencesActivity.class);
                startActivity(i);
                d.dismiss();
            }
        });
        
        return builder.create();
    }
    
}
