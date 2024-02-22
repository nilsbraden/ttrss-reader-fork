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

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.gui.interfaces.ICacheEndListener;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.Utils;
import org.ttrssreader.utils.WakeLocker;

public class ForegroundService extends Service implements ICacheEndListener {

	private static final String TAG = ForegroundService.class.getSimpleName();

	public static final String PARAM_ACTION = "action";
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
		if (imageCacher != null)
			imageCacher.cancel();
	}

	public static void registerCallback(ICacheEndListener parentGUI) {
		parent = parentGUI;
	}

	@Override
	public void onCreate() {
		instance = this;
		super.onCreate();
	}

	@Override
	public void onCacheStart() {
		if (parent != null)
			parent.onCacheStart();
	}

	/**
	 * Cleans up all running notifications, notifies waiting activities and clears the instance of the service.
	 */
	@Override
	public void onCacheEnd() {
		WakeLocker.release();
		if (instance != null) {
			stopForeground(STOP_FOREGROUND_REMOVE);
			imageCacher = null;
			instance = null;
		}
		if (parent != null)
			parent.onCacheEnd();
		this.stopSelf();
	}

	@Override
	public void onCacheInterrupted() {
		if (parent != null)
			parent.onCacheInterrupted();
	}

	@Override
	public void onCacheProgress(int taskCount, int progress) {
		if (parent != null)
			parent.onCacheProgress(taskCount, progress);
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
				boolean onlyArticles = ACTION_LOAD_ARTICLES.equals(intent.getAction());

				CharSequence title = onlyArticles ? getText(R.string.Cache_service_articlecache) : getText(R.string.Cache_service_imagecache);

				Log.i(TAG, String.format("Caching (%s) started", onlyArticles ? "articles" : "images"));
				imageCacher = new ImageCacher(instance, this, onlyArticles, networkType);
				imageCacher.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

				WakeLocker.acquire(this);

				if (intent.getBooleanExtra(PARAM_SHOW_NOTIFICATION, false)) {
					int icon = R.drawable.notification_icon;
					CharSequence ticker = getText(R.string.Cache_service_started);
					CharSequence text = getText(R.string.Cache_service_text);
					Notification notification = Utils.buildNotification(this, icon, ticker, title, text, true, new Intent(), Data.NOTIFICATION_CHANNEL_ID_TASKER);
					doStartForeground(R.string.Cache_service_started, notification);
				} else {
					// Show dummy notification with a text exlaining this can be hidden...
					// According to google this should be done with an Intent Service. Half a year later it doesn't work
					// and the next thing is a JobIntentService which does not work reliably and is deprecated. Right now
					// they expect us to use a WorkManager which I'm too lazy to implement, maybe the next iteration...
					CharSequence dummytitle = getText(R.string.Cache_service_title_dummy);
					int icon = R.drawable.notification_icon;
					CharSequence ticker = getText(R.string.Cache_service_started);
					CharSequence text = getText(R.string.Cache_service_text);
					Notification notification = Utils.buildNotification(this, icon, ticker, dummytitle, text, true, new Intent(), Data.NOTIFICATION_CHANNEL_ID_TASKER);
					doStartForeground(R.string.Cache_service_started, notification);
				}

			}
		}
		return START_STICKY;
	}

	public void doStartForeground(int id, Notification notification) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			startForeground(id, notification);
		} else {
			startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
		}
	}

}
