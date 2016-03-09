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

package org.ttrssreader.gui.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.webkit.URLUtil;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.ttrssreader.MyApplication;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.gui.MediaPlayerActivity;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.FileUtils;
import org.ttrssreader.utils.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

public class ArticleWebViewClient extends WebViewClient {

	private static final String TAG = ArticleWebViewClient.class.getSimpleName();

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, final String url) {

		final Context context = view.getContext();

		boolean audio = false;
		boolean video = false;

		for (String s : FileUtils.AUDIO_EXTENSIONS) {
			if (url.toLowerCase(Locale.getDefault()).contains("." + s)) {
				audio = true;
				break;
			}
		}

		for (String s : FileUtils.VIDEO_EXTENSIONS) {
			if (url.toLowerCase(Locale.getDefault()).contains("." + s)) {
				video = true;
				break;
			}
		}

		final String contentType = audio ? "audio/*" : "video/*";

		if (audio || video) {
			// @formatter:off
			final CharSequence[] items = {
					context.getText(R.string.WebViewClientActivity_Display),
					context.getText(R.string.WebViewClientActivity_Download),
					context.getText(R.string.WebViewClientActivity_ChooseApp)};
			// @formatter:on

			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle("What shall we do?");
			builder.setItems(items, new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int item) {

					switch (item) {
						case 0:
							Log.i(TAG, "Displaying file in mediaplayer: " + url);
							Intent i = new Intent(context, MediaPlayerActivity.class);
							i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							i.putExtra(MediaPlayerActivity.URL, url);
							context.startActivity(i);
							break;
						case 1:
							try {
								new AsyncMediaDownloader(context)
										.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new URL(url));
							} catch (MalformedURLException e) {
								e.printStackTrace();
							}
							break;
						case 2:
							Intent intent = new Intent();
							intent.setAction(android.content.Intent.ACTION_VIEW);
							intent.setDataAndType(Uri.parse(url), contentType);
							context.startActivity(intent);
							break;
						default:
							Log.e(TAG, "Doing nothing, but why is that?? Item: " + item);
							break;
					}
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
		} else {
			Uri uri = Uri.parse(url);

			try {
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intent);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return true;
	}

	private class AsyncMediaDownloader extends AsyncTask<URL, Void, Void> {
		private final static int BUFFER = (int) Utils.KB;

		private WeakReference<Context> contextRef;

		public AsyncMediaDownloader(Context context) {
			this.contextRef = new WeakReference<>(context);
		}

		protected Void doInBackground(URL... urls) {

			if (urls.length < 1) {
				String msg = "No URL given, skipping download...";
				Log.w(TAG, msg);
				Utils.showFinishedNotification(msg, 0, true, contextRef.get());
				return null;
			} else if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
				String msg = "External Storage not available, skipping download...";
				Log.w(TAG, msg);
				Utils.showFinishedNotification(msg, 0, true, contextRef.get());
				return null;
			}

			long start = System.currentTimeMillis();
			Utils.showRunningNotification(contextRef.get(), false);

			// Use configured output directory
			File folder = new File(Controller.getInstance().saveAttachmentPath());
			if (!folder.exists() && !folder.mkdirs()) {
				// Folder could not be created, fallback to internal directory on sdcard
				folder = MyApplication.context().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
				if (folder != null && !folder.exists() && !folder.mkdirs()) {
					String msg = "Folder could not be created: " + folder.getAbsolutePath();
					Log.w(TAG, msg);
					Utils.showFinishedNotification(msg, 0, true, contextRef.get());
				}
			}

			BufferedInputStream in;
			FileOutputStream fos;
			BufferedOutputStream bout = null;

			int size = -1;

			File file;
			try {
				URL url = urls[0];
				URLConnection c;

				c = Controller.getInstance().openConnection(url);

				file = new File(folder, URLUtil.guessFileName(url.toString(), null, ".mp3"));
				if (file.exists()) {
					size = (int) file.length();
					c.setRequestProperty("Range", "bytes=" + size + "-"); // try to resume downloads
				}

				in = new BufferedInputStream(c.getInputStream());
				fos = (size == 0) ? new FileOutputStream(file) : new FileOutputStream(file, true);
				bout = new BufferedOutputStream(fos, BUFFER);

				byte[] data = new byte[BUFFER];
				int count;
				while ((count = in.read(data, 0, BUFFER)) >= 0) {
					bout.write(data, 0, count);
					size += count;
				}

				int time = (int) ((System.currentTimeMillis() - start) / Utils.SECOND);

				// Show Intent which opens the file
				Intent intent = new Intent();
				intent.setAction(android.content.Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.fromFile(file), FileUtils.getMimeType(file.getName()));

				Log.i(TAG, "Finished. Path: " + file.getAbsolutePath() + " Time: " + time + "s Bytes: " + size);
				Utils.showFinishedNotification(file.getAbsolutePath(), time, false, contextRef.get(), intent);

			} catch (IOException e) {
				String msg = "Error while downloading: " + e;
				Log.e(TAG, msg, e);
				Utils.showFinishedNotification(msg, 0, true, contextRef.get());
			} finally {
				// Remove "running"-notification
				Utils.showRunningNotification(contextRef.get(), true);
				if (bout != null) {
					try {
						bout.close();
					} catch (IOException e) {
						// Empty!
					}
				}
			}
			return null;
		}
	}

	/*
	 * WebKit does not call onReceivedHttpAuthRequest (or onReceivedError for that matter) when
	 * processing resources within a rendered document. As a result, it is not possible to
	 * inject authentication information without intercepting the resource loading itself.
	 */
	@Override
	public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
		Controller controller = Controller.getInstance();

		/* Short-circuit ahead of UrlNeedsAuthentication to avoid needless URL building. */
		if (!controller.useHttpAuth()) return null;

		try {
			URL url = new URL(request.getUrl().toString());

			if (!controller.UrlNeedsAuthentication(url)) return null;

			URLConnection c = controller.openConnection(url);

			return new WebResourceResponse(c.getContentType(), c.getContentEncoding(), c.getInputStream());
		} catch (IOException e) {
			Log.e(TAG, "Failed to fetch " + request.getUrl().toString());
		}

		return null;
	}

}
