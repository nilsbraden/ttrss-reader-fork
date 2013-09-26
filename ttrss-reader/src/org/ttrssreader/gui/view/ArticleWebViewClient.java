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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.gui.MediaPlayerActivity;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.FileUtils;
import org.ttrssreader.utils.Utils;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ArticleWebViewClient extends WebViewClient {
    
    private Context context;
    private Activity activity;
    
    public ArticleWebViewClient(Activity a) {
        this.activity = a;
    }
    
    /**
     * clear internal WebView cache after page was viewed (this should save storage place).
     * The use can manually cache images, they should not be stored twice.
     *
     * @param view
     *            web view, which was just viewed
     * @param url
     *            viewed URL
     */
    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        view.clearCache(true);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, final String url) {
        
        context = view.getContext();
        
        boolean audioOrVideo = false;
        for (String s : FileUtils.AUDIO_EXTENSIONS) {
            if (url.toLowerCase(Locale.getDefault()).contains("." + s)) {
                audioOrVideo = true;
                break;
            }
        }
        
        for (String s : FileUtils.VIDEO_EXTENSIONS) {
            if (url.toLowerCase(Locale.getDefault()).contains("." + s)) {
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
            
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("What shall we do?");
            builder.setItems(items, new DialogInterface.OnClickListener() {
                
                public void onClick(DialogInterface dialog, int item) {
                    
                    switch (item) {
                        case 0:
                            Log.i(Utils.TAG, "Displaying file in mediaplayer: " + url);
                            Intent i = new Intent(context, MediaPlayerActivity.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            i.putExtra(MediaPlayerActivity.URL, url);
                            context.startActivity(i);
                            break;
                        case 1:
                            try {
                                new AsyncDownloader().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new URL(url));
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
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return true;
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
        private final static int BUFFER = (int) Utils.KB;
        
        protected Void doInBackground(URL... urls) {
            
            if (urls.length < 1) {
                
                String msg = "No URL given, skipping download...";
                Log.w(Utils.TAG, msg);
                Utils.showFinishedNotification(msg, 0, true, context);
                return null;
                
            } else if (!externalStorageState()) {
                
                String msg = "External Storage not available, skipping download...";
                Log.w(Utils.TAG, msg);
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
            
            // Use configured output directory
            File folder = new File(Controller.getInstance().saveAttachmentPath());
            
            if (!folder.exists()) {
                if (!folder.mkdirs()) {
                    // Folder could not be created, fallback to internal directory on sdcard
                    // Path: /sdcard/Android/data/org.ttrssreader/files/
                    folder = new File(Constants.SAVE_ATTACHMENT_DEFAULT);
                    folder.mkdirs();
                }
            }
            
            if (!folder.exists())
                folder.mkdirs();
            
            BufferedInputStream in = null;
            FileOutputStream fos = null;
            BufferedOutputStream bout = null;
            
            int count = -1;
            
            File file = null;
            try {
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                
                file = new File(folder, name);
                if (file.exists()) {
                    count = (int) file.length();
                    c.setRequestProperty("Range", "bytes=" + file.length() + "-"); // try to resume downloads
                }
                
                c.setRequestMethod("GET");
                c.setDoInput(true);
                c.setDoOutput(true);
                
                in = new BufferedInputStream(c.getInputStream());
                fos = (count == 0) ? new FileOutputStream(file) : new FileOutputStream(file, true);
                bout = new BufferedOutputStream(fos, BUFFER);
                
                byte[] data = new byte[BUFFER];
                int x = 0;
                
                while ((x = in.read(data, 0, BUFFER)) >= 0) {
                    bout.write(data, 0, x);
                    count += x;
                }
                
                int time = (int) ((System.currentTimeMillis() - start) / Utils.SECOND);
                
                // Show Intent which opens the file
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                if (file != null)
                    intent.setDataAndType(Uri.fromFile(file), FileUtils.getMimeType(file.getName()));
                
                Log.i(Utils.TAG, "Finished. Path: " + file.getAbsolutePath() + " Time: " + time + "s Bytes: " + count);
                Utils.showFinishedNotification(file.getAbsolutePath(), time, false, context, intent);
                
            } catch (IOException e) {
                String msg = "Error while downloading: " + e;
                Log.e(Utils.TAG, msg);
                e.printStackTrace();
                Utils.showFinishedNotification(msg, 0, true, context);
            } finally {
                // Remove "running"-notification
                Utils.showRunningNotification(context, true);
                
                if (bout != null) {
                    try {
                        bout.close();
                    } catch (IOException e) {
                    }
                }
            }
            return null;
        }
    }
    
}
