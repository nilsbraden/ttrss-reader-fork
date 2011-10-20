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
import org.ttrssreader.gui.FeedActivity;
import org.ttrssreader.gui.FeedHeadlineActivity;
import org.ttrssreader.gui.interfaces.IConfigurable;
import org.ttrssreader.model.CategoryAdapter;
import org.ttrssreader.utils.Utils;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

public class CategoryListFragment extends ItemListFragment {
    
    private static final int SELECTED_VIRTUAL_CATEGORY = 1;
    private static final int SELECTED_CATEGORY = 2;
    private static final int SELECTED_LABEL = 3;
    
    private CategoryAdapter adapter = null;
    
    @Override
    public void onActivityCreated(Bundle instance) {
        super.onActivityCreated(instance);
        
        adapter = new CategoryAdapter(context);
        setListAdapter(adapter);
        
        // Read the selected list item after orientation changes and similar
        if (instance != null)
            selectedIndex = instance.getInt(SELECTED_INDEX, SELECTED_INDEX_DEFAULT);
        
        // Inject Adapter into activity. Don't know if this is the way to do stuff here...
        if (getActivity() instanceof IConfigurable)
            ((IConfigurable) getActivity()).setAdapter(adapter);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        Controller.getInstance().lastOpenedFeed = null;
        Controller.getInstance().lastOpenedArticle = null;
        
        DBHelper.getInstance().checkAndInitializeDB(context);
    }
    
    protected void showDetails() {
        
        if (adapter == null) {
            Log.d(Utils.TAG, "Adapter shouldn't be null here...");
            return;
        }
        
        // Decide what kind of item was selected
        int catId = adapter.getId(selectedIndex);
        final int selection;
        
        if (catId < 0 && catId >= -4) {
            selection = SELECTED_VIRTUAL_CATEGORY;
        } else if (catId < -10) {
            selection = SELECTED_LABEL;
        } else {
            selection = SELECTED_CATEGORY;
        }
        
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
            
            switch (selection) {
                case SELECTED_VIRTUAL_CATEGORY:
                    details = FeedHeadlineListFragment.newInstance(catId, adapter.getTitle(selectedIndex), 0, false);
                    break;
                case SELECTED_LABEL:
                    details = FeedHeadlineListFragment.newInstance(catId, adapter.getTitle(selectedIndex), -2, false);
                    break;
                case SELECTED_CATEGORY:
                    details = FeedListFragment.newInstance(catId, adapter.getTitle(selectedIndex));
                    break;
            }
            
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
            Intent i = null;
            switch (selection) {
                case SELECTED_VIRTUAL_CATEGORY:
                    i = new Intent(context, FeedHeadlineActivity.class);
                    i.putExtra(FeedHeadlineActivity.FEED_ID, catId);
                    i.putExtra(FeedHeadlineActivity.FEED_TITLE, adapter.getTitle(selectedIndex));
                case SELECTED_LABEL:
                    i = new Intent(context, FeedHeadlineActivity.class);
                    i.putExtra(FeedHeadlineActivity.FEED_ID, catId);
                    i.putExtra(FeedHeadlineActivity.FEED_CAT_ID, -2);
                    i.putExtra(FeedHeadlineActivity.FEED_TITLE, adapter.getTitle(selectedIndex));
                case SELECTED_CATEGORY:
                    i = new Intent(context, FeedActivity.class);
                    i.putExtra(FeedActivity.FEED_CAT_ID, catId);
                    i.putExtra(FeedActivity.FEED_CAT_TITLE, adapter.getTitle(selectedIndex));
            }
            if (i != null)
                startActivity(i);
            
        }
    }
    
}
