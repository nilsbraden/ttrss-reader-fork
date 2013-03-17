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
import org.ttrssreader.gui.FeedHeadlineActivity;
import org.ttrssreader.gui.MenuActivity;
import org.ttrssreader.gui.interfaces.IItemSelectedListener;
import org.ttrssreader.gui.interfaces.IItemSelectedListener.TYPE;
import org.ttrssreader.gui.interfaces.IUpdateEndListener;
import org.ttrssreader.model.CategoryAdapter;
import org.ttrssreader.model.updaters.ReadStateUpdater;
import org.ttrssreader.model.updaters.Updater;
import org.ttrssreader.utils.Utils;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

public class CategoryListFragment extends ListFragment implements IUpdateEndListener {
    
    private static final TYPE THIS_TYPE = TYPE.CATEGORY;
    
    private static final String SELECTED_INDEX = "selectedIndex";
    private static final int SELECTED_INDEX_DEFAULT = -1;
    private int selectedIndex = SELECTED_INDEX_DEFAULT;
    private int selectedIndexOld = SELECTED_INDEX_DEFAULT;
    
    private static final int SELECT_ARTICLES = MenuActivity.MARK_GROUP + 54;
    
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
    }
    
    @Override
    public void onStop() {
        super.onStop();
        getListView().setVisibility(View.GONE);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null)
            adapter.makeQuery(true);
        getListView().setVisibility(View.VISIBLE);
        Controller.getInstance().lastOpenedFeeds.clear();
        Controller.getInstance().lastOpenedArticles.clear();
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        selectedIndexOld = selectedIndex;
        selectedIndex = position; // Set selected item
        
        Activity activity = getActivity();
        if (activity instanceof IItemSelectedListener)
            ((IItemSelectedListener) activity).itemSelected(THIS_TYPE, selectedIndex, selectedIndexOld,
                    adapter.getId(selectedIndex));
    }
    
    @Override
    public void onUpdateEnd() {
        adapter.refreshQuery();
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(MenuActivity.MARK_GROUP, SELECT_ARTICLES, Menu.NONE, R.string.Commons_SelectArticles);
    }
    
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        Log.d(Utils.TAG, "CategoryActivity: onContextItemSelected called");
        AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) item.getMenuInfo();
        if (adapter == null)
            return false;
        
        int id = adapter.getId(cmi.position);
        
        switch (item.getItemId()) {
            case MenuActivity.MARK_READ:
                if (id < -10)
                    new Updater(this, new ReadStateUpdater(id, 42)).exec();
                new Updater(this, new ReadStateUpdater(id)).exec();
                return true;
            case SELECT_ARTICLES:
                if (id < 0)
                    return false; // Do nothing for Virtual Category or Labels
                Intent i = new Intent(getActivity(), FeedHeadlineActivity.class);
                i.putExtra(FeedHeadlineActivity.FEED_ID, FeedHeadlineActivity.FEED_NO_ID);
                i.putExtra(FeedHeadlineActivity.FEED_CAT_ID, id);
                i.putExtra(FeedHeadlineActivity.FEED_SELECT_ARTICLES, true);
                startActivity(i);
        }
        return false;
    }
    
}
