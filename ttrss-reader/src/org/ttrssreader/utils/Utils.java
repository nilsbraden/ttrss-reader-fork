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
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.params.HttpParams;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.gui.activities.AboutActivity;
import org.ttrssreader.net.HttpClientFactory;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
    public static final int WAIT = 400;
    
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
    
    private static final String UPDATE_MATCH = "Current Version: <strong>";
    private static final String UPDATE_MATCH_END = "</strong>";
    
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
    
    /**
     * Checks the project-page for a version string not matching the current version. Doesn't check if the version is
     * older or newer, just looks for the difference.
     * 
     * @param a - The Activity to retrieve the current version
     * @return true if there is an update available
     */
    public static boolean newVersionAvailable(Activity a) {
        String thisVersion = getVersion(a);
        String remoteVersion = "";
        
        String html = "";
        String url = "https://code.google.com/p/ttrss-reader-fork/";
        HttpPost httpPost = new HttpPost(url);
        HttpParams httpParams = httpPost.getParams();
        HttpClient httpclient = HttpClientFactory.createInstance(httpParams);
        try {
            HttpResponse response = httpclient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                html = Utils.convertStreamToString(instream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        int start = html.indexOf(UPDATE_MATCH) + UPDATE_MATCH.length();
        int end = html.indexOf(UPDATE_MATCH_END, start);
        
        remoteVersion = html.substring(start, end);
        
        Log.d(TAG, "Local Version: " + thisVersion + " // Remote Version: " + remoteVersion);
        Log.d(TAG, (thisVersion.equals(remoteVersion) ? " (no Update)" : " (Updatable)"));
        
        return !(thisVersion.equals(remoteVersion));
    }
    
    /**
     * Retrieves the packaged version of the application
     * 
     * @param a - The Activity to retrieve the current version
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
}
