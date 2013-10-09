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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.ttrssreader.R;
import org.ttrssreader.utils.TopExceptionHandler;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class CrashreportDialog extends DialogFragment {
    
    public static CrashreportDialog getInstance() {
        return new CrashreportDialog();
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(android.R.drawable.ic_dialog_info);
        
        builder.setTitle(getResources().getString(R.string.ErrorActivity_Title));
        builder.setMessage(getResources().getString(R.string.Check_Crash));
        builder.setPositiveButton(getResources().getString(R.string.Yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface d, final int which) {
                sendReport();
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.No), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface d, final int which) {
                getActivity().deleteFile(TopExceptionHandler.FILE);
                d.dismiss();
            }
        });
        
        return builder.create();
    }
    
    public void sendReport() {
        String line = "";
        StringBuilder sb = new StringBuilder();
        
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(getActivity().openFileInput(TopExceptionHandler.FILE)));
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (Exception ioe) {
            // Catch everything, we dont want more exceptions while we handle the last one...
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }
        
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        String subject = "Error report";
        String mail = getResources().getString(R.string.About_mail);
        String body = "Please mail this to " + mail + ": " + "\n\n" + sb.toString() + "\n\n";
        
        sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { mail });
        sendIntent.putExtra(Intent.EXTRA_TEXT, body);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        sendIntent.setType("message/rfc822");
        
        startActivity(Intent.createChooser(sendIntent, "Title:"));
        
        getActivity().deleteFile(TopExceptionHandler.FILE);
    }
    
}
