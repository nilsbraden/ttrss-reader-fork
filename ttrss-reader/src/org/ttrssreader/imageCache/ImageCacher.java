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
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.gui.interfaces.ICacheEndListener;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.utils.FileDateComparator;
import org.ttrssreader.utils.FileUtils;
import org.ttrssreader.utils.StringSupport;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class ImageCacher extends AsyncTask<Void, Integer, Void> {
    
    private static final int DEFAULT_TASK_COUNT = 3;
    
    private ICacheEndListener parent;
    private Context context;
    
    private boolean onlyArticles;
    private boolean onlyUnreadImages;
    private long cacheSizeMax;
    private ImageCache imageCache;
    private long folderSize;
    private long downloaded = 0;
    private int taskCount = 0;
    
    Handler handler;
    long start;
    Map<Integer, DownloadImageTask> map;
    
    public ImageCacher(ICacheEndListener parent, Context context, boolean onlyArticles) {
        this.parent = parent;
        this.context = context;
        this.onlyArticles = onlyArticles;
        
        // Create Handler in a new Thread so all tasks are started in this new thread instead of the main UI-Thread
        MyHandler.start();
    }
    
    public Thread MyHandler = new Thread() {
        // Source: http://mindtherobot.com/blog/159/android-guts-intro-to-loopers-and-handlers/
        @Override
        public void run() {
            try {
                // preparing a looper on current thread
                // the current thread is being detected implicitly
                Looper.prepare();
                
                // now, the handler will automatically bind to the
                // Looper that is attached to the current thread
                // You don't need to specify the Looper explicitly
                handler = new Handler();
                
                // After the following line the thread will start
                // running the message loop and will not normally
                // exit the loop unless a problem happens or you
                // quit() the looper (see below)
                Looper.loop();
            } catch (Throwable t) {
                Log.e(Utils.TAG, "halted due to an error", t);
            }
        }
    };
    
    @Override
    protected Void doInBackground(Void... params) {
        start = System.currentTimeMillis();
        
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
            Set<Feed> labels = DBHelper.getInstance().getFeeds(-2);
            taskCount = DEFAULT_TASK_COUNT + labels.size() + 1; // 1 for the caching of all articles
            
            int progress = 0;
            Data.getInstance().updateCounters(true);
            publishProgress(++progress); // Move progress forward
            Data.getInstance().updateCategories(true);
            publishProgress(++progress); // Move progress forward
            Data.getInstance().updateFeeds(Data.VCAT_ALL, true);
            
            // Cache all articles
            publishProgress(++progress);
            Data.getInstance().cacheArticles(false);
            
            for (Feed f : labels) {
                if (f.unread == 0)
                    continue;
                publishProgress(++progress); // Move progress forward
                Data.getInstance().updateArticles(f.id, true, false, false);
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
        
        // Call parent from another Thread to avoid Exception.
        // CalledFromWrongThreadException: Only the original thread that created a view hierarchy can touch its views.
        serviceHandler.sendEmptyMessage(0);
        return null;
    }
    
    // Only used to inform parent about the status when finished...
    private Handler serviceHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (parent != null)
                parent.onCacheEnd();
        }
    };
    
    /**
     * Calls the parent method to update the progress-bar in the UI while articles are refreshed. This is not called
     * anymore from the moment on when the image-downloading starts.
     */
    @Override
    protected void onProgressUpdate(Integer... values) {
        if (parent != null)
            parent.onCacheProgress(taskCount, values[0]);
    }
    
    protected void downloadFinished(int articleId, Long size, boolean ok) {
        synchronized (map) {
            // Add size to overall download-sum and remove job from map
            if (size > 0)
                downloaded += size;
            
            map.remove(articleId);
            
            // Only mark images as "downloaded" when everything went fine
            if (ok)
                DBHelper.getInstance().updateArticleCachedImages(articleId, true);
            
            // Log.d(Utils.TAG, "Download finished. articleId: " + articleId);
            map.notifyAll();
        }
    }
    
    private void downloadImages() {
        long time = System.currentTimeMillis();
        // DownloadImageTask[] tasks = new DownloadImageTask[DOWNLOAD_IMAGES_THREADS];
        map = new HashMap<Integer, ImageCacher.DownloadImageTask>();
        
        Cursor c = DBHelper.getInstance().queryArticlesForImageCache(onlyUnreadImages);
        if (c != null && c.moveToFirst()) {
            while (!c.isAfterLast()) {
                // Get images included in HTML
                Set<String> set = new HashSet<String>();
                
                for (String url : findAllImageUrls(c.getString(1), c.getInt(0))) {
                    if (!imageCache.containsKey(url))
                        set.add(url);
                }
                
                // Get images from attachments separately
                for (String url : c.getString(2).split(";")) {
                    for (String ext : FileUtils.IMAGE_EXTENSIONS) {
                        if (url.toLowerCase().contains("." + ext) && !imageCache.containsKey(url)) {
                            set.add(url);
                            break;
                        }
                    }
                }
                
                if (set.size() > 0) {
                    ImageCacher.DownloadImageTask task = new ImageCacher.DownloadImageTask(imageCache, c.getInt(0),
                            StringSupport.setToArray(set));
                    handler.post(task);
                    map.put(c.getInt(0), task);
                }
                
                if (downloaded > cacheSizeMax) {
                    Log.w(Utils.TAG, "Stopping download, downloaded data exceeds cache-size-limit from options.");
                    break;
                }
                c.move(1);
            }
        }
        c.close();
        
        while (!map.isEmpty()) {
            synchronized (map) {
                try {
                    map.wait(1000);
                } catch (InterruptedException e) {
                    Log.d(Utils.TAG, "Got an InterruptedException!");
                }
            }
        }
        
        Log.i(Utils.TAG, "Downloading images took " + (System.currentTimeMillis() - time) + "ms");
    }
    
    public class DownloadImageTask implements Runnable {
        
        private static final long maxFileSize = 1024 * 1024 * 6; // Max size for one image is 6 MB
        
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
            // Log.i(Utils.TAG, String.format("Before - Cache: %s bytes (Limit: %s bytes)", folderSize, cacheSizeMax));
            
            // Sort list of files by last access date
            List<File> list = Arrays.asList(cacheFolder.listFiles());
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
            // Log.i(Utils.TAG, String.format("After - Cache: %s bytes (Limit: %s bytes)", folderSize, cacheSizeMax));
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
    public static Set<String> findAllImageUrls(String html, int articleId) {
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
            } else if (found) {
                i++;
                continue;
            } else {
                i++;
                continue;
            }
            
            // Log.i(Utils.TAG, ret.size() + " URL" + (ret.size() == 1 ? "" : "s") +" found for Article-ID " + articleId
            // + ".");
        }
        return ret;
    }
    
}
