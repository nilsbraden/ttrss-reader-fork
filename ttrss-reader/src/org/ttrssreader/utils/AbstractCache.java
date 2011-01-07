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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.google.common.collect.MapMaker;

/**
 * <p>
 * A simple 2-level cache consisting of a small and fast in-memory cache (1st level cache) and an (optional) slower but
 * bigger disk cache (2nd level cache). For disk caching, either the application's cache directory or the SD card can be
 * used. Please note that in the case of the app cache dir, Android may at any point decide to wipe that entire
 * directory if it runs low on internal storage. The SD card cache <i>must</i> be managed by the application, e.g. by
 * calling {@link #wipe} whenever the app quits.
 * </p>
 * <p>
 * When pulling from the cache, it will first attempt to load the data from memory. If that fails, it will try to load
 * it from disk (assuming disk caching is enabled). If that succeeds, the data will be put in the in-memory cache and
 * returned (read-through). Otherwise it's a cache miss.
 * </p>
 * <p>
 * Pushes to the cache are always write-through (i.e. the data will be stored both on disk, if disk caching is enabled,
 * and in memory).
 * </p>
 * 
 * @author Matthias Kaeppler
 */
public abstract class AbstractCache<KeyT, ValT> implements Map<KeyT, ValT> {
    
    public static final int DISK_CACHE_INTERNAL = 0;
    public static final int DISK_CACHE_SDCARD = 1;
    
    protected boolean isDiskCacheEnabled;
    
    protected String diskCacheDir;
    
    protected ConcurrentMap<KeyT, ValT> cache;
    
    private String name;
    
    /**
     * Creates a new cache instance.
     * 
     * @param name
     *            a human readable identifier for this cache. Note that this value will be used to
     *            derive a directory name if the disk cache is enabled, so don't get too creative
     *            here (camel case names work great)
     * @param initialCapacity
     *            the initial element size of the cache
     * @param expirationInMinutes
     *            time in minutes after which elements will be purged from the cache (NOTE: this
     *            only affects the memory cache, the disk cache does currently NOT handle element
     *            TTLs!)
     * @param maxConcurrentThreads
     *            how many threads you think may at once access the cache; this need not be an exact
     *            number, but it helps in fragmenting the cache properly
     */
    public AbstractCache(String name, int initialCapacity, long expirationInMinutes, int maxConcurrentThreads) {
        
        MapMaker mapMaker = new MapMaker();
        mapMaker.initialCapacity(initialCapacity);
        mapMaker.expiration(expirationInMinutes * 60, TimeUnit.SECONDS);
        mapMaker.concurrencyLevel(maxConcurrentThreads);
        mapMaker.softValues();
        this.cache = mapMaker.makeMap();
    }
    
    /**
     * Enable caching to the phone's internal storage or SD card.
     * 
     * @param context
     *            the current context
     * @param storageDevice
     *            where to store the cached files, either {@link #DISK_CACHE_INTERNAL} or {@link #DISK_CACHE_SDCARD})
     * @return
     */
    public boolean enableDiskCache(Context context, int storageDevice) {
        Context appContext = context.getApplicationContext();
        
        String rootDir = null;
        if (storageDevice == DISK_CACHE_SDCARD
                && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            // SD-card available
            rootDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + appContext.getPackageName();
        } else {
            rootDir = appContext.getCacheDir().getAbsolutePath();
        }
        
        this.diskCacheDir = rootDir + File.separator + StringSupport.underscore(name.replaceAll("\\s", ""));
        File outFile = new File(diskCacheDir);
        outFile.mkdirs();
        
        isDiskCacheEnabled = outFile.exists();
        
        if (!isDiskCacheEnabled) {
            Log.w(Utils.TAG, "Failed creating disk cache directory " + diskCacheDir);
        }
        
        return isDiskCacheEnabled;
    }
    
    /**
     * Only meaningful if disk caching is enabled. See {@link #enableDiskCache}.
     * 
     * @return the full absolute path to the directory where files are cached, if the disk cache is
     *         enabled, otherwise null
     */
    public String getDiskCacheDirectory() {
        return diskCacheDir;
    }
    
    /**
     * Only meaningful if disk caching is enabled. See {@link #enableDiskCache}. Turns a cache key
     * into the file name that will be used to persist the value to disk. Subclasses must implement
     * this.
     * 
     * @param key
     *            the cache key
     * @return the file name
     */
    public abstract String getFileNameForKey(KeyT key);
    
    /**
     * Only meaningful if disk caching is enabled. See {@link #enableDiskCache}. Restores a value
     * previously persisted to the disk cache.
     * 
     * @param file
     *            the file holding the cached value
     * @return the cached value
     * @throws IOException
     */
    protected abstract ValT readValueFromDisk(File file) throws IOException;
    
    /**
     * Only meaningful if disk caching is enabled. See {@link #enableDiskCache}. Persists a value to
     * the disk cache.
     * 
     * @param ostream
     *            the file output stream (buffered).
     * @param value
     *            the cache value to persist
     * @throws IOException
     */
    protected abstract void writeValueToDisk(BufferedOutputStream ostream, ValT value) throws IOException;
    
    private void cacheToDisk(KeyT key, ValT value) {
        File file = new File(diskCacheDir + "/" + getFileNameForKey(key));
        try {
            file.createNewFile();
            file.deleteOnExit();
            
            BufferedOutputStream ostream = new BufferedOutputStream(new FileOutputStream(file));
            
            writeValueToDisk(ostream, value);
            
            ostream.close();
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private File getFileForKey(KeyT key) {
        return new File(diskCacheDir + "/" + getFileNameForKey(key));
    }
    
    /**
     * Reads a value from the cache by probing the in-memory cache, and if enabled and the in-memory
     * probe was a miss, the disk cache.
     * 
     * @param elementKey
     *            the cache key
     * @return the cached value, or null if element was not cached
     */
    @SuppressWarnings("unchecked")
    public synchronized ValT get(Object elementKey) {
        KeyT key = (KeyT) elementKey;
        ValT value = cache.get(key);
        if (value != null) {
            // memory hit
            Log.d(name, "MEM cache hit for " + key.toString());
            return value;
        }
        
        // memory miss, try reading from disk
        File file = getFileForKey(key);
        if (file.exists()) {
            // disk hit
            Log.d(name, "DISK cache hit for " + key.toString());
            try {
                value = readValueFromDisk(file);
            } catch (IOException e) {
                // treat decoding errors as a cache miss
                e.printStackTrace();
                return null;
            }
            if (value == null) {
                return null;
            }
            cache.put(key, value);
            return value;
        }
        
        // cache miss
        return null;
    }
    
    /**
     * Writes an element to the cache. NOTE: If disk caching is enabled, this will write through to
     * the disk, which may introduce a performance penalty.
     */
    public synchronized ValT put(KeyT key, ValT value) {
        if (isDiskCacheEnabled) {
            cacheToDisk(key, value);
        }
        
        return cache.put(key, value);
    }
    
    public synchronized void putAll(Map<? extends KeyT, ? extends ValT> t) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Checks if a value is present in the cache. If the disk cached is enabled, this will also
     * check whether the value has been persisted to disk.
     * 
     * @param key
     *            the cache key
     * @return true if the value is cached in memory or on disk, false otherwise
     */
    @SuppressWarnings("unchecked")
    public synchronized boolean containsKey(Object key) {
        return cache.containsKey(key) || (isDiskCacheEnabled && getFileForKey((KeyT) key).exists());
    }
    
    /**
     * Checks if a value is present in the in-memory cache. This method ignores the disk cache.
     * 
     * @param key
     *            the cache key
     * @return true if the value is currently hold in memory, false otherwise
     */
    public synchronized boolean containsKeyInMemory(Object key) {
        return cache.containsKey(key);
    }
    
    /**
     * Checks if the given value is currently hold in memory.
     */
    public synchronized boolean containsValue(Object value) {
        return cache.containsValue(value);
    }
    
    @SuppressWarnings("unchecked")
    public synchronized ValT remove(Object key) {
        ValT value = cache.remove(key);
        
        if (isDiskCacheEnabled) {
            File cachedValue = getFileForKey((KeyT) key);
            if (cachedValue.exists()) {
                cachedValue.delete();
            }
        }
        
        return value;
    }
    
    public Set<KeyT> keySet() {
        return cache.keySet();
    }
    
    public Set<Map.Entry<KeyT, ValT>> entrySet() {
        return cache.entrySet();
    }
    
    public synchronized int size() {
        return cache.size();
    }
    
    public synchronized boolean isEmpty() {
        return cache.isEmpty();
    }
    
    public synchronized void clear() {
        cache.clear();
        
        if (isDiskCacheEnabled) {
            File[] cachedFiles = new File(diskCacheDir).listFiles();
            if (cachedFiles == null) {
                return;
            }
            for (File f : cachedFiles) {
                f.delete();
            }
        }
    }
    
    public Collection<ValT> values() {
        return cache.values();
    }
}
