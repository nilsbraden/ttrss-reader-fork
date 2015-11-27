/*
 * ttrss-reader-fork for Android
 *
 * Copyright (C) 2010 Nils Braden
 * Copyright (C) 2009 The Android Open Source Project
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

import org.ttrssreader.R;
import org.ttrssreader.gui.interfaces.ICacheEndListener;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.Utils;
import org.ttrssreader.utils.WakeLocker;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ForegroundService extends Service implements ICacheEndListener {

	private static final String TAG = ForegroundService.class.getSimpleName();

	public static final String PARAM_SHOW_NOTIFICATION = "show_notification";
	public static final String PARAM_NETWORK = "network";

	public static final String ACTION_LOAD_IMAGES = "load_images";
	public static final String ACTION_LOAD_ARTICLES = "load_articles";

	private static final Object LOCK_INSTANCE = new Object();
	private static volatile ForegroundService instance = null;
	private static ICacheEndListener parent;
	private static ImageCacher imageCacher;

	public static boolean isInstanceCreated() {
		return instance != null;
	}

	public static void cancel() {
		if (imageCacher != null) imageCacher.cancel();
	}

	public static void registerCallback(ICacheEndListener parentGUI) {
		parent = parentGUI;
	}

	@Override
	public void onCreate() {
		instance = this;
		super.onCreate();
	}

	/**
	 * Cleans up all running notifications, notifies waiting activities and clears the instance of the service.
	 */
	@Override
	public void onCacheEnd() {
		WakeLocker.release();
		if (instance != null) {
			stopForeground(true);
			imageCacher = null;
			instance = null;
		}
		if (parent != null) parent.onCacheEnd();
		this.stopSelf();
	}

	@Override
	public void onCacheInterrupted() {
		if (parent != null) parent.onCacheInterrupted();
	}

	@Override
	public void onCacheProgress(int taskCount, int progress) {
		if (parent != null) parent.onCacheProgress(taskCount, progress);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		synchronized (LOCK_INSTANCE) {
			if (imageCacher == null && intent != null && intent.getAction() != null) {

				int networkType = intent.getIntExtra(PARAM_NETWORK, Utils.NETWORK_NONE);

				CharSequence title = "";
				if (ACTION_LOAD_IMAGES.equals(intent.getAction())) {
					title = getText(R.string.Cache_service_imagecache);
					imageCacher = new ImageCacher(this, this, false, networkType);
					imageCacher.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					Log.i(TAG, "Caching images started");
				} else if (ACTION_LOAD_ARTICLES.equals(intent.getAction())) {
					title = getText(R.string.Cache_service_articlecache);
					imageCacher = new ImageCacher(this, this, true, networkType);
					imageCacher.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					Log.i(TAG, "Caching (articles only) started");
				}

				WakeLocker.acquire(this);

				if (intent.getBooleanExtra(PARAM_SHOW_NOTIFICATION, false)) {
					int icon = R.drawable.notification_icon;
					CharSequence ticker = getText(R.string.Cache_service_started);
					CharSequence text = getText(R.string.Cache_service_text);
					Notification notification = Utils
							.buildNotification(this, icon, ticker, title, text, true, new Intent());
					startForeground(R.string.Cache_service_started, notification);
				}

			}
		}
		return START_STICKY;
	}

}
