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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Copied and modified for my purpose, originally developed for K-9 Mail. Source:
 * https://github.com/k9mail/k-9/blob/master/src/com/fsck/k9/view/MessageHeader.java
 * 
 * @author https://code.google.com/p/k9mail/
 * 
 */
public class ArticleView extends RelativeLayout {
    
    private RelativeLayout centralView;
    private WebView webView;
    private LinearLayout buttonView;
    private Button buttonPrev;
    private Button buttonNext;
    private TextView swypeView;
    
    private boolean useSwype;
    private boolean useButtons;
    private boolean leftHanded;
    private Context context;
    
    public ArticleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setBackgroundColor(Color.WHITE);
        this.context = context;
    }
    
    private void initializeLayout() {
        centralView = (RelativeLayout) findViewById(R.id.centralView);
        webView = (WebView) findViewById(R.id.webView);
        buttonView = (LinearLayout) findViewById(R.id.buttonView);
        buttonPrev = (Button) findViewById(R.id.buttonPrev);
        buttonNext = (Button) findViewById(R.id.buttonNext);
        swypeView = (TextView) findViewById(R.id.swypeView);
        
        // First check for swipe-option, this overrides the buttons-option
        if (useSwype) {
            
            // Load Swipe-Text-Field
            swypeView.setVisibility(TextView.INVISIBLE);
            swypeView.setPadding(16, Controller.padding, 16, Controller.padding);
            swypeView.setHeight(Controller.swipeHeight);
            
            // Set Buttons invisible
            buttonView.setVisibility(LinearLayout.INVISIBLE);
            buttonView.setEnabled(false);
            this.removeView(buttonView);
            
            if (leftHanded) {
                // TODO: Bring swypeView to left side
            }
            
        } else if (useButtons) {
            
            // Load Swipe-Text-Field and disable it
            centralView.removeView(swypeView);
            
            buttonView.bringToFront();
            buttonNext.bringToFront();
            buttonPrev.bringToFront();
            
            if (leftHanded) {
                // TODO: Bring buttons to left side
            }
            
            // Disable webView zoom-controls
            webView.getSettings().setBuiltInZoomControls(false);
        }
        
        // Recalculate values
        this.recomputeViewAttributes(centralView);
    }
    
    public void populate(boolean useSwype, boolean useButtons, boolean leftHanded) {
        this.useSwype = useSwype;
        this.useButtons = useButtons;
        this.leftHanded = leftHanded;
        this.initializeLayout();
    }
    
}
