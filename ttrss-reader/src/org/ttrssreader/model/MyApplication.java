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

package org.ttrssreader.model;

import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.controllers.ProgressBarManager;
import org.ttrssreader.controllers.UpdateController;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.Utils;
import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.view.WindowManager;

public class MyApplication extends Application {
    
    public void onCreate() {
        super.onCreate();
        // make sure AsyncTask is loaded in the Main thread
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                return null;
            }
        }.execute();
        initSingletons();
    }
    
    protected void initSingletons() {
        long time = System.currentTimeMillis();
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Log.d(Utils.TAG, "TIME: " + (System.currentTimeMillis() - time) + "ms");
        
        time = System.currentTimeMillis();
        Log.d(Utils.TAG, "TIME 1: " + (System.currentTimeMillis() - time) + "ms");
        
        time = System.currentTimeMillis();
        ProgressBarManager.getInstance();
        Log.d(Utils.TAG, "TIME 2: " + (System.currentTimeMillis() - time) + "ms");
        
        time = System.currentTimeMillis();
        Controller.getInstance().checkAndInitializeController(this, wm.getDefaultDisplay());
        Log.d(Utils.TAG, "TIME 3: " + (System.currentTimeMillis() - time) + "ms");
        
        time = System.currentTimeMillis();
        DBHelper.getInstance().checkAndInitializeDB(this);
        Log.d(Utils.TAG, "TIME 4: " + (System.currentTimeMillis() - time) + "ms");
        
        time = System.currentTimeMillis();
        Data.getInstance().checkAndInitializeData(this);
        Log.d(Utils.TAG, "TIME 5: " + (System.currentTimeMillis() - time) + "ms");
        
        time = System.currentTimeMillis();
        UpdateController.getInstance().notifyListeners(); // Notify once to make sure the handler is initialized
        Log.d(Utils.TAG, "TIME 6: " + (System.currentTimeMillis() - time) + "ms");
    }
    
}
