/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 Nils Braden
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

package org.ttrssreader.gui.fragments;

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
import android.app.Activity;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class FeedListFragment extends MainListFragment {
    
    @SuppressWarnings("unused")
    private static final String TAG = FeedListFragment.class.getSimpleName();
    
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
        getLoaderManager().restartLoader(TYPE_FEED_ID, null, this);
        super.onActivityCreated(instance);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(FEED_CAT_ID, categoryId);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
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
                new Updater(getActivity(), new ReadStateUpdater(adapter.getId(cmi.position), 42)).exec();
                return true;
            case UNSUBSCRIBE:
                YesNoUpdaterDialog dialog = YesNoUpdaterDialog.getInstance(
                        new UnsubscribeUpdater(adapter.getId(cmi.position)), R.string.Dialog_unsubscribeTitle,
                        R.string.Dialog_unsubscribeText);
                dialog.show(getFragmentManager(), YesNoUpdaterDialog.DIALOG);
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
    
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == TYPE_FEED_ID) {
            Builder builder = ListContentProvider.CONTENT_URI_FEED.buildUpon();
            builder.appendQueryParameter(ListContentProvider.PARAM_CAT_ID, categoryId + "");
            feedUri = builder.build();
            return new CursorLoader(getActivity(), feedUri, null, null, null, null);
        }
        return null;
    }
    
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
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
        // getLoaderManager().restartLoader(TYPE_HEADLINE_ID, null, this);
        Activity activity = getActivity();
        if (activity != null && feedUri != null)
            activity.getContentResolver().notifyChange(feedUri, null);
        super.doRefresh();
    }
    
}
