/*
 * Copyright (c) 2009 Matthias Kaeppler
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

package org.ttrssreader.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

/**
 * Implements a cache capable of caching image files. It exposes helper methods to immediately
 * access binary image data as {@link Bitmap} objects.
 * 
 * @author Matthias Kaeppler
 * 
 */
public class ImageCache extends AbstractCache<String, byte[]> {
    
    public ImageCache(int initialCapacity, long expirationInMinutes, int maxConcurrentThreads) {
        super("ImageCache", initialCapacity, expirationInMinutes, maxConcurrentThreads);
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
            
            this.diskCacheDir = Environment.getExternalStorageDirectory() + File.separator + Utils.SDCARD_PATH_CACHE;
            File outFile = new File(diskCacheDir);
            outFile.mkdirs();
            
            isDiskCacheEnabled = outFile.exists();
            
            // Create .nomedia File in Cache-Folder so android doesn't generate thumbnails
            File nomediaFile = new File(this.diskCacheDir + File.separator + ".nomedia");
            if (!nomediaFile.exists()) {
                try {
                    nomediaFile.createNewFile();
                } catch (IOException e) {
                    Log.e(Utils.TAG, "Couldn't create File: " + nomediaFile.getAbsolutePath());
                }
            }
        }
        
        if (!isDiskCacheEnabled) {
            Log.w(Utils.TAG, "Failed creating disk cache directory " + diskCacheDir);
        }
        
        return isDiskCacheEnabled;
    }
    
    @Override
    public String getFileNameForKey(String imageUrl) {
        return imageUrl.replaceAll("[.:/,%?&=]", "+").replaceAll("[+]+", "+");
    }
    
    @Override
    protected byte[] readValueFromDisk(File file) throws IOException {
        BufferedInputStream istream = new BufferedInputStream(new FileInputStream(file));
        long fileSize = file.length();
        if (fileSize > Integer.MAX_VALUE) {
            throw new IOException("Cannot read files larger than " + Integer.MAX_VALUE + " bytes");
        }
        
        int imageDataLength = (int) fileSize;
        
        byte[] imageData = new byte[imageDataLength];
        istream.read(imageData, 0, imageDataLength);
        istream.close();
        
        return imageData;
    }
    
    public synchronized Bitmap getBitmap(Object elementKey) {
        byte[] imageData = super.get(elementKey);
        if (imageData == null) {
            return null;
        }
        return BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
    }
    
    @Override
    protected void writeValueToDisk(BufferedOutputStream ostream, byte[] imageData) throws IOException {
        ostream.write(imageData);
    }
    
    public File getCacheFile(String key) {
        File f = new File(diskCacheDir);
        if (!f.exists()) {
            f.mkdirs();
        }
        return new File(diskCacheDir + "/" + getFileNameForKey(key));
    }
    
}
