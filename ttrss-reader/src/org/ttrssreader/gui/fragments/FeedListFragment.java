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
import org.ttrssreader.gui.MenuActivity;
import org.ttrssreader.gui.dialogs.FeedUnsubscribeDialog;
import org.ttrssreader.gui.interfaces.IItemSelectedListener.TYPE;
import org.ttrssreader.model.FeedAdapter;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import com.actionbarsherlock.view.Menu;

public class FeedListFragment extends MainListFragment {
    
    protected static final TYPE THIS_TYPE = TYPE.FEED;
    
    public static final String FEED_CAT_ID = "FEED_CAT_ID";
    public static final String FEED_CAT_TITLE = "FEED_CAT_TITLE";
    
    // Extras
    private int categoryId;
    
    public static FeedListFragment newInstance(int id) {
        // Create a new fragment instance
        FeedListFragment detail = new FeedListFragment();
        detail.categoryId = id;
        detail.setHasOptionsMenu(true);
        detail.setRetainInstance(true);
        return detail;
    }
    
    @Override
    public void onActivityCreated(Bundle instance) {
        super.onActivityCreated(instance);
        
        if (instance != null) {
            categoryId = instance.getInt(FEED_CAT_ID);
            selectedIndex = instance.getInt(SELECTED_INDEX, SELECTED_INDEX_DEFAULT);
        }
        
        adapter = new FeedAdapter(getActivity().getApplicationContext(), categoryId);
        setListAdapter(adapter);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(FEED_CAT_ID, categoryId);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(MenuActivity.MARK_GROUP, MenuActivity.UNSUBSCRIBE, Menu.NONE, R.string.Subscribe_unsubscribe);
    }
    
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) item.getMenuInfo();
        if (item.getItemId() == MenuActivity.MARK_READ) {
            new Updater(this, new ReadStateUpdater(adapter.getId(cmi.position), 42)).exec();
            return true;
        } else if (item.getItemId() == MenuActivity.UNSUBSCRIBE) {
            FeedUnsubscribeDialog.getInstance(this, adapter.getId(cmi.position)).show(getFragmentManager(),
                    FeedUnsubscribeDialog.DIALOG_UNSUBSCRIBE);
            return true;
        }
        return false;
    }
    
}
