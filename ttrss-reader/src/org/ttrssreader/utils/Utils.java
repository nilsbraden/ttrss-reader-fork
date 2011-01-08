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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.gui.activities.AboutActivity;
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
    public static final String SDCARD_PATH_FILES = "/Android/data/org.ttrssreader/files/";
    
    /**
     * Path on sdcard to store files (DB, Certificates, ...)
     */
    public static final String SDCARD_PATH_CACHE = "/Android/data/org.ttrssreader/cache/";
    
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
    
    /**
     * Checks if the option to work offline is set or if the data-connection isn't established, else returns true. If we
     * are about to connect it waits for maximum 2 seconds and then returns the network state without waiting anymore.
     * 
     * @param cm
     * @return
     */
    public static boolean isOnline(ConnectivityManager cm) {
        if (Controller.getInstance().isWorkOffline()) {
            return false;
        } else if (cm == null) {
            return false;
        }
        
        NetworkInfo info = cm.getActiveNetworkInfo();
        
        if (info == null) {
            return false;
        }
        
        synchronized (Utils.class) {
            int wait = 0;
            while (info.isConnectedOrConnecting() && !info.isConnected()) {
                try {
                    wait += 100;
                    Utils.class.wait(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                if (wait > 2000) {
                    break;
                }
            }
        }
        
        return info.isConnected();
    }
    
    /**
     * Searches for cached versions of the given image and returns the local URL to access the file
     * 
     * @param url
     *            the original URL
     * @return the local URL or null if not available
     */
    public static String getCachedImageUrl(String url) {
        ImageCache cache = Controller.getInstance().getImageCache(null);
        if (cache != null && cache.containsKey(url)) {
            StringBuffer sb = new StringBuffer();
            sb.append("file://").append(cache.getDiskCacheDirectory()).append(File.separator)
                    .append(cache.getFileNameForKey(url));
            return sb.toString();
        }
        return null;
    }
    
    private static Pattern findImageUrlsPattern;
    
    /**
     * Searches the given html code for img-Tags and filters out all src-attributes, beeing URLs to images.
     * 
     * @param html
     *            the html code which is to be searched
     * @return a set of URLs in their string representation
     */
    public static Set<String> findAllImageUrls(String html) {
        Set<String> ret = new LinkedHashSet<String>();
        if (html == null || html.length() < 10) {
            return ret;
        }
        
        if (findImageUrlsPattern == null) {
            findImageUrlsPattern = Pattern.compile("<img.+src=\"([^\"]*)\".*/>", Pattern.CASE_INSENSITIVE);
        }
        
        for (int i = 0; i < html.length();) {
            i = html.indexOf("<img", i);
            if (i == -1) {
                break;
            }
            Matcher m = findImageUrlsPattern.matcher(html.substring(i, html.length()));
            
            // Filter out URLs without leading http, we cannot work with relative URLs yet.
            if (m.find() && m.group(1).startsWith("http://")) {
                ret.add(m.group(1));
                i += m.group(1).length();
            } else {
                break;
            }
        }
        return ret;
    }
    
    /**
     * Injects the local path to every image which could be found in the local cache, replacing the original URLs in the
     * html.
     * 
     * @param html
     *            the original html
     * @return the altered html with the URLs replaced so they point on local files if available
     */
    public static String injectCachedImages(String html) {
        if (html == null || html.length() < 40)
            return html;
        
        for (String url : findAllImageUrls(html)) {
            String localUrl = getCachedImageUrl(url);
            if (localUrl != null) {
                Log.d(Utils.TAG, "Replacing image: " + localUrl);
                html = html.replace(url, localUrl);
            }
        }
        return html;
    }
    
    /**
     * Downloads a given URL directly to a file, stops after maxSize bytes.
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
    public static void showNotification(String content, int time, boolean error, Context context) {
        if (content == null)
            return;
        
        CharSequence ticker = "";
        CharSequence title = "";
        
        if (error) {
            ticker = (String) context.getText(R.string.Utils_DownloadError);
            title = (String) context.getText(R.string.Utils_DownloadErrorMessage);
        } else {
            ticker = (String) context.getText(R.string.Utils_DownloadFinished);
            title = String.format((String) context.getText(R.string.Utils_DownloadFinished), time);
        }
        
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotMan = (NotificationManager) context.getSystemService(ns);
        
        long when = System.currentTimeMillis();
        int icon = R.drawable.icon;
        
        PendingIntent intent = PendingIntent.getActivity(context, 0, new Intent(), 0);
        Notification n = new Notification(icon, ticker, when);
        n.flags |= Notification.FLAG_AUTO_CANCEL;
        n.setLatestEventInfo(context, title, content, intent);
        
        // TODO replace with proper ID like articleID or something
        mNotMan.notify(time - 1290000000, n);
    }
    
}
