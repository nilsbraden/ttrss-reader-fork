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

package org.ttrssreader.model.updaters;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.model.IUpdatable;
import org.ttrssreader.model.article.ArticleItem;
import org.ttrssreader.utils.ImageCache;
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.util.Log;

public class ImageCacheUpdater implements IUpdatable {
    
    private static final long maxSize = 1024 * 1024 * 1024;
    
    private ImageCache cache;
    private Context context;
    
    public ImageCacheUpdater(Context context) {
        this.context = context;
    }
    
    @Override
    public void update() {
        cache = Controller.getInstance().getImageCache(context);
        long start = System.currentTimeMillis();
        if (cache == null) {
            Log.e(Utils.TAG, "Could not update cache, Disk-Cache-Directory is not available.");
            return;
        }
        
        Log.d(Utils.TAG, "Updating images in cache...");
        Map<Integer, Set<ArticleItem>> map = DBHelper.getInstance().getArticles(true);
        for (Set<ArticleItem> set : map.values()) {
            for (ArticleItem a : set) {
                
                String content = a.getContent();
                for (String s : Utils.findAllImageUrls(content)) {
                    File file = cache.getCacheFile(s);
                    
                    if (Utils.getCachedImageUrl(s) == null) {
                        Utils.downloadToFile(s, file, maxSize);
                    }
                }
                
            }
        }
        
        // Shrink cache to size set in options
        File folder = new File(cache.getDiskCacheDirectory());
        long sizeMax = Controller.getInstance().getImageCacheSize() * 1024 * 1024;
        long size = Utils.getFolderSize(folder);
        
        Log.d(Utils.TAG, String.format("BEFORE Cache: %s MB (Limit: %s MB)", size / 1048576, sizeMax / 1048576));
        
        if (size > sizeMax) {
            List<File> list = Arrays.asList(folder.listFiles());
            
            // Delete old images (86400000 = 24 * 60 * 60 * 1000)
            long maxAge = System.currentTimeMillis() - Controller.getInstance().getImageCacheAge() * 86400000;
            for (File f : list) {
                if (f.lastModified() > maxAge) {
                    f.delete();
                }
            }
            
            // Refresh size and check again
            size = Utils.getFolderSize(folder);
            if (size > sizeMax) {
                // Sort list by last access date
                Collections.sort(list, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        return ((Long) f1.lastModified()).compareTo((Long) f2.lastModified());
                    }
                });
                
                // Delete files until size is fine with the settings
                int i = 0;
                while (size > sizeMax) {
                    File fileDelete = list.get(i++);
                    long tempSize = fileDelete.length();
                    if (fileDelete.delete()) {
                        size -= tempSize;
                    }
                }
            }
        }
        
        int time = (int) (System.currentTimeMillis() - start) / 1000;
        String content = String.format("Cache: %s MB (Limit: %s MB, took %s seconds)", size / 1048576,
                sizeMax / 1048576, time);
        Utils.showNotification(content, time, false, context);
        
        Log.d(Utils.TAG, String.format("AFTER  Cache: %s MB (Limit: %s MB, took %s seconds)", size / 1048576,
                sizeMax / 1048576, time));
    }
    
}
