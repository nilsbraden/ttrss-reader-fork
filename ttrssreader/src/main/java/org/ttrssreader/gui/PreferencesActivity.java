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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.internal.widget.TintCheckBox;
import android.support.v7.internal.widget.TintCheckedTextView;
import android.support.v7.internal.widget.TintEditText;
import android.support.v7.internal.widget.TintRadioButton;
import android.support.v7.internal.widget.TintSpinner;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

import java.util.List;

public class PreferencesActivity extends PreferenceActivity implements Toolbar.OnMenuItemClickListener {

	@SuppressWarnings("unused")
	private static final String TAG = PreferencesActivity.class.getSimpleName();

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

	/**
	 * Try to add tinting for devices below lollipop (source: http://stackoverflow.com/a/27455363)
	 */
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
					return new TintEditText(this, attrs);
				case "Spinner":
					return new TintSpinner(this, attrs);
				case "CheckBox":
					return new TintCheckBox(this, attrs);
				case "RadioButton":
					return new TintRadioButton(this, attrs);
				case "CheckedTextView":
					return new TintCheckedTextView(this, attrs);
			}
		}

		return null;
	}

	@Override
	public boolean onMenuItemClick(MenuItem menuItem) {
		Log.w(TAG, "== MenuItem clicked: " + menuItem.getItemId());
		ComponentName comp = new ComponentName(this.getPackageName(), getClass().getName());
		switch (menuItem.getItemId()) {
			case R.id.Preferences_Menu_Reset:
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
				Constants.resetPreferences(prefs);
				this.finish();
				startActivity(new Intent().setComponent(comp));
				return true;
			case R.id.Preferences_Menu_ResetDatabase:
				Controller.getInstance().setDeleteDBScheduled(true);
				DBHelper.getInstance().initialize(this);
				this.finish();
				startActivity(new Intent().setComponent(comp));
				return true;
			case R.id.Preferences_Menu_ResetCache:
				Data.getInstance().deleteAllRemoteFiles();
				DBHelper.getInstance().initialize(this);
				this.finish();
				startActivity(new Intent().setComponent(comp));
				return true;
		}
		return false;
	}

}
