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
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
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
    private TextView swipeView;
    
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
        swipeView = (TextView) findViewById(R.id.swipeView);
        
        // First check for swipe-option, this overrides the buttons-option
        if (Controller.getInstance().useSwipe()) {
            
            if (Controller.getInstance().leftHanded() && Controller.landscape) {
                // Try to move swipe-area to left side...

                // First: Remove the view
                centralView.removeView(swipeView);
                
                // calculate width of the swipe-area in pixels, its the number of pixels of the value 45dip
                int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) 45, getResources()
                        .getDisplayMetrics());
                
                // Create new layout-parameters which align this view left in the parent view
                LayoutParams params = new LayoutParams(width, LayoutParams.FILL_PARENT);
                params.addRule(ALIGN_LEFT, webView.getId());
                
                // Add the view again
                centralView.addView(swipeView, params);
                
                // Recalculate values
                recomputeViewAttributes(swipeView);
            }
            
            swipeView.setVisibility(TextView.INVISIBLE);
            swipeView.setPadding(16, Controller.padding, 16, Controller.padding);
            
            if (Controller.landscape)
                swipeView.setHeight(Controller.swipeAreaHeight);
            else
                swipeView.setWidth(Controller.swipeAreaWidth);
            
            // remove Buttons
            this.removeView(buttonView);
            
        } else if (Controller.getInstance().useButtons()) {
            
            if (Controller.getInstance().leftHanded() && Controller.landscape) {
                // Try to move buttons to left side...
                
                // First: Remove the view
                this.removeView(buttonView);
                // calculate width of the swipe-area in pixels, its the number of pixels of the value 45dip
                int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) 45, getResources()
                        .getDisplayMetrics());
                // Create new layout-parameters which align this view left in the parent view
                LayoutParams params = new LayoutParams(width, LayoutParams.FILL_PARENT);
                params.addRule(ALIGN_PARENT_LEFT, TRUE);
                // Add the view again
                this.addView(buttonView, params);
                
                // Webview and its container has to be moved to the right side to make room for the buttons.
                // Not necessary for the swipe area since this is an overlay to the webview.
                this.removeView(centralView);
                LayoutParams centralViewParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                params.addRule(RIGHT_OF, buttonView.getId());
                this.addView(centralView, centralViewParams);
                
                // Recalculate values
                recomputeViewAttributes(buttonView);
                recomputeViewAttributes(centralView);
                getParent().recomputeViewAttributes(this);
                // TODO: Buttons werden nicht gezeichnet, warum??
            }
            
            // Disable webView zoom-controls
            webView.getSettings().setBuiltInZoomControls(false);
            
            // Remove Swipe-Area
            centralView.removeView(swipeView);
        }
        
        // Recalculate values
        recomputeViewAttributes(centralView);
    }
    
    public void populate() {
        this.initializeLayout();
    }
    
}
