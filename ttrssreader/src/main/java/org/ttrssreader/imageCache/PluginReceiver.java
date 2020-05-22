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
import android.os.Bundle;
import android.util.Log;

import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginSettingReceiver;

import org.ttrssreader.controllers.Controller;
import org.ttrssreader.imageCache.bundle.PluginBundleManager;

import androidx.annotation.NonNull;

/**
 * This is the "fire" BroadcastReceiver for a Locale Plug-in setting.
 */
public final class PluginReceiver extends AbstractPluginSettingReceiver {

	private static final String TAG = PluginReceiver.class.getSimpleName();

	@Override
	protected void firePluginSetting(@NonNull Context context, @NonNull Bundle bundle) {
		Log.d(TAG, "firePluginSetting() called, received Bundle for action...");

		Controller.getInstance().setHeadless(true);

		final boolean images = bundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_IMAGES);
		final boolean notification = bundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_NOTIFICATION);

		Intent serviceIntent;
		if (images) {
			serviceIntent = new Intent(ForegroundService.ACTION_LOAD_IMAGES);
		} else {
			serviceIntent = new Intent(ForegroundService.ACTION_LOAD_ARTICLES);
		}
		serviceIntent.setClass(context, ForegroundService.class);
		serviceIntent.putExtra(ForegroundService.PARAM_SHOW_NOTIFICATION, notification);
		context.startService(serviceIntent);
	}

	@Override
	protected boolean isBundleValid(@NonNull final Bundle bundle) {
		return PluginBundleManager.isBundleValid(bundle);
	}

	@Override
	protected boolean isAsync() {
		return false;
	}

}
