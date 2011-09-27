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

package org.ttrssreader.gui.view;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.ttrssreader.R;
import org.ttrssreader.gui.ArticleActivity;
import org.ttrssreader.gui.MediaPlayerActivity;
import org.ttrssreader.utils.Utils;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ArticleWebViewClient extends WebViewClient {
    
    private Context context;
    private float originalScale = Float.MAX_VALUE;
    private ArticleActivity articleActivity;
    
    public ArticleWebViewClient(ArticleActivity a) {
        this.articleActivity = a;
    }
    
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, final String url) {
        
        Log.e(Utils.TAG, "Link clicked: " + url);
        context = view.getContext();
        
        boolean audioOrVideo = false;
        for (String s : Utils.MEDIA_EXTENSIONS) {
            if (url.toLowerCase().contains("." + s)) {
                audioOrVideo = true;
                break;
            }
        }
        
        if (audioOrVideo) {
            // @formatter:off
            final CharSequence[] items = {
                    (String) context.getText(R.string.WebViewClientActivity_Display),
                    (String) context.getText(R.string.WebViewClientActivity_Download) };
            // @formatter:on
            
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("What shall we do?");
            builder.setItems(items, new DialogInterface.OnClickListener() {
                
                public void onClick(DialogInterface dialog, int item) {
                    
                    switch (item) {
                        case 0:
                            Log.e(Utils.TAG, "Displaying file in mediaplayer: " + url);
                            Intent i = new Intent(context, MediaPlayerActivity.class);
                            i.putExtra(MediaPlayerActivity.URL, url);
                            context.startActivity(i);
                            break;
                        case 1:
                            try {
                                new AsyncDownloader().execute(new URL(url));
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            }
                            break;
                        default:
                            Log.e(Utils.TAG, "Doing nothing, but why is that?? Item: " + item);
                            break;
                    }
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        } else {
            Uri uri = Uri.parse(url);
            
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return true;
    }
    
    // This calls the webview which loads different HTML-Headers for original zoom and other zoom-factors, we got
    // original zoom with images scaled to match display-width and other factors to display in original width.
    @Override
    public void onScaleChanged(WebView view, float oldScale, float newScale) {
        super.onScaleChanged(view, oldScale, newScale);
        Log.d(Utils.TAG,
                String.format("originalScale: %s, oldScale: %s, newScale: %s", originalScale, oldScale, newScale));
        if (originalScale == Float.MAX_VALUE) {
            originalScale = oldScale;
            articleActivity.onZoomChanged(); // originalScale == newScale);
        }
        
    }
    
    private boolean externalStorageState() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        } else {
            return false;
        }
    }
    
    private class AsyncDownloader extends AsyncTask<URL, Void, Void> {
        protected Void doInBackground(URL... urls) {
            
            if (urls.length < 1) {
                
                String msg = "No URL given, skipping download...";
                Log.e(Utils.TAG, msg);
                Utils.showFinishedNotification(msg, 0, true, context);
                return null;
                
            } else if (!externalStorageState()) {
                
                String msg = "External Storage not available, skipping download...";
                Log.e(Utils.TAG, msg);
                Utils.showFinishedNotification(msg, 0, true, context);
                return null;
                
            }
            
            Utils.showRunningNotification(context, false);
            
            URL url = urls[0];
            long start = System.currentTimeMillis();
            
            // Build name as "download_123801230712", then try to extract a proper name from URL
            String name = "download_" + System.currentTimeMillis();
            if (!url.getFile().equals("")) {
                String n = url.getFile();
                name = n.substring(1).replaceAll("[^A-Za-z0-9_.]", "");
                
                if (name.contains(".") && name.length() > name.indexOf(".") + 4) {
                    // Try to guess the position of the extension..
                    name = name.substring(0, name.indexOf(".") + 4);
                } else if (name.length() == 0) {
                    // just to make sure..
                    name = "download_" + System.currentTimeMillis();
                }
            }
            
            // Path: /sdcard/Android/data/org.ttrssreader/files/
            StringBuilder sb = new StringBuilder();
            sb.append(Environment.getExternalStorageDirectory()).append(File.separator).append(Utils.SDCARD_PATH_FILES);
            File folder = new File(sb.toString());
            
            if (!folder.exists()) {
                folder.mkdirs();
            }
            
            RandomAccessFile file = null;
            try {
                file = new RandomAccessFile(new File(folder, name), "rw");
                file.seek(file.length()); // try to resume downloads
                
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("GET");
                c.setDoOutput(true);
                c.setRequestProperty("Range", "bytes=" + file.length() + "-"); // try to resume downloads
                c.connect();
                
                InputStream in = c.getInputStream();
                byte[] buffer = new byte[1024];
                int len1 = 0;
                while ((len1 = in.read(buffer)) != -1) {
                    file.write(buffer, 0, len1);
                }
                file.close();
                
                int time = (int) (System.currentTimeMillis() - start) / 1000;
                String path = folder + File.separator + name;
                
                Log.d(Utils.TAG, "Finished. Path: " + path + " Time: " + time + "s.");
                Utils.showFinishedNotification(path, time, false, context);
                
            } catch (IOException e) {
                String msg = "Error while downloading: " + e;
                Log.d(Utils.TAG, msg);
                Utils.showFinishedNotification(msg, 0, true, context);
            } finally {
                // Remove "running"-notification
                Utils.showRunningNotification(context, true);
            }
            return null;
        }
    }
    
}
