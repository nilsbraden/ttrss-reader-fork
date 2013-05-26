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
import org.ttrssreader.model.pojos.Article;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.method.ScrollingMovementMethod;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class ArticleHeaderDialog extends DialogFragment {
    
    public static final String DIALOG_HEADER = "header";
    
    private Article article;
    
    public static ArticleHeaderDialog newInstance(Article article) {
        ArticleHeaderDialog fragment = new ArticleHeaderDialog();
        fragment.article = article;
        return fragment;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle args) {
        // AboutDialog benutzt als Schriftfarbe automatisch die invertierte Hintergrundfarbe
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(),
                R.style.AboutDialog));
        
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.articleheaderdisplay, null);
        TextView title = (TextView) view.findViewById(R.id.articleheaderdisplay_title);
        title.setTextAppearance(getActivity(), android.R.style.TextAppearance_Medium);
        title.setMovementMethod(new ScrollingMovementMethod());
        
        title.setText(article.title);
        
        builder.setView(view).setPositiveButton(R.string.Close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });
        
        return builder.create();
    }
    
}
