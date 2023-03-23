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

package org.ttrssreader.gui;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.utils.PostMortemReportExceptionHandler;

public class MediaPlayerActivity extends Activity {

	//	private static final String TAG = MediaPlayerActivity.class.getSimpleName();

	private PostMortemReportExceptionHandler mDamageReport = new PostMortemReportExceptionHandler(this);

	public static final String URL = "media_url";

	@Override
	protected void onCreate(Bundle instance) {
		super.onCreate(instance);
		setTheme(Controller.getInstance().getThemeResource());
		Controller.getInstance().initializeThemeMode();
		mDamageReport.initialize();

		setContentView(R.layout.media);

		Bundle extras = getIntent().getExtras();
		String url;
		if (extras != null) {
			url = extras.getString(URL);
		} else if (instance != null) {
			url = instance.getString(URL);
		} else {
			url = "";
		}

		VideoView videoView = (VideoView) findViewById(R.id.MediaView);
		MediaController mediaController = new MediaController(this);
		mediaController.setAnchorView(videoView);
		Uri video = Uri.parse(url);
		videoView.setMediaController(mediaController);
		videoView.setVideoURI(video);
		videoView.start();
	}

	@Override
	protected void onDestroy() {
		mDamageReport.restoreOriginalHandler();
		mDamageReport = null;
		super.onDestroy();
	}

}
