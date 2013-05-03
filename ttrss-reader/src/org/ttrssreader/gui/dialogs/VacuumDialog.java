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
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.utils.AsyncTask;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class VacuumDialog extends DialogFragment {
    
    public static VacuumDialog getInstance() {
        return new VacuumDialog();
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(android.R.drawable.ic_dialog_info);
        
        builder.setTitle(getResources().getString(R.string.Dialog_vacuumTitle));
        builder.setMessage(getResources().getString(R.string.Dialog_vacuumText));
        builder.setPositiveButton(getResources().getString(R.string.Yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface d, final int which) {
                new VacuumTask(ProgressDialog.show(getActivity(),
                        getResources().getString(R.string.Dialog_vacuumTitle),
                        getResources().getString(R.string.Dialog_vacuumProgress), true)).execute();
                d.dismiss();
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.NotNow), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface d, final int which) {
                d.dismiss();
            }
        });
        
        return builder.create();
    }
    
    private class VacuumTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog dialog = null;
        
        public VacuumTask(ProgressDialog dialog) {
            this.dialog = dialog;
        }
        
        protected Void doInBackground(Void... args) {
            try {
                DBHelper.getInstance().vacuum();
            } finally {
                // Reset scheduling-data
                Controller.getInstance().setVacuumDBScheduled(false);
                Controller.getInstance().setLastVacuumDate();
            }
            return null;
        }
        
        @Override
        protected void onPostExecute(Void result) {
            dialog.dismiss();
        }
    }
    
}
