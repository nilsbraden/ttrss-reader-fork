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

import org.ttrssreader.controllers.Controller;
import org.ttrssreader.controllers.DBHelper;
import org.ttrssreader.gui.interfaces.IConfigurable;
import org.ttrssreader.gui.interfaces.IItemSelectedListener;
import org.ttrssreader.gui.interfaces.IItemSelectedListener.TYPE;
import org.ttrssreader.model.CategoryAdapter;
import org.ttrssreader.utils.Utils;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class CategoryListFragment extends ListFragment {
    
    private static final TYPE THIS_TYPE = TYPE.CATEGORY;

    private static final String SELECTED_INDEX = "selectedIndex";
    private static final int SELECTED_INDEX_DEFAULT = -1;
    private int selectedIndex = SELECTED_INDEX_DEFAULT;
    private int selectedIndexOld = SELECTED_INDEX_DEFAULT;
    
    private CategoryAdapter adapter = null;
    private ListView listView;
    
    @Override
    public void onActivityCreated(Bundle instance) {
        super.onActivityCreated(instance);

        listView = getListView();
        registerForContextMenu(listView);
        
        adapter = new CategoryAdapter(getActivity().getApplicationContext());
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
        
        DBHelper.getInstance().checkAndInitializeDB(getActivity().getApplicationContext());
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (adapter == null) {
            Log.w(Utils.TAG, "CategoryListFragment: Adapter shouldn't be null here...");
            return;
        }

        selectedIndexOld = selectedIndex;
        selectedIndex = position; // Set selected item
        
        if (getActivity() instanceof IItemSelectedListener)
            ((IItemSelectedListener) getActivity()).itemSelected(THIS_TYPE, selectedIndex, selectedIndexOld);
    }
    
}
