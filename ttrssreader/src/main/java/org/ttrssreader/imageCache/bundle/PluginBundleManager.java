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

package org.ttrssreader.imageCache.bundle;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.twofortyfouram.assertion.BundleAssertions;

import org.ttrssreader.utils.Utils;

import androidx.annotation.NonNull;

/**
 * Class for managing the {@link com.twofortyfouram.locale.api.Intent#EXTRA_BUNDLE} for this plug-in.
 */
public final class PluginBundleManager {

	private static final String TAG = PluginBundleManager.class.getSimpleName();

	/**
	 * Type: {@code String}.
	 * <p>
	 * String message to display in a Toast message.
	 */
	public static final String BUNDLE_EXTRA_IMAGES = "org.ttrssreader.FETCH_IMAGES"; //$NON-NLS-1$
	public static final String BUNDLE_EXTRA_NOTIFICATION = "org.ttrssreader.SHOW_NOTIFICATION"; //$NON-NLS-1$

	/**
	 * Type: {@code int}.
	 * <p>
	 * versionCode of the plug-in that saved the Bundle.
	 */
	/*
	 * This extra is not strictly required, however it makes backward and forward compatibility significantly
	 * easier. For example, suppose a bug is found in how some version of the plug-in stored its Bundle. By
	 * having the version, the plug-in can better detect when such bugs occur.
	 */
	private static final String BUNDLE_EXTRA_VERSION_CODE = "org.ttrssreader.VERSION_CODE"; //$NON-NLS-1$

	/**
	 * Method to verify the content of the bundle are correct.
	 * <p>
	 * This method will not mutate {@code bundle}.
	 *
	 * @param bundle bundle to verify. May be null, which will always return false.
	 * @return true if the Bundle is valid, false if the bundle is invalid.
	 */
	public static boolean isBundleValid(final Bundle bundle) {
		if (null == bundle) {
			return false;
		}

		try {
			BundleAssertions.assertHasLong(bundle, BUNDLE_EXTRA_VERSION_CODE);
			BundleAssertions.assertHasBoolean(bundle, BUNDLE_EXTRA_IMAGES);
			BundleAssertions.assertHasBoolean(bundle, BUNDLE_EXTRA_NOTIFICATION);
			BundleAssertions.assertKeyCount(bundle, 3);
		} catch (final AssertionError e) {
			Log.e(TAG, "Bundle failed verification%s", e);
			return false;
		}

		return true;
	}

	/**
	 * @param context Application context.
	 * @return A plug-in bundle.
	 */
	public static Bundle generateBundle(final Context context, final boolean fetchImages, final boolean showNotification) {
		final Bundle result = new Bundle();
		result.putLong(BUNDLE_EXTRA_VERSION_CODE, Utils.getAppVersionCode(context));
		result.putBoolean(BUNDLE_EXTRA_IMAGES, fetchImages);
		result.putBoolean(BUNDLE_EXTRA_NOTIFICATION, showNotification);
		return result;
	}

	public static boolean isSaveImages(@NonNull final Bundle bundle) {
		return bundle.getBoolean(BUNDLE_EXTRA_IMAGES, false);
	}

	public static boolean isShowNotification(@NonNull final Bundle bundle) {
		return bundle.getBoolean(BUNDLE_EXTRA_NOTIFICATION, false);
	}

	/**
	 * Private constructor prevents instantiation
	 *
	 * @throws UnsupportedOperationException because this class cannot be instantiated.
	 */
	private PluginBundleManager() {
		throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
	}

}
