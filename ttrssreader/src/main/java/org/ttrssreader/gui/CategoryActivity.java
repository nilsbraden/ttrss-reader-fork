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
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.gui.dialogs.ChangelogDialog;
import org.ttrssreader.gui.dialogs.WelcomeDialog;
import org.ttrssreader.gui.fragments.CategoryListFragment;
import org.ttrssreader.gui.fragments.FeedHeadlineListFragment;
import org.ttrssreader.gui.fragments.FeedListFragment;
import org.ttrssreader.gui.fragments.MainListFragment;
import org.ttrssreader.gui.interfaces.IItemSelectedListener;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.Utils;

import java.util.LinkedHashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class CategoryActivity extends MenuActivity implements IItemSelectedListener {

	private static final String TAG = CategoryActivity.class.getSimpleName();

	private static final String DIALOG_WELCOME = "welcome";
	private static final String DIALOG_UPDATE = "update";

	private static final int SELECTED_VIRTUAL_CATEGORY = 1;
	private static final int SELECTED_CATEGORY = 2;
	private static final int SELECTED_LABEL = 3;

	private boolean cacherStarted = false;
	private CategoryUpdater categoryUpdater = null;

	private static final String SELECTED = "SELECTED";
	private int selectedCategoryId = Integer.MIN_VALUE;

	@Override
	protected void onCreate(Bundle instance) {
		// Only needed to debug ANRs:
		// StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectCustomSlowCalls().detectDiskReads()
		// .detectDiskWrites().detectNetwork().penaltyLog().penaltyLog().build());
		// StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects()
		// .detectLeakedClosableObjects().penaltyLog().build());

		super.onCreate(instance);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			selectedCategoryId = extras.getInt(SELECTED, Integer.MIN_VALUE);
		} else if (instance != null) {
			selectedCategoryId = instance.getInt(SELECTED, Integer.MIN_VALUE);
		}

		FragmentManager fm = getSupportFragmentManager();
		CategoryListFragment categoryFragment = (CategoryListFragment) fm.findFragmentByTag(CategoryListFragment.FRAGMENT);

		if (categoryFragment == null) {
			fm.beginTransaction().add(R.id.frame_main, CategoryListFragment.newInstance(), CategoryListFragment.FRAGMENT).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
		}

		if (Utils.checkIsFirstRun()) {
			WelcomeDialog.getInstance().show(fm, DIALOG_WELCOME);
		} else if (Utils.checkIsNewVersion(this)) {
			ChangelogDialog.getInstance().show(fm, DIALOG_UPDATE);
		} else if (Utils.checkIsConfigInvalid()) {
			// Check if we have a server specified
			openConnectionErrorDialog((String) getText(R.string.CategoryActivity_NoServer));
		}

		// Start caching if requested
		if (Controller.getInstance().cacheImagesOnStartup()) {
			boolean startCache = true;

			if (Controller.getInstance().cacheImagesOnlyWifi()) {
				// Check if Wifi is connected, if not don't start the ImageCache
				ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

				if (!Utils.checkConnected(cm, true, true)) {
					Log.i(TAG, "Preference Start ImageCache only on WIFI set, doing nothing...");
					startCache = false;
				}
			}

			// Indicate that the cacher started anyway so the refresh is supressed if the ImageCache is configured but
			// only for Wifi.
			cacherStarted = true;

			if (startCache) {
				Log.i(TAG, "Starting ImageCache...");
				doStartImageCache();
			}
		}
	}

	@Override
	protected int getLayoutResource() {
		return useTabletLayout()
			? R.layout.main_tablet
			: R.layout.main;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putInt(SELECTED, selectedCategoryId);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle instance) {
		selectedCategoryId = instance.getInt(SELECTED, Integer.MIN_VALUE);
		super.onRestoreInstanceState(instance);
	}

	@Override
	public void dataLoadingFinished() {
		setTitleAndUnread();
	}

	@Override
	protected void doRefresh() {
		super.doRefresh();

		CategoryListFragment categoryFragment = getCategoryListFragment();
		if (categoryFragment != null)
			categoryFragment.doRefresh();

		FeedListFragment feedFragment = getFeedListFragment();
		if (feedFragment != null)
			feedFragment.doRefresh();

		setTitleAndUnread();
	}

	public void setTitleAndUnread() {
		// Title and unread information:
		FeedListFragment feedFragment = getFeedListFragment();
		if (feedFragment != null && feedFragment.isVisible() && !feedFragment.isEmptyPlaceholder()) {
			setTitle(feedFragment.getTitle());
			setUnread(feedFragment.getUnread());
		} else {
			CategoryListFragment categoryFragment = getCategoryListFragment();
			if (categoryFragment != null && categoryFragment.isVisible()) {
				setTitle(categoryFragment.getTitle());
				setUnread(categoryFragment.getUnread());
			}
		}
	}

	@Override
	protected void doUpdate(boolean forceUpdate) {
		// Only update if no categoryUpdater already running
		if (categoryUpdater != null) {
			if (categoryUpdater.getStatus().equals(AsyncTask.Status.FINISHED)) {
				categoryUpdater = null;
			} else {
				return;
			}
		}

		if (Data.getInstance().isConnected()) {
			if ((!isCacherRunning() && !cacherStarted) || forceUpdate) {
				categoryUpdater = new CategoryUpdater(forceUpdate);
				categoryUpdater.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean ret = super.onPrepareOptionsMenu(menu);
		menu.removeItem(R.id.Menu_MarkFeedRead);

		if (!Controller.isTablet && selectedCategoryId != Integer.MIN_VALUE)
			menu.removeItem(R.id.Menu_MarkAllRead);
		if (selectedCategoryId == Integer.MIN_VALUE)
			menu.removeItem(R.id.Menu_MarkFeedsRead);

		return ret;
	}

	@Override
	public final boolean onOptionsItemSelected(@NonNull final MenuItem item) {
		if (super.onOptionsItemSelected(item))
			return true;

		if (item.getItemId() == R.id.Menu_Refresh) {
			doUpdate(true);
			return true;
		}

		return false;
	}

	/**
	 * This does a full update including all labels, feeds, categories and all articles.
	 */
	private class CategoryUpdater extends ActivityUpdater {
		private static final int DEFAULT_TASK_COUNT = 6;

		private CategoryUpdater(boolean forceUpdate) {
			super(forceUpdate);
		}

		@Override
		protected Void doInBackground(Void... params) {
			boolean onlyUnreadArticles = Controller.getInstance().onlyUnread();

			Set<Feed> labels = new LinkedHashSet<>();
			for (Feed f : DBHelper.getInstance().getFeeds(-2)) {
				if (f.unread == 0 && onlyUnreadArticles)
					continue;
				labels.add(f);
			}

			taskCount = DEFAULT_TASK_COUNT + labels.size();
			int progress = 0;
			publishProgress(progress);

			// Try to synchronize any ids left in TABLE_MARK:
			Data.getInstance().synchronizeStatus();
			publishProgress(++progress);

			// Cache articles for all categories
			Data.getInstance().cacheArticles(false, forceUpdate);
			publishProgress(++progress);

			// Refresh articles for all labels
			for (Feed f : labels) {
				Data.getInstance().updateArticles(f.id, false, false, false, forceUpdate);
				publishProgress(++progress);
			}

			// This stuff will be done in background without UI-notification, but the progress-calls will be done
			// anyway to ensure the UI is refreshed properly.
			Data.getInstance().updateVirtualCategories(getApplicationContext());
			publishProgress(++progress);

			Data.getInstance().updateCategories(false);
			publishProgress(++progress);

			Set<Feed> feeds = Data.getInstance().updateFeeds(Data.VCAT_ALL, false);
			publishProgress(++progress);

			Data.getInstance().calculateCounters();
			Data.getInstance().notifyListeners();
			publishProgress(Integer.MAX_VALUE); // Move progress forward to 100%

			// Silently remove articles which belong to feeds which do not exist on the server anymore:
			Data.getInstance().purgeOrphanedArticles();

			// Silently update all feed-icons
			if (feeds != null && Controller.getInstance().displayFeedIcons()) {
				for (Feed f : feeds) {
					// Virtual feeds don't have icons...
					Data.getInstance().updateFeedIcon(f.id);
				}
			}
			return null;
		}
	}

	@Override
	public void itemSelected(MainListFragment source, int selectedId) {
		switch (source.getType()) {
			case CATEGORY:
				switch (decideCategorySelection(selectedId)) {
					case SELECTED_VIRTUAL_CATEGORY:
						displayHeadlines(selectedId, 0, false);
						break;
					case SELECTED_LABEL:
						displayHeadlines(selectedId, -2, false);
						break;
					case SELECTED_CATEGORY:
						if (Controller.getInstance().invertBrowsing()) {
							displayHeadlines(FeedHeadlineActivity.FEED_NO_ID, selectedId, true);
						} else {
							displayFeed(selectedId);
						}
						break;
				}
				break;
			case FEED:
				FeedListFragment feeds = (FeedListFragment) source;
				displayHeadlines(selectedId, feeds.getCategoryId(), false);
				break;
			default:
				Toast.makeText(this, "Invalid request!", Toast.LENGTH_SHORT).show();
				break;
		}
	}

	public void displayHeadlines(int feedId, int categoryId, boolean selectArticles) {
		ActionBar ab = getSupportActionBar();
		boolean hideTitlebar = false;
		if (ab != null)
			hideTitlebar = !ab.isShowing();

		Intent i = new Intent(this, FeedHeadlineActivity.class);
		i.putExtra(FeedHeadlineListFragment.FEED_CAT_ID, categoryId);
		i.putExtra(FeedHeadlineListFragment.FEED_ID, feedId);
		i.putExtra(FeedHeadlineListFragment.FEED_SELECT_ARTICLES, selectArticles);
		i.putExtra(FeedHeadlineListFragment.ARTICLE_ID, Integer.MIN_VALUE);
		i.putExtra(FeedHeadlineListFragment.TITLEBAR_HIDDEN, hideTitlebar);
		startActivity(i);
	}

	public void displayFeed(int categoryId) {
		if (mOnSaveInstanceStateCalled) {
			Log.w(TAG, "displayFeed() has been called after onSaveInstanceState(), this call has been supressed!");
			Toast.makeText(this, "displayFeed() has been called after onSaveInstanceState(), this call has been supressed!", Toast.LENGTH_SHORT).show();
			return;
		}

		selectedCategoryId = categoryId;
		FeedListFragment feedFragment = FeedListFragment.newInstance(categoryId);
		FragmentManager fm = getSupportFragmentManager();

		// Clear back stack
		fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

		FragmentTransaction ft = fm.beginTransaction();
		if (!Controller.isTablet)
			ft.addToBackStack(null);
		ft.replace(R.id.frame_sub, feedFragment, FeedListFragment.FRAGMENT);

		// Animation
		if (Controller.isTablet)
			ft.setCustomAnimations(R.animator.slide_in_left, android.R.animator.fade_out, android.R.animator.fade_in, R.animator.slide_out_left);
		else
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

		ft.commit();
	}

	private static int decideCategorySelection(int selectedId) {
		if (selectedId < 0 && selectedId >= -4) {
			return SELECTED_VIRTUAL_CATEGORY;
		} else if (selectedId < -10) {
			return SELECTED_LABEL;
		} else {
			return SELECTED_CATEGORY;
		}
	}

	@Override
	public void onBackPressed() {
		selectedCategoryId = Integer.MIN_VALUE;
		// Back button automatically finishes the activity since Lollipop
		// so we have to work around by checking the backstack before
		FragmentManager fm = getSupportFragmentManager();
		if (fm.getBackStackEntryCount() > 0 && fm.isStateSaved()) {
			fm.popBackStack();
			doRefresh();
		} else if (fm.getBackStackEntryCount() > 0) {
			doRefresh();
			super.onBackPressed();
		} else {
			super.onBackPressed();
		}
	}

	private FeedListFragment getFeedListFragment() {
		return (FeedListFragment) getSupportFragmentManager().findFragmentByTag(FeedListFragment.FRAGMENT);
	}

	private CategoryListFragment getCategoryListFragment() {
		return (CategoryListFragment) getSupportFragmentManager().findFragmentByTag(CategoryListFragment.FRAGMENT);
	}

}
