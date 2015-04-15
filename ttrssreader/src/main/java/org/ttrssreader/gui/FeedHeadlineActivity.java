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
import org.ttrssreader.controllers.Data;
import org.ttrssreader.gui.fragments.ArticleFragment;
import org.ttrssreader.gui.fragments.FeedHeadlineListFragment;
import org.ttrssreader.gui.fragments.FeedListFragment;
import org.ttrssreader.gui.fragments.MainListFragment;
import org.ttrssreader.utils.AsyncTask;
import org.ttrssreader.utils.Utils;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

public class FeedHeadlineActivity extends MenuActivity {

	@SuppressWarnings("unused")
	private static final String TAG = FeedHeadlineActivity.class.getSimpleName();

	public static final int FEED_NO_ID = 37846914;

	private int categoryId = Integer.MIN_VALUE;
	private int feedId = Integer.MIN_VALUE;
	private boolean selectArticlesForCategory = false;

	private FeedHeadlineUpdater headlineUpdater = null;

	private int articleId = Integer.MIN_VALUE;

	private FeedListFragment feedFragment;
	private FeedHeadlineListFragment headlineFragment;

	@Override
	protected void onCreate(Bundle instance) {
		super.onCreate(instance);

		Bundle extras = getIntent().getExtras();
		if (instance != null) {
			categoryId = instance.getInt(FeedHeadlineListFragment.FEED_CAT_ID);
			feedId = instance.getInt(FeedHeadlineListFragment.FEED_ID);
			selectArticlesForCategory = instance.getBoolean(FeedHeadlineListFragment.FEED_SELECT_ARTICLES);
			articleId = instance.getInt(FeedHeadlineListFragment.ARTICLE_ID, Integer.MIN_VALUE);
		} else if (extras != null) {
			categoryId = extras.getInt(FeedHeadlineListFragment.FEED_CAT_ID);
			feedId = extras.getInt(FeedHeadlineListFragment.FEED_ID);
			selectArticlesForCategory = extras.getBoolean(FeedHeadlineListFragment.FEED_SELECT_ARTICLES);
			articleId = extras.getInt(FeedHeadlineListFragment.ARTICLE_ID, Integer.MIN_VALUE);
		}

		FragmentManager fm = getFragmentManager();
		feedFragment = (FeedListFragment) fm.findFragmentByTag(FeedListFragment.FRAGMENT);
		if (feedFragment == null) {
			feedFragment = FeedListFragment.newInstance(categoryId);
			fm.beginTransaction().add(R.id.frame_invisible, feedFragment, FeedListFragment.FRAGMENT).commit();
		}

		headlineFragment = (FeedHeadlineListFragment) fm.findFragmentByTag(FeedHeadlineListFragment.FRAGMENT);
		if (headlineFragment == null) {
			displayFeed(feedId, 0);
		}

		// TEMPORARY_SOLUTION_MARKER
		// Put empty articleFragment into the layout so the space doesn't get cluttered with graphic artifacts.
		if (Controller.isTablet) {
			ArticleFragment articleFragment = (ArticleFragment) fm.findFragmentByTag(ArticleFragment.FRAGMENT);
			if (articleFragment == null) {
				articleFragment = ArticleFragment
						.newInstance(Integer.MIN_VALUE, feedId, categoryId, selectArticlesForCategory, 0);
				FragmentTransaction ft = fm.beginTransaction();
				if (!Controller.isTablet) ft.addToBackStack(null);
				ft.add(R.id.frame_sub, articleFragment, ArticleFragment.FRAGMENT);
				ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
				ft.commit();
			}
		}
	}

	@Override
	protected int getLayoutResource() {
		return R.layout.main;
	}

	@Override
	public void onSaveInstanceState(@NotNull Bundle outState) {
		outState.putInt(FeedHeadlineListFragment.FEED_CAT_ID, categoryId);
		outState.putInt(FeedHeadlineListFragment.FEED_ID, feedId);
		outState.putBoolean(FeedHeadlineListFragment.FEED_SELECT_ARTICLES, selectArticlesForCategory);
		outState.putInt(FeedHeadlineListFragment.ARTICLE_ID, articleId);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(@NotNull Bundle instance) {
		categoryId = instance.getInt(FeedHeadlineListFragment.FEED_CAT_ID);
		feedId = instance.getInt(FeedHeadlineListFragment.FEED_ID);
		selectArticlesForCategory = instance.getBoolean(FeedHeadlineListFragment.FEED_SELECT_ARTICLES);
		articleId = instance.getInt(FeedHeadlineListFragment.ARTICLE_ID, Integer.MIN_VALUE);
		super.onRestoreInstanceState(instance);
	}

	@Override
	public void dataLoadingFinished() {
		setTitleAndUnread();
	}

	@Override
	protected void doRefresh() {
		super.doRefresh();
		headlineFragment.doRefresh();

		setTitleAndUnread();
	}

	private void setTitleAndUnread() {
		// Title and unread information:
		if (headlineFragment != null) {
			setTitle(headlineFragment.getTitle());
			setUnread(headlineFragment.getUnread());
		}
	}

	@Override
	protected void doUpdate(boolean forceUpdate) {
		// Only update if no headlineUpdater already running
		if (headlineUpdater != null) {
			if (headlineUpdater.getStatus().equals(AsyncTask.Status.FINISHED)) {
				headlineUpdater = null;
			} else {
				return;
			}
		}

		if (Data.getInstance().isConnected()) {
			if (!isCacherRunning()) {
				headlineUpdater = new FeedHeadlineUpdater(forceUpdate);
				headlineUpdater.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (articleId != Integer.MIN_VALUE) {
			getMenuInflater().inflate(R.menu.article, menu);
			if (Controller.isTablet) super.onCreateOptionsMenu(menu);
		} else {
			super.onCreateOptionsMenu(menu);
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.removeItem(R.id.Menu_MarkAllRead);
		menu.removeItem(R.id.Menu_MarkFeedsRead);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		if (super.onOptionsItemSelected(item)) return true;

		switch (item.getItemId()) {
			case R.id.Menu_Refresh: {
				doUpdate(true);
				return true;
			}
			default:
				return false;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, @NotNull KeyEvent event) {
		if (Controller.getInstance().useVolumeKeys()) {
			if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_N) {
				openNextFragment(-1);
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_B) {
				openNextFragment(1);
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, @NotNull KeyEvent event) {
		if (Controller.getInstance().useVolumeKeys()) {
			if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP
					|| keyCode == KeyEvent.KEYCODE_N || keyCode == KeyEvent.KEYCODE_B) {
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}

	private void openNextFragment(int direction) {
		if (articleId != Integer.MIN_VALUE) {
			openNextArticle(direction);
		} else {
			openNextFeed(direction);
		}
	}

	public void openNextFeed(int direction) {
		int newId = findNext(feedFragment.getFeedIds(), feedId, direction);
		if (newId == Integer.MIN_VALUE) {
			Utils.alert(this, true);
			return;
		}

		displayFeed(newId, direction);
	}

	public void openNextArticle(int direction) {
		int newId = findNext(headlineFragment.getArticleIds(), articleId, direction);
		if (newId == Integer.MIN_VALUE) {
			Utils.alert(this, true);
			return;
		}

		displayArticle(newId, direction);
	}

	protected static int findNext(List<Integer> list, int current, int direction) {
		int[] ids = getNextPrevIds(list, current);
		return (direction < 0) ? ids[0] : ids[1];
	}

	protected static int[] getNextPrevIds(List<Integer> list, Integer search) {
		int index = list.indexOf(search);
		if (index < 0) return new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE};

		int prev = 0 <= (index - 1) ? list.get(index - 1) : Integer.MIN_VALUE;
		int next = list.size() > (index + 1) ? list.get(index + 1) : Integer.MIN_VALUE;
		return new int[] {prev, next};
	}

	/**
	 * Updates all articles from the selected feed.
	 */
	private class FeedHeadlineUpdater extends ActivityUpdater {
		private static final int DEFAULT_TASK_COUNT = 1;

		private FeedHeadlineUpdater(boolean forceUpdate) {
			super(forceUpdate);
		}

		@Override
		protected Void doInBackground(Void... params) {
			taskCount = DEFAULT_TASK_COUNT;

			boolean displayUnread = Controller.getInstance().onlyUnread();
			int progress = 0;
			publishProgress(progress);

			if (selectArticlesForCategory) {
				Data.getInstance().updateArticles(categoryId, displayUnread, true, false, forceUpdate);
			} else {
				Data.getInstance().updateArticles(feedId, displayUnread, false, false, forceUpdate);
			}
			publishProgress(++progress);

			Data.getInstance().calculateCounters();
			Data.getInstance().notifyListeners();
			publishProgress(Integer.MAX_VALUE); // Move progress forward to 100%
			return null;
		}
	}

	@Override
	public void itemSelected(MainListFragment source, int selectedIndex, int selectedId) {
		switch (source.getType()) {
			case FEEDHEADLINE:
				displayArticle(selectedId, 0);
				break;
		}
	}

	private void displayFeed(int newFeedId, int direction) {
		feedId = newFeedId;
		headlineFragment = FeedHeadlineListFragment.newInstance(feedId, categoryId, selectArticlesForCategory);
		FragmentManager fm = getFragmentManager();

		FragmentTransaction ft = fm.beginTransaction();
		setAnimationForDirection(ft, direction);
		if (direction == 0) ft.add(R.id.frame_main, headlineFragment, FeedHeadlineListFragment.FRAGMENT).commit();
		else ft.replace(R.id.frame_main, headlineFragment, FeedHeadlineListFragment.FRAGMENT).commit();

		// Check if a next feed in this direction exists
		if (direction != 0) {
			if (findNext(feedFragment.getFeedIds(), feedId, direction) == Integer.MIN_VALUE) {
				Utils.alert(this);
				return;
			}
		}
	}

	private void displayArticle(int newArticleId, int direction) {
		articleId = newArticleId;
		headlineFragment.setSelectedId(articleId);
		FragmentManager fm = getFragmentManager();

		// Clear back stack
		Controller.sFragmentAnimationDirection = direction;
		fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

		ArticleFragment articleFragment = ArticleFragment
				.newInstance(articleId, feedId, categoryId, selectArticlesForCategory, direction);

		FragmentTransaction ft = fm.beginTransaction();
		if (!Controller.isTablet) ft.addToBackStack(null);
		setAnimationForDirection(ft, direction);
		if (direction == 0) ft.add(R.id.frame_sub, articleFragment, ArticleFragment.FRAGMENT).commit();
		else ft.replace(R.id.frame_sub, articleFragment, ArticleFragment.FRAGMENT).commit();

		// Check if a next feed in this direction exists
		if (direction != 0) {
			if (findNext(headlineFragment.getArticleIds(), articleId, direction) == Integer.MIN_VALUE) {
				Utils.alert(this);
				return;
			}
		}
	}

	private static FragmentTransaction setAnimationForDirection(final FragmentTransaction ft, final int direction) {
		if (Controller.getInstance().animations()) {
			if (direction >= 0) ft.setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left);
			else ft.setCustomAnimations(R.animator.slide_in_left, R.animator.slide_out_right);
		}
		return ft;
	}

	@Override
	public void onBackPressed() {
		articleId = Integer.MIN_VALUE;
		super.onBackPressed();
	}

	public int getCategoryId() {
		return categoryId;
	}

	public int getFeedId() {
		return feedId;
	}

}
