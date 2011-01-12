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
import org.ttrssreader.utils.ImageCache;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class ImageCacher implements ICacheable {
    
    private long maxSize = 1024 * 1024 * 6; // Max size for one image is 6 MB
    
    private Context context;
    
    public ImageCacher(Context context) {
        this.context = context;
    }
    
    @Override
    public void cache() {
        long start = System.currentTimeMillis();
        long sizeMax = Controller.getInstance().getImageCacheSize() * 1048576;
        ImageCache cache = Controller.getInstance().getImageCache(context);
        if (cache == null)
            return;
        
        Log.w(Utils.TAG, "Updating images in cache...");
        cache.fillMemoryCacheFromDisk();
        
        // Read from DB
        Cursor c = DBHelper.getInstance().query(DBHelper.TABLE_ARTICLES, new String[] { "content", "attachments" },
                null, null, null, null, null);
        if (c.moveToFirst()) {
            long downloaded = 0;
            
            while (!c.isAfterLast()) {
                
                // Get images which are included in HTML
                for (String url : Utils.findAllImageUrls(c.getString(0))) {
                    if (!cache.containsKey(url)) {
                        downloaded += Utils.downloadToFile(url, cache.getCacheFile(url), maxSize);
                    }
                }
                
                // Get images from attachments separately
                for (String url : c.getString(1).split(";")) {
                    for (String ext : Utils.IMAGE_EXTENSIONS) {
                        if (url.toLowerCase().contains(ext) && !cache.containsKey(url)) {
                            downloaded += Utils.downloadToFile(url, cache.getCacheFile(url), maxSize);
                            break;
                        }
                    }
                }
                
                if (downloaded > sizeMax) {
                    Log.w(Utils.TAG, "Stopping download, downloaded data exceeds cache-size-limit from options.");
                    break;
                }
                
                c.move(1);
            }
        }
        c.close();
        
        int half = (int) (System.currentTimeMillis() - start) / 1000;
        Log.w(Utils.TAG, "Downloading finished after " + half + "s, starting cache-shrinking...");
        
        // ** FINIHSED UPDATING, NOW SHRINKING CACHE **
        File cacheFolder = new File(cache.getDiskCacheDirectory());
        long size = Utils.getFolderSize(cacheFolder);
        
        if (size > sizeMax) {
            Log.d(Utils.TAG, String.format("Cache: %s MB (Limit: %s MB)", size / 1048576, sizeMax / 1048576));
            
            // Delete old images (86400000 = 24 * 60 * 60 * 1000)
            long maxAge = System.currentTimeMillis() - (Controller.getInstance().getImageCacheAge() * 86400000);
            File[] fArray = cacheFolder.listFiles();
            for (File f : fArray) {
                if (f.lastModified() < maxAge) {
                    f.delete();
                }
            }
            
            // Refresh size and check again
            size = Utils.getFolderSize(cacheFolder);
            if (size > sizeMax) {
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
                while (size > sizeMax) {
                    File fileDelete = list.get(i++);
                    tempSize = fileDelete.length();
                    if (fileDelete.delete()) {
                        Log.d(Utils.TAG, String.format("Cache: %s bytes, Limit: %s bytes, Deleting: %s bytes", size,
                                sizeMax, tempSize));
                        size -= tempSize;
                    }
                }
            }
        }
        
        int time = (int) (System.currentTimeMillis() - start) / 1000;
        String content = String.format("Cache: %s MB (Limit: %s MB, took %s seconds)", size / 1048576,
                sizeMax / 1048576, time);
        Utils.showNotification(content, time, false, context);
        Log.w(Utils.TAG, content);
    }
}
