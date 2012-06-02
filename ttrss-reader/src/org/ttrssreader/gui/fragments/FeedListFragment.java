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

import org.ttrssreader.gui.interfaces.IConfigurable;
import org.ttrssreader.gui.interfaces.IItemSelectedListener;
import org.ttrssreader.gui.interfaces.IItemSelectedListener.TYPE;
import org.ttrssreader.model.FeedAdapter;
import org.ttrssreader.utils.Utils;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class FeedListFragment extends ListFragment {
    
    private static final TYPE THIS_TYPE = TYPE.FEED;
    
    public static final String FEED_CAT_ID = "FEED_CAT_ID";
    public static final String FEED_CAT_TITLE = "FEED_CAT_TITLE";

    private static final String SELECTED_INDEX = "selectedIndex";
    private static final int SELECTED_INDEX_DEFAULT = -1;
    private int selectedIndex = SELECTED_INDEX_DEFAULT;
    private int selectedIndexOld = SELECTED_INDEX_DEFAULT;
    
    // Extras
    private int categoryId;
    private String categoryTitle;
    
    private FeedAdapter adapter = null;
    private ListView listView;
    
    public static FeedListFragment newInstance(int id, String title) {
        // Create a new fragment instance
        FeedListFragment detail = new FeedListFragment();
        detail.categoryId = id;
        detail.categoryTitle = title;
        detail.setHasOptionsMenu(true);
        return detail;
    }
    
    @Override
    public void onStop() {
        super.onStop();
        getListView().setVisibility(View.GONE);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        getListView().setVisibility(View.VISIBLE);
    }
    
    @Override
    public void onActivityCreated(Bundle instance) {
        super.onActivityCreated(instance);

        listView = getListView();
        registerForContextMenu(listView);
        
        Bundle extras = getActivity().getIntent().getExtras();
        if (extras != null) {
            categoryId = extras.getInt(FEED_CAT_ID);
            categoryTitle = extras.getString(FEED_CAT_TITLE);
        } else if (instance != null) {
            categoryId = instance.getInt(FEED_CAT_ID);
            categoryTitle = instance.getString(FEED_CAT_TITLE);
            selectedIndex = instance.getInt(SELECTED_INDEX, SELECTED_INDEX_DEFAULT);
        }
        
        adapter = new FeedAdapter(getActivity().getApplicationContext(), categoryId);
        setListAdapter(adapter);
        
        // Inject Adapter into activity. Don't know if this is the way to do stuff here...
        if (getActivity() instanceof IConfigurable)
            ((IConfigurable) getActivity()).setAdapter(adapter);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(FEED_CAT_ID, categoryId);
        outState.putString(FEED_CAT_TITLE, categoryTitle);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (adapter == null) {
            Log.w(Utils.TAG, "FeedListFragment: Adapter shouldn't be null here...");
            return;
        }
        
        selectedIndexOld = selectedIndex;
        selectedIndex = position; // Set selected item
        
        if (getActivity() instanceof IItemSelectedListener)
            ((IItemSelectedListener) getActivity()).itemSelected(THIS_TYPE, selectedIndex, selectedIndexOld);
    }
    
}
