/*
 * ttrss-reader-fork for Android
 * 
 * Copyright (C) 2010 N. Braden.
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
import org.ttrssreader.controllers.Data;
import org.ttrssreader.gui.CategoryActivity;
import org.ttrssreader.gui.FeedHeadlineActivity;
import org.ttrssreader.gui.MenuActivity;
import org.ttrssreader.gui.interfaces.IItemSelectedListener.TYPE;
import org.ttrssreader.model.CategoryAdapter;
import org.ttrssreader.model.ListContentProvider;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class CategoryListFragment extends MainListFragment {
    
    protected static final TYPE THIS_TYPE = TYPE.CATEGORY;
    public static final String FRAGMENT = "CATEGORY_FRAGMENT";
    
    private static final int SELECT_ARTICLES = MenuActivity.MARK_GROUP + 54;
    private static final int SELECT_FEEDS = MenuActivity.MARK_GROUP + 55;
    
    private Uri categoryUri;
    
    public static CategoryListFragment newInstance() {
        // Create a new fragment instance
        CategoryListFragment detail = new CategoryListFragment();
        detail.setHasOptionsMenu(true);
        detail.setRetainInstance(true);
        return detail;
    }
    
    @Override
    public void onCreate(Bundle instance) {
        if (!Controller.isTablet)
            Controller.getInstance().lastOpenedFeeds.clear();
        Controller.getInstance().lastOpenedArticles.clear();
        super.onCreate(instance);
    }
    
    @Override
    public void onActivityCreated(Bundle instance) {
        adapter = new CategoryAdapter(getActivity());
        setListAdapter(adapter);
        getLoaderManager().initLoader(TYPE_CAT_ID, null, this);
        super.onActivityCreated(instance);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (Controller.getInstance().invertBrowsing())
            menu.add(MenuActivity.MARK_GROUP, SELECT_FEEDS, Menu.NONE, R.string.Commons_SelectFeeds);
        else
            menu.add(MenuActivity.MARK_GROUP, SELECT_ARTICLES, Menu.NONE, R.string.Commons_SelectArticles);
        
    }
    
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) item.getMenuInfo();
        if (cmi == null)
            return false;
        
        int id = adapter.getId(cmi.position);
        switch (item.getItemId()) {
            case MenuActivity.MARK_READ:
                if (id < -10)
                    new Updater(getActivity(), new ReadStateUpdater(id, 42)).exec();
                new Updater(getActivity(), new ReadStateUpdater(id)).exec();
                return true;
            case SELECT_ARTICLES:
                if (id < 0)
                    return false;
                if (getActivity() instanceof CategoryActivity)
                    ((CategoryActivity) getActivity()).displayHeadlines(FeedHeadlineActivity.FEED_NO_ID, id, true);
                return true;
            case SELECT_FEEDS:
                if (id < 0)
                    return false;
                if (getActivity() instanceof CategoryActivity)
                    ((CategoryActivity) getActivity()).displayFeed(id);
                return true;
        }
        return false;
    }
    
    @Override
    public TYPE getType() {
        return THIS_TYPE;
    }
    
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == TYPE_CAT_ID) {
            Builder builder = ListContentProvider.CONTENT_URI_CAT.buildUpon();
            categoryUri = builder.build();
            return new CursorLoader(getActivity(), categoryUri, null, null, null, null);
        }
        return null;
    }
    
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == TYPE_CAT_ID)
            adapter.changeCursor(data);
        super.onLoadFinished(loader, data);
    }
    
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == TYPE_CAT_ID)
            adapter.changeCursor(null);
    }
    
    @Override
    protected void fetchOtherData() {
        title = "TTRSS-Reader"; // Hardcoded since this does not change and we would need to be attached to an activity
                                // here to be able to read from the ressources.
        unreadCount = DBHelper.getInstance().getUnreadCount(Data.VCAT_ALL, true);
    }
    
    @Override
    public void doRefresh() {
        // getLoaderManager().restartLoader(TYPE_HEADLINE_ID, null, this);
        Activity activity = getActivity();
        if (activity != null && categoryUri != null)
            activity.getContentResolver().notifyChange(categoryUri, null);
        super.doRefresh();
    }
    
}
