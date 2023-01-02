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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.controllers.ProgressBarManager;
import org.ttrssreader.gui.fragments.MainListFragment;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.net.JSONConnector.SubscriptionResponse;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.PostMortemReportExceptionHandler;
import org.ttrssreader.utils.Utils;

import java.util.ArrayList;

import androidx.annotation.NonNull;

public class SubscribeActivity extends MenuActivity {

	private static final String TAG = SubscribeActivity.class.getSimpleName();

	private PostMortemReportExceptionHandler mDamageReport = new PostMortemReportExceptionHandler(this);

	private static final String PARAM_FEEDURL = "feed_url";

	private Button feedPasteButton;
	private EditText feedUrl;

	private ArrayAdapter<Category> categoriesAdapter;
	private Spinner categorieSpinner;

	String m_UrlValue;
	Category m_Category;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(Controller.getInstance().getThemeResource());
		Controller.getInstance().initializeThemeMode();
		mDamageReport.initialize();

		setTitle(R.string.IntentSubscribe);
		ProgressBarManager.getInstance().addProgress(activity);
		setMyProgressBarVisibility(true);

		String urlValue = getIntent().getStringExtra(Intent.EXTRA_TEXT);

		if (savedInstanceState != null) {
			urlValue = savedInstanceState.getString(PARAM_FEEDURL);
		}

		feedUrl = findViewById(R.id.subscribe_url);
		feedUrl.setText(urlValue);

		categoriesAdapter = new SimpleCategoryAdapter(getApplicationContext());
		categoriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
		categorieSpinner = findViewById(R.id.subscribe_categories);
		categorieSpinner.setAdapter(categoriesAdapter);

		SubscribeCategoryUpdater categoryUpdater = new SubscribeCategoryUpdater();
		categoryUpdater.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

		Button okButton = findViewById(R.id.subscribe_ok_button);
		okButton.setOnClickListener(v -> {

			int itemNumber = (int) categorieSpinner.getSelectedItemId();
			if (itemNumber < 0 || categoriesAdapter.getCount() < itemNumber) {
				Toast.makeText(getApplicationContext(), "Couldn't find selected category.", Toast.LENGTH_SHORT).show();
				Log.e(TAG, "Couldn't find selected category #" + itemNumber);
				return;
			}
			if (categoriesAdapter.getCount() == 0) {
				Toast.makeText(getApplicationContext(), "Could not read categories.", Toast.LENGTH_SHORT).show();
				Log.e(TAG, "Could not read categories.");
				return;
			}

			Toast.makeText(getApplicationContext(), "Sending update...", Toast.LENGTH_SHORT).show();
			m_UrlValue = feedUrl.getText().toString();
			m_Category = categoriesAdapter.getItem(itemNumber);

			new MyPublisherTask().execute();
		});

		feedPasteButton = findViewById(R.id.subscribe_paste);
		feedPasteButton.setOnClickListener(v -> {
			String url = Utils.getTextFromClipboard(activity);
			if (url == null) {
				Toast.makeText(getApplicationContext(), R.string.SubscribeActivity_noClip, Toast.LENGTH_SHORT).show();
				return;
			}

			String text = feedUrl.getText() != null ? feedUrl.getText().toString() : "";
			int start = feedUrl.getSelectionStart();
			int end = feedUrl.getSelectionEnd();
			// Insert text at current position, replace text if selected
			if (start <= end && end <= text.length()) {
				text = text.substring(0, start) + url + text.substring(end);
			}
			feedUrl.setText(text);
			feedUrl.setSelection(end);
		});
	}

	@Override
	protected int getLayoutResource() {
		return useTabletLayout()
			? R.layout.feedsubscribe_tablet
			: R.layout.feedsubscribe;
	}

	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		// Enable/Disable Paste-Button:
		if (hasFocus)
			feedPasteButton.setEnabled(Utils.clipboardHasText(getApplicationContext()));
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle out) {
		super.onSaveInstanceState(out);
		EditText url = findViewById(R.id.subscribe_url);
		out.putString(PARAM_FEEDURL, url.getText().toString());
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
				if (Controller.getInstance().workOffline()) {
					showErrorDialog(getResources().getString(R.string.SubscribeActivity_offline));
					return null;
				}

				if (!Utils.validateURL(m_UrlValue)) {
					showErrorDialog(getResources().getString(R.string.SubscribeActivity_invalidUrl));
					return null;
				}

				SubscriptionResponse ret = Data.getInstance().feedSubscribe(m_UrlValue, m_Category.id);
				String message = "\n\n(" + ret.message + ")";

				if (ret.code == 0)
					showErrorDialog(getResources().getString(R.string.SubscribeActivity_invalidUrl));
				if (ret.code == 1)
					finish();
				else if (Controller.getInstance().getConnector().hasLastError())
					showErrorDialog(Controller.getInstance().getConnector().pullLastError());
				else if (ret.code == 2)
					showErrorDialog(getResources().getString(R.string.SubscribeActivity_invalidUrl) + " " + message);
				else if (ret.code == 3)
					showErrorDialog(getResources().getString(R.string.SubscribeActivity_contentIsHTML) + " " + message);
				else if (ret.code == 4)
					showErrorDialog(getResources().getString(R.string.SubscribeActivity_multipleFeeds) + " " + message);
				else if (ret.code == 5)
					showErrorDialog(getResources().getString(R.string.SubscribeActivity_cannotDownload) + " " + message);
				else
					showErrorDialog(getResources().getString(R.string.SubscribeActivity_errorCode, String.valueOf(ret.code), message));

			} catch (Exception e) {
				showErrorDialog(e.getMessage());
			}
			return null;
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

	private class SimpleCategoryAdapter extends ArrayAdapter<Category> {
		private SimpleCategoryAdapter(Context context) {
			super(context, android.R.layout.simple_list_item_1);
		}

		@NonNull
		@Override
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			return initView(position, convertView, parent);
		}

		@Override
		public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
			return initView(position, convertView, parent);
		}

		private View initView(int position, View convertView, ViewGroup parent) {
			if (convertView == null)
				convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);

			TextView tvText1 = convertView.findViewById(android.R.id.text1);

			if (position < 0 || getCount() < position || getCount() == 0) {
				Toast.makeText(getApplicationContext(), "CategoryAdapter: Couldn't find selected item.", Toast.LENGTH_SHORT).show();
				Log.e(TAG, "CategoryAdapter: Couldn't find selected category #" + position);
				return convertView;
			}

			Category cat = getItem(position);
			if (cat != null)
				tvText1.setText(cat.title);

			return convertView;
		}
	}

	// Fill the adapter for the spinner in the background to avoid direct DB-access
	private class SubscribeCategoryUpdater extends AsyncTask<Void, Integer, Void> {
		private ArrayList<Category> catList = null;

		@Override
		protected Void doInBackground(Void... params) {
			catList = new ArrayList<>(DBHelper.getInstance().getAllCategories());
			publishProgress(0);
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if (catList != null && !catList.isEmpty())
				categoriesAdapter.addAll(catList);

			ProgressBarManager.getInstance().removeProgress(activity);
		}
	}

	// @formatter:off // Not needed here:
	@Override
	public void itemSelected(MainListFragment source,  int selectedId) {
	}

	@Override
	protected void doUpdate(boolean forceUpdate) {
	}
	// @formatter:on

}
