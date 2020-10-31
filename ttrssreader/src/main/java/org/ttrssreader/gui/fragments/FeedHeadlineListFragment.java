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

package org.ttrssreader.gui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.gui.FeedHeadlineActivity;
import org.ttrssreader.gui.TextInputAlert;
import org.ttrssreader.gui.dialogs.ReadStateDialog;
import org.ttrssreader.gui.dialogs.YesNoUpdaterDialog;
import org.ttrssreader.gui.interfaces.IItemSelectedListener.TYPE;
import org.ttrssreader.gui.interfaces.TextInputAlertCallback;
import org.ttrssreader.gui.view.SwipeGestureListener;
import org.ttrssreader.model.FeedHeadlineAdapter;
import org.ttrssreader.model.ListContentProvider;
import org.ttrssreader.model.pojos.Article;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.pojos.Feed;
import org.ttrssreader.model.updaters.ArticleReadStateUpdater;
import org.ttrssreader.model.updaters.IUpdatable;
import org.ttrssreader.model.updaters.NoteUpdater;
import org.ttrssreader.model.updaters.PublishedStateUpdater;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.StarredStateUpdater;
import org.ttrssreader.model.updaters.UnsubscribeUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.utils.AsyncTask;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

public class FeedHeadlineListFragment extends MainListFragment implements TextInputAlertCallback {

	@SuppressWarnings("unused")
	private static final String TAG = FeedHeadlineListFragment.class.getSimpleName();

	private static final TYPE THIS_TYPE = TYPE.FEEDHEADLINE;
	public static final String FRAGMENT = "FEEDHEADLINE_FRAGMENT";

	public static final String FEED_CAT_ID = "FEED_CAT_ID";
	public static final String FEED_ID = "ARTICLE_FEED_ID";
	public static final String ARTICLE_ID = "ARTICLE_ID";
	public static final String FEED_SELECT_ARTICLES = "FEED_SELECT_ARTICLES";
	public static final String TITLEBAR_HIDDEN = "TITLEBAR_HIDDEN";

	private static final int MARK_GROUP = 200;
	private static final int MARK_READ = MARK_GROUP + 1;
	private static final int MARK_STAR = MARK_GROUP + 2;
	private static final int MARK_PUBLISH = MARK_GROUP + 3;
	private static final int MARK_NOTE = MARK_GROUP + 4;
	private static final int MARK_ABOVE_READ = MARK_GROUP + 5;
	private static final int SHARE = MARK_GROUP + 6;

	private int categoryId = Integer.MIN_VALUE;
	private int feedId = Integer.MIN_VALUE;
	private boolean selectArticlesForCategory = false;

	private Uri headlineUri;

	public static FeedHeadlineListFragment newInstance(int id, int categoryId, boolean selectArticles) {
		FeedHeadlineListFragment detail = new FeedHeadlineListFragment();
		detail.categoryId = categoryId;
		detail.feedId = id;
		detail.selectArticlesForCategory = selectArticles;
		detail.setRetainInstance(true);
		return detail;
	}

	@Override
	public void onCreate(Bundle instance) {
		if (instance != null) {
			categoryId = instance.getInt(FEED_CAT_ID);
			feedId = instance.getInt(FEED_ID);
			selectArticlesForCategory = instance.getBoolean(FEED_SELECT_ARTICLES);
			instance.getBoolean(FEED_SELECT_ARTICLES);
		}

		if (feedId > 0)
			Controller.getInstance().lastOpenedFeeds.add(feedId);
		Controller.getInstance().lastOpenedArticles.clear();
		setHasOptionsMenu(true);
		super.onCreate(instance);
	}

	@Override
	public void onActivityCreated(Bundle instance) {
		AppCompatActivity activity = (AppCompatActivity) getActivity();
		adapter = new FeedHeadlineAdapter(activity, feedId, selectArticlesForCategory);
		LoaderManager.getInstance(this).restartLoader(TYPE_HEADLINE_ID, null, this);

		super.onActivityCreated(instance);

		// Detect touch gestures like swipe and scroll down:
		if (activity != null) {
			ActionBar actionBar = activity.getSupportActionBar();

			gestureDetector = new GestureDetector(getActivity(),
					new HeadlineListGestureListener(actionBar, Controller.getInstance().hideActionbar(), getActivity()));
			gestureListener = new View.OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					return gestureDetector.onTouchEvent(event) || v.performClick();
				}
			};

			if (getView() != null)
				getView().setOnTouchListener(gestureListener);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(FEED_CAT_ID, categoryId);
		outState.putInt(FEED_ID, feedId);
		outState.putBoolean(FEED_SELECT_ARTICLES, selectArticlesForCategory);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		// Get selected Article
		AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		Article a = (Article) adapter.getItem(info.position);

		menu.add(MARK_GROUP, MARK_ABOVE_READ, Menu.NONE, R.string.Commons_MarkAboveRead);

		if (a.isUnread) {
			menu.add(MARK_GROUP, MARK_READ, Menu.NONE, R.string.Commons_MarkRead);
		} else {
			menu.add(MARK_GROUP, MARK_READ, Menu.NONE, R.string.Commons_MarkUnread);
		}

		if (a.isStarred) {
			menu.add(MARK_GROUP, MARK_STAR, Menu.NONE, R.string.Commons_MarkUnstar);
		} else {
			menu.add(MARK_GROUP, MARK_STAR, Menu.NONE, R.string.Commons_MarkStar);
		}

		if (a.isPublished) {
			menu.add(MARK_GROUP, MARK_PUBLISH, Menu.NONE, R.string.Commons_MarkUnpublish);
		} else {
			menu.add(MARK_GROUP, MARK_PUBLISH, Menu.NONE, R.string.Commons_MarkPublish);
			menu.add(MARK_GROUP, MARK_NOTE, Menu.NONE, R.string.Commons_MarkNote);
		}

		menu.add(MARK_GROUP, SHARE, Menu.NONE, R.string.ArticleActivity_ShareLink);
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) item.getMenuInfo();
		if (cmi == null)
			return false;

		Article a = (Article) adapter.getItem(cmi.position);
		if (a == null)
			return false;

		switch (item.getItemId()) {
			case MARK_READ:
				new Updater(getActivity(), new ArticleReadStateUpdater(a, a.isUnread ? 0 : 1)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				break;
			case MARK_STAR:
				new Updater(getActivity(), new StarredStateUpdater(a, a.isStarred ? 0 : 1)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				break;
			case MARK_PUBLISH:
				new Updater(getActivity(), new PublishedStateUpdater(a, a.isPublished ? 0 : 1)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				break;
			case MARK_NOTE:
				new TextInputAlert(this, a).show(getActivity());
				break;
			case MARK_ABOVE_READ:
				new Updater(getActivity(), new ArticleReadStateUpdater(getUnreadAbove(cmi.position), 0)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				break;
			case SHARE:
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("text/plain");
				i.putExtra(Intent.EXTRA_TEXT, a.url);
				i.putExtra(Intent.EXTRA_SUBJECT, a.title);
				startActivity(Intent.createChooser(i, getText(R.string.ArticleActivity_ShareTitle)));
				break;
			default:
				return false;
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (super.onOptionsItemSelected(item))
			return true;
		FragmentActivity activity = getActivity();
		if (activity == null)
			return false;

		switch (item.getItemId()) {
			case R.id.Menu_MarkFeedRead: {
				boolean backAfterUpdate = Controller.getInstance().goBackAfterMarkAllRead();
				if (selectArticlesForCategory) {
					IUpdatable updateable = new ReadStateUpdater(categoryId);
					ReadStateDialog.getInstance(updateable, backAfterUpdate).show(activity.getSupportFragmentManager());
				} else if (feedId >= -4 && feedId < 0) { // Virtual Category
					IUpdatable updateable = new ReadStateUpdater(feedId, 42);
					ReadStateDialog.getInstance(updateable, backAfterUpdate).show(activity.getSupportFragmentManager());
				} else {
					new Updater(activity, new ReadStateUpdater(feedId, 42), backAfterUpdate).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}

				return true;
			}
			case R.id.Menu_FeedUnsubscribe: {
				YesNoUpdaterDialog dialog = YesNoUpdaterDialog.getInstance(new UnsubscribeUpdater(feedId), R.string.Dialog_unsubscribeTitle, R.string.Dialog_unsubscribeText);
				dialog.show(activity.getSupportFragmentManager(), YesNoUpdaterDialog.DIALOG);
				return true;
			}
			default:
				return false;
		}
	}

	/**
	 * Creates a list of articles which are above the given index in the currently displayed list of items.
	 *
	 * @param index the selected index, will be excluded in returned list
	 * @return a list of items above the selected item
	 */
	private List<Article> getUnreadAbove(int index) {
		List<Article> ret = new ArrayList<>();
		for (int i = 0; i < index; i++) {
			Article a = (Article) adapter.getItem(i);
			if (a != null && a.isUnread)
				ret.add(a);
		}
		return ret;
	}

	public void onAddNoteResult(Article a, String note) {
		new Updater(getActivity(), new NoteUpdater(a, note)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public TYPE getType() {
		return THIS_TYPE;
	}

	public int getFeedId() {
		return feedId;
	}

	List<Integer> currentSelectedList = null;

	@Override
	public void setSelectedId(int selectedId) {
		// Cache the current list of articleIds. This is necessary to allow jumping between articles to stop at the
		// end of the unread articles. The list in this fragments adapter is reloaded when there are no more unread
		// articles but we still want to stop reading when the last unread has been reached.
		currentSelectedList = adapter.getIds();
		super.setSelectedId(selectedId);
	}

	public List<Integer> getArticleIds() {
		if (currentSelectedList != null)
			return currentSelectedList;
		return adapter.getIds();
	}

	private class HeadlineListGestureListener extends SwipeGestureListener {
		private HeadlineListGestureListener(ActionBar actionBar, boolean hideActionbar, Activity activity) {
			super(actionBar, hideActionbar, activity);
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

			try {
				if (Math.abs(e1.getY() - e2.getY()) > Controller.relSwipeMaxOffPath)
					return false;

				return super.onFling(e1, e2, velocityX, velocityY);
			} catch (Exception e) {
				// Empty!
			}
			return false;
		}

		@Override
		public boolean onSwipe(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			int pos1 = getListView().pointToPosition((int)e1.getX(), (int)e1.getY());
			int pos2 = getListView().pointToPosition((int)e2.getX(), (int)e2.getY());
			if (pos1 == pos2 && pos1 >= 0 && e2.getX() > e1.getX()){
				Article article = (Article)adapter.getItem(pos1);
				new Updater(getActivity(), new ArticleReadStateUpdater(article, article.isUnread ? 0 : 1)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				// https://stackoverflow.com/questions/4817770/android-listview-with-onitemclicklistener-and-gesturedetector
				MotionEvent cancelEvent = MotionEvent.obtain(e2);
				cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
				getListView().onTouchEvent(cancelEvent);
				return true;
			}

			int direction = e1.getX() > e2.getX() ? 1 : -1;
			FeedHeadlineActivity fhActivity = (FeedHeadlineActivity) getActivity();
			if (fhActivity != null)
				fhActivity.openNextFeed(direction);
			return true;
		}
	}

	@NonNull
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Builder builder = ListContentProvider.CONTENT_URI_HEAD.buildUpon();
		builder.appendQueryParameter(ListContentProvider.PARAM_CAT_ID, categoryId + "");
		builder.appendQueryParameter(ListContentProvider.PARAM_FEED_ID, feedId + "");
		builder.appendQueryParameter(ListContentProvider.PARAM_SELECT_FOR_CAT, (selectArticlesForCategory ? "1" : "0"));
		headlineUri = builder.build();
		return new CursorLoader(requireActivity(), headlineUri, null, null, null, null);
	}

	@Override
	public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
		if (loader.getId() == TYPE_HEADLINE_ID)
			adapter.changeCursor(data);
		super.onLoadFinished(loader, data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (loader.getId() == TYPE_HEADLINE_ID)
			adapter.changeCursor(null);
	}

	@Override
	protected void fetchOtherData() {
		if (selectArticlesForCategory) {
			Category category = DBHelper.getInstance().getCategory(categoryId);
			if (category != null)
				title = category.title;
		} else if (feedId >= -4 && feedId < 0) { // Virtual Category
			Category category = DBHelper.getInstance().getCategory(feedId);
			if (category != null)
				title = category.title;
		} else {
			Feed feed = DBHelper.getInstance().getFeed(feedId);
			if (feed != null) {
				title = feed.title;
				if (feed.icon != null)
					icon = feed.icon;
			}
		}
		unreadCount = DBHelper.getInstance().getUnreadCount(selectArticlesForCategory ? categoryId : feedId, selectArticlesForCategory);
	}

	@Override
	public void doRefresh() {
		Activity activity = getActivity();
		if (activity != null && headlineUri != null)
			activity.getContentResolver().notifyChange(headlineUri, null);
		super.doRefresh();
	}

}
