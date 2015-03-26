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

import org.ttrssreader.R;
import org.ttrssreader.imageCache.PluginReceiver;
import org.ttrssreader.imageCache.bundle.BundleScrubber;
import org.ttrssreader.imageCache.bundle.PluginBundleManager;
import org.ttrssreader.utils.PostMortemReportExceptionHandler;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;

/**
 * This is the "Edit" activity for a Locale Plug-in.
 * <p>
 * This Activity can be started in one of two states:
 * <ul>
 * <li>New plug-in instance: The Activity's Intent will not contain
 * {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE}.</li>
 * <li>Old plug-in instance: The Activity's Intent will contain {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE}
 * from a previously saved plug-in instance that the user is editing.</li>
 * </ul>
 *
 * @see com.twofortyfouram.locale.Intent#ACTION_EDIT_SETTING
 * @see com.twofortyfouram.locale.Intent#EXTRA_BUNDLE
 */
public final class EditPluginActivity extends AbstractPluginActivity {

	@SuppressWarnings("unused")
	private static final String TAG = EditPluginActivity.class.getSimpleName();

	protected PostMortemReportExceptionHandler mDamageReport = new PostMortemReportExceptionHandler(this);

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDamageReport.initialize();

		BundleScrubber.scrub(getIntent());

		final Bundle localeBundle = getIntent().getBundleExtra(PluginReceiver.EXTRA_BUNDLE);
		BundleScrubber.scrub(localeBundle);

		setContentView(R.layout.localeplugin);

		if (null == savedInstanceState) {
			if (PluginBundleManager.isBundleValid(localeBundle)) {
				final boolean images = localeBundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_IMAGES);
				final boolean notification = localeBundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_NOTIFICATION);
				((CheckBox) findViewById(R.id.cb_images)).setChecked(images);
				((CheckBox) findViewById(R.id.cb_notification)).setChecked(notification);
			}
		}
	}

	@Override
	public void finish() {
		if (!isCanceled()) {
			final boolean images = ((CheckBox) findViewById(R.id.cb_images)).isChecked();
			final boolean notification = ((CheckBox) findViewById(R.id.cb_notification)).isChecked();
			final Intent resultIntent = new Intent();

			/*
			 * This extra is the data to ourselves: either for the Activity or the BroadcastReceiver. Note
			 * that anything placed in this Bundle must be available to Locale's class loader. So storing
			 * String, int, and other standard objects will work just fine. Parcelable objects are not
			 * acceptable, unless they also implement Serializable. Serializable objects must be standard
			 * Android platform objects (A Serializable class private to this plug-in's APK cannot be
			 * stored in the Bundle, as Locale's classloader will not recognize it).
			 */
			final Bundle result = PluginBundleManager.generateBundle(getApplicationContext(), images, notification);
			resultIntent.putExtra(PluginReceiver.EXTRA_BUNDLE, result);

			/*
			 * The blurb is concise status text to be displayed in the host's UI.
			 */
			final String blurb = generateBlurb(getApplicationContext(), images, notification);
			resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, blurb);

			setResult(RESULT_OK, resultIntent);
		}

		super.finish();
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

}
