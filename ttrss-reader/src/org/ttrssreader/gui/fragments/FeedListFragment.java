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
import org.ttrssreader.gui.FeedHeadlineActivity;
import org.ttrssreader.model.FeedAdapter;
import org.ttrssreader.utils.Utils;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

public class FeedListFragment extends ItemListFragment {
    
    public static final String FEED_CAT_ID = "FEED_CAT_ID";
    public static final String FEED_CAT_TITLE = "FEED_CAT_TITLE";
    
    // Extras
    private int categoryId;
    private String categoryTitle;
    
    private FeedAdapter adapter = null;
    
    public static FeedListFragment newInstance(int id, String title) {
        // Create a new fragment instance
        FeedListFragment detail = new FeedListFragment();
        detail.categoryId = id;
        detail.categoryTitle = title;
        detail.setHasOptionsMenu(true);
        return detail;
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
        } else {
            categoryId = -1;
            categoryTitle = null;
        }
        
        adapter = new FeedAdapter(getActivity().getApplicationContext(), categoryId);
        setListAdapter(adapter);
    }
    
    @Override
    public void onResume() {
        super.onResume();
    }
    
    @Override
    public void onPause() {
        super.onPause();
    }
    
    @Override
    public void onStop() {
        super.onStop();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(FEED_CAT_ID, categoryId);
        outState.putString(FEED_CAT_TITLE, categoryTitle);
        super.onSaveInstanceState(outState);
    }
    
    protected void showDetails() {
        
        if (adapter == null) {
            Log.d(Utils.TAG, "Adapter shouldn't be null here...");
            return;
        }
        
        // Decide what kind of item was selected
        int categoryId = adapter.getId(selectedIndex);
        
        if (isTablet) {
            // This is a tablet. Show the recipe in the detail fragment
            Log.d(Utils.TAG, "Filling right pane... (" + selectedIndex + " " + selectedIndexOld + ")");
            
            // Set the list item as checked
            getListView().setItemChecked(selectedIndex, true);
            
            // Get the fragment instance
            ItemListFragment details = (ItemListFragment) getFragmentManager().findFragmentById(R.id.details);
            
            // Is the current selected ondex the same as the clicked? If so, there is no need to update
            if (details != null && selectedIndex == selectedIndexOld)
                return;
            
            details = FeedHeadlineListFragment.newInstance(adapter.getId(selectedIndex),
                    adapter.getTitle(selectedIndex), categoryId, false);
            
            // Replace the old fragment with the new one
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.details, details);
            // Use a fade animation. This makes it clear that this is not a new "layer"
            // above the current, but a replacement
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
            
        } else {
            Log.d(Utils.TAG, "Showing new activity as we are not in 2-pane-mode...");
            
            // This is not a tablet - start a new activity
            Intent i = new Intent(context, FeedHeadlineActivity.class);
            i.putExtra(FeedHeadlineActivity.FEED_CAT_ID, categoryId);
            i.putExtra(FeedHeadlineActivity.FEED_ID, adapter.getId(selectedIndex));
            i.putExtra(FeedHeadlineActivity.FEED_TITLE, adapter.getTitle(selectedIndex));
            if (i != null)
                startActivity(i);
            
        }
    }
    
}
