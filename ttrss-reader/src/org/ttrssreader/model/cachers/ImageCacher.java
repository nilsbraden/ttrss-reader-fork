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

package org.ttrssreader.model.cachers;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.model.pojos.FeedItem;
import org.ttrssreader.utils.ImageCache;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.util.Log;

public class ImageCacher implements ICacheable {
    
    private long maxFileSize = 1024 * 1024 * 6; // Max size for one image is 6 MB
    
    private Context context;
    private boolean onlyArticles;
    private boolean onlyUnreadImages;
    private boolean onlyUnreadArticles;
    private long start;
    private long cacheSizeMax;
    private ImageCache cache;
    private long folderSize;
    
    public ImageCacher(Context context, boolean onlyArticles) {
        this.context = context;
        this.onlyArticles = onlyArticles;
        this.onlyUnreadImages = Controller.getInstance().isImageCacheUnread();
        this.onlyUnreadArticles = Controller.getInstance().isArticleCacheUnread();
        this.start = System.currentTimeMillis();
        this.cacheSizeMax = Controller.getInstance().getImageCacheSize() * 1048576;
        this.cache = Controller.getInstance().getImageCache(context);
    }
    
    @Override
    public void cache() {
        if (cache == null)
            return;
        
        // Update all articles
        Log.w(Utils.TAG, "Updating articles...");
        updateLocalArticles(4);
        if (onlyArticles) // We are done here..
            return;
        
        Log.w(Utils.TAG, "Updating cache...");
        cache.fillMemoryCacheFromDisk();
        
        // Check connectivity
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (!Utils.checkConnection(cm)) {
            Log.w(Utils.TAG, "No connectivity, aborting...");
            return;
        }
        
        downloadImages();
        
        int half = (int) (System.currentTimeMillis() - start) / 1000;
        Log.w(Utils.TAG, "Downloading finished after " + half + " seconds");
        
        shrinkCache();
        
        int time = (int) (System.currentTimeMillis() - start) / 1000;
        String content = String.format("Cache: %s MB (Limit: %s MB, took %s seconds)", folderSize / 1048576,
                cacheSizeMax / 1048576, time);
        Utils.showNotification(content, time, false, context);
        Log.w(Utils.TAG, content);
    }
    
    private void updateLocalArticles(int nrOfTasks) {
        long time = System.currentTimeMillis();
        UpdateArticlesTask[] tasks = new UpdateArticlesTask[nrOfTasks];
        Data.getInstance().updateFeeds(-4);
        
        for (FeedItem f : DBHelper.getInstance().getFeeds(-4)) {
            if (f.getUnread() == 0)
                continue;
            
            boolean done = false;
            while (!done) {
                
                // Start new Task if task-slot is available
                for (int i = 0; i < nrOfTasks; i++) {
                    UpdateArticlesTask t = tasks[i];
                    if (t == null || t.getStatus().equals(AsyncTask.Status.FINISHED)) {
                        t = new UpdateArticlesTask(onlyUnreadArticles);
                        t.execute(f.getId());
                        tasks[i] = t;
                        done = true;
                        break;
                    }
                }
                
                if (!done) { // No task-slot available, wait.
                    synchronized (this) {
                        try {
                            wait(100);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        }
        
        Log.i(Utils.TAG, "Fetching new Articles took " + (System.currentTimeMillis() - time)
                + "ms (There can be still tasks running...)");
    }
    
    private void downloadImages() {
        // Add where-clause for only unread articles
        String where = "";
        if (onlyUnreadImages) {
            where = "isUnread>0";
        }
        
        Cursor c = DBHelper.getInstance().query(DBHelper.TABLE_ARTICLES, new String[] { "content", "attachments" },
                where, null, null, null, null);
        if (c.moveToFirst()) {
            long downloaded = 0;
            
            while (!c.isAfterLast()) {
                
                // Get images included in HTML
                for (String url : Utils.findAllImageUrls(c.getString(0))) {
                    if (!cache.containsKey(url)) {
                        downloaded += Utils.downloadToFile(url, cache.getCacheFile(url), maxFileSize);
                    }
                }
                
                // Get images from attachments separately
                for (String url : c.getString(1).split(";")) {
                    for (String ext : Utils.IMAGE_EXTENSIONS) {
                        if (url.toLowerCase().contains(ext) && !cache.containsKey(url)) {
                            downloaded += Utils.downloadToFile(url, cache.getCacheFile(url), maxFileSize);
                            break;
                        }
                    }
                }
                
                if (downloaded > cacheSizeMax) {
                    Log.w(Utils.TAG, "Stopping download, downloaded data exceeds cache-size-limit from options.");
                    break;
                }
                
                c.move(1);
            }
        }
        c.close();
    }
    
    private void shrinkCache() {
        File cacheFolder = new File(cache.getDiskCacheDirectory());
        folderSize = Utils.getFolderSize(cacheFolder);
        
        if (folderSize > cacheSizeMax) {
            Log.d(Utils.TAG, String.format("Cache: %s MB (Limit: %s MB)", folderSize / 1048576, cacheSizeMax / 1048576));
            
            // Delete old images (86400000 = 24 * 60 * 60 * 1000)
            long maxAge = System.currentTimeMillis() - (Controller.getInstance().getImageCacheAge() * 86400000);
            File[] fArray = cacheFolder.listFiles();
            for (File f : fArray) {
                if (f.lastModified() < maxAge) {
                    f.delete();
                }
            }
            
            // Refresh size and check again
            folderSize = Utils.getFolderSize(cacheFolder);
            if (folderSize > cacheSizeMax) {
                List<File> list = Arrays.asList(fArray);
                
                // Sort list by last access date
                Collections.sort(list, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        return ((Long) f1.lastModified()).compareTo((Long) f2.lastModified());
                    }
                });
                
                // Delete files until size is fine with the settings
                int i = 0;
                long tempSize = 0;
                while (folderSize > cacheSizeMax) {
                    File fileDelete = list.get(i++);
                    tempSize = fileDelete.length();
                    if (fileDelete.delete()) {
                        Log.d(Utils.TAG, String.format("Cache: %s bytes, Limit: %s bytes, Deleting: %s bytes",
                                folderSize, cacheSizeMax, tempSize));
                        folderSize -= tempSize;
                    }
                }
            }
        }
    }
    
}
