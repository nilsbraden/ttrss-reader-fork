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

import java.io.FileNotFoundException;
import java.net.URI;
import java.util.regex.Pattern;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.preferences.Constants;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

public class Utils {
    
    /**
     * Min supported versions of the Tiny Tiny RSS Server
     */
    public static final int SERVER_VERSION = 150;
    
    /**
     * The TAG for Log-Output
     */
    public static final String TAG = "ttrss";
    
    /**
     * Vibrate-Time for vibration when end of list is reached
     */
    public static final long SHORT_VIBRATE = 50;
    
    /**
     * The time after which data will be fetched again from the server if asked for the data
     */
    public static final int UPDATE_TIME = 600000;
    public static final int HALF_UPDATE_TIME = UPDATE_TIME / 2;
    
    /**
     * The Pattern to match image-urls inside HTML img-tags.
     */
    public static final Pattern findImageUrlsPattern = Pattern.compile("<img.+src=\"([^\\\"]*)\".*>",
            Pattern.CASE_INSENSITIVE);
    
    private static final int ID_RUNNING = 4564561;
    private static final int ID_FINISHED = 7897891;
    
    /*
     * Check if this is the first run of the app, if yes, returns false.
     */
    public static boolean checkFirstRun(Context a) {
        return !(Controller.getInstance().newInstallation());
    }
    
    /*
     * Check if a new version of the app was installed, returns false if this is the case.
     */
    public static boolean checkNewVersion(Context c) {
        String thisVersion = getAppVersionName(c);
        String lastVersionRun = Controller.getInstance().getLastVersionRun();
        Controller.getInstance().setLastVersionRun(thisVersion);
        
        if (thisVersion.equals(lastVersionRun)) {
            // No new version installed, perhaps a new version exists
            // Only run task once for every session
            if (AsyncTask.Status.PENDING.equals(updateVersionTask.getStatus())) {
                if (Controller.getInstance().isExecuteOnExecutorAvailable())
                    updateVersionTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                else
                    updateVersionTask.execute();
            }
            
            return true;
        } else {
            return false;
        }
    }
    
    /*
     * Check if crashreport-file exists, returns false if it exists.
     */
    public static boolean checkCrashReport(Context c) {
        // Ignore crashreport if this version isn't the newest from market
        int latest = Controller.getInstance().appLatestVersion();
        int current = getAppVersionCode(c);
        if (latest > current)
            return true; // Ignore!
            
        try {
            c.openFileInput(TopExceptionHandler.FILE);
            return false;
        } catch (FileNotFoundException e) {
            return true;
        }
    }
    
    /*
     * Checks the config for a user-defined server, returns true if a server has been defined
     */
    public static boolean checkConfig() {
        URI uri = Controller.getInstance().url();
        if (uri == null || uri.toASCIIString().equals(Constants.URL_DEFAULT + Controller.JSON_END_URL)) {
            return false;
        }
        return true;
    }
    
    /**
     * Retrieves the packaged version-code of the application
     * 
     * @param c
     *            - The Activity to retrieve the current version
     * @return the version-string
     */
    public static int getAppVersionCode(Context c) {
        int result = 0;
        try {
            PackageManager manager = c.getPackageManager();
            PackageInfo info = manager.getPackageInfo(c.getPackageName(), 0);
            result = info.versionCode;
        } catch (NameNotFoundException e) {
            Log.w(TAG, "Unable to get application version: " + e.getMessage());
            result = 0;
        }
        return result;
    }
    
    /**
     * Retrieves the packaged version-name of the application
     * 
     * @param c
     *            - The Activity to retrieve the current version
     * @return the version-string
     */
    public static String getAppVersionName(Context c) {
        String result = "";
        try {
            PackageManager manager = c.getPackageManager();
            PackageInfo info = manager.getPackageInfo(c.getPackageName(), 0);
            result = info.versionName;
        } catch (NameNotFoundException e) {
            Log.w(TAG, "Unable to get application version: " + e.getMessage());
            result = "";
        }
        return result;
    }
    
    /**
     * Checks if the option to work offline is set or if the data-connection isn't established, else returns true. If we
     * are about to connect it waits for maximum one second and then returns the network state without waiting anymore.
     * 
     * @param cm
     * @return
     */
    public static boolean isConnected(ConnectivityManager cm) {
        if (Controller.getInstance().workOffline())
            return false;
        
        return checkConnected(cm);
    }
    
    /**
     * Wrapper for Method checkConnected(ConnectivityManager cm, boolean onlyWifi)
     * 
     * @param cm
     * @return
     */
    public static boolean checkConnected(ConnectivityManager cm) {
        return checkConnected(cm, false);
    }
    
    /**
     * Only checks the connectivity without regard to the preferences
     * 
     * @param cm
     * @return
     */
    public static boolean checkConnected(ConnectivityManager cm, boolean onlyWifi) {
        if (cm == null)
            return false;
        
        NetworkInfo info;
        if (onlyWifi) {
            info = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        } else {
            info = cm.getActiveNetworkInfo();
        }
        
        if (info == null)
            return false;
        
        if (info.isConnected())
            return true;
        
        synchronized (Utils.class) {
            int wait = 0;
            while (info.isConnectedOrConnecting() && !info.isConnected()) {
                try {
                    wait += 100;
                    Utils.class.wait(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                if (wait > 1000) { // Wait a maximum of one second for connection
                    break;
                }
            }
        }
        
        return info.isConnected();
    }
    
    public static void showFinishedNotification(String content, int time, boolean error, Context context) {
        showFinishedNotification(content, time, error, context, new Intent());
    }
    
    /**
     * Shows a notification with the given parameters
     * 
     * @param content
     *            the string to display
     * @param time
     *            how long the process took
     * @param error
     *            set to true if an error occured
     * @param context
     *            the context
     */
    public static void showFinishedNotification(String content, int time, boolean error, Context context, Intent intent) {
        
        int icon;
        CharSequence title = "";
        CharSequence ticker = "";
        CharSequence text = content;
        if (content == null)
            text = context.getText(R.string.Utils_DownloadFinishedText);
        
        if (error) {
            icon = R.drawable.icon;
            title = context.getText(R.string.Utils_DownloadErrorTitle);
            ticker = context.getText(R.string.Utils_DownloadErrorTicker);
        } else {
            icon = R.drawable.icon;
            title = String.format((String) context.getText(R.string.Utils_DownloadFinishedTitle), time);
            ticker = context.getText(R.string.Utils_DownloadFinishedTicker);
        }
        
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotMan = (NotificationManager) context.getSystemService(ns);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        Notification n = new Notification(icon, ticker, System.currentTimeMillis());
        n.flags |= Notification.FLAG_AUTO_CANCEL;
        n.setLatestEventInfo(context, title, text, pendingIntent);
        
        mNotMan.notify(ID_FINISHED, n);
    }
    
    public static void showRunningNotification(Context context, boolean finished) {
        showRunningNotification(context, finished, new Intent());
    }
    
    /**
     * Shows a notification indicating that something is running. When called with finished=true it removes the
     * notification.
     * 
     * @param context
     *            the context
     * @param finished
     *            if the notification is to be removed
     */
    public static void showRunningNotification(Context context, boolean finished, Intent intent) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotMan = (NotificationManager) context.getSystemService(ns);
        
        // if finished remove notification and return, else display notification
        if (finished) {
            mNotMan.cancel(ID_RUNNING);
            return;
        }
        
        int icon = R.drawable.notification_icon;
        CharSequence title = context.getText(R.string.Utils_DownloadRunningTitle);
        CharSequence ticker = context.getText(R.string.Utils_DownloadRunningTicker);
        CharSequence text = context.getText(R.string.Utils_DownloadRunningText);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        Notification n = new Notification(icon, ticker, System.currentTimeMillis());
        n.flags |= Notification.FLAG_AUTO_CANCEL;
        n.setLatestEventInfo(context, title, text, pendingIntent);
        
        mNotMan.notify(ID_RUNNING, n);
    }
    
    /**
     * Reads a file from my webserver and parses the content. It containts the version code of the latest supported
     * version. If the version of the installed app is lower then this the feature "Send mail with stacktrace on error"
     * will be disabled to make sure I only receive "new" Bugreports.
     */
    private static AsyncTask<Void, Void, Void> updateVersionTask = new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
            
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            
            // Check last appVersionCheckDate
            long last = Controller.getInstance().appVersionCheckTime();
            long time = System.currentTimeMillis();
            if (time - last > 86400000) { // More then one day
            
                // Retrieve remote version
                int remote = 0;
                
                try {
                    DefaultHttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost("http://nilsbraden.de/android/tt-rss/minSupportedVersion.txt");
                    
                    HttpResponse httpResponse = httpClient.execute(httpPost);
                    HttpEntity httpEntity = httpResponse.getEntity();
                    
                    if (httpEntity.getContentLength() < 0 || httpEntity.getContentLength() > 100)
                        throw new Exception("Content too long or empty.");
                    
                    String content = EntityUtils.toString(httpEntity);
                    
                    // Only ever read the integer if it matches the regex and is not too long
                    if (content.matches("[0-9]*[\\r\\n]*")) {
                        content = content.replaceAll("[^0-9]*", "");
                        remote = Integer.parseInt(content);
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error while downloading version-information: " + e.getMessage());
                }
                
                // Store version
                if (remote > 0)
                    Controller.getInstance().setAppLatestVersion(remote);
            }
            
            // Also fetch the current API-Level from the server. This may be helpful later.
            last = Controller.getInstance().apiLevelUpdated();
            if (time - last > 86400000) { // One day
                int apiLevel = Data.getInstance().getApiLevel();
                Controller.getInstance().setApiLevel(apiLevel);
            }
            
            return null;
        }
    };
}
