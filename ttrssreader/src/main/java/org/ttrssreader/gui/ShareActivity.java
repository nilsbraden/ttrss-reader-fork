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

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.gui.fragments.MainListFragment;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.PostMortemReportExceptionHandler;

import androidx.annotation.NonNull;

public class ShareActivity extends MenuActivity {

	//	private static final String TAG = ShareActivity.class.getSimpleName();
	protected PostMortemReportExceptionHandler mDamageReport = new PostMortemReportExceptionHandler(this);

	private static final String PARAM_TITLE = "title";
	private static final String PARAM_URL = "url";
	private static final String PARAM_CONTENT = "content";

	private EditText title;
	private EditText url;
	private EditText content;

	String m_TitleValue;
	String m_UrlValue;
	String m_ContentValue;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(Controller.getInstance().getThemeResource());
		Controller.getInstance().initializeThemeMode();
		mDamageReport.initialize();

		setTitle(R.string.IntentPublish);

		String titleValue = getIntent().getStringExtra(Intent.EXTRA_SUBJECT);
		String urlValue = getIntent().getStringExtra(Intent.EXTRA_TEXT);
		String contentValue = "";

		if (savedInstanceState != null) {
			titleValue = savedInstanceState.getString(PARAM_TITLE);
			urlValue = savedInstanceState.getString(PARAM_URL);
			contentValue = savedInstanceState.getString(PARAM_CONTENT);
		}

		title = findViewById(R.id.share_title);
		url = findViewById(R.id.share_url);
		content = findViewById(R.id.share_content);

		title.setText(titleValue);
		url.setText(urlValue);
		content.setText(contentValue);

		Button shareButton = findViewById(R.id.share_ok_button);
		shareButton.setOnClickListener(v -> {
			Toast.makeText(getApplicationContext(), "Sending update...", Toast.LENGTH_SHORT).show();

			m_TitleValue = title.getText().toString();
			m_UrlValue = url.getText().toString();
			m_ContentValue = content.getText().toString();

			new MyPublisherTask().execute();
		});
	}

	@Override
	protected int getLayoutResource() {
		return useTabletLayout()
			? R.layout.sharetopublished_tablet
			: R.layout.sharetopublished;
	}

	@Override
	protected void onResume() {
		super.onResume();
		doRefresh();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle out) {
		super.onSaveInstanceState(out);

		EditText url = findViewById(R.id.share_url);
		EditText title = findViewById(R.id.share_title);
		EditText content = findViewById(R.id.share_content);
		out.putString(PARAM_TITLE, title.getText().toString());
		out.putString(PARAM_URL, url.getText().toString());
		out.putString(PARAM_CONTENT, content.getText().toString());
	}

	@Override
	protected void onDestroy() {
		mDamageReport.restoreOriginalHandler();
		mDamageReport = null;
		super.onDestroy();
	}

	private class MyPublisherTask extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... params) {

			try {
				boolean ret = Data.getInstance().shareToPublished(m_TitleValue, m_UrlValue, m_ContentValue);

				if (ret)
					finishCompat();
				else if (Controller.getInstance().getConnector().hasLastError())
					showErrorDialog(Controller.getInstance().getConnector().pullLastError());
				else if (Controller.getInstance().workOffline())
					showErrorDialog("Working offline, synchronisation of published articles is not implemented yet.");
				else
					showErrorDialog("An unknown error occurred.");

			} catch (RuntimeException r) {
				showErrorDialog(r.getMessage());
			}

			return null;
		}

		private void finishCompat() {
			if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
				finishAffinity();
			else
				finish();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (super.onCreateOptionsMenu(menu)) {
			menu.removeItem(R.id.Menu_Refresh);
			menu.removeItem(R.id.Menu_MarkAllRead);
			menu.removeItem(R.id.Menu_MarkFeedsRead);
			menu.removeItem(R.id.Menu_MarkFeedRead);
			menu.removeItem(R.id.Menu_FeedSubscribe);
			menu.removeItem(R.id.Menu_FeedUnsubscribe);
			menu.removeItem(R.id.Menu_DisplayOnlyUnread);
			menu.removeItem(R.id.Menu_InvertSort);
		}
		return true;
	}

	// @formatter:off // Not needed here:
	@Override
	public void itemSelected(MainListFragment source,  int selectedId) {
	}

	@Override
	protected void doUpdate(boolean forceUpdate) {
	}
	//@formatter:on

}
