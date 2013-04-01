/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
 * Copyright (C) The Developers from K-9 Mail (https://code.google.com/p/k9mail/)
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

package org.ttrssreader.gui.view;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

/**
 * Copied and modified for my purpose, originally developed for K-9 Mail. Source:
 * https://github.com/k9mail/k-9/blob/master/src/com/fsck/k9/view/MessageHeader.java
 * 
 * @author https://code.google.com/p/k9mail/
 * 
 */
public class ArticleView extends RelativeLayout {
    
    private RelativeLayout centralView;
    private LinearLayout buttonView;
    
    public ArticleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(Color.WHITE);
    }
    
    private void initializeLayout(final WebView webView) {
        centralView = (RelativeLayout) findViewById(R.id.centralView);
        buttonView = (LinearLayout) findViewById(R.id.buttonView);
        
        // First check for swipe-option, this overrides the buttons-option
        if (Controller.getInstance().useSwipe()) {
            
            // remove Buttons
            removeView(buttonView);
        } else if (Controller.getInstance().useButtons()) {
            //
        } else {
            // Both disabled, remove everything
            removeView(buttonView);
        }
        
        // Recalculate values
        recomputeViewAttributes(centralView);
    }
    
    public void populate(final WebView webView) {
        initializeLayout(webView);
    }
    
}
