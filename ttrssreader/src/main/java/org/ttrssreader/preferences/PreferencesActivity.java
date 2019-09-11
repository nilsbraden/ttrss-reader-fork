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

package org.ttrssreader.preferences;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.preferences.fragments.PreferencesFragment;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.PostMortemReportExceptionHandler;
import org.ttrssreader.utils.Utils;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class PreferencesActivity extends FragmentActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

	@SuppressWarnings("unused")
	private static final String TAG = PreferencesActivity.class.getSimpleName();

	public static final String KEY_SSID = "SSID";
	public static final int ACTIVITY_RELOAD = 45;

	private static AsyncTask<Void, Void, Void> init;
	private static String TITLE_TAG = "settingsActivityTitle";

	private PostMortemReportExceptionHandler mDamageReport = new PostMortemReportExceptionHandler(this);

	private CharSequence title;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); // IMPORTANT!
		setContentView(R.layout.preferences);
		setTheme(Controller.getInstance().getTheme());
		setResult(Constants.ACTIVITY_SHOW_PREFERENCES);
		mDamageReport.initialize();

		if (savedInstanceState == null) {
			FragmentManager fm = getSupportFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			ft.replace(R.id.settings, new PreferencesFragment());
			ft.commit();
		} else {
			title = savedInstanceState.getCharSequence(TITLE_TAG);
		}
		getSupportFragmentManager().addOnBackStackChangedListener(() -> {
			if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
				setTitle(R.string.PreferencesTitle);
			}
		});

		if (getActionBar() != null)
			getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO: Menu is not displayed, why is this method not called?
		Log.d(TAG, "ACHTUNG MENU WIRD ERZEUGT!!!");
		getMenuInflater().inflate(R.menu.preferences, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onPause() {
		super.onPause();
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(Controller.getInstance());
	}

	@Override
	protected void onResume() {
		super.onResume();
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(Controller.getInstance());
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
	public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
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
					return new EditText(this, attrs);
				case "Spinner":
					return new Spinner(this, attrs);
				case "CheckBox":
					return new CheckBox(this, attrs);
				case "RadioButton":
					return new RadioButton(this, attrs);
				case "CheckedTextView":
					return new CheckedTextView(this, attrs);
			}
		}

		return null;
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		// Save current activity title so we can set it again after a configuration change
		outState.putCharSequence(TITLE_TAG, title);
	}

	@Override
	public boolean onNavigateUp() {
		if (getSupportFragmentManager().popBackStackImmediate()) {
			return true;
		}
		return super.onNavigateUp();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		boolean doReset;
		doReset = (id == R.id.Preferences_Menu_Reset);
		doReset |= (id == R.id.Preferences_Menu_ResetCache);
		doReset |= (id == R.id.Preferences_Menu_ResetDatabase);

		if (doReset)
			new ResetTask(this).execute(item.getItemId());

		return doReset;
	}

	@Override
	public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
		// Instantiate the new Fragment
		Bundle args = pref.getExtras();
		FragmentManager fm = getSupportFragmentManager();
		Fragment fragment = fm.getFragmentFactory().instantiate(getClassLoader(), pref.getFragment());
		fragment.setArguments(args);
		fragment.setTargetFragment(caller, 0);

		// Replace the existing Fragment with the new Fragment
		FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.settings, fragment);
		ft.addToBackStack(null);
		ft.commit();

		title = pref.getTitle();
		return true;
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
