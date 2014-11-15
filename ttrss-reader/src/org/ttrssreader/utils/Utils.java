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

package org.ttrssreader.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.preferences.Constants;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class Utils {
    
    private static final String TAG = Utils.class.getSimpleName();
    
    public static final long SECOND = 1000;
    public static final long MINUTE = 60 * SECOND;
    public static final long HOUR = 60 * MINUTE;
    public static final long DAY = 24 * HOUR;
    
    public static final long KB = 1024;
    public static final long MB = KB * KB;
    
    /**
     * The maximum number of articles to store.
     */
    public static final int ARTICLE_LIMIT = 5000;
    
    /**
     * Vibrate-Time for vibration when end of list is reached
     */
    public static final long SHORT_VIBRATE = 50;
    
    /**
     * The time after which data will be fetched again from the server if asked for the data
     */
    public static final long UPDATE_TIME = MINUTE * 30;
    
    /**
     * The time after which the DB and other data will be cleaned up again,
     */
    public static final long CLEANUP_TIME = DAY;
    
    /**
     * The Pattern to match image-urls inside HTML img-tags.
     */
    public static final Pattern findImageUrlsPattern = Pattern.compile("<img[^>]+?src=[\"']([^\\\"']*)",
            Pattern.CASE_INSENSITIVE);
    
    private static final int ID_RUNNING = 4564561;
    private static final int ID_FINISHED = 7897891;
    
    /*
     * Check if this is the first run of the app, if yes, returns false.
     */
    public static boolean checkIsFirstRun(Context a) {
        return Controller.getInstance().newInstallation();
    }
    
    /*
     * Check if a new version of the app was installed, returns true if this is the case. This also triggers the reset
     * of the preference noCrashreportsUntilUpdate since with a new update the crash reporting should now be enabled
     * again.
     */
    public static boolean checkIsNewVersion(Context c) {
        String thisVersion = getAppVersionName(c);
        String lastVersionRun = Controller.getInstance().getLastVersionRun();
        Controller.getInstance().setLastVersionRun(thisVersion);
        
        if (thisVersion.equals(lastVersionRun)) {
            // No new version installed, perhaps a new version exists
            // Only run task once for every session and only if we are online
            if (!checkConnected((ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE)))
                return false;
            if (AsyncTask.Status.PENDING.equals(updateVersionTask.getStatus()))
                updateVersionTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            return false;
        } else {
            // New update was installed, reset noCrashreportsUntilUpdate and return true to display the changelog...
            Controller.getInstance().setNoCrashreportsUntilUpdate(false);
            return true;
        }
    }
    
    /*
     * Checks the config for a user-defined server, returns true if the config is invalid and the user has not yet
     * entered a valid server adress.
     */
    public static boolean checkIsConfigInvalid() {
        try {
            URI uri = Controller.getInstance().uri();
            if (uri == null || uri.toASCIIString().equals(Constants.URL_DEFAULT + Controller.JSON_END_URL)) {
                return true;
            }
        } catch (URISyntaxException e) {
            return true;
        }
        return false;
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
        return checkConnected(cm, Controller.getInstance().onlyUseWifi());
    }
    
    /**
     * Only checks the connectivity without regard to the preferences
     * 
     * @param cm
     * @return
     */
    private static boolean checkConnected(ConnectivityManager cm, boolean onlyWifi) {
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
        
        NotificationManager mNotMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        int icon = R.drawable.icon;
        CharSequence title = String.format((String) context.getText(R.string.Utils_DownloadFinishedTitle), time);
        CharSequence ticker = context.getText(R.string.Utils_DownloadFinishedTicker);
        CharSequence text = content;
        
        if (content == null)
            text = context.getText(R.string.Utils_DownloadFinishedText);
        
        if (error) {
            icon = R.drawable.icon;
            title = context.getText(R.string.Utils_DownloadErrorTitle);
            ticker = context.getText(R.string.Utils_DownloadErrorTicker);
        }
        
        Notification notification = buildNotification(context, icon, ticker, title, text, true, intent);
        mNotMan.notify(ID_FINISHED, notification);
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
    private static void showRunningNotification(Context context, boolean finished, Intent intent) {
        NotificationManager mNotMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        // if finished remove notification and return, else display notification
        if (finished) {
            mNotMan.cancel(ID_RUNNING);
            return;
        }
        
        int icon = R.drawable.notification_icon;
        CharSequence title = context.getText(R.string.Utils_DownloadRunningTitle);
        CharSequence ticker = context.getText(R.string.Utils_DownloadRunningTicker);
        CharSequence text = context.getText(R.string.Utils_DownloadRunningText);
        
        Notification notification = buildNotification(context, icon, ticker, title, text, true, intent);
        mNotMan.notify(ID_RUNNING, notification);
    }
    
    /**
     * Reads a file from my webserver and parses the content. It containts the version code of the latest supported
     * version. If the version of the installed app is lower then this the feature "Send mail with stacktrace on error"
     * will be disabled to make sure I only receive "new" Bugreports.
     */
    private static AsyncTask<Void, Void, Void> updateVersionTask = new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
            // Check last appVersionCheckDate
            long last = Controller.getInstance().appVersionCheckTime();
            if ((System.currentTimeMillis() - last) < (Utils.HOUR * 4))
                return null;
            
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
            }
            
            // Store version
            if (remote > 0)
                Controller.getInstance().setAppLatestVersion(remote);
            
            return null;
        }
    };
    
    @SuppressWarnings("deprecation")
    public static Notification buildNotification(Context context, int icon, CharSequence ticker, CharSequence title, CharSequence text, boolean autoCancel, Intent intent) {
        Notification notification = null;
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        try {
            Notification.Builder builder = new Notification.Builder(context);
            builder.setSmallIcon(icon);
            builder.setTicker(ticker);
            builder.setWhen(System.currentTimeMillis());
            builder.setContentTitle(title);
            builder.setContentText(text);
            builder.setContentIntent(pendingIntent);
            builder.setAutoCancel(autoCancel);
            if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN)
                notification = builder.getNotification();
            else
                notification = builder.build();
        } catch (Exception re) {
            Log.e(TAG, "Exception while building notification. Does your device propagate the right API-Level? ("
                    + Build.VERSION.SDK_INT + ")", re);
        }
        
        return notification;
    }
    
    public static String separateItems(Set<?> att, String separator) {
        if (att == null)
            return "";
        
        String ret;
        StringBuilder sb = new StringBuilder();
        for (Object s : att) {
            sb.append(s);
            sb.append(separator);
        }
        if (att.size() > 0) {
            ret = sb.substring(0, sb.length() - separator.length());
        } else {
            ret = sb.toString();
        }
        
        return ret;
    }
    
    private static final String REGEX_URL = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    
    public static boolean validateURL(String url) {
        return url != null && url.matches(REGEX_URL);
        
    }
    
    public static String getTextFromClipboard(Context context) {
        // New Clipboard API
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            
            if (!clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))
                return null;
            
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            CharSequence chars = item.getText();
            if (chars != null && chars.length() > 0) {
                return chars.toString();
            } else {
                Uri pasteUri = item.getUri();
                if (pasteUri != null) {
                    return pasteUri.toString();
                }
            }
        }
        return null;
    }
    
    public static boolean clipboardHasText(Context context) {
        return (getTextFromClipboard(context) != null);
    }
    
    public static void alert(Activity activity) {
        alert(activity, false);
    }
    
    /**
     * Alert the user by a short vibration or a flash of the whole screen.
     * 
     * @param activity
     */
    public static void alert(Activity activity, boolean error) {
        Vibrator vib = ((Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE));
        if (vib.hasVibrator()) {
            vib.vibrate(Utils.SHORT_VIBRATE);
        } else if (error) {
            // Only flash when user tried to move forward, flashing when reaching the last article looks just wrong.
            Animation flash = AnimationUtils.loadAnimation(activity, R.anim.flash);
            View main = activity.findViewById(R.id.frame_all);
            main.startAnimation(flash);
        }
    }
    
}
