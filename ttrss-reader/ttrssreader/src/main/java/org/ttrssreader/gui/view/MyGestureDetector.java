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

package org.ttrssreader.gui.view;

import org.ttrssreader.controllers.Controller;
import android.app.ActionBar;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
    
    @SuppressWarnings("unused")
    private static final String TAG = MyGestureDetector.class.getSimpleName();
    
    private ActionBar actionBar;
    private boolean hideActionbar;
    private long lastShow = -1;
    
    public MyGestureDetector(ActionBar actionBar, boolean hideActionbar) {
        this.actionBar = actionBar;
        this.hideActionbar = hideActionbar;
    }
    
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (!hideActionbar)
            return false;
        
        if (Controller.isTablet)
            return false;
        
        if (System.currentTimeMillis() - lastShow < 700)
            return false;
        
        if (Math.abs(distanceX) > Math.abs(distanceY))
            return false;
        
        if (distanceY < -10) {
            actionBar.show();
            lastShow = System.currentTimeMillis();
        } else if (distanceY > 10) {
            actionBar.hide();
            lastShow = System.currentTimeMillis();
        }
        
        return false;
    }
    
}
