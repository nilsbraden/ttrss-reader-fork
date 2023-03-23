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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;

import org.ttrssreader.R;
import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.controllers.Data;
import org.ttrssreader.gui.CategoryActivity;
import org.ttrssreader.gui.FeedHeadlineActivity;
import org.ttrssreader.gui.dialogs.ReadStateDialog;
import org.ttrssreader.gui.interfaces.IItemSelectedListener.TYPE;
import org.ttrssreader.model.CategoryAdapter;
import org.ttrssreader.model.ListContentProvider;
import org.ttrssreader.model.updaters.IUpdatable;
import org.ttrssreader.model.updaters.ReadStateUpdater;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

public class CategoryListFragment extends MainListFragment {

	//	private static final String TAG = CategoryListFragment.class.getSimpleName();

	private static final TYPE THIS_TYPE = TYPE.CATEGORY;
	public static final String FRAGMENT = "CATEGORY_FRAGMENT";

	private static final int MARK_GROUP = 100;
	private static final int MARK_READ = MARK_GROUP + 1;
	private static final int SELECT_ARTICLES = MARK_GROUP + 2;
	private static final int SELECT_FEEDS = MARK_GROUP + 3;

	private Uri categoryUri;

	public static CategoryListFragment newInstance() {
		// Create a new fragment instance
		CategoryListFragment detail = new CategoryListFragment();
		detail.setRetainInstance(true);
		return detail;
	}

	@Override
	public void onCreate(Bundle instance) {
		if (!Controller.isTablet)
			Controller.getInstance().lastOpenedFeeds.clear();
		Controller.getInstance().lastOpenedArticles.clear();
		setHasOptionsMenu(true);
		super.onCreate(instance);
	}

	@Override
	public void onActivityCreated(Bundle instance) {
		adapter = new CategoryAdapter(getActivity());
		LoaderManager.getInstance(this).restartLoader(TYPE_CAT_ID, null, this);
		super.onActivityCreated(instance);
	}

	@Override
	public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(MARK_GROUP, MARK_READ, Menu.NONE, R.string.Commons_MarkRead);
		if (Controller.getInstance().invertBrowsing())
			menu.add(MARK_GROUP, SELECT_FEEDS, Menu.NONE, R.string.Commons_SelectFeeds);
		else
			menu.add(MARK_GROUP, SELECT_ARTICLES, Menu.NONE, R.string.Commons_SelectArticles);

	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) item.getMenuInfo();
		if (cmi == null)
			return false;

		int id = adapter.getId(cmi.position);
		switch (item.getItemId()) {
			case MARK_READ:
				IUpdatable updater;
				if (id < -10)
					updater = new ReadStateUpdater(id, 42);
				else
					updater = new ReadStateUpdater(id);
				if (getActivity() != null)
					ReadStateDialog.getInstance(updater).show(getActivity().getSupportFragmentManager());
				return true;
			case SELECT_ARTICLES:
				if (id < 0)
					return false;
				if (getActivity() != null && getActivity() instanceof CategoryActivity)
					((CategoryActivity) getActivity()).displayHeadlines(FeedHeadlineActivity.FEED_NO_ID, id, true);
				return true;
			case SELECT_FEEDS:
				if (id < 0)
					return false;
				if (getActivity() != null && getActivity() instanceof CategoryActivity)
					((CategoryActivity) getActivity()).displayFeed(id);
				return true;
		}
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (super.onOptionsItemSelected(item))
			return true;

		FragmentActivity activity = getActivity();
		if (activity == null)
			return false;

		boolean backAfterUpdate = Controller.getInstance().goBackAfterMarkAllRead() && !Controller.isTablet;

		int itemId = item.getItemId();
		if (itemId == R.id.Menu_MarkAllRead) {
			IUpdatable updateable = new ReadStateUpdater(ReadStateUpdater.TYPE.ALL_CATEGORIES);
			ReadStateDialog.getInstance(updateable, false).show(activity.getSupportFragmentManager());
			return true;
		} else if (itemId == R.id.Menu_MarkFeedsRead) {
			if (selectedId > Integer.MIN_VALUE) {
				IUpdatable updateable = new ReadStateUpdater(selectedId);
				ReadStateDialog.getInstance(updateable, backAfterUpdate).show(activity.getSupportFragmentManager());
			}
			return true;
		}
		return false;
	}

	@Override
	public TYPE getType() {
		return THIS_TYPE;
	}

	@NonNull
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		categoryUri = ListContentProvider.CONTENT_URI_CAT;
		return new CursorLoader(requireActivity(), categoryUri, null, null, null, null);
	}

	@Override
	public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
		if (loader.getId() == TYPE_CAT_ID)
			adapter.changeCursor(data);
		super.onLoadFinished(loader, data);

		CategoryActivity activity = (CategoryActivity) getActivity();
		if (activity != null)
			activity.setTitleAndUnread();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (loader.getId() == TYPE_CAT_ID)
			adapter.changeCursor(null);
	}

	@Override
	protected void fetchOtherData() {
		title = "TTRSS-Reader"; // Hardcoded since this does not change and we would need to be attached to an activity
		// here to be able to read from the resources.
		unreadCount = DBHelper.getInstance().getUnreadCount(Data.VCAT_ALL, true);
	}

	@Override
	public void doRefresh() {
		Activity activity = getActivity();
		if (activity != null && categoryUri != null)
			activity.getContentResolver().notifyChange(categoryUri, null);
		super.doRefresh();
	}

}
