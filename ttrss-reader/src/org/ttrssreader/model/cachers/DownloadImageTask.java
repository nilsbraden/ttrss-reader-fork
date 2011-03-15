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

import org.ttrssreader.utils.ImageCache;
import org.ttrssreader.utils.Utils;
import android.os.AsyncTask;

public class DownloadImageTask extends AsyncTask<String, Void, Long> {
    
    private ImageCache imageCache;
    private long maxFileSize;
    
    public DownloadImageTask(ImageCache cache, long maxFileSize) {
        this.imageCache = cache;
        this.maxFileSize = maxFileSize;
    }
    
    @Override
    protected Long doInBackground(String... params) {
        long downloaded = 0;
        for (String url : params) {
            downloaded += Utils.downloadToFile(url, imageCache.getCacheFile(url), maxFileSize);
        }
        return downloaded;
    }
    
}
