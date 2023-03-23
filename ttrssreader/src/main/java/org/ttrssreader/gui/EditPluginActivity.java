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

import android.os.Bundle;
import android.widget.CheckBox;

import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractPluginActivity;

import org.json.JSONObject;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.imageCache.PluginJsonValues;
import org.ttrssreader.utils.PostMortemReportExceptionHandler;

import androidx.annotation.NonNull;

public final class EditPluginActivity extends AbstractPluginActivity {

	//	private static final String TAG = EditPluginActivity.class.getSimpleName();

	final PostMortemReportExceptionHandler mDamageReport = new PostMortemReportExceptionHandler(this);

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(Controller.getInstance().getThemeResource());
		Controller.getInstance().initializeThemeMode();
		mDamageReport.initialize();
		setContentView(R.layout.localeplugin);
	}

	/* package */
	static String generateBlurb(final boolean images, final boolean notification) {
		String imageText = (images ? "Caching images" : "Not caching images");
		String notificationText = (notification ? "Showing notification" : "Not showing notification");
		return imageText + ", " + notificationText;
	}

	@Override
	protected void onDestroy() {
		mDamageReport.restoreOriginalHandler();
		super.onDestroy();
	}

	@Override
	public void onPostCreateWithPreviousResult(@NonNull final JSONObject previousJson, @NonNull final String previousBlurb) {
		if (PluginJsonValues.isJsonValid(previousJson)) {
			final boolean isSaveImages = PluginJsonValues.getExtraImages(previousJson);
			final boolean isShowNotification = PluginJsonValues.getExtraNotification(previousJson);

			CheckBox images = findViewById(R.id.cb_images);
			CheckBox notification = findViewById(R.id.cb_notification);

			images.setChecked(isSaveImages);
			notification.setChecked(isShowNotification);
		}
	}

	@Override
	public boolean isJsonValid(@NonNull final JSONObject json) {
		return PluginJsonValues.isJsonValid(json);
	}

	@Override
	public JSONObject getResultJson() {
		final boolean images = ((CheckBox) findViewById(R.id.cb_images)).isChecked();
		final boolean notification = ((CheckBox) findViewById(R.id.cb_notification)).isChecked();
		return PluginJsonValues.generateJson(getApplicationContext(), images, notification);
	}

	@NonNull
	@Override
	public String getResultBlurb(@NonNull final JSONObject json) {
		final boolean images = ((CheckBox) findViewById(R.id.cb_images)).isChecked();
		final boolean notification = ((CheckBox) findViewById(R.id.cb_notification)).isChecked();
		return generateBlurb(images, notification);
	}
}
