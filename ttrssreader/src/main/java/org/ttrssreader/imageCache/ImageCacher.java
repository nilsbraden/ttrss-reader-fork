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

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

class ImageCacher extends AsyncTask<Void, Integer, Void> {

	private static final String TAG = ImageCacher.class.getSimpleName();

	private static final int DEFAULT_TASK_COUNT = 5;
	private static final int ON_CACHE_END = -1;
	private static final int ON_CACHE_INTERRUPTED = -2;
	private static final int ON_CACHE_START = -3;

	private final ICacheEndListener parent;
	final ConnectivityManager cm;

	private volatile boolean shouldBeStopped = false;

	private final boolean onlyArticles;
	private final int networkType;

	private long cacheSizeMax;
	private ImageCache imageCache;
	private long folderSize;
	private long downloaded = 0;
	private int taskCount = 0;

	private final Map<Integer, DownloadImageTask> map = new HashMap<>();
	// Cache values and insert them later:
	final Map<Integer, List<String>> articleFiles = new HashMap<>();
	final Map<String, Long> remoteFiles = new HashMap<>();

	ImageCacher(ICacheEndListener parent, final Context context, boolean onlyArticles, int networkType) {
		this.parent = parent;
		this.onlyArticles = onlyArticles;
		this.networkType = networkType;
		this.cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		// Create Handler in a new Thread so all tasks are started in this new thread instead of the main UI-Thread
		Thread myHandler = new MyHandler();
		myHandler.start();
	}

	private static final Object LOCK_HANDLER = new Object();
	private static Handler handler;
	private static volatile Boolean handlerInitialized = false;

	private static class MyHandler extends Thread {
		// Source: http://mindtherobot.com/blog/159/android-guts-intro-to-loopers-and-handlers/
		@Override
		public void run() {
			try {
				Looper.prepare();
				handler = new Handler();
				synchronized (LOCK_HANDLER) {
					handlerInitialized = true;
					LOCK_HANDLER.notifyAll();
				}
				Looper.loop();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	// This method is allowed to be called from any thread
	private void requestStop() {
		// Wait for the handler to be fully initialized:
		long wait = Utils.SECOND * 2;
		if (!handlerInitialized) {
			synchronized (LOCK_HANDLER) {
				while (!handlerInitialized && wait > 0) {
					try {
						wait = wait - 300;
						LOCK_HANDLER.wait(300);
					} catch (InterruptedException e) {
						// Empty!
					}
				}
			}
		}
		handler.post(() -> {
			Looper looper = Looper.myLooper();
			if (looper != null)
				looper.quitSafely();
		});
	}

	@Override
	protected Void doInBackground(Void... params) {
		long start = System.currentTimeMillis();
		publishProgress(ON_CACHE_START); // Call onCacheStart()
		doProcess();

		long folderSizeMB = folderSize / 1048576;
		long cacheSizeMaxMB = cacheSizeMax / 1048576;
		long timeSecs = (System.currentTimeMillis() - start) / Utils.SECOND;
		Log.i(TAG, String.format("Cache: %s MB (Limit: %s MB, took %s seconds)", folderSizeMB, cacheSizeMaxMB, timeSecs));

		// Cleanup
		publishProgress(ON_CACHE_END); // Call onCacheEnd()
		requestStop();
		return null;
	}

	private void doProcess() {
		if (checkCancelRequested())
			return;

		// Update all articles
		long timeArticles = System.currentTimeMillis();

		// Loop only once, helps getting outta here when cancel was requested
		//noinspection LoopStatementThatDoesntLoop
		while (true) {
			// sync local status changes to server
			Data.getInstance().synchronizeStatus();

			// Only use progress-updates and callbacks for downloading articles, images are done in background
			Set<Feed> labels = DBHelper.getInstance().getFeeds(-2);
			taskCount = DEFAULT_TASK_COUNT + labels.size();

			int progress = 0;
			publishProgress(++progress);
			if (checkCancelRequested())
				return;
			Data.getInstance().updateCategories(true);
			publishProgress(++progress);
			if (checkCancelRequested())
				return;
			Data.getInstance().updateFeeds(Data.VCAT_ALL, true);

			// Cache all articles
			publishProgress(++progress);
			if (checkCancelRequested())
				return;
			Data.getInstance().cacheArticles(false, true);

			for (Feed f : labels) {
				if (f.unread == 0)
					continue;
				publishProgress(++progress);
				if (checkCancelRequested())
					return;
				Data.getInstance().updateArticles(f.id, true, false, false, true);
				Data.getInstance().updateFeedIcon(f.id);
			}

			Data.getInstance().calculateCounters();
			Data.getInstance().notifyListeners();

			Log.i(TAG, String.format("Updating articles took %s ms", (System.currentTimeMillis() - timeArticles)));
			publishProgress(++progress);
			if (checkCancelRequested())
				return;

			if (onlyArticles) // We are done here..
				return;

			// Initialize other preferences
			this.cacheSizeMax = Controller.getInstance().cacheFolderMaxSize() * Utils.MB;
			this.imageCache = Controller.getInstance().getImageCache();
			if (imageCache == null)
				return;

			downloadImages();

			taskCount = DEFAULT_TASK_COUNT + labels.size();
			publishProgress(++progress);

			// Fall out of the loop
			break;
		}

		purgeCache();
	}

	/**
	 * Calls the parent method to update the progress-bar in the UI while articles are refreshed.
	 */
	@Override
	protected void onProgressUpdate(Integer... values) {
		if (parent != null) {
			if (values[0] == ON_CACHE_START) {
				parent.onCacheStart();
			} else if (values[0] == ON_CACHE_END) {
				parent.onCacheEnd();
			} else if (values[0] == ON_CACHE_INTERRUPTED) {
				Log.i(TAG, "Flag ON_CACHE_INTERRUPTED has been set...");
				shouldBeStopped = true;
				parent.onCacheInterrupted();
			} else {
				parent.onCacheProgress(taskCount, values[0]);
			}
		}
	}

	public void cancel() {
		Log.i(TAG, "Method cancel() of ImageCacher has been called...");
		shouldBeStopped = true;
	}

	private boolean checkCancelRequested() {
		if (shouldBeStopped) {
			return true;
		}
		int currentType = Utils.getNetworkType(cm);
		if (currentType < networkType) {
			Log.d(TAG, String.format("Current Network-Type: %s, Started with: %s", currentType, networkType));
			publishProgress(ON_CACHE_INTERRUPTED);
			return true;
		}
		return false;
	}

	@SuppressLint("UseSparseArrays")
	private void downloadImages() {
		long time = System.currentTimeMillis();
		ArrayList<Article> articles = DBHelper.getInstance().queryArticlesForImagecache();
		taskCount = articles.size();
		Log.d(TAG, "Articles count for image caching: " + taskCount);

		int count = 0;
		for (Article article : articles) {
			if (count++ % 5 == 0 && checkCancelRequested())
				break;

			int articleId = article.id;
			Set<String> set = new HashSet<>();

			for (String url : findAllImageUrls(article.content)) {
				if (!imageCache.containsKey(url))
					set.add(url);
			}

			// Get images from attachments separately
			for (String url : article.attachments) {
				for (String ext : FileUtils.IMAGE_EXTENSIONS) {
					if (url.toLowerCase(Locale.getDefault()).contains("." + ext) && !imageCache.containsKey(url)) {
						set.add(url);
						break;
					}
				}
			}

			if (!set.isEmpty()) {
				DownloadImageTask task = new DownloadImageTask(articleId, StringSupport.setToArray(set));
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
			if (count++ % 5 == 0 && checkCancelRequested())
				break;
			synchronized (map) {
				try {
					// Only wait for 30 Minutes, then stop all running tasks
					int minutes = (int) ((System.currentTimeMillis() - timeWait) / Utils.MINUTE);
					if (minutes > 30) {
						for (DownloadImageTask task : map.values()) {
							task.cancel();
						}
						break;
					}
					map.wait(Utils.SECOND);
					map.notifyAll();
				} catch (InterruptedException e) {
					Log.d(TAG, "Got an InterruptedException!");
				}
			}
		}

		/* Insert cached values, clone map before to avoid ConcurrentModificationException if threads have not
		cancelled yet. Ignore still running threads. */
		Map<Integer, List<String>> articleFilesCopy = new HashMap<>(articleFiles);
		Map<String, Long> remoteFilesCopy = new HashMap<>(remoteFiles);
		DBHelper.getInstance().insertArticleFiles(articleFilesCopy);
		DBHelper.getInstance().markRemoteFilesCached(remoteFilesCopy);

		Log.i(TAG, String.format("Downloading images took %s ms", (System.currentTimeMillis() - time)));
	}

	private class DownloadImageTask implements Runnable {
		private final long minFileSize = Controller.getInstance().cacheImageMinSize() * Utils.KB;
		private final long maxFileSize = Controller.getInstance().cacheImageMaxSize() * Utils.KB;

		private final int articleId;
		private final List<String> fileUrls;

		// Thread-Local cache:
		private final List<String> finishedFileUrls = new ArrayList<>();
		private final Map<Integer, List<String>> articleFilesLocal = new HashMap<>();
		private final Map<String, Long> remoteFilesLocal = new HashMap<>();

		private volatile boolean isCancelled = false;

		private DownloadImageTask(int articleId, String... params) {
			this.articleId = articleId;
			this.fileUrls = Arrays.asList(params);
		}

		@Override
		public void run() {
			if (checkCancelRequested())
				return;

			long size = 0;
			try {
				for (String url : fileUrls) {
					long urlSize = downloadToFile(url, imageCache.getCacheFile(url), maxFileSize, minFileSize);
					if (urlSize > 0) {
						size += urlSize;
						finishedFileUrls.add(url);
						remoteFilesLocal.put(url, urlSize);
					}
					if (isCancelled || checkCancelRequested())
						break;
				}
			} catch (Throwable t) {
				t.printStackTrace();
			} finally {
				articleFilesLocal.put(articleId, finishedFileUrls);
				if (size > 0)
					downloaded += size;

				synchronized (map) {
					articleFiles.putAll(articleFilesLocal);
					remoteFiles.putAll(remoteFilesLocal);

					map.remove(articleId);
					map.notifyAll();
				}
			}
		}

		public void cancel() {
			this.isCancelled = true;
		}
	}

	/**
	 * Downloads a given URL directly to a file, when maxSize bytes are reached the download is stopped and the file is
	 * deleted.
	 *
	 * @param downloadUrl the URL of the file
	 * @param file        the destination file
	 * @param maxSize     the size in bytes after which to abort the download
	 * @param minSize     the minimum size in bytes after which to start the download
	 * @return length of downloaded file or negated file length if it exceeds {@code maxSize} or downloaded with
	 * errors.
	 * So, if returned value less or equals to 0, then the file was not cached.
	 */
	private long downloadToFile(String downloadUrl, File file, long maxSize, long minSize) {
		if (file.exists() && file.length() > 0)
			return file.length();
		if (checkCancelRequested())
			return 0;

		long byteWritten = 0;
		boolean error = false;
		InputStream is = null;

		try (FileOutputStream fos = new FileOutputStream(file)) {
			if (checkCancelRequested())
				throw new InterruptedIOException("Download was cancelled.");

			URL url = new URL(downloadUrl);
			URLConnection connection = Controller.getInstance().openConnection(url);

			connection.setConnectTimeout((int) (Utils.SECOND * 2));
			connection.setReadTimeout((int) Utils.SECOND);

			// Check filesize if available from header
			try {
				long length = Long.parseLong(connection.getHeaderField("Content-Length"));

				if (length <= 0) {
					byteWritten = length;
					Log.w(TAG, "Content-Length equals 0 or is negative: " + length);
				} else if (length < minSize) {
					error = true;
					byteWritten = -length;
					Log.i(TAG, String.format("Not starting download of %s, the size (%s bytes) is less then the minimum " + "filesize of %s bytes.", downloadUrl, length, minSize));
				} else if (length > maxSize) {
					error = true;
					byteWritten = -length;
					Log.i(TAG, String.format("Not starting download of %s, the size (%s bytes) exceeds the " + "maximum " + "filesize of %s bytes.", downloadUrl, length, maxSize));
				}
			} catch (Exception e) {
				Log.w(TAG, "Couldn't read Content-Length from url: " + downloadUrl);
			}

			if (byteWritten == 0) {

				if (!file.exists() && !file.createNewFile())
					Log.i(TAG, "File could not be created: " + file.getAbsolutePath());

				is = connection.getInputStream();

				int size = (int) Utils.KB * 8;
				byte[] buf = new byte[size];
				int byteRead;

				int count = 0;
				while (((byteRead = is.read(buf)) != -1)) {
					if (count++ % 20 == 0 && checkCancelRequested())
						throw new InterruptedIOException("Download was cancelled.");

					fos.write(buf, 0, byteRead);
					byteWritten += byteRead;

					if (byteWritten > maxSize) {
						throw new InterruptedIOException(String.format("Download interrupted, the size of %s bytes exceeds maximum filesize.", byteWritten));
					}
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Download not finished properly. Exception: " + e.getMessage(), e);
			error = true;
			byteWritten = -file.length();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException ignored) {
				}
			}
		}

		if (error)
			Log.w(TAG, String.format("Stopped download from url '%s'. Downloaded %d bytes", downloadUrl, byteWritten));
		else
			Log.i(TAG, String.format("Download from '%s' finished. Downloaded %d bytes", downloadUrl, byteWritten));

		if (error && file.exists())
			if (!file.delete())
				Log.w(TAG, "File could not be deleted: " + file.getAbsolutePath());

		return byteWritten;
	}

	/**
	 * cache cleanup
	 */
	private void purgeCache() {
		long time = System.currentTimeMillis();
		folderSize = DBHelper.getInstance().getCachedFilesSize();

		if (folderSize > cacheSizeMax) {
			Collection<RemoteFile> rfs = DBHelper.getInstance().getUncacheFiles(folderSize - cacheSizeMax);
			Log.d(TAG, String.format("Found %s cached files for deletion", rfs.size()));

			ArrayList<Integer> rfIds = new ArrayList<>(rfs.size());
			for (RemoteFile rf : rfs) {
				File file = imageCache.getCacheFile(rf.url);

				if (file.exists() && !file.delete())
					Log.w(TAG, "File " + file.getAbsolutePath() + " was not " + "deleted!");

				rfIds.add(rf.id);
			}

			DBHelper.getInstance().markRemoteFilesNonCached(rfIds);
		}
		Log.i(TAG, String.format("Purging cache took %s ms", (System.currentTimeMillis() - time)));
	}

	/**
	 * Searches the given html code for img-Tags and filters out all src-attributes, beeing URLs to images.
	 *
	 * @param html the html code which is to be searched
	 * @return a set of URLs in their string representation
	 */
	private static Set<String> findAllImageUrls(String html) {
		Set<String> ret = new LinkedHashSet<>();
		if (html == null || html.length() < 10)
			return ret;

		int i = html.indexOf("<img");
		if (i == -1)
			return ret;

		// Filter out URLs without leading http, we cannot work with relative URLs (yet?).
		Matcher m = Utils.findImageUrlsPattern.matcher(html.substring(i));

		while (m.find()) {
			String url = m.group(1);

			if (url != null) {
				if (url.startsWith("http") || url.startsWith("ftp://")) {
					ret.add(url);
				}
			}
		}
		return ret;
	}

}
