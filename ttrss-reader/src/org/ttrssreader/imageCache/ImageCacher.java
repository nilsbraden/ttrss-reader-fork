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

package org.ttrssreader.imageCache;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.gui.interfaces.ICacheEndListener;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.FileDateComparator;
import org.ttrssreader.utils.FileUtils;
import org.ttrssreader.utils.StringSupport;
import org.ttrssreader.utils.Utils;
import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class ImageCacher extends AsyncTask<Void, Integer, Void> {
    
    private static final int DEFAULT_TASK_COUNT = 3;
    
    private ICacheEndListener parent;
    private Context context;
    
    private boolean onlyArticles;
    private long cacheSizeMax;
    private ImageCache imageCache;
    private long folderSize;
    private long downloaded = 0;
    private int taskCount = 0;
    
    private static Handler handler;
    private long start;
    private Map<Integer, DownloadImageTask> map;
    
    public ImageCacher(ICacheEndListener parent, Context context, boolean onlyArticles) {
        this.parent = parent;
        this.context = context;
        this.onlyArticles = onlyArticles;
        
        // Create Handler in a new Thread so all tasks are started in this new thread instead of the main UI-Thread
        myHandler = new MyHandler();
        myHandler.start();
    }
    
    private static class MyHandler extends Thread {
        // Source: http://mindtherobot.com/blog/159/android-guts-intro-to-loopers-and-handlers/
        @Override
        public void run() {
            try {
                Looper.prepare();
                handler = new Handler();
                Looper.loop();
            } catch (Throwable t) {
            }
        }
    };
    
    public static Thread myHandler;
    
    // This method is allowed to be called from any thread
    public synchronized void requestStop() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Looper.myLooper().quit();
            }
        });
    }
    
    @Override
    protected Void doInBackground(Void... params) {
        start = System.currentTimeMillis();
        
        while (true) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (!Utils.checkConnected(cm)) {
                Log.e(Utils.TAG, "No connectivity, aborting...");
                break;
            }
            
            // Update all articles
            long timeArticles = System.currentTimeMillis();
            // Only use progress-updates and callbacks for downloading articles, images are done in background
            // completely
            Set<Feed> labels = DBHelper.getInstance().getFeeds(-2);
            taskCount = DEFAULT_TASK_COUNT + labels.size() + 1; // 1 for the caching of all articles
            
            int progress = 0;
            Data.getInstance().updateCounters(true, true);
            publishProgress(++progress); // Move progress forward
            Data.getInstance().updateCategories(true);
            publishProgress(++progress); // Move progress forward
            Data.getInstance().updateFeeds(Data.VCAT_ALL, true);
            
            // Cache all articles
            publishProgress(++progress);
            Data.getInstance().cacheArticles(false, true);
            
            for (Feed f : labels) {
                if (f.unread == 0)
                    continue;
                publishProgress(++progress); // Move progress forward
                Data.getInstance().updateArticles(f.id, true, false, false, true);
            }
            
            publishProgress(taskCount); // Move progress forward
            Log.i(Utils.TAG, "Updating articles took " + (System.currentTimeMillis() - timeArticles) + "ms");
            
            if (onlyArticles) // We are done here..
                break;
            
            // Initialize other preferences
            this.cacheSizeMax = Utils.IMAGE_CACHE_SIZE * Utils.MB;
            this.imageCache = Controller.getInstance().getImageCache();
            if (imageCache == null)
                break;
            
            imageCache.fillMemoryCacheFromDisk();
            downloadImages();
            
            purgeCache();
            
            Log.i(Utils.TAG, String.format("Cache: %s MB (Limit: %s MB, took %s seconds)", folderSize / 1048576,
                    cacheSizeMax / 1048576, (System.currentTimeMillis() - start) / Utils.SECOND));
            
            break;
        }
        
        // Cleanup
        publishProgress(Integer.MAX_VALUE); // Call onCacheEnd()
        requestStop();
        return null;
    }
    
    /**
     * Calls the parent method to update the progress-bar in the UI while articles are refreshed. This is not called
     * anymore from the moment on when the image-downloading starts.
     */
    @Override
    protected void onProgressUpdate(Integer... values) {
        if (parent != null) {
            if (values[0] == Integer.MAX_VALUE) {
                parent.onCacheEnd();
            } else {
                parent.onCacheProgress(taskCount, values[0]);
            }
        }
    }
    
    protected void downloadFinished(int articleId, Long size, boolean ok) {
        synchronized (map) {
            if (size > 0)
                downloaded += size;
            map.remove(articleId);
            if (ok)
                DBHelper.getInstance().updateArticleCachedImages(articleId, true);
            map.notifyAll();
        }
    }
    
    @SuppressLint("UseSparseArrays")
    private void downloadImages() {
        long time = System.currentTimeMillis();
        // DownloadImageTask[] tasks = new DownloadImageTask[DOWNLOAD_IMAGES_THREADS];
        map = new HashMap<Integer, ImageCacher.DownloadImageTask>();
        
        Cursor cursor = null;
        try {
            cursor = DBHelper.getInstance().queryArticlesForImageCache();
            cursor.moveToFirst();
            
            while (!cursor.isAfterLast()) {
                // Get images included in HTML
                Set<String> set = new HashSet<String>();
                
                for (String url : findAllImageUrls(cursor.getString(1), cursor.getInt(0))) {
                    if (!imageCache.containsKey(url))
                        set.add(url);
                }
                
                // Get images from attachments separately
                for (String url : cursor.getString(2).split(";")) {
                    for (String ext : FileUtils.IMAGE_EXTENSIONS) {
                        if (url.toLowerCase(Locale.getDefault()).contains("." + ext) && !imageCache.containsKey(url)) {
                            set.add(url);
                            break;
                        }
                    }
                }
                
                if (set.size() > 0) {
                    ImageCacher.DownloadImageTask task = new ImageCacher.DownloadImageTask(imageCache,
                            cursor.getInt(0), StringSupport.setToArray(set));
                    handler.post(task);
                    map.put(cursor.getInt(0), task);
                }
                
                if (downloaded > cacheSizeMax) {
                    Log.w(Utils.TAG, "Stopping download, downloaded data exceeds cache-size-limit from options.");
                    break;
                }
                cursor.moveToNext();
            }
            
        } finally {
            cursor.close();
        }
        
        while (!map.isEmpty()) {
            synchronized (map) {
                try {
                    map.wait(Utils.SECOND);
                } catch (InterruptedException e) {
                    Log.d(Utils.TAG, "Got an InterruptedException!");
                }
            }
        }
        
        Log.i(Utils.TAG, "Downloading images took " + (System.currentTimeMillis() - time) + "ms");
    }
    
    public class DownloadImageTask implements Runnable {
        private static final long maxFileSize = Utils.MB * 6; // Max size for one image is 6 MB
        private ImageCache imageCache;
        private int articleId;
        private String[] params;
        public boolean allOK = true;
        
        public DownloadImageTask(ImageCache cache, int articleId, String... params) {
            this.imageCache = cache;
            this.articleId = articleId;
            this.params = params;
        }
        
        @Override
        public void run() {
            long downloaded = 0;
            for (String url : params) {
                long size = FileUtils.downloadToFile(url, imageCache.getCacheFile(url), maxFileSize);
                
                if (size == -1)
                    allOK = false; // Error
                else
                    downloaded += size;
            }
            downloadFinished(articleId, downloaded, allOK);
        }
    }
    
    private void purgeCache() {
        long time = System.currentTimeMillis();
        File cacheFolder = new File(imageCache.getDiskCacheDirectory());
        
        folderSize = 0;
        if (cacheFolder.isDirectory()) {
            for (File f : cacheFolder.listFiles()) {
                folderSize += f.length();
            }
        }
        
        if (folderSize > cacheSizeMax) {
            // Sort list of files by last access date
            List<File> list = Arrays.asList(cacheFolder.listFiles());
            if (list == null)
                return;
            Collections.sort(list, new FileDateComparator());
            
            int i = 0;
            while (folderSize > cacheSizeMax) {
                if (i >= list.size()) // Should only happen if cacheSize has been set to 0
                    break;
                
                File f = list.get(i);
                i++;
                folderSize -= f.length();
                f.delete();
            }
        }
        Log.i(Utils.TAG, "Purging cache took " + (System.currentTimeMillis() - time) + "ms");
    }
    
    /**
     * Searches for cached versions of the given image and returns the local URL to access the file.
     * 
     * @param url
     *            the original URL
     * @return the local URL or null if no image in cache or if the file couldn't be found or another thread is creating
     *         the imagecache at the moment.
     */
    public static String getCachedImageUrl(String url) {
        ImageCache cache = Controller.getInstance().getImageCache(false);
        if (cache != null && cache.containsKey(url)) {
            
            StringBuffer sb = new StringBuffer();
            sb.append(cache.getDiskCacheDirectory());
            sb.append(File.separator);
            sb.append(cache.getFileNameForKey(url));
            
            File file = new File(sb.toString());
            if (file.exists()) {
                sb.insert(0, "file://"); // Add "file:" at the beginning..
                return sb.toString();
            }
        }
        return null;
    }
    
    /**
     * Searches the given html code for img-Tags and filters out all src-attributes, beeing URLs to images.
     * 
     * @param html
     *            the html code which is to be searched
     * @return a set of URLs in their string representation
     */
    public static Set<String> findAllImageUrls(String html, int articleId) {
        Set<String> ret = new LinkedHashSet<String>();
        if (html == null || html.length() < 10)
            return ret;
        
        for (int i = 0; i < html.length();) {
            i = html.indexOf("<img", i);
            if (i == -1)
                break;
            
            // Filter out URLs without leading http, we cannot work with relative URLs yet.
            Matcher m = Utils.findImageUrlsPattern.matcher(html.substring(i, html.length()));
            boolean found = m.find();
            if (found && m.group(1).startsWith("http")) {
                ret.add(m.group(1));
                i += m.group(1).length();
            } else if (found) {
                i++;
                continue;
            } else {
                i++;
                continue;
            }
        }
        return ret;
    }
    
}
