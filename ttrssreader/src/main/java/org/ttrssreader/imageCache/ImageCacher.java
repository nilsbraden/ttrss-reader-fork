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

import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.gui.interfaces.ICacheEndListener;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.model.pojos.RemoteFile;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.FileUtils;
import org.ttrssreader.utils.StringSupport;
import org.ttrssreader.utils.Utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

class ImageCacher extends AsyncTask<Void, Integer, Void> {

	private static final String TAG = ImageCacher.class.getSimpleName();

	private static final int DEFAULT_TASK_COUNT = 6;

	private static volatile int progressImageDownload;

	private ICacheEndListener parent;
	ConnectivityManager cm;

	private boolean onlyArticles;
	private long cacheSizeMax;
	private ImageCache imageCache;
	private long folderSize;
	private long downloaded = 0;
	private int taskCount = 0;

	private final Map<Integer, DownloadImageTask> map = new HashMap<>();
	// Cache values and insert them later:
	Map<Integer, String[]> articleFiles = new HashMap<>();
	Map<String, Long> remoteFiles = new HashMap<>();

	ImageCacher(ICacheEndListener parent, final Context context, boolean onlyArticles) {
		this.parent = parent;
		this.onlyArticles = onlyArticles;
		this.cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		// Create Handler in a new Thread so all tasks are started in this new thread instead of the main UI-Thread
		Thread myHandler = new MyHandler();
		myHandler.start();
	}

	private static Handler handler;

	private static final Object lock = new Object();

	private static volatile Boolean handlerInitialized = false;

	private static class MyHandler extends Thread {
		// Source: http://mindtherobot.com/blog/159/android-guts-intro-to-loopers-and-handlers/
		@Override
		public void run() {
			try {
				Looper.prepare();
				handler = new Handler();
				synchronized (lock) {
					handlerInitialized = true;
					lock.notifyAll();
				}
				Looper.loop();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	// This method is allowed to be called from any thread
	private synchronized void requestStop() {
		// Wait for the handler to be fully initialized:
		long wait = Utils.SECOND * 2;
		if (!handlerInitialized) {
			synchronized (lock) {
				while (!handlerInitialized && wait > 0) {
					try {
						wait = wait - 300;
						lock.wait(300);
					} catch (InterruptedException e) {
						// Empty!
					}
				}
			}
		}
		handler.post(new Runnable() {
			@Override
			public void run() {
				Looper.myLooper().quit();
			}
		});
	}

	@Override
	protected Void doInBackground(Void... params) {
		long start = System.currentTimeMillis();

		doProcess();

		Log.i(TAG, String.format("Cache: %s MB (Limit: %s MB, took %s seconds)", folderSize / 1048576,
				cacheSizeMax / 1048576, (System.currentTimeMillis() - start) / Utils.SECOND));

		// Cleanup
		publishProgress(Integer.MAX_VALUE); // Call onCacheEnd()
		requestStop();
		return null;
	}

	private void doProcess() {
		if (!Utils.checkConnected(cm)) {
			Log.e(TAG, "No connectivity, aborting...");
			return;
		}

		// Update all articles
		long timeArticles = System.currentTimeMillis();

		// sync local status changes to server
		Data.getInstance().synchronizeStatus();

		// Only use progress-updates and callbacks for downloading articles, images are done in background
		// completely
		Set<Feed> labels = DBHelper.getInstance().getFeeds(-2);
		taskCount = DEFAULT_TASK_COUNT + labels.size();

		int progress = 0;
		publishProgress(++progress);
		// Data.getInstance().updateCounters(true, true);
		publishProgress(++progress);
		Data.getInstance().updateCategories(true);
		publishProgress(++progress);
		Data.getInstance().updateFeeds(Data.VCAT_ALL, true);

		// Cache all articles
		publishProgress(++progress);
		Data.getInstance().cacheArticles(false, true);

		for (Feed f : labels) {
			if (f.unread == 0) continue;
			publishProgress(++progress);
			Data.getInstance().updateArticles(f.id, true, false, false, true);
		}

		Data.getInstance().calculateCounters();
		Data.getInstance().notifyListeners();

		publishProgress(++progress);
		Log.i(TAG, "Updating articles took " + (System.currentTimeMillis() - timeArticles) + "ms");

		if (onlyArticles) // We are done here..
			return;

		// Initialize other preferences
		this.cacheSizeMax = Controller.getInstance().cacheFolderMaxSize() * Utils.MB;
		this.imageCache = Controller.getInstance().getImageCache();
		if (imageCache == null) return;

		imageCache.fillMemoryCacheFromDisk();
		downloadImages();

		taskCount = DEFAULT_TASK_COUNT + labels.size();
		publishProgress(++progress);
		purgeCache();
	}

	/**
	 * Calls the parent method to update the progress-bar in the UI while articles are refreshed. This is not called
	 * anymore from the moment on when the image-downloading starts.
	 */
	@Override
	protected void onProgressUpdate(Integer... values) {
		if (parent != null) {
			if (values[0] == Integer.MAX_VALUE) {
				parent.onCacheEnd();
			} else {
				parent.onCacheProgress(taskCount, values[0]);
			}
		}
	}

	@SuppressLint("UseSparseArrays")
	private void downloadImages() {
		long time = System.currentTimeMillis();
		ArrayList<Article> articles = DBHelper.getInstance().queryArticlesForImagecache();
		taskCount = articles.size();
		Log.d(TAG, "Articles count for image caching: " + taskCount);

		for (Article article : articles) {
			int articleId = article.id;

			// Log.d(TAG, "Cache images for article ID: " + articleId);

			// Get images included in HTML
			Set<String> set = new HashSet<>();

			for (String url : findAllImageUrls(article.content)) {
				if (!imageCache.containsKey(url)) set.add(url);
			}
			// Log.d(TAG, "Amount of uncached images for article ID " + articleId + ":" + set.size());

			// Get images from attachments separately
			for (String url : article.attachments) {
				for (String ext : FileUtils.IMAGE_EXTENSIONS) {
					if (url.toLowerCase(Locale.getDefault()).contains("." + ext) && !imageCache.containsKey(url)) {
						set.add(url);
						break;
					}
				}
			}
			// Log.d(TAG, "Total amount of uncached images for article ID " + articleId + ":" + set.size());

			if (!set.isEmpty()) {
				DownloadImageTask task = new DownloadImageTask(imageCache, articleId, StringSupport.setToArray(set));
				handler.post(task);
				map.put(articleId, task);
			} else {
				DBHelper.getInstance().updateArticleCachedImages(articleId, 0);
			}

			if (downloaded > cacheSizeMax) {
				Log.w(TAG, "Stopping download, downloaded data exceeds cache-size-limit from options.");
				break;
			}
		}

		long timeWait = System.currentTimeMillis();
		while (!map.isEmpty()) {
			synchronized (map) {
				try {
					// Only wait for 10 Minutes
					if (System.currentTimeMillis() - timeWait > Utils.MINUTE * 10) break;
					map.wait(Utils.SECOND);
					map.notifyAll();
				} catch (InterruptedException e) {
					Log.d(TAG, "Got an InterruptedException!");
				}
			}
		}

		// Insert cached values:
		DBHelper.getInstance().insertArticleFiles(articleFiles);
		DBHelper.getInstance().markRemoteFilesCached(remoteFiles);

		Log.i(TAG, "Downloading images took " + (System.currentTimeMillis() - time) + "ms");
	}

	private class DownloadImageTask implements Runnable {
		// Max size for one image
		private final long maxFileSize = Controller.getInstance().cacheImageMaxSize() * Utils.KB;
		private final long minFileSize = Controller.getInstance().cacheImageMinSize() * Utils.KB;
		private ImageCache imageCache;
		private int articleId;
		private String[] fileUrls;
		// Thread-Local cache:
		Map<Integer, String[]> articleFilesLocal = new HashMap<>();
		Map<String, Long> remoteFilesLocal = new HashMap<>();

		private DownloadImageTask(ImageCache cache, int articleId, String... params) {
			this.imageCache = cache;
			this.articleId = articleId;
			this.fileUrls = params;
		}

		@Override
		public void run() {
			long size = 0;
			try {
				Log.d(TAG, "maxFileSize = " + maxFileSize + " and minFileSize = " + minFileSize);
				articleFilesLocal.put(articleId, fileUrls);
				for (String url : fileUrls) {
					size = FileUtils.downloadToFile(url, imageCache.getCacheFile(url), maxFileSize, minFileSize);
					remoteFilesLocal.put(url, size);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			} finally {
				synchronized (map) {
					if (downloaded > 0) downloaded += size;

					articleFiles.putAll(articleFilesLocal);
					remoteFiles.putAll(remoteFilesLocal);

					map.remove(articleId);
					publishProgress(++progressImageDownload);
					map.notifyAll();
				}
			}
		}
	}

	/**
	 * cache cleanup
	 */
	private void purgeCache() {
		long time = System.currentTimeMillis();
		folderSize = DBHelper.getInstance().getCachedFilesSize();

		if (folderSize > cacheSizeMax) {
			Collection<RemoteFile> rfs = DBHelper.getInstance().getUncacheFiles(folderSize - cacheSizeMax);
			Log.d(TAG, "Found " + rfs.size() + " cached files for deletion");

			ArrayList<Integer> rfIds = new ArrayList<>(rfs.size());
			for (RemoteFile rf : rfs) {
				File file = imageCache.getCacheFile(rf.url);

				if (file.exists() && !file.delete())
					Log.w(TAG, "File " + file.getAbsolutePath() + " was not " + "deleted!");

				rfIds.add(rf.id);
			}

			DBHelper.getInstance().markRemoteFilesNonCached(rfIds);
		}
		Log.i(TAG, "Purging cache took " + (System.currentTimeMillis() - time) + "ms");
	}

	/**
	 * Searches the given html code for img-Tags and filters out all src-attributes, beeing URLs to images.
	 *
	 * @param html the html code which is to be searched
	 * @return a set of URLs in their string representation
	 */
	private static Set<String> findAllImageUrls(String html) {
		Set<String> ret = new LinkedHashSet<>();
		if (html == null || html.length() < 10) return ret;

		int i = html.indexOf("<img");
		if (i == -1) return ret;

		// Filter out URLs without leading http, we cannot work with relative URLs (yet?).
		Matcher m = Utils.findImageUrlsPattern.matcher(html.substring(i, html.length()));

		while (m.find()) {
			String url = m.group(1);
			if (url.startsWith("http")) ret.add(url);
		}
		return ret;
	}

}
