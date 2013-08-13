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

package org.ttrssreader.gui.view;

import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import com.actionbarsherlock.app.ActionBar;

public class MyGestureDetector implements OnGestureListener {
    
    private ActionBar actionBar;
    private long lastShow = -1;
    
    public MyGestureDetector(ActionBar actionBar) {
        this.actionBar = actionBar;
    }
    
    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }
    
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }
    
    @Override
    public void onLongPress(MotionEvent e) {
    }
    
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (System.currentTimeMillis() - lastShow < 700)
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
    
    @Override
    public void onShowPress(MotionEvent e) {
    }
    
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }
    
}
