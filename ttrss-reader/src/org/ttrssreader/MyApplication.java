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

package org.ttrssreader;

import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.controllers.ProgressBarManager;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.PRNGFixes;
import android.app.Application;

public class MyApplication extends Application {
    
    @SuppressWarnings("unused")
    private static final String TAG = MyApplication.class.getSimpleName();
    
    public void onCreate() {
        super.onCreate();
        
        PRNGFixes.apply();
        initAsyncTask();
        initSingletons();
        
        Data.getInstance().notifyListeners(); // Notify once to make sure the handler is initialized
    }
    
    private void initAsyncTask() {
        // make sure AsyncTask is loaded in the Main thread
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                return null;
            }
        }.execute();
    }
    
    protected void initSingletons() {
        ProgressBarManager.getInstance();
        Controller.getInstance().initialize(this);
        DBHelper.getInstance().initialize(this);
        Data.getInstance().initialize(this);
    }
    
    @Override
    public void onLowMemory() {
        Controller.getInstance().lowMemory(true);
        super.onLowMemory();
    }
    
}
