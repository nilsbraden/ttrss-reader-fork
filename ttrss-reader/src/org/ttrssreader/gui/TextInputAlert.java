/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 Nils Braden
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

package org.ttrssreader.gui;

import org.ttrssreader.R;
import org.ttrssreader.gui.interfaces.TextInputAlertCallback;
import org.ttrssreader.model.pojos.Article;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;

public class TextInputAlert {
    
    protected static final String TAG = TextInputAlert.class.getSimpleName();
    
    private Article article;
    private TextInputAlertCallback callback;
    
    public TextInputAlert(TextInputAlertCallback callback, Article article) {
        this.callback = callback;
        this.article = article;
    }
    
    public void show(Context context) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        
        alert.setTitle(context.getString(R.string.Commons_MarkPublishNote));
        
        final EditText input = new EditText(context);
        input.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        input.setMinLines(3);
        input.setMaxLines(10);
        alert.setView(input);
        
        alert.setPositiveButton(context.getString(R.string.Utils_OkayAction), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                callback.onPublishNoteResult(article, value);
            }
        });
        
        alert.setNegativeButton(context.getString(R.string.Utils_CancelAction), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        
        alert.show();
    }
}
