package org.ttrssreader.imageCache;

import android.content.Context;

import com.twofortyfouram.spackle.AppBuildInfo;

import net.jcip.annotations.ThreadSafe;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.twofortyfouram.assertion.Assertions.assertNotNull;

/**
 * Manages the {@link com.twofortyfouram.locale.api.LocalePluginIntent#EXTRA_STRING_JSON EXTRA_STRING_JSON} for this
 * plug-in.
 */
@ThreadSafe
public final class PluginJsonValues {

	private static final String TAG = PluginJsonValues.class.getSimpleName();

	/**
	 * Type: {@code String}.
	 * <p>
	 * String message to display in a Toast message.
	 */
	public static final String EXTRA_IMAGES = "org.ttrssreader.FETCH_IMAGES"; //$NON-NLS-1$
	public static final String EXTRA_NOTIFICATION = "org.ttrssreader.SHOW_NOTIFICATION"; //$NON-NLS-1$

	/**
	 * Type: {@code int}.
	 * <p>
	 * versionCode of the plug-in that saved the Bundle.
	 */
	/*
	 * This extra is not strictly required, however it makes backward and forward compatibility
	 * significantly easier. For example, suppose a bug is found in how some version of the plug-in
	 * stored its Bundle. By having the version, the plug-in can better detect when such bugs occur.
	 */
	@NonNull
	/*package*/ static final String LONG_VERSION_CODE = "version_code";//$NON-NLS-1$

	/**
	 * Method to verify the content of the JSON is correct.
	 * <p>
	 * This method will not mutate {@code json}.
	 *
	 * @param json JSON to verify. May be null, which will always return false.
	 * @return true if the JSON is valid, false if the JSON is invalid.
	 */
	public static boolean isJsonValid(@Nullable final JSONObject json) {
		if (null == json) {
			return false;
		}

		if (3 != json.length()) {
			return false;
		}

		// IMAGES
		if (json.isNull(EXTRA_IMAGES)) {
			return false;
		}
		try {
			json.getBoolean(EXTRA_IMAGES);
		} catch (final JSONException e) {
			return false;
		}

		// NOTIFICATION
		if (json.isNull(EXTRA_NOTIFICATION)) {
			return false;
		}
		try {
			json.getBoolean(EXTRA_NOTIFICATION);
		} catch (final JSONException e) {
			return false;
		}

		if (json.isNull(LONG_VERSION_CODE)) {
			return false;
		}

		try {
			json.getInt(LONG_VERSION_CODE);
		} catch (final JSONException e) {
			return false;
		}

		return true;
	}

	/**
	 * @param context Application context.
	 * @return A plug-in bundle as JSON.
	 */
	@NonNull
	public static JSONObject generateJson(@NonNull final Context context, final boolean fetchImages, final boolean showNotification) {
		assertNotNull(context, "context"); //$NON-NLS-1$

		final JSONObject result = new JSONObject();
		try {
			result.put(LONG_VERSION_CODE, AppBuildInfo.getVersionCode(context));
			result.put(EXTRA_IMAGES, fetchImages);
			result.put(EXTRA_NOTIFICATION, showNotification);

			return result;
		} catch (final JSONException e) {
			//A failure creating the JSON object isn't expected.
			throw new RuntimeException(e);
		}
	}

	public static boolean getExtraImages(@NonNull final JSONObject json) {
		try {
			return json.getBoolean(EXTRA_IMAGES);
		} catch (final JSONException e) {
			// Users are expected to validate with isValid() first
			throw new RuntimeException(e);
		}
	}

	public static boolean getExtraNotification(@NonNull final JSONObject json) {
		try {
			return json.getBoolean(EXTRA_NOTIFICATION);
		} catch (final JSONException e) {
			// Users are expected to validate with isValid() first
			throw new RuntimeException(e);
		}
	}

	/**
	 * Private constructor prevents instantiation
	 *
	 * @throws UnsupportedOperationException because this class cannot be instantiated.
	 */
	private PluginJsonValues() {
		throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
	}
}
