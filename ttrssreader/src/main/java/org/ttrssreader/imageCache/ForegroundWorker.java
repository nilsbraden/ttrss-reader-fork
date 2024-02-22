package org.ttrssreader.imageCache;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.ttrssreader.gui.interfaces.ICacheEndListener;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.Utils;
import org.ttrssreader.utils.WakeLocker;

import androidx.annotation.NonNull;
import androidx.work.WorkInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ForegroundWorker extends Worker implements ICacheEndListener {
	public static final String TAG = ForegroundWorker.class.getSimpleName();


	private static final Object LOCK_INSTANCE = new Object();
	private static volatile ForegroundWorker instance = null;
	private static ICacheEndListener parent;
	private static ImageCacher imageCacher;

	public ForegroundWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
		super(context, workerParams);
		instance = this;
	}

	@NonNull
	@Override
	public Result doWork() {
		Context ctx = getApplicationContext();
		androidx.work.Data data = getInputData();

		//call methods to perform background task
		synchronized (LOCK_INSTANCE) {
			if (imageCacher == null) {
				int networkType = data.getInt(ForegroundService.PARAM_NETWORK, Utils.NETWORK_NONE);
				boolean onlyArticles = ForegroundService.ACTION_LOAD_ARTICLES.equals(data.getString(ForegroundService.PARAM_ACTION));

				Log.i(TAG, String.format("Caching (%s) started", onlyArticles ? "articles" : "images"));
				new Handler(Looper.getMainLooper()).post(() -> {
					Log.d("UI thread", "I am the UI thread");
					imageCacher = new ImageCacher(instance, ctx, onlyArticles, networkType);
					imageCacher.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				});

				WakeLocker.acquire(ctx);
			}
		}
		return Result.success();
	}

	@Override
	public void onCacheStart() {
		if (parent != null)
			parent.onCacheStart();
	}

	/**
	 * Cleans up all running notifications, notifies waiting activities and clears the instance of the service.
	 */
	@SuppressLint("RestrictedApi")
	@Override
	public void onCacheEnd() {
		WakeLocker.release();
		if (instance != null) {
			imageCacher = null;
			instance = null;
		}
		if (parent != null)
			parent.onCacheEnd();
		this.stop(WorkInfo.STOP_REASON_CANCELLED_BY_APP);
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
}
