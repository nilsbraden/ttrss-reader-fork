/*
 * Copyright (C) 2009 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ttrssreader.imageCache;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.gui.interfaces.ICacheEndListener;
import org.ttrssreader.utils.Utils;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

public class ForegroundService extends Service implements ICacheEndListener {
    private static final Class<?>[] mSetForegroundSignature = new Class[] { boolean.class };
    private static final Class<?>[] mStartForegroundSignature = new Class[] { int.class, Notification.class };
    private static final Class<?>[] mStopForegroundSignature = new Class[] { boolean.class };
    
    private NotificationManager mNM;
    private Method mSetForeground;
    private Method mStartForeground;
    private Method mStopForeground;
    private Object[] mSetForegroundArgs = new Object[1];
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];
    
    public static final String ACTION_LOAD_IMAGES = "load_images";
    public static final String ACTION_LOAD_ARTICLES = "load_articles";
    
    private ImageCacher imageCacher;
    private static ForegroundService instance = null;
    private static ICacheEndListener parent;
    
    public static boolean isInstanceCreated() {
        return instance != null;
    }
    
    private boolean imageCache = false;
    
    public static void loadImagesToo() {
        if (instance != null)
            instance.imageCache = true;
    }
    
    public static void registerCallback(ICacheEndListener parentGUI) {
        parent = parentGUI;
    }
    
    void invokeMethod(Method method, Object[] args) {
        try {
            method.invoke(this, mStartForegroundArgs);
        } catch (InvocationTargetException e) {
            // Should not happen.
            Log.e(Utils.TAG, "Unable to invoke method", e);
        } catch (IllegalAccessException e) {
            // Should not happen.
            Log.e(Utils.TAG, "Unable to invoke method", e);
        }
    }
    
    /**
     * This is a wrapper around the new startForeground method, using the older
     * APIs if it is not available.
     */
    void startForegroundCompat(int id, Notification notification) {
        // If we have the new startForeground API, then use it.
        if (mStartForeground != null) {
            mStartForegroundArgs[0] = Integer.valueOf(id);
            mStartForegroundArgs[1] = notification;
            invokeMethod(mStartForeground, mStartForegroundArgs);
            return;
        }
        
        // Fall back on the old API.
        mSetForegroundArgs[0] = Boolean.TRUE;
        invokeMethod(mSetForeground, mSetForegroundArgs);
        mNM.notify(id, notification);
    }
    
    /**
     * This is a wrapper around the new stopForeground method, using the older
     * APIs if it is not available.
     */
    void stopForegroundCompat(int id) {
        // If we have the new stopForeground API, then use it.
        if (mStopForeground != null) {
            mStopForegroundArgs[0] = Boolean.TRUE;
            try {
                mStopForeground.invoke(this, mStopForegroundArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                Log.e(Utils.TAG, "Unable to invoke stopForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.e(Utils.TAG, "Unable to invoke stopForeground", e);
            }
            return;
        }
        
        // Fall back on the old API. Note to cancel BEFORE changing the
        // foreground state, since we could be killed at that point.
        mNM.cancel(id);
        mSetForegroundArgs[0] = Boolean.FALSE;
        invokeMethod(mSetForeground, mSetForegroundArgs);
    }
    
    @Override
    public void onCreate() {
        instance = this;
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        try {
            mStartForeground = getClass().getMethod("startForeground", mStartForegroundSignature);
            mStopForeground = getClass().getMethod("stopForeground", mStopForegroundSignature);
        } catch (NoSuchMethodException e) {
            // Running on an older platform.
            mStartForeground = mStopForeground = null;
            return;
        }
        try {
            mSetForeground = getClass().getMethod("setForeground", mSetForegroundSignature);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("OS doesn't have Service.startForeground OR Service.setForeground!");
        }
    }
    
    @Override
    public void onDestroy() {
        finishService();
    }
    
    /**
     * Cleans up all running notifications, notifies waiting activities and clears the instance of the service.
     */
    public void finishService() {
        if (instance != null) {
            // Remove the notification
            stopForegroundCompat(R.string.Cache_service_started);
            
            // Call all activities
            Controller.getInstance().notifyActivities();
            
            // Reset Instance
            instance = null;
        }
    }
    
    // This is the old onStart method that will be called on the pre-2.0
    // platform. On 2.0 or later we override onStartCommand() so this
    // method will not be called.
    @Override
    public void onStart(Intent intent, int startId) {
        handleCommand(intent);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleCommand(intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
    
    void handleCommand(Intent intent) {
        // Fail-safe
        if (intent == null || intent.getAction() == null)
            return;
        
        int icon = R.drawable.notification_icon;
        CharSequence title = "";
        CharSequence ticker = getText(R.string.Cache_service_started);
        CharSequence text = getText(R.string.Cache_service_text);
        
        if (ACTION_LOAD_IMAGES.equals(intent.getAction())) {
            imageCacher = new ImageCacher(this, this, false);
            imageCacher.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            title = getText(R.string.Cache_service_imagecache);
        } else if (ACTION_LOAD_ARTICLES.equals(intent.getAction())) {
            imageCacher = new ImageCacher(this, this, true);
            imageCacher.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            title = getText(R.string.Cache_service_articlecache);
        }
        
        // Display notification
        Notification notification = new Notification(icon, ticker, System.currentTimeMillis());
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(), 0);
        notification.setLatestEventInfo(this, title, text, pendingIntent);
        startForegroundCompat(R.string.Cache_service_started, notification);
    }
    
    @Override
    public void onCacheEnd() {
        // Start a new cacher if images have been requested
        if (imageCache) {
            imageCache = false;
            imageCacher = new ImageCacher(this, this, false);
            imageCacher.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            finishService();
            this.stopSelf();
        }
    }
    
    @Override
    public void onCacheProgress(int taskCount, int progress) {
        if (parent != null)
            parent.onCacheProgress(taskCount, progress);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
}
