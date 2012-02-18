/*
 * Copyright (c) 2009 Matthias Kaeppler
 * Modified 2010 by Nils Braden
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ttrssreader.imageCache;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.AbstractCache;
import org.ttrssreader.utils.Utils;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

/**
 * Implements a cache capable of caching image files. It exposes helper methods to immediately
 * access binary image data as {@link Bitmap} objects.
 * 
 * @author Matthias Kaeppler
 * @author Nils Braden (modified some stuff)
 */
public class ImageCache extends AbstractCache<String, byte[]> {
    
    public ImageCache(int initialCapacity) {
        super("ImageCache", initialCapacity, 1);
    }
    
    /**
     * Enable caching to the phone's SD card.
     * 
     * @param context
     *            the current context
     * @return
     */
    public boolean enableDiskCache() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            
            // Use configured output directory
            diskCacheDir = Controller.getInstance().cacheFolder();
            File folder = new File(diskCacheDir);
            
            if (!folder.exists()) {
                if (!folder.mkdirs()) {
                    // Folder could not be created, fallback to internal directory on sdcard
                    // Path: /sdcard/Android/data/org.ttrssreader/cache/
                    diskCacheDir = Constants.CACHE_FOLDER_DEFAULT;
                    folder = new File(diskCacheDir);
                }
            }
            
            if (!folder.exists())
                folder.mkdirs();
            
            isDiskCacheEnabled = folder.exists();
            
            // Create .nomedia File in Cache-Folder so android doesn't generate thumbnails
            File nomediaFile = new File(diskCacheDir + File.separator + ".nomedia");
            if (!nomediaFile.exists()) {
                try {
                    nomediaFile.createNewFile();
                } catch (IOException e) {
                }
            }
        }
        
        if (!isDiskCacheEnabled)
            Log.e(Utils.TAG, "Failed creating disk cache directory " + diskCacheDir);
        
        return isDiskCacheEnabled;
    }
    
    public void fillMemoryCacheFromDisk() {
        byte[] b = new byte[] {};
        File folder = new File(diskCacheDir);
        File[] files = folder.listFiles();
        
        if (files == null)
            return;
        
        for (File file : files) {
            try {
                cache.put(file.getName(), b);
            } catch (RuntimeException e) {
                Log.d(Utils.TAG, "Runtime Exception while doing fillMemoryCacheFromDisk: " + e.getMessage());
            }
        }
    }
    
    public boolean containsKey(String key) {
        if (cache.containsKey(getFileNameForKey(key)))
            return true;
        
        return (isDiskCacheEnabled && getCacheFile((String) key).exists());
    }
    
    @Override
    public String getFileNameForKey(String imageUrl) {
        return imageUrl.replaceAll("[:;#~%$\"!<>|+*\\()^/,%?&=]", "+").replaceAll("[+]+", "+");
    }
    
    public File getCacheFile(String key) {
        File f = new File(diskCacheDir);
        if (!f.exists())
            f.mkdirs();
        
        return new File(diskCacheDir + "/" + getFileNameForKey(key));
    }
    
    @Override
    protected byte[] readValueFromDisk(File file) throws IOException {
        return null;
    }
    
    @Override
    protected void writeValueToDisk(BufferedOutputStream ostream, byte[] value) throws IOException {
    }
    
}
