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

import org.jetbrains.annotations.NotNull;
import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.gui.fragments.PreferencesFragment;
import org.ttrssreader.model.HeaderAdapter;
import org.ttrssreader.preferences.Constants;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.PostMortemReportExceptionHandler;
import org.ttrssreader.utils.Utils;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.Toast;

import java.util.List;

public class PreferencesActivity extends PreferenceActivity implements Toolbar.OnMenuItemClickListener {

	@SuppressWarnings("unused")
	private static final String TAG = PreferencesActivity.class.getSimpleName();

	public static final int ACTIVITY_RELOAD = 45;

	private PostMortemReportExceptionHandler mDamageReport = new PostMortemReportExceptionHandler(this);

	private static AsyncTask<Void, Void, Void> init;
	private static List<Header> _headers;
	private boolean needResource = false;

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
		Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar_preferences, root, false);
		bar.inflateMenu(R.menu.preferences);
		bar.setOnMenuItemClickListener(this);
		//		bar.setMenu();
		root.addView(bar, 0); // insert at top
		bar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(Controller.getInstance().getTheme());
		super.onCreate(savedInstanceState); // IMPORTANT!

		mDamageReport.initialize();
		setResult(Constants.ACTIVITY_SHOW_PREFERENCES);

		if (needResource) {
			addPreferencesFromResource(R.xml.prefs_main_top);
			addPreferencesFromResource(R.xml.prefs_http);
			addPreferencesFromResource(R.xml.prefs_ssl);
			addPreferencesFromResource(R.xml.prefs_wifi);
			addPreferencesFromResource(R.xml.prefs_usage);
			addPreferencesFromResource(R.xml.prefs_display);
			addPreferencesFromResource(R.xml.prefs_system);
			addPreferencesFromResource(R.xml.prefs_main_bottom);
		}
	}

	@Override
	public void onBuildHeaders(List<Header> headers) {
		_headers = headers;
		if (onIsHidingHeaders()) {
			needResource = true;
		} else {
			loadHeadersFromResource(R.xml.prefs_headers, headers);
		}
	}

	@Override
	public void setListAdapter(ListAdapter adapter) {
		if (adapter != null && _headers != null) {
			super.setListAdapter(new HeaderAdapter(this, _headers));
		} else {
			super.setListAdapter(null);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		PreferenceManager.getDefaultSharedPreferences(this)
				.unregisterOnSharedPreferenceChangeListener(Controller.getInstance());
	}

	@Override
	protected void onResume() {
		super.onResume();
		PreferenceManager.getDefaultSharedPreferences(this)
				.registerOnSharedPreferenceChangeListener(Controller.getInstance());
	}

	@Override
	protected void onStop() {
		super.onStop();

		if (init != null) {
			init.cancel(true);
			init = null;
		}

		if (!Utils.checkIsConfigInvalid()) {
			init = new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					Controller.getInstance().initialize(getApplicationContext());
					return null;
				}
			};
			init.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		if (Controller.getInstance().isPreferencesChanged()) {
			new BackupManager(this).dataChanged();
			Controller.getInstance().setPreferencesChanged(false);
		}
	}

	@Override
	protected void onDestroy() {
		mDamageReport.restoreOriginalHandler();
		mDamageReport = null;
		super.onDestroy();
	}

	@Override
	protected boolean isValidFragment(String fragmentName) {
		return PreferencesFragment.class.getName().equals(fragmentName);
	}

	@Override
	public void switchToHeader(@NotNull Header header) {
		if (header.fragment != null) {
			super.switchToHeader(header);
		}
	}

	@Override
	public View onCreateView(String name, Context context, AttributeSet attrs) {
		// Allow super to try and create a view first
		final View result = super.onCreateView(name, context, attrs);
		if (result != null) {
			return result;
		}

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			// If we're running pre-L, we need to 'inject' our tint aware Views in place of the
			// standard framework versions
			switch (name) {
				case "EditText":
					return new AppCompatEditText(this, attrs);
				case "Spinner":
					return new AppCompatSpinner(this, attrs);
				case "CheckBox":
					return new AppCompatCheckBox(this, attrs);
				case "RadioButton":
					return new AppCompatRadioButton(this, attrs);
				case "CheckedTextView":
					return new AppCompatCheckedTextView(this, attrs);
			}
		}

		return null;
	}

	@Override
	public boolean onMenuItemClick(MenuItem menuItem) {
		int id = menuItem.getItemId();

		boolean doReset;
		doReset = (id == R.id.Preferences_Menu_Reset);
		doReset |= (id == R.id.Preferences_Menu_ResetCache);
		doReset |= (id == R.id.Preferences_Menu_ResetDatabase);

		if (doReset) new ResetTask(this).execute(menuItem.getItemId());

		return doReset;
	}

	/**
	 * All Reset-Operations should be done from Background.
	 */
	private class ResetTask extends AsyncTask<Integer, Void, Void> {

		private final Context context;
		private int textResource;

		public ResetTask(Context context) {
			this.context = context;
		}

		@Override
		protected Void doInBackground(Integer... params) {
			switch (params[0]) {
				case R.id.Preferences_Menu_Reset:
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
					Constants.resetPreferences(prefs);
					textResource = R.string.Preferences_Reset_Done;
					break;
				case R.id.Preferences_Menu_ResetDatabase:
					Controller.getInstance().setDeleteDBScheduled(true);
					Controller.getInstance().setSinceId(0);
					DBHelper.getInstance().initialize(context);
					Data.getInstance().initTimers();
					textResource = R.string.Preferences_ResetDatabase_Done;
					break;
				case R.id.Preferences_Menu_ResetCache:
					Data.getInstance().deleteAllRemoteFiles();
					DBHelper.getInstance().initialize(context);
					textResource = R.string.Preferences_ResetCache_Done;
					break;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			Toast.makeText(context, textResource, Toast.LENGTH_SHORT).show();
			setResult(ACTIVITY_RELOAD);
			finish();
			super.onPostExecute(aVoid);
		}
	}

}
