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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.gui.interfaces.ICacheEndListener;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.utils.FileDateComparator;
import org.ttrssreader.utils.ImageCache;
import org.ttrssreader.utils.StringSupport;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ImageCacher extends AsyncTask<Void, Integer, Void> {
    
    private static final long maxFileSize = 1024 * 1024 * 6; // Max size for one image is 6 MB
    private static final int DOWNLOAD_IMAGES_THREADS = 4;
    private static final int DEFAULT_TASK_COUNT = 3;
    
    private ICacheEndListener parent;
    private Context context;
    
    private boolean onlyArticles;
    private boolean onlyUnreadImages;
    private boolean onlyUnreadArticles;
    private long cacheSizeMax;
    private ImageCache imageCache;
    private long folderSize;
    private long downloaded = 0;
    private int taskCount = 0;
    
    public ImageCacher(ICacheEndListener parent, Context context, boolean onlyArticles) {
        this.parent = parent;
        this.context = context;
        this.onlyArticles = onlyArticles;
        this.onlyUnreadArticles = Controller.getInstance().isArticleCacheUnread();
    }
    
    @Override
    protected Void doInBackground(Void... params) {
        long start = System.currentTimeMillis();
        
        while (true) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (!Utils.checkConnected(cm)) {
                Log.e(Utils.TAG, "Error: No connectivity, aborting...");
                break;
            }
            
            // Update all articles
            long timeArticles = System.currentTimeMillis();
            // Only use progress-updates and callbacks for downloading articles, images are done in background
            // completely
            Set<Category> cats = DBHelper.getInstance().getCategoriesIncludingUncategorized();
            Set<Feed> labels = DBHelper.getInstance().getFeeds(-2);
            taskCount = DEFAULT_TASK_COUNT + cats.size() + labels.size();
            
            int progress = 0;
            Data.getInstance().updateCounters(true);
            publishProgress(++progress); // Move progress forward
            Data.getInstance().updateCategories(true);
            publishProgress(++progress); // Move progress forward
            Data.getInstance().updateFeeds(Data.VCAT_ALL, true);
            
            for (Category c : cats) {
                if (c.unread == 0 && onlyUnreadArticles)
                    continue;
                publishProgress(++progress); // Move progress forward
                Data.getInstance().updateArticles(c.id, onlyUnreadArticles, true, true);
            }
            
            for (Feed f : labels) {
                if (f.unread == 0 && onlyUnreadArticles)
                    continue;
                publishProgress(++progress); // Move progress forward
                Data.getInstance().updateArticles(f.id, onlyUnreadArticles, false);
            }
            
            publishProgress(taskCount); // Move progress forward
            Log.i(Utils.TAG, "Updating articles took " + (System.currentTimeMillis() - timeArticles) + "ms");
            
            if (onlyArticles) // We are done here..
                break;
            
            // Initialize other preferences
            this.cacheSizeMax = Controller.getInstance().getImageCacheSize() * 1048576;
            this.onlyUnreadImages = Controller.getInstance().isImageCacheUnread();
            this.imageCache = Controller.getInstance().getImageCache(context);
            if (imageCache == null)
                break;
            
            imageCache.fillMemoryCacheFromDisk();
            downloadImages();
            purgeCache();
            
            Log.i(Utils.TAG, String.format("Cache: %s MB (Limit: %s MB, took %s seconds)", folderSize / 1048576,
                    cacheSizeMax / 1048576, (System.currentTimeMillis() - start) / 1000));
            break; // Always break in the end, "while" is just useful for the different places in which we leave the
                   // loop
        }
        
        handler.sendEmptyMessage(0);
        return null;
    }
    
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (parent != null)
                parent.onCacheEnd();
        }
    };
    
    @Override
    protected void onProgressUpdate(Integer... values) {
        if (values[0] == taskCount)
            return;
        if (parent != null)
            parent.onCacheProgress(taskCount, values[0]);
    }
    
    private void downloadImages() {
        long time = System.currentTimeMillis();
        DownloadImageTask[] tasks = new DownloadImageTask[DOWNLOAD_IMAGES_THREADS];
        
        Cursor c = DBHelper.getInstance().queryArticlesForImageCache(onlyUnreadImages);
        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                // Get images included in HTML
                Set<String> set = new HashSet<String>();
                
                for (String url : findAllImageUrls(c.getString(1))) {
                    if (!imageCache.containsKey(url))
                        set.add(url);
                }
                
                // Get images from attachments separately
                for (String url : c.getString(2).split(";")) {
                    for (String ext : Utils.IMAGE_EXTENSIONS) {
                        if (url.toLowerCase().contains("." + ext) && !imageCache.containsKey(url)) {
                            set.add(url);
                            break;
                        }
                    }
                }
                
                assignTask(tasks, c.getInt(0), StringSupport.setToArray(set));
                
                if (downloaded > cacheSizeMax) {
                    Log.w(Utils.TAG, "Stopping download, downloaded data exceeds cache-size-limit from options.");
                    break;
                }
                c.move(1);
            }
        }
        c.close();
        
        // Wait for all tasks to finish and retrieve results
        boolean tasksRunning = true;
        while (tasksRunning) {
            tasksRunning = false;
            synchronized (this) {
                try {
                    wait(200);
                } catch (InterruptedException e) {
                }
            }
            for (int i = 0; i < DOWNLOAD_IMAGES_THREADS; i++) {
                DownloadImageTask t = tasks[i];
                retrieveResult(t);
                if (t != null && t.getStatus().equals(AsyncTask.Status.RUNNING)) {
                    tasksRunning = true;
                }
            }
        }
        
        Log.i(Utils.TAG, "Downloading images took " + (System.currentTimeMillis() - time) + "ms");
    }
    
    private void purgeCache() {
        long time = System.currentTimeMillis();
        File cacheFolder = new File(imageCache.getDiskCacheDirectory());
        
        folderSize = 0;
        for (File f : cacheFolder.listFiles()) {
            folderSize += f.length();
        }
        
        if (folderSize > cacheSizeMax) {
            Log.i(Utils.TAG, String.format("Before - Cache: %s bytes (Limit: %s bytes)", folderSize, cacheSizeMax));
            
            // Sort list of files by last access date
            List<File> list = Arrays.asList(cacheFolder.listFiles());
            Collections.sort(list, new FileDateComparator());
            
            int i = 0;
            while (folderSize > cacheSizeMax) {
                File f = list.get(i++);
                folderSize -= f.length();
                f.delete();
            }
            Log.i(Utils.TAG, String.format("After - Cache: %s bytes (Limit: %s bytes)", folderSize, cacheSizeMax));
        }
        Log.i(Utils.TAG, "Purging cache took " + (System.currentTimeMillis() - time) + "ms");
    }
    
    /**
     * Returns true if all downloads for this Task have been successful.
     * 
     * @param t
     * @return
     */
    private boolean retrieveResult(DownloadImageTask t) {
        boolean ret = false;
        
        if (t != null && t.getStatus().equals(AsyncTask.Status.FINISHED)) {
            try {
                downloaded += t.get();
                if (t.allOK)
                    ret = true;
                
                t = null; // Make sure tasks[i] is null
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return ret;
    }
    
    private void assignTask(DownloadImageTask[] tasks, int articleId, String... urls) {
        if (urls.length == 0)
            return;
        
        if (tasks == null)
            tasks = new DownloadImageTask[DOWNLOAD_IMAGES_THREADS];
        
        boolean done = false;
        while (!done) {
            
            // Start new Task if task-slot is available
            for (int i = 0; i < DOWNLOAD_IMAGES_THREADS; i++) {
                DownloadImageTask t = tasks[i];
                
                // Retrieve result (downloaded size) and reset task
                if (retrieveResult(t))
                    DBHelper.getInstance().updateArticleCachedImages(articleId, true);
                
                // Assign new task if possible
                if (t == null) {
                    t = new DownloadImageTask(imageCache, maxFileSize);
                    t.execute(urls);
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
    
    /**
     * Searches for cached versions of the given image and returns the local URL to access the file.
     * 
     * @param url
     *            the original URL
     * @return the local URL or null if no image in cache or if the file couldn't be found.
     */
    public static String getCachedImageUrl(String url) {
        ImageCache cache = Controller.getInstance().getImageCache(null);
        if (cache != null && cache.containsKey(url)) {
            
            StringBuffer sb = new StringBuffer();
            sb.append(cache.getDiskCacheDirectory());
            sb.append(File.separator);
            sb.append(cache.getFileNameForKey(url));
            
            File file = new File(sb.toString());
            
            if (file.exists()) {
                sb.insert(0, "file://"); // Add "file:" at the beginning..
                return sb.toString();
            } else {
                Log.w(Utils.TAG, "File " + sb.toString() + " is in cache but does not exist.");
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
    public static Set<String> findAllImageUrls(String html) {
        Set<String> ret = new LinkedHashSet<String>();
        if (html == null || html.length() < 10)
            return ret;
        
        for (int i = 0; i < html.length();) {
            i = html.indexOf("<img", i);
            if (i == -1)
                break;
            
            Matcher m = Utils.findImageUrlsPattern.matcher(html.substring(i, html.length()));
            
            // Filter out URLs without leading http, we cannot work with relative URLs yet.
            boolean found = m.find();
            if (found && m.group(1).startsWith("http")) {
                ret.add(m.group(1));
                i += m.group(1).length();
                Log.w(Utils.TAG, "URL found: " + m.group(1));
            } else if (found) {
                Log.w(Utils.TAG, "Relative URL found: " + m.group(1) + " -> i=" + i);
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
