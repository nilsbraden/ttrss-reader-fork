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

package org.ttrssreader.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class AsyncDownloaderService extends Service {
    
    @Override
    public void onCreate() {
        Log.d(Utils.TAG, "Starting Service, onCreate...");
    }
    
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d(Utils.TAG, "Starting Service, onStart...");

        String url = "";
        Bundle extras = intent.getExtras();
        if (extras != null) {
            url = extras.getString("url");
        }
        
        long start = System.currentTimeMillis();
        
        
        AsyncDownloader ad = new AsyncDownloader(getApplicationContext());
        try {
            ad.execute(new URL (url));
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
            return;
        }
        
        while (!ad.getStatus().equals(AsyncTask.Status.FINISHED)) {
            try {
                wait(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        long stop = System.currentTimeMillis();
        try {
            showNotification(ad.get(), stop-start);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }
    
    private void showNotification(String path, long time) {
        if (path == null) {
            return;
        }
        
        int icon = android.R.drawable.alert_dark_frame;
        CharSequence tickerText = "Download finished.";
        long when = System.currentTimeMillis();
        CharSequence contentTitle = "Download finished (" + time/1000 + "ms)";
        CharSequence contentText = "File: " + path;

        Notification notification = new Notification(icon, tickerText, when);
        notification.setLatestEventInfo(getApplicationContext(), contentTitle, contentText, null);
    }
}
