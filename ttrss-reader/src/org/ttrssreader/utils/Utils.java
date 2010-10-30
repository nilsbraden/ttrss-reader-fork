/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
 * Copyright (C) 2009-2010 J. Devauchelle.
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

package org.ttrssreader.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBInsertArticlesTask;
import org.ttrssreader.gui.activities.AboutActivity;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

public class Utils {
    
    /**
     * Supported extensions of multimedia files, only audio/video, no images.
     */
    public static final String[] MEDIA_EXTENSIONS = { "3gp", "mp4", "wav", "mp3", "ogg", "m4a" };
    
    /**
     * The TAG for Log-Output
     */
    public static final String TAG = "ttrss";
    
    /**
     * Time to wait before starting the background-update from the activities
     */
    public static final int WAIT = 100;
    
    /**
     * Vibrate-Time for vibration when end of list is reached
     */
    public static final long SHORT_VIBRATE = 50;
    
    /**
     * The time after which data will be fetched again from the server if asked for the data
     */
    public static int UPDATE_TIME = 120000;
    
    /**
     * Path on sdcard to store files (DB, Certificates, ...)
     */
    public static final String SDCARD_PATH = "/Android/data/org.ttrssreader/files/";
    
    public static String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is), 1024 * 10);
        StringBuilder sb = new StringBuilder();
        
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
    
    public static boolean newVersionInstalled(Activity a) {
        String thisVersion = getVersion(a);
        String lastVersionRun = Controller.getInstance().getLastVersionRun();
        Controller.getInstance().setLastVersionRun(thisVersion);
        
        if (thisVersion.equals(lastVersionRun)) {
            return false;
        } else {
            return true;
        }
    }
    
    /**
     * Retrieves the packaged version of the application
     * 
     * @param a
     *            - The Activity to retrieve the current version
     * @return the version-string
     */
    public static String getVersion(Activity a) {
        String result = "";
        try {
            PackageManager manager = a.getPackageManager();
            PackageInfo info = manager.getPackageInfo(a.getPackageName(), 0);
            result = info.versionName;
        } catch (NameNotFoundException e) {
            Log.w(AboutActivity.class.toString(), "Unable to get application version: " + e.getMessage());
            result = "";
        }
        return result;
    }
    
    public static boolean isOnline(ConnectivityManager cm) {
        if (Controller.getInstance().isWorkOffline()) {
            // Log.i(Utils.TAG, "isOnline: Config has isWorkOffline activated...");
            return false;
        } else if (cm == null) {
            return false;
        }
        
        NetworkInfo info = cm.getActiveNetworkInfo();
        
        if (info == null) {
            // Log.i(Utils.TAG, "isOnline: No network available...");
            return false;
        }
        
        synchronized (Utils.class) {
            int wait = 0;
            while (info.isConnectedOrConnecting() && !info.isConnected()) {
                try {
                    // Log.d(Utils.TAG, "isOnline: Waiting for " + wait + " seconds...");
                    wait += 100;
                    Utils.class.wait(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                if (wait > 2000)
                    break;
            }
        }
        
        // Log.i(Utils.TAG, "isOnline: Network available, State: " + info.isConnected());
        return info.isConnected();
    }
    
    public static void waitForTask(DBInsertArticlesTask task) {
        synchronized (task) {
            int count = 0;
            while (true) {
                try {
                    count += 300;
                    task.wait(300);
                } catch (InterruptedException e) {
                }
                if (task.getStatus().equals(AsyncTask.Status.FINISHED)) {
                    return;
                }
                if (count > 2999) {
                    break;
                }
            }
        }
    }
}
