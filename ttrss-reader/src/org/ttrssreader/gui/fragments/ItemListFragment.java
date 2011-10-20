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
import org.ttrssreader.utils.Utils;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public abstract class ItemListFragment extends ListFragment {
    
    protected static final String SELECTED_INDEX = "selectedIndex";
    protected static final int SELECTED_INDEX_DEFAULT = -1;
    
    protected int selectedIndex = SELECTED_INDEX_DEFAULT;
    protected int selectedIndexOld = SELECTED_INDEX_DEFAULT;
    
    protected static final int MARK_GROUP = 42;
    protected static final int MARK_READ = MARK_GROUP + 1;
    protected static final int MARK_STAR = MARK_GROUP + 2;
    protected static final int MARK_PUBLISH = MARK_GROUP + 3;
    
    protected Context context = null;
    protected ListView listView;
    protected boolean isTablet = false;
    
    @Override
    public void onActivityCreated(Bundle instance) {
        super.onActivityCreated(instance);
        
        context = getActivity().getApplicationContext();
        
        listView = getListView();
        registerForContextMenu(listView);
        
        // This is a tablet if this view exists
        View details = getActivity().findViewById(R.id.details);
        isTablet = details != null && details.getVisibility() == View.VISIBLE;
        
        if (isTablet && selectedIndex != SELECTED_INDEX_DEFAULT)
            showDetails();
    }
    
    public void onListItemClick(ListView l, View v, int position, long id) {
        Log.d(Utils.TAG, "onListItemClick: " + position + " (id: " + id + ")");
        selectedIndexOld = selectedIndex;
        selectedIndex = position; // Set selected item
        
        showDetails();
    }
    
    protected abstract void showDetails();
    
}
