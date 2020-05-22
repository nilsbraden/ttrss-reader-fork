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

package org.ttrssreader.gui;

import android.content.Context;
import android.os.Bundle;
import android.widget.CheckBox;

import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractPluginActivity;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.imageCache.bundle.PluginBundleManager;
import org.ttrssreader.utils.PostMortemReportExceptionHandler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class EditPluginActivity extends AbstractPluginActivity {

	@SuppressWarnings("unused")
	private static final String TAG = EditPluginActivity.class.getSimpleName();

	protected PostMortemReportExceptionHandler mDamageReport = new PostMortemReportExceptionHandler(this);

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(Controller.getInstance().getTheme());
		mDamageReport.initialize();
		setContentView(R.layout.localeplugin);
	}

	/* package */
	static String generateBlurb(final Context context, final boolean images, final boolean notification) {
		String imageText = (images ? "Caching images" : "Not caching images");
		String notificationText = (notification ? "Showing notification" : "Not showing notification");
		return imageText + ", " + notificationText;
	}

	@Override
	protected void onDestroy() {
		mDamageReport.restoreOriginalHandler();
		mDamageReport = null;
		super.onDestroy();
	}

	@Override
	public boolean isBundleValid(@NonNull Bundle bundle) {
		return PluginBundleManager.isBundleValid(bundle);
	}

	@Override
	public void onPostCreateWithPreviousResult(@NonNull Bundle previousBundle, @NonNull String s) {
		if (PluginBundleManager.isBundleValid(previousBundle)) {
			final boolean isSaveImages = PluginBundleManager.isSaveImages(previousBundle);
			final boolean isShowNotification = PluginBundleManager.isShowNotification(previousBundle);

			CheckBox images = findViewById(R.id.cb_images);
			CheckBox notification = findViewById(R.id.cb_notification);

			images.setChecked(isSaveImages);
			notification.setChecked(isShowNotification);
		}
	}

	@Nullable
	@Override
	public Bundle getResultBundle() {
		final boolean images = ((CheckBox) findViewById(R.id.cb_images)).isChecked();
		final boolean notification = ((CheckBox) findViewById(R.id.cb_notification)).isChecked();
		return PluginBundleManager.generateBundle(getApplicationContext(), images, notification);
	}

	@NonNull
	@Override
	public String getResultBlurb(@NonNull Bundle bundle) {
		final boolean images = ((CheckBox) findViewById(R.id.cb_images)).isChecked();
		final boolean notification = ((CheckBox) findViewById(R.id.cb_notification)).isChecked();
		return generateBlurb(getApplicationContext(), images, notification);
	}
}
