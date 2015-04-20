/*
 * Copyright (c) 2015, Nils Braden
 *
 * This file is part of ttrss-reader-fork. This program is free software; you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation;
 * either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program; If
 * not, see http://www.gnu.org/licenses/.
 */

package org.ttrssreader.imageCache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;

import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.FileUtils;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Implements a cache capable of caching image files. It exposes helper methods to immediately
 * access binary image data as {@link Bitmap} objects.
 */
public class ImageCache {

	private static final String TAG = ImageCache.class.getSimpleName();

	protected boolean isDiskCacheEnabled;
	protected String diskCacheDir;
	protected Cache<String, Byte[]> cache;

	public ImageCache(int initialCapacity, String cacheDir) {
		this.diskCacheDir = cacheDir;
		this.cache = CacheBuilder.newBuilder().initialCapacity(initialCapacity).concurrencyLevel(1).softValues()
				.build(new CacheLoader<String, Byte[]>() {
					@Override
					public Byte[] load(String key) throws Exception {

						return null;
					}
				});
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
					if (!nomediaFile.createNewFile()) Log.w(TAG, "Couldn't create .nomedia File for Disk-Cache!");
				} catch (IOException e) {
					// Empty!
				}
			}
		}

		if (!isDiskCacheEnabled) Log.e(TAG, "Failed creating disk cache directory " + diskCacheDir);

		return isDiskCacheEnabled;
	}

	public void fillMemoryCacheFromDisk() {
		File folder = new File(diskCacheDir);
		File[] files = folder.listFiles();

		Log.d(TAG, "Image cache before fillMemoryCacheFromDisk: " + cache.size());
		if (files == null) return;

		for (File file : files) {
			try {
				cache.put(file.getName(), null);
			} catch (RuntimeException e) {
				Log.e(TAG, "Runtime Exception while doing fillMemoryCacheFromDisk: " + e.getMessage());
			}
		}
		Log.d(TAG, "Image cache after fillMemoryCacheFromDisk: " + cache.size());
	}

	boolean containsKey(String key) {
		return cache.getIfPresent(getFileNameForKey(key)) != null || isDiskCacheEnabled && getCacheFile(key).exists();
	}

	/**
	 * create uniq string from file url, which can be used as file name
	 *
	 * @param imageUrl URL of given image
	 * @return calculated hash
	 */
	public static String getHashForKey(String imageUrl) {
		return imageUrl.replaceAll("[:;#~%$\"!<>|+*\\()^/,?&=]+", "+");
	}

	private String getFileNameForKey(String imageUrl) {
		return getHashForKey(imageUrl);
	}

	private File getFileForKey(String key) {
		return new File(diskCacheDir + "/" + getFileNameForKey(key));
	}

	public File getCacheFile(String key) {
		File f = new File(diskCacheDir);
		if (!f.exists() && !f.mkdirs()) Log.w(TAG, "Couldn't create File: " + f.getAbsolutePath());

		return getFileForKey(key);
	}

	/**
	 * Only meaningful if disk caching is enabled.
	 *
	 * @return the full absolute path to the directory where files are cached, if the disk cache is
	 * enabled, otherwise null
	 */
	public String getDiskCacheDirectory() {
		return diskCacheDir;
	}

	public boolean deleteAllCachedFiles() {
		File f = new File(diskCacheDir);
		return FileUtils.deleteFolderRcursive(f);
	}

	public synchronized void clear() {
		cache.invalidateAll();

		if (isDiskCacheEnabled) {
			File[] cachedFiles = new File(diskCacheDir).listFiles();
			if (cachedFiles == null) {
				return;
			}
			for (File f : cachedFiles) {
				if (!f.delete()) Log.e(TAG, "File couldn't be deleted: " + f.getAbsolutePath());
			}
		}
	}

}
