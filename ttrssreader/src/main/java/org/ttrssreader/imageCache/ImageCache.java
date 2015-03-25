/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 Nils Braden
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

package org.ttrssreader.imageCache;

import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.AbstractCache;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Implements a cache capable of caching image files. It exposes helper methods to immediately
 * access binary image data as {@link Bitmap} objects.
 *
 * @author Matthias Kaeppler
 * @author Nils Braden (modified some stuff)
 */
public class ImageCache extends AbstractCache<String, byte[]> {

    private static final String TAG = ImageCache.class.getSimpleName();

    public ImageCache(int initialCapacity, String cacheDir) {
        super(initialCapacity, 1);
        this.diskCacheDir = cacheDir;
    }

    /**
     * Enable caching to the phone's SD card.
     */
    public boolean enableDiskCache() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            // Use configured output directory
            File folder = new File(diskCacheDir);

            if (!folder.exists() && !folder.mkdirs()) {
                // Folder could not be created, fallback to internal directory on sdcard
                // Path: /sdcard/Android/data/org.ttrssreader/cache/
                diskCacheDir = Constants.CACHE_FOLDER_DEFAULT;
                folder = new File(diskCacheDir);
            }

            if (!folder.exists() && !folder.mkdirs()) {
                Log.w(TAG, "Couldn't create Folder for Disk-Cache!");
                isDiskCacheEnabled = false;
            } else {
                isDiskCacheEnabled = folder.exists();
            }

            // Create .nomedia File in Cache-Folder so android doesn't generate thumbnails
            File nomediaFile = new File(diskCacheDir + File.separator + ".nomedia");
            if (!nomediaFile.exists()) {
                try {
                    if (!nomediaFile.createNewFile())
                        Log.w(TAG, "Couldn't create .nomedia File for Disk-Cache!");
                } catch (IOException e) {
                    // Empty!
                }
            }
        }

        if (!isDiskCacheEnabled)
            Log.e(TAG, "Failed creating disk cache directory " + diskCacheDir);

        return isDiskCacheEnabled;
    }

    public void fillMemoryCacheFromDisk() {
        byte[] b = new byte[]{};
        File folder = new File(diskCacheDir);
        File[] files = folder.listFiles();

        Log.d(TAG, "Image cache before fillMemoryCacheFromDisk: " + cache.size());
        if (files == null)
            return;

        for (File file : files) {
            try {
                cache.put(file.getName(), b);
            } catch (RuntimeException e) {
                Log.e(TAG, "Runtime Exception while doing fillMemoryCacheFromDisk: " + e.getMessage());
            }
        }
        Log.d(TAG, "Image cache after fillMemoryCacheFromDisk: " + cache.size());
    }

    boolean containsKey(String key) {
        return cache.containsKey(getFileNameForKey(key)) || (isDiskCacheEnabled && getCacheFile(key).exists());
    }

    /**
     * create uniq string from file url, which can be used as file name
     *
     * @param imageUrl URL of given image
     * @return calculated hash
     */
    public static String getHashForKey(String imageUrl) {
        return imageUrl.replaceAll("[:;#~%$\"!<>|+*\\()^/,%?&=]+", "+");
    }

    @Override
    public String getFileNameForKey(String imageUrl) {
        return getHashForKey(imageUrl);
    }

    public File getCacheFile(String key) {
        File f = new File(diskCacheDir);
        if (!f.exists() && !f.mkdirs())
            Log.w(TAG, "Couldn't create File: " + f.getAbsolutePath());

        return getFileForKey(key);
    }

    @Override
    protected byte[] readValueFromDisk(File file) throws IOException {
        return null;
    }

    @Override
    protected void writeValueToDisk(BufferedOutputStream ostream, byte[] value) {
    }

}
