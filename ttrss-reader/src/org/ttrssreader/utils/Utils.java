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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import android.app.Activity;
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
import android.util.Log;

public class Utils {
    
    /**
     * Supported extensions of imagefiles, see http://developer.android.com/guide/appendix/media-formats.html
     */
    public static final String[] IMAGE_EXTENSIONS = { "jpeg", "jpg", "gif", "png", "bmp" };
    
    /**
     * Supported extensions of mediafiles, see http://developer.android.com/guide/appendix/media-formats.html
     */
    public static final String[] MEDIA_EXTENSIONS = { "3gp", "mp4", "m4a", "aac", "mp3", "mid", "xmf", "mxmf", "rtttl",
            "rtx", "ota", "imy", "ogg", "wav" };
    
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
    public static final int UPDATE_TIME = 120000;
    
    /**
     * Path on sdcard to store files (DB, Certificates, ...)
     */
    public static final String SDCARD_PATH_FILES = "/Android/data/org.ttrssreader/files/";
    
    /**
     * Path on sdcard to store files (DB, Certificates, ...)
     */
    public static final String SDCARD_PATH_CACHE = "/Android/data/org.ttrssreader/cache/";
    
    /**
     * The Pattern to match image-urls inside HTML img-tags.
     */
    public static final Pattern findImageUrlsPattern = Pattern.compile("<img.+src=\"([^\"]*)\".*/>",
            Pattern.CASE_INSENSITIVE);
    
    private static final int ID_RUNNING = 4564561;
    private static final int ID_FINISHED = 7897891;
    
    /*
     * Check if this is the first run of the app, if yes, returns true.
     */
    public static boolean checkFirstRun(Activity a) {
        return Controller.getInstance().newInstallation();
    }
    
    /*
     * Check if a new version of the app was installed, returns true if this is the case.
     */
    public static boolean checkNewVersion(Activity a) {
        String thisVersion = getAppVersion(a);
        String lastVersionRun = Controller.getInstance().getLastVersionRun();
        Controller.getInstance().setLastVersionRun(thisVersion);
        
        if (thisVersion.equals(lastVersionRun)) {
            return false;
        } else {
            return true;
        }
    }
    
    /*
     * Check if crashreport-file exists, returns true if it exists.
     */
    public static boolean checkCrashReport(Activity a) {
        try {
            a.openFileInput(TopExceptionHandler.FILE);
            return true;
        } catch (FileNotFoundException e) {
            return false;
        }
    }
    
    /*
     * Checks if the server is supported by the app, returns true if it is NOT supported.
     */
    public static boolean checkServerVersion(Activity a) {
        int version = Controller.getInstance().getServerVersion();
        if (version > 0 && version < SERVER_VERSION) {
            
            // Reset the stored value so it get updated on next run.
            Controller.getInstance().resetServerVersion();
            return true;
        }
        return false;
    }
    
    /**
     * Retrieves the packaged version of the application
     * 
     * @param a
     *            - The Activity to retrieve the current version
     * @return the version-string
     */
    public static String getAppVersion(Activity a) {
        String result = "";
        try {
            PackageManager manager = a.getPackageManager();
            PackageInfo info = manager.getPackageInfo(a.getPackageName(), 0);
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
     * Only checks the connectivity without regard to the preferences
     * 
     * @param cm
     * @return
     */
    public static boolean checkConnected(ConnectivityManager cm) {
        if (cm == null)
            return false;
        
        NetworkInfo info = cm.getActiveNetworkInfo();
        
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
            Log.d(Utils.TAG, "Synchronized: Waited for " + wait + "ms for connection to become available...");
        }
        
        return info.isConnected();
    }
    
    /**
     * Downloads a given URL directly to a file, when maxSize bytes are reached the download is stopped and the file is
     * deleted.
     * 
     * @param downloadUrl
     *            the URL of the file
     * @param file
     *            the destination file
     * @param maxSize
     *            the size in bytes after which to abort the download
     * @return length of the downloaded file
     */
    public static long downloadToFile(String downloadUrl, File file, long maxSize) {
        FileOutputStream fos = null;
        int byteWritten = 0;
        try {
            if (file.exists()) {
                file.delete();
            }
            
            URL url = new URL(downloadUrl);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            
            long length = Long.parseLong(connection.getHeaderField("Content-Length"));
            if (length > maxSize) {
                Log.w(Utils.TAG, String.format(
                        "Not starting download of %s, the size (%s MB) exceeds the maximum filesize of %s MB.",
                        downloadUrl, length / 1048576, maxSize / 1048576));
                return 0;
            }
            
            file.createNewFile();
            fos = new FileOutputStream(file);
            InputStream is = connection.getInputStream();
            
            int size = 1024 * 1024;
            byte[] buf = new byte[size];
            int byteRead;
            
            while (((byteRead = is.read(buf)) != -1)) {
                fos.write(buf, 0, byteRead);
                byteWritten += byteRead;
                
                if (byteWritten > maxSize) {
                    Log.d(Utils.TAG, String.format(
                            "Download interrupted, the size of %s bytes exceeds maximum filesize.", byteWritten));
                    file.delete();
                    byteWritten = 0;
                    break;
                }
            }
        } catch (Exception e) {
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
        return byteWritten;
    }
    
    /**
     * Sums up the size of a folder including all files and subfolders.
     * 
     * @param folder
     *            the folder
     * @return the size of the folder
     */
    public static long getFolderSize(File folder) {
        long size = 0;
        for (File f : folder.listFiles()) {
            if (f.isDirectory()) {
                size += getFolderSize(f);
            } else {
                size += f.length();
            }
        }
        return size;
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
    public static void showFinishedNotification(String content, int time, boolean error, Context context) {
        
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
        
        PendingIntent intent = PendingIntent.getActivity(context, 0, new Intent(), 0);
        Notification n = new Notification(icon, ticker, System.currentTimeMillis());
        n.flags |= Notification.FLAG_AUTO_CANCEL;
        n.setLatestEventInfo(context, title, text, intent);
        
        mNotMan.notify(ID_FINISHED, n);
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
    public static void showRunningNotification(Context context, boolean finished) {
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
        
        PendingIntent intent = PendingIntent.getActivity(context, 0, new Intent(), 0);
        Notification n = new Notification(icon, ticker, System.currentTimeMillis());
        n.flags |= Notification.FLAG_AUTO_CANCEL;
        n.setLatestEventInfo(context, title, text, intent);
        
        mNotMan.notify(ID_RUNNING, n);
    }
    
}
