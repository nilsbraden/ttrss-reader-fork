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
    
    private ImageCache cache;
    private Context context;
    
    public ImageCacheUpdater(Context context) {
        this.context = context;
    }
    
    @Override
    public void update() {
        cache = Controller.getInstance().getImageCache(context);
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
                    Log.d(Utils.TAG, a.getTitle() + " - " + s);
                    
                    File file = cache.getCacheFile(s);
                    
                    if (Utils.getCachedImageUrl(s) == null) {
                        Utils.downloadImage(s, file);
                    }
                }
                
            }
        }
        
        // Shrink cache to size set in options
        File folder = new File(cache.getDiskCacheDirectory());
        long size = Utils.getFolderSize(folder);
        long sizeOption = Controller.getInstance().getImageCacheSize() * 1024 * 1024;
        
        Log.d(Utils.TAG, "Local cache size: " + size + " (Limit: " + sizeOption + ")");
        if (size > sizeOption) {
            List<File> l = Arrays.asList(folder.listFiles());
            
            Collections.sort(l, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    Long l1 = f1.lastModified();
                    Long l2 = f2.lastModified();
                    return l1.compareTo(l2);
                }
            });
            
            int i = 0;
            while (size > sizeOption) {
                File fileDelete = l.get(i++);
                long tempSize = fileDelete.length();
                if (fileDelete.delete()) {
                    size -= tempSize; 
                }
            }
        }
        Log.d(Utils.TAG, "Local cache size: " + size + " (Limit: " + sizeOption + ")");
        
    }
    
}
