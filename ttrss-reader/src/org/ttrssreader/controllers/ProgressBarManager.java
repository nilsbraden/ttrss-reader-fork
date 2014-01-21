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

package org.ttrssreader.controllers;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class ProgressBarManager {
    
    protected static final String TAG = ProgressBarManager.class.getSimpleName();
    
    private static ProgressBarManager instance = null;
    private int progressIndeterminateCount = 0;
    
    // Singleton
    private ProgressBarManager() {
    }
    
    public static ProgressBarManager getInstance() {
        if (instance == null) {
            synchronized (ProgressBarManager.class) {
                if (instance == null) {
                    instance = new ProgressBarManager();
                }
            }
        }
        return instance;
    }
    
    public void addProgress(SherlockFragmentActivity activity) {
        progressIndeterminateCount++;
        setIndeterminateVisibility(activity);
    }
    
    public void removeProgress(SherlockFragmentActivity activity) {
        progressIndeterminateCount--;
        if (progressIndeterminateCount <= 0)
            progressIndeterminateCount = 0;
        setIndeterminateVisibility(activity);
    }
    
    public void resetProgress(SherlockFragmentActivity activity) {
        progressIndeterminateCount = 0;
        setIndeterminateVisibility(activity);
    }
    
    public void setIndeterminateVisibility(SherlockFragmentActivity activity) {
        boolean visible = (progressIndeterminateCount > 0);
        activity.setSupportProgressBarIndeterminateVisibility(visible);
        if (!visible) {
            activity.setProgress(0);
            activity.setSupportProgressBarVisibility(false);
        }
    }
    
}
