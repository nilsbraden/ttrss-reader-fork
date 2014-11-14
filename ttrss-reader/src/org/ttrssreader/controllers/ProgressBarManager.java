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

package org.ttrssreader.controllers;

import android.app.Activity;

public class ProgressBarManager {

    @SuppressWarnings("unused")
    private static final String TAG = ProgressBarManager.class.getSimpleName();
    
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
    
    public void addProgress(Activity activity) {
        progressIndeterminateCount++;
        setIndeterminateVisibility(activity);
    }
    
    public void removeProgress(Activity activity) {
        progressIndeterminateCount--;
        if (progressIndeterminateCount <= 0)
            progressIndeterminateCount = 0;
        setIndeterminateVisibility(activity);
    }
    
    public void resetProgress(Activity activity) {
        progressIndeterminateCount = 0;
        setIndeterminateVisibility(activity);
    }
    
    public void setIndeterminateVisibility(Activity activity) {
        boolean visible = (progressIndeterminateCount > 0);
        activity.setProgressBarIndeterminateVisibility(visible);
        if (!visible) {
            activity.setProgress(0);
            activity.setProgressBarVisibility(false);
        }
    }
    
}
