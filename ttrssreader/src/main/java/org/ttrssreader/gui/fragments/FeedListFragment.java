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

import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.gui.dialogs.YesNoUpdaterDialog;
import org.ttrssreader.gui.interfaces.IItemSelectedListener.TYPE;
import org.ttrssreader.model.FeedAdapter;
import org.ttrssreader.model.ListContentProvider;
import org.ttrssreader.model.pojos.Category;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.UnsubscribeUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.utils.AsyncTask;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

public class FeedListFragment extends MainListFragment {

	//	private static final String TAG = FeedListFragment.class.getSimpleName();

	private static final TYPE THIS_TYPE = TYPE.FEED;
	public static final String FRAGMENT = "FEED_FRAGMENT";

	private static final String FEED_CAT_ID = "FEED_CAT_ID";

	private static final int MARK_GROUP = 300;
	private static final int MARK_READ = MARK_GROUP + 1;
	private static final int UNSUBSCRIBE = MARK_GROUP + 2;

	// Extras
	private int categoryId;

	private Uri feedUri;

	public static FeedListFragment newInstance(int id) {
		// Create a new fragment instance
		FeedListFragment detail = new FeedListFragment();
		detail.categoryId = id;
		detail.setRetainInstance(true);
		return detail;
	}

	@Override
	public void onCreate(Bundle instance) {
		Controller.getInstance().lastOpenedFeeds.clear();
		if (instance != null)
			categoryId = instance.getInt(FEED_CAT_ID);
		setHasOptionsMenu(true);
		super.onCreate(instance);
	}

	@Override
	public void onActivityCreated(Bundle instance) {
		adapter = new FeedAdapter(getActivity());
		LoaderManager.getInstance(this).restartLoader(TYPE_FEED_ID, null, this);
		super.onActivityCreated(instance);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(FEED_CAT_ID, categoryId);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(MARK_GROUP, MARK_READ, Menu.NONE, R.string.Commons_MarkRead);
		menu.add(MARK_GROUP, UNSUBSCRIBE, Menu.NONE, R.string.Subscribe_unsubscribe);
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) item.getMenuInfo();
		if (cmi == null)
			return false;

		switch (item.getItemId()) {
			case MARK_READ:
				new Updater(getActivity(), new ReadStateUpdater(adapter.getId(cmi.position), 42)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				return true;
			case UNSUBSCRIBE:
				YesNoUpdaterDialog dialog = YesNoUpdaterDialog.getInstance(new UnsubscribeUpdater(adapter.getId(cmi.position)), R.string.Dialog_unsubscribeTitle, R.string.Dialog_unsubscribeText);
				FragmentActivity activity = getActivity();
				if (activity != null)
					dialog.show(activity.getSupportFragmentManager(), YesNoUpdaterDialog.DIALOG);
				return true;
		}
		return false;
	}

	@Override
	public TYPE getType() {
		return THIS_TYPE;
	}

	public int getCategoryId() {
		return categoryId;
	}

	public List<Integer> getFeedIds() {
		return adapter.getIds();
	}

	@NonNull
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Builder builder = ListContentProvider.CONTENT_URI_FEED.buildUpon();
		builder.appendQueryParameter(ListContentProvider.PARAM_CAT_ID, categoryId + "");
		feedUri = builder.build();
		return new CursorLoader(requireActivity(), feedUri, null, null, null, null);
	}

	@Override
	public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
		if (loader.getId() == TYPE_FEED_ID)
			adapter.changeCursor(data);
		super.onLoadFinished(loader, data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (loader.getId() == TYPE_FEED_ID)
			adapter.changeCursor(null);
	}

	@Override
	protected void fetchOtherData() {
		Category category = DBHelper.getInstance().getCategory(categoryId);
		if (category != null)
			title = category.title;
		unreadCount = DBHelper.getInstance().getUnreadCount(categoryId, true);
	}

	@Override
	public void doRefresh() {
		FragmentActivity activity = getActivity();
		if (activity != null && feedUri != null)
			activity.getContentResolver().notifyChange(feedUri, null);
		super.doRefresh();
	}

	@Override
	public boolean isEmptyPlaceholder() {
		return categoryId == Integer.MIN_VALUE;
	}

}
