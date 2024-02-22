/*
 * ttrss-reader-fork for Android
 *
 * Copyright (C) 2010 Nils Braden
 * Copyright 2013 two forty four a.m. LLC <http://www.twofortyfouram.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.ttrssreader.imageCache;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginSettingReceiver;

import org.json.JSONObject;
import org.ttrssreader.controllers.Controller;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

/**
 * This is the "fire" BroadcastReceiver for a Locale Plug-in setting.
 */
public final class PluginReceiver extends AbstractPluginSettingReceiver {

	private static final String TAG = PluginReceiver.class.getSimpleName();

	@Override
	protected boolean isJsonValid(@NonNull final JSONObject json) {
		return PluginJsonValues.isJsonValid(json);
	}

	@Override
	protected boolean isAsync() {
		return false;
	}

	@Override
	protected void firePluginSetting(@NonNull final Context context, @NonNull final JSONObject json) {
		Log.d(TAG, "firePluginSetting() called, received JSON for action...");

		Controller.getInstance().setHeadless(true);

		final boolean images = PluginJsonValues.getExtraImages(json);
		final boolean notification = PluginJsonValues.getExtraNotification(json);

		Intent intent = new Intent(images ? ForegroundService.ACTION_LOAD_IMAGES : ForegroundService.ACTION_LOAD_ARTICLES);
		intent.setClass(context, ForegroundService.class);
		intent.putExtra(ForegroundService.PARAM_SHOW_NOTIFICATION, notification);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			Data data = new Data.Builder()
					.putString(ForegroundService.PARAM_ACTION, images ? ForegroundService.ACTION_LOAD_IMAGES : ForegroundService.ACTION_LOAD_ARTICLES)
					.putBoolean(ForegroundService.PARAM_SHOW_NOTIFICATION, notification)
					.build();
			OneTimeWorkRequest request = new OneTimeWorkRequest.Builder ( ForegroundWorker.class ).setInputData(data).addTag ( ForegroundWorker.TAG ).build ();
			WorkManager.getInstance ( context ).enqueue ( request );
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			context.startForegroundService ( intent );
		} else {
			context.startService ( intent );
		}
	}

}
