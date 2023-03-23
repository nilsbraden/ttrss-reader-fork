/*
 * Copyright (c) 2015, Nils Braden
 *
 * This file is part of ttrss-reader-fork. This program is free software; you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation;
 * either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program; If
 * not, see http://www.gnu.org/licenses/.
 */

package org.ttrssreader.utils;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.preferences.Constants;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;

import androidx.annotation.RequiresApi;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Utils {

	private static final String TAG = Utils.class.getSimpleName();

	public static final long SECOND = 1000;
	public static final long MINUTE = 60 * SECOND;
	public static final long HOUR = 60 * MINUTE;
	public static final long DAY = 24 * HOUR;

	public static final long KB = 1024;
	public static final long MB = KB * KB;

	/**
	 * The maximum number of articles to store.
	 */
	public static final int ARTICLE_LIMIT = 50000;

	/**
	 * Vibrate-Time for vibration when end of list is reached
	 */
	public static final long SHORT_VIBRATE = 50;

	/**
	 * The time after which data will be fetched again from the server if asked for the data
	 */
	public static final long UPDATE_TIME = MINUTE * 30;

	/**
	 * The time after which the DB and other data will be cleaned up again,
	 */
	public static final long CLEANUP_TIME = DAY;

	/**
	 * The Pattern to match image-urls inside HTML img-tags.
	 */
	public static final Pattern findImageUrlsPattern = Pattern.compile("<(?:img|video)[^>]+?src=[\"']([^\"']*)", Pattern.CASE_INSENSITIVE);

	private static final int ID_RUNNING = 4564561;
	private static final int ID_FINISHED = 7897891;

	/**
	 * Different network states
	 */
	public static final int NETWORK_NONE = 0;
	public static final int NETWORK_MOBILE = 1;
	public static final int NETWORK_METERED = 2;
	public static final int NETWORK_WIFI = 3;

	/*
	 * Check if this is the first run of the app.
	 */
	public static boolean checkIsFirstRun() {
		if (Controller.getInstance().isFirstRun()) {
			// Set first run to false anyway.
			Controller.getInstance().setFirstRun(false);
			// Compatibility for already installed apps that don't have this pref yet:
			return Constants.LAST_VERSION_RUN_DEFAULT.equals(Controller.getInstance().getLastVersionRun());
		} else {
			return false;
		}
	}

	/*
	 * Check if a new version of the app was installed, returns true if this is the case. This also triggers the reset
	 * of the preference noCrashreportsUntilUpdate since with a new update the crash reporting should now be enabled
	 * again.
	 */
	public static boolean checkIsNewVersion(Context c) {
		String thisVersion = getAppVersionName(c);
		String lastVersionRun = Controller.getInstance().getLastVersionRun();
		Controller.getInstance().setLastVersionRun(thisVersion);

		if (thisVersion.equals(lastVersionRun)) {
			// No new version installed, perhaps a new version exists
			// Only run task once for every session and only if we are online
			if (!checkConnected((ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE)))
				return false;
			if (AsyncTask.Status.PENDING.equals(updateVersionTask.getStatus()))
				updateVersionTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			return false;
		} else {
			// New update was installed, reset noCrashreportsUntilUpdate and return true to display the changelog...
			Controller.getInstance().setNoCrashreportsUntilUpdate(false);
			return true;
		}
	}

	/*
	 * Checks the config for a user-defined server, returns true if the config is invalid and the user has not yet
	 * entered a valid server adress.
	 */
	public static boolean checkIsConfigInvalid() {
		try {
			URI uri = Controller.getInstance().uri();
			if (uri == null || uri.toASCIIString().equals(Constants.URL_DEFAULT + Controller.JSON_END_URL)) {
				return true;
			}
		} catch (URISyntaxException e) {
			return true;
		}
		return false;
	}

	/**
	 * Retrieves the packaged version-code of the application
	 *
	 * @param c - The Activity to retrieve the current version
	 * @return the version-string
	 */
	public static long getAppVersionCode(Context c) {
		long result;
		try {
			PackageManager manager = c.getPackageManager();
			PackageInfo info = manager.getPackageInfo(c.getPackageName(), 0);

			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
				result = info.versionCode;
			else
				result = info.getLongVersionCode();
		} catch (NameNotFoundException e) {
			Log.w(TAG, "Unable to get application version: " + e.getMessage());
			result = 0;
		}
		return result;
	}

	/**
	 * Retrieves the packaged version-name of the application
	 *
	 * @param c - The Activity to retrieve the current version
	 * @return the version-string
	 */
	public static String getAppVersionName(Context c) {
		String result;
		try {
			PackageManager manager = c.getPackageManager();
			PackageInfo info = manager.getPackageInfo(c.getPackageName(), 0);
			result = info.versionName;
		} catch (NameNotFoundException e) {
			Log.w(TAG, "Unable to get application version: " + e.getMessage());
			result = "";
		}
		return result;
	}

	/**
	 * Checks if the option to work offline is set or if the data-connection isn't established, else returns true. If
	 * we are about to connect it waits for maximum one second and then returns the network state without waiting
	 * anymore.
	 */
	public static boolean isConnected(ConnectivityManager cm) {
		return !Controller.getInstance().workOffline() && checkConnected(cm);
	}

	/**
	 * Wrapper for Method checkConnected(ConnectivityManager cm, boolean onlyWifi)
	 */
	public static boolean checkConnected(ConnectivityManager cm) {
		return checkConnected(cm, Controller.getInstance().onlyUseWifi(), false);
	}

	/**
	 * Only checks the connectivity without regard to the preferences
	 */
	public static boolean checkConnected(ConnectivityManager cm, boolean onlyWifi, boolean onlyUnmeteredNetwork) {
		if (cm == null)
			return false;

		int networkType = getNetworkType(cm);
		if (networkType != NETWORK_NONE) {
			if (onlyWifi && networkType != NETWORK_WIFI)
				return false;
			if (onlyUnmeteredNetwork)
				return networkType != NETWORK_METERED;
			return true;
		}
		return false;
	}

	public static int getNetworkType(final ConnectivityManager cm) {
		int ret;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			ret = getNetworkTypeApi23(cm);
		} else {
			ret = getNetworkTypeApiCurrent(cm);
		}
		//		Log.d(TAG, "GetNetworkType: Type = " + ret + " (0 = none, 1 = mobile, 2 = metered, 3 = wifi)");
		return ret;
	}

	public static int getNetworkTypeApi23(final ConnectivityManager cm) {
		if (cm == null)
			return NETWORK_NONE;
		final NetworkInfo info = cm.getActiveNetworkInfo();
		if (info == null || !info.isConnected()) {
			return NETWORK_NONE;
		} else if (info.getType() != ConnectivityManager.TYPE_WIFI) {
			return NETWORK_MOBILE;
		} else if (cm.isActiveNetworkMetered()) {
			return NETWORK_METERED;
		} else {
			return NETWORK_WIFI;
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.Q)
	public static int getNetworkTypeApiCurrent(final ConnectivityManager cm) {
		if (cm == null)
			return NETWORK_NONE;

		final NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
		if (caps == null)
			return NETWORK_NONE;

		boolean isConnected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
		isConnected &= caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
		boolean isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
		boolean isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);

		if (!isConnected) {
			return NETWORK_NONE;
		} else if (isWifi) {
			return NETWORK_WIFI;
		/*} else if (isMetered) { // Disable this for now, for some reason NET_CAPABILITY_NOT_METERED always returns false...
			Log.d(TAG, "GetNetworkType: NETWORK_METERED");
			return NETWORK_METERED;*/
		} else {
			return NETWORK_MOBILE;
		}
	}

	/**
	 * Allos to send a toast from a background thread
	 *
	 * @param context the context, eg. MyApplication.context()
	 * @param message like Toast.makeText(...)
	 * @param length  like Toast.makeText(...)
	 */
	public static void showBackgroundToast(final Context context, final String message, final int length) {
		Handler handler = new Handler(context.getMainLooper());
		handler.post(() -> Toast.makeText(context, message, length).show());
	}

	public static void showFinishedNotification(String content, int time, boolean error, Context context, String NOTIFICATION_CHANNEL_ID_INFO) {
		showFinishedNotification(content, time, error, context, new Intent(), NOTIFICATION_CHANNEL_ID_INFO);
	}

	/**
	 * Shows a notification with the given parameters
	 *
	 * @param content the string to display
	 * @param time    how long the process took
	 * @param error   set to true if an error occured
	 * @param context the context
	 */
	public static void showFinishedNotification(String content, int time, boolean error, Context context, Intent intent, String NOTIFICATION_CHANNEL_ID_INFO) {
		if (context == null)
			return;

		NotificationManager mNotMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (mNotMan == null)
			return;

		int icon = R.drawable.icon;
		CharSequence title = String.format((String) context.getText(R.string.Utils_DownloadFinishedTitle), time);
		CharSequence ticker = context.getText(R.string.Utils_DownloadFinishedTicker);
		CharSequence text = content;

		if (content == null)
			text = context.getText(R.string.Utils_DownloadFinishedText);

		if (error) {
			title = context.getText(R.string.Utils_DownloadErrorTitle);
			ticker = context.getText(R.string.Utils_DownloadErrorTicker);
		}

		Notification notification = buildNotification(context, icon, ticker, title, text, true, intent, NOTIFICATION_CHANNEL_ID_INFO);
		mNotMan.notify(ID_FINISHED, notification);
	}

	public static void showRunningNotification(Context context, boolean finished, String NOTIFICATION_CHANNEL_ID_INFO) {
		showRunningNotification(context, finished, new Intent(), NOTIFICATION_CHANNEL_ID_INFO);
	}

	/**
	 * Shows a notification indicating that something is running. When called with finished=true it removes the
	 * notification.
	 *
	 * @param context  the context
	 * @param finished if the notification is to be removed
	 */
	private static void showRunningNotification(Context context, boolean finished, Intent intent, String NOTIFICATION_CHANNEL_ID_INFO) {
		if (context == null)
			return;

		NotificationManager mNotMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (mNotMan == null)
			return;

		// if finished remove notification and return, else display notification
		if (finished) {
			mNotMan.cancel(ID_RUNNING);
			return;
		}

		int icon = R.drawable.notification_icon;
		CharSequence title = context.getText(R.string.Utils_DownloadRunningTitle);
		CharSequence ticker = context.getText(R.string.Utils_DownloadRunningTicker);

		Notification notification = buildNotification(context, icon, ticker, title, "…", true, intent, NOTIFICATION_CHANNEL_ID_INFO);
		mNotMan.notify(ID_RUNNING, notification);
	}

	/**
	 * Reads a file from my webserver and parses the content. It containts the version code of the latest supported
	 * version. If the version of the installed app is lower then this the feature "Send mail with stacktrace on error"
	 * will be disabled to make sure I only receive "new" Bugreports.
	 */
	private static final AsyncTask<Void, Void, Void> updateVersionTask = new AsyncTask<Void, Void, Void>() {
		@Override
		protected Void doInBackground(Void... params) {
			// Check last appVersionCheckDate
			long last = Controller.getInstance().appVersionCheckTime();
			if ((System.currentTimeMillis() - last) < (Utils.HOUR * 4))
				return null;

			if (Controller.getInstance().isNoCrashreports())
				return null;

			try {
				URL url = new URL("http://nilsbraden.de/android/tt-rss/minSupportedVersion.txt");
				HttpURLConnection con = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
				con.connect();
				int code = con.getResponseCode();

				if (code < 400 || code >= 600) {

					BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
					String content = br.readLine(); // Just read one line!

					// Only ever read the integer if it matches the regex and is not too long
					if (content.matches("[0-9]*[\\r\\n]*")) {
						content = content.replaceAll("[^0-9]*", "");
						Controller.getInstance().setAppLatestVersion(Integer.parseInt(content));
					}
				}
			} catch (Exception e) {
				// Empty!
			}

			return null;
		}
	};

	public static Notification buildNotification(Context context, int icon, CharSequence ticker, CharSequence title, CharSequence text, boolean autoCancel, Intent intent, String NOTIFICATION_CHANNEL_ID_INFO) {
		Notification notification = null;
		PendingIntent pendingIntent = null;
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
			pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		} else {
			pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
		}

		try {
			Notification.Builder builder;
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
				builder = new Notification.Builder(context);
			} else {
				builder = new Notification.Builder(context, NOTIFICATION_CHANNEL_ID_INFO);
			}
			builder.setSmallIcon(icon);
			builder.setTicker(ticker);
			builder.setWhen(System.currentTimeMillis());
			builder.setContentTitle(title);
			builder.setContentText(text);
			builder.setContentIntent(pendingIntent);
			builder.setAutoCancel(autoCancel);
			notification = builder.build();
		} catch (Exception re) {
			Log.e(TAG, "Exception while building notification. Does your device propagate the right API-Level? (" + Build.VERSION.SDK_INT + ")", re);
		}

		return notification;
	}

	public static String separateItems(Set<?> att, String separator) {
		if (att == null)
			return "";

		String ret;
		StringBuilder sb = new StringBuilder();
		for (Object s : att) {
			sb.append(s);
			sb.append(separator);
		}
		if (att.size() > 0) {
			ret = sb.substring(0, sb.length() - separator.length());
		} else {
			ret = sb.toString();
		}

		return ret;
	}

	private static final String REGEX_URL = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";

	public static boolean validateURL(String url) {
		return url != null && url.matches(REGEX_URL);

	}

	public static String getTextFromClipboard(Context context) {
		// New Clipboard API
		ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
		if (clipboard != null && clipboard.hasPrimaryClip()) {

			ClipDescription cd = clipboard.getPrimaryClipDescription();
			if (cd != null && !cd.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))
				return null;

			ClipData cData = clipboard.getPrimaryClip();
			if (cData != null) {
				ClipData.Item item = cData.getItemAt(0);
				CharSequence chars = item.getText();
				if (chars != null && chars.length() > 0) {
					return chars.toString();
				} else {
					Uri pasteUri = item.getUri();
					if (pasteUri != null) {
						return pasteUri.toString();
					}
				}
			}
		}
		return null;
	}

	public static boolean clipboardHasText(Context context) {
		return (getTextFromClipboard(context) != null);
	}

	public static void alert(Activity activity) {
		alert(activity, false);
	}

	/**
	 * Alert the user by a short vibration or a flash of the whole screen.
	 */
	public static void alert(Activity activity, boolean error) {
		Vibrator vib = ((Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE));
		if (vib != null && vib.hasVibrator()) {

			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
				vib.vibrate(Utils.SHORT_VIBRATE);
			} else {
				VibrationEffect effect = VibrationEffect.createOneShot(Utils.SHORT_VIBRATE, VibrationEffect.DEFAULT_AMPLITUDE);
				vib.vibrate(effect);
			}

		} else if (error) {
			// Only flash when user tried to move forward, flashing when reaching the last article looks just wrong.
			Animation flash = AnimationUtils.loadAnimation(activity, R.anim.flash);
			View main = activity.findViewById(R.id.frame_all);
			main.startAnimation(flash);
		}
	}

	/**
	 * Downloads the given URL and returns the body as an <code>byte[]</code>.
	 *
	 * @param url URL to download
	 * @return content as <code>byte[]</code>
	 */
	public static byte[] download(URL url) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			// Build Request-Object:
			Request.Builder reqBuilder = new Request.Builder();
			reqBuilder.url(url);

			// HTTP-Basic Authentication
			if (Controller.getInstance().useHttpAuth()) {
				String user = Controller.getInstance().httpUsername();
				String pw = Controller.getInstance().httpPassword();
				reqBuilder.addHeader("Authorization", Credentials.basic(user, pw));
			}

			Request request = reqBuilder.build();
			Response response = new OkHttpClient().newCall(request).execute();

			// download the file
			try (ResponseBody body = response.body()) {
				InputStream input = body.byteStream();

				byte[] data = new byte[1024];
				int count;
				while ((count = input.read(data)) != -1) {
					// writing data to file
					output.write(data, 0, count);
				}

				// closing streams
				output.close();
				input.close();
			}

		} catch (Exception e) {
			Log.e(TAG, "Error while downloading feed icon: " + e.getMessage());
		}

		if (output.size() != 0) {
			Log.d(TAG, "Downloaded " + output.size() + " bytes as feed icon from " + url.toExternalForm());
			return output.toByteArray();
		} else {
			return null;
		}
	}

	/**
	 * Returns a base64 encoded representation of the input string and considers the current android version to access the appropriate API.
	 *
	 * @param input the string to be encoded
	 * @return the base64 encoded representation of the string
	 */
	@SuppressWarnings("CharsetObjectCanBeUsed")
	public static String encodeBase64ToString(String input) {
		return Base64.encodeToString(input.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
	}

}
